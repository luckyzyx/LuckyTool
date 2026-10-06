package com.luckyzyx.luckytool.ui.compose

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.fragment.NavHostFragment
import com.highcapable.betterandroid.ui.extension.component.fragmentManager
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.fragment.scopes.XposedFragment
import com.luckyzyx.luckytool.utils.RestartMenuUtils

/**
 * Function tab：以 AndroidView 承载旧 NavHostFragment 子树（P2 过渡，P3 迁移完成后移除）。
 * TopAppBar 标题/返回/搜索/重启/版本信息对齐旧 XposedFragment 行为。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FunctionPage(
    activity: MainActivity,
    functionNavController: NavController,
    onShellBack: () -> Boolean,
) {
    val backStackEntry by functionNavController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val atRoot = destination?.id == R.id.nav_function

    // 返回键：子树优先；根时回上一 tab；无可回则退出（与旧版语义一致）
    BackHandler {
        if (!atRoot) {
            functionNavController.popBackStack()
        } else if (!onShellBack()) {
            activity.finish()
        }
    }

    // 目的地 label 形如 "{title_text}"，用导航参数替换占位符（等价旧 NavigationUI 标题解析）
    val title = destination?.let { dest ->
        val label = dest.label?.toString().orEmpty()
        val args = backStackEntry?.arguments
        if (label.contains("{")) {
            Regex("\\{(.+?)\\}").replace(label) { match ->
                args?.getString(match.groupValues[1]).orEmpty()
            }
        } else {
            label
        }
    }.orEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title.ifEmpty { stringResource(R.string.nav_function) }) },
                navigationIcon = {
                    if (!atRoot) {
                        IconButton(onClick = { functionNavController.popBackStack() }) {
                            Icon(
                                painterResource(R.drawable.ic_baseline_arrow_back_24),
                                contentDescription = null,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        XposedFragment.currentInstance?.showSearchFromShell(activity)
                    }) {
                        Icon(
                            painterResource(R.drawable.ic_baseline_search_24),
                            contentDescription = stringResource(R.string.menu_search),
                        )
                    }
                    IconButton(onClick = {
                        RestartMenuUtils.showMainRestartMenu(activity)
                    }) {
                        Icon(
                            painterResource(R.drawable.ic_baseline_refresh_24),
                            contentDescription = stringResource(R.string.menu_reboot),
                        )
                    }
                    IconButton(onClick = {
                        XposedFragment.currentInstance?.showVersionInfoFromShell(activity)
                    }) {
                        Icon(
                            painterResource(R.drawable.ic_baseline_extension_24),
                            contentDescription = stringResource(R.string.menu_versioninfo),
                        )
                    }
                },
            )
        }
    ) { padding ->
        FunctionHost(
            activity = activity,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            onReady = activity::notifyFunctionHostReady,
        )
    }
}

/**
 * 旧功能树子树宿主：负责把 MainActivity 里 headless 创建（或恢复隐藏）的 NavHostFragment
 * 收养进容器并设为 primary navigation fragment；离开 Function tab 时隐藏（状态保留在 FM）。
 */
@Composable
fun FunctionHost(
    activity: MainActivity,
    modifier: Modifier = Modifier,
    onReady: () -> Unit,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            FragmentContainerView(context).apply { id = R.id.function_nav_host }
        },
        update = { container ->
            val fm = activity.fragmentManager()
            val fragment = fm.findFragmentByTag(MainActivity.FUNCTION_NAV_TAG) as? NavHostFragment
            if (fragment == null) {
                // 首次进入 Function tab：直接在容器内创建子树
                val created = NavHostFragment.create(R.navigation.function_nav)
                fm.beginTransaction()
                    .add(container.id, created, MainActivity.FUNCTION_NAV_TAG)
                    .setPrimaryNavigationFragment(created)
                    .commitNow()
                onReady()
                return@AndroidView
            }
            val view = fragment.view
            if (view != null && view.parent === container && !fragment.isHidden) {
                onReady()
                return@AndroidView
            }
            if (fragment.isHidden || view == null) {
                // 恢复/回切：显示回容器（容器可能尚未附着，等 attach 再提交）
                fun show() {
                    fm.beginTransaction()
                        .show(fragment)
                        .setPrimaryNavigationFragment(fragment)
                        .commitNow()
                    onReady()
                }
                if (container.isAttachedToWindow) {
                    show()
                } else {
                    container.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(v: View) {
                            v.removeOnAttachStateChangeListener(this)
                            show()
                        }

                        override fun onViewDetachedFromWindow(v: View) = Unit
                    })
                }
                return@AndroidView
            }
            // headless 实例（containerId=0）首次收养进容器：保留导航状态迁移
            val state = fm.saveFragmentInstanceState(fragment)
            fm.beginTransaction().remove(fragment).commitNow()
            fragment.setInitialSavedState(state)
            fm.beginTransaction()
                .add(container.id, fragment, MainActivity.FUNCTION_NAV_TAG)
                .show(fragment)
                .setPrimaryNavigationFragment(fragment)
                .commitNow()
            onReady()
        },
    )
    // 离开 Function tab：隐藏子树（FM 保留状态；实例保存前 hide 见 MainActivity.onSaveInstanceState）
    DisposableEffect(Unit) {
        onDispose {
            val fm = activity.fragmentManager()
            val fragment = fm.findFragmentByTag(MainActivity.FUNCTION_NAV_TAG) as? NavHostFragment
            if (fragment != null && fragment.isAdded && !fragment.isHidden) {
                fm.beginTransaction()
                    .hide(fragment)
                    .setPrimaryNavigationFragment(null)
                    .commitNow()
            }
        }
    }
}
