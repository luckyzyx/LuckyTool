package com.luckyzyx.luckytool.ui.fragment.base

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.RippleDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.core.view.MenuProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup.PreferencePositionCallback
import androidx.recyclerview.widget.RecyclerView
import com.highcapable.betterandroid.ui.extension.component.runDelayed
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.PrefsItem
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.utils.LogUtils
import com.luckyzyx.luckytool.utils.RemotePreferenceDataStore
import com.luckyzyx.luckytool.utils.RestartMenuUtils
import com.luckyzyx.luckytool.utils.ThemeUtils
import com.luckyzyx.luckytool.utils.appPrefs
import com.luckyzyx.luckytool.utils.checkPackName
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.getOSVersionName
import com.luckyzyx.luckytool.utils.navigatePage
import com.luckyzyx.luckytool.utils.safeOfNull
import com.luckyzyx.luckytool.utils.setupMenuProvider
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
@Suppress("unused")
abstract class BaseScopePreferenceFeagment : PreferenceFragmentCompat(), MenuProvider {

    /**
     * @see [getOSVersionName]
     */
    val osName = getOSVersionName

    /**
     * @see [getOSVersionCode]
     */
    val osCode = getOSVersionCode

    /**
     * 相关作用域
     */
    open val scopes = arrayOf<String>()

    /**
     * 是否启用重启菜单
     */
    open val isEnableRestartMenu: Boolean = false

    /**
     * 是否启用跳转菜单
     */
    open val isEnableOpenMenu: Boolean = false

    /**
     * 当前Prefs存储名称
     */
    abstract val currentPrefsName: String

    /**
     * 跳转Action
     */
    abstract val navigateFragmentId: Int

    abstract fun Context.loadRootPreference(): Preference

    abstract fun Context.loadPreferences(): ArrayList<Preference>

    open val allPrefsItems = arrayListOf<PrefsItem>()

    fun getRootPreference(context: Context) = context.loadRootPreference().apply {
        allPrefsItems.clear()
        allPrefsItems.addAll(getAllPrefsItem(context))

        isVisible = isVisible && allPrefsItems.any { it.isVisible == true }
        setOnPreferenceClickListener {
            val navController = when (context) {
                is MainActivity -> context.navController
                else -> safeOfNull { findNavController() }
            }
            val bundle = Bundle().apply {
                if (!title.isNullOrBlank()) putCharSequence("title_text", title)
            }
            navController?.navigatePage(navigateFragmentId, bundle)
            true
        }
    }

    fun getAllPrefsItem(context: Context): ArrayList<PrefsItem> {
        return ArrayList<PrefsItem>().apply {
            if (scopes.size == 1 && scopes.first() != "system" && !context.checkPackName(scopes.first())) return@apply
            val rootPreference = context.loadRootPreference()
            context.loadPreferences().forEachIndexed { index, preference ->
                if (preference is PreferenceCategory) return@forEachIndexed
                val item = PrefsItem(
                    preference,
                    index,
                    preference.key,
                    preference.icon,
                    preference.title,
                    preference.summary,
                    preference.isVisible,
                    rootPreference.title,
                    rootPreference.summary,
                    navigateFragmentId
                )
                add(item)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenuProvider(this)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        if (currentPrefsName.isNotBlank()) preferenceManager.preferenceDataStore =
            RemotePreferenceDataStore(requireContext().appPrefs(currentPrefsName))
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            context.loadPreferences().forEachIndexed { index, preference ->
                try {
                    addPreference(preference)
                } catch (t: Throwable) {
                    LogUtils.e(
                        "$javaClass loadPreferences",
                        "$index | ${preference.key} | ${preference.title}",
                        "$t",
                        true
                    )
                }
            }
        }

        arguments?.apply {
            val scrollKey = getString("scrollKey", "")
            val scrollPosition = getInt("scrollPosition", -1)
            lifecycleScope.runDelayed(200) {
                highLight(scrollKey, scrollPosition)
                remove("scrollKey")
                remove("scrollPosition")
            }
        }
    }

    private fun highLight(scrollKey: String, scrollPosition: Int) {
        val recyclerView = listView ?: return
        val adapter = recyclerView.adapter ?: return

        val preference = if (scrollKey.isBlank()) {
            if (scrollPosition == -1) return
            val preference = preferenceScreen.getPreference(scrollPosition)
            scrollToPreference(preference)
            preference
        } else {
            scrollToPreference(scrollKey)
            findPreference(scrollKey) ?: return
        }

        if (adapter is PreferencePositionCallback) {
            val position = adapter.getPreferenceAdapterPosition(preference)
            if (position != RecyclerView.NO_POSITION) {
                recyclerView.postDelayed({
                    val holder = recyclerView.findViewHolderForAdapterPosition(position)
                    if (holder != null) {
                        val background = holder.itemView.background
                        if (background is RippleDrawable) {
                            forceRippleAnimation(background)
                        }
                    }
                }, 300)
            }
        }
    }

    private fun forceRippleAnimation(background: RippleDrawable) {
        background.setState(
            intArrayOf(android.R.attr.state_pressed, android.R.attr.state_enabled)
        )
        lifecycleScope.runDelayed(300) {
            background.setState(intArrayOf())
        }
    }

    /**
     * 自定义跳转菜单点击事件
     * @return Unit
     */
    open fun callOpenMenu() {}

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        if (isEnableRestartMenu) menu.add(0, 1, 0, getString(R.string.menu_reboot)).apply {
            setIcon(R.drawable.ic_baseline_refresh_24)
            setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            if (ThemeUtils.isNightMode(resources.configuration)) {
                iconTintList = ColorStateList.valueOf(Color.WHITE)
            }
        }
        if (isEnableOpenMenu) menu.add(0, 2, 0, getString(R.string.open)).apply {
            setIcon(R.drawable.baseline_open_in_new_24)
            setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            if (ThemeUtils.isNightMode(resources.configuration)) {
                iconTintList = ColorStateList.valueOf(Color.WHITE)
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            1 -> RestartMenuUtils.showRestartScopeDialog(requireActivity(), scopes, true)
            2 -> callOpenMenu()
        }
        return true
    }

}