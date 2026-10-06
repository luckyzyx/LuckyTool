package com.luckyzyx.luckytool.hook.scopes.games

import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.IntentUtils
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.startMirageWindow
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class EnableGameRunInBackground(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode

        //Source HangUpUtil
        dexKitBridge.findClass {
            matcher {
                addFieldForType(classOf<List<*>>())
                addMethod { paramCount(0);returnType(classOf<Boolean>()) }
                addMethod { paramCount(0);returnType(Void.TYPE) }
                addMethod { paramTypes(classOf<Context>());returnType(Void.TYPE) }
                usingStrings("HangUpUtil", "isSupportBackgroundHangUp")
            }
        }.apply {
            checkDataList("EnableGameRunInBackground Cls")
            findMethod {
                matcher {
                    paramCount(0)
                    returnType(classOf<Boolean>())
                    usingStrings("isSupportBackgroundHangUp")
                }
            }.apply {
                checkDataList("EnableGameRunInBackground Support")
                single().className.toClass().resolve().apply {
                    firstMethod {
                        name = single().methodName
                        emptyParameters()
                        returnType = Boolean::class
                    }.hook {
                        intercept(true)
                    }
                    firstMethod {
                        parameters(Context::class)
                        returnType = Void.TYPE
                    }.hook {
                        before {
                            if (osCode >= 34) {
                                startMirageWindow(null)
                            } else {
                                val context = firstArg().get<Context>() ?: return@before
                                IntentUtils(context).startBackgroundRunServiceV14()
                            }
                            result = null
                        }
                    }
                }
            }
        }
    }
}