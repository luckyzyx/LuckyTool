package com.luckyzyx.luckytool.hook.scopes.packageinstaller

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.KavaRef.MemberScope
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate
import java.security.SecureRandom

@Obfuscate
object FixInstallButtonDisplayException : YukiBaseHooker() {
    override fun onHook() {
        //Source ConfusedButton
        "com.android.packageinstaller.oplus.view.ConfusedButton".toClass().resolve().apply {
            firstMethod { name = "getAccessibilityViewId" }.hook {
                before { resetConfusedState(instance) }
            }
            firstMethodOrNull { name = "getText" }?.hook {
                before { resetConfusedState(instance) }
            }
        }
        //Source ConfusedTextView
        "com.android.packageinstaller.oplus.view.ConfusedTextView".toClass().resolve().apply {
            firstMethod { name = "getAccessibilityViewId" }.hook {
                before { resetConfusedState(instance) }
            }
            firstMethodOrNull { name = "getText" }?.hook {
                before { resetConfusedState(instance) }
            }
        }
    }

    /**
     * 复位视图的 cts 状态：把 cts 置回 true 并换一个全新的随机数，
     * 避免安装页底部按钮因异常状态而显示不出来。
     */
    private fun MemberScope<Any>.resetConfusedState(instance: Any) {
        firstMethod { name = "setCts" }.of(instance).invoke(true)
        firstField { type = SecureRandom::class }.of(instance).set(SecureRandom())
    }
}