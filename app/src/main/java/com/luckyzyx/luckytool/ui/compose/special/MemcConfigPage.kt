package com.luckyzyx.luckytool.ui.compose.special

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.util.ArraySet
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.data.MemcConfigActivity
import com.luckyzyx.luckytool.data.MemcConfigPackage
import com.luckyzyx.luckytool.ui.components.AppPickerDialog
import com.luckyzyx.luckytool.ui.compose.components.PrefCard
import com.luckyzyx.luckytool.ui.compose.components.PrefGroup
import com.luckyzyx.luckytool.ui.compose.components.PrefRow
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedItem
import com.luckyzyx.luckytool.ui.compose.components.material.SegmentedListItem
import com.luckyzyx.luckytool.ui.compose.scopes.ScopePageSpec
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
import com.luckyzyx.luckytool.ui.theme.LocalUiMode
import com.luckyzyx.luckytool.ui.theme.UiMode

internal var memcRefresh: (suspend () -> Unit)? = null
private const val CONFIG_PACKAGE_LIST = GlobalKeyValue.memcConfigPackageList
private const val CONFIG_ACTIVITY_LIST = GlobalKeyValue.memcConfigActivityList

/**
 * P4 特殊页：Memc 帧插入配置（旧 MemcConfigFragment 1:1 迁移）。
 * 双 Tab（Packages/Activitys）+ 搜索 + 下拉刷新 + Import Xml / Reset。
 */
object MemcConfigPage {

    val spec = ScopePageSpec(
        pageKey = "memc_config",
        prefsName = ModulePrefs,
        packName = "",
        scopes = arrayOf(),
        restartEnabled = false,
        onRefresh = { memcRefresh?.invoke() },
        fullContent = { _ ->
            when (LocalUiMode.current) {
                UiMode.Miuix -> MemcConfigContentMiuix()
                UiMode.Material -> MemcConfigContent()
            }
        },
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

    fun savePackage(old: MemcConfigPackage?, new: MemcConfigPackage) {
        val index = old?.let { pkgAll.indexOf(it) } ?: -1
        if (index != -1) pkgAll[index] = new else pkgAll.add(new)
        persistPackages()
    }

    fun deletePackage(info: MemcConfigPackage) {
        pkgAll.remove(info)
        persistPackages()
    }

    fun saveActivity(old: MemcConfigActivity?, new: MemcConfigActivity) {
        val index = old?.let { actAll.indexOf(it) } ?: -1
        if (index != -1) actAll[index] = new else actAll.add(new)
        persistActivities()
    }

    fun deleteActivity(info: MemcConfigActivity) {
        actAll.remove(info)
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
