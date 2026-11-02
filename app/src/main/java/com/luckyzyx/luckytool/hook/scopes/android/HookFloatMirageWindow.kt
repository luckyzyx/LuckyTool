package com.luckyzyx.luckytool.hook.scopes.android

import android.app.ActivityManager.RunningTaskInfo
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.os.UserHandle
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.luckyzyx.luckytool.hook.utils.MirageHangUpState
import com.luckyzyx.luckytool.hook.utils.MirageHangUpUtils
import com.luckyzyx.luckytool.hook.utils.OplusMirageDisplayManagerUtils
import com.luckyzyx.luckytool.utils.LogUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.startMirageWindow
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
@Suppress("LocalVariableName")
object HookFloatMirageWindow : YukiBaseHooker() {

    override fun onHook() {
        if (preferences(ModulePrefs).getBoolean("run_floating_window_tasks_in_foreground", false)) {
            loadHooker(MultiAppFloatWindowBackRun)
            //后台保活（Game HangUp）从 ColorOS 15 起才有贴边最小化入口，C14 无此类
            if (getOSVersionCode >= 35) loadHooker(HangUpKeepAlive)
        }
    }

    @Obfuscate
    object FloatWindowBackRun : YukiBaseHooker() {

        private val Task = "com.android.server.wm.Task"
        private val ActivityTaskManagerService = "com.android.server.wm.ActivityTaskManagerService"
        private val RootWindowContainer = "com.android.server.wm.RootWindowContainer"

        override fun onHook() {
            var taskId: Int = -1
            var task: Any? = null

            //宿主在任务进入可调整大小的分屏 / 小窗状态时回调，借此记下 taskId 与对应 Task
            "com.android.server.wm.FlexibleTaskController".toClass().resolve().apply {
                firstMethod { name = "notifyFlexibleTaskEvent" }.hook {
                    before {
                        taskId = firstArg().get<Int>() ?: 0
                        val mAtms = firstField { type = ActivityTaskManagerService }.of(instance)
                            .get() ?: return@before
                        val mRootWindowContainer = mAtms.asResolver().firstField {
                            type = RootWindowContainer;superclass()
                        }.get() ?: return@before
                        task = mRootWindowContainer.asResolver().firstMethod {
                            name = "anyTaskForId"
                            parameterCount = 1
                        }.invoke(taskId) ?: return@before
                    }
                }
            }

            //宿主在悬浮球进入 / 任务最小化时回调，借此把挂起任务接到后台串流（mirage）上
            "com.android.server.wm.OplusFlexibleDCSManager".toClass().resolve().apply {
                (firstMethodOrNull { name = "onFloatHandleEnter" }
                    ?: firstMethod { name = "startMinimize" }).hook {
                    after {
                        val info = firstArg().get() ?: return@after
                        val curTaskId = info.asResolver().firstField { name = "taskId" }.get<Int>()
                            ?: -1
                        val curUserId = info.asResolver().firstField { name = "userId" }.get<Int>()
                            ?: 0

                        if (taskId != curTaskId) return@after
                        if (task == null) return@after

                        val taskInfo = task.asResolver().firstMethod {
                            name = "getTaskInfo"
                            returnType = classOf<RunningTaskInfo>()
                        }.invoke<RunningTaskInfo>() ?: return@after

                        val baseIntent = taskInfo.asResolver().firstField {
                            type = Intent::class;superclass()
                        }.get<Intent>() ?: return@after

                        baseIntent.putExtra("TASKINFO_UID", curUserId)

                        startMirageWindow(baseIntent)
                    }
                }
            }

            //挂起期间拦下退后台，别让任务真的被移走
            "com.android.server.wm.OplusMirageWindowManagerService".toClass().resolve().apply {
                firstMethod { name = "moveTaskToBack" }.hook {
                    before {
                        val curTask = firstArg().get() ?: return@before
                        val mTaskId = curTask.asResolver().firstField { name = "mTaskId" }.get<Int>()
                            ?: -1
                        if (taskId == mTaskId) result = null
                    }
                }
            }
        }
    }

