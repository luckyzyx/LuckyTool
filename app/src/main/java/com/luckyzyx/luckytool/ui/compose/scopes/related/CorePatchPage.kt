package com.luckyzyx.luckytool.ui.compose.scopes.related

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.PrefCard
import com.luckyzyx.luckytool.ui.compose.components.PrefRowInset
import com.luckyzyx.luckytool.ui.compose.components.PrefSwitchRow
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode
import com.luckyzyx.luckytool.utils.ModulePrefs
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.TextButton as MiuixTextButton

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
        custom(
            key = "ColorOSCorePatchTip",
            title = c.getString(R.string.ColorOSCorePatchTip),
            // 提示卡是「非选项行」的独立卡片，用 standalone 独占一张卡片。
            // 不能用 bare：bare 是分类标题专用的裸渲染，渲染缝会给它统一 offset(-CardHorizontalInset)
            // 来抵消列表级内缩，卡片左边缘因此被推成贴屏（右侧反而多留 24dp）。
            standalone = true,
        ) {
            PrefCard(
                title = stringResource(R.string.ColorOSCorePatchTip),
                // Miuix 线：12dp 水平内缩由列表级 contentPadding 单一提供，卡片不再自带 padding；
                // material 线：列表无水平内缩，卡片自带 16dp（与 switch/list 通行内缩一致）。
                modifier = Modifier.padding(
                    horizontal = if (LocalUiMode.current == UiMode.Miuix) 0.dp else 16.dp,
                ),
            )
        }
        category(c.getString(R.string.corepatch))
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
                when (LocalUiMode.current) {
                    UiMode.Miuix -> OverlayDialog(
                        show = true,
                        onDismissRequest = { showWarning = false },
                    ) {
                        MiuixText(
                            text = stringResource(R.string.use_previous_signatures_warning),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        MiuixTextButton(
                            text = stringResource(android.R.string.ok),
                            onClick = { showWarning = false },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            colors = ButtonDefaults.textButtonColorsPrimary(),
                        )
                    }

                    UiMode.Material -> AlertDialog(
                        onDismissRequest = { showWarning = false },
                        text = { Text(stringResource(R.string.use_previous_signatures_warning)) },
                        confirmButton = {
                            TextButton(onClick = { showWarning = false }) {
                                Text(stringResource(android.R.string.ok))
                            }
                        },
                    )
                }
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
