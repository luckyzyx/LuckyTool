package com.luckyzyx.luckytool.hook.scopes.launcher

import android.graphics.drawable.Drawable
import android.os.UserHandle
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.oplus.multiapp.OplusMultiAppManager
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class HookAppBadge(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode
        if (osCode >= 30) loadHooker(AppBadge(dexKitBridge))
        else loadHooker(AppBadgeC13)
    }

    @Obfuscate
    class AppBadge(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
        override fun onHook() {
            val osCode = getOSVersionCode
            val isShortcut = preferences(ModulePrefs).getBoolean("remove_app_shortcut_badge", false)
            val isWork = preferences(ModulePrefs).getBoolean("remove_app_work_badge", false)
            val isClone = preferences(ModulePrefs).getBoolean("remove_app_clone_badge", false)

            // 直接调用框架的 OplusMultiAppManager 分身判定接口

            // 宿主给桌面图标补徽标的统一入口是 BitmapInfo.applyFlags：
            // 它按 this.flags 的位决定画工作/分身徽标。分身用户在图标 flags
            // 重算时可能被误判成工作资料，从而带上工作位画出工作徽标。
            // 这里用经典 before 模式而不是拦截器，是因为要改写原方法的 int 参数：
            // 给创建 flags 置 bit2 是宿主自身的跳过徽标位(OplusBubbleTextView.mHideBadge 同款)，
            // 同时回写 this.flags，让宿主后续(如按压动画)按修正后的位处理，而不是跳过整个原方法。
            "com.android.launcher3.icons.BitmapInfo".toClass().resolve().apply {
                val flagsField = firstField { name = "flags" }
                val userField = firstField { name = "mUserHandle" }
                val badgeInfoField = firstField { name = "badgeInfo" }
                firstMethod { name = "applyFlags"; parameterCount = 3 }.hook {
                    before {
                        val creationFlags = arg(2).get<Int>() ?: return@before
                        // 宿主已要求隐藏徽标(bit2)，无需再处理
                        if ((creationFlags and 2) != 0) return@before
                        val userHandle = userField.copy().of(instance).get<UserHandle>()
                        val userId = userHandle?.hashCode() ?: -1
                        val isCloneUser = OplusMultiAppManager.getInstance().isMultiAppUserId(userId)
                        val needClearBadge = when {
                            isClone && isCloneUser -> true
                            isWork && userId != 0 && !isCloneUser -> true
                            isShortcut && badgeInfoField.copy().of(instance).get() != null -> true
                            else -> false
                        }
                        if (!needClearBadge) return@before
                        val rawFlag = flagsField.copy().of(instance).get<Int>() ?: 0
                        val correctedFlag = when {
                            isClone && isCloneUser -> (rawFlag and 1.inv()) or 4
                            isWork -> rawFlag and 1.inv()
                            else -> rawFlag
                        }
                        if (correctedFlag != rawFlag) {
                            flagsField.copy().of(instance).set(correctedFlag)
                        }
                        arg(2).set(creationFlags or 2)
                    }
                }
            }

            // 17.0.1 起桌面在 OplusBubbleTextView.applyIconAndLabel 里对分身/归档图标
            // 不经 flags 直接调 getCloneAppDrawable(UserHandle) 补徽标；SDK>=38 时它内部取
            // getUserBadgeForDensity，分身资料运行时会拿到工作徽标，所以这里把返回值置空。
            // C16 及以下类名与方法名均未混淆，按名字定位；C17 起方法被混淆为 b(UserHandle)，只能经 DexKit 定位。
            if (osCode >= 40) {
                dexKitBridge.findMethod {
                    matcher {
                        paramTypes(classOf<UserHandle>())
                        returnType(classOf<Drawable>())
                    }
                }.apply {
                    checkDataList("getCloneAppDrawable")
                }.single().getMethodInstance(hostClassLoader!!).hook {
                    after {
                        if (!isClone) return@after
                        result = null
                    }
                }
            } else {
                "com.android.common.util.CacheUtils".toClass().resolve()
                    .firstMethod {
                        name = "getCloneAppDrawable"
                        returnType = Drawable::class
                    }.hook {
                        after {
                            if (!isClone) return@after
                            result = null
                        }
                    }
            }
        }
    }

    @Obfuscate
    object AppBadgeC13 : YukiBaseHooker() {
        override fun onHook() {
            val isShortcut = preferences(ModulePrefs).getBoolean("remove_app_shortcut_badge", false)
            val isWork = preferences(ModulePrefs).getBoolean("remove_app_work_badge", false)
            val isClone = preferences(ModulePrefs).getBoolean("remove_app_clone_badge", false)

            //Source BitmapInfo
            "com.android.launcher3.icons.BitmapInfo".toClass().resolve().apply {
                firstMethod { name = "applyFlags"; parameterCount = 3 }.hook {
                    before {
                        val drawableCreationFlags = lastArg().get<Int>() ?: 0
                        val badgeInfo = firstField { name = "badgeInfo" }.of(instance).get()
                        val flag = firstField { name = "flags" }.of(instance).get<Int>()
                            ?: return@before
                        if ((drawableCreationFlags and 2) == 0) {
                            if (badgeInfo != null) {
                                if (isShortcut) result = null
                            } else if ((flag and 2) != 0) {
                                //ic_instant_app_badge
                                //result = null
                            } else if ((flag and 1) != 0) {
                                //ic_work_app_badge
                                if (isWork) result = null
                            } else if ((flag and 4) != 0) {
                                //ic_oplus_clone_app_badge
                                if (isClone) result = null
                            }
                        }
                    }
                }
            }
        }
    }
}
