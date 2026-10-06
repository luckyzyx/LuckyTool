package com.luckyzyx.luckytool.hook.scopes.launcher

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object CustomDesktopDefaultHomePage : YukiBaseHooker() {
    override fun onHook() {
        val page = preferences(ModulePrefs).getString("custom_desktop_default_home_page", "0")
        if (page.isBlank() || page.toIntOrNull() == null) return

        //Source Workspace
        "com.android.launcher3.Workspace".toClass().resolve().apply {
            firstMethod { name = "initWorkspace" }.hook {
                before {
                    firstField { name = "DEFAULT_PAGE" }.set(page.toIntOrNull() ?: 0)
                }
                after {
                    firstField { name = "mCurrentPage"; superclass() }.of(instance)
                        .set(page.toIntOrNull() ?: 0)
                }
            }
            firstMethod { name = "moveToDefaultScreen" }.hook {
                before {
                    firstField { name = "DEFAULT_PAGE" }.set(page.toIntOrNull() ?: 0)
                }
            }
        }
    }
}