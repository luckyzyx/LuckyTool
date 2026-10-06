package org.lsposed.corepatch

import android.os.Build
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModuleInterface
import org.lsposed.corepatch.Config.printAllConfig
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

class XposedMain {

    private val hooks = listOf(
        ApkSignatureVerifierHook,
        ApkSigningBlockUtilsHook,
        ApplicationInfoHook,
        AssetManagerHook,
        InstallPackageHelperHook,
        KeySetManagerServiceHook,
        MessageDigestHook,
        NtConfigListServiceImplHook,
        PackageManagerServiceHook,
        PackageManagerServiceUtilsHook,
        ReconcilePackageUtilsHook,
        ScanPackageUtilsHook,
        SharedUserSettingHook,
        SigningDetailsHook,
        StrictJarVerifierHook,
        VerificationParamsHook,
        VerifyingSessionHook,
    )

    fun onModuleLoaded(base: XposedInterface) {
        XposedHelper.setXposedModule(base)
    }

    fun onSystemServerStarting(param: XposedModuleInterface.SystemServerStartingParam) {
        XposedHelper.log("onSystemServerStarting: Current sdk version is ${Build.VERSION.SDK_INT}")
        XposedHelper.setHostClassLoader(param.classLoader)
        printAllConfig()
        hooks.forEach { it.init() }
    }

    /**
     * 热重载前（旧代码执行）：返回需要跨代保存的宿主 classLoader（宿主对象，跨代安全）。
     * libxposed 的 saved 槽位只有一个，多组件状态由入口合并写入，这里不能直接
     * setSavedInstanceState，否则会覆盖其他组件的状态。
     */
    fun onHotReloading(): Any? =
        if (XposedHelper.isHostClassLoaderInitialized) XposedHelper.hostClassLoader else null

    /**
     * 热重载后（新代码执行）：先摘旧钩、再重挂。
     * CorePatch 的 hook 没有设 ID，无法用 replaceHook 原子替换，只能先摘后挂。
     */
    fun onHotReloaded(param: XposedModuleInterface.HotReloadedParam, savedState: Any?) {
        if (!param.isSystemServer) return
        val hostClassLoader = savedState as? ClassLoader ?: return
        param.oldHookHandles.forEach { handle -> runCatching { handle.unhook() } }
        XposedHelper.setHostClassLoader(hostClassLoader)
        XposedHelper.log("onHotReloaded: reinstalling corepatch hooks")
        hooks.forEach { runCatching { it.init() } }
    }
}