    @Obfuscate
    object MultiAppFloatWindowBackRun : YukiBaseHooker() {
        override fun onHook() {
            val activityTaskManagerService = "com.android.server.wm.ActivityTaskManagerService"
            val OPLUS_MIRAGE_CAR_DUMMY_ACTION = "android.intent.action.OPLUS_MIRAGE_CAR_DUMMY"

            //把启动请求改投到 mirage 虚拟屏上，实现多个应用同时后台运行
            "com.android.server.wm.OplusMirageWindowManagerService".toClass().resolve().apply {
                firstMethod { name = "startActivityToMirageDisplay" }.hook {
                    before {
                        val parcelable = firstArg().get<Parcelable>()
                        val displayId = arg(1).get<Int>() ?: 0
                        val startOptions = lastArg().get<Bundle>()

                        val mAtms = firstField { type = activityTaskManagerService }.of(instance)
                            .get() ?: return@before
                        val context = mAtms.asResolver().firstField { type = Context::class }
                            .get<Context>() ?: return@before

                        val options = startOptions?.let {
                            ActivityOptions::class.resolve().firstMethod {
                                name = "fromBundle";parameters(Bundle::class)
                            }.invoke<ActivityOptions>(startOptions)
                        } ?: ActivityOptions.makeBasic()
                        options.setLaunchDisplayId(displayId)

                        Handler(Looper.getMainLooper()).post {
                            var intent: Intent? = null
                            var pendingIntent: PendingIntent? = null

                            try {
                                when (parcelable) {
                                    is Intent -> intent = parcelable
                                    is PendingIntent -> {
                                        pendingIntent = parcelable
                                        intent = pendingIntent.asResolver().firstMethod {
                                            name = "getIntent";emptyParameters()
                                        }.invoke<Intent?>()
                                    }
                                }

                                if (intent != null) {
                                    if (intent.action != OPLUS_MIRAGE_CAR_DUMMY_ACTION) {
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        try {
                                            if (pendingIntent == null) {

                                                val uid = intent.getIntExtra("TASKINFO_UID", -1)
                                                if (uid > 0 && uid.toString().startsWith("999")) {
                                                    val userHandle =
                                                        UserHandle.getUserHandleForUid(uid)
                                                    context.asResolver().firstMethod {
                                                        name = "startActivityAsUser"
                                                        parameters(
                                                            Intent::class, Bundle::class,
                                                            UserHandle::class
                                                        )
                                                    }.invoke(intent, options.toBundle(), userHandle)
                                                } else {
                                                    context.startActivity(
                                                        intent, options.toBundle()
                                                    )
                                                }
                                            } else {
                                                pendingIntent.send(
                                                    null, 0, null, null,
                                                    null, null, options.toBundle()
                                                )
                                            }
                                        } catch (_: Exception) {

                                        }
                                    } else {
                                        firstField { name = "mRealCarDisplayId" }.of(instance)
                                            .set(displayId)
                                    }
                                }
                                OplusMirageDisplayManagerUtils(hostClassLoader!!).apply {
                                    val ins = getInstance() ?: return@post
                                    notifyCastSuccess(ins, displayId)
                                }
                            } catch (_: ActivityNotFoundException) {

                            }
                        }
                        result = null
                    }
                }
            }
        }
    }
}

/**
 * 后台保活（Game HangUp）
 *
 * 任务被最小化 / 从悬浮球挂起时，把它以「后台串流」模式
 * 拉起到 mirage 虚拟屏上，同时给任务打保活标记、临时压住悬浮球的删除，应用继续在后台运行；
 * 从悬浮球恢复任务时清掉挂起标记并释放虚拟屏。
 */
@Obfuscate
object HangUpKeepAlive : YukiBaseHooker() {

