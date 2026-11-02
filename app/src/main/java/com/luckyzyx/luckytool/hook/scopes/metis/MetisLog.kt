package com.luckyzyx.luckytool.hook.scopes.metis

import com.luckyzyx.luckytool.utils.LogUtils
import org.lsposed.lsparanoid.Obfuscate

/**
 * 流体云 hook 的统一日志出口。
 *
 * 分两档，用途不同，别混用：
 * - info：装载结果、跳过进程这类一次性事实，只在 debug 构建里出现；
 * - warn：缺类、反射失败等异常，始终记录 —— 用户报「点了没反应」时这是唯一的线索。
 */
@Obfuscate
object MetisLog {
    private const val TAG = "Metis"

    fun info(method: String, msg: String) = LogUtils.i(TAG, method, msg)

    fun warn(method: String, msg: String) = LogUtils.w(TAG, method, msg, send = true)
}
