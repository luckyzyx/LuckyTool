package com.luckyzyx.luckytool.hook.hookers

import android.os.Build
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.corepatch.Config
import org.lsposed.corepatch.hook.ApkSignatureVerifierHook
import org.lsposed.corepatch.hook.ApkSigningBlockUtilsHook
import org.lsposed.corepatch.hook.ApplicationInfoHook
import org.lsposed.corepatch.hook.AssetManagerHook
import org.lsposed.corepatch.hook.InstallPackageHelperHook
import org.lsposed.corepatch.hook.KeySetManagerServiceHook
import org.lsposed.corepatch.hook.MessageDigestHook
import org.lsposed.corepatch.hook.NtConfigListServiceImplHook
import org.lsposed.corepatch.hook.PackageManagerServiceHook
import org.lsposed.corepatch.hook.PackageManagerServiceUtilsHook
import org.lsposed.corepatch.hook.ReconcilePackageUtilsHook
import org.lsposed.corepatch.hook.ScanPackageUtilsHook
import org.lsposed.corepatch.hook.SharedUserSettingHook
import org.lsposed.corepatch.hook.SigningDetailsHook
import org.lsposed.corepatch.hook.StrictJarVerifierHook
import org.lsposed.corepatch.hook.VerificationParamsHook
import org.lsposed.corepatch.hook.VerifyingSessionHook
import org.lsposed.lsparanoid.Obfuscate

/**
 * 上游 CorePatch 的装载入口。
 *
 * 由 MainHook 在 system_server 进程 loadSystem，各子 hook 自行按配置开关决定是否生效。
 */
@Obfuscate
object HookCorePatch : YukiBaseHooker() {

    override fun onHook() {
        YLog.debug("onSystemServerStarting: Current sdk version is ${Build.VERSION.SDK_INT}")
        Config.bind(preferences(ModulePrefs))
        Config.printAllConfig()

        loadHooker(ApkSignatureVerifierHook)
        loadHooker(ApkSigningBlockUtilsHook)
        loadHooker(ApplicationInfoHook)
        loadHooker(AssetManagerHook)
        loadHooker(InstallPackageHelperHook)
        loadHooker(KeySetManagerServiceHook)
        loadHooker(MessageDigestHook)
        loadHooker(NtConfigListServiceImplHook)
        loadHooker(PackageManagerServiceHook)
        loadHooker(PackageManagerServiceUtilsHook)
        loadHooker(ReconcilePackageUtilsHook)
        loadHooker(ScanPackageUtilsHook)
        loadHooker(SharedUserSettingHook)
        loadHooker(SigningDetailsHook)
        loadHooker(StrictJarVerifierHook)
        loadHooker(VerificationParamsHook)
        loadHooker(VerifyingSessionHook)
    }
}
