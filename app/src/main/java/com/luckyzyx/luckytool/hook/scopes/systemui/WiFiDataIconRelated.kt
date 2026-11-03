package com.luckyzyx.luckytool.hook.scopes.systemui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.ScanResult
import android.net.wifi.WifiInfo
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import com.highcapable.betterandroid.ui.extension.view.textToString
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.hook.utils.sysui.AbsSettingsValueProxyUtils
import com.luckyzyx.luckytool.hook.utils.sysui.WifiStandardIconDrawable
import com.luckyzyx.luckytool.hook.utils.sysui.WifiUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate
import java.lang.ref.WeakReference

@Obfuscate
object WiFiDataIconRelated : YukiBaseHooker() {
    override fun onHook() {
        when (getOSVersionCode) {
            in 34..Int.MAX_VALUE -> loadHooker(WiFiDataIcon)
            else -> loadHooker(WiFiDataIconV14)
        }
    }

    @Obfuscate
    object WiFiDataIcon : YukiBaseHooker() {

        private const val WIFI_LEFT_ID_NAME = "wifi_left"
        private const val WIFI_LEFT_TV_ID_NAME = "wifi_left_tv"
        private const val TECHNICAL_ICON_SWITCH = "wifi_use_technical_standard_icons_switch_on"
        private const val ICON_WIDTH_DP = 22f
        private const val ICON_HEIGHT_DP = 17f
        private const val STANDARD_GATE_CACHE_MS = 3000L

        @Volatile
        private var wifiStandard = ScanResult.WIFI_STANDARD_UNKNOWN

        @Volatile
        private var wifiConnected = false

        @Volatile
        private var callbackRegistered = false

        @Volatile
        private var wifiLeftId = 0

        @Volatile
        private var wifiLeftIdResolved = false

        @Volatile
        private var wifiLeftTvId = 0

        @Volatile
        private var wifiLeftTvIdResolved = false

        // null 表示还没观察到宿主用哪个槽位渲染，默认按 C17 主题的文本槽处理
        @Volatile
        private var hostTextMode: Boolean? = null

        // 自己写进 wifi_left_tv 的文本，断开连接时要原样收掉（宿主此时不会再发一次）
        @Volatile
        private var injectedText: String? = null

        // 记住已经挂过布局监听的容器，状态栏重建时容器会换，换一次挂一次
        private var watchedContainer: WeakReference<View>? = null

        // 双 WiFi / PassPoint / 系统技术标准图标开关的判定要走反射和设置查询，按时间窗口缓存结果
        @Volatile
        private var gateCheckedAt = 0L

        @Volatile
        private var gateAllowed = false

        // 标准号是异步拿到的，拿到后补画一次，避免宿主只贴一次图时错过
        private var lastWifiLeftView: WeakReference<ImageView>? = null

        private var lastWifiLeftTv: WeakReference<TextView>? = null

        private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

        override fun onHook() {
            val forceStandard =
                preferences(ModulePrefs).getBoolean("force_display_wifi_standard", false)

            if (!forceStandard) return

            //Source OplusWifiSignalExImpl
            // 宿主只给 11AX(6)/11BE(8) 赋值：技术标准图标开时 wifiLeftResId 给 stat_signal_wifi_6/7，
            // 或 O17 主题下 wifiLeftText 给 "6"/"7"。11N(4)/11AC(5) 两条路径都返回空，两个槽位一并隐藏，
            // 所以这两个标准要在宿主每次摆完槽位之后自己补上
            runCatching {
                "com.oplus.systemui.statusbar.pipeline.OplusWifiSignalExImpl".toClass().resolve()
                    .firstMethod { name = "bindEx"; parameterCount = 3 }.intercept {
                        val result = proceed()
                        arg<ViewGroup>(0)?.let { captureLeftViews(it) }
                        result
                    }
            }.onFailure { YLog.warn("wifi left bindEx hook failed: " + it.message) }
        }

