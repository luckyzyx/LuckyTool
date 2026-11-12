package com.luckyzyx.luckytool.hook.scopes.games

import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.data.AppVerInfo
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.IntentUtils
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.startMirageWindow
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class EnableGameRunInBackground(
    private val appVer: AppVerInfo?, val dexKitBridge: DexKitBridge
) : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode
        //开始挂机在 Runnable 里的动作值：V10.44.6 起 switch 前面多了一支，从 6 挪到 5
        //只按已验证的版本切换，未知版本沿用旧值，避免误接管其它分支
        val startAction = if ((appVer?.versionCode ?: 0) >= 500440006) 5.toByte() else 6.toByte()

        //Source HangUpUtil.isSupportBackgroundHangUp
        //新版把支持判定内联进后台挂机面板项(GameHungUpItemState)的构造函数，结果写进该类唯一的 boolean 字段
        //该类的 d() 用 !isSupport 算出 mHide，把字段置 true 面板项就不会被隐藏
        val supportNameList = dexKitBridge.findClass {
            matcher { usingStrings("isSupportBackgroundHangUp") }
        }.map { it.name }
        val itemNameList = dexKitBridge.findClass {
            matcher { usingStrings("GameHungUpItemState") }
        }.map { it.name }
        val supportClassList = supportNameList.filter { itemNameList.contains(it) }
        if (supportClassList.size != 1) {
            YLog.error("EnableGameRunInBackground Support Class -> $supportClassList")
        } else {
            val clazz = supportClassList.first().toClass()
            val supportField = clazz.declaredFields.firstOrNull {
                it.type == Boolean::class.javaPrimitiveType
            }
            clazz.resolve().firstConstructor { parameters(Context::class) }.hook {
                after {
                    runCatching {
                        supportField?.isAccessible = true
                        supportField?.setBoolean(instance, true)
                    }.onFailure { YLog.error("EnableGameRunInBackground Support -> $it") }
                }
            }
        }

        //Source HangUpUtil.startBackgroundHangUp
        //开始挂机被内联进一个多用途 Runnable，靠 byte 动作值选分支，这里只接管开始挂机那一支
        dexKitBridge.findClass {
            matcher { usingStrings("startBackgroundHangUp not in game, return") }
        }.apply {
            checkDataList("EnableGameRunInBackground HangUp Runnable")
            val clazz = single().name.toClass()
            val actionField = clazz.declaredFields.firstOrNull {
                it.type == Byte::class.javaPrimitiveType
            }
            val contextField = clazz.declaredFields.firstOrNull {
                Context::class.java.isAssignableFrom(it.type)
            }
            clazz.resolve().firstMethod { name = "run";emptyParameters() }.hook {
                before {
                    val self = instance
                    val action = runCatching {
                        actionField?.isAccessible = true
                        actionField?.get(self) as? Byte
                    }.getOrNull()
                    if (action != startAction) return@before
                    if (osCode >= 34) {
                        startMirageWindow(null)
                    } else {
                        val context = runCatching {
                            contextField?.isAccessible = true
                            contextField?.get(self) as? Context
                        }.getOrNull() ?: return@before
                        IntentUtils(context).startBackgroundRunServiceV14()
                    }
                    result = null
                }
            }
        }
    }
}
