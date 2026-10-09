package com.luckyzyx.luckytool.hook.hookers

import android.annotation.SuppressLint
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.SurfaceControl
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import java.util.function.BiConsumer
import java.util.function.BiPredicate

/**
 * 由上游 io.github.lsposed.disableflagsecure.DisableFlagSecure（libxposed 链式 Hook API）迁移为 YukiHook 经典钩子。
 *
 * - [HookDisableFlagSecure]：system_server 侧，对应原 onSystemServerStarting 入口
 * - [HookDisableFlagSecureApp]：应用侧（systemui / appplatform / screenshot 等），对应原 onPackageReady 入口
 *
 * 开关语义与上游保持一致：上游 isEnabled() 返回 !disable_flag_secure 并据此早退，
 * 因此只有 ModulePrefs.disable_flag_secure 为 true（界面开关“禁用FLAG_SECURE”已打开）时才安装钩子，否则直接退出。
 */
@SuppressLint("PrivateApi", "BlockedPrivateApi")
@Obfuscate
object HookDisableFlagSecure : YukiBaseHooker() {

    override fun onHook() {
        //LuckyTool：上游 isEnabled() = !disable_flag_secure，为 true 时早退，故此处仅在开关打开时继续
        if (!preferences(ModulePrefs).getBoolean("disable_flag_secure", false)) return

        try {
            deoptimizeSystemServer()
        } catch (t: Throwable) {
            YLog.error("deoptimize system server failed", t)
        }

        hookSystemServer()
    }

    private fun hookSystemServer() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            // Screen record detection (V~Baklava)
            try {
                hookWindowManagerService()
            } catch (t: Throwable) {
                YLog.error("hook WindowManagerService failed", t)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Screenshot detection (U~Baklava)
            try {
                hookActivityTaskManagerService()
            } catch (t: Throwable) {
                YLog.error("hook ActivityTaskManagerService failed", t)
            }

            // Xiaomi HyperOS (U~Baklava)
            // OS2.0.300.1.WOCCNXM
            try {
                hookHyperOS()
            } catch (ignored: ClassNotFoundException) {
            } catch (t: Throwable) {
                YLog.error("hook HyperOS failed", t)
            }
        }

        // ScreenCapture in WindowManagerService (S~Baklava)
        try {
            hookScreenCapture()
        } catch (t: Throwable) {
            YLog.error("hook ScreenCapture failed", t)
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // Blackout permission check (S~T)
            try {
                hookActivityManagerService()
            } catch (t: Throwable) {
                YLog.error("hook ActivityManagerService failed", t)
            }
        }

        // WifiDisplay (S~Baklava) / OverlayDisplay (S~Baklava) / VirtualDisplay (U~Baklava)
        try {
            hookDisplayControl()
        } catch (t: Throwable) {
            YLog.error("hook DisplayControl failed", t)
        }

        // VirtualDisplay with MediaProjection (S~Baklava)
        try {
            hookVirtualDisplayAdapter()
        } catch (t: Throwable) {
            YLog.error("hook VirtualDisplayAdapter failed", t)
        }

        // OneUI
        try {
            hookScreenshotHardwareBuffer()
        } catch (t: Throwable) {
            if (t !is ClassNotFoundException) {
                YLog.error("hook ScreenshotHardwareBuffer failed", t)
            }
        }
        try {
            hookOneUI()
        } catch (t: Throwable) {
            if (t !is ClassNotFoundException) {
                YLog.error("hook OneUI failed", t)
            }
        }

        // secureLocked flag
        try {
            // Screenshot
            hookWindowState()
        } catch (t: Throwable) {
            YLog.error("hook WindowState failed", t)
        }

