package com.luckyzyx.luckytool.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import com.luckyzyx.luckytool.ui.compose.ActivationLoadingLayer
import com.luckyzyx.luckytool.ui.compose.AppRoute
import com.luckyzyx.luckytool.ui.compose.ScopePageHost
import com.luckyzyx.luckytool.ui.compose.ScopeRoute
import com.luckyzyx.luckytool.ui.compose.ThemeRoute
import com.luckyzyx.luckytool.ui.compose.components.RootScaffold
import com.luckyzyx.luckytool.ui.compose.pages.ThemeScreen
import com.luckyzyx.luckytool.ui.service.XposedServiceBridge
import com.luckyzyx.luckytool.ui.service.awaitActivation
import com.luckyzyx.luckytool.ui.theme.AppTheme
import com.luckyzyx.luckytool.utils.PermissionUtils
import com.luckyzyx.luckytool.utils.ThemeUtils
import kotlinx.serialization.json.Json

/** 二级页使用系统 Activity 栈；不注册返回回调或覆盖窗口转场。 */
class PageActivity : AppCompatActivity() {
    private var openingPage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtils.initTheme(this)
        val route = readRoute(intent) ?: run {
            finish()
            return
        }
        setContent {
            // 进程死亡后直接恢复二级页时，也要等待 remote prefs 服务重新连接。
            var ready by remember { mutableStateOf(XposedServiceBridge.isModuleActive.value) }
            LaunchedEffect(Unit) {
                if (!ready) {
                    XposedServiceBridge.isModuleActive.awaitActivation(3000L)
                    ready = true
                }
            }
            AppTheme {
                if (!ready) {
                    ActivationLoadingLayer()
                } else {
                    RootScaffold { padding ->
                        Box(Modifier.fillMaxSize().padding(padding)) {
                            when (route) {
                                ThemeRoute -> ThemeScreen(onBack = ::finish)
                                is ScopeRoute -> ScopePageHost(
                                    route = route,
                                    onBack = ::finish,
                                    onNavigate = { key, title ->
                                        open(this@PageActivity, ScopeRoute(key, title.orEmpty()))
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        openingPage = false
    }

    override fun onDestroy() {
        PermissionUtils.onHostDestroyed(this)
        super.onDestroy()
    }

    companion object {
        private const val EXTRA_ROUTE = "com.luckyzyx.luckytool.PAGE_ROUTE"

        internal fun createIntent(activity: Activity, route: AppRoute): Intent =
            Intent(activity, PageActivity::class.java)
                .putExtra(EXTRA_ROUTE, Json.encodeToString<AppRoute>(route))

        internal fun readRoute(intent: Intent): AppRoute? =
            intent.getStringExtra(EXTRA_ROUTE)?.let { encoded ->
                runCatching { Json.decodeFromString<AppRoute>(encoded) }.getOrNull()
            }

        internal fun open(activity: AppCompatActivity, route: AppRoute) {
            if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
            if (activity is PageActivity) {
                if (activity.openingPage || readRoute(activity.intent) == route) return
                activity.openingPage = true
            }
            activity.startActivity(createIntent(activity, route))
        }
    }
}