        private fun captureLeftViews(viewGroup: ViewGroup) {
            val context = viewGroup.context ?: return
            val leftId = resolveWifiLeftId(context)
            if (leftId != 0) viewGroup.findViewById<ImageView>(leftId)?.let {
                lastWifiLeftView = WeakReference(it)
            }
            val leftTvId = resolveWifiLeftTvId(context)
            if (leftTvId != 0) viewGroup.findViewById<TextView>(leftTvId)?.let {
                lastWifiLeftTv = WeakReference(it)
            }
            ensureWifiCallback(context)
            if (watchedContainer?.get() !== viewGroup) {
                watchedContainer = WeakReference(viewGroup)
                // 只在状态栏真正重新布局时才看一眼槽位，不挂 framework 的 setter
                viewGroup.viewTreeObserver.addOnGlobalLayoutListener { onLeftSlotLaidOut() }
            }
            onLeftSlotLaidOut()
        }

        //宿主主题决定用哪个槽位：默认主题走 wifi_left_tv 文本，只有缺 O17 信号图的三方主题才走 wifi_left 图标。
        //两个槽位同时被收起，就是「这个标准宿主不管」，这时才轮到我们补画
        private fun onLeftSlotLaidOut() {
            val textView = lastWifiLeftTv?.get()
            val imageView = lastWifiLeftView?.get()
            if (textView == null && imageView == null) return
            if (textView?.isVisible == true) {
                hostTextMode = true
                return
            }
            if (imageView?.isVisible == true) {
                hostTextMode = false
                return
            }
            applyLowStandardDigit()
        }

        //11N/11AC 宿主不会给任何槽位赋值，按宿主当前用的槽位把标准号补上
        private fun applyLowStandardDigit() {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                mainHandler.post { applyLowStandardDigit() }
                return
            }
            val digit = lowStandardDigitOf(wifiStandard) ?: return
            if (!wifiConnected) return
            if (hostTextMode != false) {
                val textView = lastWifiLeftTv?.get() ?: return
                if (!isStandardIconEnabled(textView.context)) return
                val text = digit.toString()
                injectedText = text
                textView.text = text
                if (textView.visibility != View.VISIBLE) textView.visibility = View.VISIBLE
            } else {
                val imageView = lastWifiLeftView?.get() ?: return
                if (!isStandardIconEnabled(imageView.context)) return
                imageView.setImageDrawable(buildDigitDrawable(imageView.context, digit))
                if (imageView.visibility != View.VISIBLE) imageView.setVisibility(View.VISIBLE)
            }
        }

