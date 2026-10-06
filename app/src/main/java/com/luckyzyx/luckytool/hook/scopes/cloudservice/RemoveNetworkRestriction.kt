package com.luckyzyx.luckytool.hook.scopes.cloudservice

import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class RemoveNetworkRestriction(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        //Source BackUpActivity / BackupRestoreHelper -> backup_currently_mobile
        //Source BackupRestoreCheckerUtils -> check -> ? 2 : 0
        //Source RestoreCheck / WifiCheck -> check -> BackupRestoreCode -> NO_WIFI / SUCCESS
        //Source NetworkUtil -> 2 == ?() -> getSystemService -> connectivity
        //Search Const.Callback.NetworkState.NetworkType.NETWORK_MOBILE -> ? 1 : 0 -> Method
        dexKitBridge.findClass {
            matcher {
                methods {
                    add {
                        paramCount(0)
                        returnType(classOf<Int>())
                        usingStrings("connectivity")
                        usingNumbers(0, 1, 2)
                    }
                    add {
                    paramTypes(classOf<Int>())
                        returnType(classOf<Boolean>())
                    }
                    add {
                    paramTypes(classOf<Context>())
                        returnType(classOf<Boolean>())
                        usingStrings("NetworkUtil", "connectivity", "isMobileDataNetwork")
                    }
                    add {
                    paramTypes(classOf<Context>())
                        returnType(classOf<Boolean>())
                        usingStrings("NetworkUtil", "connectivity", "isNetworkConnected")
                    }
                }
            }
        }.apply {
            checkDataList("RemoveNetworkRestriction")
            single().name.toClass().resolve().apply {
                method { emptyParameters();returnType = Int::class }.hookAll {
                    after { if (result<Int>() == 1) result = 2 }
                }
            }
        }
    }
}