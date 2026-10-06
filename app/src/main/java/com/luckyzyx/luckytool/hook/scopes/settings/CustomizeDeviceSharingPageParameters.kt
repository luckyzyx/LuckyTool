package com.luckyzyx.luckytool.hook.scopes.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.view.allViews
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.utils.appcompat.dialog.COUIAlertDialogBuilder
import com.luckyzyx.luckytool.utils.safeOfNull
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object CustomizeDeviceSharingPageParameters : YukiBaseHooker() {

    @SuppressLint("DiscouragedApi")
    override fun onHook() {
        //Source ShareAboutPhoneActivity
        "com.oplus.settings.feature.deviceinfo.aboutphone.ShareAboutPhoneActivity".toClass()
            .resolve().apply {
            firstMethod { name = "onCreate" }.hook {
                after {
                    val activity = instance<Activity>()
                    val shareViewId = activity.resources.getIdentifier(
                        "share_view", "id", this@CustomizeDeviceSharingPageParameters.packageName
                    ).takeIf { it != 0 } ?: return@after
                    val shareView = activity.findViewById<ViewGroup>(shareViewId) ?: return@after
                    shareView.allViews.forEachIndexed { _, view ->
                        if (view is TextView) {
                            val name = safeOfNull {
                                activity.resources.getResourceEntryName(view.id)
                            }
                            if (name != "share_picture") view.setClickInfo()
                        }
                    }
                }
            }
        }
    }

    private fun TextView.setClickInfo() {
        setOnClickListener {
            var editText: EditText? = null
            var dialog: Any? = null
            COUIAlertDialogBuilder(context, "COUIAlertDialog.SingleInput", hostClassLoader).apply {
                setTitle(text)
                setNegativeButton(android.R.string.cancel, null)
                setPositiveButton(android.R.string.ok) { _, _ ->
                    val newText = editText?.text as CharSequence
                    text = newText.ifBlank { " " }
                    dialog?.dismiss()
                }
                dialog = builder?.show()
                editText = dialog?.getEditText("edit_text_1")
                editText?.isSingleLine = false
                editText?.maxLines = 5
                editText?.setText(text)
            }
        }
    }
}