    private const val TAG = MirageHangUpState.TAG
    private const val TASKINFO_UID = "TASKINFO_UID"
    private const val FLEXIBLE_DCS_MANAGER = "com.android.server.wm.OplusFlexibleDCSManager"
    private const val MIRAGE_WINDOW_MANAGER_SERVICE =
        "com.android.server.wm.OplusMirageWindowManagerService"
    private const val FLOAT_HANDLE_CONTROLLER = "com.android.server.wm.FloatHandleController"
    private const val ZOOM_STATE_MANAGER = "com.android.server.wm.OplusZoomStateManager"
    private const val MIRAGE_DISPLAY_MANAGER = "com.android.server.display.OplusMirageDisplayManagerService"
    private const val MIRAGE_KEEP_ALIVE_POLICY = "com.android.server.wm.OplusMirageKeepAlivePolicy"
    private const val APP_GLOBALS = "android.app.AppGlobals"

    //zoom 状态机里「进入贴边态」的两个迁移值：ZOOM_TO_MINI / SUPER_MINI_TO_MINI
    private const val ZOOM_TO_MINI = 12
    private const val SUPER_MINI_TO_MINI = 52
    private const val ROOT_TASK_FIELD = "mRootTask"

    //宿主先做完贴边/最小化转场才轮到我们搬运任务，搬早了转场结束会把刚搬走的任务重新收回主屏
    private const val MIRAGE_START_DELAY_MS = 400L
    //搬到虚拟屏的任务仍保持暂停态，稍后再补一次顶层活动恢复
    private const val MIRAGE_RESUME_DELAY_MS = 300L
    private const val RESTORE_RELEASE_DELAY_MS = 1200L

    private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

    override fun onHook() {
        runCatching { hookMinimize() }
            .onFailure { LogUtils.e(TAG, "onHook", "hook minimize failed: " + it) }
        runCatching { hookMoveTaskToBack() }
            .onFailure { LogUtils.e(TAG, "onHook", "hook moveTaskToBack failed: " + it) }
        runCatching { hookKeepFloatHandle() }
            .onFailure { LogUtils.e(TAG, "onHook", "hook keep float handle failed: " + it) }
        runCatching { hookReleaseOnRestore() }
            .onFailure { LogUtils.e(TAG, "onHook", "hook release on restore failed: " + it) }
        runCatching { hookZoomState() }
            .onFailure { LogUtils.e(TAG, "onHook", "hook zoom state failed: " + it) }
        runCatching { hookSelfHeal() }
            .onFailure { LogUtils.e(TAG, "onHook", "hook self heal failed: " + it) }
    }

    //自愈：宿主释放虚拟屏或把任务搬回主屏后挂起态已经结束，清掉残留状态，
    //否则同一个任务再次贴边会被 displayId 判定成「已在挂起中」而永远跳过
    private fun hookSelfHeal() {
        MIRAGE_DISPLAY_MANAGER.toClass().resolve().apply {
            firstMethodOrNull { name = "releaseVirtualDisplayDevice";parameterCount = 1 }?.intercept {
                val result = proceed()
                runCatching {
                    val displayId = firstArg() as? Int ?: return@runCatching
                    MirageHangUpState.markDisplayReleased(displayId)
                    val cleared = MirageHangUpState.onDisplayReleased(displayId)
                    if (cleared > 0) LogUtils.d(TAG, "hookSelfHeal", "display $displayId released, cleared $cleared task(s)")
                }.onFailure { LogUtils.e(TAG, "hookSelfHeal", "clear released display failed: " + it) }
                result
            }
            LogUtils.d(TAG, "hookSelfHeal", "hooked display release")
        }
        MIRAGE_WINDOW_MANAGER_SERVICE.toClass().resolve().apply {
            firstMethodOrNull { name = "moveStacksToDefaultDisplay" }?.intercept {
                val result = proceed()
                runCatching {
                    val cleared = MirageHangUpState.onStacksMovedBack()
                    if (cleared > 0) LogUtils.d(TAG, "hookSelfHeal", "stacks moved back, cleared $cleared task(s)")
                }.onFailure { LogUtils.e(TAG, "hookSelfHeal", "clear moved back stacks failed: " + it) }
                result
            }
        }
        MIRAGE_KEEP_ALIVE_POLICY.toClass().resolve().apply {
            firstMethodOrNull { name = "onStartFromPrimaryScreenRecents" }?.intercept {
                val result = proceed()
                runCatching { onRestoreFromPrimary(args) }
                    .onFailure { LogUtils.e(TAG, "hookSelfHeal", "handle restore from primary failed: " + it) }
                result
            }
        }
    }

