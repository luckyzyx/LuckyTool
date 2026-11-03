package com.luckyzyx.luckytool.hook.scopes.appdetail

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.TextUtils
import android.text.format.Formatter
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.highcapable.betterandroid.ui.extension.view.textColor
import com.luckyzyx.luckytool.R
import org.lsposed.lsparanoid.Obfuscate
import java.io.File

@SuppressLint("ViewConstructor")
@Obfuscate
internal class ApkDetailsView(context: Context, private val moduleResources: Resources) :
    LinearLayout(context) {
    private val dark =
        resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    private val primary =
        themeColor("couiColorLabelPrimary", if (dark) 0xFFF2F2F2.toInt() else 0xFF191919.toInt())
    private val secondary =
        themeColor("couiColorLabelSecondary", if (dark) 0xFFAAAAAA.toInt() else 0xFF757575.toInt())
    private val cardColor =
        themeColor("couiColorCardBackground", if (dark) 0xFF242424.toInt() else Color.WHITE)

    init {
        orientation = VERTICAL
        setPadding(0, dp(24), 0, dp(8))
    }

    fun bind(
        pkg: String,
        versionName: String,
        versionCode: String,
        path: String,
        incoming: PackageInfo?,
        current: PackageInfo?
    ) {
        removeAllViews()
        val unknown = moduleResources.getString(R.string.apk_details_unknown)
        val newVersion = versionName.ifBlank { unknown }
        val newCode = incoming?.longVersionCode?.toString() ?: versionCode.ifBlank { unknown }

        val versions = card()
        versions.addView(text(moduleResources.getString(R.string.apk_details_versions), 16f, primary, true))
        val comparison =
            LinearLayout(context).apply { orientation = HORIZONTAL; isBaselineAligned = false }
        comparison.addView(
            versionColumn(
                moduleResources.getString(R.string.apk_details_installed),
                if (current == null) moduleResources.getString(R.string.apk_details_not_installed) else current.versionName
                    ?: unknown,
                current?.longVersionCode?.toString()
            ), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        )
        if (incoming != null) {
            comparison.addView(View(context), LayoutParams(dp(30), 1))
            comparison.addView(
                versionColumn(
                    moduleResources.getString(R.string.apk_details_incoming), newVersion, newCode
                ), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
            )
        }
        versions.addView(
            comparison, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = dp(20)
            })
        addView(versions)

        if (incoming != null) {
            val info = card()
            info.addView(
                text(
                    moduleResources.getString(R.string.apk_details_information), 16f, primary, true
                )
            )
            info.addView(
                row(
                    R.string.apk_details_package, pkg.ifBlank { unknown })
            )
            val size =
                path.takeIf { it.isNotBlank() }?.let { File(it).takeIf(File::isFile)?.length() }
            info.addView(
                row(
                R.string.apk_details_size,
                size?.let { Formatter.formatFileSize(context, it) } ?: unknown))
            info.addView(
                row(
                    R.string.apk_details_min_sdk,
                    incoming.applicationInfo?.minSdkVersion?.toString() ?: unknown
                )
            )
            info.addView(
                row(
                    R.string.apk_details_target_sdk,
                    incoming.applicationInfo?.targetSdkVersion?.toString() ?: unknown
                )
            )
            // 部分系统应用在清单 metadata 里声明可用的 OS 版本区间，缺字段时整行不显示
            val metaData = incoming.applicationInfo?.metaData
            metaData?.metadataValue("minOsdkVersion")?.let {
                info.addView(row(R.string.apk_details_min_os_version, it))
            }
            metaData?.metadataValue("maxOsdkVersion")?.let {
                info.addView(row(R.string.apk_details_max_os_version, it))
            }
            addView(
                info, LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(12) })
        }
    }

    fun addAppHeader(name: String, icon: Drawable?, source: String, version: String, size: String) {
        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(ImageView(context).apply {
                setImageDrawable(icon)
                importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
                scaleType = ImageView.ScaleType.FIT_CENTER
            }, LayoutParams(dp(72), dp(72)))
            addView(LinearLayout(context).apply {
                orientation = VERTICAL
                addView(text(name, 22f, primary, true, TextUtils.TruncateAt.END))
                addView(
                    text(
                        listOf(version, size).filter { it.isNotBlank() }.joinToString(" · "),
                        13f,
                        secondary,
                        truncateAt = TextUtils.TruncateAt.MIDDLE
                    ), LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(6) })
                if (source.isNotBlank()) addView(
                    text(source, 13f, secondary), LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(6) })
            }, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(16) })
        }
        setPadding(0, dp(12), 0, dp(8))
        addView(
            header, 0, LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(24) })
    }

    private fun card() = LinearLayout(context).apply {
        orientation = VERTICAL
        setPadding(dp(20), dp(20), dp(20), dp(12))
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        background =
            GradientDrawable().apply { setColor(cardColor); cornerRadius = dp(24).toFloat() }
    }

    private fun versionColumn(label: String, version: String, code: String?) =
        LinearLayout(context).apply {
            orientation = VERTICAL
            addView(text(label, 13f, secondary))
            addView(
                text(version, 20f, primary, true, TextUtils.TruncateAt.MIDDLE), LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) })
            code?.let {
                addView(
                    text(moduleResources.getString(R.string.apk_details_version_code, it), 12f, secondary),
                    LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(4) })
            }
            setPadding(0, 0, 0, dp(8))
        }

    // 标签只占自身文字宽度，剩下的宽度全部留给值，包名这类长文本才不会一半就换行
    private fun row(label: Int, value: String) = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(48)
        setPadding(0, dp(10), 0, dp(10))
        addView(
            text(moduleResources.getString(label), 14f, secondary),
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        )
        addView(
            text(value, 15f, primary, truncateAt = TextUtils.TruncateAt.MIDDLE)
                .apply { gravity = Gravity.END },
            LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(12) }
        )
    }

    // 单行加省略号：包名、版本号这类长度不受控的文本折行会把整张卡片撑高，挤掉宿主底部的按钮
    private fun text(
        value: String,
        size: Float,
        color: Int,
        medium: Boolean = false,
        truncateAt: TextUtils.TruncateAt? = null
    ) = TextView(context).apply {
        text = value
        textSize = size
        textColor = color
        includeFontPadding = false
        textDirection = TEXT_DIRECTION_FIRST_STRONG
        if (medium) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        truncateAt?.let {
            setSingleLine(true)
            ellipsize = it
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun themeColor(name: String, fallback: Int): Int {
        val id = resources.getIdentifier(name, "attr", context.packageName)
        val value = TypedValue()
        if (id == 0 || !context.theme.resolveAttribute(id, value, true)) return fallback
        return if (value.resourceId != 0) runCatching { context.getColor(value.resourceId) }.getOrDefault(
            fallback
        )
        else if (value.type in TypedValue.TYPE_FIRST_COLOR_INT..TypedValue.TYPE_LAST_COLOR_INT) value.data
        else fallback
    }

    // meta-data 的值可能是数字也可能是字符串，统一转成文本后再判断有没有实际内容
    private fun Bundle.metadataValue(key: String) =
        get(key)?.toString()?.trim()?.takeIf { it.isNotEmpty() }

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()
}
