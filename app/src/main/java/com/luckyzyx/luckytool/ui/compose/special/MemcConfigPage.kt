package com.luckyzyx.luckytool.ui.compose.special

import android.content.Context
import android.content.pm.PackageManager
import android.util.ArraySet
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.itos.coui.theme.CouiTheme
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.MemcConfigActivity
import com.luckyzyx.luckytool.data.MemcConfigPackage
import com.luckyzyx.luckytool.ui.compose.components.ActionListDialog
import com.luckyzyx.luckytool.ui.compose.components.pageInset
import com.luckyzyx.luckytool.ui.compose.components.AppPickerDialog
import com.luckyzyx.luckytool.ui.compose.components.ChoiceItem
import com.luckyzyx.luckytool.ui.compose.components.ConfirmDialog
import com.luckyzyx.luckytool.ui.compose.components.PrefCard
import com.luckyzyx.luckytool.ui.compose.components.PrefNote
import com.luckyzyx.luckytool.ui.compose.components.PrefRowSummary
import com.luckyzyx.luckytool.ui.compose.components.PrefRowTitle
import com.luckyzyx.luckytool.ui.compose.components.AppItem
import com.luckyzyx.luckytool.ui.compose.components.AppListItem
import com.luckyzyx.luckytool.ui.compose.components.currentDialogStyle
import com.luckyzyx.luckytool.ui.compose.components.currentPrefStyle
import com.luckyzyx.luckytool.ui.compose.PageAction
import com.luckyzyx.luckytool.ui.compose.PageSpec
import com.luckyzyx.luckytool.ui.compose.components.currentUiStyle
import com.luckyzyx.luckytool.utils.CommandUtils
import com.luckyzyx.luckytool.utils.FileUtils
import com.luckyzyx.luckytool.utils.GlobalKeyValue
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.PackageUtils
import com.luckyzyx.luckytool.utils.getStringSet
import com.luckyzyx.luckytool.utils.putStringSet
import com.luckyzyx.luckytool.utils.safeOfNull
import com.luckyzyx.luckytool.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.InputStream

internal var memcRefresh: (suspend () -> Unit)? = null
private const val CONFIG_PACKAGE_LIST = GlobalKeyValue.memcConfigPackageList
private const val CONFIG_ACTIVITY_LIST = GlobalKeyValue.memcConfigActivityList

/**
 * P4 特殊页：Memc 帧插入配置。
 * 双 Tab（Packages/Activitys）+ 搜索 + 下拉刷新 + Import Xml / Reset。
 *
 * 单文件承载：状态与写值逻辑只有一份 [MemcConfigState]，渲染集中在 [MemcConfigContent]——Import Xml / Reset
 * 通过 [PageAction] 进顶栏溢出菜单（不设 `showAsAction`，只出现在溢出菜单里、不占页面空间），
 * 正文是双面板与四个弹层。
 */
object MemcConfigPage {

    /** 顶栏溢出菜单动作 key，页面在 `actionHandler` 里据此分派。 */
    const val ACTION_IMPORT_XML = "import_xml"
    const val ACTION_RESET = "reset"

    val spec = PageSpec(
        pageKey = "memc_config",
        prefsName = ModulePrefs,
        packName = "",
        scopes = arrayOf(),
        restartEnabled = false,
        // 特殊页：整页由 fullContent 自渲染，入口可见性只看 isVisible + 作用域
        selfRendered = true,
        onRefresh = { memcRefresh?.invoke() },
        fullContent = { _ -> MemcConfigContent() },
        // 这两项都不设图标、不显示为顶栏按钮，只出现在溢出菜单里
        topActions = listOf(
            PageAction(ACTION_IMPORT_XML, R.string.menu_import_xml),
            PageAction(ACTION_RESET, R.string.reset),
        ),
    ) { }
}

/** Memc 双 Tab 共享状态（存于内容层，Tab 切换不丢失）。 */
internal class MemcConfigState(private val context: Context) {

    var pkgAll by mutableStateOf(ArrayList<MemcConfigPackage>())
    var pkgFilter by mutableStateOf(ArrayList<MemcConfigPackage>())
    var pkgQuery by mutableStateOf("")
    var pkgLoading by mutableStateOf(false)

    var actAll by mutableStateOf(ArrayList<MemcConfigActivity>())
    var actFilter by mutableStateOf(ArrayList<MemcConfigActivity>())
    var actQuery by mutableStateOf("")
    var actLoading by mutableStateOf(false)

