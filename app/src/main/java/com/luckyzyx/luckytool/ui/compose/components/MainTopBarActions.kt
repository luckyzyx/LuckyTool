package com.luckyzyx.luckytool.ui.compose.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.composables.icons.lucide.R as LucideR
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode

/** 一级功能页顶栏：重启与版本信息各自成键，搜索已独立成一整个标签页，不再放在顶栏。 */
@Composable
internal fun FunctionTopBarActions(
    onRestart: () -> Unit,
    onVersionInfo: () -> Unit,
) {
    if (LocalUiMode.current == UiMode.Coui) {
        TopBarActionButton(
            onClick = onRestart,
            contentDescription = stringResource(R.string.menu_reboot),
            painter = painterResource(LucideR.drawable.lucide_ic_power),
        )
        TopBarActionButton(
            onClick = onVersionInfo,
            contentDescription = stringResource(R.string.menu_versioninfo),
            painter = painterResource(LucideR.drawable.lucide_ic_info),
        )
    } else {
        TopBarActionButton(
            onClick = onRestart,
            contentDescription = stringResource(R.string.menu_reboot),
            materialIcon = Icons.Filled.PowerSettingsNew,
        )
        TopBarActionButton(
            onClick = onVersionInfo,
            contentDescription = stringResource(R.string.menu_versioninfo),
            materialIcon = Icons.Outlined.Info,
        )
    }
}

@Composable
internal fun HomeTopBarActions(onRestart: () -> Unit, onAbout: () -> Unit) {
    if (LocalUiMode.current == UiMode.Coui) {
        TopBarActionButton(
            onClick = onRestart,
            contentDescription = stringResource(R.string.menu_reboot),
            painter = painterResource(LucideR.drawable.lucide_ic_power),
        )
        TopBarActionButton(
            onClick = onAbout,
            contentDescription = stringResource(R.string.about_title),
            painter = painterResource(LucideR.drawable.lucide_ic_info),
        )
    } else {
        TopBarActionButton(
            onClick = onRestart,
            contentDescription = stringResource(R.string.menu_reboot),
            materialIcon = Icons.Filled.PowerSettingsNew,
        )
        TopBarActionButton(
            onClick = onAbout,
            contentDescription = stringResource(R.string.about_title),
            materialIcon = Icons.Outlined.Info,
        )
    }
}

@Composable
internal fun OtherTopBarActions(onOptimize: () -> Unit) {
    if (LocalUiMode.current == UiMode.Coui) {
        TopBarActionButton(
            onClick = onOptimize,
            contentDescription = stringResource(R.string.action_optimize_apps),
            painter = painterResource(LucideR.drawable.lucide_ic_sliders_horizontal),
        )
    } else {
        TopBarActionButton(
            onClick = onOptimize,
            contentDescription = stringResource(R.string.action_optimize_apps),
            materialIcon = Icons.Outlined.Tune,
        )
    }
}
