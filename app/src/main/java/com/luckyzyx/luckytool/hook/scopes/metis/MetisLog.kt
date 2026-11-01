package com.luckyzyx.luckytool.hook.scopes.metis

import com.highcapable.yukihookapi.hook.param.PackageParam
import com.luckyzyx.luckytool.utils.LogUtils
import org.lsposed.lsparanoid.Obfuscate

/**
 * 流体云 hook 的统一日志出口。
 *
 * 分三档，用途不同，别混用：
 * - info：装载结果、跳过进程这类一次性事实，直接记；
 * - debug：每次点击都打，受 metis_debug_log 控制，排查卡片命中哪条规则时用；
 * - warn：缺类、反射失败等异常，始终记录 —— 开关关着时用户看不到原因就无法反馈问题。
 */
@Obfuscate
object MetisLog {
    private const val TAG = "Metis"

    fun info(method: String, msg: String) = LogUtils.i(TAG, method, msg)

    fun debug(param: PackageParam, method: String, msg: String) {
        if (MetisSettings.debugLog(param)) LogUtils.i(TAG, method, msg)
    }

    fun warn(method: String, msg: String) = LogUtils.w(TAG, method, msg, send = true)
}
