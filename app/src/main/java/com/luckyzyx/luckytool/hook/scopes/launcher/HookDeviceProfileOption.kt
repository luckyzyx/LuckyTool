package com.luckyzyx.luckytool.hook.scopes.launcher

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookDeviceProfileOption : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode

        val enableFolder = preferences(ModulePrefs).getBoolean("enable_folder_layout_adjustment", false)
        val folderRow = preferences(ModulePrefs).getInt("set_icon_rows_in_folder", 4)
        val folderColumn = preferences(ModulePrefs).getInt("set_icon_columns_in_folder", 3)
        val syncPreview =
            preferences(ModulePrefs).getBoolean("sync_folder_icon_column_number_preview", false)

        val enableDrawer = preferences(ModulePrefs).getBoolean("enable_drawer_layout_adjustment", false)
        val drawerColumn = preferences(ModulePrefs).getInt("set_icon_columns_in_drawer", 4)

        //Source InvariantDeviceProfile
        "com.android.launcher3.InvariantDeviceProfile".toClass().resolve().apply {
            method { name = "initGrid" }.hookAll {
                after {
                    if (enableFolder) {
                        firstField { name = "numFolderRows" }.of(instance).set(folderRow)
                        firstField { name = "numFolderColumns" }.of(instance).set(folderColumn)
                    }
                    if (enableDrawer) {
                        firstField { name = "numAllAppsColumns" }.of(instance).set(drawerColumn)
                    }
                }
            }
        }

        //Source InvariantDeviceProfile GridOption
        "com.android.launcher3.InvariantDeviceProfile\$GridOption".toClass().resolve().apply {
            firstConstructor { parameterCount { it in 2..3 } }.hook {
                after {
                    if (enableFolder) {
                        firstField { name = "numFolderRows" }.of(instance).set(folderRow)
                        firstField { name = "numFolderColumns" }.of(instance).set(folderColumn)
                        if (syncPreview && folderColumn > 3) {
                            firstField { name = "numFolderPreview" }.of(instance).set(folderColumn)
                        }
                    }
                    if (enableDrawer) {
                        firstField { name = "numAllAppsColumns" }.of(instance).set(drawerColumn)
                    }
                }
            }
        }

        //Source OplusInvariantDeviceProfile
        "com.android.launcher3.OplusInvariantDeviceProfile".toClass().resolve().apply {
            (if (osCode >= 40) method { name { it.startsWith("initGrid") } }
            else method { name { it.startsWith("injectInitGrid") } }).hookAll {
                after {
                    if (enableFolder) {
//                        field { name = "numFolderRows" }.get(instance).set(3)
                        (firstFieldOrNull { name = "numFolderRows" } ?: firstField {
                            name = "numFolderRows"; superclass()
                        }).of(instance).set(folderRow)
                        (firstFieldOrNull { name = "numFolderColumns" } ?: firstField {
                            name = "numFolderColumns"; superclass()
                        }).of(instance).set(folderColumn)
                        if (syncPreview && folderColumn > 3) {
                            firstField { name = "numFolderPreview" }.of(instance).set(folderColumn)
                        }
                    }
                    if (enableDrawer) {
                        firstField { name = "numAllAppsColumns"; superclass() }.of(instance)
                            .set(drawerColumn)
                    }
                }
            }
        }

        //Source FolderInfo
        "com.android.launcher3.model.data.FolderInfo".toClass().resolve().apply {
            firstMethodOrNull { name = "getPreviewRow" }?.hook {
                before {
                    if (!(enableFolder && syncPreview)) return@before
                    val spanX = firstField { name = "spanX"; superclass() }.of(instance).get<Int>()
                    val spanY = firstField { name = "spanY"; superclass() }.of(instance).get<Int>()
                    if (spanX == 1 && spanY == 1) result = folderRow
                }
            }
            firstMethodOrNull { name = "getPreviewColumn" }?.hook {
                before {
                    if (!(enableFolder && syncPreview)) return@before
                    val spanX = firstField { name = "spanX"; superclass() }.of(instance).get<Int>()
                    val spanY = firstField { name = "spanY"; superclass() }.of(instance).get<Int>()
                    if (spanX == 1 && spanY == 1) result = folderColumn
                }
            }
        }

        //Source AllAppsParam
        "com.android.launcher.layoutparam.AllAppsParam".toClassOrNull()?.resolve()?.apply {
            firstMethod { name = "getNumAllAppsColumns" }.hook {
                before {
                    if (enableDrawer) result = drawerColumn
                }
            }
        }
    }
}