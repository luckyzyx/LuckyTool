package com.luckyzyx.luckytool.hook.utils

import android.content.Intent
import android.os.SystemClock
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.luckyzyx.luckytool.utils.LogUtils
import com.oplus.miragewindow.OplusMirageOptions
import com.oplus.miragewindow.OplusMirageWindowManager
import org.lsposed.lsparanoid.Obfuscate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 后台挂起（Game HangUp）状态机
 *
 * 任务被最小化时记录 taskId 与后台串流（mirage）displayId，
 * 之后靠 taskId 判定"这个任务正处于挂起中"，用于阻止 moveTaskToBack、临时压住悬浮球删除、
 * 以及恢复时释放虚拟屏。这里只维护状态与判定，不装任何 hook。
 */
@Obfuscate
object MirageHangUpState {

    const val TAG = "MirageHangUp"

    //悬浮球保留窗口：任务最小化后系统会立刻删掉悬浮球，这段时间压住删除等 mirage 起来
    const val KEEP_FLOAT_MS = 2800L

    //同一任务在该窗口内的重复最小化回调直接忽略
    const val MINIMIZE_DEDUPE_MS = 3000L

    private const val TASK_CLASS = "com.android.server.wm.Task"
    private const val FLOAT_HANDLE_INFO = "FloatHandleInfo"

    //C15 的贴边最小化只给这个信息对象（两参 startMinimize），参数里没有 Task
    private const val ZOOM_TASK_INFO = "ZoomTaskInfo"
    private val TASK_ID_FIELDS = arrayOf("mTaskId", "taskId")
    private val USER_ID_FIELDS = arrayOf("userId", "mUserId", "uid")
    private val PKG_NAME_FIELDS = arrayOf("pkgName", "mPkgName", "packageName")

    private val hangUpTaskId = AtomicInteger(-1)
    private val mirageDisplays = ConcurrentHashMap<Int, Int>()
    private val minimizeAt = ConcurrentHashMap<Int, Long>()
    private val releasedDisplays = ConcurrentHashMap.newKeySet<Int>()

    @Volatile
    private var keepFloatUntil = 0L

    fun currentTaskId(): Int = hangUpTaskId.get()

    fun isHangUp(taskId: Int): Boolean = taskId >= 0 && hangUpTaskId.get() == taskId

    /** 最小化去重，true 表示这次回调需要处理 */
    fun shouldHandleMinimize(taskId: Int): Boolean {
        if (taskId < 0) return false
        val now = SystemClock.uptimeMillis()
        val last = minimizeAt[taskId]
        if (last != null && now - last < MINIMIZE_DEDUPE_MS) return false
        minimizeAt[taskId] = now
        return true
    }

    fun markHangUp(taskId: Int) {
        hangUpTaskId.set(taskId)
    }

    fun attachDisplay(taskId: Int, displayId: Int) {
        if (taskId >= 0 && displayId > 0) mirageDisplays[taskId] = displayId
    }

    fun displayOf(taskId: Int): Int = mirageDisplays[taskId] ?: 0

    fun armKeepFloat() {
        keepFloatUntil = SystemClock.uptimeMillis() + KEEP_FLOAT_MS
    }

    /** 清掉挂起标记并返回需要释放的 displayId，0 表示无需释放 */
    fun clearHangUp(taskId: Int): Int {
        if (taskId < 0 || hangUpTaskId.get() != taskId) return 0
        hangUpTaskId.set(-1)
        keepFloatUntil = 0L
        return mirageDisplays.remove(taskId) ?: 0
    }

    /** 只取走 displayId 映射，0 表示无需释放 */
    fun takeDisplay(taskId: Int): Int =
        if (taskId < 0) 0 else mirageDisplays.remove(taskId) ?: 0

    /** 记下宿主已经释放过的虚拟屏，恢复路径据此跳过重复释放 */
    fun markDisplayReleased(displayId: Int) {
        if (displayId > 0) releasedDisplays.add(displayId)
    }

    /** 宿主是否已经释放过这个虚拟屏 */
    fun wasDisplayReleased(displayId: Int): Boolean = displayId > 0 && releasedDisplays.contains(displayId)

    /** 宿主释放虚拟屏后清掉挂在它上面的任务状态，返回清掉的任务数 */
    fun onDisplayReleased(displayId: Int): Int {
        if (displayId <= 0) return 0
        val tasks = mirageDisplays.filterValues { it == displayId }.keys.toList()
        tasks.forEach { mirageDisplays.remove(it) }
        if (hangUpTaskId.get() in tasks) {
            hangUpTaskId.set(-1)
            keepFloatUntil = 0L
        }
        return tasks.size
    }

