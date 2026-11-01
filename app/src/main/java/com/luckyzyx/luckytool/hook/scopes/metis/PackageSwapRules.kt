package com.luckyzyx.luckytool.hook.scopes.metis

import com.highcapable.yukihookapi.hook.param.PackageParam
import org.lsposed.lsparanoid.Obfuscate

/**
 * 替换表解析。
 *
 * 表是用户手写的 "源包名=目标包名"，多条用 ; 分隔，例如 com.quark.browser=com.quark.clouddrive。
 * 匹配方式是卡片目标包名的精确匹配 —— 「复制口令 / 仅口令卡」里没有网址，metis 直接把包名写死，
 * 这是唯一能接住这类卡片的入口。表很短，每次判定都重新解析，用户改完即时生效。
 */
@Obfuscate
object PackageSwapRules {
    private const val SEPARATOR = ";"
    private const val DELIMITER = "="

    /**
     * 解析成「源包名 → 目标包名」映射。
     * 容错优先：空段、缺 =、只有一侧内容、源与目标相同、两侧带空白都跳过，坏段不能拖垮整张表。
     */
    fun parse(raw: String?): Map<String, String> {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return emptyMap()
        val result = LinkedHashMap<String, String>()
        text.split(SEPARATOR).forEach { segment ->
            val item = segment.trim()
            if (item.isEmpty()) return@forEach
            val index = item.indexOf(DELIMITER)
            if (index <= 0) return@forEach
            val source = item.substring(0, index).trim()
            val target = item.substring(index + 1).trim()
            if (source.isEmpty() || target.isEmpty() || source == target) return@forEach
            result[source] = target
        }
        return result
    }

    /** 按当前偏好值解析出替换表 */
    fun fromPreferences(param: PackageParam): Map<String, String> =
        parse(MetisSettings.packageSwapRules(param))

    /** 源包名查目标包名，表里没有返回 null；同时用作「这个包名是否需要额外处理」的廉价预筛 */
    fun targetOf(param: PackageParam, sourcePackage: String?): String? {
        val source = sourcePackage?.trim().orEmpty()
        if (source.isEmpty()) return null
        return fromPreferences(param)[source]
    }
}