    fun applyPkgQuery(q: String) {
        pkgQuery = q
        pkgFilter = if (q.isBlank()) pkgAll
        else ArrayList(pkgAll.filter { it.packName.lowercase().contains(q.lowercase()) })
    }

    fun applyActQuery(q: String) {
        actQuery = q
        actFilter = if (q.isBlank()) actAll
        else ArrayList(
            actAll.filter {
                it.packName.lowercase().contains(q.lowercase()) ||
                        it.activity.lowercase().contains(q.lowercase())
            }
        )
    }

    suspend fun reload() {
        pkgLoading = true
        actLoading = true
        pkgQuery = ""
        actQuery = ""
        withContext(Dispatchers.IO) {
            var packages = decodePackages(
                context.getStringSet(ModulePrefs, CONFIG_PACKAGE_LIST, ArraySet())
            )
            var activities = decodeActivities(
                context.getStringSet(ModulePrefs, CONFIG_ACTIVITY_LIST, ArraySet())
            )
            if (packages.isEmpty() || activities.isEmpty()) {
                resetFromStream(null, "")
                packages = decodePackages(
                    context.getStringSet(ModulePrefs, CONFIG_PACKAGE_LIST, ArraySet())
                )
                activities = decodeActivities(
                    context.getStringSet(ModulePrefs, CONFIG_ACTIVITY_LIST, ArraySet())
                )
            }
            pkgAll = packages
            actAll = activities
            applyPkgQuery("")
            applyActQuery("")
        }
        pkgLoading = false
        actLoading = false
    }

    suspend fun reset(inputStream: InputStream?, version: String) {
        withContext(Dispatchers.IO) {
            resetFromStream(inputStream, version)
        }
        reload()
    }

    // 写入一律「先拷贝、再改、最后整体赋值」：pkgAll/actAll 是 MutableState 持有的列表，
    // 就地改后再赋一个内容相等的副本会被结构相等策略丢弃（列表不刷新、要重开页面才看到）。
    fun savePackage(old: MemcConfigPackage?, new: MemcConfigPackage) {
        val next = ArrayList(pkgAll)
        val index = old?.let { next.indexOf(it) } ?: -1
        if (index != -1) next[index] = new else next.add(new)
        pkgAll = next
        persistPackages()
    }

    fun deletePackage(info: MemcConfigPackage) {
        val next = ArrayList(pkgAll)
        next.remove(info)
        pkgAll = next
        persistPackages()
    }

    fun saveActivity(old: MemcConfigActivity?, new: MemcConfigActivity) {
        val next = ArrayList(actAll)
        val index = old?.let { next.indexOf(it) } ?: -1
        if (index != -1) next[index] = new else next.add(new)
        actAll = next
        persistActivities()
    }

    fun deleteActivity(info: MemcConfigActivity) {
        val next = ArrayList(actAll)
        next.remove(info)
        actAll = next
        persistActivities()
    }

    private fun decodePackages(raw: Set<String>): ArrayList<MemcConfigPackage> =
        ArrayList(raw.mapNotNull { safeOfNull { Json.decodeFromString<MemcConfigPackage>(it) } })

    private fun decodeActivities(raw: Set<String>): ArrayList<MemcConfigActivity> =
        ArrayList(raw.mapNotNull { safeOfNull { Json.decodeFromString<MemcConfigActivity>(it) } })

    private fun persistPackages() {
        val set = pkgAll.mapNotNull { safeOfNull { Json.encodeToString(it) } }.toSet()
        context.putStringSet(ModulePrefs, CONFIG_PACKAGE_LIST, set)
        applyPkgQuery(pkgQuery)
    }

    private fun persistActivities() {
        val set = actAll.mapNotNull { safeOfNull { Json.encodeToString(it) } }.toSet()
        context.putStringSet(ModulePrefs, CONFIG_ACTIVITY_LIST, set)
        applyActQuery(actQuery)
    }