    //主屏入口（最近任务）把挂起中的任务拉回来时，挂起态结束，顺手释放它的虚拟屏
    private fun onRestoreFromPrimary(args: List<Any?>) {
        val taskId = MirageHangUpState.taskIdFromArgs(args)
        if (taskId < 0 || !MirageHangUpState.isHangUp(taskId)) return
        LogUtils.d(TAG, "onRestoreFromPrimary", "restore task $taskId from primary screen")
        releaseAfterRestore(MirageHangUpState.clearHangUp(taskId))
    }

    //挂起入口：贴边最小化时宿主调用 startMinimize
    private fun hookMinimize() {
        FLEXIBLE_DCS_MANAGER.toClass().resolve().apply {
            //startMinimize 是实测入口，onFloatHandleEnter 只是同族兜底
            val method = firstMethodOrNull { name = "startMinimize" }
                ?: firstMethodOrNull { name = "onFloatHandleEnter" }
            if (method == null) {
                LogUtils.w(TAG, "hookMinimize", "startMinimize not found")
                return
            }
            method.intercept {
                //先放行原生挂起流程，成功后再把它接到后台串流上
                val result = proceed()
                runCatching { onTaskMinimized(args) }
                    .onFailure { LogUtils.e(TAG, "hookMinimize", "handle minimize failed: " + it) }
                result
            }
            LogUtils.d(TAG, "hookMinimize", "hooked startMinimize")
        }
    }

    //C14 没有 startMinimize：贴边挂起由 zoom 状态机自己承接，进入 MINI 态就是挂起时刻
    private fun hookZoomState() {
        //C15+ 有 startMinimize，C13- 走 SystemUI 的 zoom 状态机（那边已验证可用），这条只给 C14 兜底；
        //C13 上同一个类也存在，但那条链路已验证能用，不在这里重复挂
        if (getOSVersionCode != 34) return
        ZOOM_STATE_MANAGER.toClass().resolve().apply {
            val methods = listOfNotNull(
                firstMethodOrNull { name = "onZoomEnter";parameterCount = 4 },
                firstMethodOrNull { name = "enterZoom";parameterCount = 5 },
            )
            if (methods.isEmpty()) {
                LogUtils.w(TAG, "hookZoomState", "zoom enter not found")
                return
            }
            methods.forEach { method ->
                method.intercept {
                    //先放行原生缩放流程，状态落定后任务才挂在实例上
                    val result = proceed()
                    runCatching { onZoomStateEntered(instance, args) }
                        .onFailure { LogUtils.e(TAG, "hookZoomState", "handle zoom enter failed: " + it) }
                    result
                }
            }
            LogUtils.d(TAG, "hookZoomState", "hooked zoom state " + methods.size)
        }
    }

    //进入贴边态时宿主把当前任务挂在实例的 mRootTask 上，从这里取
    private fun onZoomStateEntered(instance: Any?, args: List<Any?>) {
        val state = args.getOrNull(0) as? Int ?: return
        if (state != ZOOM_TO_MINI && state != SUPER_MINI_TO_MINI) return
        val task = (instance ?: return).asResolver()
            .firstFieldOrNull { name = ROOT_TASK_FIELD;superclass() }
            ?.get<Any?>()
        if (!MirageHangUpState.isTask(task)) {
            LogUtils.d(TAG, "onZoomStateEntered", "zoom state=$state without task, skip")
            return
        }
        LogUtils.d(TAG, "onZoomStateEntered", "zoom state=$state taskId=" + MirageHangUpState.taskIdOf(task))
        hangUpTask(task, null, args, "zoom state=$state")
    }

