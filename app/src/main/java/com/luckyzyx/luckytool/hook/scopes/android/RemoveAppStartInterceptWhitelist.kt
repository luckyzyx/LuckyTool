package com.luckyzyx.luckytool.hook.scopes.android

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.util.Xml
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate
import org.xmlpull.v1.XmlPullParser

@Obfuscate
object RemoveAppStartInterceptWhitelist : YukiBaseHooker() {
    private const val PREF_KEY = "remove_app_start_intercept_whitelist"
    private const val SECURITY_PERMISSION_PACKAGE = "com.oplus.securitypermission"
    private const val PRESET_SRC_PACKAGE = "src_pkg"
    private const val PRESET_DST_PACKAGE = "dst_pkg"
    private const val PRESET_ACTION = "action"
    private const val PRESET_ACTIVITY = "activity"
    private const val PER_USER_RANGE = 100000
    private const val SECURITY_PERMISSION_ASSET = "permission/activity_start_whitelist.xml"

    private val keptPackages = setOf("com.tencent.mobileqq", "com.tencent.mm")

    @Volatile
    private var isEnable = false

    @Volatile
    private var activityStartController: Any? = null

    @Volatile
    private var assetPresetList: Map<String, Set<String>> = emptyMap()

    override fun onHook() {
        isEnable = preferences(ModulePrefs).getBoolean(PREF_KEY, false)
        dataChannel.wait<Boolean>(PREF_KEY) { isEnable = it }

        //Source ActivityStartList
        "com.android.server.am.OplusActivityStartController\$ActivityStartList".toClass()
            .resolve().apply {
                firstMethod {
                    name = "isInPresetList"
                    parameters(String::class, String::class)
                    returnType = Boolean::class
                }.hook {
                    after {
                        if (!isEnable || result<Boolean>() != true) return@after

                        val value = firstArg().get<String>()
                        val type = arg(1).get<String>()
                        if (value != null && type != null && shouldRemovePreset(value, type)) result = false
                    }
                }
            }

        //Source OplusSecurityPermissionManager
        "com.android.server.am.OplusSecurityPermissionManager".toClass().resolve().apply {
            firstMethod { name = "init" }.hook {
                after {
                    val context = firstField { type = Context::class }.of(instance).get<Context>()
                    activityStartController = firstField {
                        type = "com.android.server.am.OplusActivityStartController"
                    }.of(instance).get()
                    context?.let(::loadAssetPresetList)
                }
            }
        }

        //Source OplusAppStartConfirmManager
        "com.android.server.wm.OplusAppStartConfirmManager".toClass().resolve().apply {
            firstMethod {
                name = "isSystemAppOrSameApp"
                parameters(
                    Int::class,
                    String::class,
                    ActivityInfo::class,
                    Intent::class,
                )
                returnType = Boolean::class
            }.intercept {
                if (!isEnable) return@intercept proceed()

                val originalResult = proceed() as? Boolean ?: return@intercept false
                if (!originalResult) return@intercept false

                val callerUid = firstArg<Int>() ?: return@intercept true
                val callerPackage = arg<String>(1) ?: return@intercept true
                val activityInfo = arg<ActivityInfo>(2) ?: return@intercept true
                val intent = arg<Intent>(3) ?: return@intercept true
                val targetApplicationInfo = activityInfo.applicationInfo ?: return@intercept true
                val targetPackage = targetApplicationInfo.packageName
                    ?: return@intercept true

                if (callerUid % PER_USER_RANGE < 10000 ||
                    targetApplicationInfo.uid % PER_USER_RANGE < 10000 ||
                    callerPackage == targetPackage ||
                    callerPackage == SECURITY_PERMISSION_PACKAGE ||
                    callerPackage in keptPackages ||
                    targetPackage in keptPackages
                ) {
                    return@intercept true
                }

                !isRemovedPreset(callerPackage, targetPackage, intent)
            }
        }
    }