    private fun resetFromStream(inputStream: InputStream?, version: String) {
        val packages = ArrayList<MemcConfigPackage>()
        val activities = ArrayList<MemcConfigActivity>()
        val stream = inputStream ?: safeOfNull {
            context.resources.openRawResource(R.raw.multimedia_pixelworks_apps_x7)
        } ?: return
        FileUtils.parseMemcXml(stream, packages, activities)
        if (version == "x7p") {
            activities.forEachIndexed { index, config ->
                activities[index] =
                    MemcConfigActivity(config.packName, config.activity, "258-10-0-0")
            }
        }
        val packageSet =
            packages.mapNotNull { safeOfNull { Json.encodeToString(it) } }.toSet()
        val activitySet =
            activities.mapNotNull { safeOfNull { Json.encodeToString(it) } }.toSet()
        if (packageSet.isNotEmpty() && activitySet.isNotEmpty()) {
            context.putStringSet(ModulePrefs, CONFIG_PACKAGE_LIST, packageSet)
            context.putStringSet(ModulePrefs, CONFIG_ACTIVITY_LIST, activitySet)
        }
    }
}

/**
 * 页面渲染体：状态/加载逻辑唯一一份。
 *
 * LazyItemScope 扩展：[PageSpec.fullContent] 挂在 LazyColumn 的 item 内，item 的高度约束无界，
 * 根容器必须用 `fillParentMaxHeight()` 取列表视口高度，否则整列高度为 0（子项 weight 分不到空间）→ 白屏。
 */
@Composable
internal fun LazyItemScope.MemcConfigContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state = remember { MemcConfigState(context) }
    memcRefresh = { state.reload() }

    var tabIndex by remember { mutableIntStateOf(0) }

    var showPkgDialog by remember { mutableStateOf(false) }
    var pkgTarget by remember { mutableStateOf<MemcConfigPackage?>(null) }
    var showActDialog by remember { mutableStateOf(false) }
    var actTarget by remember { mutableStateOf<MemcConfigActivity?>(null) }

    var showResetConfirm by remember { mutableStateOf(false) }
    var showVersionSheet by remember { mutableStateOf(false) }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                val stream = safeOfNull { context.contentResolver.openInputStream(uri) }
                if (stream != null) scope.launch { state.reset(stream, "") }
            }
        }

    // Import Xml / Reset 在顶栏溢出菜单里触发，事件期回调到本页状态
    MemcConfigPage.spec.actionHandler = { key ->
        when (key) {
            MemcConfigPage.ACTION_IMPORT_XML -> {
                FileUtils.checkDownloadDir(context, "LuckyTool")
                importLauncher.launch("text/xml")
            }

            MemcConfigPage.ACTION_RESET -> showResetConfirm = true
        }
    }

    LaunchedEffect(Unit) {
        state.reload()
    }

    Column(Modifier.fillParentMaxHeight()) {
        MemcTabRow(selected = tabIndex, onSelect = { tabIndex = it })
        when (tabIndex) {
            0 -> MemcPackagePanel(
                state = state,
                modifier = Modifier.weight(1f),
                onEdit = { target ->
                    pkgTarget = target
                    showPkgDialog = true
                },
            )

            1 -> MemcActivityPanel(
                state = state,
                modifier = Modifier.weight(1f),
                onEdit = { target ->
                    actTarget = target
                    showActDialog = true
                },
            )
        }
    }

    if (showPkgDialog) {
        MemcPackageDialog(
            context = context,
            initial = pkgTarget,
            onDismiss = { showPkgDialog = false },
            onSave = { new ->
                state.savePackage(pkgTarget, new)
                showPkgDialog = false
            },
            onDelete = { info ->
                state.deletePackage(info)
                showPkgDialog = false
            },
        )
    }

    if (showActDialog) {
        MemcActivityDialog(
            context = context,
            initial = actTarget,
            onDismiss = { showActDialog = false },
            onSave = { new ->
                state.saveActivity(actTarget, new)
                showActDialog = false
            },
            onDelete = { info ->
                state.deleteActivity(info)
                showActDialog = false
            },
        )
    }

    if (showResetConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.reset),
            message = stringResource(R.string.restore_frame_insertion_configuration_data),
            onDismiss = { showResetConfirm = false },
            onConfirm = {
                showResetConfirm = false
                showVersionSheet = true
            },
        )
    }

    if (showVersionSheet) {
        ActionListDialog(
            title = stringResource(R.string.memc_version_select_title),
            items = listOf("x7", "x7p").map { ChoiceItem(key = it, title = it) },
            onDismiss = { showVersionSheet = false },
            onSelect = { item ->
                // 动作列表语义：组件自己不关闭弹层，先关再执行（保持原来「关弹层 → 重置」的顺序）
                showVersionSheet = false
                scope.launch { state.reset(null, item.key) }
            },
        )
    }
}

