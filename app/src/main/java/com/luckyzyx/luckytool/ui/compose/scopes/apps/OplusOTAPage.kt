package com.luckyzyx.luckytool.ui.compose.scopes.apps

import android.os.SystemProperties
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.drake.net.utils.scopeLife
import com.drake.net.utils.withDefault
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.PrefRow
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.CommandUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.formatStringAuto
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.topjohnwu.superuser.ShellUtils

/**
 * 旧 ui.fragment.scopes.apps.OplusOTA 的 Compose 等价物（机械翻译 loadPreferences）。
 * 旧 restore_ota_update_verity 条目 isVisible=false 从未展示，按规则丢弃。
 */
object OplusOTAPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_ota",
        prefsName = ModulePrefs,
        packName = "com.oplus.ota",
        scopes = arrayOf("com.oplus.ota"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        val osCode = getOSVersionCode
        switch(
            key = "remove_ota_notify_install_success",
            title = c.getString(R.string.remove_ota_notify_install_success),
        )
        switch(
            key = "remove_ota_auto_download_dialog",
            title = c.getString(R.string.remove_ota_auto_download_dialog),
        )
        category("OTA")
        // 纯信息型（旧 Preference 无点击）：展示当前 OTA 校验结果
        // shell 调用只在渲染期执行：headless 索引构建不得执行任何 IO
        val verifyTitle = c.getString(R.string.get_ota_verify_result)
        custom(key = "get_ota_verify_result", title = verifyTitle) {
            val verifySummary = c.getString(
                R.string.get_ota_verify_result_summary,
                formatStringAuto(
                    runCatching {
                        ShellUtils.fastCmd("${CommandUtils.getprop} ${CommandUtils.otaVerifyResult}")
                            .split(",")
                    }.getOrDefault(emptyList()),
                    ",", false,
                ),
            )
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                PrefRow(
                    title = verifyTitle,
                    summary = verifySummary,
                )
            }
        }
        // 解锁本地升级（原 Preference 点击执行 shell 命令序列）
        custom(
            key = "unlock_local_upgrade",
            title = c.getString(R.string.unlock_local_upgrade),
            summary = c.getString(R.string.unlock_local_upgrade_summary),
        ) {
            val lifecycleOwner = LocalLifecycleOwner.current
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                PrefRow(
                    title = c.getString(R.string.unlock_local_upgrade),
                    summary = c.getString(R.string.unlock_local_upgrade_summary),
                    onClick = {
                        lifecycleOwner.scopeLife {
                            val command = arrayOf(
                                "settings put global development_settings_enabled 1",
                                "pm clear com.oplus.ota",
                                "settings put global airplane_mode_on 1",
                                "am broadcast --user all -a android.intent.action.AIRPLANE_MODE --ez 'state' 'true'",
                                "am start com.oplus.ota/com.oplus.otaui.activity.EntryActivity"
                            )
                            withDefault { ShellUtils.fastCmd(*command) }
                        }
                    },
                )
            }
        }
        switch(
            key = "remove_ota_local_update_verity",
            title = c.getString(R.string.remove_ota_local_update_verity),
        )
        if (osCode >= 30 && SystemProperties.getBoolean("oplus.opex.merge", false)) {
            switch(
                key = "enable_opex_local_install",
                title = c.getString(R.string.enable_opex_local_install),
            )
        }
        switch(
            key = "disable_dm_verity_verification",
            title = c.getString(R.string.disable_dm_verity_verification),
        )
        if (osCode < 37) {
            page(
                title = c.getString(R.string.extract_ota_information),
                target = "extractOTAFragment",
                summary = c.getString(R.string.extract_ota_information_summary),
            )
        }
    }
}