    //挂起中的任务不允许退到后台
    private fun hookMoveTaskToBack() {
        MIRAGE_WINDOW_MANAGER_SERVICE.toClass().resolve().apply {
            firstMethodOrNull { name = "moveTaskToBack" }?.intercept {
                if (!MirageHangUpState.isHangUp(MirageHangUpState.taskIdOf(firstArg()))) {
                    return@intercept proceed()
                }
                LogUtils.d(
                    TAG, "hookMoveTaskToBack",
                    "block moveTaskToBack taskId=" + MirageHangUpState.currentTaskId()
                )
                //置空结果让这次退后台不生效（原方法无返回值）
                null
            }
        }
    }

    //挂起瞬间系统会立刻删掉悬浮球，保留窗口内压住这次删除
    private fun hookKeepFloatHandle() {
        FLOAT_HANDLE_CONTROLLER.toClass().resolve().apply {
            firstMethodOrNull { name = "removeFloatHandle" }?.intercept {
                if (!MirageHangUpState.shouldKeepFloat(args)) return@intercept proceed()
                LogUtils.d(TAG, "hookKeepFloatHandle", "keep float handle")
                null
            }
            firstMethodOrNull { name = "isNeedRemoveFloat" }?.intercept {
                if (!MirageHangUpState.shouldKeepFloat(args)) return@intercept proceed()
                //C15+ 该方法返回 void，置空结果即跳过原方法
                null
            }
        }
    }

    //从贴边视图/悬浮球恢复任务时清掉挂起标记，虚拟屏等宿主把任务搬回主屏之后再放
    private fun hookReleaseOnRestore() {
        FLOAT_HANDLE_CONTROLLER.toClass().resolve().apply {
            firstMethodOrNull { name = "startActivityByFloatInfo" }?.intercept {
                val taskId = MirageHangUpState.taskIdFromArgs(args)
                val displayId = MirageHangUpState.clearHangUp(taskId)
                if (taskId >= 0) LogUtils.d(TAG, "hookReleaseOnRestore", "restore from float info taskId=$taskId displayId=$displayId")
                //先放行宿主的恢复流程：任务还挂在虚拟屏上时释放它，会把任务连同小窗一起带走
                val result = proceed()
                releaseAfterRestore(displayId)
                result
            }
            firstMethodOrNull { name = "removeFloatHandleInner" }?.intercept {
                //保留窗口内先压住删除，窗口过了同一入口负责释放虚拟屏
                if (MirageHangUpState.shouldKeepFloat(args)) {
                    LogUtils.d(TAG, "hookReleaseOnRestore", "keep float handle inner")
                    return@intercept null
                }
                val result = proceed()
                val displayId = MirageHangUpState.takeDisplay(MirageHangUpState.taskIdFromArgs(args))
                if (displayId > 0) LogUtils.d(TAG, "hookReleaseOnRestore", "float handle inner removed, take displayId=$displayId")
                releaseAfterRestore(displayId)
                result
            }
        }
    }

    //恢复流程可能由宿主自己释放虚拟屏，等它收尾后确认没人释放才由我们兜底，避免重复释放
    private fun releaseAfterRestore(displayId: Int) {
        if (displayId <= 0) return
        mainHandler.postDelayed({
            if (MirageHangUpState.wasDisplayReleased(displayId)) {
                LogUtils.d(TAG, "releaseAfterRestore", "display $displayId released by host, skip")
                return@postDelayed
            }
            LogUtils.d(TAG, "releaseAfterRestore", "release leftover mirage displayId=$displayId")
            MirageHangUpUtils.releaseMirage(hostClassLoader, displayId)
        }, RESTORE_RELEASE_DELAY_MS)
    }

    private fun onTaskMinimized(args: List<Any?>) {
        //C16+ 的参数里直接带 Task；C15 的两参 startMinimize 只有贴边信息对象
        val task = MirageHangUpState.findTask(args)
        val info = if (task == null) MirageHangUpState.findMinimizeInfo(args) else null
        hangUpTask(task, info, args, "minimize")
    }

