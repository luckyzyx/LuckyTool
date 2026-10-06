package com.luckyzyx.luckytool.hook.scopes.weather

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.core.YukiHookCreator
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.data.AppVerInfo
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType

@Obfuscate
class WeatherAdsAndJumpBrowser(
    private val appVer: AppVerInfo?, val dexKitBridge: DexKitBridge
) : YukiBaseHooker() {
    override fun onHook() {
        val isNew = appVer?.versionCode?.let { it >= 13000000 } ?: return
        if (isNew) loadHooker(HookWeatherAdsAndJump)
        else loadHooker(HookWeatherAdsAndJumpC12(dexKitBridge))
    }

    @Obfuscate
    object HookWeatherAdsAndJump : YukiBaseHooker() {
        private const val weatherWrapper = "com.oplus.weather.main.model.WeatherWrapper"
//        private const val BrowserCommonUtils = "com.oplus.weather.plugin.webview.BrowserCommonUtils"
        override fun onHook() {
            val removeAds =
                preferences(ModulePrefs).getBoolean("remove_weather_some_page_bottom_ads", false)
            val disableJump = preferences(ModulePrefs).getBoolean("disable_weather_jump_browser", false)
            if (!removeAds && !disableJump) return

            //Source OPPOFeedAdManager switchesPopularRecommended
            "com.oplus.weather.ad.OPPOFeedAdManager".toClassOrNull()?.resolve()?.apply {
                firstMethod { name = "hasOpenPopularRecommended" }.hook {
                    if (removeAds) {
                        intercept(false)
                    }
                }
                firstMethod { name = "hasOpenAdSdkShowBannerFromNetwork" }.hook {
                    if (removeAds) {
                        intercept(false)
                    }
                }
            }

            //Source AppFeatureUtils
            "com.oplus.weather.utils.AppFeatureUtils".toClassOrNull()?.resolve()?.apply {
                firstMethod { name = "isSupportOplusAd" }.hook {
                    if (removeAds) {
                        intercept(false)
                    }
                }
            }

            //Source LocalUtils
            "com.oplus.weather.utils.LocalUtils".toClass().resolve().apply {
                method { name { it.contains("jumpToBrowser") } }.hookAll {
                    hookBefore(removeAds, disableJump)
                }
                method { name = "startBrowserForUrl" }.hookAll {
                    hookBefore(removeAds, disableJump)
                }
                firstMethodOrNull { name = "isBrowserSupportJump" }?.hook {
                    intercept(false)
                }
                firstMethod { name = "getH5StringBuffer" }.hook {
                    after {
                        val stringBuffer = result<StringBuffer>() ?: return@after
                        val url = formatWeatherUrl(stringBuffer.toString())
                        result = StringBuffer(url)
                    }
                }
            }

            //Source NoticeItem C14
            "com.oplus.weather.main.view.itemview.NoticeItem".toClassOrNull()?.resolve()?.apply {
                firstMethod { name = "showRainfallPanel" }.hook {
                    before {
                        if (!disableJump) return@before
                        val wrapper =
                            firstField { type = weatherWrapper }.of(instance).get() ?: return@before
                        wrapper.asResolver().firstMethod { name = "setRainFallAdLink" }.invoke("")
                    }
                }
                firstMethod { name = "showWarnWeatherPanel" }.hook {
                    before {
                        if (!disableJump) return@before
                        val warnInfo = lastArg().get() ?: return@before
                        warnInfo.asResolver().firstField { name = "addLink" }.set("")
                    }
                }
            }

            //Source SecondaryPageUtil
            "com.oplus.weather.utils.SecondaryPageUtil".toClassOrNull()?.resolve()?.apply {
                firstMethod { name = "newLink" }.hook {
                    after {
                        if (removeAds) result = formatWeatherUrl(
                            result<String>() ?: return@after
                        )
                    }
                }
                method {
                    name { it.startsWith("jump") && it.contains("Browser") }
                    parameters { it.contains(classOf<String>()) && it.contains(classOf<Boolean>()) }
                    returnType { it == classOf<Intent>() || it == classOf<Any>() }
                }.hookAll {
                    after {
                        val intent = result<Intent>() ?: return@after
                        intent.data = formatWeatherUrl(intent.data.toString()).toUri()
                    }
                }
            }

            //Source RainReminder -> Channel com.oplus.weather.service.rain
            "com.oplus.weather.service.service.RainReminder".toClass().resolve().apply {
                firstMethod { name = "createIntentOpenWeatherMainActivity" }.hook {
                    before {
                        if (disableJump) lastArg().set("")
                    }
                }
            }
            //Source WarnReminder -> Channel oppo.oplus.weather.warnWeather
            "com.oplus.weather.service.service.WarnReminder".toClass().resolve().apply {
                firstMethod { name = "getWarnWeatherIntent" }.hook {
                    before {
                        if (disableJump) lastArg().set("")
                    }
                }
            }
            //Source MorningReminder -> Channel oppo.oplus.weather.morningWeather C13.1
            "com.oplus.weather.morning.MorningReminder".toClassOrNull()?.resolve()?.apply {
                firstMethod {
                    parameters(weatherWrapper, Context::class)
                    returnType(PendingIntent::class)
                }.hook {
                    before {
                        if (disableJump) result = null
                    }
                }
            }
        }

        private fun YukiHookCreator.ClassicMemberHooker.hookBefore(
            removeAds: Boolean, disableJump: Boolean
        ) {
            before {
                val context = (args.find { it is Context } ?: return@before) as Context
                val type = arg(args.indexOfFirst { it is Int }).get<Int>() ?: 0

//                var url = arg(2).get<String>() ?: ""
//                val statisticsTag = arg(3).get<String>() ?: ""
                val urlIndex = args.indexOfFirst {
                    it is String && (it.contains("http") || it.contains("://"))
                }
                val url = arg(urlIndex).get<String>() ?: ""
                val tagIndex = args.indexOfFirst {
                    it is String && (!it.contains("http") && !it.contains("://"))
                }
                val statisticsTag = arg(tagIndex).get<String>() ?: ""

                //CCTV
                if (url.startsWith("heytapbrowser://")) return@before

                if (removeAds) arg(urlIndex).set(formatWeatherUrl(url))
                if (disableJump) {
                    val newUrl = arg(urlIndex).get<String>() ?: ""
                    startWebActivity(type, context, newUrl, statisticsTag)
                    result = null
                }
            }
        }
    }

    @Obfuscate
    class HookWeatherAdsAndJumpC12(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
        private var startWebView = ""
        override fun onHook() {
            val removeAds =
                preferences(ModulePrefs).getBoolean("remove_weather_some_page_bottom_ads", false)
            val disableJump =
                preferences(ModulePrefs).getBoolean("disable_weather_jump_browser", false)
            if (!removeAds && !disableJump) return

            //Source OppoUtils
            dexKitBridge.findClass {
                matcher {
                    fields {
                        addForType(classOf<Boolean>())
                        addForType("java.util.regex.Pattern")
                    }
                    methods {
                        add {
                            paramTypes(
                                classOf<Int>(), classOf<Context>(),
                                classOf<String>(), classOf<String>(), classOf<Boolean>(),
                                classOf<Boolean>()
                            )
                            returnType(Void.TYPE)
                            usingStrings(
                                "OppoUtils", "frontCode", "infoEnable", "fromWeatherApp"
                            )
                        }
                        add {
                            paramTypes(
                                classOf<Context>(), classOf<Int>(),
                                classOf<String>(), classOf<String>(), classOf<Boolean>()
                            )
                            returnType(Void.TYPE)
                            usingStrings(
                                "com.heytap.browser",
                                "com.android.browser",
                                "com.coloros.browser"
                            )
                        }

                    }
                }
            }.apply {
                checkDataList("HookWeatherAdsAndJumpC12 OppoUtils")
                single().name.toClass().resolve().apply {
                    method {
                        parameters(
                            Int::class,
                            Context::class,
                            String::class,
                            String::class,
                            Boolean::class,
                            Boolean::class
                        )
                        returnType(Void.TYPE)
                    }.hookAll {
                        hookBefore(removeAds, disableJump)
                    }
                    method {
                        parameters(
                            Context::class, Int::class, String::class, String::class, Boolean::class
                        )
                        returnType(Void.TYPE)
                    }.hookAll {
                        hookBefore(removeAds, disableJump)
                    }
                }
            }
            dexKitBridge.findClass {
                matcher {
                    className("com.coloros.weather.plugin.webview", StringMatchType.StartsWith)
                }
            }.findMethod {
                matcher {
                    paramCount(5)
                    returnType(Void.TYPE)
                    usingNumbers(536870912)
                    usingStrings(
                        "context",
                        "url",
                        "statisticsTag",
                        "intent_params_url",
                        "intent_params_isFirst",
                        "intent_params_statistics"
                    )
                }
            }.apply {
                checkDataList("HookWeatherAdsAndJumpC12 BrowserCommonUtils")
                startWebView = single().className
            }
        }

        private fun YukiHookCreator.ClassicMemberHooker.hookBefore(
            removeAds: Boolean, disableJump: Boolean
        ) {
            before {
                if (startWebView.isBlank()) return@before
                val context = (args.find { it is Context } ?: return@before) as Context
//                val url = arg(2).get<String>() ?: ""
//                val statisticsTag = arg(3).get<String>() ?: ""

                val urlIndex = args.indexOfFirst {
                    it is String && (it.contains("http") || it.contains("://"))
                }
                val url = arg(urlIndex).get<String>() ?: ""
                val tagIndex = args.indexOfFirst {
                    it is String && (!it.contains("http") && !it.contains("://"))
                }
                val statisticsTag = arg(tagIndex).get<String>() ?: ""

                //CCTV
                if (url.startsWith("heytapbrowser://")) return@before

                if (removeAds) arg(urlIndex).set(formatWeatherUrl(url))
                if (disableJump) {
                    val newUrl = arg(urlIndex).get<String>() ?: ""
                    startWebActivity(startWebView.toClass(), context, newUrl, statisticsTag)
                    result = null
                }
            }
        }
    }

    companion object {
        private fun getWeatherIntent(
            action: String, browser: Int, url: String, statisticsTag: String
        ): Intent {
            return Intent("com.heytap.browser.action.DETAIL_PAGE").apply {
                if (action.isNotBlank()) setAction(action)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
//                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
                data = url.toUri()
                when (browser) {
                    1 -> setPackage("com.android.browser")
                    2 -> setPackage("com.heytap.browser")
                    3 -> setPackage("com.coloros.browser")
                }
                putExtra("clickTime", System.currentTimeMillis())
                putExtra("clickType", "weather")
                putExtra("intent_params_url", url)
                putExtra("intent_params_isFirst", true)
                putExtra("intent_params_statistics", statisticsTag)
            }
        }

        fun startWebActivity(browser: Int, context: Context, url: String, statisticsTag: String) {
            //Source BrowserCommonUtils -> startWeatherWebActivity
//            clazz.method { paramCount = 5 }.get().call(context, url, true, statisticsTag, true)
            var intent = getWeatherIntent("", browser, url, statisticsTag)
            try {
                context.startActivity(intent)
                return
            } catch (_: Throwable) {
                intent = getWeatherIntent(Intent.ACTION_VIEW, browser, url, statisticsTag)
                try {
                    context.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    startWebActivity(browser - 1, context, url, statisticsTag)
                }
                return
            }
        }

        fun startWebActivity(
            clazz: Class<*>, context: Context, url: String, statisticsTag: String
        ) {
            //Source BrowserCommonUtils -> startWeatherWebActivity
            clazz.resolve().firstMethod { parameterCount = 5 }
                .invoke(context, url, true, statisticsTag, true)
        }

        fun formatWeatherUrl(url: String): String {
            if (url.isBlank()) return url
            var cacheUrl = url
            if (cacheUrl.contains("fromWeatherApp=true")) cacheUrl = cacheUrl.replace(
                "fromWeatherApp=true", "fromWeatherApp=false"
            )
            if (cacheUrl.contains("infoEnable=true")) cacheUrl = cacheUrl.replace(
                "infoEnable=true", "infoEnable=false"
            )
            if (cacheUrl.contains("infoEnable").not()) {
                if (cacheUrl.lastOrNull() != '&') cacheUrl += "&"
                cacheUrl += "infoEnable=false"
            }
//            YLog.debug("url -> $cacheUrl")
            return cacheUrl
        }
    }
}