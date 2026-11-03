package com.luckyzyx.luckytool.utils

import android.content.Context
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.toClass
import com.topjohnwu.superuser.ShellUtils
import org.lsposed.hiddenapibypass.HiddenApiBypass

/**
 * 极暗模式（AOSP 额外调暗 / ReduceBrightColors）的 Secure 键读写。
 *
 * 框架侧的 [android.hardware.display.ColorDisplayService] 无条件监听下面三个 Secure 键，
 * 键一变就激活/更新滤镜，所以用 root 直接写键即可控制该功能，不必依赖系统设置页
 * （部分 ROM 的极暗设置页会因「不受支持」被过滤成空白页）。
 *
 * 注意：
 * - 三个键都走 `settings` 命令，必须在 IO 线程调用，绝不能放在组合/拖动回调的每一帧里。
 * - 写入 [KEY_ACTIVATED] 之前必须用 [isSupported] 判定颜色变换是否加速：不加速时框架套用的是
 *   全零颜色矩阵，此时写 1 会让屏幕全黑（只能再写 0 恢复）。
 */
object ExtraDimUtils {

    const val KEY_ACTIVATED = "reduce_bright_colors_activated"
    const val KEY_LEVEL = "reduce_bright_colors_level"
    const val KEY_PERSIST = "reduce_bright_colors_persist_across_reboots"

    /** 强度键的取值范围（百分比，服务侧再映射到 deviceMin..deviceMax）。 */
    const val MIN_LEVEL = 0
    const val MAX_LEVEL = 100

    private const val SECURE = "secure"

    /** 框架 dump 出的颜色显示状态；这一节直接打印 isAvailable() 的结论 */
    private const val DUMPSYS_CMD = "dumpsys color_display"
    private const val RB_SECTION_TITLE = "Reduce bright colors"
    private const val RB_ACTIVATED = "Activated:"
    private const val RB_NOT_AVAILABLE = "Not available"

    /** 读「是否已激活」；键缺失、命令失败或输出无法解析时返回 null。 */
    fun readActivated(): Boolean? = when (readRaw(KEY_ACTIVATED)) {
        "1" -> true
        "0" -> false
        else -> null
    }

    /**
     * 读强度百分比；键缺失（输出 null）、命令失败、非数字或负数时返回 null。
     * null 表示框架按 deviceMin（最弱档）处理。
     */
    fun readLevel(): Int? = readRaw(KEY_LEVEL)?.toIntOrNull()?.takeIf { it >= MIN_LEVEL }

    /** 读「重启后保留」；取不到时返回 null。 */
    fun readPersist(): Boolean? = when (readRaw(KEY_PERSIST)) {
        "1" -> true
        "0" -> false
        else -> null
    }

    /** 写「是否激活」。不加速时框架会套用全零矩阵导致黑屏，因此这里再兜一道门禁。 */
    fun writeActivated(context: Context, activated: Boolean): Boolean {
        return !(activated && !isSupported(context)) && writeRaw(KEY_ACTIVATED, if (activated) "1" else "0")
    }

    /** 写强度百分比，越界值先夹到 0..100。 */
    fun writeLevel(level: Int): Boolean =
        writeRaw(KEY_LEVEL, level.coerceIn(MIN_LEVEL, MAX_LEVEL).toString())

    /** 写「重启后保留」；为 0 时开机把激活键复位，即极暗模式重启后失效。 */
    fun writePersist(persist: Boolean): Boolean = writeRaw(KEY_PERSIST, if (persist) "1" else "0")

    /**
     * 是否允许开启极暗模式。
     *
     * 判定必须与框架 applyTint 的门禁同源：显示服务只在
     * ReduceBrightColorsTintController.isAvailable() 为真时装配 tint 控制器，否则颜色矩阵系数全 0，
     * 此时写 activated=1 会让屏幕全黑。取值按可靠性分两条路：
     * 1. `dumpsys color_display` 里 "Reduce bright colors:" 小节自己打印的结论
     *    （"Activated:" = 可用，"Not available" = 不可用）—— 这就是 isAvailable() 的原话，
     *    root 可读，不受隐藏 API 名单影响；
     * 2. 解析 ColorDisplayManager.isColorTransformAccelerated(context)：该类不在公开 SDK 里，
     *    要先加隐藏 API 豁免，签名或豁免一变就失败，只能兜底。
     * 两条都拿不到结论时返回 false：宁可禁用，也不给黑屏留窗口。
     *
     * 会执行 shell 命令，只能在 IO 线程调用。
     */
    fun isSupported(context: Context): Boolean =
        readSupportFromDumpsys() ?: isSupportedByManager(context) ?: false

    /** 解析 dumpsys 的极暗模式小节；true/false 为框架结论，null 表示没解析出来（服务名或文案变了）。 */
    private fun readSupportFromDumpsys(): Boolean? = try {
        val lines = ShellUtils.fastCmd(DUMPSYS_CMD).lines()
        // 必须先定位到极暗模式小节：Night display / Global saturation 等小节也有 "Not available"
        val start = lines.indexOfFirst { it.contains(RB_SECTION_TITLE) }
        if (start < 0) {
            null
        } else {
            // 只看标题后三行，读数不会越过下一节标题
            val body = lines.subList(start + 1, minOf(lines.size, start + 4)).map { it.trim() }
            when {
                body.any { it.startsWith(RB_ACTIVATED) } -> true
                body.any { it.startsWith(RB_NOT_AVAILABLE) } -> false
                else -> null
            }
        }
    } catch (_: Throwable) {
        null
    }

    /** 二路兜底；异常一律返回 null，由 [isSupported] 决定最终降级结果。 */
    private fun isSupportedByManager(context: Context): Boolean? = try {
        // 隐藏 API 名单会拦截访问，先给自己加豁免；老 ROM 没有名单，豁免失败也不影响后续调用
        runCatching { HiddenApiBypass.addHiddenApiExemptions("Landroid/hardware/display/ColorDisplayManager;") }
        "android.hardware.display.ColorDisplayManager".toClass().resolve()
            .firstMethod { name = "isColorTransformAccelerated" }
            .invoke(context.applicationContext) as? Boolean
    } catch (_: Throwable) {
        null
    }

    private fun readRaw(key: String): String? = try {
        // 键不存在时 settings 输出字面量 "null"
        ShellUtils.fastCmd("settings get $SECURE $key").trim().takeIf { it.isNotEmpty() && it != "null" }
    } catch (_: Throwable) {
        null
    }

    private fun writeRaw(key: String, value: String): Boolean = try {
        ShellUtils.fastCmd("settings put $SECURE $key $value")
        // commands 成功不代表键真的落下（可能被策略拦），回读一次确认
        readRaw(key) == value
    } catch (_: Throwable) {
        false
    }
}