        // oplus dumpsys
        // dumpsys window screenshot systemQuickTileScreenshotOut display_id=0
        try {
            hookOplus()
        } catch (t: Throwable) {
            if (t !is ClassNotFoundException) {
                YLog.error("hook Oplus failed", t)
            }
        }
    }

    private fun deoptimizeSystemServer() {
        deoptimizeMethods(
            "com.android.server.wm.WindowStateAnimator".toClass(),
            "createSurfaceLocked"
        )

        deoptimizeMethods(
            "com.android.server.wm.WindowManagerService".toClass(),
            "relayoutWindow"
        )

        for (i in 0..19) {
            try {
                val clazz = hostClassLoader!!.loadClass(
                    "com.android.server.wm.RootWindowContainer\$\$ExternalSyntheticLambda$i"
                )
                if (BiConsumer::class.java.isAssignableFrom(clazz)) {
                    deoptimizeMethods(clazz, "accept")
                }
            } catch (ignored: ClassNotFoundException) {
            }
            try {
                val clazz = hostClassLoader!!.loadClass("com.android.server.wm.DisplayContent\$$i")
                if (BiPredicate::class.java.isAssignableFrom(clazz)) {
                    deoptimizeMethods(clazz, "test")
                }
            } catch (ignored: ClassNotFoundException) {
            }
        }
    }

    private fun deoptimizeMethods(clazz: Class<*>, vararg names: String) {
        clazz.declaredMethods.filter { it.name in names }.forEach { it.deoptimize() }
    }

    private fun hookWindowState() {
        val windowStateClazz = "com.android.server.wm.WindowState".toClass()
        val systemServerCl = windowStateClazz.classLoader
        val isSecureLockedMethod = windowStateClazz.getDeclaredMethod("isSecureLocked")
        isSecureLockedMethod.hook {
            before {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                    val match = walker.walk<Boolean> { frames ->
                        frames.anyMatch { frame ->
                            frame.declaringClass != null &&
                                frame.declaringClass.classLoader == systemServerCl &&
                                (frame.methodName == "setInitialSurfaceControlProperties" ||
                                    frame.methodName == "createSurfaceLocked")
                        }
                    }
                    if (match) return@before
                } else {
                    val stackTrace = Throwable().stackTrace
                    for (frame in stackTrace) {
                        val name = frame.methodName
                        try {
                            if ((name == "setInitialSurfaceControlProperties" ||
                                    name == "createSurfaceLocked") &&
                                hostClassLoader!!.loadClass(frame.className).classLoader == systemServerCl
                            ) {
                                return@before
                            }
                        } catch (ignored: ClassNotFoundException) {
                        }
                    }
                }
                result = false
            }
        }
    }

    private fun hookScreenCapture() {
        val screenCaptureClazz: Class<*>
        val captureArgsClazz: Class<*>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA &&
            Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1
        ) {
            screenCaptureClazz = "android.window.ScreenCaptureInternal".toClass()
            captureArgsClazz = "android.window.ScreenCaptureInternal\$CaptureArgs".toClass()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            screenCaptureClazz = "android.window.ScreenCapture".toClass()
            captureArgsClazz = "android.window.ScreenCapture\$CaptureArgs".toClass()
        } else {
            screenCaptureClazz = SurfaceControl::class.java
            captureArgsClazz = "android.view.SurfaceControl\$CaptureArgs".toClass()
        }
        val captureSecureLayersField = captureArgsClazz.getDeclaredField(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA &&
                Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1
            ) "mSecureContentPolicy" else "mCaptureSecureLayers"
        )
        captureSecureLayersField.isAccessible = true

        screenCaptureClazz.declaredMethods
            .filter { it.name == "nativeCaptureDisplay" || it.name == "nativeCaptureLayers" }
            .forEach { method ->
                method.hook {
                    before {
                        val captureArgs = args[0]
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA &&
                                Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1
                            ) {
                                captureSecureLayersField.set(captureArgs, 1)
                            } else {
                                captureSecureLayersField.set(captureArgs, true)
                            }
                        } catch (t: IllegalAccessException) {
                            YLog.error("ScreenCaptureHooker failed", t)
                        }
                    }
                }
            }
    }

    private fun hookDisplayControl() {
        val displayControlClazz = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            "com.android.server.display.DisplayControl".toClass()
        } else {
            SurfaceControl::class.java
        }
        val systemServerCl = displayControlClazz.classLoader
        val method = displayControlClazz.getDeclaredMethod(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                "createVirtualDisplay"
            } else {
                "createDisplay"
            },
            String::class.java, Boolean::class.javaPrimitiveType!!
        )
        method.hook {
            before {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val stackTrace = Throwable().stackTrace
                    for (frame in stackTrace) {
                        val name = frame.methodName
                        try {
                            if (name == "createVirtualDisplayLocked" &&
                                hostClassLoader!!.loadClass(frame.className).classLoader == systemServerCl
                            ) {
                                return@before
                            }
                        } catch (ignored: ClassNotFoundException) {
                        }
                    }
                }
                args[1] = true
            }
        }
    }

    private fun hookVirtualDisplayAdapter() {
        "com.android.server.display.VirtualDisplayAdapter".toClass().declaredMethods
            .filter { it.name == "createVirtualDisplayLocked" }
            .forEach { method ->
                method.hook {
                    before {
                        val caller = args[2] as Int
                        if (caller >= 10000 && args[1] == null) {
                            // not os and not media projection
                            return@before
                        }
                        for (i in 3 until args.size) {
                            val arg = args[i]
                            if (arg is Int) {
                                args[i] = arg or DisplayManager.VIRTUAL_DISPLAY_FLAG_SECURE
                                return@before
                            }
                        }
                        YLog.warn("flag not found in CreateVirtualDisplayLockedHooker")
                    }
                }
            }
    }

    private fun hookActivityTaskManagerService() {
        val activityTaskManagerServiceClazz = "com.android.server.wm.ActivityTaskManagerService".toClass()
        val iBinderClazz = "android.os.IBinder".toClass()
        val iScreenCaptureObserverClazz = "android.app.IScreenCaptureObserver".toClass()
        val method = activityTaskManagerServiceClazz.getDeclaredMethod(
            "registerScreenCaptureObserver", iBinderClazz, iScreenCaptureObserverClazz
        )
        method.hook {
            //经典 before 钩子无法可靠跳过 void 方法的原始调用，改用 intercept
            intercept {
                //registerScreenCaptureObserver 无返回值，恒返回 null 即完全跳过原始调用
                null
            }
        }
    }

    private fun hookWindowManagerService() {
        val windowManagerServiceClazz = "com.android.server.wm.WindowManagerService".toClass()
        val iScreenRecordingCallbackClazz = "android.window.IScreenRecordingCallback".toClass()
        val method = windowManagerServiceClazz.getDeclaredMethod(
            "registerScreenRecordingCallback", iScreenRecordingCallbackClazz
        )
        method.hook {
            //经典 before 钩子无法可靠跳过 void 方法的原始调用，改用 intercept
            intercept {
                //与上游一致：不注册屏幕录制回调（方法无返回值时该返回值会被忽略）
                false
            }
        }
    }

    private fun hookActivityManagerService() {
        val activityManagerServiceClazz = "com.android.server.am.ActivityManagerService".toClass()
        val method = activityManagerServiceClazz.getDeclaredMethod(
            "checkPermission", String::class.java,
            Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!
        )
        method.hook {
            before {
                if ("android.permission.CAPTURE_BLACKOUT_CONTENT" == args[0]) {
                    args[0] = "android.permission.READ_FRAME_BUFFER"
                }
            }
        }
    }

    private fun hookHyperOS() {
        "com.android.server.wm.WindowManagerServiceImpl".toClass().declaredMethods
            .filter { it.name == "notAllowCaptureDisplay" }
            .forEach { method -> method.hook { before { result = false } } }
    }

    private fun hookScreenshotHardwareBuffer() {
        val screenshotHardwareBufferClazz = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            "android.window.ScreenCapture\$ScreenshotHardwareBuffer"
        } else {
            "android.view.SurfaceControl\$ScreenshotHardwareBuffer"
        }).toClass()
        val method = screenshotHardwareBufferClazz.getDeclaredMethod("containsSecureLayers")
        method.hook {
            before { result = false }
        }
    }

    private fun hookOplus() {
        // caller: com.android.server.wm.OplusLongshotWindowDump#dumpWindows
        "com.android.server.wm.OplusLongshotMainWindow".toClass().declaredMethods
            .filter { it.name == "hasSecure" }
            .forEach { method -> method.hook { before { result = false } } }
    }

    private fun hookOneUI() {
        "com.android.server.wm.WmScreenshotController".toClass().declaredMethods
            .filter { it.name == "canBeScreenshotTarget" }
            .forEach { method -> method.hook { before { result = true } } }
    }
}

