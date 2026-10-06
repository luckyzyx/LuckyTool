package com.luckyzyx.luckytool.hook.scopes.appdetail

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageInfo
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.format.Formatter
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.highcapable.betterandroid.ui.extension.view.child
import com.highcapable.betterandroid.ui.extension.view.textColor
import com.highcapable.yukihookapi.hook.factory.injectModuleResources
import com.luckyzyx.luckytool.R
import org.lsposed.lsparanoid.Obfuscate
import java.io.File

@Obfuscate
internal class ApkDetailsView(context: Context) : LinearLayout(context) {
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
        context.injectModuleResources()
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
        val unknown = context.getString(R.string.apk_details_unknown)
        val newVersion = versionName.ifBlank { unknown }
        val newCode = incoming?.longVersionCode?.toString() ?: versionCode.ifBlank { unknown }

        val versions = card()
        versions.addView(text(context.getString(R.string.apk_details_versions), 16f, primary, true))
        val comparison =
            LinearLayout(context).apply { orientation = HORIZONTAL; isBaselineAligned = false }
        comparison.addView(
            versionColumn(
                context.getString(R.string.apk_details_installed),
                if (current == null) context.getString(R.string.apk_details_not_installed) else current.versionName
                    ?: unknown,
                current?.longVersionCode?.toString()
            ), LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        )
        if (incoming != null) {
            comparison.addView(View(context), LayoutParams(dp(30), 1))
            comparison.addView(
                versionColumn(
                    context.getString(R.string.apk_details_incoming), newVersion, newCode
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
                    context.getString(R.string.apk_details_information), 16f, primary, true
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
                addView(text(name, 22f, primary, true))
                addView(
                    text(
                        listOf(version, size).filter { it.isNotBlank() }.joinToString(" · "),
                        13f,
                        secondary
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
                text(version, 20f, primary, true), LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(8) })
            code?.let {
                addView(
                    text(context.getString(R.string.apk_details_version_code, it), 12f, secondary),
                    LayoutParams(
                        LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(4) })
            }
            setPadding(0, 0, 0, dp(8))
        }

    private fun row(label: Int, value: String) = LinearLayout(context).apply {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(48)
        setPadding(0, dp(10), 0, dp(10))
        addView(
            text(context.getString(label), 14f, secondary),
            LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        )
        addView(
            text(value, 15f, primary).apply { gravity = Gravity.END },
            LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        )
    }

    private fun text(value: String, size: Float, color: Int, medium: Boolean = false) =
        TextView(context).apply {
            text = value
            textSize = size
            textColor = color
            includeFontPadding = false
            textDirection = TEXT_DIRECTION_FIRST_STRONG
            if (medium) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
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

    private fun dp(value: Int) = (value * resources.displayMetrics.density + 0.5f).toInt()

    companion object {
        private const val TAG = "LuckyTool.ApkDetails"

        fun show(header: ViewGroup, pkg: String, version: String, code: String, path: String) {
            val incoming = runCatching {
                header.context.packageManager.getPackageArchiveInfo(
                    path, 0
                )
            }.getOrNull()
            val current =
                runCatching { header.context.packageManager.getPackageInfo(pkg, 0) }.getOrNull()
            val existing = header.findViewWithTag<View>(TAG)
            if (existing != null && existing !is ApkDetailsView) header.removeView(existing)
            val panel =
                (existing as? ApkDetailsView) ?: ApkDetailsView(header.context).also { panel ->
                    panel.tag = TAG
                    header.addView(
                        panel,
                        ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    )
                    // Host ConstraintLayout measures the native header. Place the extra content below it
                    // and remeasure when width/font scale changes, without depending on obfuscated fields.
                    header.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                        resize(
                            header, panel
                        )
                    }
                }
            panel.bind(pkg, version, code, path, incoming, current)
            header.post { resize(header, panel) }
        }

        private fun resize(header: ViewGroup, panel: ApkDetailsView) {
            if (header.width <= 0) return
            val bottom = (0 until header.childCount).map { header.child(it) }
                .filter { it !== panel && it.visibility != GONE }.maxOfOrNull { it.bottom } ?: 0
            val width = (header.width - header.paddingLeft - header.paddingRight).coerceAtLeast(0)
            panel.measure(
                MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            )
            panel.translationY = (bottom - panel.top).toFloat()
            val height = bottom + panel.measuredHeight + header.paddingBottom
            if (header.layoutParams.height != height) header.layoutParams =
                header.layoutParams.apply { this.height = height }
        }
    }
}
