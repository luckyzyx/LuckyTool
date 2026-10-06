package com.luckyzyx.luckytool.hook.scopes.android

import android.content.Intent
import android.content.pm.ParceledListSlice
import android.content.pm.ResolveInfo
import android.util.ArraySet
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.core.YukiHookCreator
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.data.AppIntentInfo
import com.luckyzyx.luckytool.enums.IntentType
import com.luckyzyx.luckytool.utils.IntentPrefs
import com.luckyzyx.luckytool.utils.IntentUtils.Companion.getIntentFilter
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.safeOfNull
import kotlinx.serialization.json.Json
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
class HookIPackageManager : YukiBaseHooker() {

    val allIntent = ArraySet<AppIntentInfo>()
    private val allEnabledApps = ArraySet<String>()

    var isEnable = preferences(IntentPrefs).getBoolean("custom_config_app_intent_list", false)

    val types = arrayOf(
        IntentType.SINGLE_SHARE, IntentType.MULTI_SHARE, IntentType.PROCESS_TEXT,
        IntentType.CONTENT, IntentType.FILE,
        IntentType.HTTP_LINK, IntentType.HTTPS_LINK
    )

    override fun onHook() {
        val osCode = getOSVersionCode

        initData()
        initDataChannel()

        if (osCode >= 26) loadHooker(HookQueryIntentActivitie())
        else loadHooker(HookQueryIntentActivitieV12())
    }

    private fun initData() {
        allIntent.clear()
        allEnabledApps.clear()

        allEnabledApps.addAll(preferences(IntentPrefs).getStringSet("enable_app_hide_list", ArraySet()))
        allEnabledApps.forEachIndexed { _, packName ->
            preferences(IntentPrefs).getStringSet(packName, ArraySet()).forEachIndexed { _, js ->
                val info = safeOfNull { Json.decodeFromString<AppIntentInfo>(js) }
                    ?: return@forEachIndexed
                allIntent.add(info)
            }
        }
        YLog.debug("init app intent configs success -> ${allEnabledApps.size}")
    }

    private fun initDataChannel() {
        dataChannel.wait<Boolean>("custom_config_app_intent_list") {
            isEnable = it
            YLog.debug("update custom app intent configs status -> $it")
        }
        dataChannel.wait<String>("custom_config_app_intent_list_update_app_config") { its ->
            val old = allIntent.filter { it.packName == its }
            val new = preferences(IntentPrefs).getStringSet(its, ArraySet())

            allIntent.removeIf { it.packName == its }
            new.forEachIndexed { _, js ->
                val info = safeOfNull { Json.decodeFromString<AppIntentInfo>(js) }
                    ?: return@forEachIndexed
                allIntent.add(info)
            }
            YLog.debug("update $its configs -> ${old.size} | ${new.size}")
        }
        dataChannel.wait<Pair<String, Boolean>>("custom_config_app_intent_list_update_apps") {
            if (it.second) allEnabledApps.add(it.first) else allEnabledApps.remove(it.first)
            YLog.debug("update app intent enabled list -> ${it.first} | ${it.second}")
        }
    }

    fun YukiHookCreator.ClassicMemberHooker.hookAfter() {
        after {
            if (!isEnable) return@after
            val intent = firstArg().get<Intent>() ?: return@after
            val action = intent.action ?: return@after
//            val data = if (action == Intent.ACTION_VIEW)

            val isOrigin = intent.getBooleanExtra("result_origin_data", false)
            if (isOrigin) return@after

            val res = result<ParceledListSlice<ResolveInfo>>() ?: return@after
            val list = res.list ?: return@after
            types.forEachIndexed { _, intentType ->
                val filte = getIntentFilter(intentType)
                val intents = allIntent.filter(filte)
                list.removeIf {
                    val packName = it.activityInfo.packageName
                    val activity = it.activityInfo.name
                    intents.find { info ->
                        info.action == action && info.packName == packName && info.activity == activity
                    } != null
                }
            }
        }
    }

    @Obfuscate
    inner class HookQueryIntentActivitieV12 : YukiBaseHooker() {
        override fun onHook() {
            //Source PackageManagerService
            "com.android.server.pm.PackageManagerService".toClass().resolve().apply {
                firstMethod {
                    name = "queryIntentActivities"
                    parameters(Intent::class, String::class, Int::class, Int::class)
                }.hook {
                    hookAfter()
                }
            }
        }
    }

    @Obfuscate
    inner class HookQueryIntentActivitie : YukiBaseHooker() {
        override fun onHook() {
            //Source IPackageManagerBase
            "com.android.server.pm.IPackageManagerBase".toClass().resolve().apply {
                firstMethod {
                    name = "queryIntentActivities"
                    parameters(Intent::class, String::class, Long::class, Int::class)
                }.hook {
                    hookAfter()
                }
            }
        }
    }
}