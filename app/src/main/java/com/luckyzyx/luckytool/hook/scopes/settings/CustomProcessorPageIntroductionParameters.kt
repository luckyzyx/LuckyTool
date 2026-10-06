package com.luckyzyx.luckytool.hook.scopes.settings

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.edit
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.utils.appcompat.dialog.COUIAlertDialogBuilder
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.dp
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
@SuppressLint("DiscouragedApi")
object CustomProcessorPageIntroductionParameters : YukiBaseHooker() {
    override fun onHook() {
        val replaceImage =
            preferences(ModulePrefs).getBoolean("custom_processor_image_path_switch", false)
        val imagePath = preferences(ModulePrefs).getString("customize_processor_image_path", "")
        val replaceText = preferences(ModulePrefs).getBoolean("custom_processor_introduction_text", false)

        //Source ProcessorDetailPreference
        "com.oplus.settings.feature.deviceinfo.processordetail.ProcessorDetailPreference".toClass()
            .resolve().apply {
                firstMethod { name = "onBindViewHolder" }.hook {
                    after {
                        val viewHolder = firstArg().get() ?: return@after
                        val context = firstMethod { name = "getContext";superclass() }.of(instance)
                            .invoke<Context>() ?: return@after

                        if (replaceImage) {
                            context.resources.getIdentifier(
                                "iv_top", "id",
                                this@CustomProcessorPageIntroductionParameters.packageName
                            ).takeIf { e -> e != 0 }?.let {
                                viewHolder.asResolver().firstMethod {
                                    name = "findViewById";parameters(Int::class)
                                }.invoke<ImageView>(it)
                            }?.apply {
                                val bitmap = BitmapFactory.decodeFile(imagePath)
                                if (bitmap != null) {
                                    val drawableFactory = RoundedBitmapDrawableFactory.create(
                                        resources, bitmap
                                    )
                                    drawableFactory.cornerRadius = 12F.dp
                                    setImageDrawable(drawableFactory)
                                }
                            }
                        }

                        if (replaceText) {
                            "tv_processor_description_1".let { key ->
                                context.resources.getIdentifier(
                                    key, "id",
                                    this@CustomProcessorPageIntroductionParameters.packageName
                                ).takeIf { e -> e != 0 }?.let {
                                    viewHolder.asResolver().firstMethod {
                                        name = "findViewById";parameters(Int::class)
                                    }.invoke<TextView>(it)
                                }?.setClickInfo(key)
                            }

                            "tv_processor_description_2".let { key ->
                                context.resources.getIdentifier(
                                    key, "id",
                                    this@CustomProcessorPageIntroductionParameters.packageName
                                ).takeIf { e -> e != 0 }?.let {
                                    viewHolder.asResolver().firstMethod {
                                        name = "findViewById";parameters(Int::class)
                                    }.invoke<TextView>(it)
                                }?.setClickInfo(key)
                            }

                            "tv_processor_description_3".let { key ->
                                context.resources.getIdentifier(
                                    key, "id",
                                    this@CustomProcessorPageIntroductionParameters.packageName
                                ).takeIf { e -> e != 0 }?.let {
                                    viewHolder.asResolver().firstMethod {
                                        name = "findViewById";parameters(Int::class)
                                    }.invoke<TextView>(it)
                                }?.setClickInfo(key)
                            }
                        }
                    }
                }
            }
    }

    @SuppressLint("ApplySharedPref")
    private fun TextView.setClickInfo(key: String) {
        val sp = context.getSharedPreferences(
            context.packageName + "_lt_preferences", Context.MODE_PRIVATE
        )
        sp.getString(key, "").takeIf { e -> !e.isNullOrBlank() }?.let { text = it }
        setOnClickListener {
            var editText: EditText? = null
            var dialog: Any? = null
            COUIAlertDialogBuilder(context, "COUIAlertDialog.SingleInput", hostClassLoader).apply {
                setTitle(text)
                setNegativeButton(android.R.string.cancel, null)
                setPositiveButton(android.R.string.ok) { _, _ ->
                    val newText = editText?.text as CharSequence
                    if (newText.isNotBlank()) {
                        text = newText
                        val edit = sp.edit()
                        edit.putString(key, newText.toString())
                        if (edit.commit()) dialog?.dismiss()
                    }
                }
                dialog = builder?.show()
                editText = dialog?.getEditText("edit_text_1")
                editText?.isSingleLine = false
                editText?.maxLines = 5
                editText?.setText(text)
            }
        }
        setOnLongClickListener {
            sp.edit(commit = true) { remove(key) }
            true
        }
    }
}