    private fun shouldRemovePreset(value: String, type: String): Boolean = when (type) {
        PRESET_SRC_PACKAGE, PRESET_DST_PACKAGE -> value !in keptPackages
        PRESET_ACTION, PRESET_ACTIVITY -> true
        else -> false
    }

    private fun isRemovedPreset(callerPackage: String, targetPackage: String, intent: Intent): Boolean {
        if (callerPackage in keptPackages || targetPackage in keptPackages) return false
        val cachedPresetList = readCachedPresetList()
        val presetList = cachedPresetList.ifEmpty { assetPresetList }
        val component = intent.component
        val activity = component?.let { "${it.packageName}/${it.className}" }
        val action = intent.action

        return (presetMatches(presetList, PRESET_SRC_PACKAGE, callerPackage) &&
            shouldRemovePreset(callerPackage, PRESET_SRC_PACKAGE)) ||
            (presetMatches(presetList, PRESET_DST_PACKAGE, targetPackage) &&
                shouldRemovePreset(targetPackage, PRESET_DST_PACKAGE)) ||
            (presetMatches(presetList, PRESET_ACTION, action) &&
                shouldRemovePreset(action.orEmpty(), PRESET_ACTION)) ||
            (presetMatches(presetList, PRESET_ACTIVITY, activity) &&
                shouldRemovePreset(activity.orEmpty(), PRESET_ACTIVITY))
    }

    private fun readCachedPresetList(): Map<String, Set<String>> {
        val controller = activityStartController ?: return emptyMap()
        return runCatching {
            val resolver = controller.asResolver()
            val lock = resolver.firstField { name = "mActivityStartLock" }.get<Any>()
                ?: return emptyMap()
            synchronized(lock) {
                val list = resolver.firstField { name = "mCachedActivityStartList" }.get()
                    ?: return@synchronized emptyMap()
                val rawPresetList = list.asResolver().firstField { name = "mPresetList" }.get<Any>()
                val presetList = rawPresetList as? Map<*, *> ?: return@synchronized emptyMap()
                presetList.entries.mapNotNull { (key, value) ->
                    val keyString = key as? String ?: return@mapNotNull null
                    val values = (value as? Collection<*>)
                        ?.mapNotNull { it as? String }
                        ?.toSet()
                        ?: return@mapNotNull null
                    keyString to values
                }.toMap()
            }
        }.onFailure {
            YLog.error("RemoveAppStartInterceptWhitelist runtime preset read failed", e = it)
        }.getOrDefault(emptyMap())
    }

    private fun loadAssetPresetList(context: Context) {
        runCatching {
            val parsed = context.createPackageContext(
                SECURITY_PERMISSION_PACKAGE,
                Context.CONTEXT_IGNORE_SECURITY,
            ).assets.open(SECURITY_PERMISSION_ASSET).use { input ->
                val parser = Xml.newPullParser()
                parser.setInput(input, "UTF-8")
                val values = linkedMapOf<String, MutableSet<String>>()
                var event = parser.eventType
                while (event != XmlPullParser.END_DOCUMENT) {
                    if (event == XmlPullParser.START_TAG && parser.name == "item") {
                        val type = parser.getAttributeValue(null, "type")
                        val value = parser.getAttributeValue(null, "value")
                        if (!type.isNullOrEmpty() && !value.isNullOrEmpty()) {
                            values.getOrPut(type) { linkedSetOf() }.add(value)
                        }
                    }
                    event = parser.next()
                }
                values.mapValues { it.value.toSet() }
            }
            assetPresetList = parsed
        }.onFailure {
            assetPresetList = emptyMap()
            YLog.error("RemoveAppStartInterceptWhitelist asset preset load failed", e = it)
        }
    }

    private fun presetMatches(presetList: Map<String, Set<String>>, type: String, value: String?): Boolean {
        if (value.isNullOrEmpty()) return false
        return presetList[type]?.contains(value) == true
    }
}
