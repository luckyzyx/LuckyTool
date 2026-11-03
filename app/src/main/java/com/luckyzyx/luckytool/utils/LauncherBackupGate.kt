package com.luckyzyx.luckytool.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * 自动备份的到期闸门（纯逻辑 + 极薄 IO，只为能在单测里直接验 [autoBackupDue]）。
 *
 * 为什么需要它：自动备份由桌面进程自查（onHook / 布局加载完成 / 固定间隔重排），需要判断这一轮该不该备份。
 * 如果只看「进程起来了就备份」，每次桌面进程重启（作用域重装、开机、桌面被系统杀掉）都会多做一份快照，
 * 还会顺带触发 toast 和隐藏应用同步这些副作用。所以到期判定统一压在一份宿主（launcher）自己的
 * shared_prefs 时间戳上：桌面进程重启只是重新读一份记录，不是到期理由。
 *
 * 只允许在 launcher 进程（hook 侧）读写这份 prefs，app 进程永远不碰：
 *  - app 进程的 prefs 目录是它自己的 uid（/data/user/0/<module-pkg>），而桌面数据在桌面的 uid 下，
 *    app 读了也没有意义；反过来 hook 在桌面进程内以桌面 uid 读写自己目录，权限天然成立。
 *  - 备份/恢复本体在桌面进程执行（LauncherBackupHooker），时间戳跟着执行者走才不会出现两个真源。
 *
 * 所有读写一律 runCatching 包裹：读不到就返回 0，绝不抛异常（hook 抛异常会连累桌面进程）。
 */
object LauncherBackupGate {

    /** 宿主（桌面进程）自己的 prefs 文件名；不出现在任何 app 侧代码里 */
    private const val PREFS_NAME = "luckytool_launcher_backup"

    /** 上次成功备份的时间戳（毫秒，0 表示从未成功过） */
    private const val KEY_LAST_SUCCESS_MS = "auto_last_success_ms"

    /** 上次尝试备份的时间戳（毫秒，0 表示从未尝试过） */
    private const val KEY_LAST_ATTEMPT_MS = "auto_last_attempt_ms"

    /** 两次尝试之间至少间隔 30 分钟：失败或桌面没响应时不要反复重试 */
    const val DEFAULT_RETRY_GAP_MS = 30 * 60 * 1000L

    /** 媒体 provider 不可用（ContentResolver 抛 Unknown URL）后的重试间隔：5 分钟后再试一次 */
    const val PROVIDER_RETRY_DELAY_MS = 5 * 60 * 1000L

    /** 媒体 provider 不可用后的重试次数：首轮之外再试 3 次，间隔都是 [PROVIDER_RETRY_DELAY_MS] */
    const val PROVIDER_RETRY_TIMES = 3

    /** 时钟回拨的容差：now 比记录小 1 分钟以上才当回拨，避免正常的时钟抖动判成回拨 */
    private const val CLOCK_ROLLBACK_TOLERANCE_MS = 60 * 1000L

    private const val HOUR_MS = 3600 * 1000L

    /**
     * 是否到期该做一次自动备份。
     *
     * 两个条件同时成立才算到期：
     *  1) 到期：从未成功过，或距离上次成功已经满一个周期；
     *  2) 未节流：从未尝试过，或距离上次尝试已经超过 [retryGapMs]。
     *
     * 节流只在「已经到期」的前提下起作用：上次尝试超过 30 分钟但周期还没到，仍然不备份，
     * 否则 30 分钟一到就会变成按 30 分钟备份一次，把用户设的周期彻底架空。
     *
     * 时钟回拨（now 明显早于记录）按到期处理：否则在用户把时间调回去之后，要等时钟重新追上
     * 那个未来的时间戳才会恢复备份，用户会以为自动备份坏了。传 [retryGapMs] = 0 可只判到期不判节流
     * （用于「这一轮由调用方自己判定该备份」、不需要再等 30 分钟节流的场景）。
     */
    fun autoBackupDue(
        now: Long,
        lastSuccess: Long,
        lastAttempt: Long,
        intervalHours: Int,
        retryGapMs: Long = DEFAULT_RETRY_GAP_MS,
    ): Boolean {
        //周期非法（关闭 / 脏值）时永远不到期，交给界面把周期改回合法值
        if (intervalHours <= 0) return false
        if (clockRolledBack(now, lastSuccess, lastAttempt)) return true
        val expired = lastSuccess <= 0L || now - lastSuccess >= intervalHours * HOUR_MS
        if (!expired) return false
        return lastAttempt <= 0L || now - lastAttempt >= retryGapMs
    }

