package com.luckyzyx.luckytool.ui.compose

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.components.preference.ScopeScreen
import com.luckyzyx.luckytool.ui.components.preference.ScrollTarget
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageRegistry
import com.luckyzyx.luckytool.ui.theme.LuckyAppTheme
import com.luckyzyx.luckytool.utils.PrefState
import com.luckyzyx.luckytool.utils.RestartMenuUtils
import com.luckyzyx.luckytool.utils.ThemeUtils
import com.luckyzyx.luckytool.utils.navigatePage
import com.luckyzyx.luckytool.utils.sendPrefsValue

/**
 * Compose 作用域页容器：替换旧 PreferenceFragmentCompat 页面的过渡壳。
 * 由 nav_container.xml 以 page_key 参数驱动（见 [ScopePageRegistry]）。
 * P2 起 Compose 页不再经 Fragment 导航（直接 NavHost composable 目标）。
 */
class ComposeScopeFragment : Fragment(), MenuProvider {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent { PageContent() }
    }

    @Composable
    private fun PageContent() {
        val ctx = LocalContext.current
        val spec = ScopePageRegistry[requireArguments().getString("page_key").orEmpty()]
        if (spec == null) {
            Text("Unknown page: ${requireArguments().getString("page_key")}")
            return
        }
        LuckyAppTheme {
            ScopeScreen(
                state = PrefState.of(ctx, spec.prefsName),
                sendValue = { key, value -> ctx.sendPrefsValue(spec.packName, key, value) },
                scrollTarget = ScrollTarget(
                    key = requireArguments().getString("scrollKey").orEmpty(),
                    position = requireArguments().getInt("scrollPosition", -1),
                ),
                onNavigate = { target, title ->
                    val id = ctx.resources.getIdentifier(target, "id", ctx.packageName)
                    if (id != 0) findNavController().navigatePage(id, title)
                },
                onRestart = if (spec.restartEnabled) {
                    { (activity as? MainActivity)?.restart() }
                } else {
                    null
                },
                onRefresh = spec.onRefresh,
                fullContent = spec.fullContent,
                content = spec.content,
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        val spec = ScopePageRegistry[requireArguments().getString("page_key").orEmpty()] ?: return
        if (!spec.restartEnabled) return
        menu.add(0, 1, 0, getString(R.string.menu_reboot)).apply {
            setIcon(R.drawable.ic_baseline_refresh_24)
            setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            if (ThemeUtils.isNightMode(resources.configuration)) {
                iconTintList = ColorStateList.valueOf(Color.WHITE)
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        val spec = ScopePageRegistry[requireArguments().getString("page_key").orEmpty()] ?: return false
        when (menuItem.itemId) {
            1 -> RestartMenuUtils.showRestartScopeDialog(requireActivity(), spec.scopes, true)
        }
        return true
    }
}
