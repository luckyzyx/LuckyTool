package com.luckyzyx.luckytool.hook.scopes.launcher

import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object DisableLongPressAppIconSecondaryMenu : YukiBaseHooker() {
    override fun onHook() {
        //Source OplusPopupContainerWithArrow -> PopupDataProvider
        "com.android.launcher3.popup.PopupDataProvider".toClass().resolve().apply {
            firstMethod {
                name = "getNotificationKeysForItem"
                returnType = List::class
            }.hook {
                before {
                    val itemInfo = firstArg().get() ?: return@before
                    itemInfo.asResolver().firstField {
                        name = "mAddShortcutCount"; superclass()
                    }.set(0)
                }
            }
        }
    }
}