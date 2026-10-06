package com.luckyzyx.luckytool.hook.scopes.calendar

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.core.view.isVisible
import com.highcapable.betterandroid.ui.extension.view.removeSelf
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveHoroscopePageInformationFlow : YukiBaseHooker() {
    @SuppressLint("DiscouragedApi")
    override fun onHook() {
        //Source HoroscopeFragment -> H5InterfaceHelper getHoroscopeUrl
        "com.android.calendar.module.subscription.horoscope.HoroscopeFragment".toClass().resolve()
            .apply {
                firstMethod { name = "onViewCreated"; parameterCount = 2 }.hook {
                    after {
                        val viewGroup = firstArg().get<ViewGroup>() ?: return@after
                        val res = viewGroup.resources
                        viewGroup.findViewById<View>(
                            res.getIdentifier(
                                "web_title_layout", "id",
                                this@RemoveHoroscopePageInformationFlow.packageName
                            )
                        )?.removeSelf()
                        viewGroup.findViewById<WebView>(
                            res.getIdentifier(
                                "webView", "id",
                                this@RemoveHoroscopePageInformationFlow.packageName
                            )
                        )?.removeSelf()
                        viewGroup.findViewById<View>(
                            res.getIdentifier(
                                "fb_to_top", "id",
                                this@RemoveHoroscopePageInformationFlow.packageName
                            )
                        )?.removeSelf()
                        viewGroup.findViewById<View>(
                            res.getIdentifier(
                                "tv_source", "id",
                                this@RemoveHoroscopePageInformationFlow.packageName
                            )
                        )?.isVisible = true
                    }
                }
            }
    }
}