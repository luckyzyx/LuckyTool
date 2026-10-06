package com.luckyzyx.luckytool.hook.scopes.uiengine

import com.drake.net.Get
import com.drake.net.okhttp.trustSSLCertificate
import com.drake.net.utils.scopeNet
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import java.io.File

@Obfuscate
object EnableRandomTextOnAod : YukiBaseHooker() {
    override fun onHook() {
        val mode = preferences(ModulePrefs).getString("set_random_text_display_mode", "0")

        val customFile = preferences(ModulePrefs).getString("custom_random_text_file", "")
        val customApi = preferences(ModulePrefs).getString("custom_random_text_api", "")

        val yiyanTextArrayCache = ArrayList<String>()
        var yiyanTextCache = ""

        //Source AodRootLayout
        "com.oplus.aodimpl.AodRootLayout".toClass().resolve().apply {
            firstConstructor { parameterCount = 2 }.hook {
                before {
                    when (mode) {
                        "1" -> {
                            if (yiyanTextArrayCache.isEmpty()) {
                                if (customFile.isBlank()) return@before
                                val file = File(customFile)
                                if (file.exists().not()) return@before
                                yiyanTextArrayCache.addAll(file.readLines())
                            }
                        }

                        "2" -> {
                            if (customApi.isBlank()) return@before
                            if ((customApi.startsWith("http://") || customApi.startsWith("https://")).not()) return@before
                            scopeNet {
                                val text = Get<String>(customApi) {
                                    setClient {
                                        trustSSLCertificate()
                                    }
                                }.await()
                                if (text.isNotBlank()) yiyanTextCache = text
                            }.catch {
                                return@catch
                            }
                        }
                    }
                }
            }
            firstMethod { name = "getCustomView" }.hook {
                before {
                    val viewBean = firstArg().get() ?: return@before
                    val mViewType =
                        viewBean.asResolver().firstMethod { name = "getViewType" }.invoke<String>()
                    if (mViewType != "AodTextView") return@before

                    val mMethodBeanList =
                        viewBean.asResolver().firstField { name = "mMethodBeanList" }
                        .get<List<Any>>() ?: listOf()
                    val textMethod = mMethodBeanList.find {
                        it.asResolver().firstMethod { name = "getXmlAttribute" }
                            .invoke<String>() == "text" &&
                                it.asResolver().firstMethod { name = "getMethodName" }
                                    .invoke<String>() == "setText"
                    }
                    if (textMethod != null) {

                        when (mode) {
                            "1" -> {
                                if (yiyanTextArrayCache.isNotEmpty()) {
                                    textMethod.asResolver().firstMethod { name = "setXmlValue" }
                                        .invoke(yiyanTextArrayCache.shuffled().first())
                                    textMethod.asResolver().firstMethod { name = "setValue" }
                                        .invoke(yiyanTextArrayCache.shuffled().first())
                                }
                            }

                            "2" -> if (yiyanTextCache.isNotBlank()) {
                                textMethod.asResolver().firstMethod { name = "setXmlValue" }
                                    .invoke(yiyanTextCache)
                                textMethod.asResolver().firstMethod { name = "setValue" }
                                    .invoke(yiyanTextCache)
                            }
                        }
                    }
                }
            }
        }
    }
}