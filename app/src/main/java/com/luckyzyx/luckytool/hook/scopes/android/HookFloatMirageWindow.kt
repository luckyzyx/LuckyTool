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
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.utils.OplusMirageDisplayManagerUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.startMirageWindow
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
@Suppress("LocalVariableName")
object HookFloatMirageWindow : YukiBaseHooker() {

    override fun onHook() {
        if (preferences(ModulePrefs).getBoolean("run_floating_window_tasks_in_foreground", false)) {
//            if (SDK >= A15) loadHooker(FloatWindowBackRun)
            loadHooker(MultiAppFloatWindowBackRun)
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

            //Source FlexibleTaskController
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

            //Source OplusFlexibleDCSManager
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

            //Source OplusMirageWindowManagerService
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

            //Source OplusMirageWindowManagerService
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