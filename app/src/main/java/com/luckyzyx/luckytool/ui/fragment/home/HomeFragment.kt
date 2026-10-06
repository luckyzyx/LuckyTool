package com.luckyzyx.luckytool.ui.fragment.home

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.highcapable.betterandroid.ui.extension.view.textColor
import com.luckyzyx.luckytool.BuildConfig
import com.luckyzyx.luckytool.IGlobalFuncController
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.databinding.FragmentHomeBinding
import com.luckyzyx.luckytool.service.GlobalFuncService
import com.luckyzyx.luckytool.ui.fragment.base.BaseFragment
import com.luckyzyx.luckytool.ui.service.XposedServiceBridge
import com.luckyzyx.luckytool.utils.DeviceUtils
import com.luckyzyx.luckytool.utils.DonateUtils
import com.luckyzyx.luckytool.utils.RestartMenuUtils
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.ThemeUtils
import com.luckyzyx.luckytool.utils.UpdateUtils
import com.luckyzyx.luckytool.utils.copyStr
import com.luckyzyx.luckytool.utils.dialogCentered
import com.luckyzyx.luckytool.utils.dp
import com.luckyzyx.luckytool.utils.getBoolean
import com.luckyzyx.luckytool.utils.getDeviceInfo
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.getString
import com.luckyzyx.luckytool.utils.getVersionCode
import com.luckyzyx.luckytool.utils.getVersionName
import com.luckyzyx.luckytool.utils.isZh
import com.luckyzyx.luckytool.utils.putBoolean
import com.luckyzyx.luckytool.utils.putString
import com.luckyzyx.luckytool.utils.setupMenuProvider
import com.luckyzyx.luckytool.utils.showToast
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class HomeFragment : BaseFragment<FragmentHomeBinding>(), MenuProvider {

    var dexOptimizeDialog: AlertDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        setupMenuProvider(this)
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val isDev = requireActivity().getBoolean(SettingsPrefs, "hidden_function")
        if (requireActivity().getBoolean(SettingsPrefs, "auto_check_update", true)) {
            UpdateUtils(requireActivity(), isDev)
                .checkUpdate { versionName, versionCode, function ->
                    if (getVersionCode < versionCode) {
                        function()
                        binding.updateView.apply {
                            isVisible = true
                            setOnClickListener { function() }
                        }
                        binding.updateInfo.apply {
                            text =
                                getString(R.string.check_update_hint) + "  -->  $versionName($versionCode)"
                        }
                    }
                    binding.statusCard.apply {
                        if (isDev) setOnLongClickListener {
                            function()
                            true
                        }
                    }
                }
        }

        binding.systemInfo.apply {
            setOnLongClickListener {
                context.copyStr(DeviceUtils.getOTACOnfigs())
                context.showToast("Copy Device OTA Data Success!")
                true
            }
        }

        binding.donateTvTitle.text = getString(R.string.donate_tv_title) + " by: 忆清鸣、luckyzyx"
        binding.donateTvView.apply {
            setOnClickListener {
                val url = if (isZh(context)) "https://docs.qq.com/doc/DS2ZDZlNIeUlpdlV1"
                else "https://luckyzyx.github.io/LuckyTool_Doc/en/donate"
                startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            }
            setOnLongClickListener {
                val donateList = arrayListOf(
                    getString(R.string.qq),
                    getString(R.string.wechat),
                    getString(R.string.alipay),
                )
                if (!isZh(context)) {
                    donateList.add(3, getString(R.string.patreon))
//                    donateList.add(4, getString(R.string.paypal))
                }
                MaterialAlertDialogBuilder(context).apply {
                    setItems(donateList.toTypedArray()) { _, which ->
                        when (which) {
                            0 -> DonateUtils.showQRCode(context, which)
                            1 -> DonateUtils.showQRCode(context, which)
                            2 -> DonateUtils.showQRCode(context, which)
                            3 -> if (!isZh(context)) startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "https://www.patreon.com/LuckyTool".toUri()
                                )
                            )
                        }
                    }
                }.show()
                true
            }
        }

        binding.authorized.apply {
            if (isZh(context)) {
                isVisible = true
                textSize = 16F
                text = context.getString(R.string.authorized)
                textColor = Color.RED
            }
            setOnClickListener {
                val url = "https://luckyzyx.github.io/LuckyTool_Doc/use/download_link"
                startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
            }
        }

        binding.tv.apply {
            isVisible = false
        }
    }

    private fun initSystemInfoView() {
        GlobalFuncService.get(activity) {
            if (view == null) return@get
            val deviceInfo = activity?.getDeviceInfo(it)
            if (deviceInfo.isNullOrBlank()) return@get
            binding.systemInfoLoading.isVisible = false
            binding.systemInfo.apply {
                gravity = Gravity.START
                text = deviceInfo
                isVisible = true
            }
            checkDexOptimize(it)
        }
    }

    private fun checkDexOptimize(controller: IGlobalFuncController?) {
        if (getOSVersionCode < 34) return
        val getOs = requireActivity().getString(SettingsPrefs, "current_os_version", "")
        val curOs = controller?.otaVersion ?: DeviceUtils.getOtaVersion()
            .takeIf { e -> e != "null" } ?: ""
        if (getOs != curOs && (dexOptimizeDialog == null || !dexOptimizeDialog!!.isShowing)) {
            dexOptimizeDialog =
                MaterialAlertDialogBuilder(requireActivity(), dialogCentered).apply {
                    setMessage(R.string.optimize_dex_after_system_update)
                    setCancelable(false)
                    setPositiveButton(android.R.string.ok) { _, _ ->
                        RestartMenuUtils.showOptimizeAllDexDialog(context, true)
                        requireActivity().putString(SettingsPrefs, "current_os_version", curOs)
                    }
                    setNeutralButton(R.string.ignore, null)
                }.show()
        }
    }

    override fun onResume() {
        super.onResume()
        initSystemInfoView()
        refreshModuleStatus()
    }

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menu.add(0, 1, 0, getString(R.string.menu_reboot)).apply {
            setIcon(R.drawable.ic_baseline_refresh_24)
            setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            if (ThemeUtils.isNightMode(resources.configuration)) {
                iconTintList = ColorStateList.valueOf(Color.WHITE)
            }
        }
        menu.add(0, 2, 0, getString(R.string.menu_settings)).apply {
            setIcon(R.drawable.ic_baseline_info_24)
            setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
            if (ThemeUtils.isNightMode(resources.configuration)) {
                iconTintList = ColorStateList.valueOf(Color.WHITE)
            }
        }
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        if (menuItem.itemId == 1) RestartMenuUtils.showMainRestartMenu(requireActivity())
        if (menuItem.itemId == 2) {
            MaterialAlertDialogBuilder(requireActivity()).apply {
                setTitle(getString(R.string.about_author))
                setView(MaterialTextView(context).apply {
                    var hideFunc = context.getBoolean(SettingsPrefs, "hidden_function", false)
                    setPadding(20.dp)
                    text = if (hideFunc) "忆清鸣、luckyzyx T" else "忆清鸣、luckyzyx"
                    setOnLongClickListener {
                        context.putBoolean(SettingsPrefs, "hidden_function", !hideFunc)
                        hideFunc = context.getBoolean(SettingsPrefs, "hidden_function", false)
                        text = if (hideFunc) "忆清鸣、luckyzyx T" else "忆清鸣、luckyzyx"
                        true
                    }
                })
                show()
            }
        }
        return true
    }

    @SuppressLint("SetTextI18n")
    fun refreshModuleStatus() {
        when {
            XposedServiceBridge.isModuleActive -> {
                binding.moduleStatusIcon.setImageResource(R.drawable.ic_round_check_24)
            }

            else -> {
                binding.statusCard.setCardBackgroundColor(Color.GRAY)
                binding.moduleStatusIcon.setImageResource(R.drawable.ic_round_warning_24)
            }
        }
        binding.moduleStatus.text = when {
            XposedServiceBridge.isModuleActive.not() -> getString(R.string.module_is_disabled)
            XposedServiceBridge.isModuleActive -> getString(R.string.module_isactivated)
            else -> getString(R.string.module_notactive)
        }

        binding.moduleVersion.apply {
            text = "${getString(R.string.module_version)} $getVersionName ($getVersionCode)" +
                    " ${BuildConfig.BUILD_TYPE.uppercase()}"
        }

        binding.rootVersion.apply {
            text = DeviceUtils.getRootVersion(context)
        }

        binding.frameworkVersion.apply {
            text = DeviceUtils.getFrameWorkVersion(context)
        }
    }
}