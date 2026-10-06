package com.luckyzyx.luckytool.ui.fragment.scopes

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.ArrayMap
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.drake.net.utils.scopeDialog
import com.drake.net.utils.scopeLife
import com.drake.net.utils.withMain
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.highcapable.betterandroid.ui.extension.view.layoutInflater
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.FragmentItem
import com.luckyzyx.luckytool.data.PrefsItem
import com.luckyzyx.luckytool.databinding.DialogLoadingLayoutBinding
import com.luckyzyx.luckytool.databinding.DialogScopeVersionInfoBinding
import com.luckyzyx.luckytool.databinding.DialogSearchResultLayoutBinding
import com.luckyzyx.luckytool.databinding.LayoutSearchResultItemBinding
import com.luckyzyx.luckytool.listener.OnSelectSearchResultListener
import com.luckyzyx.luckytool.ui.fragment.base.BaseScopePreferenceFeagment
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusAlarmClock
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusBattery
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusBeaconLink
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusBrowser
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusCalendar
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusCamera
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusCloudService
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusDirectUI
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusEngineerMode
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusEyeProtect
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusFileManager
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusGallery
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusGames
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusGesture
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusHealth
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusLinker
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusMMS
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusMarket
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusMcs
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusMyDevices
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusNfc
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusOShare
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusOTA
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusPermissionController
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusPhoneManager
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusPictorial
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusScreenshot
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusSearchBox
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusSecuritypPermission
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusSettings
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusSmartSidebar
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusSoundRecorder
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusSpeechAssist
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusTeleService
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusThemeStore
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusWeather
import com.luckyzyx.luckytool.ui.fragment.scopes.apps.OplusWirelessSettings
import com.luckyzyx.luckytool.ui.fragment.scopes.others.ADM
import com.luckyzyx.luckytool.ui.fragment.scopes.others.AlphaBackupPro
import com.luckyzyx.luckytool.ui.fragment.scopes.others.Claw
import com.luckyzyx.luckytool.ui.fragment.scopes.others.GpsJoyStick
import com.luckyzyx.luckytool.ui.fragment.scopes.others.KsWeb
import com.luckyzyx.luckytool.ui.fragment.scopes.related.AndroidRelated
import com.luckyzyx.luckytool.ui.fragment.scopes.related.AodRelated
import com.luckyzyx.luckytool.ui.fragment.scopes.related.ApplicationRelated
import com.luckyzyx.luckytool.ui.fragment.scopes.related.CorePatch
import com.luckyzyx.luckytool.ui.fragment.scopes.related.DialogRelated
import com.luckyzyx.luckytool.ui.fragment.scopes.related.FingerPrintRelated
import com.luckyzyx.luckytool.ui.fragment.scopes.related.LauncherRelated
import com.luckyzyx.luckytool.ui.fragment.scopes.related.LockScreenRelated
import com.luckyzyx.luckytool.ui.fragment.scopes.related.Miscellaneous
import com.luckyzyx.luckytool.ui.fragment.scopes.related.SoundRelated
import com.luckyzyx.luckytool.ui.fragment.scopes.related.StatusBarRelated
import com.luckyzyx.luckytool.ui.fragment.scopes.statusbar.StatusBarBattery
import com.luckyzyx.luckytool.ui.fragment.scopes.statusbar.StatusBarClock
import com.luckyzyx.luckytool.ui.fragment.scopes.statusbar.StatusBarControlCenter
import com.luckyzyx.luckytool.ui.fragment.scopes.statusbar.StatusBarIcon
import com.luckyzyx.luckytool.ui.fragment.scopes.statusbar.StatusBarLayout
import com.luckyzyx.luckytool.ui.fragment.scopes.statusbar.StatusBarNetWorkSpeed
import com.luckyzyx.luckytool.ui.fragment.scopes.statusbar.StatusBarNotify
import com.luckyzyx.luckytool.ui.fragment.scopes.statusbar.StatusBarNotifyRemoval
import com.luckyzyx.luckytool.ui.fragment.scopes.statusbar.StatusBarTiles
import com.luckyzyx.luckytool.utils.AppUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.RestartMenuUtils
import com.luckyzyx.luckytool.utils.ThemeUtils
import com.luckyzyx.luckytool.utils.dialogCentered
import com.luckyzyx.luckytool.utils.formatStringAuto
import com.luckyzyx.luckytool.utils.navigatePage
import com.luckyzyx.luckytool.utils.safeOf
import com.luckyzyx.luckytool.utils.setupMenuProvider
import com.luckyzyx.luckytool.utils.showBottomSheet
import com.luckyzyx.luckytool.utils.showToast
import io.noties.markwon.Markwon
import io.noties.markwon.ext.tables.TablePlugin
import kotlinx.coroutines.Dispatchers
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.lsposed.lsparanoid.Obfuscate
import java.util.Arrays

