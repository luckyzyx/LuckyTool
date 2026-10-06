package com.luckyzyx.luckytool.hook.scopes.systemui

import android.annotation.SuppressLint
import android.graphics.drawable.Drawable
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import com.android.internal.graphics.drawable.BackgroundBlurDrawable
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.dp
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object NotificationBackgroundBlurAlpha : YukiBaseHooker() {
    override fun onHook() {
        val osCode = getOSVersionCode
        if (osCode >= 34) loadHooker(NotificationBackgroundBlurAlphaV15)
        else loadHooker(NotificationBackgroundBlurAlphaV14)
    }

    @Obfuscate
    object NotificationBackgroundBlurAlphaV15 : YukiBaseHooker() {
        override fun onHook() {
            var customAlpha =
                preferences(ModulePrefs).getInt("custom_notification_background_transparency", -1)
            dataChannel.wait<Int>("custom_notification_background_transparency") {
                customAlpha = it
            }
            var enableBlur =
                preferences(ModulePrefs).getBoolean("enable_notification_background_blur_effect", false)
            dataChannel.wait<Boolean>("enable_notification_background_blur_effect") {
                enableBlur = it
            }

            //Source NotificationBackgroundView
            "com.android.systemui.statusbar.notification.row.NotificationBackgroundView".toClass()
                .resolve().apply {
                    firstMethod { name = "draw";parameterCount = 2 }.hook {
                        before {
                            if (customAlpha < 0 || enableBlur) return@before
                            val alphaValue = customAlpha * 25
                            val mBackground = lastArg().get<Drawable>() ?: return@before
                            mBackground.alpha = alphaValue
                        }
                    }
                }

            //Source NotificationBackgroundViewExtImp
            "com.oplus.systemui.statusbar.notification.row.NotificationBackgroundViewExtImp".toClass()
                .resolve().apply {
                    firstMethod { name = "getOplusStyle";superclass() }.hook {
                        before {
                            if (customAlpha < 0) return@before
                            result = if (enableBlur) true else false
                        }
                    }
                }

            //Source ExpandableNotificationRow
            "com.android.systemui.statusbar.notification.row.ExpandableNotificationRow".toClass()
                .resolve().apply {
                    firstMethod { name = "updateBackgroundForGroupState" }.hook {
                        before {
                            if (customAlpha < 0 || !enableBlur) return@before
                            firstField { name = "mShowGroupBackgroundWhenExpanded" }.of(instance)
                                .set(true)
                        }
                    }
                }
        }
    }

    @Obfuscate
    object NotificationBackgroundBlurAlphaV14 : YukiBaseHooker() {
        private var disableBlur = false

        @SuppressLint("DiscouragedApi")
        override fun onHook() {
            var customAlpha =
                preferences(ModulePrefs).getInt("custom_notification_background_transparency", -1)
            dataChannel.wait<Int>("custom_notification_background_transparency") {
                customAlpha = it
            }
            var enableBlur =
                preferences(ModulePrefs).getBoolean("enable_notification_background_blur_effect", false)
            dataChannel.wait<Boolean>("enable_notification_background_blur_effect") {
                enableBlur = it
            }

            //Source NotificationBackgroundView
            "com.android.systemui.statusbar.notification.row.NotificationBackgroundView".toClass()
                .resolve().apply {
                    firstMethod { name = "draw";parameterCount = 2 }.hook {
                        before {
                            if (customAlpha < 0 || enableBlur) return@before
                            val alphaValue = customAlpha * 25
                            val mBackground = lastArg().get<Drawable>() ?: return@before
                            mBackground.alpha = alphaValue
                        }
                    }
                }

            //Source NotificationBackgroundViewExtImp
            "com.oplus.systemui.statusbar.notification.row.NotificationBackgroundViewExtImp".toClass()
                .resolve().apply {
                    firstMethod { name = "getOplusStyle";superclass() }.hook {
                        before {
                            if (customAlpha < 0) return@before
                            result = if (enableBlur) true else false
                        }
                    }
                    firstMethod { name = "drawBlur";superclass() }.hook {
                        before {
                            if (customAlpha < 0) return@before
                            result = if (enableBlur) true else false
                        }
                    }
                    firstMethod { name = "decideBlurDrawable" }.hook {
                        before {
                            if (customAlpha < 0) return@before
                            if (!disableBlur) firstMethod {
                                name = "getRowBlurDelegate";superclass()
                            }
                                .of(instance).invoke()?.asResolver()?.firstMethod {
                                    name = "setBlurType";superclass()
                                }?.invoke(1)
                        }
                        after {
                            if (customAlpha < 0) return@after
                            val value = customAlpha * 25
                            val res = result<Drawable>() ?: return@after
                            if (res is BackgroundBlurDrawable) {
                                val mBlurRadius =
                                    res.asResolver().firstField { name = "mBlurRadius" }.get<Int>()
                                if (mBlurRadius != value.dp) res.setBlurRadius(value.dp)
                            }
                        }
                    }
                }

            //Source OplusRowsBlurManager
            "com.oplus.systemui.blur.OplusRowsBlurManager".toClass().resolve().apply {
                firstMethod { name = "blurMediaPanel" }.hook {
                    before {
                        if (customAlpha < 0) return@before
                        disableBlur = firstArg().get<Boolean>() ?: false
                    }
                }
            }

            //Source ExpandableNotificationRow
            "com.android.systemui.statusbar.notification.row.ExpandableNotificationRow".toClass()
                .resolve().apply {
                    firstMethod { name = "updateBackgroundForGroupState" }.hook {
                        before {
                            if (customAlpha < 0 || !enableBlur) return@before
                            firstField { name = "mShowGroupBackgroundWhenExpanded" }.of(instance)
                                .set(true)
                        }
                    }
                }

            //Source SeedlingItemRow
            "com.oplus.systemui.plugins.seedling.notification.widget.SeedlingItemRow".toClass()
                .resolve().apply {
                    firstMethod { name = "initBackground" }.hook {
                        after {
                            val view = instance<View>()
                            val drawableId = view.resources.getIdentifier(
                                "notification_seed_action_rounded_bg", "drawable",
                                this@NotificationBackgroundBlurAlphaV14.packageName
                            )
                            val drawable = AppCompatResources.getDrawable(view.context, drawableId)
                                ?: return@after
                            val newDrawable = drawable.mutate().apply {
                                alpha = 255 / 10 * customAlpha
                            }
                            val backgroundNormal =
                                firstField { name = "mBackgroundNormal" }.of(instance).get()
                                    ?: return@after
                            backgroundNormal.asResolver().firstMethod {
                                name = "setCustomBackground";parameters(Drawable::class)
                            }.invoke(newDrawable)
                            backgroundNormal.asResolver().firstMethod {
                                name = "setTint";parameters(Int::class)
                            }.invoke(0)
                        }
                    }
                }
        }
    }

}