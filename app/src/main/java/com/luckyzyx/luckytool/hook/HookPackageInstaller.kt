package com.luckyzyx.luckytool.hook

import android.util.ArraySet
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.apps.packageinstaller.AllowReplaceInstall
import com.luckyzyx.luckytool.hook.apps.packageinstaller.RemoveInstallAds
import com.luckyzyx.luckytool.hook.apps.packageinstaller.ReplaseAospInstaller
import com.luckyzyx.luckytool.hook.apps.packageinstaller.SkipApkScan
import com.luckyzyx.luckytool.utils.data.XposedPrefs
import java.util.*

class HookPackageInstaller : YukiBaseHooker() {
    override fun onHook() {
        loadApp("com.android.packageinstaller") {
            val appSet = prefs(XposedPrefs).getStringSet(packageName, ArraySet()).toTypedArray().apply {
                    Arrays.sort(this)
                    forEach {
                        this[this.indexOf(it)] = it.substring(2)
                    }
                }
            //非ColorOS官方安装器直接返回
            if (appSet[2] == "null") return
            //跳过安装扫描
            if (prefs(XposedPrefs).getBoolean("skip_apk_scan", false)) loadHooker(SkipApkScan())

            //低/相同版本警告
            if (prefs(XposedPrefs).getBoolean("allow_downgrade_install", false)) loadHooker(
                AllowReplaceInstall()
            )

            //移除安装完成广告
            if (prefs(XposedPrefs).getBoolean("remove_install_ads", false)) loadHooker(
                RemoveInstallAds()
            )

            //ColorOS安装器替换为原生安装器
            if (prefs(XposedPrefs).getBoolean("replase_aosp_installer", false)) loadHooker(
                ReplaseAospInstaller()
            )
        }
    }
}