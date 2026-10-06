package com.luckyzyx.luckytool.hook.scopes.permissioncontroller

import android.app.Activity
import android.app.Application
import android.app.admin.DevicePolicyManager
import android.os.UserHandle
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class RemoveStoragePermissionExceptionDialog(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        //Source GrantPermissionsViewModel
        dexKitBridge.findClass {
            matcher {
            addFieldForType(classOf<Application>())
                addFieldForType(classOf<UserHandle>())
                addFieldForType(classOf<DevicePolicyManager>())
                usingStrings("GrantPermissionsViewModel")
            }
        }.apply {
            checkDataList("RemoveStoragePermissionExceptionDialog findClass")

            findMethod {
                matcher {
                    paramTypes(classOf<Activity>())
                    returnType(Void.TYPE)
                    usingStrings(
                        "oplus.intent.extra.PACKAGE_LABEL",
                        "oplus.intent.extra.GROUP_NAME",
                        "android.permission-group.STORAGE"
                    )
                }
            }.apply {
                checkDataList("RemoveStoragePermissionExceptionDialog findMethod")

                single().className.toClass().resolve().apply {
                    firstMethod {
                        name = single().name
                        parameters(Activity::class)
                        returnType = Void.TYPE
                    }.hook {
                        intercept()
                    }
                }
            }
        }
    }
}