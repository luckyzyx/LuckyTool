@file:Suppress("unused")

package com.luckyzyx.luckytool.utils

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

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
 *
 * 读通道按「类型 + key」缓存：同一个 key 可能同时被不同类型的读通道读取（例如集合键既被
 * stringFlow 又被 stringSetFlow 读），共用一个 StateFlow 会把一种类型的值塞进另一种类型的
 * 读通道（历史上表现为「集合键被当成 String 读出」并抛 ClassCastException）。
 * 各类型读通道都只接受与自身类型匹配的原始值，类型不符时回落默认值；集合键另兼容
 * 历史遗留的字符串形态（`{a, b}`）并异步写回规范形态自愈。
 */
class PrefState private constructor(
    private val provider: () -> SharedPreferences,
) {
    /** 读通道缓存：key = "类型标签|prefs key"，不同类型不复用同一个 MutableStateFlow */
    private val flows = ConcurrentHashMap<String, MutableStateFlow<Any?>>()

    /** prefs key -> 该 key 下所有已创建的读通道，键变化/数据源切换时按各自读法回读 */
    private val channels = ConcurrentHashMap<String, MutableList<ReadChannel<*>>>()

    /** 已安排自愈写回的 key（每键只做一次） */
    private val healedKeys = ConcurrentHashMap<String, Boolean>()

    private var registered: SharedPreferences? = null

    /** 每次落盘/外部写入递增；ScopeScreen 订阅它以在“条件可见性”依赖的键变化时重建条目 */
    val revision = MutableStateFlow(0L)

    private fun bump() {
        revision.value++
    }

    /** 一条读通道：自带默认值与读法，刷新时只用自己的读法，绝不把原始值跨类型赋值 */
    private inner class ReadChannel<T>(
        private val default: T,
        private val read: (SharedPreferences) -> T,
    ) {
        val flow = MutableStateFlow(default)

        fun refresh(prefs: SharedPreferences) {
            flow.value = try {
                read(prefs)
            } catch (t: Throwable) {
                default
            }
        }
    }

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        val prefs = registered
        if (prefs != null) channels[key]?.forEach { it.refresh(prefs) }
        bump()
    }

    @Synchronized
    private fun current(): SharedPreferences {
        val prefs = provider()
        if (prefs !== registered) {
            registered?.unregisterOnSharedPreferenceChangeListener(listener)
            prefs.registerOnSharedPreferenceChangeListener(listener)
            registered = prefs
            // 数据源切换后全量刷新（按各读通道自己的读法回读）
            channels.values.forEach { list -> list.forEach { it.refresh(prefs) } }
            bump()
        }
        return prefs
    }

    /** 原始存储值（不做类型转换/强转），任何异常都回落 null */
    private fun rawValue(prefs: SharedPreferences, key: String): Any? =
        try {
            prefs.all?.get(key)
        } catch (t: Throwable) {
            null
        }

    // ---------------- 响应式读（Compose collectAsStateWithLifecycle 用） ----------------

    fun allFlow(): StateFlow<Map<String, Any?>> =
        flow("all", "all", mapOf()) { it.all ?: mapOf() }

    fun stringFlow(key: String, default: String = ""): StateFlow<String> =
        flow(key, "string", default) { prefs -> rawValue(prefs, key) as? String ?: default }

    fun booleanFlow(key: String, default: Boolean = false): StateFlow<Boolean> =
        flow(key, "boolean", default) { prefs -> rawValue(prefs, key) as? Boolean ?: default }

    fun intFlow(key: String, default: Int = -1): StateFlow<Int> =
        flow(key, "int", default) { prefs -> (rawValue(prefs, key) as? Number)?.toInt() ?: default }

    fun longFlow(key: String, default: Long = -1L): StateFlow<Long> =
        flow(key, "long", default) { prefs -> (rawValue(prefs, key) as? Number)?.toLong() ?: default }

    fun floatFlow(key: String, default: Float = -1f): StateFlow<Float> =
        flow(key, "float", default) { prefs -> (rawValue(prefs, key) as? Number)?.toFloat() ?: default }

    fun stringSetFlow(key: String, default: Set<String> = emptySet()): StateFlow<Set<String>> =
        flow(key, "stringSet", default) { prefs ->
            val raw = rawValue(prefs, key)
            val set = rawStringSet(raw, default)
            // 历史遗留的字符串形态（备份 JSONObject.put(Set) / Set.toString 写入的 `{a, b}`）：
            // 读回后异步写回规范形态，宿主进程的 getStringSet 才不会抛 ClassCastException
            if (raw is String && looksLikeStringSet(raw) && healedKeys.putIfAbsent(key, true) == null) {
                healExecutor.execute {
                    try {
                        prefs.edit().putStringSet(key, set).commit()
                    } catch (t: Throwable) {
                        LogUtils.e("PrefState", "heal stringSet $key", "$t", true)
                    }
                }
            }
            set
        }

    // ---------------- 同步读（DSL 条件判断 / 非 Compose 代码兼容） ----------------

    fun getAll(): Map<String, Any?> =
        try { current().all } catch (t: Throwable) { mapOf() }

    fun getString(key: String, default: String? = null): String? =
        try { rawValue(current(), key) as? String ?: default } catch (t: Throwable) { default }

    fun getBoolean(key: String, default: Boolean = false): Boolean =
        try { rawValue(current(), key) as? Boolean ?: default } catch (t: Throwable) { default }

    fun getInt(key: String, default: Int = -1): Int =
        try { (rawValue(current(), key) as? Number)?.toInt() ?: default } catch (t: Throwable) { default }

    fun getLong(key: String, default: Long = -1L): Long =
        try { (rawValue(current(), key) as? Number)?.toLong() ?: default } catch (t: Throwable) { default }

    fun getFloat(key: String, default: Float = 0f): Float =
        try { (rawValue(current(), key) as? Number)?.toFloat() ?: default } catch (t: Throwable) { default }

    fun getStringSet(key: String, default: Set<String> = emptySet()): Set<String> =
        try {
            rawStringSet(rawValue(current(), key), default)
        } catch (t: Throwable) {
            default
        }

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
    private fun <T> flow(
        key: String,
        typeTag: String,
        default: T,
        read: (SharedPreferences) -> T,
    ): StateFlow<T> {
        val prefs = current()
        val channelKey = "$typeTag|$key"
        (flows[channelKey] as? MutableStateFlow<T>)?.let { cached ->
            cached.value = try {
                read(prefs)
            } catch (t: Throwable) {
                default
            }
            return cached
        }
        val channel = ReadChannel(default, read)
        val raced = flows.putIfAbsent(channelKey, channel.flow as MutableStateFlow<Any?>)
        if (raced != null) {
            val cached = raced as MutableStateFlow<T>
            cached.value = try {
                read(prefs)
            } catch (t: Throwable) {
                default
            }
            return cached
        }
        channels.getOrPut(key) { Collections.synchronizedList(mutableListOf()) }.add(channel)
        channel.refresh(prefs)
        return channel.flow
    }

    companion object {
        private val cache = ConcurrentHashMap<String, PrefState>()

        /** 自愈写回用的单线程：避免在 SP 变更回调里同步改写同一份 prefs */
        private val healExecutor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "PrefState-heal")
        }

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
