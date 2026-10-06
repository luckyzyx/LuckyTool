package com.luckyzyx.luckytool.hook.scopes.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.view.children
import androidx.core.view.isVisible
import com.highcapable.betterandroid.ui.extension.component.startActivity
import com.highcapable.betterandroid.ui.extension.view.parentOrNull
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.dp
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.safeOfNull
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object CustomizeDeviceOTACardBackground : YukiBaseHooker() {

    @SuppressLint("DiscouragedApi")
    override fun onHook() {
        val osCode = getOSVersionCode
        val backgroundPath =
            preferences(ModulePrefs).getString("customize_device_ota_card_background_path", "")
        val hideText = preferences(ModulePrefs).getBoolean("hide_ota_card_top_text", false)
        val applySharePage =
            preferences(ModulePrefs).getBoolean("apply_device_parameter_sharing_page", false)

        //Source AboutDeviceOtaUpdatePreference
        "com.oplus.settings.widget.preference.AboutDeviceOtaUpdatePreference".toClass().resolve()
            .apply {
                firstMethod { name = "onBindViewHolder" }.hook {
                    after {
                        val holder = firstArg().get() ?: return@after
                        val itemView =
                            holder.asResolver().firstField { name = "itemView"; superclass() }
                                .get<View>() ?: return@after
                        if (itemView is RelativeLayout) {
                            val topId = itemView.resources.getIdentifier(
                                "about_device_top_bg", "id",
                                this@CustomizeDeviceOTACardBackground.packageName
                            )
                            val maskId = itemView.resources.getIdentifier(
                                "about_device_top_video_mask", "id",
                                this@CustomizeDeviceOTACardBackground.packageName
                            )
                            val bitmap = BitmapFactory.decodeFile(backgroundPath) ?: return@after
                            val drawable = RoundedBitmapDrawableFactory.create(
                                itemView.resources, bitmap
                            )
                            drawable.cornerRadius = 12F.dp

                            itemView.findViewById<ImageView>(topId)?.setImageDrawable(drawable)
                            itemView.findViewById<ImageView>(maskId)?.setImageDrawable(drawable)

                            if (hideText) itemView.children.forEach {
                                val id = safeOfNull { it.resources.getResourceEntryName(it.id) }
                                    ?: return@forEach
                                val hidelist = arrayOf(
                                    "logo_view",
                                    "linearLayout",
                                    "model_name",
                                    "model_ai",
                                    "model_description",
                                    "coloros_logo",
                                    "model_number",
                                    "img_intent_ota",
                                    "update_linear",
                                    "lin_center",
                                    "device_market_name",
                                    "update_text",
                                    "update_find",
                                )
                                if (id in hidelist) it.isVisible = false
                            }
                        }
                    }
                }
                if (false) {
                    firstMethod { name = "isSupportTopVideo" }.hook {
                        intercept(false)
                    }
                    firstMethod { name = "isSupportEasterEggVideo" }.hook {
                        intercept(false)
                    }
                    firstMethod { name = "applyVideoTransform" }.hook {
                        before {
                            firstArg().set(null)
                        }
                    }
                    firstMethod { name = "getColorOSVideoPath" }.hook {
                        after {
                            result = "/sdcard/DNA/1.mp4"
                        }
                    }
                }
            }

        if (osCode < 34 || !applySharePage) return

        //Source DeviceInfoFragment
        if (false) "com.oplus.settings.feature.deviceinfo.aboutphone.DeviceInfoFragment".toClass()
            .resolve().apply {
                firstMethod {
                    name = "onCreateOptionsMenu"
                    parameters(Menu::class, MenuInflater::class)
                }.hook {
                    after {
                        val context = firstMethod { name = "getContext"; superclass() }
                            .of(instance).invoke<Context>() ?: return@after
                        val menu = firstArg().get<Menu>() ?: return@after
                        val menuInflater = lastArg().get<MenuInflater>() ?: return@after
                        val menuId = context.resources.getIdentifier(
                            "about_device_share_menu", "menu",
                            this@CustomizeDeviceOTACardBackground.packageName
                        )
                        menuInflater.inflate(menuId, menu)
                    }
                }
                firstMethod {
                    name = "onOptionsItemSelected"
                    parameters(MenuItem::class)
                    returnType = Boolean::class
                }.hook {
                    after {
                        val context = firstMethod { name = "getContext"; superclass() }
                            .of(instance).invoke<Context>() ?: return@after
                        val menuItem = firstArg().get<MenuItem>() ?: return@after
                        val shareId = context.resources.getIdentifier(
                            "about_share", "id",
                            this@CustomizeDeviceOTACardBackground.packageName
                        )
                        if (menuItem.itemId != shareId) return@after
                        context.startActivity(
                            "com.android.settings",
                            "com.oplus.settings.feature.deviceinfo.aboutphone.ShareAboutPhoneActivity",
                            false
                        )
                    }
                }
            }

        //Source ShareAboutPhoneActivity parent_relativeLayout
        "com.oplus.settings.feature.deviceinfo.aboutphone.ShareAboutPhoneActivity".toClass()
            .resolve().apply {
                firstMethod { name = "updateOsVersion" }.hook {
                    after {
                        val activity = instance<Activity>()
                        val viewId = activity.resources.getIdentifier(
                            "parent_relativeLayout", "id",
                            this@CustomizeDeviceOTACardBackground.packageName
                        )
                        val relativeLayout = activity.findViewById<RelativeLayout>(viewId)

                        val bitmap = BitmapFactory.decodeFile(backgroundPath) ?: return@after
                        val drawableFactory = RoundedBitmapDrawableFactory.create(
                            relativeLayout.resources, bitmap
                        )
                        drawableFactory.cornerRadius = 12F.dp

                        if (relativeLayout != null) {
                            val topId = activity.resources.getIdentifier(
                                "about_device_top_bg", "id",
                                this@CustomizeDeviceOTACardBackground.packageName
                            )
                            activity.findViewById<View>(topId)?.let {
                                relativeLayout.removeView(it)
                            }

                            relativeLayout.background = drawableFactory
                        } else {
                            val linId = activity.resources.getIdentifier(
                                "lin_button", "id",
                                this@CustomizeDeviceOTACardBackground.packageName
                            )
                            (activity.findViewById<LinearLayout>(linId)
                                ?.parentOrNull<RelativeLayout>())
                                ?.background = drawableFactory
                        }
                    }
                }
            }
    }
}