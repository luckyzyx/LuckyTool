package com.luckyzyx.luckytool.ui.fragment.extension

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.util.ArraySet
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.viewpager2.widget.ViewPager2
import com.drake.net.utils.scopeLife
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayoutMediator
import com.highcapable.betterandroid.ui.component.adapter.factory.bindAdapter
import com.highcapable.betterandroid.ui.component.adapter.factory.bindFragments
import com.highcapable.betterandroid.ui.component.adapter.recycler.factory.notifyDataSetChangedIgnore
import com.highcapable.betterandroid.ui.extension.view.layoutInflater
import com.highcapable.betterandroid.ui.extension.view.textToString
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.AppInfo
import com.luckyzyx.luckytool.data.MemcConfigActivity
import com.luckyzyx.luckytool.data.MemcConfigPackage
import com.luckyzyx.luckytool.databinding.DialogMemcConfigLayoutBinding
import com.luckyzyx.luckytool.databinding.FragmentMemcActivityLayoutBinding
import com.luckyzyx.luckytool.databinding.FragmentMemcLayoutBinding
import com.luckyzyx.luckytool.databinding.FragmentMemcPackageLayoutBinding
import com.luckyzyx.luckytool.databinding.LayoutMemcActivityItemBinding
import com.luckyzyx.luckytool.databinding.LayoutMemcPackageItemBinding
import com.luckyzyx.luckytool.listener.OnSelectActivityInfoListener
import com.luckyzyx.luckytool.listener.OnSelectAppInfoListener
import com.luckyzyx.luckytool.selector.ActivityInfoSelectDialog
import com.luckyzyx.luckytool.selector.AppInfoSelectDialog
import com.luckyzyx.luckytool.ui.fragment.base.BaseFragment
import com.luckyzyx.luckytool.utils.CommandUtils
import com.luckyzyx.luckytool.utils.FileUtils
import com.luckyzyx.luckytool.utils.GlobalKeyValue
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.PackageUtils
import com.luckyzyx.luckytool.utils.ThemeUtils
import com.luckyzyx.luckytool.utils.dialogCentered
import com.luckyzyx.luckytool.utils.getStringSet
import com.luckyzyx.luckytool.utils.putStringSet
import com.luckyzyx.luckytool.utils.safeOfNull
import com.luckyzyx.luckytool.utils.setupMenuProvider
import com.luckyzyx.luckytool.utils.showToast
import kotlinx.serialization.json.Json
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.lsposed.lsparanoid.Obfuscate
import java.io.InputStream

@Obfuscate
class MemcConfigFragment : BaseFragment<FragmentMemcLayoutBinding>(), MenuProvider {

    private val TAG = "MemcConfigFragment"

