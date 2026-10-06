package com.luckyzyx.luckytool.ui.compose.scopes.apps

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.components.AppPickerDialog
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
import com.luckyzyx.luckytool.utils.A14
import com.luckyzyx.luckytool.utils.AppUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.SDK
import com.luckyzyx.luckytool.utils.checkPackName
import com.luckyzyx.luckytool.utils.checkResolveActivity
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.topjohnwu.superuser.ShellUtils

/**
 * 游戏中心页（旧 ui.fragment.scopes.apps.OplusGames 的 Compose 等价物）。
 * 旧 open 菜单（fastCmd 启动 GameBoxCoverActivity）不在 Compose 页呈现。
 */
object OplusGamesPage {

    val spec = ScopePageSpec(
        pageKey = "oplus_games",
        prefsName = ModulePrefs,
        packName = "com.oplus.games",
        scopes = arrayOf("com.oplus.games", "com.oplus.cosa"),
        restartEnabled = true,
    ) {
        val c = requireNotNull(context) { "ScopeScreen 未注入 Context" }
        val appUtils = AppUtils(c)
        val appVerInfo = appUtils.getAppVerInfo("com.oplus.games")
        val isNew = (AppUtils(c).getAppVersionName("com.oplus.games")
            ?.substringBefore(".")?.toIntOrNull() ?: 10) >= 10

        fun launchShell(component: String) {
            ShellUtils.fastCmd("am start -n $component")
        }

        // 游戏助手页面入口
        if (c.checkPackName("com.oplus.games") && c.checkResolveActivity(
                Intent().setClassName(
                    "com.oplus.games", "business.compact.activity.GameBoxCoverActivity"
                )
            )
        ) {
            custom(title = c.getString(R.string.game_assistant_page)) {
                ListItem(
                    onClick = {
                        launchShell(
                            "com.oplus.games/business.compact.activity.GameBoxCoverActivity"
                        )
                    },
                    supportingContent = {
                        Text("(${appUtils.getAppLabel("com.oplus.games")})")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(c.getString(R.string.game_assistant_page)) }
            }
        }
        // 游戏空间页面入口
        if (c.checkPackName("com.nearme.gamecenter") && c.checkResolveActivity(
                Intent().setClassName(
                    "com.nearme.gamecenter",
                    "com.nearme.gamespace.desktopspace.ui.DesktopSpaceMainActivity"
                )
            )
        ) {
            custom(title = c.getString(R.string.game_space_page)) {
                ListItem(
                    onClick = {
                        launchShell(
                            "com.nearme.gamecenter/" +
                                "com.nearme.gamespace.desktopspace.ui.DesktopSpaceMainActivity"
                        )
                    },
                    supportingContent = {
                        Text("(${appUtils.getAppLabel("com.nearme.gamecenter")})")
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(c.getString(R.string.game_space_page)) }
            }
        }
        // 布局
        category(c.getString(R.string.OplusGamesLayout))
        switch(
            key = "remove_startup_animation",
            title = c.getString(R.string.remove_startup_animation),
        )
        if (!isNew) {
            switch(
                key = "remove_welfare_page",
                title = c.getString(R.string.remove_welfare_page),
            )
        }
        if (appVerInfo?.versionCode?.let { it < 90000000 } ?: false) {
            switch(
                key = "remove_tool_recommendation_card",
                title = c.getString(R.string.remove_tool_recommendation_card),
            )
        }
        // 工具
        category(c.getString(R.string.OplusGamesTool))
        switch(
            key = "remove_root_check",
            title = c.getString(R.string.remove_root_check),
            summary = c.getString(R.string.remove_root_check_summary),
        )
        switch(
            key = "remove_some_vip_limit",
            title = c.getString(R.string.remove_some_vip_limit),
            summary = c.getString(R.string.remove_some_vip_limit_summary),
        )
        switch(
            key = "enable_developer_page",
            title = c.getString(R.string.enable_developer_page),
            summary = c.getString(R.string.enable_developer_page_summary),
            onChange = { restart?.invoke() },
        )
        if (state.getBoolean("enable_developer_page")) {
            if (c.checkPackName("com.oplus.games")) {
                custom(title = c.getString(R.string.game_assistant_develop_page)) {
                    ListItem(
                        onClick = {
                            launchShell(
                                "com.oplus.games/" +
                                    "business.compact.activity.GameDevelopOptionsActivity"
                            )
                        },
                        supportingContent = {
                            Text("(${appUtils.getAppLabel("com.oplus.games")})")
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(c.getString(R.string.game_assistant_develop_page)) }
                }
            }
        }
        custom(
            key = "custom_media_player_support_list",
            title = c.getString(R.string.custom_media_player_support),
        ) {
            val saved by state.stringSetFlow("custom_media_player_support_list")
                .collectAsStateWithLifecycle()
            var show by remember { mutableStateOf(false) }
            ListItem(
                onClick = { show = true },
                supportingContent = { Text(saved.toString()) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(c.getString(R.string.custom_media_player_support)) }
            if (show) {
                AppPickerDialog(
                    title = c.getString(R.string.custom_media_player_support),
                    multiMode = true,
                    onDismiss = { show = false },
                    onConfirm = { apps ->
                        state.set(
                            "custom_media_player_support_list",
                            apps.map { it.packageName }.toSet(),
                        )
                        restart?.invoke()
                    },
                )
            }
        }
        custom(
            key = "custom_barrage_notification_whitelist_list",
            title = c.getString(R.string.custom_barrage_notification_whitelist),
        ) {
            val saved by state
                .stringSetFlow("custom_barrage_notification_whitelist_list")
                .collectAsStateWithLifecycle()
            var show by remember { mutableStateOf(false) }
            ListItem(
                onClick = { show = true },
                supportingContent = { Text(saved.toString()) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(c.getString(R.string.custom_barrage_notification_whitelist)) }
            if (show) {
                AppPickerDialog(
                    title = c.getString(R.string.custom_barrage_notification_whitelist),
                    multiMode = true,
                    onDismiss = { show = false },
                    onConfirm = { apps ->
                        state.set(
                            "custom_barrage_notification_whitelist_list",
                            apps.map { it.packageName }.toSet(),
                        )
                        restart?.invoke()
                    },
                )
            }
        }
        if (getOSVersionCode >= 27) {
            switch(
                key = "enable_game_run_in_background",
                title = c.getString(R.string.enable_run_in_background),
            )
        }
        if (!isNew) {
            switch(
                key = "enable_game_ai_play",
                title = c.getString(R.string.enable_game_ai_play),
            )
        }
        if (SDK < A14) {
            switch(
                key = "remove_danmaku_notification_whitelist",
                title = c.getString(R.string.remove_danmaku_notification_whitelist),
            )
        }
        if (!isNew) {
            switch(
                key = "remove_game_voice_changer_whitelist",
                title = c.getString(R.string.remove_game_voice_changer_whitelist),
            )
        }
        switch(
            key = "remove_game_assistant_temperature_detection",
            title = c.getString(R.string.remove_game_assistant_temperature_detection),
            summary = c.getString(R.string.remove_game_assistant_temperature_detection_summary),
        )
        switch(
            key = "enable_support_competition_mode",
            title = c.getString(R.string.enable_support_competition_mode),
        )
        switch(
            key = "remove_competition_mode_sound",
            title = c.getString(R.string.remove_competition_mode_sound),
        )
        switch(
            key = "enable_x_mode_feature",
            title = c.getString(R.string.enable_x_mode_feature),
        )
        if (!isNew) {
            switch(
                key = "enable_gt_mode_feature",
                title = c.getString(R.string.enable_gt_mode_feature),
            )
            switch(
                key = "enable_one_plus_characteristic",
                title = c.getString(R.string.enable_one_plus_characteristic),
            )
            switch(
                key = "enable_adreno_gpu_controller",
                title = c.getString(R.string.enable_adreno_gpu_controller),
            )
            switch(
                key = "enable_increase_fps_limit_feature",
                title = c.getString(R.string.enable_increase_fps_limit_feature),
            )
            switch(
                key = "enable_increase_fps_feature",
                title = c.getString(R.string.enable_increase_fps_feature),
            )
            switch(
                key = "enable_optimise_power_feature",
                title = c.getString(R.string.enable_optimise_power_feature),
            )
            switch(
                key = "enable_super_resolution_feature",
                title = c.getString(R.string.enable_super_resolution_feature),
            )
        }
    }
}
