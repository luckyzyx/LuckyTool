@file:Suppress("unused")

package com.luckyzyx.luckytool.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * SharedPreferences 的响应式读取层（Compose 迁移的存储桥）。
 *
 * 存储层保持 SP 不变：hook 在宿主进程（SystemUI 等）通过 XSharedPreferences / remote prefs
 * 跨进程读取同一份数据，不能迁 DataStore（protobuf 宿主进程读不了）。
 * 本类只补充 StateFlow 读取通道与监听回流；写路径语义与 SPUtils 的 commit() 保持一致。
 *
 * 数据源经 [com.luckyzyx.luckytool.utils.appPrefs] 解析：libxposed service 绑定时为 remote
 * prefs（与宿主进程 YukiHookPreferences 同一数据源），未绑定时回落本地 MODE_PRIVATE prefs。
 * 数据源切换（service 绑定/解绑）时自动重新注册监听并全量刷新已缓存 Flow。
 */
class PrefState private constructor(
    private val provider: () -> SharedPreferences,
) {
    private val flows = ConcurrentHashMap<String, MutableStateFlow<Any?>>()
    private var registered: SharedPreferences? = null

    /** 每次落盘/外部写入递增；ScopeScreen 订阅它以在“条件可见性”依赖的键变化时重建条目 */
    val revision = MutableStateFlow(0L)

    private fun bump() {
        revision.value++
    }

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        flows[key]?.value = registered?.all?.get(key)
        bump()
    }

    @Synchronized
    private fun current(): SharedPreferences {
        val prefs = provider()
        if (prefs !== registered) {
            registered?.unregisterOnSharedPreferenceChangeListener(listener)
            prefs.registerOnSharedPreferenceChangeListener(listener)
            registered = prefs
            // 数据源切换后全量刷新
            flows.forEach { (key, flow) -> flow.value = prefs.all[key] }
            bump()
        }
        return prefs
    }

    // ---------------- 响应式读（Compose collectAsStateWithLifecycle 用） ----------------

    fun stringFlow(key: String, default: String = ""): StateFlow<String> =
        flow(key, default) { it.getString(key, default) ?: default }

    fun booleanFlow(key: String, default: Boolean = false): StateFlow<Boolean> =
        flow(key, default) { it.getBoolean(key, default) }

    fun intFlow(key: String, default: Int = -1): StateFlow<Int> =
        flow(key, default) { it.getInt(key, default) }

    fun longFlow(key: String, default: Long = -1L): StateFlow<Long> =
        flow(key, default) { it.getLong(key, default) }

    fun stringSetFlow(key: String, default: Set<String> = emptySet()): StateFlow<Set<String>> =
        flow(key, default) { it.getStringSet(key, default)?.toSet() ?: default }

    // ---------------- 同步读（DSL 条件判断 / 非 Compose 代码兼容） ----------------

    fun getString(key: String, default: String? = null): String? =
        try { current().getString(key, default) } catch (t: Throwable) { default }

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        try { current().getBoolean(key, default) } catch (t: Throwable) { default }

    fun getInt(key: String, default: Int = -1): Int =
        try { current().getInt(key, default) } catch (t: Throwable) { default }

    fun getLong(key: String, default: Long = -1L): Long =
        try { current().getLong(key, default) } catch (t: Throwable) { default }

    fun getStringSet(key: String, default: Set<String> = emptySet()): Set<String> =
        try { current().getStringSet(key, default)?.toSet() ?: default } catch (t: Throwable) { default }

    // ---------------- 写（commit 语义，与 SPUtils 一致；写入后监听器同步回流对应 Flow） ----------------

    fun set(key: String, value: Any?): Boolean = try {
        val editor = current().edit()
        when (value) {
            null -> editor.remove(key)
            is String -> editor.putString(key, value)
            is Boolean -> editor.putBoolean(key, value)
            is Int -> editor.putInt(key, value)
            is Long -> editor.putLong(key, value)
            is Float -> editor.putFloat(key, value)
            is Set<*> -> @Suppress("UNCHECKED_CAST") editor.putStringSet(key, value as Set<String>)
            else -> return false
        }
        val committed = editor.commit()
        if (committed) bump()
        committed
    } catch (t: Throwable) {
        false
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> flow(key: String, default: T, read: (SharedPreferences) -> T): StateFlow<T> =
        flows.getOrPut(key) { MutableStateFlow(read(current())) }.also {
            (it as MutableStateFlow<T>).value = read(current())
        } as MutableStateFlow<T>

    companion object {
        private val cache = ConcurrentHashMap<String, PrefState>()

        /** 按 prefs 文件名缓存实例（进程生命周期）；测试/迁移过渡可显式 [invalidate] */
        fun of(context: Context, prefsName: String): PrefState =
            cache.getOrPut(prefsName) { PrefState { context.appPrefs(prefsName) } }

        /** 测试/过渡用：直接注入 prefs 来源 */
        internal fun ofProvider(provider: () -> SharedPreferences): PrefState = PrefState(provider)

        fun invalidate(prefsName: String) {
            cache.remove(prefsName)
        }
    }
}
