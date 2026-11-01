package com.luckyzyx.luckytool.hook.scopes.systemui

import android.app.ActivityManager.RunningTaskInfo
import android.content.Intent
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.startMirageWindow
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RunFloatingWindowTasksInForeground : YukiBaseHooker() {

    override fun onHook() {
        var flag = -1
        var status: Boolean

        //Source ZoomStateManager
        "com.oplus.zoom.zoomstate.ZoomStateManager".toClass().resolve().apply {
            firstMethod {
                name = "requestChangeZoomTask"
                parameters(Int::class, Boolean::class)
            }.hook {
                before {
                    flag = firstArg().get<Int>() ?: 0
                    status = lastArg().get<Boolean>() ?: false

                    //浮窗全屏 flag 4
                    //浮窗贴边 flag 5
                    //浮窗退出 flag 6

                    if (flag == 5 && status) {
                        val mTaskInfo = firstField { name = "mTaskInfo" }.of(instance)
                            .get<RunningTaskInfo>() ?: return@before

                        val baseIntent = mTaskInfo.asResolver().firstField {
                            type = Intent::class;superclass()
                        }.get<Intent>() ?: return@before

                        val uid = mTaskInfo.asResolver().firstField { name = "uid";superclass() }
                            .get<Int>() ?: return@before
                        if (uid > 0) baseIntent.putExtra("TASKINFO_UID", uid)

                        val displayId = startMirageWindow(baseIntent)
                        YLog.info("MirageHangUp -> systemui hang up uid=$uid displayId=$displayId")
                        result = null
                    }
                }
            }
        }

        //Source FloatHandleController
        "com.oplus.zoom.ui.floathandle.FloatHandleController".toClass().resolve().apply {
            firstMethod { name = "onTaskMovedToFront" }.hook {
                before {
                    if (flag != 5) return@before
                    YLog.info("MirageHangUp -> systemui block onTaskMovedToFront")
                    result = null
                }
            }
        }
    }
}