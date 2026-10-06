package com.luckyzyx.luckytool.selector

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.drake.net.utils.scope
import com.drake.net.utils.withDefault
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.highcapable.betterandroid.ui.component.adapter.factory.bindAdapter
import com.highcapable.betterandroid.ui.component.adapter.recycler.factory.notifyDataSetChangedIgnore
import com.highcapable.betterandroid.ui.extension.view.layoutInflater
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.AppInfo
import com.luckyzyx.luckytool.databinding.DialogAppInfoSelectorLayoutBinding
import com.luckyzyx.luckytool.databinding.LayoutAppinfoCheckboxItemBinding
import com.luckyzyx.luckytool.databinding.LayoutAppinfoItemBinding
import com.luckyzyx.luckytool.listener.OnSelectAppInfoListener
import com.luckyzyx.luckytool.utils.PackageUtils
import com.luckyzyx.luckytool.utils.dialogCentered
import me.zhanghai.android.fastscroll.FastScrollerBuilder
import org.lsposed.lsparanoid.Obfuscate

/**
 * AppInfo选择器
 * @property context Context
 * @property multiMode Boolean 多选模式
 * @constructor
 */
@Obfuscate
class AppInfoSelectDialog(context: Context, val multiMode: Boolean = false) :
    MaterialAlertDialogBuilder(context, dialogCentered) {

    private val TAG = "AppInfoSelectDialog"

    private val binding = DialogAppInfoSelectorLayoutBinding.inflate(context.layoutInflater)

    private lateinit var dialog: AlertDialog

    private var allAppInfos = ArrayList<AppInfo>()
    private var filterAppInfos = ArrayList<AppInfo>()
    private var allEnabledInfos = ArrayList<AppInfo>()
    private var enabledList = ArrayList<String>()

    private var enableSortFilter = true
    private var isReverse = false
    private var sortMode = 0
    private var selectAllMode = false
    private var showSystemApp = false

    private lateinit var sortFilterBottomSheetDialog: SortFilterBottomSheetDialog

    private var onSelectAppInfoListener: OnSelectAppInfoListener? = null

    init {
        setView(binding.root)

        initSortFilterSelector()
        initSearchViewLayout()
        binding.searchView.apply {
            addTextChangedListener(onTextChanged = { text: CharSequence?, _: Int, _: Int, _: Int ->
                val query = text?.toString() ?: ""
                filterAppInfos = if (query.isBlank()) allAppInfos
                else {
                    val newList = allAppInfos.filter {
                        it.name.contains(query) ||
                                it.packageName.lowercase().contains(query)
                    }
                    ArrayList(newList)
                }
                binding.recyclerView.adapter?.notifyDataSetChangedIgnore()
            })
        }
        binding.swipeRefreshLayout.setOnRefreshListener {
            loadData()
        }
        binding.btnOk.apply {
            isVisible = multiMode
            setOnClickListener {
                dialog.dismiss()
                onSelectAppInfoListener?.resultSelectAppInfos(allEnabledInfos)
            }
        }

        binding.recyclerView.apply {
            adapter = bindAdapter<AppInfo> {
                onBindData { filterAppInfos }
                if (!multiMode) {
                    onBindItemView<LayoutAppinfoItemBinding> { item, info, _ ->
                        item.appIcon.setImageDrawable(info.icon)
                        item.appName.text = info.name
                        item.packName.text = info.packageName
                    }
                } else {
                    onBindItemView<LayoutAppinfoCheckboxItemBinding> { item, info, _ ->
                        item.appIcon.setImageDrawable(info.icon)
                        item.appName.text = info.name
                        item.packName.text = info.packageName

                        item.checkboxView.setOnCheckedChangeListener(null)
                        item.checkboxView.isChecked = allEnabledInfos.contains(info)
                        item.checkboxView.setOnCheckedChangeListener { _, isChecked ->
                            allEnabledInfos.remove(info)
                            if (isChecked) allEnabledInfos.add(info)
                        }
                    }
                }
                onItemViewClick { view, info, _ ->
                    if (multiMode) {
                        view.findViewById<MaterialCheckBox>(R.id.checkbox_view)?.toggle()
                    } else {
                        dialog.dismiss()
                        onSelectAppInfoListener?.resultSelectAppInfos(arrayListOf(info))
                    }
                }
            }
            FastScrollerBuilder(this).useMd2Style().build()
        }
    }

    override fun show(): AlertDialog {
        if (allAppInfos.isEmpty()) loadData()
        dialog = super.show()
        return dialog
    }

    fun setOnSelectAppListener(onSelectAppInfoListener: OnSelectAppInfoListener) {
        this.onSelectAppInfoListener = onSelectAppInfoListener
    }

    fun setDefaultShowSystem(show: Boolean) {
        this.showSystemApp = show
        initSortFilterSelector()
    }

    fun setEnableSortFilter(enable: Boolean) {
        this.enableSortFilter = enable
        initSearchViewLayout()
    }

    fun setEnabledList(list: ArrayList<String>) {
        enabledList = list
    }

    fun setSelectAllMode(mode: Boolean) {
        selectAllMode = mode

        binding.btnSelectAll.apply {
            isVisible = multiMode && selectAllMode
            setOnClickListener {
                val isAll = allAppInfos.size == allEnabledInfos.size
                if (isAll) allAppInfos.clear() else allEnabledInfos = allAppInfos
                filterAppInfos = allAppInfos
                binding.recyclerView.adapter?.notifyDataSetChangedIgnore()
            }
        }
    }

    private fun initSortFilterSelector() {
        sortFilterBottomSheetDialog = SortFilterBottomSheetDialog(context).apply {
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
            setFilterChips(true, arrayOf(Chip(context).apply {
                text = context.getString(R.string.appinfo_system_app)
                isCheckable = true
                isClickable = true
                isChecked = showSystemApp
                setOnCheckedChangeListener { _, isChecked ->
                    showSystemApp = isChecked
                    loadData()
                }
            }))
        }
    }

    private fun initSearchViewLayout() {
        binding.searchViewLayout.apply {
            hint = "Name / PackageName"
            if (enableSortFilter) {
                endIconMode = TextInputLayout.END_ICON_CUSTOM
                setEndIconDrawable(R.drawable.baseline_filter_list_24)
                setEndIconOnClickListener {
                    sortFilterBottomSheetDialog.show()
                }
            } else {
                endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
            }
        }
    }

    private fun loadData() {
        scope {
            allAppInfos.clear()
            filterAppInfos.clear()
            allEnabledInfos.clear()

            binding.swipeRefreshLayout.isRefreshing = true
            binding.searchViewLayout.isEnabled = false
            binding.searchView.text = null

            withDefault {
                val packageManager = context.packageManager
                allAppInfos = PackageUtils(packageManager).getInstalledAppInfos(0)
                allAppInfos.removeIf { it.isOverlay }

                enabledList.forEach { its ->
                    val find = allAppInfos.find { it.packageName == its }
                    if (find != null) allEnabledInfos.add(find)
                }

                allAppInfos.apply {
                    if (!showSystemApp) removeIf { it.isSystem }
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
                allEnabledInfos.apply {
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
                allAppInfos.removeAll(allEnabledInfos.toSet())
                allAppInfos.addAll(0, allEnabledInfos)
                filterAppInfos = allAppInfos
            }

            binding.recyclerView.adapter?.notifyDataSetChangedIgnore()

            binding.swipeRefreshLayout.isRefreshing = false
            binding.searchViewLayout.isEnabled = true
        }
    }
}