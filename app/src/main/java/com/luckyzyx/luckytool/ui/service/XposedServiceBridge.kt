package com.luckyzyx.luckytool.ui.service

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import com.highcapable.yukihookapi.YukiHook
import com.highcapable.yukihookapi.hook.factory.preferences
import com.highcapable.yukihookapi.hook.xposed.application.ModuleApplication
import com.highcapable.yukihookapi.hook.xposed.preference.YukiHookPreferences
import com.luckyzyx.luckytool.utils.LogUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections
import java.util.WeakHashMap
import kotlin.time.Duration.Companion.milliseconds

/**
 * 模块 App 侧接入框架 service 的唯一入口（YukiHook 门面）：
 * - 用 [YukiHook.Status.observeFrameworkService] **订阅**，而不是自己注册 libxposed 监听：
 *   XposedServiceHelper 只有一个静态监听槽，且已由 YukiHook 在 ModuleApplication.onCreate()
 *   里占住，谁后注册谁覆盖、被覆盖的一侧拿不到 service，所以这里不能再注册监听
 * - 回调给的是 available 布尔（= 框架 service 已绑定，库注释明确不校验 scope），
 *   激活态因此仍按「service 可用 **且** scope 非空」判定（scope 非空才是真的生效）
 * - [preferences] 在 service 可用时返回 remote prefs（宿主进程 YukiHookPreferences 的同一数据源），
 *   不可用时返回 null，由调用方回落本地 prefs
 *
 * 激活检测是「观察」而不是「问一次」：绑定是异步的，绑定/断开都经回调送达（主线程注册时首帧同步回调），
 * 所以 UI 侧要等 [isModuleActive] 出结果再读 prefs，等待期间只组合加载层（见 ActivationLoadingLayer），
 * 检测到激活即取消。
 */
object XposedServiceBridge {

    private const val TAG = "YukiHook Service Bridge"

    @Volatile
    private var initialized = false

    /**
     * 框架 service 是否可用：回调直接给出的值，不含 scope 判定。
     * 只有它为 true 时 remote prefs 才是真的 remote（否则 YukiHookPreferences 会静默回落本地存储）。
     */
    @Volatile
    var isAvailable = false
        private set

    private val _isModuleActive = MutableStateFlow(false)

    /**
     * 模块激活态：框架服务已绑定且 scope 非空。
     * false 同时覆盖「未激活」与「绑定还没完成」（异步），调用方按超时区分两者。
     */
    val isModuleActive: StateFlow<Boolean> = _isModuleActive.asStateFlow()

    private val remotePreferences = mutableMapOf<String, SharedPreferences>()

    fun init() {
        if (initialized) return
        initialized = true
        YukiHook.Status.observeFrameworkService { available ->
            isAvailable = available
            updateActiveState()
            LogUtils.d(
                TAG,
                if (available) "onServiceBind" else "onServiceDied",
                "framework ${YukiHook.Status.Executor.name} API ${YukiHook.Status.Executor.apiVersion}",
                true
            )
        }
    }

    /** scope 是 Binder 调用：框架进程异常时会抛，读失败按未激活处理并记日志 */
    private fun updateActiveState() {
        _isModuleActive.value = isAvailable && runCatching {
            YukiHook.Module.scope.isAvailable && YukiHook.Module.scope.packages.isNotEmpty()
        }.onFailure { LogUtils.e(TAG, "updateActiveState", "read module scope failed: $it", true) }
            .getOrDefault(false)
    }

    /**
     * remote prefs（宿主进程 YukiHookPreferences 的同一数据源）。
     * 按名字缓存实例：调用方（[com.luckyzyx.luckytool.utils.PrefState]）用实例身份判断数据源是否切换、
     * 并把变更监听器注册在拿到的实例上，每次新建包装对象会让它每次都当成「数据源变了」而全量重读。
     * service 不可用、或取用失败（框架缺 remote 能力、多框架并存、Context 未就绪都会抛）时返回 null，
     * 由调用方回落本地 prefs，避免静默读写成模块自己的本地存储。
     */
    fun preferences(name: String): SharedPreferences? {
        if (!isAvailable) return null
        return runCatching {
            synchronized(remotePreferences) {
                remotePreferences.getOrPut(name) {
                    RemotePreferences(ModuleApplication.requireContext().preferences(name))
                }
            }
        }.onFailure { LogUtils.e(TAG, "preferences", "open remote prefs failed: $it", true) }
            .getOrNull()
    }
}

/**
 * 把 YukiHook 的 remote prefs 门面适配成 [SharedPreferences]（调用方统一按 SharedPreferences 使用）：
 * 读走 [YukiHookPreferences] 的类型化读取，写走它的 Editor（本身就是 SharedPreferences.Editor），
 * 并在 commit/apply 之后通知注册在本实例上的监听器。
 *
 * 通知范围只在本实例内：远端不会推送外部变更，因此只有经本实例写入才会有通知；
 * 实例由 [XposedServiceBridge.preferences] 按名字缓存，所以「同一份 prefs 的不同调用点共享
 * 监听器」这一行为必须保留。
 */
