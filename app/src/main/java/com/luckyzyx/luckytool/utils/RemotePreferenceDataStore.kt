package com.luckyzyx.luckytool.utils

import android.content.SharedPreferences
import androidx.preference.PreferenceDataStore

/**
 * Preference UI 的数据存储层（替代 preferenceManager 默认的 SharedPreferences 文件）：
 * 所有 Preference 控件的持久化读写都经 [target]——
 * target 由 [appPrefs] 提供：libxposed service 绑定可用时即 remote prefs
 * （与宿主进程 YukiHookPreferences 同一数据源），未绑定时回落本地 prefs。
 */
class RemotePreferenceDataStore(private val target: SharedPreferences) : PreferenceDataStore() {

    private fun editor(): SharedPreferences.Editor = target.edit()

    override fun putString(key: String, value: String?) {
        editor().apply {
            if (value == null) remove(key) else putString(key, value)
        }.apply()
    }

    override fun putStringSet(key: String, values: Set<String>?) {
        editor().apply {
            if (values == null) remove(key) else putStringSet(key, values)
        }.apply()
    }

    override fun putInt(key: String, value: Int) = editor().putInt(key, value).apply()

    override fun putLong(key: String, value: Long) = editor().putLong(key, value).apply()

    override fun putFloat(key: String, value: Float) = editor().putFloat(key, value).apply()

    override fun putBoolean(key: String, value: Boolean) = editor().putBoolean(key, value).apply()

    override fun getString(key: String, defValue: String?): String? =
        target.getString(key, defValue)

    override fun getStringSet(key: String, defValues: Set<String>?): Set<String>? =
        target.getStringSet(key, defValues)

    override fun getInt(key: String, defValue: Int): Int = target.getInt(key, defValue)

    override fun getLong(key: String, defValue: Long): Long = target.getLong(key, defValue)

    override fun getFloat(key: String, defValue: Float): Float = target.getFloat(key, defValue)

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        target.getBoolean(key, defValue)
}