/**
 * 顶部双 Tab：选中态由调用方持有，点击只回传下标。
 *
 * 控件形态由样式位给（Material 线是带下划线的次级 Tab 行，COUI 线是库的分段选择器，
 * 低版本回退等宽胶囊分段条）；两条线共用同一个下标语义，切换只改调用方的 [selected]。
 *
 * 水平内缩两条线统一取 pageInset：COUI 的分段条本来就要这段留白，material 的 Tab 行虽然能满幅，
 * 但同页的搜索框、新增卡与列表都从这一档起排，满幅会让 Tab 文案与下划线和内容列错位。
 */
@Composable
private fun MemcTabRow(selected: Int, onSelect: (Int) -> Unit) {
    currentPrefStyle().Tabs(
        labels = listOf("Packages", "Activitys"),
        selected = selected,
        onSelect = onSelect,
        modifier = Modifier.pageInset().padding(vertical = 8.dp),
    )
}

/**
 * 面板搜索框：仅搜索（无排序按钮）。
 *
 * 控件与配色由样式位给；水平内缩留在调用点，与下面列表的内容内缩对齐。
 */
@Composable
private fun MemcSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    placeholder: String,
) {
    currentPrefStyle().TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        modifier = Modifier
            .fillMaxWidth()
            .pageInset(),
        enabled = enabled,
        singleLine = true,
        leadingIcon = null,
    )
}

/**
 * 包名前缀面板：搜索 + 新增卡 + 列表。
 *
 * 行内文字与空态走行层原语，两条线共用一份排版；新增卡的水平内缩由 containerInset() 给——
 * COUI 线的卡片底色自带 16dp 内缩（CouiCard），material 线的卡片不自带，要自己补，
 * 否则会与搜索框、列表行错位。
 */
