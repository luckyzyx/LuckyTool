package com.luckyzyx.luckytool.ui.fragment.settings

import android.app.KeyguardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.ArraySet
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import androidx.preference.DropDownPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.highcapable.kavaref.extension.classOf
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.application.MyApplication
import com.luckyzyx.luckytool.utils.AppUtils
import com.luckyzyx.luckytool.utils.BiometricUtils
import com.luckyzyx.luckytool.utils.DonateUtils
import com.luckyzyx.luckytool.utils.FileUtils
import com.luckyzyx.luckytool.utils.IntentPrefs
import com.luckyzyx.luckytool.utils.IntentUtils
import com.luckyzyx.luckytool.utils.LogUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.OtherPrefs
import com.luckyzyx.luckytool.utils.RemotePreferenceDataStore
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.appPrefs
import com.luckyzyx.luckytool.utils.backupAllPrefs
import com.luckyzyx.luckytool.utils.base64Decode
import com.luckyzyx.luckytool.utils.base64Encode
import com.luckyzyx.luckytool.utils.clearAllPrefs
import com.luckyzyx.luckytool.utils.dialogCentered
import com.luckyzyx.luckytool.utils.formatDate
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.getOSVersionName
import com.luckyzyx.luckytool.utils.isZh
import com.luckyzyx.luckytool.utils.logcatToFile
import com.luckyzyx.luckytool.utils.navigatePage
import com.luckyzyx.luckytool.utils.openUrl
import com.luckyzyx.luckytool.utils.putBoolean
import com.luckyzyx.luckytool.utils.putInt
import com.luckyzyx.luckytool.utils.putString
import com.luckyzyx.luckytool.utils.putStringSet
import com.luckyzyx.luckytool.utils.showToast
import org.json.JSONArray
import org.json.JSONObject
import org.lsposed.lsparanoid.Obfuscate
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import kotlin.system.exitProcess

