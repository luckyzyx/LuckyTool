package com.luckyzyx.luckytool.ui.compose.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.compose.components.material.MaterialPageScaffold

/**
 * Logs 页：旧 LoggerFragment 的 fragment_logs.xml 仅一个居中占位文本
 * （"重新规划中......"），菜单四项无逻辑（onMenuItemSelected 恒 return true）。
 */
@Composable
fun LogPage() {
    MaterialPageScaffold(
        title = stringResource(R.string.nav_log),
        actions = {
            IconButton(onClick = {}) {
                Icon(
                    painterResource(R.drawable.ic_baseline_refresh_24),
                    contentDescription = null,
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    painterResource(R.drawable.baseline_filter_list_24),
                    contentDescription = null,
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    painterResource(R.drawable.ic_baseline_save_24),
                    contentDescription = null,
                )
            }
            IconButton(onClick = {}) {
                Icon(
                    painterResource(R.drawable.baseline_share_24),
                    contentDescription = null,
                )
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "重新规划中......", fontSize = 18.sp)
        }
    }
}
