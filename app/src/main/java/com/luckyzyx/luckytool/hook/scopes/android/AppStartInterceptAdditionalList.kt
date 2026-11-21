package com.luckyzyx.luckytool.hook.scopes.android

import android.content.Intent
import android.content.pm.ActivityInfo
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object AppStartInterceptAdditionalList : YukiBaseHooker() {
    private const val TAG = "AppStartInterceptAdditionalList"
    private const val PREF_KEY = "app_start_intercept_additional_list"
    private const val INTENT_FLAG_ALLOW_START = 0x200000
    private const val PER_USER_RANGE = 100000
    private const val FIRST_APPLICATION_UID = 10000
    private const val TYPE_ALLOW = -1
    private const val TYPE_BLACK_LIST = 7

    @Volatile
    private var packages: Set<String> = emptySet()

    override fun onHook() {
        packages = preferences(ModulePrefs).getStringSet(PREF_KEY, emptySet()).toSet()
        dataChannel.wait<Set<String>>(PREF_KEY) { packages = it.toSet() }

        runCatching {
            //Source ActivityStartList
            "com.android.server.am.OplusActivityStartController\$ActivityStartList"
                .toClassOrNull()?.resolve()?.apply {
                    firstMethodOrNull {
                        name = "checkBlackAndWhiteList"
                        parameters(String::class, String::class, Intent::class, Int::class)
                        returnType = Int::class
                    }?.hook {
                        after {
                            if (result<Int>() != TYPE_ALLOW) return@after
                            val caller = firstArg().get<String>() ?: return@after
                            val target = arg(1).get<String>() ?: return@after
                            if (caller == target) return@after
                            if (caller in packages || target in packages) result = TYPE_BLACK_LIST
                        }
                    } ?: YLog.warn("$TAG: checkBlackAndWhiteList not found")
                }
        }.onFailure {
            YLog.error("$TAG: list hook failed", e = it)
        }

        runCatching {
            //Source OplusAppStartConfirmManager
            "com.android.server.wm.OplusAppStartConfirmManager".toClassOrNull()?.resolve()?.apply {
                firstMethodOrNull {
                    name = "isSystemAppOrSameApp"
                    parameters(Int::class, String::class, ActivityInfo::class, Intent::class)
                    returnType = Boolean::class
                }?.hook {
                    after {
                        if (result<Boolean>() != true || packages.isEmpty()) return@after
                        val callerUid = firstArg().get<Int>() ?: return@after
                        val caller = arg(1).get<String>() ?: return@after
                        val info = arg(2).get<ActivityInfo>() ?: return@after
                        val targetApp = info.applicationInfo ?: return@after
                        if (callerUid % PER_USER_RANGE < FIRST_APPLICATION_UID ||
                            targetApp.uid % PER_USER_RANGE < FIRST_APPLICATION_UID
                        ) return@after
                        val target = targetApp.packageName ?: return@after
                        val intent = arg(3).get<Intent>() ?: return@after
                        if (caller == target || hasIntentAllowFlag(intent)) return@after
                        if (caller in packages || target in packages) result = false
                    }
                } ?: YLog.warn("$TAG: isSystemAppOrSameApp not found")
            }
        }.onFailure {
            YLog.error("$TAG: confirm hook failed", e = it)
        }
    }

    private fun hasIntentAllowFlag(intent: Intent): Boolean = runCatching {
        val intentExt = intent.asResolver().firstMethod { name = "getIntentExt"; superclass() }
            .invoke<Any>() ?: return false
        val flags = intentExt.asResolver().firstMethod { name = "getOplusFlags" }.invoke<Int>() ?: 0
        flags and INTENT_FLAG_ALLOW_START != 0
    }.getOrDefault(false)
}
