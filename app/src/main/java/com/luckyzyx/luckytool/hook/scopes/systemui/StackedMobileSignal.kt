package com.luckyzyx.luckytool.hook.scopes.systemui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.telephony.SubscriptionManager
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.BuildConfig
import com.luckyzyx.luckytool.R
import com.luckyzyx.luckytool.utils.MobileSignalDisplay
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import kotlin.math.abs
import kotlin.math.roundToInt

/** Combines two supported ColorOS cellular indicators in each System UI status bar. */
internal class StackedMobileSignal(private val rotated: Boolean) : YukiBaseHooker() {
    private val levels = WeakHashMap<ImageView, Int>()
    private val groups = WeakHashMap<ViewGroup, Merge?>()
    private var readingStockVisibility = false
    private var listeningForDataSim = false
    private var moduleResources: Resources? = null

    override fun onHook() {
        val mobile = "com.oplus.systemui.statusbar.phone.signal.widget.OplusModernStatusBarMobileView"
            .toClassOrNull() ?: return
        val container = "com.android.systemui.statusbar.phone.StatusIconContainer".toClassOrNull() ?: return
        val cellular = "com.android.systemui.statusbar.pipeline.mobile.domain.model.SignalIconModel\$Cellular"
            .toClassOrNull() ?: return
        val modern = "com.android.systemui.statusbar.pipeline.shared.ui.view.ModernStatusBarView"
            .toClassOrNull() ?: return
        val binder = "com.oplus.systemui.statusbar.pipeline.mobile.ui.view.OplusStatusBarMobileViewBinder"
            .toClassOrNull() ?: return
        val resourceId = runCatching { cellular.getMethod("toSignalDrawableState") }.getOrNull() ?: return
        val subId = runCatching { mobile.getMethod("getSubId") }.getOrNull() ?: return
        val isVisible = runCatching { modern.getMethod("isIconVisible") }.getOrNull() ?: return
        val visibleHook = modern.resolve().firstMethodOrNull { name = "isIconVisible"; parameterCount = 0 } ?: return
        val measure = container.resolve().firstMethodOrNull { name = "onMeasure"; parameterCount = 2 } ?: return
        val update = binder.resolve().firstMethodOrNull {
            name = "bindCustEx\$updateSignalIcon"; parameterCount = 2
        } ?: return

        update.hook {
            after {
                val image = args.getOrNull(0) as? ImageView ?: return@after
                val row = parentChain(image).firstOrNull(mobile::isInstance) ?: return@after
                if (image.id != id(image, "mobile_signal")) return@after
                val model = args.getOrNull(1)
                val level = if (cellular.isInstance(model)) {
                    runCatching {
                        val entry = image.resources.getResourceEntryName(resourceId.invoke(model) as Int)
                        MobileSignalDisplay.stockLevel(entry)
                    }.getOrNull()
                } else null
                if (level == null) levels.remove(image) else levels[image] = level
                (row.parent as? ViewGroup)?.takeIf(container::isInstance)?.requestLayout()
            }
        }
        visibleHook.hook {
            after {
                if (readingStockVisibility || result != true) return@after
                val view = instanceOrNull<View>() ?: return@after
                val group = view.parent as? ViewGroup ?: return@after
                val merge = groups[group] ?: return@after
                if (merge.secondary.get() === view && merge.isRenderedIn(group)) result = false
            }
        }
        measure.hook {
            before {
                val group = instanceOrNull<ViewGroup>() ?: return@before
                // The phone bar, lock screen and shade headers each own a statusIcons group.
                val statusIcons = id(group, "statusIcons")
                if (statusIcons == 0 || group.id != statusIcons) {
                    groups.remove(group)?.restore()
                    return@before
                }
                track(group)
                val rows = (0 until group.childCount).map(group::getChildAt).filter(mobile::isInstance)
                if (rows.size != 2) {
                    groups[group]?.restore()
                    groups[group] = null
                    return@before
                }
                readingStockVisibility = true
                val icons = try {
                    rows.mapNotNull { row ->
                        val signal = row.findViewById<ImageView>(id(row, "mobile_signal")) ?: return@mapNotNull null
                        val level = levels[signal] ?: return@mapNotNull null
                        val subscription = runCatching {
                            if (isVisible.invoke(row) == true) subId.invoke(row) as Int else null
                        }.getOrNull() ?: return@mapNotNull null
                        if (!SubscriptionManager.isValidSubscriptionId(subscription) || !signal.hasStockGeometry()) {
                            return@mapNotNull null
                        }
                        Candidate(row, signal, subscription, level)
                    }
                } finally {
                    readingStockVisibility = false
                }
                refresh(group, icons)
            }
        }
    }

