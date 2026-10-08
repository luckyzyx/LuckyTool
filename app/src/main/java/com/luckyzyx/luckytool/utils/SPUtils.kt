@file:Suppress("DEPRECATION", "WorldReadableFiles", "ApplySharedPref", "UseKtx", "unused")

package com.luckyzyx.luckytool.utils

import android.content.Context
import android.util.ArrayMap
import android.util.ArraySet
import androidx.collection.arrayMapOf
import androidx.collection.arraySetOf
import com.luckyzyx.luckytool.ui.service.XposedServiceBridge

const val ModulePrefs: String = "ModulePrefs"
const val IntentPrefs: String = "IntentPrefs"
const val SettingsPrefs: String = "SettingsPrefs"
const val OtherPrefs: String = "OtherPrefs"

/**
 * UI 侧 prefs 访问统一入口：绑定 libxposed service 时走 remote prefs
 * （与宿主进程 YukiHookPreferences 同一数据源），未绑定时回落本地 prefs。
 */
internal fun Context.appPrefs(prefsName: String): android.content.SharedPreferences =
    XposedServiceBridge.preferences(prefsName)
        ?: getSharedPreferences(prefsName, Context.MODE_PRIVATE)

// ---------------- 集合语义的兼容读（历史遗留的字符串形态数据） ----------------

private val STRING_SET_TEXT = Regex("^\\s*[\\[{](.*)[]}]\\s*$", RegexOption.DOT_MATCHES_ALL)

/** 字符串是否为 `Set.toString()` 形态（`{a, b}` / `[a, b]`）：据判断集合键能否安全还原 */
internal fun looksLikeStringSet(value: String?): Boolean =
    STRING_SET_TEXT.matches(value?.trim().orEmpty())

/** 把字符串形态的集合还原成 Set；单个包名/逗号分隔串同样接受，空串得到空集 */
internal fun legacyStringSet(value: String?): Set<String> {
    val text = value?.trim().orEmpty()
    if (text.isEmpty()) return emptySet()
    val body = STRING_SET_TEXT.matchEntire(text)?.groupValues?.get(1) ?: text
    return body.split(',').map { it.trim().trim('"', '\'') }.filter { it.isNotEmpty() }.toSet()
}

/**
 * 按集合语义解析原始存储值：兼容 Set / Collection / 字符串形态。
 *
 * 备份 json 的 `JSONObject.put(key, Set)`（只接受 HashSet，其余类型走 toString）、
 * 以及任何 `Set.toString()` 落盘都会留下 `{a, b}` 形态的字符串，之后 getStringSet
 * 的未检查强转就会抛 ClassCastException，这里统一还原。
 */
internal fun rawStringSet(value: Any?, default: Set<String> = emptySet()): Set<String> = when (value) {
    null -> default
    is String -> legacyStringSet(value)
    is Collection<*> -> value.filterIsInstance<String>().toSet()
    else -> default
}


fun Context.getString(prefsName: String, key: String, defaultValue: String = ""): String {
    return try {
        val prefs = appPrefs(prefsName)
        prefs.getString(key, defaultValue) ?: defaultValue
    } catch (t: Throwable) {
        LogUtils.e("SPUtils", "getString $key -> $defaultValue", "$t", true)
        defaultValue
    }
}

fun Context.putString(prefsName: String, key: String, value: String): Boolean {
    return try {
        val prefs = appPrefs(prefsName)
        prefs.edit().putString(key, value).commit()
    } catch (t: Throwable) {
        LogUtils.e("SPUtils", "putString $key -> $value", "$t", true)
        false
    }
}

fun Context.getStringSet(
    prefsName: String, key: String, defaultValue: Set<String> = arraySetOf()
): Set<String> {
    return try {
        val prefs = appPrefs(prefsName)
        // 直接 getStringSet 对字符串形态的历史数据会抛 CCE，改为按原始值判定类型
        val value = prefs.all?.get(key)
        if (value == null) ArraySet(defaultValue) else rawStringSet(value, defaultValue)
    } catch (t: Throwable) {
        LogUtils.e("SPUtils", "getStringSet $key -> $defaultValue", "$t", true)
        defaultValue
    }
}

fun Context.putStringSet(prefsName: String, key: String, value: Set<String>): Boolean {
    return try {
        val prefs = appPrefs(prefsName)
        prefs.edit().putStringSet(key, value).commit()
    } catch (t: Throwable) {
        LogUtils.e("SPUtils", "putStringSet $key -> $value", "$t", true)
        false
    }
}

fun Context.getInt(prefsName: String, key: String, defaultValue: Int = -1): Int {
    return try {
        val prefs = appPrefs(prefsName)
        prefs.getInt(key, defaultValue)
    } catch (t: Throwable) {
        LogUtils.e("SPUtils", "getInt $key -> $defaultValue", "$t", true)
        defaultValue
    }
}