@SuppressLint("PrivateApi", "BlockedPrivateApi")
@Obfuscate
object HookDisableFlagSecureApp : YukiBaseHooker() {

    private const val SYSTEMUI = "com.android.systemui"
    private const val OPLUS_APPPLATFORM = "com.oplus.appplatform"
    private const val OPLUS_SCREENSHOT = "com.oplus.screenshot"
    private const val FLYME_SYSTEMUIEX = "com.flyme.systemuiex"
    private const val MIUI_SCREENSHOT = "com.miui.screenshot"

    override fun onHook() {
        //LuckyTool：上游 isEnabled() = !disable_flag_secure，为 true 时早退，故此处仅在开关打开时继续
        if (!preferences(ModulePrefs).getBoolean("disable_flag_secure", false)) return
        if (!isFirstApplication) return

        hookPackage()
    }

    private fun hookPackage() {
        //注意：上游 Java 的 switch 各 case 之间没有 break（顺序贯穿），下面按等价分组还原
        val screenshotHardwareBufferPackages =
            setOf(OPLUS_SCREENSHOT, FLYME_SYSTEMUIEX, OPLUS_APPPLATFORM)
        val screenCapturePackages =
            setOf(OPLUS_SCREENSHOT, FLYME_SYSTEMUIEX, OPLUS_APPPLATFORM, SYSTEMUI, MIUI_SCREENSHOT)

        if (packageName == OPLUS_SCREENSHOT) {
            // Oplus Screenshot 15.0.0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                try {
                    hookOplusScreenCapture()
                } catch (t: Throwable) {
                    if (t !is ClassNotFoundException) {
                        YLog.error("hook OplusScreenCapture failed", t)
                    }
                }
            }
        }

        if (packageName in screenshotHardwareBufferPackages) {
            // Flyme SystemUI Ext 10.3.0
            // OPlus AppPlatform 13.1.0 / 14.0.0
            try {
                hookScreenshotHardwareBuffer()
            } catch (t: Throwable) {
                if (t !is ClassNotFoundException) {
                    YLog.error("hook ScreenshotHardwareBuffer failed", t)
                }
            }
        }

        if (packageName in screenCapturePackages) {
            if (packageName == OPLUS_APPPLATFORM || packageName == OPLUS_SCREENSHOT ||
                Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            ) {
                // ScreenCapture in App (S~T) (OPlus S~V)
                // TODO: test Oplus Baklava
                try {
                    hookScreenCapture()
                } catch (t: Throwable) {
                    YLog.error("hook ScreenCapture failed", t)
                }
            }
        }
    }

    private fun hookOplusScreenCapture() {
        val oplusScreenCaptureClazz = "com.oplus.screenshot.OplusScreenCapture\$CaptureArgs\$Builder".toClass()
        val method = oplusScreenCaptureClazz.getDeclaredMethod("setUid", Long::class.javaPrimitiveType!!)
        method.hook {
            before {
                args[0] = -1
            }
        }
    }

    private fun hookScreenshotHardwareBuffer() {
        val screenshotHardwareBufferClazz = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            "android.window.ScreenCapture\$ScreenshotHardwareBuffer"
        } else {
            "android.view.SurfaceControl\$ScreenshotHardwareBuffer"
        }).toClass()
        val method = screenshotHardwareBufferClazz.getDeclaredMethod("containsSecureLayers")
        method.hook {
            before { result = false }
        }
    }

    private fun hookScreenCapture() {
        val screenCaptureClazz: Class<*>
        val captureArgsClazz: Class<*>
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA &&
            Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1
        ) {
            screenCaptureClazz = "android.window.ScreenCaptureInternal".toClass()
            captureArgsClazz = "android.window.ScreenCaptureInternal\$CaptureArgs".toClass()
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            screenCaptureClazz = "android.window.ScreenCapture".toClass()
            captureArgsClazz = "android.window.ScreenCapture\$CaptureArgs".toClass()
        } else {
            screenCaptureClazz = SurfaceControl::class.java
            captureArgsClazz = "android.view.SurfaceControl\$CaptureArgs".toClass()
        }
        val captureSecureLayersField = captureArgsClazz.getDeclaredField(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA &&
                Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1
            ) "mSecureContentPolicy" else "mCaptureSecureLayers"
        )
        captureSecureLayersField.isAccessible = true

        screenCaptureClazz.declaredMethods
            .filter { it.name == "nativeCaptureDisplay" || it.name == "nativeCaptureLayers" }
            .forEach { method ->
                method.hook {
                    before {
                        val captureArgs = args[0]
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA &&
                                Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1
                            ) {
                                captureSecureLayersField.set(captureArgs, 1)
                            } else {
                                captureSecureLayersField.set(captureArgs, true)
                            }
                        } catch (t: IllegalAccessException) {
                            YLog.error("ScreenCaptureHooker failed", t)
                        }
                    }
                }
            }
    }
}