    /** 宿主把虚拟屏上的任务全部搬回主屏后挂起态已结束，返回清掉的任务数 */
    fun onStacksMovedBack(): Int {
        val count = mirageDisplays.size
        mirageDisplays.clear()
        hangUpTaskId.set(-1)
        keepFloatUntil = 0L
        return count
    }

    /** 悬浮球保留窗口内命中挂起任务，true 表示需要压住这次删除 */
    fun shouldKeepFloat(args: List<Any?>?): Boolean {
        if (SystemClock.uptimeMillis() > keepFloatUntil) return false
        val taskId = currentTaskId()
        if (taskId < 0 || args == null) return false
        return args.any { matchesTask(it, taskId) }
    }

    /** 是否是窗口管理器里的 Task 实例 */
    fun isTask(obj: Any?): Boolean = obj != null && obj.javaClass.name == TASK_CLASS

    /** 从参数里找 com.android.server.wm.Task 实例 */
    fun findTask(args: Iterable<Any?>?): Any? {
        args?.forEach { arg ->
            if (arg?.javaClass?.name == TASK_CLASS) return arg
        }
        return null
    }

    /** 从参数里找贴边信息对象（只有它时才没有 Task 可用） */
    fun findMinimizeInfo(args: Iterable<Any?>?): Any? {
        args?.forEach { arg ->
            if (arg != null && arg.javaClass.name.contains(ZOOM_TASK_INFO)) return arg
        }
        return null
    }

    /** 读信息对象里的包名，没有 Task 时用它构造启动 Intent */
    fun pkgNameOf(obj: Any?): String? {
        if (obj == null) return null
        PKG_NAME_FIELDS.forEach { fieldName ->
            val value = runCatching {
                obj.asResolver().firstFieldOrNull { name = fieldName;superclass() }?.get<String>()
            }.getOrNull()
            if (!value.isNullOrEmpty()) return value
        }
        return null
    }

    private fun isTaskCarrier(className: String): Boolean =
        className == TASK_CLASS || className.contains(FLOAT_HANDLE_INFO) ||
                className.contains(ZOOM_TASK_INFO)

    /** 从参数里取任务 id（Task#mTaskId 或 FloatHandleInfo#taskId） */
    fun taskIdFromArgs(args: Iterable<Any?>?): Int {
        args?.forEach { arg ->
            val taskId = taskIdOf(arg)
            if (taskId >= 0) return taskId
        }
        return -1
    }

    /** 读任务 id，只认 Task、FloatHandleInfo 与贴边信息对象，避免误读其它对象的同名字段 */
    fun taskIdOf(obj: Any?): Int {
        if (obj == null) return -1
        if (!isTaskCarrier(obj.javaClass.name)) return -1
        TASK_ID_FIELDS.forEach { fieldName ->
            val value = runCatching {
                obj.asResolver().firstFieldOrNull {
                    name = fieldName;superclass()
                }?.get<Int>()
            }.getOrNull()
            if (value != null) return value
        }
        return -1
    }

    /** 取任务所属用户 id（后台串流的 TASKINFO_UID 用它拼 userId） */
    fun userIdOf(task: Any?, args: Iterable<Any?>?): Int {
        args?.forEach { arg ->
            readUserId(arg)?.let { return it }
        }
        return readUserId(task) ?: 0
    }

    private fun readUserId(obj: Any?): Int? {
        if (obj == null) return null
        USER_ID_FIELDS.forEach { fieldName ->
            val value = runCatching {
                obj.asResolver().firstFieldOrNull {
                    name = fieldName;superclass()
                }?.get<Int>()
            }.getOrNull()
            if (value != null && value >= 0) return value
        }
        return null
    }

    private fun matchesTask(obj: Any?, taskId: Int): Boolean {
        if (obj == null) return false
        if (obj is Int) return obj == taskId
        if (isTaskCarrier(obj.javaClass.name)) return taskIdOf(obj) == taskId
        return false
    }
}

/**
 * 后台挂起（mirage 后台串流）原语
 *
 * 与状态机分离：这里只管调得通还是调不通，状态由 [MirageHangUpState] 维护，
 * 在哪个 hook 点调用由 HookFloatMirageWindow 决定。
 */
@Obfuscate
object MirageHangUpUtils {

    //任务上的保活标记：各 ROM 只实现其中几个，逐个尝试
    private val LIVELY_FLAGS = arrayOf(
        "setLively",
        "setMaintainTaskState",
        "setSceenOffPlay",
        "setSkipMoveTaskToBack",
        "setIgnoreHiddenFlag",
        "setInsensibleFlag",
        "dontPauseAfterQActivityIfNeed"
    )