fun Context.putInt(prefsName: String, key: String, value: Int): Boolean {
    return try {
        val prefs = appPrefs(prefsName)
        prefs.edit().putInt(key, value).commit()
    } catch (t: Throwable) {
        LogUtils.e("SPUtils", "putInt $key -> $value", "$t", true)
        false
    }
}

fun Context.getLong(prefsName: String, key: String, defaultValue: Long = -1L): Long {
    return try {
        val prefs = appPrefs(prefsName)
        prefs.getLong(key, defaultValue)
    } catch (t: Throwable) {
        LogUtils.e("SPUtils", "getLong $key -> $defaultValue", "$t", true)
        defaultValue
    }
}

fun Context.putLong(prefsName: String, key: String, value: Long): Boolean {
    return try {
        val prefs = appPrefs(prefsName)
        prefs.edit().putLong(key, value).commit()
    } catch (t: Throwable) {
        LogUtils.e("SPUtils", "putLong $key -> $value", "$t", true)
        false
    }
}

fun Context.getFloat(prefsName: String, key: String, defaultValue: Float = -1F): Float {
    return try {
        val prefs = appPrefs(prefsName)
        prefs.getFloat(key, defaultValue)
    } catch (t: Throwable) {
        LogUtils.e("SPUtils", "getFloat $key -> $defaultValue", "$t", true)
        defaultValue
    }
}

fun Context.putFloat(prefsName: String, key: String, value: Float): Boolean {
    return try {
        val prefs = appPrefs(prefsName)
        prefs.edit().putFloat(key, value).commit()
    } catch (t: Throwable) {
        LogUtils.e("SPUtils", "putFloat $key -> $value", "$t", true)
        false
    }
}

fun Context.getBoolean(prefsName: String, key: String, defaultValue: Boolean = false): Boolean {
    return try {
        val prefs = appPrefs(prefsName)
        prefs.getBoolean(key, defaultValue)
    } catch (t: Throwable) {
        LogUtils.e("SPUtils", "getBoolean $key -> $defaultValue", "$t", true)
        defaultValue
    }
}

fun Context.putBoolean(prefsName: String, key: String, value: Boolean): Boolean {
    return try {
        val prefs = appPrefs(prefsName)
        prefs.edit().putBoolean(key, value).commit()
    } catch (t: Throwable) {
        LogUtils.e("SPUtils", "putBoolean $key -> $value", "$t", true)
        false
    }
}

/**
 * 删除键值数据
 * @receiver Context
 * @param prefsName String
 * @param key String
 */
fun Context.removeKey(prefsName: String, key: String): Boolean {
    return try {
        val prefs = appPrefs(prefsName)
        prefs.edit().remove(key).commit()
    } catch (t: Throwable) {
        LogUtils.e("SPUtils", "removeKey $key", "$t", true)
        false
    }
}

/**
 * 删除此配置键值数据
 * @receiver Context
 * @param prefsName String?
 * @return Boolean
 */
fun Context.clearPrefs(prefsName: String): Boolean {
    return try {
        val prefs = appPrefs(prefsName)
        prefs.edit().clear().commit()
    } catch (t: Throwable) {
        LogUtils.e("SPUtils", "clearPrefs $prefsName", "$t", true)
        false
    }
}

/**
 * 删除全部配置键值数据
 * @receiver Context
 * @param prefList Array<out String?>
 */
fun Context.clearAllPrefs(vararg prefList: String): Boolean {
    val curStatus = BooleanArray(prefList.size)
    prefList.forEachIndexed { index, name ->
        try {
            val prefs = appPrefs(name)
            curStatus[index] = prefs.edit().clear().commit()
        } catch (t: Throwable) {
            LogUtils.e("SPUtils", "clearAllPrefs $name", "$t", true)
            curStatus[index] = false
        }
    }
    return curStatus.contains(false)
}

/**
 * 获取配置键值数据
 * @receiver Context
 * @param prefsName String?
 * @return MutableMap<String, *>?
 */
fun Context.backupPrefs(prefsName: String): MutableMap<String, *> {
    return try {
        val prefs = appPrefs(prefsName)
        prefs.all
    } catch (t: Throwable) {
        LogUtils.e("SPUtils", "backupPrefs $prefsName", "$t", true)
        arrayMapOf<String, Any>()
    }
}

/**
 * 获取配置键值数据
 * @receiver Context
 * @param prefList Array<out String?>
 * @return ArrayMap<String, MutableMap<String, *>>?
 */
fun Context.backupAllPrefs(vararg prefList: String): ArrayMap<String, MutableMap<String, *>?> {
    val map = ArrayMap<String, MutableMap<String, *>?>()
    prefList.forEachIndexed { _, name ->
        try {
            val prefs = appPrefs(name)
            map[name] = prefs.all
        } catch (t: Throwable) {
            LogUtils.e("SPUtils", "backupAllPrefs $name", "$t", true)
            map[name] = arrayMapOf<String, Any>()
        }
    }
    return map
}