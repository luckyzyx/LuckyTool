package com.luckyzyx.luckytool.ui.fragment

import android.content.Intent
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import androidx.navigation.fragment.findNavController
import androidx.preference.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.highcapable.yukihookapi.hook.xposed.prefs.ui.ModulePreferenceFragment
import com.joom.paranoid.Obfuscate
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.ui.activity.MainActivity
import com.luckyzyx.luckytool.ui.adapter.DonateListAdapter
import com.luckyzyx.luckytool.utils.data.*
import com.luckyzyx.luckytool.utils.tools.clearAll
import kotlin.system.exitProcess

@Obfuscate
class SettingsFragment : PreferenceFragmentCompat(), OnSharedPreferenceChangeListener {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.sharedPreferencesName = SettingsPrefs
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            addPreference(
                PreferenceCategory(context).apply {
                    setTitle(R.string.theme_title)
                    setSummary(R.string.theme_title_summary)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(context).apply {
                    key = "use_dynamic_color"
                    setDefaultValue(false)
                    setTitle(R.string.use_dynamic_color)
                    setSummary(R.string.use_dynamic_color_summary)
                    isIconSpaceReserved = false
                    setOnPreferenceChangeListener { _, _ ->
                        (activity as MainActivity).restart()
                        true
                    }
                }
            )
            addPreference(
                DropDownPreference(context).apply {
                    key = "dark_theme"
                    title = getString(R.string.dark_theme)
                    summary = "%s"
                    entries = resources.getStringArray(R.array.dark_theme)
                    entryValues = resources.getStringArray(R.array.dark_theme_value)
                    setDefaultValue("MODE_NIGHT_FOLLOW_SYSTEM")
                    isIconSpaceReserved = false
                    setOnPreferenceChangeListener { _, _ ->
                        (activity as MainActivity).restart()
                        true
                    }
                }
            )
            addPreference(
                DropDownPreference(context).apply {
                    key = "language"
                    title = getString(R.string.language)
                    summary = "%s"
                    entries = resources.getStringArray(R.array.language)
                    entryValues = resources.getStringArray(R.array.language_value)
                    setDefaultValue("SYSTEM")
                    isIconSpaceReserved = false
                    setOnPreferenceChangeListener { _, _ ->
                        (activity as MainActivity).restart()
                        true
                    }
                    isVisible = false
                }
            )
            addPreference(
                PreferenceCategory(context).apply {
                    title = getString(R.string.other_settings)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(context).apply {
                    key = "auto_check_update"
                    title = getString(R.string.auto_check_update)
                    summary = getString(R.string.auto_check_update_summary)
                    setDefaultValue(true)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                SwitchPreference(context).apply {
                    key = "hide_xp_page_icon"
                    title = getString(R.string.hide_xp_page_icon)
                    setDefaultValue(false)
                    isIconSpaceReserved = false
                    setOnPreferenceChangeListener { _, _ ->
                        (activity as MainActivity).restart()
                        true
                    }
                }
            )
            addPreference(
                SwitchPreference(context).apply {
                    key = "hide_desktop_appicon"
                    setDefaultValue(false)
                    title = getString(R.string.hide_desktop_appicon)
                    summary = getString(R.string.hide_desktop_appicon_summary)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                Preference(context).apply {
                    title = getString(R.string.clear_all_data)
                    summary = getString(R.string.clear_all_data_summary)
                    isIconSpaceReserved = false
                    setOnPreferenceClickListener {
                        MaterialAlertDialogBuilder(context).apply {
                            setMessage(getString(R.string.clear_all_data_message))
                            setPositiveButton(android.R.string.ok) { _, _ ->
                                context.clearAll(
                                    SettingsPrefs,
                                    XposedPrefs,
                                    OtherPrefs,
                                    MagiskPrefs
                                )
                                exitProcess(0)
                            }
                            setNeutralButton(android.R.string.cancel, null)
                            show()
                        }
                        true
                    }
                }
            )
            addPreference(
                PreferenceCategory(context).apply {
                    setTitle(R.string.about_title)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                Preference(context).apply {
                    title = getString(R.string.donate)
                    summary = getString(R.string.donate_summary)
                    isIconSpaceReserved = false
                    setOnPreferenceClickListener {
                        val donateList = arrayOf(
                            getString(R.string.qq),
                            getString(R.string.wechat),
                            getString(R.string.alipay),
                            getString(R.string.donation_list)
                        )
                        MaterialAlertDialogBuilder(context).apply {

                            setItems(donateList) { _, which ->
                                when (which) {
                                    0 -> {
                                        val dialog = MaterialAlertDialogBuilder(context, dialogCentered).apply {
                                            setTitle(getString(R.string.qq))
                                            setView(R.layout.layout_donate_dialog)
                                        }.show()
                                        dialog.findViewById<MaterialTextView>(R.id.donate_message)?.text =
                                            getString(R.string.donate_message)
                                        dialog.findViewById<ImageView>(R.id.donate_image)
                                            ?.setImageBitmap(baseDecode(Base64Code.qqCode))
                                    }
                                    1 -> {
                                        val dialog = MaterialAlertDialogBuilder(context, dialogCentered).apply {
                                            setTitle(getString(R.string.wechat))
                                            setView(R.layout.layout_donate_dialog)
                                        }.show()
                                        dialog.findViewById<MaterialTextView>(R.id.donate_message)?.text =
                                            getString(R.string.donate_message)
                                        dialog.findViewById<ImageView>(R.id.donate_image)
                                            ?.setImageBitmap(baseDecode(Base64Code.wechatCode))
                                    }
                                    2 -> {
                                        val dialog = MaterialAlertDialogBuilder(context, dialogCentered).apply {
                                            setTitle(getString(R.string.alipay))
                                            setView(R.layout.layout_donate_dialog)
                                        }.show()
                                        dialog.findViewById<MaterialTextView>(R.id.donate_message)?.text =
                                            getString(R.string.donate_message)
                                        dialog.findViewById<ImageView>(R.id.donate_image)
                                            ?.setImageBitmap(baseDecode(Base64Code.alipayCode))
                                    }
                                    3 -> {
                                        MaterialAlertDialogBuilder(context, dialogCentered).apply {
                                            setTitle(getString(R.string.donation_list))
                                            setView(
                                                RecyclerView(context).apply {
                                                    setPadding(0, 10.dp, 0, 10.dp)
                                                    adapter = DonateListAdapter(
                                                        context,
                                                        DonateData.getDonateList()
                                                    )
                                                    layoutManager = LinearLayoutManager(context)
                                                }
                                            )
                                        }.show()
                                    }
                                }
                            }
                        }.show()
                        true
                    }
                }
            )
            addPreference(
                Preference(context).apply {
                    title = getString(R.string.feedback_download)
                    summary = getString(R.string.feedback_download_summary)
                    isIconSpaceReserved = false
                    setOnPreferenceClickListener {
                        val updatelist = arrayOf(
                            getString(R.string.coolmarket),
                            getString(R.string.telegram_channel),
                            getString(R.string.telegram_group),
                            getString(R.string.lsposed_repo)
                        )
                        MaterialAlertDialogBuilder(context)
                            .setItems(updatelist) { _, which ->
                                when (which) {
                                    0 -> startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("coolmarket://u/1930284")
                                        )
                                    )
                                    1 -> startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://t.me/LuckyTool")
                                        )
                                    )
                                    2 -> startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://t.me/+F42pfv-c0h4zNDc9")
                                        )
                                    )
                                    3 -> startActivity(
                                        Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://modules.lsposed.org/module/com.luckyzyx.luckytool")
                                        )
                                    )
                                }
                            }.show()
                        true
                    }
                }
            )
            addPreference(
                Preference(context).apply {
                    title = getString(R.string.participate_translation)
                    summary = getString(R.string.participate_translation_summary)
                    isIconSpaceReserved = false
                    setOnPreferenceClickListener {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://crwd.in/luckytool")
                            )
                        )
                        true
                    }
                }
            )
            addPreference(
                Preference(context).apply {
                    setTitle(R.string.open_source)
                    setSummary(R.string.open_source_summary)
                    isIconSpaceReserved = false
                    setOnPreferenceClickListener {
                        findNavController().navigate(R.id.action_nav_setting_to_sourceFragment)
                        true
                    }
                }
            )
            addPreference(
                Preference(context).apply {
                    title = getString(R.string.privacy_agreement)
                    summary = getString(R.string.read_agreement)
                    isIconSpaceReserved = false
                    setOnPreferenceClickListener {
                        (activity as MainActivity).initAgreement(false)
                        true
                    }
                }
            )
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "hide_desktop_appicon") sharedPreferences?.let {
            requireActivity().setDesktopIcon(
                it.getBoolean("hide_desktop_appicon", false)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        preferenceScreen.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
    }
}