        //断开后宿主不会再发一次（resId 和 text 都没变化），自己塞进去的数字要自己收掉
        private fun clearLowDigit() {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                mainHandler.post { clearLowDigit() }
                return
            }
            val injected = injectedText
            if (injected != null) {
                injectedText = null
                val textView = lastWifiLeftTv?.get()
                if (textView != null && textView.textToString() == injected) {
                    textView.text = ""
                    textView.visibility = View.GONE
                }
            }
            val imageView = lastWifiLeftView?.get() ?: return
            if (lowStandardDigitOf(wifiStandard) == null && imageView.drawable is WifiStandardIconDrawable) {
                imageView.setVisibility(View.GONE)
            }
        }

        @SuppressLint("DiscouragedApi")
        private fun resolveWifiLeftId(context: Context): Int {
            if (wifiLeftIdResolved) return wifiLeftId
            wifiLeftIdResolved = true
            wifiLeftId = context.resources.getIdentifier(WIFI_LEFT_ID_NAME, "id", packageName)
            if (wifiLeftId == 0) YLog.warn("wifi_left not found in $packageName resources")
            return wifiLeftId
        }

        @SuppressLint("DiscouragedApi")
        private fun resolveWifiLeftTvId(context: Context): Int {
            if (wifiLeftTvIdResolved) return wifiLeftTvId
            wifiLeftTvIdResolved = true
            wifiLeftTvId =
                context.resources.getIdentifier(WIFI_LEFT_TV_ID_NAME, "id", packageName)
            if (wifiLeftTvId == 0) YLog.warn("wifi_left_tv not found in $packageName resources")
            return wifiLeftTvId
        }

        //宿主自己会给 11AX/11BE 出数字（还会出 "6E"），这里只补宿主不管的 11N/11AC
        private fun lowStandardDigitOf(standard: Int): Char? = when (standard) {
            ScanResult.WIFI_STANDARD_11N -> '4'
            ScanResult.WIFI_STANDARD_11AC -> '5'
            else -> null
        }

        private fun buildDigitDrawable(context: Context, digit: Char): Drawable {
            val density = context.resources.displayMetrics.density
            return WifiStandardIconDrawable(
                digit = digit,
                widthPx = (ICON_WIDTH_DP * density).toInt(),
                heightPx = (ICON_HEIGHT_DP * density).toInt()
            )
        }

        private fun isStandardIconEnabled(context: Context): Boolean {
            val now = SystemClock.elapsedRealtime()
            if (now - gateCheckedAt < STANDARD_GATE_CACHE_MS) return gateAllowed
            gateCheckedAt = now
            gateAllowed = runCatching {
                val wifiUtils = WifiUtils(hostClassLoader)
                if (wifiUtils.isDualWifiConnected(context) || wifiUtils.isPassPointAp(context)) {
                    return@runCatching false
                }
                AbsSettingsValueProxyUtils(hostClassLoader!!)
                    .getGlobalIntValue(context, TECHNICAL_ICON_SWITCH, 0) == 1
            }.getOrDefault(false)
            return gateAllowed
        }

        @SuppressLint("MissingPermission")
        private fun ensureWifiCallback(context: Context) {
            if (callbackRegistered) return
            callbackRegistered = true
            runCatching {
                val manager = (context.applicationContext ?: context)
                    .getSystemService(classOf<ConnectivityManager>()) ?: return
                val request = NetworkRequest.Builder().apply {
                    addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                }.build()
                manager.registerNetworkCallback(
                    request,
                    object : ConnectivityManager.NetworkCallback() {
                        override fun onCapabilitiesChanged(
                            network: Network, networkCapabilities: NetworkCapabilities
                        ) {
                            val info = networkCapabilities.transportInfo as? WifiInfo ?: return
                            val isPrimary = info.asResolver().firstMethod {
                                name = "isPrimary"; emptyParameters()
                            }.invoke<Boolean>() ?: false
                            if (!isPrimary) return
                            wifiConnected = true
                            onWifiStandardChanged(info.wifiStandard)
                        }

                        override fun onLost(network: Network) {
                            wifiConnected = false
                            wifiStandard = ScanResult.WIFI_STANDARD_UNKNOWN
                            clearLowDigit()
                        }
                    },
                    mainHandler
                )
            }.onFailure { YLog.error("register wifi network callback failed", it) }
        }

        private fun onWifiStandardChanged(standard: Int) {
            if (wifiStandard != standard) wifiStandard = standard
            applyLowStandardDigit()
        }
    }

    @Obfuscate
    object WiFiDataIconV14 : YukiBaseHooker() {
        override fun onHook() {
            val removeInout = preferences(ModulePrefs).getBoolean("remove_wifi_data_inout", false)

            //Source OplusStatusBarWifiView
            VariousClass(
                "com.oplusos.systemui.statusbar.OplusStatusBarWifiView",
                "com.oplus.systemui.statusbar.phone.signal.OplusStatusBarWifiViewExImpl"
            ).toClass().resolve().apply {
                firstMethod { name = "initViewState" }.hook {
                    after {
                        if (!removeInout) return@after
                        firstField { name = "mWifiActivity" }.of(instance).get<View>()?.isVisible =
                            false
                    }
                }
                firstMethod { name = "updateState" }.hook {
                    after {
                        if (!removeInout) return@after
                        firstField { name = "mWifiActivity" }.of(instance).get<View>()?.isVisible =
                            false
                    }
                }
            }
        }
    }
}
