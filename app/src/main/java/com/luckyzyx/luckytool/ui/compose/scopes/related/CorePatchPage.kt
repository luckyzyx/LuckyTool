package com.luckyzyx.luckytool.ui.compose.scopes.related

import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.dialogCentered

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
        switch(
            key = "use_previous_signatures",
            title = c.getString(R.string.use_previous_signatures),
            summary = c.getString(R.string.use_previous_signatures_summary),
            onChange = { newValue ->
                if (newValue) {
                    MaterialAlertDialogBuilder(c, dialogCentered).apply {
                        setMessage(R.string.use_previous_signatures_warning)
                        setPositiveButton(android.R.string.ok, null)
                        show()
                    }
                }
            },
        )
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
