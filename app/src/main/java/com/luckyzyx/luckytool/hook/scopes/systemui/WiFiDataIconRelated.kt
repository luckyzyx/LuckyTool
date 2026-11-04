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
import java.util.concurrent.CopyOnWriteArrayList

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

        // 当前用来取标准号的主 WiFi 网络：别的 WiFi 网络断开时不能连带清掉状态
        @Volatile
        private var wifiNetwork: Network? = null

        @Volatile
        private var wifiLeftId = 0

        @Volatile
        private var wifiLeftIdResolved = false

        @Volatile
        private var wifiLeftTvId = 0

        @Volatile
        private var wifiLeftTvIdResolved = false

        // 双 WiFi / PassPoint / 系统技术标准图标开关的判定要走反射和设置查询，按时间窗口缓存结果
        @Volatile
        private var gateCheckedAt = 0L

        @Volatile
        private var gateAllowed = false

        // 状态栏、锁屏、AOD 各自建一份 WiFi 视图并各自走一次 bindEx，每份都有一对 wifi_left/wifi_left_tv。
        // 只记住最后绑定的那一份时，指针经常落在不显示的那份容器上，补画进去必然看不见，所以每份都单独记录、单独补画
        private val slotSets = CopyOnWriteArrayList<LeftSlots>()

        // 已经挂过布局监听的容器：同一个容器只挂一次，容器换一次挂一次
        private val observedGroups = CopyOnWriteArrayList<WeakReference<ViewGroup>>()

        private val mainHandler by lazy { Handler(Looper.getMainLooper()) }

        // 一份容器上的两个槽位，以及这份容器自己的渲染状态，互不干扰
        private class LeftSlots(group: ViewGroup, text: TextView?, image: ImageView?) {
            private val groupRef = WeakReference(group)
            private val textRef = WeakReference(text)
            private val imageRef = WeakReference(image)

            // 宿主这份容器当前用哪个槽位渲染；null 表示还没观察到，按 C17 主题的文本槽处理
            @Volatile
            var textMode: Boolean? = null

            // 自己写进 wifi_left_tv 的文本，断开连接、换成宿主自己出数字的标准时要原样收掉
            @Volatile
            var injectedText: String? = null

            // 自己画进 wifi_left 的数字，同样用于断开清理和避免每帧重设 drawable
            @Volatile
            var injectedDigit: Char? = null

            fun group(): ViewGroup? = groupRef.get()
            fun text(): TextView? = textRef.get()
            fun image(): ImageView? = imageRef.get()
        }

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
            val leftTvId = resolveWifiLeftTvId(context)
            val imageView = if (leftId != 0) viewGroup.findViewById<ImageView>(leftId) else null
            val textView = if (leftTvId != 0) viewGroup.findViewById<TextView>(leftTvId) else null
            ensureWifiCallback(context)
            // 同一个容器重新绑定时替换掉旧记录，已经随状态栏销毁的记录顺手清掉
            dropSlots { it.group() == null || it.group() === viewGroup }
            if (imageView != null || textView != null) {
                slotSets += LeftSlots(viewGroup, textView, imageView)
                ensureObservers(viewGroup)
            }
            onGroupLaidOut(viewGroup)
        }

        // 容器记录要用 CopyOnWriteArrayList 的逐个删除：它的迭代器不支持 remove，批量删除会抛异常
        private fun dropSlots(predicate: (LeftSlots) -> Boolean) {
            slotSets.toList().forEach { if (predicate(it)) slotSets.remove(it) }
        }

        // 每份容器挂两个回调：布局后看一次（宿主摆完槽位的落点），帧前再看一次
        //（宿主的 combine 收集器在协程里随时重写可见性，只有帧前这一遍赶得上）。不挂 framework 的 setter
        private fun ensureObservers(group: ViewGroup) {
            if (observedGroups.any { it.get() === group }) return
            observedGroups.toList().forEach { if (it.get() == null) observedGroups.remove(it) }
            observedGroups += WeakReference(group)
            val groupRef = WeakReference(group)
            group.viewTreeObserver.addOnGlobalLayoutListener {
                groupRef.get()?.let { onGroupLaidOut(it) }
            }
            group.viewTreeObserver.addOnPreDrawListener {
                groupRef.get()?.let { onPreDraw(it) }
                true
            }
        }

        // 宿主摆完槽位后的落点：只有它没在这份容器里出东西时才补画
        private fun onGroupLaidOut(group: ViewGroup) {
            val slots = slotSets.firstOrNull { it.group() === group } ?: return
            if (slots.text() == null && slots.image() == null) return
            if (hostOwnsSlots(slots)) return
            applyLowStandardDigit()
        }

        //宿主主题决定用哪个槽位：默认主题走 wifi_left_tv 文本，只有缺 O17 信号图的三方主题才走 wifi_left 图标。
        // 宿主在这份容器里自己出了东西就让位，同时记下它用的是哪个槽位；
        // 两个槽位同时被收起，就是「这个标准宿主不管」，这时才轮到我们补画
        private fun hostOwnsSlots(slots: LeftSlots): Boolean {
            val textView = slots.text()
            val imageView = slots.image()
            if (textView?.isVisible == true) {
                slots.textMode = true
                return true
            }
            if (imageView?.isVisible == true) {
                slots.textMode = false
                return true
            }
            return false
        }

        // 帧前补画：宿主在协程里收起槽位不一定会重新走全局布局回调，
        // 每帧画之前比对一次，才能保证用户看到的画面里是我们的结果
        private fun onPreDraw(group: ViewGroup) {
            if (!wifiConnected) return
            val digit = lowStandardDigitOf(wifiStandard) ?: return
            val slots = slotSets.firstOrNull { it.group() === group } ?: return
            if (hostOwnsSlots(slots)) return
            paintLowDigit(slots, digit)
        }

        //11N/11AC 宿主不会给任何槽位赋值，按各容器自己用的槽位把标准号补上
        private fun applyLowStandardDigit() {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                mainHandler.post { applyLowStandardDigit() }
                return
            }
            if (slotSets.isEmpty()) return
            dropSlots { it.group() == null }
            val digit = lowStandardDigitOf(wifiStandard)
            if (digit == null) return
            if (!wifiConnected) return
            slotSets.forEach { paintLowDigit(it, digit) }
        }

        // 只补画这一个容器：宿主已经在这份容器里出了东西就让它自己画。
        // 写入前先比对现值，否则帧前回调会和宿主的每帧收回打架，变成每帧一次布局
        private fun paintLowDigit(slots: LeftSlots, digit: Char) {
            if (slots.textMode != false) {
                val textView = slots.text() ?: return
                if (!isStandardIconEnabled(textView.context)) return
                val text = digit.toString()
                slots.injectedText = text
                slots.injectedDigit = null
                if (textView.textToString() != text) textView.text = text
                if (textView.visibility != View.VISIBLE) textView.visibility = View.VISIBLE
            } else {
                val imageView = slots.image() ?: return
                if (!isStandardIconEnabled(imageView.context)) return
                if (slots.injectedDigit != digit) {
                    slots.injectedDigit = digit
                    slots.injectedText = null
                    imageView.setImageDrawable(buildDigitDrawable(imageView.context, digit))
                }
                if (imageView.visibility != View.VISIBLE) imageView.setVisibility(View.VISIBLE)
            }
        }

        //断开后宿主不会再发一次（resId 和 text 都没变化），自己塞进去的数字要自己收掉
        private fun clearLowDigit() {
            if (Looper.myLooper() != Looper.getMainLooper()) {
                mainHandler.post { clearLowDigit() }
                return
            }
            val hostOwnsDigit = lowStandardDigitOf(wifiStandard) == null
            slotSets.forEach { slots ->
                val injected = slots.injectedText
                if (injected != null) {
                    slots.injectedText = null
                    val textView = slots.text()
                    if (textView != null && textView.textToString() == injected) {
                        textView.text = ""
                        textView.visibility = View.GONE
                    }
                }
                val imageView = slots.image() ?: return@forEach
                if (hostOwnsDigit && imageView.drawable is WifiStandardIconDrawable) {
                    slots.injectedDigit = null
                    imageView.setVisibility(View.GONE)
                }
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
            gateAllowed = evaluateStandardGate(context)
            return gateAllowed
        }

        //双 WiFi / PassPoint 由宿主自己接管标准号，系统技术标准图标开关没开时宿主也不显示，
        //任何一项读不到都按「不补画」处理，但要把原因留下来，不能静默失效
        private fun evaluateStandardGate(context: Context): Boolean {
            val classLoader = hostClassLoader ?: return false
            val wifiUtils = WifiUtils(classLoader)
            val dualWifi = runCatching { wifiUtils.isDualWifiConnected(context) }
                .onFailure { YLog.warn("wifi standard gate: isDualWifiConnected failed: " + it.message) }
                .getOrDefault(false)
            if (dualWifi) return false
            val passPoint = runCatching { wifiUtils.isPassPointAp(context) }
                .onFailure { YLog.warn("wifi standard gate: isPassPointAp failed: " + it.message) }
                .getOrDefault(false)
            return !passPoint && runCatching {
                AbsSettingsValueProxyUtils(classLoader)
                    .getGlobalIntValue(context, TECHNICAL_ICON_SWITCH, 0) == 1
            }.onFailure { YLog.warn("wifi standard gate: technical switch read failed: " + it.message) }
                .getOrDefault(false)
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
                            val isPrimary = runCatching {
                                info.asResolver().firstMethod {
                                    name = "isPrimary"; emptyParameters()
                                }.invoke<Boolean>() ?: false
                            }.onFailure {
                                YLog.warn("wifi standard: isPrimary failed: " + it.message)
                            }.getOrDefault(false)
                            if (!isPrimary) return
                            wifiNetwork = network
                            wifiConnected = true
                            onWifiStandardChanged(info.wifiStandard)
                        }

                        //只有当前取标准号的那个网络断开才收掉状态，避免别的 WiFi 网络（副卡热点等）
                        //断开时把主网络的标准号一起清掉，之后又等不到新的回调
                        override fun onLost(network: Network) {
                            if (network != wifiNetwork) return
                            wifiNetwork = null
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
