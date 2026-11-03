package com.luckyzyx.luckytool.hook.utils

import com.highcapable.yukihookapi.hook.xposed.preference.YukiHookPreferences
import com.luckyzyx.luckytool.data.AppVerInfo
import com.luckyzyx.luckytool.utils.safeOfNull
import kotlinx.serialization.json.Json

/**
 * 读取模块侧 [com.luckyzyx.luckytool.utils.AppUtils.getAppVerInfo] 写入的宿主版本信息。
 *
 * YukiHook 1.5.0 没有宿主版本信息 API，故由项目自己的业务工具补上；
 * 写入格式见 AppUtils（JSON 字符串集合，这里取集合首个元素反序列化）。
 */
fun YukiHookPreferences.getAppVerInfo(packName: String): AppVerInfo? {
    val set = getStringSet(packName, emptySet())
    return if (set.isEmpty()) null else safeOfNull {
        Json.decodeFromString<AppVerInfo>(set.firstOrNull() ?: "")
    }
}