    private fun hangUpTask(task: Any?, info: Any?, args: List<Any?>, source: String) {
        val taskId = if (task != null) {
            MirageHangUpState.taskIdOf(task)
        } else {
            MirageHangUpState.taskIdOf(info)
        }
        if (taskId < 0) {
            LogUtils.e(TAG, "hangUpTask", "task id not found, skip hang up ($source)")
            return
        }
        if (MirageHangUpState.displayOf(taskId) > 0) {
            LogUtils.d(TAG, "hangUpTask", "task $taskId already hanging up")
            return
        }
        if (!MirageHangUpState.shouldHandleMinimize(taskId)) return
        val baseIntent = if (task != null) {
            baseIntentOf(task)
        } else {
            launchIntentOf(MirageHangUpState.pkgNameOf(info))
        }
        if (baseIntent == null) {
            LogUtils.e(TAG, "hangUpTask", "base intent null, skip hang up taskId=$taskId")
            return
        }
        val userId = MirageHangUpState.userIdOf(task ?: info, args)
        if (userId >= 0) baseIntent.putExtra(TASKINFO_UID, userId)
        MirageHangUpState.markHangUp(taskId)
        MirageHangUpState.armKeepFloat()
        val lively = MirageHangUpUtils.keepTaskLively(task)
        mainHandler.postDelayed({
            startMirage(taskId, task, baseIntent, userId, lively, source)
        }, MIRAGE_START_DELAY_MS)
    }

    //贴边转场结束后再把任务搬到虚拟屏，转场收尾不会把任务重新收回主屏
    private fun startMirage(
        taskId: Int,
        task: Any?,
        baseIntent: Intent,
        userId: Int,
        lively: Boolean,
        source: String,
    ) {
        runCatching {
            val displayId = MirageHangUpUtils.startMirageWindow(baseIntent)
            MirageHangUpState.attachDisplay(taskId, displayId)
            LogUtils.d(
                TAG, "startMirage",
                "hang up taskId=$taskId userId=$userId displayId=$displayId lively=$lively ($source)"
            )
            if (displayId <= 0 || task == null) return@runCatching
            mainHandler.postDelayed({ resumeTask(taskId, task) }, MIRAGE_RESUME_DELAY_MS)
        }.onFailure { LogUtils.e(TAG, "startMirage", "start mirage for task $taskId failed: " + it) }
    }

    //搬到虚拟屏的任务仍是暂停态，补一次顶层活动恢复应用才真正继续运行
    private fun resumeTask(taskId: Int, task: Any) {
        runCatching {
            val resumed = task.asResolver()
                .firstMethodOrNull { name = "resumeTopActivityUncheckedLocked";emptyParameters() }
                ?.invoke<Boolean>() ?: false
            LogUtils.d(TAG, "resumeTask", "resume taskId=$taskId result=$resumed")
        }.onFailure { LogUtils.e(TAG, "resumeTask", "resume task $taskId failed: " + it) }
    }

    //system_server 里没有现成 Context，经 AppGlobals 取系统 Application 查启动 Intent
    private fun launchIntentOf(pkgName: String?): Intent? {
        if (pkgName.isNullOrEmpty()) return null
        return runCatching {
            val application = APP_GLOBALS.toClass().resolve()
                .firstMethodOrNull { name = "getInitialApplication";emptyParameters() }
                ?.invoke<Context>() ?: return@runCatching null
            application.packageManager.getLaunchIntentForPackage(pkgName)
        }.getOrElse {
            LogUtils.e(TAG, "launchIntentOf", "resolve launch intent failed for $pkgName: " + it)
            null
        }
    }

    private fun baseIntentOf(task: Any): Intent? = runCatching {
        val taskInfo = task.asResolver()
            .firstMethodOrNull { name = "getTaskInfo";emptyParameters() }
            ?.invoke<Any?>() ?: return@runCatching null
        taskInfo.asResolver().firstFieldOrNull { type = Intent::class;superclass() }
            ?.get<Intent>()?.let { Intent(it) }
    }.getOrNull()
}