    private fun track(group: ViewGroup) {
        if (!groups.containsKey(group)) {
            groups[group] = null
            group.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(view: View) = Unit
                override fun onViewDetachedFromWindow(view: View) {
                    groups.remove(group)?.restore()
                    group.removeOnAttachStateChangeListener(this)
                }
            })
        }
        if (listeningForDataSim) return
        val context = group.context.applicationContext ?: return
        listeningForDataSim = runCatching {
            context.registerReceiver(object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (intent.action == DEFAULT_DATA_CHANGED) groups.keys.toList().forEach(View::requestLayout)
                }
            }, IntentFilter(DEFAULT_DATA_CHANGED), Context.RECEIVER_EXPORTED)
        }.isSuccess
    }

    private fun refresh(group: ViewGroup, candidates: List<Candidate>) {
        val dataSim = SubscriptionManager.getDefaultDataSubscriptionId()
        val primary = candidates.singleOrNull { it.subscription == dataSim }
        val secondary = candidates.singleOrNull { it.subscription != dataSim }
        val previous = groups[group]
        if (primary == null || secondary == null) {
            previous?.restore()
            groups[group] = null
            return
        }
        val merge = if (previous?.matches(primary, secondary) == true) previous else {
            previous?.restore()
            Merge(primary, secondary, rotated)
        }
        val description = secondaryDescription(group.context, secondary)
        val stock = primary.signal.drawable ?: run {
            merge.restore()
            groups[group] = null
            return
        }
        if (stock is StackedSignalDrawable) stock.secondaryLevel = secondary.level
        else primary.signal.setImageDrawable(StackedSignalDrawable(stock, secondary.level, rotated))
        if (primary.row.stateDescription != description) primary.row.stateDescription = description
        groups[group] = merge
    }

    private fun secondaryDescription(context: Context, sim: Candidate): CharSequence? {
        val resources = moduleResources ?: runCatching {
            context.packageManager.getResourcesForApplication(BuildConfig.APPLICATION_ID)
        }.getOrNull()?.also { moduleResources = it }
        return runCatching {
            resources?.getString(R.string.mobile_signal_secondary_description, sim.level)
        }.getOrNull() ?: sim.row.contentDescription ?: sim.signal.contentDescription
    }

    private fun ImageView.hasStockGeometry(): Boolean {
        val stock = drawable ?: return false
        val density = resources.displayMetrics.density
        return abs(stock.intrinsicWidth - (StockSignalGeometry.WIDTH * density).roundToInt()) <= 1 &&
            abs(stock.intrinsicHeight - (StockSignalGeometry.HEIGHT * density).roundToInt()) <= 1
    }

    private fun parentChain(view: View) = generateSequence(view.parent as? View) { it.parent as? View }

    private data class Candidate(val row: View, val signal: ImageView, val subscription: Int, val level: Int)

    private class Merge(primary: Candidate, secondary: Candidate, rotated: Boolean) {
        private val row = WeakReference(primary.row)
        private val image = WeakReference(primary.signal)
        val secondary = WeakReference(secondary.row)
        private val description = primary.row.stateDescription
        private val markers = buildList {
            fun find(name: String) = primary.row.findViewById<View>(id(primary.row, name))
            fun move(view: View?, offset: Float) {
                if (view == null) return
                val y = view.translationY
                view.translationY = y + offset * view.resources.displayMetrics.density
                add(WeakReference(view) to y)
            }
            val inlineOffset = if (rotated) 6.5f else -.9f
            move(find("mobile_type"), inlineOffset)
            move(find("data_inout"), inlineOffset)
            if (rotated) {
                // Large network badges and separate arrows already occupy a lane left of the glyph.
                val separate = listOfNotNull(find("mobile_type_alone"), find("data_inout_alone"))
                val lane = separate.firstOrNull()?.parent as? View
                if (lane != null && lane !== primary.row && lane !== primary.signal.parent &&
                    separate.all { it.parent === lane }
                ) move(lane, 2f)
                else separate.forEach { move(it, 2f) }
                move(find("inout_container"), 2f)
            }
        }

        fun matches(first: Candidate, second: Candidate) =
            row.get() === first.row && image.get() === first.signal && secondary.get() === second.row

        fun isRenderedIn(group: ViewGroup) = row.get()?.parent === group && image.get()?.drawable is StackedSignalDrawable

        fun restore() {
            image.get()?.let { signal ->
                val stacked = signal.drawable as? StackedSignalDrawable ?: return@let
                signal.setImageDrawable(stacked.stock)
            }
            row.get()?.let { if (it.stateDescription != description) it.stateDescription = description }
            markers.forEach { (view, y) -> view.get()?.translationY = y }
        }
    }

    private companion object {
        const val DEFAULT_DATA_CHANGED = "android.intent.action.ACTION_DEFAULT_DATA_SUBSCRIPTION_CHANGED"

        fun id(view: View, name: String) = view.resources.getIdentifier(name, "id", "com.android.systemui")
    }
}
