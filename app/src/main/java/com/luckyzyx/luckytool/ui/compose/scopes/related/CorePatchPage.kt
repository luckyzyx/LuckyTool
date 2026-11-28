package com.luckyzyx.luckytool.ui.compose.scopes.related

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.ConfirmDialog
import com.luckyzyx.luckytool.ui.compose.components.PrefRowInset
import com.luckyzyx.luckytool.ui.compose.components.PrefSwitchRow
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * CorePatch 页：核心破解各开关的列表。
 * 配置键名与默认值需与 org.lsposed.corepatch.Config 的常量保持一致。
 */
object CorePatchPage {

    val spec = ScopePageSpec(
        pageKey = "core_patch",
        prefsName = ModulePrefs,
        packName = "",
        scopes = arrayOf(),
        restartEnabled = false,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        // 提示不再单独占一张卡：并进分组标题下方那行说明，标题与说明同一版式（PrefCategoryHeader）
        group(
            title = c.getString(R.string.corepatch),
            summary = c.getString(R.string.ColorOSCorePatchTip),
        ) {
            switch(
                key = "downgrade",
                title = c.getString(R.string.bypass_downgrade),
                summary = c.getString(R.string.bypass_downgrade_summary),
            )
            switch(
                key = "bypass_verification",
                title = c.getString(R.string.bypass_verification),
                summary = c.getString(R.string.bypass_verification_summary),
            )
            switch(
                key = "bypass_resource_arsc_restrictions",
                title = c.getString(R.string.bypass_resource_arsc_restrictions),
                summary = c.getString(R.string.bypass_resource_arsc_restrictions_summary),
            )
            switch(
                key = "bypass_digest",
                title = c.getString(R.string.bypass_digest),
                summary = c.getString(R.string.bypass_digest_summary),
            )
            switch(
                key = "bypass_exact_sig_match",
                title = c.getString(R.string.bypass_exact_signature_match),
                summary = c.getString(R.string.bypass_exact_signature_match_summary),
            )
            custom(
                key = "use_previous_signatures",
                title = c.getString(R.string.use_previous_signatures),
                summary = c.getString(R.string.use_previous_signatures_summary),
            ) {
                val checked by state.booleanFlow("use_previous_signatures")
                    .collectAsStateWithLifecycle()
                var showWarning by remember { mutableStateOf(false) }
                fun apply(newValue: Boolean) {
                    state.set("use_previous_signatures", newValue)
                    if (newValue) showWarning = true
                }
                PrefRowInset {
                    PrefSwitchRow(
                        title = stringResource(R.string.use_previous_signatures),
                        summary = stringResource(R.string.use_previous_signatures_summary),
                        checked = checked,
                        onCheckedChange = ::apply,
                    )
                }
                if (showWarning) {
                    // 两条线的弹窗与按钮样式由 ConfirmDialog 分派；这个提示只有「确定」一个出路，
                    // 因此不画取消按钮（返回键 / 点外部仍然关掉）
                    ConfirmDialog(
                        message = stringResource(R.string.use_previous_signatures_warning),
                        title = stringResource(R.string.use_previous_signatures),
                        onConfirm = { showWarning = false },
                        onDismiss = { showWarning = false },
                        showDismissButton = false,
                    )
                }
            }
            switch(
                key = "allow_hidden_apis_for_system_apps",
                title = c.getString(R.string.allow_hidden_apis_for_system_apps),
                summary = c.getString(R.string.allow_hidden_apis_for_system_apps_summary),
            )
            switch(
                key = "bypass_shared_user",
                title = c.getString(R.string.bypass_shared_user),
                summary = c.getString(R.string.bypass_shared_user_summary),
            )
            switch(
                key = "disable_verification_agent",
                title = c.getString(R.string.disable_verification_agent),
                summary = c.getString(R.string.disable_verification_agent_summary),
            )
            switch(
                key = "bypass_block",
                title = c.getString(R.string.bypass_block),
                summary = c.getString(R.string.bypass_block_summary),
            )
        }
    }
}