private class RemotePreferences(private val prefs: YukiHookPreferences) : SharedPreferences {

    private val listeners =
        Collections.synchronizedMap(WeakHashMap<OnSharedPreferenceChangeListener, Any>())

    override fun getAll(): MutableMap<String, *> = prefs.all()

    override fun getString(key: String, defValue: String?): String? =
        if (defValue != null) prefs.getString(key, defValue)
        else prefs.getString(key).takeIf { prefs.contains(key) }

    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
        if (defValues != null) prefs.getStringSet(key, defValues).toMutableSet()
        else prefs.getStringSet(key).takeIf { prefs.contains(key) }?.toMutableSet()

    override fun getInt(key: String, defValue: Int): Int = prefs.getInt(key, defValue)

    override fun getLong(key: String, defValue: Long): Long = prefs.getLong(key, defValue)

    override fun getFloat(key: String, defValue: Float): Float = prefs.getFloat(key, defValue)

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        prefs.getBoolean(key, defValue)

    override fun contains(key: String): Boolean = prefs.contains(key)

    override fun edit(): SharedPreferences.Editor = NotifyingEditor(prefs.edit())

    override fun registerOnSharedPreferenceChangeListener(
        listener: OnSharedPreferenceChangeListener?
    ) {
        if (listener != null) listeners[listener] = Unit
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: OnSharedPreferenceChangeListener?
    ) {
        if (listener != null) listeners.remove(listener)
    }

    /**
     * 按 key 通知本次改动。[cleared] 为真时先以 key == null 通知一次
     * （本地 SharedPreferences 与 SPUtils.clearPrefs 的整份清空约定；libxposed 侧这次通知受
     * `RemotePreferences.shouldNotifyCleared` 开关控制，这里无条件通知，最坏只是让监听方多整体重读一次）。
     */
    private fun notifyChanged(cleared: Boolean, changedKeys: Set<String>) {
        val snapshot = synchronized(listeners) { listeners.keys.toList() }
        if (snapshot.isEmpty()) return
        snapshot.forEach { listener ->
            if (cleared) listener.onSharedPreferenceChanged(this, null)
            changedKeys.forEach { listener.onSharedPreferenceChanged(this, it) }
        }
    }

    /** 记录改动过的键，在 commit/apply 成功后统一通知 */
    private inner class NotifyingEditor(
        private val delegate: SharedPreferences.Editor
    ) : SharedPreferences.Editor {

        private val changedKeys = linkedSetOf<String>()
        private var cleared = false

        override fun putString(key: String, value: String?): SharedPreferences.Editor =
            if (value == null) remove(key) else track(key) { delegate.putString(key, value) }

        override fun putStringSet(
            key: String, values: MutableSet<String>?
        ): SharedPreferences.Editor = if (values == null) remove(key) else track(key) {
            // 复制成 java.util 实现再交给框架侧反序列化（Kotlin 单例/不可变集合会解析不到）
            delegate.putStringSet(key, HashSet(values))
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor =
            track(key) { delegate.putInt(key, value) }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor =
            track(key) { delegate.putLong(key, value) }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor =
            track(key) { delegate.putFloat(key, value) }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor =
            track(key) { delegate.putBoolean(key, value) }

        override fun remove(key: String): SharedPreferences.Editor = track(key) {
            delegate.remove(key)
        }

        override fun clear(): SharedPreferences.Editor {
            cleared = true
            delegate.clear()
            return this
        }

        override fun commit(): Boolean {
            val committed = delegate.commit()
            // 顺序固定：先向框架提交（上一行的 delegate.commit()），再按改动通知本实例的监听器
            notifyChanged(cleared, changedKeys)
            return committed
        }

        override fun apply() {
            delegate.apply()
            notifyChanged(cleared, changedKeys)
        }

        private fun track(key: String, block: () -> Unit): SharedPreferences.Editor {
            changedKeys.add(key)
            block()
            return this
        }
    }
}

/**
 * 等激活检测出结果：检测到激活返回 true，[timeoutMillis] 内仍未激活返回 false。
 *
 * UI 侧（MainActivity）靠它决定「继续显示加载层」还是「弹未激活阻塞门」。
 * [first] 只在值变化时唤醒，不轮询；超时返回 false 不代表就此结束——调用方仍继续观察，
 * 激活迟到到达时撤掉阻塞门。
 */
internal suspend fun StateFlow<Boolean>.awaitActivation(timeoutMillis: Long): Boolean =
    withTimeoutOrNull(timeoutMillis.milliseconds) { first { it } } != null