@Obfuscate
class SettingsFragment : PreferenceFragmentCompat() {
    private val backupData = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) {
        if (it != null) {
            writeBackupData(requireActivity(), it)
        }
    }
    private val restoreData = registerForActivityResult(ActivityResultContracts.GetContent()) {
        if (it != null) {
            val entryData = FileUtils.readFromUri(requireActivity(), it)
            checkRestoreData(requireActivity(), entryData)
        }
    }

    private fun writeBackupData(context: Context, uri: Uri) {
        val json = JSONObject().apply {
            put("osCode", getOSVersionCode)
        }
        val dataMapList = context.backupAllPrefs(
            ModulePrefs, IntentPrefs, SettingsPrefs, OtherPrefs
        )
        dataMapList.keys.forEach { prefs ->
            val jsons = JSONObject()
            val data = dataMapList[prefs]
            data?.keys?.forEach { key ->
                data[key].apply {
                    if (this?.javaClass?.simpleName == "HashSet") {
                        val arr = JSONArray()
                        val value = (this as HashSet<*>).toTypedArray()
                        for (i in value.indices) {
                            arr.put(value[i])
                        }
                        jsons.put(key, arr)
                    } else {
                        jsons.put(key, this)
                    }
                }
            }
            json.put(prefs, jsons)
        }
        val str = base64Encode(json.toString())
        try {
            context.contentResolver.openFileDescriptor(uri, "w")?.use { its ->
                FileOutputStream(its.fileDescriptor).use {
                    it.write(str.toByteArray())
                }
            }
            context.showToast(getString(R.string.data_backup_complete))
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            context.showToast(getString(R.string.data_backup_error))
        } catch (e: IOException) {
            e.printStackTrace()
            context.showToast(getString(R.string.data_backup_error))
        }
    }

    private fun checkRestoreData(context: Context, data: String) {
        val json = JSONObject(base64Decode(data))
        val osCode = json.optInt("osCode")
        if (osCode > 0 && osCode != getOSVersionCode) {
            MaterialAlertDialogBuilder(context, dialogCentered).apply {
                setMessage(
                    """
                    ${getString(R.string.data_backup_data_version)}: ${getOSVersionName(osCode)}
                    ${getString(R.string.data_current_system_version)}: $getOSVersionName
                    
                    ${getString(R.string.data_restore_version_tips)}
                """.trimIndent()
                )
                setPositiveButton(android.R.string.ok, null)
                setNeutralButton(R.string.ignore) { _, _ ->
                    writeRestoreData(context, json)
                }
                show()
            }
        } else writeRestoreData(context, json)
    }

    private fun writeRestoreData(context: Context, json: JSONObject) {
        if (json.length() <= 0) return
        json.remove("osCode")
        json.keys().forEach { prefs ->
            val prefsDatas = json.getJSONObject(prefs)
            if (prefsDatas.length() > 0) {
                prefsDatas.keys().forEach { key ->
                    val value = prefsDatas.get(key)
                    when (value.javaClass.simpleName) {
                        "Boolean" -> context.putBoolean(prefs, key, value as Boolean)
                        "Integer" -> context.putInt(prefs, key, value as Int)
                        "JSONArray" -> {
                            val set = ArraySet<String>()
                            val list = value as JSONArray
                            for (i in 0 until list.length()) {
                                set.add(list[i] as String)
                            }
                            context.putStringSet(prefs, key, set)
                        }

                        "String" -> context.putString(prefs, key, value as String)
                        else -> context.showToast("Error: $key")
                    }
                }
            }
        }
        context.showToast(getString(R.string.data_restore_complete))
        (activity as MainActivity).restart()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore =
            RemotePreferenceDataStore(requireContext().appPrefs(SettingsPrefs))
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            //主题
            addPreference(PreferenceCategory(context).apply {
                setTitle(R.string.theme_title)
                setSummary(R.string.theme_title_summary)
                isIconSpaceReserved = false
            })
            addPreference(SwitchPreference(context).apply {
                key = "use_dynamic_color"
                title = getString(R.string.use_dynamic_color)
                summary = getString(R.string.use_dynamic_color_summary)
                setDefaultValue(true)
                isVisible = DynamicColors.isDynamicColorAvailable()
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, _ ->
                    (requireActivity().application as MyApplication).reloadAllActivities()
                    true
                }
            })
            addPreference(DropDownPreference(context).apply {
                key = "dark_theme"
                title = getString(R.string.dark_theme)
                summary = "%s"
                setEntries(R.array.dark_theme)
                entryValues = arrayOf("0", "1", "2")
                setDefaultValue("0")
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, _ ->
                    (requireActivity().application as MyApplication).reloadAllActivities()
                    true
                }
            })
            //其他
            addPreference(PreferenceCategory(context).apply {
                title = getString(R.string.other_settings)
                isIconSpaceReserved = false
            })
            addPreference(SwitchPreference(context).apply {
                key = "auto_check_update"
                title = getString(R.string.auto_check_update)
                summary = getString(R.string.auto_check_update_summary)
                setDefaultValue(true)
                isIconSpaceReserved = false
            })
            addPreference(SwitchPreference(context).apply {
                key = "enable_biometric_unlock_verification"
                title = getString(R.string.enable_biometric_unlock_verification)
                setDefaultValue(false)
                isVisible =
                    requireActivity().getSystemService(classOf<KeyguardManager>()).isDeviceSecure
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, any ->
                    val enable = any as Boolean
                    if (enable) {
                        BiometricUtils.showBiometricPrompt(requireActivity(), onSucceed = {
                            isChecked = requireActivity().putBoolean(SettingsPrefs, key, true)
                            LogUtils.d(
                                "setOnPreferenceChangeListener", "showSucceedBiometric",
                                "${it.authenticationType}", true
                            )
                        })
                        false
                    } else true
                }
            })
            addPreference(SwitchPreference(context).apply {
                key = "tile_auto_start"
                title = getString(R.string.tile_auto_start)
                summary = getString(R.string.tile_auto_start_summary)
                setDefaultValue(true)
                isIconSpaceReserved = false
            })
            addPreference(SwitchPreference(context).apply {
                key = "hide_function_page_icon"
                title = getString(R.string.hide_function_page_icon)
                setDefaultValue(false)
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, _ ->
                    (activity as MainActivity).restart()
                    true
                }
            })
            addPreference(SwitchPreference(context).apply {
                key = "hide_desktop_module_icon"
                setDefaultValue(false)
                title = getString(R.string.hide_desktop_module_icon)
                summary = getString(R.string.hide_desktop_module_icon_summary)
                isIconSpaceReserved = false
                setOnPreferenceChangeListener { _, newValue ->
                    AppUtils(context).setComponentDisabled(
                        ComponentName(
                            context.packageName, "${context.packageName}.Hide"
                        ), newValue as Boolean
                    )
                    true
                }
            })
            //备份
            addPreference(PreferenceCategory(context).apply {
                title = getString(R.string.backup_restore_clear)
                key = "backup_restore_clear"
                isIconSpaceReserved = false
            })
            addPreference(Preference(context).apply {
                title = getString(R.string.get_log_cat_log)
                key = "get_log_cat_log"
                isPersistent = false
                isVisible = false
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    val name = "logcat_" + formatDate("yyMMdd_HHmmss") + ".log"
                    val file = FileUtils.checkLogCatDir(context, name)
                    if (logcatToFile(file)) FileUtils.shareFile(context, "Share LogCat File", file)
                    true
                }
            })
            addPreference(Preference(context).apply {
                title = getString(R.string.backup_data)
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    FileUtils.checkDownloadDir(context, "LuckyTool").apply {
                        if (isFile) delete()
                        if (!exists()) mkdirs()
                    }
                    val fileName = "LuckyTool_" + formatDate("yyyyMMdd_HHmmss") + "_backup.json"
                    if (IntentUtils(requireActivity()).checkCreateDocument()) {
                        backupData.launch(fileName)
                    } else requireActivity().showToast("Intent Create Document Error!")
                    true
                }
            })
            addPreference(Preference(context).apply {
                title = getString(R.string.restore_data)
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    FileUtils.checkDownloadDir(context, "LuckyTool").apply {
                        if (isFile) delete()
                        if (!exists()) mkdirs()
                    }
                    restoreData.launch("application/json")
                    true
                }
            })
            addPreference(Preference(context).apply {
                title = getString(R.string.clear_all_data)
                summary = getString(R.string.clear_all_data_summary)
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    MaterialAlertDialogBuilder(context).apply {
                        setMessage(getString(R.string.clear_all_data_message))
                        setPositiveButton(android.R.string.ok) { _, _ ->
                            context.clearAllPrefs(
                                ModulePrefs, IntentPrefs, SettingsPrefs, OtherPrefs
                            )
                            exitProcess(0)
                        }
                        setNeutralButton(android.R.string.cancel, null)
                        show()
                    }
                    true
                }
            })
            //关于
            addPreference(PreferenceCategory(context).apply {
                setTitle(R.string.about_title)
                isIconSpaceReserved = false
            })
            addPreference(Preference(context).apply {
                title = getString(R.string.donate)
                summary = getString(R.string.donate_summary)
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    val donateList = arrayListOf(
                        getString(R.string.qq),
                        getString(R.string.wechat),
                        getString(R.string.alipay),
                        getString(R.string.donation_list)
                    )
                    if (!isZh(context)) {
                        donateList.add(3, getString(R.string.patreon))
                        donateList.add(4, getString(R.string.paypal))
                    }
                    MaterialAlertDialogBuilder(context).apply {
                        setItems(donateList.toTypedArray()) { _, which ->
                            when (which) {
                                0 -> DonateUtils.showQRCode(context, which)
                                1 -> DonateUtils.showQRCode(context, which)
                                2 -> DonateUtils.showQRCode(context, which)
                                3 -> if (isZh(context)) {
                                    findNavController().navigatePage(
                                        R.id.donateFragment,
                                        getString(R.string.donation_list)
                                    )
                                } else startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        "https://www.patreon.com/LuckyTool".toUri()
                                    )
                                )

                                4 -> startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW, "https://paypal.me/luckyzyx".toUri()
                                    )
                                )

                                5 -> findNavController().navigatePage(
                                    R.id.donateFragment,
                                    getString(R.string.donation_list)
                                )
                            }
                        }
                    }.show()
                    true
                }
            })
            addPreference(Preference(context).apply {
                title = getString(R.string.feedback_download)
                summary = getString(R.string.feedback_download_summary)
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    val updatelist = arrayOf(
                        getString(R.string.coolmarket),
                        getString(R.string.module_doc),
                        getString(R.string.qq_chat_group),
                        getString(R.string.qq_channel),
                        getString(R.string.telegram_channel),
                        getString(R.string.lsposed_repo)
                    )
                    MaterialAlertDialogBuilder(context).setItems(updatelist) { _, which ->
                        when (which) {
                            0 -> context.openUrl("coolmarket://u/1930284")
                            1 -> context.openUrl("https://luckyzyx.gitlab.io/LuckyTool_Doc")
                            2 -> context.openUrl("http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=3fYu6lT8IHrBPKAfFTNSHbd8wcWX0oGs&authKey=dyIpjTWH8KWHMU3v6gI05T0bAzr6XigJKasMiCwmco1%2F8BRtPCN%2B1zOGgXyK7IUB&noverify=0&group_code=663884734")
                            3 -> context.openUrl("https://pd.qq.com/s/ahjm4zyxb")
                            4 -> context.openUrl("https://t.me/LuckyTool")
                            5 -> context.openUrl("https://modules.lsposed.org/module/com.luckyzyx.luckytool")
                        }
                    }.show()
                    true
                }
            })
            addPreference(Preference(context).apply {
                title = getString(R.string.participate_translation)
                summary = getString(R.string.participate_translation_summary)
                isIconSpaceReserved = false
                setOnPreferenceClickListener {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW, "https://github.com/luckyzyx/LuckyTool-Localization".toUri()
                        )
                    )
                    true
                }
            })
        }
    }
}