    /**
     * 这次失败值不值得隔 [PROVIDER_RETRY_DELAY_MS] 再试一次。
     *
     * 只认「媒体 provider 不可用」这一类：ContentResolver 在 acquireProvider 返回 null 时抛
     * IllegalArgumentException("Unknown URL content://media/...")，跟 uri 怎么写无关，
     * 说明媒体 provider 还没注册好或正在重启，属于延迟可恢复的失败。
     * 权限 / 磁盘 / 找不到 launcher.db 这类确定性失败，重试多少次结果都一样，不参与重试。
     */
    fun isProviderUnavailable(throwable: Throwable?): Boolean =
        throwable is IllegalArgumentException &&
            throwable.message.orEmpty().startsWith(PROVIDER_UNAVAILABLE_PREFIX)

    /**
     * 多种落盘写法聚合后的失败文本里是否含 provider 不可用原文。
     * 聚合后类型是 IllegalStateException，靠类型判不出来，只能看文本。
     */
    fun mentionsProviderUnavailable(detail: String): Boolean =
        detail.contains(PROVIDER_UNAVAILABLE_PREFIX)

    /** ContentResolver 在 acquireProvider 返回 null 时的固定前缀 */
    private const val PROVIDER_UNAVAILABLE_PREFIX = "Unknown URL"

    /** 还有 [retriesLeft] 次机会时返回下次重试的延迟毫秒数；用尽了返回 null（调用方据此收尾） */
    fun providerRetryDelayMs(retriesLeft: Int): Long? =
        if (retriesLeft > 0) PROVIDER_RETRY_DELAY_MS else null

    private fun clockRolledBack(now: Long, lastSuccess: Long, lastAttempt: Long): Boolean =
        (lastSuccess > 0L && now + CLOCK_ROLLBACK_TOLERANCE_MS < lastSuccess) ||
            (lastAttempt > 0L && now + CLOCK_ROLLBACK_TOLERANCE_MS < lastAttempt)

    /** 上次成功备份时间；没有记录或读失败都返回 0 */
    fun readLastSuccess(context: Context): Long = read(context, KEY_LAST_SUCCESS_MS)

    /** 上次尝试备份时间；没有记录或读失败都返回 0 */
    fun readLastAttempt(context: Context): Long = read(context, KEY_LAST_ATTEMPT_MS)

    /** 记一次尝试（真正开始备份之前）：失败/超时后靠它把下一次压到 [retryGapMs] 之后 */
    fun markAttempt(context: Context, at: Long = System.currentTimeMillis()) {
        write(context) { editor -> editor.putLong(KEY_LAST_ATTEMPT_MS, at) }
    }

    /** 记一次成功：同时刷新尝试时间，保证成功之后节流窗口重新开始计时 */
    fun markSuccess(context: Context, at: Long = System.currentTimeMillis()) {
        write(context) { editor ->
            editor.putLong(KEY_LAST_SUCCESS_MS, at)
            editor.putLong(KEY_LAST_ATTEMPT_MS, at)
        }
    }

    private fun read(context: Context, key: String): Long = runCatching {
        prefs(context)?.getLong(key, 0L) ?: 0L
    }.getOrDefault(0L)

    private fun write(context: Context, block: (SharedPreferences.Editor) -> Unit) {
        runCatching {
            val prefs = prefs(context) ?: return@runCatching
            val editor = prefs.edit()
            block(editor)
            editor.commit()
        }
    }

    /** 宿主 prefs；取不到返回 null（调用方按「没有记录」处理） */
    private fun prefs(context: Context): SharedPreferences? =
        runCatching { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }.getOrNull()
}
