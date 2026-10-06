package com.luckyzyx.luckytool.hook.scopes.systemui

import android.view.View
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HideLockScreenStatusBarDisplay : YukiBaseHooker() {
    override fun onHook() {
        //Source KeyguardStatusBarView
        VariousClass(
            "com.android.systemui.statusbar.phone.KeyguardStatusBarView",  //C12.1 C13.1
            "com.android.systemui.statusbar.phone.KeyguardStatusBarView"  //C14
        ).toClass().resolve().apply {
            firstMethod { name = "setVisibility" }.hook {
                before {
                    firstArg().set(View.INVISIBLE)
                }
            }
        }
    }
}