    private val configPackageList = GlobalKeyValue.memcConfigPackageList
    private val configActivityList = GlobalKeyValue.memcConfigActivityList

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        setupMenuProvider(this)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadData()
    }

    private fun loadData() {
        scopeLife {
            val configPackages =
                requireActivity().getStringSet(ModulePrefs, configPackageList, ArraySet())
            val configActivitys =
                requireActivity().getStringSet(ModulePrefs, configActivityList, ArraySet())

            if (configPackages.isEmpty() || configActivitys.isEmpty()) {
                resetAllConfig()
            }

            binding.viewPager.apply {
                adapter = bindFragments(this@MemcConfigFragment) {
                    pageCount = 2
                    onBindFragments { position ->
                        when (position) {
                            0 -> MemcPackageFragment()
                            1 -> MemcActivityFragment()
                            else -> MemcPackageFragment()
                        }
                    }
                }
                offscreenPageLimit = ViewPager2.OFFSCREEN_PAGE_LIMIT_DEFAULT
            }
            TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
                tab.text = when (position) {
                    0 -> "Packages"
                    1 -> "Activitys"
                    else -> null
                }
            }.attach()
        }
    }

    private val restoreData = registerForActivityResult(ActivityResultContracts.GetContent()) {
        if (it != null) {
            val inputStream = safeOfNull {
                requireActivity().contentResolver.openInputStream(it)
            } ?: return@registerForActivityResult
            resetAllConfig(inputStream)
        }
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.add(0, 1, 0, getString(R.string.import_) + "Xml").apply {
//            setIcon(R.drawable.ic_baseline_refresh_24)
            setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER)
            if (ThemeUtils.isNightMode(resources.configuration)) {
                iconTintList = ColorStateList.valueOf(Color.WHITE)
            }
        }
        menu.add(0, 2, 0, getString(R.string.reset)).apply {
//            setIcon(R.drawable.ic_baseline_refresh_24)
            setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_NEVER)
            if (ThemeUtils.isNightMode(resources.configuration)) {
                iconTintList = ColorStateList.valueOf(Color.WHITE)
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            1 -> {
                FileUtils.checkDownloadDir(requireActivity(), "LuckyTool").apply {
                    if (isFile) delete()
                    if (!exists()) mkdirs()
                }
                restoreData.launch("text/xml")
            }

            2 -> {
                val version = arrayOf("x7", "x7p")
                MaterialAlertDialogBuilder(requireActivity(), dialogCentered).apply {
                    setMessage(getString(R.string.restore_frame_insertion_configuration_data))
                    setPositiveButton(android.R.string.ok) { _, _ ->
                        MaterialAlertDialogBuilder(context, dialogCentered).apply {
                            setItems(version) { _, which ->
                                resetAllConfig(null, version[which])
                            }
                        }.show()
                    }
                    setNeutralButton(android.R.string.cancel, null)
                }.show()
            }
        }
        return true
    }

    private fun resetAllConfig(inputStream: InputStream? = null, version: String = "") {
        scopeLife {
            val packages = ArrayList<MemcConfigPackage>()
            val activitys = ArrayList<MemcConfigActivity>()

            val newInputStream = inputStream ?: safeOfNull {
                requireActivity().resources.openRawResource(R.raw.multimedia_pixelworks_apps_x7)
            } ?: return@scopeLife
            FileUtils.parseMemcXml(newInputStream, packages, activitys)

            when (version) {
                "x7p" -> activitys.onEachIndexed { index, config ->
                    activitys[index] =
                        MemcConfigActivity(config.packName, config.activity, "258-10-0-0")
                }
            }

            val packageSet = ArraySet<String>()
            val activitySet = ArraySet<String>()

            if (packages.isNotEmpty()) {
                MemcCallback.callback?.invoke(configPackageList, packages)
            }
            if (activitys.isNotEmpty()) {
                MemcCallback.callback?.invoke(configActivityList, activitys)
            }

            packages.forEachIndexed { _, info ->
                val sp = safeOfNull { Json.encodeToString(info) } ?: ""
                if (sp.isNotBlank()) packageSet.add(sp)
            }
            activitys.forEachIndexed { _, info ->
                val sp = safeOfNull { Json.encodeToString(info) } ?: ""
                if (sp.isNotBlank()) activitySet.add(sp)
            }
            if (packageSet.isNotEmpty() && activitySet.isNotEmpty()) {
                requireActivity().putStringSet(ModulePrefs, configPackageList, packageSet)
                requireActivity().putStringSet(ModulePrefs, configActivityList, activitySet)
            }
        }
    }

    @Obfuscate
    object MemcCallback {
        var callback: ((key: String, value: ArrayList<*>) -> Unit)? = null
    }

    @Obfuscate
    class MemcPackageFragment : BaseFragment<FragmentMemcPackageLayoutBinding>() {

        private val TAG = "MemcPackageFragment"

        private val allConfigPackages = ArrayList<MemcConfigPackage>()
        private var filterConfigPackages = ArrayList<MemcConfigPackage>()

        private var onItemChanged: ((MemcConfigPackage?, MemcConfigPackage) -> Unit)? = null
        private var onItemRemoved: ((MemcConfigPackage) -> Unit)? = null

        private val configPackageList = GlobalKeyValue.memcConfigPackageList

        @SuppressLint("SetTextI18n")
        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            binding.searchViewLayout.apply {
                hint = "PackageName"
            }
            binding.searchView.apply {
                addTextChangedListener(onTextChanged = { text: CharSequence?, _: Int, _: Int, _: Int ->
                    val query = text?.toString() ?: ""
                    filterConfigPackages = if (query.isBlank()) allConfigPackages
                    else {
                        val newList = allConfigPackages.filter {
                            it.packName.lowercase().contains(query)
                        }
                        ArrayList(newList)
                    }
                    binding.noMemcData.isVisible = filterConfigPackages.isEmpty()
                    binding.recyclerView.adapter?.notifyDataSetChangedIgnore()
                })
            }
            binding.swipeRefreshLayout.apply {
                setOnRefreshListener {
                    loadData()
                }
            }
            binding.addData.apply {
                setOnClickListener {
                    addOrEditData(it.context)
                }
            }

            MemcCallback.callback = { key: String, value: ArrayList<*> ->
                if (key == configPackageList) loadData(value)
            }

            onItemChanged = { old, new ->
                if (old != null && allConfigPackages.indexOf(old) != -1) {
                    allConfigPackages[allConfigPackages.indexOf(old)] = new
                } else allConfigPackages.add(new)
                saveAllData()
            }
            onItemRemoved = {
                allConfigPackages.remove(it)
                saveAllData()
            }

            binding.recyclerView.apply {
                adapter = bindAdapter<MemcConfigPackage> {
                    onBindData { filterConfigPackages }
                    onBindItemView<LayoutMemcPackageItemBinding> { item, info, position ->
                        item.packageName.text = info.packName
                        item.screenRate.text = "Rate: ${info.rate}"
                        item.commandType.text = "Type: ${info.type}"
                    }
                    onItemViewClick { view, info, position ->
                        addOrEditData(view.context, info)
                    }
                }
                FastScrollerBuilder(this).useMd2Style().build()
            }

            loadData()
        }

        @Suppress("UNCHECKED_CAST")
        private fun loadData(value: ArrayList<*> = arrayListOf<Any>()) {
            scopeLife {
                allConfigPackages.clear()
                filterConfigPackages.clear()

                binding.swipeRefreshLayout.isRefreshing = true
                binding.searchViewLayout.isEnabled = false
                binding.searchView.text = null

                if (value.isEmpty()) {
                    val configPackages =
                        requireActivity().getStringSet(ModulePrefs, configPackageList, ArraySet())
                    configPackages.forEach {
                        val configPackageInfo = safeOfNull {
                            Json.decodeFromString<MemcConfigPackage>(it)
                        }
                        if (configPackageInfo != null) allConfigPackages.add(configPackageInfo)
                    }
                } else {
                    allConfigPackages.addAll(value as ArrayList<MemcConfigPackage>)
                }
                filterConfigPackages = allConfigPackages

                binding.noMemcData.apply {
                    isVisible = filterConfigPackages.isEmpty()
                }

                binding.recyclerView.adapter?.notifyDataSetChangedIgnore()

                binding.searchViewLayout.isEnabled = true
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        fun addOrEditData(context: Context, config: MemcConfigPackage? = null) {
            val binding = DialogMemcConfigLayoutBinding.inflate(context.layoutInflater)
            binding.packageLayout.hint = "PackageName"
            binding.activityLayout.isVisible = false
            binding.rateLayout.hint = "ScreenRate"
            binding.typeLayout.hint = "Type"

            if (config != null) {
                binding.packageView.setText(config.packName)
                binding.rateView.setText(config.rate)
                binding.typeView.setText(config.type)
            }

            binding.packageView.apply {
                setOnClickListener {
                    AppInfoSelectDialog(context, false).apply {
                        setOnSelectAppListener(object : OnSelectAppInfoListener {
                            override fun resultSelectAppInfos(list: ArrayList<AppInfo>) {
                                if (list.isEmpty()) return
                                setText(list.first().packageName)
                            }
                        })
                        show()
                    }
                }
            }

            binding.tipsView.apply {
                text = context.getString(
                    R.string.edit_memc_configuration_tips, CommandUtils.memcHdrConfigHelp
                )
            }

            MaterialAlertDialogBuilder(requireActivity(), dialogCentered).apply {
                setView(binding.root)
                setPositiveButton(android.R.string.ok) { _, _ ->
                    val packageName = binding.packageView.textToString()
                    val rate = binding.rateView.textToString()
                    val type = binding.typeView.textToString()
                    if (!(packageName.isBlank() || rate.isBlank() || type.isBlank())) {
                        val newConfig = MemcConfigPackage(packageName, rate, type)
                        onItemChanged?.invoke(config, newConfig)
                    } else context.showToast("Data is incomplete!")
                }
                if (config != null) {
                    setNeutralButton(R.string.remove) { _, _ ->
                        MaterialAlertDialogBuilder(context, dialogCentered).apply {
                            val msg = context.getString(
                                R.string.confirm_to_delete_this_configuration, config.packName
                            )
                            setMessage(msg)
                            setPositiveButton(android.R.string.ok) { _, _ ->
                                onItemRemoved?.invoke(config)
                            }
                            setNeutralButton(android.R.string.cancel, null)
                        }.show()
                    }
                }
                setNegativeButton(android.R.string.cancel, null)
            }.show()
        }

        private fun saveAllData() {
            val set = allConfigPackages.mapNotNull { safeOfNull { Json.encodeToString(it) } }
            requireActivity().putStringSet(ModulePrefs, configPackageList, set.toSet())
            filterConfigPackages = allConfigPackages
            binding.recyclerView.adapter?.notifyDataSetChangedIgnore()
        }
    }

    @Obfuscate
    class MemcActivityFragment : BaseFragment<FragmentMemcActivityLayoutBinding>() {

        private val TAG = "MemcActivityFragment"

        private val allConfigActivitys = ArrayList<MemcConfigActivity>()
        private var filterConfigActivitys = ArrayList<MemcConfigActivity>()

        private var onItemChanged: ((MemcConfigActivity?, MemcConfigActivity) -> Unit)? = null
        private var onItemRemoved: ((MemcConfigActivity) -> Unit)? = null

        private val configActivityList = GlobalKeyValue.memcConfigActivityList

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            binding.searchViewLayout.apply {
                hint = "PackageName / ActivityName"
            }
            binding.searchView.apply {
                addTextChangedListener(onTextChanged = { text: CharSequence?, _: Int, _: Int, _: Int ->
                    val query = text?.toString() ?: ""
                    filterConfigActivitys = if (query.isBlank()) allConfigActivitys
                    else {
                        val newList = allConfigActivitys.filter {
                            it.packName.lowercase().contains(query) ||
                                    it.activity.lowercase().contains(query)
                        }
                        ArrayList(newList)
                    }
                    binding.noMemcData.isVisible = filterConfigActivitys.isEmpty()
                    binding.recyclerView.adapter?.notifyDataSetChangedIgnore()
                })
            }
            binding.swipeRefreshLayout.apply {
                setOnRefreshListener {
                    loadData()
                }
            }
            binding.addData.apply {
                setOnClickListener {
                    addOrEditData(it.context)
                }
            }

            MemcCallback.callback = { key: String, value: ArrayList<*> ->
                if (key == configActivityList) loadData(value)
            }

            onItemChanged = { old, new ->
                if (old != null && allConfigActivitys.indexOf(old) != -1) {
                    allConfigActivitys[allConfigActivitys.indexOf(old)] = new
                } else allConfigActivitys.add(new)
                saveAllData()
            }
            onItemRemoved = {
                allConfigActivitys.remove(it)
                saveAllData()
            }

            binding.recyclerView.apply {
                adapter = bindAdapter<MemcConfigActivity> {
                    onBindData { filterConfigActivitys }
                    onBindItemView<LayoutMemcActivityItemBinding> { item, info, position ->
                        item.packageName.text = info.packName
                        item.activityName.text = info.activity
                        item.commandType.text = info.type
                    }
                    onItemViewClick { view, info, position ->
                        addOrEditData(view.context, info)
                    }
                }
                FastScrollerBuilder(this).useMd2Style().build()
            }

            loadData()
        }

        @Suppress("UNCHECKED_CAST")
        private fun loadData(value: ArrayList<*> = arrayListOf<Any>()) {
            scopeLife {
                allConfigActivitys.clear()
                filterConfigActivitys.clear()

                binding.swipeRefreshLayout.isRefreshing = true
                binding.searchViewLayout.isEnabled = false
                binding.searchView.text = null

                if (value.isEmpty()) {
                    val configActivitys =
                        requireActivity().getStringSet(ModulePrefs, configActivityList, ArraySet())
                    configActivitys.forEach {
                        val configActivityInfo = safeOfNull {
                            Json.decodeFromString<MemcConfigActivity>(it)
                        }
                        if (configActivityInfo != null) allConfigActivitys.add(configActivityInfo)
                    }
                } else {
                    allConfigActivitys.addAll(value as ArrayList<MemcConfigActivity>)
                }

                binding.noMemcData.apply {
                    isVisible = allConfigActivitys.isEmpty()
                }

                binding.recyclerView.adapter?.notifyDataSetChangedIgnore()

                binding.searchViewLayout.isEnabled = true
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }

        fun addOrEditData(context: Context, config: MemcConfigActivity? = null) {
            val binding = DialogMemcConfigLayoutBinding.inflate(context.layoutInflater)
            binding.packageLayout.hint = "PackageName"
            binding.activityLayout.hint = "ActivityName"
            binding.rateLayout.isVisible = false
            binding.typeLayout.hint = "Type"

            if (config != null) {
                binding.packageView.setText(config.packName)
                binding.activityView.setText(config.activity)
                binding.typeView.setText(config.type)
            }

            binding.packageView.apply {
                setOnClickListener {
                    AppInfoSelectDialog(context, false).apply {
                        setOnSelectAppListener(object : OnSelectAppInfoListener {
                            override fun resultSelectAppInfos(list: ArrayList<AppInfo>) {
                                if (list.isEmpty()) return
                                setText(list.first().packageName)
                            }
                        })
                        show()
                    }
                }
            }

            binding.tipsView.apply {
                text = context.getString(
                    R.string.edit_memc_configuration_tips, CommandUtils.memcConfigHelp
                )
            }

            binding.activityView.apply {
                setOnClickListener {
                    val packageName = binding.packageView.textToString()
                    val packInfo = PackageUtils(context.packageManager).getPackageInfo(
                        packageName, PackageManager.GET_ACTIVITIES
                    )
                    if (packageName.isBlank()) {
                        context.showToast("PackageName is null!")
                        return@setOnClickListener
                    }
                    if (packInfo == null) {
                        context.showToast("App data is null!")
                        return@setOnClickListener
                    }
                    ActivityInfoSelectDialog(context, false, packInfo.activities).apply {
                        setOnSelectActivityListener(object : OnSelectActivityInfoListener {
                            override fun resultSelectActivityInfos(list: ArrayList<ActivityInfo>) {
                                if (list.isEmpty()) return
                                setText(list.first().name)
                            }
                        })
                        show()
                    }
                }
            }

            MaterialAlertDialogBuilder(requireActivity(), dialogCentered).apply {
                setView(binding.root)
                setPositiveButton(android.R.string.ok) { _, _ ->
                    val packageName = binding.packageView.textToString()
                    val activity = binding.activityView.textToString()
                    val type = binding.typeView.textToString()
                    if (!(packageName.isBlank() || activity.isBlank() || type.isBlank())) {
                        val newConfig = MemcConfigActivity(packageName, activity, type)
                        onItemChanged?.invoke(config, newConfig)
                    } else context.showToast("Data is incomplete!")
                }
                if (config != null) {
                    setNeutralButton(R.string.remove) { _, _ ->
                        MaterialAlertDialogBuilder(context, dialogCentered).apply {
                            val msg = context.getString(
                                R.string.confirm_to_delete_this_configuration,
                                config.activity
                            )
                            setMessage(msg)
                            setPositiveButton(android.R.string.ok) { _, _ ->
                                onItemRemoved?.invoke(config)
                            }
                            setNeutralButton(android.R.string.cancel, null)
                        }.show()
                    }
                }
                setNegativeButton(android.R.string.cancel, null)
            }.show()
        }

        private fun saveAllData() {
            val set = allConfigActivitys.mapNotNull { safeOfNull { Json.encodeToString(it) } }
            requireActivity().putStringSet(ModulePrefs, configActivityList, set.toSet())
            filterConfigActivitys = allConfigActivitys
            binding.recyclerView.adapter?.notifyDataSetChangedIgnore()
        }
    }
}