@Composable
internal fun MemcPackagePanel(
    state: MemcConfigState,
    modifier: Modifier = Modifier,
    onEdit: (MemcConfigPackage?) -> Unit,
) {
    val addCardModifier = currentPrefStyle().containerInset()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MemcSearchField(
            value = state.pkgQuery,
            onValueChange = { state.applyPkgQuery(it) },
            enabled = !state.pkgLoading,
            placeholder = "PackageName",
        )
        PrefCard(
            title = "＋ Add",
            onClick = { onEdit(null) },
            modifier = addCardModifier,
        )
        if (state.pkgFilter.isEmpty() && !state.pkgLoading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                PrefNote(
                    text = stringResource(R.string.no_memc_data),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(currentUiStyle().listRowSpacing),
            ) {
                itemsIndexed(
                    state.pkgFilter,
                    key = { _, info -> "${info.packName}|${info.rate}|${info.type}" },
                ) { index, info ->
                    AppItem(index = index, count = state.pkgFilter.size) {
                        AppListItem(
                            onClick = { onEdit(info) },
                            headlineContent = { PrefRowTitle(info.packName) },
                            supportingContent = {
                                Column {
                                    PrefRowSummary("Rate: ${info.rate}")
                                    PrefRowSummary("Type: ${info.type}")
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 精确 Activity 面板：搜索 + 新增卡 + 列表。
 *
 * 与包名面板同一套行层原语，[state] 的过滤与加载状态两条线共用；新增卡的内缩同样取 containerInset()。
 */
@Composable
internal fun MemcActivityPanel(
    state: MemcConfigState,
    modifier: Modifier = Modifier,
    onEdit: (MemcConfigActivity?) -> Unit,
) {
    val addCardModifier = currentPrefStyle().containerInset()
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MemcSearchField(
            value = state.actQuery,
            onValueChange = { state.applyActQuery(it) },
            enabled = !state.actLoading,
            placeholder = "PackageName / ActivityName",
        )
        PrefCard(
            title = "＋ Add",
            onClick = { onEdit(null) },
            modifier = addCardModifier,
        )
        if (state.actFilter.isEmpty() && !state.actLoading) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                PrefNote(
                    text = stringResource(R.string.no_memc_data),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(currentUiStyle().listRowSpacing),
            ) {
                itemsIndexed(
                    state.actFilter,
                    key = { _, info -> "${info.packName}|${info.activity}|${info.type}" },
                ) { index, info ->
                    AppItem(index = index, count = state.actFilter.size) {
                        AppListItem(
                            onClick = { onEdit(info) },
                            headlineContent = { PrefRowTitle(info.packName) },
                            supportingContent = {
                                Column {
                                    // activity 名可能很长：单行省略，不让行被撑高
                                    PrefRowSummary(info.activity, maxLines = 1)
                                    PrefRowSummary("Type: ${info.type}")
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}

/**
 * 只读输入框当「选择器按钮」用：输入框自身会消费点击
 * （foundation 的 tapPressTextFieldModifier 在 readOnly 时同样 consume 掉 down/up），
 * 挂在输入框上的 clickable 收不到事件，所以叠一层透明浮层接住点击。
 */
@Composable
private fun PickerField(
    onClick: () -> Unit,
    field: @Composable () -> Unit,
) {
    Box(Modifier.fillMaxWidth()) {
        field()
        Box(
            Modifier
                .matchParentSize()
                .clickable(onClick = onClick)
        )
    }
}

@Composable
internal fun MemcPackageDialog(
    context: Context,
    initial: MemcConfigPackage?,
    onDismiss: () -> Unit,
    onSave: (MemcConfigPackage) -> Unit,
    onDelete: (MemcConfigPackage) -> Unit,
) {
    var packName by remember(initial) { mutableStateOf(initial?.packName ?: "") }
    var rate by remember(initial) { mutableStateOf(initial?.rate ?: "") }
    var type by remember(initial) { mutableStateOf(initial?.type ?: "") }
    var showAppPicker by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val tips = stringResource(
        R.string.edit_memc_configuration_tips,
        CommandUtils.memcHdrConfigHelp,
    )
    // 校验与落盘只写一处：两条线共用同一个确认回调
    val confirm: () -> Unit = {
        if (packName.isBlank() || rate.isBlank() || type.isBlank()) {
            context.showToast("Data is incomplete!")
        } else {
            onSave(MemcConfigPackage(packName, rate, type))
        }
    }

    val canRemove = initial != null
    // 可以移除时点次级按钮执行移除，否则等同取消
    val onSecondary: () -> Unit = if (canRemove) {
        { showDeleteConfirm = true }
    } else {
        onDismiss
    }
    currentDialogStyle().Alert(
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(android.R.string.ok),
        onConfirm = confirm,
        secondaryLabel = stringResource(if (canRemove) R.string.remove else android.R.string.cancel),
        onSecondary = onSecondary,
        content = {
            // 外壳与按钮行两条线共用，正文的输入框由样式位按外观线画
            MemcPackageFields(
                packName = packName,
                onPackNameChange = { packName = it },
                rate = rate,
                onRateChange = { rate = it },
                type = type,
                onTypeChange = { type = it },
                tips = tips,
                onPickPackage = { showAppPicker = true },
            )
        },
    )

    if (showAppPicker) {
        AppPickerDialog(
            title = "PackageName",
            multiMode = false,
            onDismiss = { showAppPicker = false },
            onConfirm = { list ->
                if (list.isNotEmpty()) packName = list.first().packageName
                showAppPicker = false
            },
        )
    }

    if (showDeleteConfirm && initial != null) {
        ConfirmDialog(
            title = stringResource(R.string.remove),
            message = stringResource(
                R.string.confirm_to_delete_this_configuration,
                initial.packName,
            ),
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                onDelete(initial)
            },
        )
    }
}

/**
 * 包名配置弹窗正文：三个字段 + 提示，字段的外形与配色由样式位按外观线画。
 *
 * 只负责渲染与回调透传，校验、落盘与按钮行动由调用方统一持有。
 * 字段间 8dp 由容器给，两条线一致。
 *
 * 包名格只用于展示选中的应用：空白浮层接住点击后打开选择器（输入框自己会吞掉按下/抬起），
 * 所以它带 readOnly——material 线的输入框真的进只读态；COUI 库没有只读态，只作语义声明，
 * 实际可编辑性由浮层挡掉，值只能由选择器写入。
 */
@Composable
private fun MemcPackageFields(
    packName: String,
    onPackNameChange: (String) -> Unit,
    rate: String,
    onRateChange: (String) -> Unit,
    type: String,
    onTypeChange: (String) -> Unit,
    tips: String,
    onPickPackage: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PickerField(onClick = onPickPackage) {
            currentPrefStyle().TextField(
                value = packName,
                onValueChange = onPackNameChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = true,
                singleLine = true,
                leadingIcon = null,
                label = "PackageName",
                readOnly = true,
            )
        }
        currentPrefStyle().TextField(
            value = rate,
            onValueChange = onRateChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = true,
            singleLine = true,
            leadingIcon = null,
            label = "ScreenRate",
        )
        currentPrefStyle().TextField(
            value = type,
            onValueChange = onTypeChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = true,
            singleLine = true,
            leadingIcon = null,
            label = "Type",
        )
        PrefNote(
            text = tips,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Activity 选择项：[label] 只用于显示与搜索，落盘的仍是 [name]（类名）。 */
internal data class MemcActivityOption(val name: String, val label: String)

/**
 * Activity 选择项的过滤语义：label 与类名都参与匹配，两侧统一 lowercase。
 * （label 常以大写开头或为中文，大小写敏感的 `contains` 会让 "MainActivity" 这类输入查不到。）
 */
internal fun filterMemcActivityOptions(
    options: List<MemcActivityOption>,
    query: String,
): List<MemcActivityOption> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return options
    return options.filter {
        it.label.lowercase().contains(q) || it.name.lowercase().contains(q)
    }
}

/**
 * Activity 选择弹层：搜索框 + 列表（标题=label / 副标题=类名）。
 *
 * 行渲染、按钮行与空态统一交给 [ActionListDialog]：这里只留过滤与 key→选项的还原。
 * 行首图标不渲染（ChoiceItem 没有行首槽），显示信息由标题/副标题承担。
 */
@Composable
private fun MemcActivityPickerDialog(
    options: List<MemcActivityOption>,
    onDismiss: () -> Unit,
    onSelect: (MemcActivityOption) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(options, query) { filterMemcActivityOptions(options, query) }
    val items = filtered.map { ChoiceItem(key = it.name, title = it.label, summary = it.name) }
    ActionListDialog(
        title = stringResource(R.string.memc_activity_picker_title),
        items = items,
        onDismiss = onDismiss,
        onSelect = { item ->
            // 动作列表语义：先按 key 还原选项再回调，关不关闭由调用点决定（原来是先动作后关闭）
            options.firstOrNull { it.name == item.key }?.let(onSelect)
        },
        header = { MemcActivityQueryField(query = query, onQueryChange = { query = it }) },
    )
}

/**
 * 弹层内搜索框：页面级的 [MemcSearchField] 带水平内边距语义、不适用于弹层正文，
 * 所以这里单独定义；放大镜图标作为 leadingIcon。
 *
 * 图标的着色由调用方给（样式位只透传槽位，两条线的输入框对图标默认色的处理不同）：
 * 这里统一取 COUI 的 onSurface，material 线因此不再随聚焦在 onSurfaceVariant 与 onSurface 之间切换。
 * 图标沿用 M3 的 Icon（COUI 没有等价图标原语）。
 */
@Composable
private fun MemcActivityQueryField(query: String, onQueryChange: (String) -> Unit) {
    currentPrefStyle().TextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = stringResource(R.string.activity_search_hint),
        modifier = Modifier.fillMaxWidth(),
        enabled = true,
        singleLine = true,
        leadingIcon = {
            Icon(
                painterResource(R.drawable.ic_baseline_search_24),
                contentDescription = null,
                tint = CouiTheme.colorScheme.onSurface,
            )
        },
    )
}

@Composable
internal fun MemcActivityDialog(
    context: Context,
    initial: MemcConfigActivity?,
    onDismiss: () -> Unit,
    onSave: (MemcConfigActivity) -> Unit,
    onDelete: (MemcConfigActivity) -> Unit,
) {
    var packName by remember(initial) { mutableStateOf(initial?.packName ?: "") }
    var activity by remember(initial) { mutableStateOf(initial?.activity ?: "") }
    var type by remember(initial) { mutableStateOf(initial?.type ?: "") }
    var showAppPicker by remember { mutableStateOf(false) }
    var showActivityPicker by remember { mutableStateOf(false) }
    var activityOptions by remember { mutableStateOf(emptyList<MemcActivityOption>()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Activity 选择：先校验包名，再拉取 Activity 列表
    // label 在这里一次算好（loadLabel 会回落 labelRes → 应用名 → 包名），列表渲染不再逐行查 PackageManager
    val openActivityPicker: () -> Unit = {
        if (packName.isBlank()) {
            context.showToast("PackageName is null!")
        } else {
            scope.launch(Dispatchers.IO) {
                val packInfo = PackageUtils(context.packageManager)
                    .getPackageInfo(packName, PackageManager.GET_ACTIVITIES)
                val options = packInfo?.activities?.map { info ->
                    MemcActivityOption(
                        name = info.name,
                        label = safeOfNull { info.loadLabel(context.packageManager).toString() }
                            ?.takeIf { it.isNotBlank() } ?: info.name,
                    )
                } ?: emptyList()
                withContext(Dispatchers.Main) {
                    if (packInfo == null) {
                        context.showToast("App data is null!")
                    } else {
                        activityOptions = options
                        showActivityPicker = true
                    }
                }
            }
        }
    }

    val tips = stringResource(
        R.string.edit_memc_configuration_tips,
        CommandUtils.memcConfigHelp,
    )
    val confirm: () -> Unit = {
        if (packName.isBlank() || activity.isBlank() || type.isBlank()) {
            context.showToast("Data is incomplete!")
        } else {
            onSave(MemcConfigActivity(packName, activity, type))
        }
    }

    val canRemove = initial != null
    // 可以移除时点次级按钮执行移除，否则等同取消
    val onSecondary: () -> Unit = if (canRemove) {
        { showDeleteConfirm = true }
    } else {
        onDismiss
    }
    currentDialogStyle().Alert(
        onDismissRequest = onDismiss,
        confirmLabel = stringResource(android.R.string.ok),
        onConfirm = confirm,
        secondaryLabel = stringResource(if (canRemove) R.string.remove else android.R.string.cancel),
        onSecondary = onSecondary,
        content = {
            // 外壳与按钮行两条线共用，正文的输入框由样式位按外观线画
            MemcActivityFields(
                packName = packName,
                onPackNameChange = { packName = it },
                activity = activity,
                onActivityChange = { activity = it },
                type = type,
                onTypeChange = { type = it },
                tips = tips,
                onPickPackage = { showAppPicker = true },
                onPickActivity = openActivityPicker,
            )
        },
    )

    if (showAppPicker) {
        AppPickerDialog(
            title = "PackageName",
            multiMode = false,
            onDismiss = { showAppPicker = false },
            onConfirm = { list ->
                if (list.isNotEmpty()) packName = list.first().packageName
                showAppPicker = false
            },
        )
    }

    if (showActivityPicker) {
        MemcActivityPickerDialog(
            options = activityOptions,
            onDismiss = { showActivityPicker = false },
            onSelect = { option ->
                activity = option.name
                showActivityPicker = false
            },
        )
    }

    if (showDeleteConfirm && initial != null) {
        ConfirmDialog(
            title = stringResource(R.string.remove),
            message = stringResource(
                R.string.confirm_to_delete_this_configuration,
                initial.activity,
            ),
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                onDelete(initial)
            },
        )
    }
}

/**
 * Activity 配置弹窗正文：包名 / ActivityName / 类型 + 提示，字段的外形与配色由样式位按外观线画。
 *
 * 包名与 ActivityName 都是选择结果、只能由选择器写入，所以两格用 [PickerField] 把点击转给选择器，
 * 并带 readOnly——material 线的输入框真的进只读态；COUI 库没有只读态，只作语义声明，
 * 实际可编辑性由浮层挡掉。
 * ActivityName 在未选包名时会由 [onPickActivity] 先弹提示，这里不重复校验。
 * 字段间 8dp 由容器给，两条线一致。
 */
@Composable
private fun MemcActivityFields(
    packName: String,
    onPackNameChange: (String) -> Unit,
    activity: String,
    onActivityChange: (String) -> Unit,
    type: String,
    onTypeChange: (String) -> Unit,
    tips: String,
    onPickPackage: () -> Unit,
    onPickActivity: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PickerField(onClick = onPickPackage) {
            currentPrefStyle().TextField(
                value = packName,
                onValueChange = onPackNameChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = true,
                singleLine = true,
                leadingIcon = null,
                label = "PackageName",
                readOnly = true,
            )
        }
        PickerField(onClick = onPickActivity) {
            currentPrefStyle().TextField(
                value = activity,
                onValueChange = onActivityChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = true,
                singleLine = true,
                leadingIcon = null,
                label = "ActivityName",
                readOnly = true,
            )
        }
        currentPrefStyle().TextField(
            value = type,
            onValueChange = onTypeChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = true,
            singleLine = true,
            leadingIcon = null,
            label = "Type",
        )
        PrefNote(
            text = tips,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