@Obfuscate
class SourceFragment : ModulePreferenceFragment() {
    override fun onCreatePreferencesInModuleApp(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceScreen = preferenceManager.createPreferenceScreen(requireActivity()).apply {
            addPreference(
                PreferenceCategory(context).apply {
                    setTitle(R.string.open_source)
                    isIconSpaceReserved = false
                }
            )
            addPreference(
                Preference(context).apply {
                    title = "Xposed"
                    summary = "rovo89 , Apache License 2.0"
                    isIconSpaceReserved = false
                    intent =
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/rovo89/Xposed"))
                }
            )
            addPreference(
                Preference(context).apply {
                    title = "LSPosed"
                    summary = "LSPosed , GPL-3.0 License"
                    isIconSpaceReserved = false
                    intent =
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/LSPosed/LSPosed"))
                }
            )
            addPreference(
                Preference(context).apply {
                    title = "YukiHookAPI"
                    summary = "fankes , MIT License"
                    isIconSpaceReserved = false
                    intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/fankes/YukiHookAPI")
                    )
                }
            )
            addPreference(
                Preference(context).apply {
                    title = "ColorOSNotifyIcon"
                    summary = "fankes , AGPL-3.0 License"
                    isIconSpaceReserved = false
                    intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/fankes/ColorOSNotifyIcon")
                    )
                }
            )
            addPreference(
                Preference(context).apply {
                    title = "ColorOSTool"
                    summary = "Oosl , GPL-3.0 License"
                    isIconSpaceReserved = false
                    intent =
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Oosl/ColorOSTool"))
                }
            )
            addPreference(
                Preference(context).apply {
                    title = "WooBoxForColorOS"
                    summary = "Simplicity-Team , GPL-3.0 License"
                    isIconSpaceReserved = false
                    intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/Simplicity-Team/WooBoxForColorOS")
                    )
                }
            )
            addPreference(
                Preference(context).apply {
                    title = "CorePatch"
                    summary = "LSPosed , GPL-2.0 license"
                    isIconSpaceReserved = false
                    intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/LSPosed/CorePatch")
                    )
                }
            )
            addPreference(
                Preference(context).apply {
                    title = "Disable-FLAG_SECURE"
                    summary = "VarunS2002 , GPL-3.0 license"
                    isIconSpaceReserved = false
                    intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/VarunS2002/Xposed-Disable-FLAG_SECURE")
                    )
                }
            )
        }
    }
}