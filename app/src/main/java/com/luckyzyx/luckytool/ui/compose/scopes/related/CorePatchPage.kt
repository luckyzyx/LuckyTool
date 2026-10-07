package com.luckyzyx.luckytool.ui.compose.scopes.related

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs

/**
 * CorePatch 页（旧 ui.fragment.scopes.related.CorePatch 的 Compose 等价物）。
 * 配置键名与默认值对齐上游 org.lsposed.corepatch。
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
        custom(key = "ColorOSCorePatchTip", title = c.getString(R.string.ColorOSCorePatchTip)) {
            ListItem { Text(c.getString(R.string.ColorOSCorePatchTip)) }
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
            ListItem(
                onClick = { apply(!checked) },
                supportingContent = { Text(c.getString(R.string.use_previous_signatures_summary)) },
                trailingContent = {
                    Switch(checked = checked, onCheckedChange = ::apply)
                },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(c.getString(R.string.use_previous_signatures)) }
            if (showWarning) {
                AlertDialog(
                    onDismissRequest = { showWarning = false },
                    text = { Text(c.getString(R.string.use_previous_signatures_warning)) },
                    confirmButton = {
                        TextButton(onClick = { showWarning = false }) {
                            Text(stringResource(android.R.string.ok))
                        }
                    },
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