@Obfuscate
class XposedFragment : BaseScopePreferenceFeagment(), MenuProvider {

    override val currentPrefsName: String = ModulePrefs

    override val navigateFragmentId: Int = -1

    private val allFragmentItem = ArrayList<FragmentItem>()

    private var loadDialog: AlertDialog? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMenuProvider(this)
    }

    override fun Context.loadRootPreference(): Preference {
        return Preference(this)
    }

    override fun Context.loadPreferences(): ArrayList<Preference> {
        val allPrefs = ArrayList<Preference>()

        //Android
        addFragmentPreference(this, allPrefs, AndroidRelated())
        //StatusBar
        addFragmentPreference(this, allPrefs, StatusBarRelated())
        //Launcher
        addFragmentPreference(this, allPrefs, LauncherRelated())
        //Aod
        addFragmentPreference(this, allPrefs, AodRelated())
        //LockScreen
        addFragmentPreference(this, allPrefs, LockScreenRelated())
        //Application
        addFragmentPreference(this, allPrefs, ApplicationRelated())
        //Miscellaneous
        addFragmentPreference(this, allPrefs, Miscellaneous())
        //Screenshot
        addFragmentPreference(this, allPrefs, OplusScreenshot())
        //Battery
        addFragmentPreference(this, allPrefs, OplusBattery())
        //AlarmClock
        addFragmentPreference(this, allPrefs, OplusAlarmClock())
        //Settings
        addFragmentPreference(this, allPrefs, OplusSettings())
        //WirelessSettings
        addFragmentPreference(this, allPrefs, OplusWirelessSettings())
        //TeleService
        addFragmentPreference(this, allPrefs, OplusTeleService())
        //Mms
        addFragmentPreference(this, allPrefs, OplusMMS())
        //Browser
        addFragmentPreference(this, allPrefs, OplusBrowser())
        //Camera
        addFragmentPreference(this, allPrefs, OplusCamera())
        //Gallery
        addFragmentPreference(this, allPrefs, OplusGallery())
        //Games
        addFragmentPreference(this, allPrefs, OplusGames())
        //Theme
        addFragmentPreference(this, allPrefs, OplusThemeStore())
        //Market
        addFragmentPreference(this, allPrefs, OplusMarket())
        //CloudService
        addFragmentPreference(this, allPrefs, OplusCloudService())
        //OTA
        addFragmentPreference(this, allPrefs, OplusOTA())
        //Pictorial
        addFragmentPreference(this, allPrefs, OplusPictorial())
        //Gesture
        addFragmentPreference(this, allPrefs, OplusGesture())
        //SpeechAssist
        addFragmentPreference(this, allPrefs, OplusSpeechAssist())
        //Directui
        addFragmentPreference(this, allPrefs, OplusDirectUI())
        //QuickSearchBox
        addFragmentPreference(this, allPrefs, OplusSearchBox())
        //Weather
        addFragmentPreference(this, allPrefs, OplusWeather())
        //Calendar
        addFragmentPreference(this, allPrefs, OplusCalendar())
        //SmartSidebar
        addFragmentPreference(this, allPrefs, OplusSmartSidebar())
        //PhoneManager
        addFragmentPreference(this, allPrefs, OplusPhoneManager())
        //Health
        addFragmentPreference(this, allPrefs, OplusHealth())
        //SoundRecorder
        addFragmentPreference(this, allPrefs, OplusSoundRecorder())
        //EyeProtect
        addFragmentPreference(this, allPrefs, OplusEyeProtect())
        //BeaconLink
        addFragmentPreference(this, allPrefs, OplusBeaconLink())
        //Nfc
        addFragmentPreference(this, allPrefs, OplusNfc())
        //OShare
        addFragmentPreference(this, allPrefs, OplusOShare())
        //PermissionController
        addFragmentPreference(this, allPrefs, OplusPermissionController())
        //Linker
        addFragmentPreference(this, allPrefs, OplusLinker())
        //SecuritypPermission
        addFragmentPreference(this, allPrefs, OplusSecuritypPermission())
        //FileManager
        addFragmentPreference(this, allPrefs, OplusFileManager())
        //EngineerMode
        addFragmentPreference(this, allPrefs, OplusEngineerMode())
        //MyDevices
        addFragmentPreference(this, allPrefs, OplusMyDevices())
        //Mcs
        addFragmentPreference(this, allPrefs, OplusMcs())
        //Claw
        addFragmentPreference(this, allPrefs, Claw())

        //Other App
        addFragmentPreference(this, allPrefs, AlphaBackupPro())
        addFragmentPreference(this, allPrefs, KsWeb())
        addFragmentPreference(this, allPrefs, ADM())
        addFragmentPreference(this, allPrefs, GpsJoyStick())

        return allPrefs
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        if (loadDialog == null) {
            val binding = DialogLoadingLayoutBinding.inflate(layoutInflater)
            loadDialog = MaterialAlertDialogBuilder(requireActivity(), dialogCentered).apply {
                setTitle(getString(R.string.loading))
                setView(binding.root)
            }.create()
        }
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity())
    }

    override fun onResume() {
        super.onResume()
        val count = safeOf(0) { preferenceScreen.preferenceCount }
        if (count <= 0) init()
    }

    private fun initAllScopePreferences() {
        allFragmentItem.clear()

        val context = requireActivity()

        addFragmentItem(context, allFragmentItem, StatusBarRelated())
        addFragmentItem(context, allFragmentItem, StatusBarBattery())
        addFragmentItem(context, allFragmentItem, StatusBarClock())
        addFragmentItem(context, allFragmentItem, StatusBarControlCenter())
        addFragmentItem(context, allFragmentItem, StatusBarIcon())
        addFragmentItem(context, allFragmentItem, StatusBarLayout())
        addFragmentItem(context, allFragmentItem, StatusBarNetWorkSpeed())
        addFragmentItem(context, allFragmentItem, StatusBarNotify())
        addFragmentItem(context, allFragmentItem, StatusBarNotifyRemoval())
        addFragmentItem(context, allFragmentItem, StatusBarTiles())

        addFragmentItem(context, allFragmentItem, AndroidRelated())
        addFragmentItem(context, allFragmentItem, CorePatch())
        addFragmentItem(context, allFragmentItem, AodRelated())
        addFragmentItem(context, allFragmentItem, ApplicationRelated())
        addFragmentItem(context, allFragmentItem, DialogRelated())
        addFragmentItem(context, allFragmentItem, FingerPrintRelated())
        addFragmentItem(context, allFragmentItem, LauncherRelated())
        addFragmentItem(context, allFragmentItem, LockScreenRelated())
        addFragmentItem(context, allFragmentItem, Miscellaneous())

        addFragmentItem(context, allFragmentItem, OplusAlarmClock())
        addFragmentItem(context, allFragmentItem, OplusBattery())
        addFragmentItem(context, allFragmentItem, OplusSpeechAssist())
        addFragmentItem(context, allFragmentItem, OplusDirectUI())
        addFragmentItem(context, allFragmentItem, OplusBrowser())
        addFragmentItem(context, allFragmentItem, OplusCalendar())
        addFragmentItem(context, allFragmentItem, OplusCamera())
        addFragmentItem(context, allFragmentItem, OplusCloudService())
        addFragmentItem(context, allFragmentItem, OplusEyeProtect())
        addFragmentItem(context, allFragmentItem, OplusGallery())
        addFragmentItem(context, allFragmentItem, OplusGames())
        addFragmentItem(context, allFragmentItem, OplusGesture())
        addFragmentItem(context, allFragmentItem, OplusMarket())
        addFragmentItem(context, allFragmentItem, OplusMMS())
        addFragmentItem(context, allFragmentItem, OplusOTA())
        addFragmentItem(context, allFragmentItem, OplusPhoneManager())
        addFragmentItem(context, allFragmentItem, OplusPictorial())
        addFragmentItem(context, allFragmentItem, OplusScreenshot())
        addFragmentItem(context, allFragmentItem, OplusSearchBox())
        addFragmentItem(context, allFragmentItem, OplusSettings())
        addFragmentItem(context, allFragmentItem, OplusSmartSidebar())
        addFragmentItem(context, allFragmentItem, OplusSoundRecorder())
        addFragmentItem(context, allFragmentItem, OplusTeleService())
        addFragmentItem(context, allFragmentItem, OplusWeather())
        addFragmentItem(context, allFragmentItem, SoundRelated())
        addFragmentItem(context, allFragmentItem, OplusThemeStore())
        addFragmentItem(context, allFragmentItem, OplusBeaconLink())
        addFragmentItem(context, allFragmentItem, OplusWirelessSettings())
        addFragmentItem(context, allFragmentItem, OplusHealth())
        addFragmentItem(context, allFragmentItem, OplusNfc())
        addFragmentItem(context, allFragmentItem, OplusOShare())
        addFragmentItem(context, allFragmentItem, OplusPermissionController())
        addFragmentItem(context, allFragmentItem, OplusLinker())
        addFragmentItem(context, allFragmentItem, OplusSecuritypPermission())
        addFragmentItem(context, allFragmentItem, OplusFileManager())
        addFragmentItem(context, allFragmentItem, OplusEngineerMode())
        addFragmentItem(context, allFragmentItem, OplusMyDevices())
        addFragmentItem(context, allFragmentItem, OplusMcs())
        addFragmentItem(context, allFragmentItem, Claw())

        addFragmentItem(context, allFragmentItem, ADM())
        addFragmentItem(context, allFragmentItem, AlphaBackupPro())
        addFragmentItem(context, allFragmentItem, KsWeb())
        addFragmentItem(context, allFragmentItem, GpsJoyStick())
    }

    private fun addFragmentPreference(
        context: Context,
        list: ArrayList<Preference>,
        fragment: BaseScopePreferenceFeagment
    ) {
        list.add(fragment.getRootPreference(context))
    }

    private fun addFragmentItem(
        context: Context,
        list: ArrayList<FragmentItem>,
        fragment: BaseScopePreferenceFeagment
    ) {
        if (fragment.navigateFragmentId == -1) return
        list.add(
            FragmentItem(
                fragment, fragment.navigateFragmentId,
                fragment.allPrefsItems.ifEmpty { fragment.getAllPrefsItem(context) }
            )
        )
    }

    private fun init() {
        scopeDialog(dialog = loadDialog, cancelable = false, dispatcher = Dispatchers.IO) {
            preferenceScreen?.apply {
                removeAll()
                context.loadPreferences().forEachIndexed { index, preference ->
                    try {
                        preferenceScreen.addPreference(preference)
                    } catch (_: Throwable) {
                        withMain { context.showToast("Error: $index ${preference.key}") }
                        return@forEachIndexed
                    }
                }
            }
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.add(0, 1, 0, getString(R.string.menu_search)).apply {
            setIcon(R.drawable.ic_baseline_search_24)
            setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            if (ThemeUtils.isNightMode(resources.configuration)) {
                iconTintList = ColorStateList.valueOf(Color.WHITE)
            }
        }
        menu.add(0, 2, 0, getString(R.string.menu_reboot)).apply {
            setIcon(R.drawable.ic_baseline_refresh_24)
            setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            if (ThemeUtils.isNightMode(resources.configuration)) {
                iconTintList = ColorStateList.valueOf(Color.WHITE)
            }
        }
        menu.add(0, 3, 0, getString(R.string.menu_versioninfo)).apply {
            setIcon(R.drawable.ic_baseline_extension_24)
            setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            if (ThemeUtils.isNightMode(resources.configuration)) {
                iconTintList = ColorStateList.valueOf(Color.WHITE)
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            1 -> requireActivity().showSearchDialog()
            2 -> RestartMenuUtils.showMainRestartMenu(requireActivity())
            3 -> requireActivity().showBottomDialog()
        }
        return true
    }

    private fun Context.showBottomDialog() {
        scopeLife {
            val dialogBinding = DialogScopeVersionInfoBinding.inflate(layoutInflater)
            val markwon = Markwon.builder(this@showBottomDialog).apply {
                usePlugin(TablePlugin.create(this@showBottomDialog))
            }.build()
            showBottomSheet(dialogBinding.root)
            val list = ArrayList<String>().apply {
                add("| name | package | version |")
                add("| ------ | ------ | ------|")
            }
            val xposedScope = resources.getStringArray(R.array.xposed_scope)
            Arrays.sort(xposedScope)
            xposedScope.forEach {
                val appVerInfo = AppUtils(this@showBottomDialog).getAppVerInfo(it)
                    ?: return@forEach
                list.add("| ${appVerInfo.name} | $it |  ${appVerInfo.versionName}(${appVerInfo.versionCode})[${appVerInfo.versionCommit}] |")
            }
            markwon.setMarkdown(dialogBinding.tv, formatStringAuto(list, "\n"))
        }
    }

    private fun Context.showSearchDialog() {
        scopeLife {
            initAllScopePreferences()

            val binding = DialogSearchResultLayoutBinding.inflate(layoutInflater)

            val dialog = MaterialAlertDialogBuilder(this@showSearchDialog).apply {
                setView(binding.root)
            }.show() ?: return@scopeLife

            val searchResultAdapter = SearchResultAdapter(
                allFragmentItem, object : OnSelectSearchResultListener {
                    override fun resultItem(fragmentItem: FragmentItem, prefsItem: PrefsItem) {
                        dialog.dismiss()
                        prefsItem.fragmentResId?.let {
                            if (it == -1) return@let
                            val bundle = Bundle().apply {
                                putCharSequence("title_text", prefsItem.fragmentTitle)
                                putString("scrollKey", prefsItem.key)
                                putInt("scrollPosition", prefsItem.position)
                            }
                            findNavController().navigatePage(prefsItem.fragmentResId, bundle)
                        }
                    }
                })

            binding.searchViewLayout.apply {
                hint = "Title / Summary"
            }

            binding.searchView.apply {
                addTextChangedListener(onTextChanged = { text: CharSequence?, _: Int, _: Int, _: Int ->
                    searchResultAdapter.getFilter.filter(text)
                })
            }

            binding.recyclerView.apply {
                adapter = searchResultAdapter
                layoutManager = LinearLayoutManager(context)
                FastScrollerBuilder(this).useMd2Style().build()
            }
        }
    }

    @Obfuscate
    class SearchResultAdapter(
        allFragmentItem: ArrayList<FragmentItem>,
        private val onSelectSearchResultListener: OnSelectSearchResultListener
    ) : RecyclerView.Adapter<SearchResultAdapter.SearchResultItemHolder>() {

        private var allFragmentItemDatas = ArrayMap<Int, FragmentItem>()
        private var allPrefsItemDatas = ArrayMap<Int, ArrayList<PrefsItem>>()

        private var allDatas = ArrayList<PrefsItem>()
        private var filterDatas = ArrayList<PrefsItem>()

        init {
            allFragmentItemDatas.clear()
            allPrefsItemDatas.clear()
            allDatas.clear()
            filterDatas.clear()

            allFragmentItem.forEachIndexed { _, fragmentItem ->
                allFragmentItemDatas[fragmentItem.fragmentId] = fragmentItem
                allPrefsItemDatas[fragmentItem.fragmentId] = fragmentItem.allPrefsItem
                allDatas.addAll(fragmentItem.allPrefsItem)
            }
//            LogUtils.d("SearchResultAdapter", "init", allDatas.size.toString(), true)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchResultItemHolder {
            val binding = LayoutSearchResultItemBinding.inflate(
                parent.context.layoutInflater, parent, false
            )
            return SearchResultItemHolder(binding)
        }

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: SearchResultItemHolder, position: Int) {
            val prefsItem = filterDatas[position]
            val icon = prefsItem.icon
            val title = prefsItem.title
            val summary = prefsItem.summary
            val fragmentId = prefsItem.fragmentResId
            val fragmentTitle = prefsItem.fragmentTitle
            val fragmentItem = allFragmentItemDatas[fragmentId]!!

            holder.item.setOnClickListener(null)
            holder.itemIcon.setImageDrawable(null)
            holder.itemTitle.text = null
            holder.itemSummary.text = null
            holder.itemRootTitle.text = null

            holder.item.setOnClickListener {
                onSelectSearchResultListener.resultItem(fragmentItem, prefsItem)
            }
            if (icon == null) holder.itemIcon.isVisible = false
            else holder.itemIcon.setImageDrawable(icon)
            holder.itemTitle.text = title
            if (summary.isNullOrBlank()) holder.itemSummary.isVisible = false
            else holder.itemSummary.text = summary
            if (summary.isNullOrBlank()) holder.itemRootTitle.isVisible = false
            else holder.itemRootTitle.text = "From: $fragmentTitle"
        }

        override fun getItemCount(): Int = filterDatas.size

        val getFilter = object : Filter() {
            override fun performFiltering(constraint: CharSequence): FilterResults {
                filterDatas = if (constraint.isNotBlank()) {
                    ArrayList(allDatas.filter {
                        ((it.key?.contains(constraint, true) == true)
                                || (it.title?.contains(constraint, true) == true)
                                || (it.summary?.contains(constraint, true) == true))
                                && it.isVisible == true && it.fragmentResId != -1
                    })
                } else arrayListOf()
//                LogUtils.d("SearchResultAdapter", "getFilter", filterDatas.size.toString(), true)
                val filterResults = FilterResults()
                filterResults.values = filterDatas
                return filterResults
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence, results: FilterResults) {
                filterDatas = results.values as ArrayList<PrefsItem>
                refreshDatas()
            }
        }

        @SuppressLint("NotifyDataSetChanged")
        fun refreshDatas() {
            notifyDataSetChanged()
        }

        @Obfuscate
        class SearchResultItemHolder(binding: LayoutSearchResultItemBinding) :
            RecyclerView.ViewHolder(binding.root) {
            val item: ConstraintLayout = binding.root
            val itemIcon: ImageView = binding.itemIcon
            val itemTitle: TextView = binding.itemTitle
            val itemSummary: TextView = binding.itemSummary
            val itemRootTitle: TextView = binding.resultItemRootTitle
        }
    }
}