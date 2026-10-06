package com.luckyzyx.luckytool.hook.scopes.wirelesssettings

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.wifi.WifiManager
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.utils.preferences.PreferenceReflections
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.formatStringAuto
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge
import java.net.Inet4Address
import java.net.Inet6Address

@Obfuscate
class EnableWifiDetailsDisplayGateway(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        //Source WifiAddressController
        dexKitBridge.findClass {
            matcher {
                methods {
                    add { name("displayPreference") }
                    add { name("onPause") }
                    add { name("onResume") }
                }
                usingStrings("WifiAddressController")
            }
        }.apply {
            checkDataList("WifiAddressController")
            findMethod {
                matcher {
                    paramCount(0)
                    usingStrings("updateIpInfo")
                }
            }.apply {
                checkDataList("updateIpInfo")
                single().className.toClass().resolve().apply {
                    firstMethod {
                        name = single().methodName
                        emptyParameters()
                    }.hook {
                        after {
                            val context = firstField { type = Context::class }.of(instance)
                                .get<Context>() ?: return@after
                            val preferenceScreen = firstField {
                                type = "androidx.preference.PreferenceScreen"
                                superclass()
                            }.of(instance).get() ?: return@after

                            val connectivityManager =
                                context.getSystemService(classOf<ConnectivityManager>())
                            val wifiManager =
                                context.applicationContext.getSystemService(classOf<WifiManager>())

                            val getCurrentNetwork = wifiManager.asResolver().firstMethod {
                                name = "getCurrentNetwork"
                                emptyParameters()
                                returnType = classOf<Network>()
                            }.invoke<Network>() ?: return@after

                            @SuppressLint("MissingPermission")
                            val linkProperties = connectivityManager
                                .getLinkProperties(getCurrentNetwork) ?: return@after

                            var ipv4Gateway = ""
                            var ipv6Gateway = ""

                            linkProperties.routes.forEachIndexed { _, routeInfo ->
                                if (routeInfo.isDefaultRoute) {
                                    val gateway = routeInfo.gateway ?: return@forEachIndexed
                                    val hostAddress = gateway.hostAddress ?: return@forEachIndexed
                                    if (routeInfo.destination.address is Inet4Address) {
                                        ipv4Gateway = hostAddress
                                    }
                                    if (routeInfo.destination.address is Inet6Address) {
                                        ipv6Gateway = hostAddress
                                    }
                                }
                            }

                            val ipv4 = PreferenceReflections.findPreference(
                                preferenceScreen, "current_ipv4_address"
                            ) ?: return@after
                            if (PreferenceReflections.isVisible(ipv4)) {
                                val summary = PreferenceReflections.getSummary(ipv4)
                                PreferenceReflections.setSummary(
                                    ipv4, formatStringAuto(
                                        arrayListOf(summary, "", ipv4Gateway), "\n"
                                    )
                                )
                            }

                            val ipv6 = PreferenceReflections.findPreference(
                                preferenceScreen, "current_ipv6_address"
                            ) ?: return@after
                            if (PreferenceReflections.isVisible(ipv6)) {
                                val summary = PreferenceReflections.getSummary(ipv6)
                                PreferenceReflections.setSummary(
                                    ipv6, formatStringAuto(
                                        arrayListOf(summary, "", ipv6Gateway), "\n"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}