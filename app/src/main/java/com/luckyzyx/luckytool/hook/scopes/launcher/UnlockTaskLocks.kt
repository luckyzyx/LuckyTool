package com.luckyzyx.luckytool.hook.scopes.launcher

import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.SDK
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class UnlockTaskLocks(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        if (SDK < A14) {
            //Source ColorLockManager OplusLockManager (C13及以下)
            VariousClass(
                "com.coloros.quickstep.applock.ColorLockManager",
                "com.oplus.quickstep.applock.OplusLockManager"
            ).toClass().resolve().apply {
                firstConstructor { parameters(Context::class) }.hook {
                    after {
                        firstField { name = "mLockAppLimit" }.of(instance).set(999)
                    }
                }
            }
            return
        }

        //Source IAppLockDataHandler实现 (C14+, C14-C16为AppLockModel, C17为混淆后的同名类)
        //C14+统一通过接口方法名定位: 类名与字段名可能被混淆, 但接口方法名可读,
        //字段名由DexKit返回真实dex名, 同一套逻辑覆盖C14至C17
        dexKitBridge.findClass {
            matcher {
                addMethod { name("updateNoDefaultLockAppLimit") }
                addMethod { name("initData") }
                addFieldForType(classOf<Int>())
            }
        }.apply {
            checkDataList("UnlockTaskLocks AppLockDataHandler")

            findField {
                matcher {
                    type(classOf<Int>())
                    addWriteMethod { name("updateNoDefaultLockAppLimit") }
                }
            }.apply {
                checkDataList("UnlockTaskLocks noDefaultLockAppLimit")

                single().className.toClass().resolve().apply {
                    firstMethod { name = "initData"; emptyParameters() }.hook {
                        after {
                            firstField { name = single().fieldName }.of(instance).set(999)
                        }
                    }
                    firstMethod {
                        name = "updateNoDefaultLockAppLimit"; parameters(Int::class)
                    }.hook {
                        after {
                            firstField { name = single().fieldName }.of(instance).set(999)
                        }
                    }
                }
            }
        }
    }
}