    /** 以后台串流模式拉起，返回 displayId，0 表示失败 */
    fun startMirageWindow(intent: Intent?): Int = runCatching {
        val options = OplusMirageOptions.makeBackgroundStreamModeOptions().toBundle()
        OplusMirageWindowManager.getInstance().startMirageWindowMode(intent, options)
    }.getOrElse {
        LogUtils.e(MirageHangUpState.TAG, "startMirageWindow", "start mirage window failed: " + it)
        0
    }

    /** 给任务打保活标记：getWrapper -> getExtImpl -> 逐个试标记（按参数形态适配），返回是否有任一标记成功 */
    fun keepTaskLively(task: Any?, enabled: Boolean = true): Boolean {
        if (task == null) return false
        val ext = runCatching {
            val wrapper = task.asResolver()
                .firstMethodOrNull { name = "getWrapper";emptyParameters() }
                ?.invoke<Any?>() ?: task
            wrapper.asResolver()
                .firstMethodOrNull { name = "getExtImpl";emptyParameters() }
                ?.invoke<Any?>() ?: wrapper
        }.getOrElse {
            LogUtils.e(MirageHangUpState.TAG, "keepTaskLively", "get task ext failed: " + it)
            return false
        }
        val applied = ArrayList<String>(LIVELY_FLAGS.size)
        LIVELY_FLAGS.forEach { methodName ->
            if (invokeLivelyFlag(ext, methodName, task, enabled)) applied += methodName
        }
        if (applied.isNotEmpty()) {
            LogUtils.d(MirageHangUpState.TAG, "keepTaskLively", "lively " + applied.joinToString("/"))
        }
        return applied.isNotEmpty()
    }

    //同名标记在各单位置上的参数形态不同：boolean 开关、int 标记位、或直接把 Task 传进去，
    //逐个形态试一次，命中的形态才真正调用
    private fun invokeLivelyFlag(ext: Any, methodName: String, task: Any, enabled: Boolean): Boolean {
        val booleanMethod = runCatching {
            ext.asResolver().firstMethodOrNull {
                name = methodName;parameterCount = 1
                parameters { types ->
                    types[0] == Boolean::class.javaPrimitiveType || types[0] == Boolean::class.javaObjectType
                }
            }
        }.getOrNull()
        if (booleanMethod != null) {
            return runCatching { booleanMethod.invoke<Any?>(enabled) }
                .onFailure {
                    LogUtils.e(MirageHangUpState.TAG, "invokeLivelyFlag", "$methodName(boolean) failed: " + it)
                }.isSuccess
        }
        val intMethod = runCatching {
            ext.asResolver().firstMethodOrNull {
                name = methodName;parameterCount = 1
                parameters { types -> types[0] == Int::class.javaPrimitiveType || types[0] == Int::class.javaObjectType }
            }
        }.getOrNull()
        if (intMethod != null) {
            val flag = if (enabled) 1 else 0
            return runCatching { intMethod.invoke<Any?>(flag) }
                .onFailure {
                    LogUtils.e(MirageHangUpState.TAG, "invokeLivelyFlag", "$methodName(int) failed: " + it)
                }.isSuccess
        }
        val taskMethod = runCatching {
            ext.asResolver().firstMethodOrNull {
                name = methodName;parameterCount = 1
                parameters { types -> types[0].isInstance(task) }
            }
        }.getOrNull()
        if (taskMethod != null) {
            return runCatching { taskMethod.invoke<Any?>(task) }
                .onFailure {
                    LogUtils.e(MirageHangUpState.TAG, "invokeLivelyFlag", "$methodName(task) failed: " + it)
                }.isSuccess
        }
        return false
    }

    /** 释放后台串流的虚拟屏 */
    fun releaseMirage(classLoader: ClassLoader?, displayId: Int): Boolean {
        if (displayId <= 0) return false
        return runCatching {
            val manager = OplusMirageDisplayManagerUtils(classLoader)
            val instance = manager.getInstance() ?: return@runCatching false
            manager.releaseVirtualDisplay(instance, displayId)
            LogUtils.d(MirageHangUpState.TAG, "releaseMirage", "released mirage displayId=$displayId")
            true
        }.getOrElse {
            LogUtils.e(
                MirageHangUpState.TAG, "releaseMirage",
                "release mirage failed displayId=$displayId: " + it
            )
            false
        }
    }
}
