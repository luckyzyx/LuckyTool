package com.luckyzyx.luckytool.hook.scopes.systemui

import android.os.Handler
import android.os.Looper
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import io.github.libxposed.api.XposedInterface
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object LockScreenCredentialShortcut : YukiBaseHooker() {
    private const val KEY = "lock_screen_credential_shortcut"
    internal var framework: XposedInterface? = null
    internal var hasNativeHooks = false

    override fun onHook() {
        if (getOSVersionCode < 37) return
        if (framework == null) return
        var enabled = preferences(ModulePrefs).getBoolean(KEY, false)
        if (!enabled) return
        val entry = CredentialEntryController()
        val shortcut = NativeCredentialShortcut(entry) { enabled }
        loadHooker(CredentialEntryController.Hooks(entry))
        loadHooker(shortcut)
        loadHooker(NotificationOnlyLockscreenSwipe(entry, shortcut) { enabled })

        dataChannel.wait<Boolean>(KEY) {
            val value = it
            Handler(Looper.getMainLooper()).post {
                enabled = value
                shortcut.onPreferenceChanged()
            }
        }
    }
}
