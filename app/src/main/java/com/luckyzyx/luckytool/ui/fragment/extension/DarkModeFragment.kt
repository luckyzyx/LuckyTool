package com.luckyzyx.luckytool.ui.fragment.extension

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.ArrayMap
import android.util.ArraySet
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.drake.net.utils.scopeLife
import com.drake.net.utils.withDefault
import com.google.android.material.materialswitch.MaterialSwitch
import com.highcapable.betterandroid.ui.component.adapter.factory.bindAdapter
import com.highcapable.betterandroid.ui.component.adapter.recycler.factory.notifyDataSetChangedIgnore
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.AppInfo
import com.luckyzyx.luckytool.data.DarkModeInfo
import com.luckyzyx.luckytool.databinding.FragmentDarkModeApplistLayoutBinding
import com.luckyzyx.luckytool.databinding.LayoutAppinfoSwitchItemDarkmodeBinding
import com.luckyzyx.luckytool.selector.SortFilterBottomSheetDialog
import com.luckyzyx.luckytool.ui.fragment.base.BaseFragment
import com.luckyzyx.luckytool.utils.IntentUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.PackageUtils
import com.luckyzyx.luckytool.utils.RestartMenuUtils
import com.luckyzyx.luckytool.utils.ThemeUtils
import com.luckyzyx.luckytool.utils.getBoolean
import com.luckyzyx.luckytool.utils.getStringSet
import com.luckyzyx.luckytool.utils.putBoolean
import com.luckyzyx.luckytool.utils.putStringSet
import com.luckyzyx.luckytool.utils.safeOfNull
import com.luckyzyx.luckytool.utils.sendPrefsKey
import com.luckyzyx.luckytool.utils.sendPrefsValue
import com.luckyzyx.luckytool.utils.setupMenuProvider
import kotlinx.serialization.json.Json
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class DarkModeFragment : BaseFragment<FragmentDarkModeApplistLayoutBinding>(), MenuProvider {

    private val TAG = "DarkModeFragment"

    private var allAppInfos = ArrayList<AppInfo>()
    private var filterAppInfos = ArrayList<AppInfo>()
    private val allEnabledInfos = ArrayMap<String, DarkModeInfo>()

    private val scopes = arrayOf("com.android.settings")

    private val enableSwitchKey = "dark_mode_list_enable"
    private val supportListKey = "dark_mode_support_list"

    private var isReverse = false
    private var sortMode = 0

    private lateinit var sortFilterBottomSheetDialog: SortFilterBottomSheetDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        setupMenuProvider(this)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sortFilterBottomSheetDialog = SortFilterBottomSheetDialog(requireActivity()).apply {
            setReverse(true) { _, isChecked ->
                isReverse = isChecked
                loadData()
            }
            setSortChips(
                true, context.resources.getStringArray(R.array.sort_selector_chips)
            ) { _, checkedIds ->
                sortMode = checkedIds.firstOrNull() ?: 0
                loadData()
            }
        }
        binding.enableSwitch.apply {
            text = context.getString(R.string.enable_dark_mode_list)
            isChecked = context.getBoolean(ModulePrefs, enableSwitchKey, false)
            setOnCheckedChangeListener { buttonView, isChecked ->
                if (buttonView.isPressed) {
                    context.putBoolean(ModulePrefs, enableSwitchKey, isChecked)
                    context.sendPrefsValue("android", enableSwitchKey, isChecked)
                }
            }
        }

        binding.searchViewLayout.apply {
            hint = "Name / PackageName"
            setEndIconOnClickListener {
                sortFilterBottomSheetDialog.show()
            }
        }
        binding.searchView.apply {
            addTextChangedListener(onTextChanged = { text: CharSequence?, _: Int, _: Int, _: Int ->
                val query = text?.toString() ?: ""
                filterAppInfos = if (query.isBlank()) allAppInfos
                else {
                    val newList = allAppInfos.filter {
                        it.name.lowercase().contains(query)
                                || it.packageName.lowercase().contains(query)
                    }
                    ArrayList(newList)
                }
                binding.recyclerView.adapter?.notifyDataSetChangedIgnore()
            })
        }
        binding.swipeRefreshLayout.apply {
            setOnRefreshListener {
                loadData()
            }
        }

        binding.recyclerView.apply {
            adapter = bindAdapter<AppInfo> {
                onBindData { filterAppInfos }
                onBindItemView<LayoutAppinfoSwitchItemDarkmodeBinding> { item, appInfo, _ ->
                    item.appIcon.setImageDrawable(appInfo.icon)
                    item.appName.text = appInfo.name
                    item.packName.text = appInfo.packageName

                    val data = allEnabledInfos[appInfo.packageName]

                    item.switchview.setOnCheckedChangeListener(null)
                    item.switchview.isChecked = data != null
                    item.switchview.setOnCheckedChangeListener { _, isChecked ->
                        allEnabledInfos.remove(appInfo.packageName)
                        item.sliderLayout.isVisible = isChecked
                        if (isChecked) {
                            allEnabledInfos[appInfo.packageName] = DarkModeInfo(appInfo.packageName)
                            item.slider.value = 0F
                        }
                        saveEnableList(allEnabledInfos)
                    }

                    item.sliderLayout.isVisible = data != null
                    item.slider.clearOnChangeListeners()
                    item.slider.value = data?.curType?.toFloat() ?: 0F
                    item.slider.addOnChangeListener { _, value, fromUser ->
                        if (!fromUser) return@addOnChangeListener
                        allEnabledInfos[appInfo.packageName]?.curType = value.toInt()
                        saveEnableList(allEnabledInfos)
                    }
                }
                onItemViewClick { view, _, _ ->
                    view.findViewById<MaterialSwitch>(R.id.switchview)?.toggle()
                }
            }
            FastScrollerBuilder(this).useMd2Style().build()
        }

        if (allAppInfos.isEmpty()) loadData()
    }

    private fun loadData() {
        scopeLife {
            allAppInfos.clear()
            filterAppInfos.clear()
            allEnabledInfos.clear()

            binding.swipeRefreshLayout.isRefreshing = true
            binding.searchViewLayout.isEnabled = false
            binding.searchView.text = null

            withDefault {
                val enableData =
                    requireActivity().getStringSet(ModulePrefs, supportListKey, ArraySet())

                val packageManager = requireActivity().packageManager
                allAppInfos = PackageUtils(packageManager).getInstalledAppInfos(0)
                allAppInfos.removeIf { it.isOverlay }

                enableData.forEach { its ->
                    val info = safeOfNull { Json.decodeFromString<DarkModeInfo>(its) }
                    if (info != null && allAppInfos.find { it.packageName == info.packName } != null) {
                        allEnabledInfos[info.packName] = info
                    }
                }
                allAppInfos.apply {
                    when (sortMode) {
                        0 -> sortBy { it.name }
                        1 -> sortBy { it.packageName }
                        2 -> sortBy { it.size }
                        3 -> sortBy { it.installTime }
                        4 -> sortBy { it.lastInstallTime }
                        5 -> sortBy { it.target }
                    }
                    if (isReverse) reverse()
                }

                val sortDatas = ArrayList<AppInfo>()
                allEnabledInfos.forEach { (k, _) ->
                    val find = allAppInfos.find { it.packageName == k } ?: return@forEach
                    sortDatas.add(find)
                }
                sortDatas.apply {
                    when (sortMode) {
                        0 -> sortBy { it.name }
                        1 -> sortBy { it.packageName }
                        2 -> sortBy { it.size }
                        3 -> sortBy { it.installTime }
                        4 -> sortBy { it.lastInstallTime }
                        5 -> sortBy { it.target }
                    }
                    if (isReverse) reverse()
                }
                filterAppInfos = allAppInfos
                filterAppInfos.removeAll(sortDatas.toSet())
                filterAppInfos.addAll(0, sortDatas)
            }

            binding.recyclerView.adapter?.notifyDataSetChangedIgnore()

            binding.swipeRefreshLayout.isRefreshing = false
            binding.searchViewLayout.isEnabled = true
        }
    }

    private fun saveEnableList(infos: ArrayMap<String, DarkModeInfo>) {
        val data = infos.mapNotNull {
            safeOfNull { Json.encodeToString(it.value) }
        }
        requireActivity().putStringSet(ModulePrefs, supportListKey, data.toSet())
        requireActivity().sendPrefsKey("android", supportListKey)
        requireActivity().sendPrefsKey("com.android.settings", supportListKey)
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.add(0, 1, 0, getString(R.string.menu_reboot)).apply {
            setIcon(R.drawable.ic_baseline_refresh_24)
            setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            if (ThemeUtils.isNightMode(resources.configuration)) {
                iconTintList = ColorStateList.valueOf(Color.WHITE)
            }
        }
        menu.add(0, 2, 0, getString(R.string.open)).apply {
            setIcon(R.drawable.baseline_open_in_new_24)
            setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            if (ThemeUtils.isNightMode(resources.configuration)) {
                iconTintList = ColorStateList.valueOf(Color.WHITE)
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == 1) RestartMenuUtils.showRestartScopeDialog(requireActivity(), scopes)
        if (menuItem.itemId == 2) IntentUtils(requireActivity()).jumpDarkMode()
        return true
    }
}