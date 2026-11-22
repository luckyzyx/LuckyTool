package com.luckyzyx.luckytool.hook.scopes.systemui

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.UserHandle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.Display
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.BuildConfig
import com.luckyzyx.luckytool.R
import java.lang.reflect.Proxy
import java.util.concurrent.Executor

internal const val NOTIFICATION_PROGRESS_KEY = "status_bar_notification_progress"
internal const val NOTIFICATION_PROGRESS_MIN_OS = 37

internal object StatusBarNotificationProgress : YukiBaseHooker() {
    // UserTracker retains a weak reference to its callback.
    private var userListener: Any? = null

    override fun onHook() {
        runCatching {
            install()
        }.onFailure { YLog.warn("Notification progress unavailable: ${it.message}") }
    }

    private fun install() {
        val pipelineType = "com.android.systemui.statusbar.notification.collection.NotifPipeline".toClassOrNull() ?: return
        val entryType = "com.android.systemui.statusbar.notification.collection.NotificationEntry".toClassOrNull() ?: return
        val collectionListener = "com.android.systemui.statusbar.notification.collection.notifcollection.NotifCollectionListener"
            .toClassOrNull() ?: return
        val centralType = "com.android.systemui.statusbar.phone.CentralSurfacesImpl".toClassOrNull() ?: return
        val controllerType = "com.android.systemui.statusbar.phone.PhoneStatusBarViewController".toClassOrNull() ?: return
        val dispatcherType = "com.android.systemui.plugins.DarkIconDispatcher".toClassOrNull() ?: return
        val receiverType = "com.android.systemui.plugins.DarkIconDispatcher\$DarkReceiver".toClassOrNull() ?: return
        val statusType = "com.android.systemui.statusbar.StatusBarStateControllerImpl".toClassOrNull() ?: return
        val stateListener = "com.android.systemui.plugins.statusbar.StatusBarStateController\$StateListener"
            .toClassOrNull() ?: return
        val shadeType = "com.android.systemui.shade.ShadeExpansionStateManager".toClassOrNull() ?: return
        val expansionListener = "com.android.systemui.shade.ShadeExpansionListener".toClassOrNull() ?: return
        val expansionEvent = "com.android.systemui.shade.ShadeExpansionChangeEvent".toClassOrNull() ?: return
        val userType = "com.android.systemui.settings.UserTracker".toClassOrNull() ?: return
        val userCallback = "com.android.systemui.settings.UserTracker\$Callback".toClassOrNull() ?: return
        val commandType = "com.android.systemui.statusbar.CommandQueue".toClassOrNull() ?: return
        val iconType = "$SYSTEM_UI.statusbar.StatusBarIconView".toClassOrNull() ?: return
        val containerType = "$SYSTEM_UI.statusbar.phone.NotificationIconContainer".toClassOrNull() ?: return

        val pipelineInit = pipelineType.resolve().firstConstructorOrNull() ?: return
        val start = centralType.resolve().firstMethodOrNull { name = "start"; parameterCount = 0 } ?: return
        val attach = controllerType.resolve().firstMethodOrNull { name = "onViewAttached"; parameterCount = 0 } ?: return
        val detach = controllerType.resolve().firstMethodOrNull { name = "onViewDetached"; parameterCount = 0 } ?: return
        val measureContainer = containerType.resolve().firstMethodOrNull { name = "onMeasure"; parameterCount = 2 } ?: return
        val configureContainer = containerType.resolve().firstMethodOrNull { name = "onConfigurationChanged"; parameterCount = 1 } ?: return
        val calculateIconPositions = containerType.resolve().firstMethodOrNull { name = "calculateIconXTranslations"; parameterCount = 0 } ?: return
        val nominalIconSize = containerType.getDeclaredField("mIconSize").apply { isAccessible = true }
        val measureIcon = iconType.resolve().firstMethodOrNull { name = "onMeasure"; parameterCount = 2 } ?: return
        val drawIcon = iconType.resolve().firstMethodOrNull { name = "onDraw"; parameterCount = 1 } ?: return
        val getIconNotification = iconType.getMethod("getNotification")
        val getVisibleState = iconType.getMethod("getVisibleState")
        val allEntries = pipelineType.getMethod("getAllNotifs")
        val entryByKey = pipelineType.getMethod("getEntry", String::class.java)
        val addCollectionListener = pipelineType.getMethod("addCollectionListener", collectionListener)
        val getSbn = entryType.getMethod("getSbn")
        val getRow = entryType.getMethod("getRow")
        val getRanking = entryType.getMethod("getRanking")
        val suppresses = entryType.getMethod("shouldSuppressVisualEffect", Int::class.javaPrimitiveType)
        val statusField = centralType.getField("mStatusBarStateController")
        val shadeField = centralType.getField("mShadeExpansionStateManager")
        val userField = centralType.getField("mUserTracker")
        val queueField = centralType.getField("mCommandQueue")
        val getState = statusType.getMethod("getCurrentOrUpcomingState")
        val isDozing = statusType.getMethod("isDozing")
        val addStateListener = statusType.getMethod("addCallback", stateListener)
        val addExpansionListener = shadeType.getMethod("addExpansionListener", expansionListener)
        val getExpanded = expansionEvent.getMethod("getExpanded")
        val getFraction = expansionEvent.getMethod("getFraction")
        val getTracking = expansionEvent.getMethod("getTracking")
        val getUser = userType.getMethod("getUserHandle")
        val addUserCallback = userType.getMethod("addCallback", userCallback, Executor::class.java)
        val openShade = commandType.getMethod("animateExpandNotificationsPanel")
        val viewField = controllerType.superclass?.getDeclaredField("mView")?.apply { isAccessible = true } ?: return
        val dispatcherField = controllerType.getDeclaredField("darkIconDispatcher").apply { isAccessible = true }
        val addDarkReceiver = dispatcherType.getMethod("addDarkReceiver", receiverType)
        val removeDarkReceiver = dispatcherType.getMethod("removeDarkReceiver", receiverType)
        val getTint = dispatcherType.getMethod("getTint", Collection::class.java, View::class.java, Int::class.javaPrimitiveType)

        val handler = Handler(Looper.getMainLooper())
        val selection = NotificationProgressSelection()
        var pipeline: Any? = null
        var status: Any? = null
        var userTracker: Any? = null
        var queue: Any? = null
        var shadeClosed = false
        var switchingUser = false
        var selectedUser: UserHandle? = null
        var binding: Binding? = null

        fun activeUser() = userTracker?.let { getUser.invoke(it) as? UserHandle }

        fun canShow(chip: NotificationProgressChip): Boolean {
            val controller = status ?: return false
            return !switchingUser && shadeClosed && getState.invoke(controller) == 0 && isDozing.invoke(controller) == false &&
                chip.context.getSystemService(KeyguardManager::class.java)?.isKeyguardLocked == false
        }

        fun taskOf(entry: Any?, user: UserHandle): NotificationProgressTask? {
            if (!entryType.isInstance(entry)) return null
            val sbn = getSbn.invoke(entry) as? StatusBarNotification ?: return null
            if (sbn.user != user || suppresses.invoke(entry, NotificationManager.Policy.SUPPRESSED_EFFECT_STATUS_BAR) != false) {
                return null
            }
            val ranking = getRanking.invoke(entry) as? NotificationListenerService.Ranking ?: return null
            if (ranking.isSuspended || ranking.importance <= NotificationManager.IMPORTANCE_MIN) return null
            return NotificationProgressTask.from(sbn.key, sbn.packageName, sbn.notification)
        }

        val refresh = Runnable {
            val current = binding ?: return@Runnable
            runCatching {
                if (!canShow(current.chip)) {
                    current.clearProgress()
                    return@runCatching
                }
                val user = activeUser()
                if (selectedUser != user) {
                    selection.update(emptyList())
                    selectedUser = user
                }
                val entries = if (user == null) null else pipeline?.let { allEntries.invoke(it) as? Collection<*> }
                val tasks = if (entries == null || user == null) emptyList() else
                    entries.mapNotNull { runCatching { taskOf(it, user) }.getOrNull() }
                val previous = current.tasks
                current.tasks = selection.update(tasks)
                if (current.tasks.isEmpty()) {
                    current.clearProgress()
                } else {
                    current.presentProgress(previous.map { it.key } != current.tasks.map { it.key })
                }
            }.onFailure { current.clearProgress() }
        }

        fun refreshLater() {
            if (!handler.hasCallbacks(refresh)) handler.post(refresh)
        }

        fun updateShade(event: Any?) {
            if (!expansionEvent.isInstance(event)) return
            val closed = getExpanded.invoke(event) == false && getTracking.invoke(event) == false &&
                (getFraction.invoke(event) as? Float) == 0f
            if (shadeClosed != closed) {
                shadeClosed = closed
                refreshLater()
            }
        }

        pipelineInit.hook {
            after {
                val instance = instanceOrNull<Any>() ?: return@after
                runCatching {
                    if (pipeline === instance) return@runCatching
                    addCollectionListener.invoke(instance, callback(collectionListener) { method, _ ->
                        when (method) {
                            "onEntryAdded", "onEntryUpdated", "onEntryRemoved", "onRankingApplied" -> refreshLater()
                        }
                    })
                    pipeline = instance
                    refreshLater()
                }
            }
        }

        start.hook {
            after {
                val instance = instanceOrNull<Any>() ?: return@after
                runCatching {
                    if (status != null) return@runCatching
                    val barState = statusField.get(instance) ?: return@runCatching
                    val shade = shadeField.get(instance) ?: return@runCatching
                    val tracker = userField.get(instance) ?: return@runCatching
                    addStateListener.invoke(barState, callback(stateListener) { method, _ ->
                        when (method) {
                            "onUpcomingStateChanged", "onStateChanged", "onStatePostChange",
                            "onDozingChanged", "onExpandedChanged" -> refreshLater()
                        }
                    })
                    updateShade(addExpansionListener.invoke(shade, callback(expansionListener) { _, args ->
                        runCatching { updateShade(args?.firstOrNull()) }
                    }))
                    userListener = callback(userCallback) { method, args ->
                        when (method) {
                            "onBeforeUserSwitching", "onUserChanging" -> {
                                try {
                                    switchingUser = true
                                    binding?.clearProgress()
                                } finally {
                                    // A user switch waits for the completion callback.
                                    (args?.lastOrNull() as? Runnable)?.run()
                                }
                            }
                            "onUserChanged" -> {
                                switchingUser = false
                                refreshLater()
                            }
                        }
                    }
                    addUserCallback.invoke(tracker, userListener, Executor { handler.post(it) })
                    queue = queueField.get(instance)
                    status = barState
                    userTracker = tracker
                    refreshLater()
                }
            }
        }

        fun clearBinding() {
            val old = binding ?: return
            binding = null
            old.clearProgress()
            runCatching { removeDarkReceiver.invoke(old.dispatcher, old.receiver) }
        }

        measureContainer.hook {
            before {
                val current = binding ?: return@before
                val container = instanceOrNull<ViewGroup>() ?: return@before
                if (current.tasks.isEmpty() || current.container() !== container) return@before
                runCatching {
                    val user = selectedUser ?: return@runCatching current.slot.update(null)
                    val app = current.tasks.first().packageName
                    val candidates = (0 until container.childCount).mapNotNull { index ->
                        val child = container.getChildAt(index) ?: return@mapNotNull null
                        if (!iconType.isInstance(child) || child.visibility != View.VISIBLE) return@mapNotNull null
                        val sbn = getIconNotification.invoke(child) as? StatusBarNotification ?: return@mapNotNull null
                        if (sbn.user == user && sbn.packageName == app) child to sbn.key else null
                    }
                    // Some regions show one icon per app. In that case the stock representative can have a different key.
                    val taskKeys = current.tasks.mapTo(hashSetOf()) { it.key }
                    val exact = candidates.filter { it.second in taskKeys }
                    val host = (exact.firstOrNull() ?: candidates.singleOrNull())?.first
                        ?: return@runCatching current.slot.update(null)
                    val widthSpec = args[0] as Int
                    val heightSpec = args[1] as Int
                    val fitWidth = if (MeasureSpec.getMode(widthSpec) == MeasureSpec.UNSPECIFIED) widthSpec
                        else MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthSpec), MeasureSpec.AT_MOST)
                    val height = host.layoutParams?.height?.takeIf { it > 0 } ?: MeasureSpec.getSize(heightSpec)
                    val chip = current.chip
                    chip.layoutDirection = host.layoutDirection
                    chip.measure(fitWidth, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY))
                    if (chip.measuredWidth == 0) return@runCatching current.slot.update(null)
                    chip.layout(0, 0, chip.measuredWidth, height)
                    val shownKeys = current.tasks.take(chip.measuredBars).mapTo(hashSetOf()) { it.key }
                    current.slot.update(host, chip.measuredWidth, exact.mapNotNullTo(hashSetOf()) { (view, key) ->
                        view.takeIf { it !== host && key in shownKeys }
                    })
                    current.slot.describe(chip.contentDescription)
                }.onFailure { current.slot.update(null) }
            }
        }

        configureContainer.hook {
            after {
                val current = binding ?: return@after
                if (current.container() !== instanceOrNull<ViewGroup>()) return@after
                val config = args[0] as? Configuration ?: return@after
                current.chip.dispatchConfigurationChanged(config)
                current.presentProgress(remeasure = true)
            }
        }

        calculateIconPositions.hook {
            before {
                val current = binding ?: return@before
                val container = instanceOrNull<ViewGroup>() ?: return@before
                val first = container.getChildAt(0) ?: return@before
                if (current.slot.widthFor(first) == null || container !== current.container()) return@before
                // The container copies its first child's width into the stock overflow calculation.
                val width = first.layoutParams?.width?.takeIf { it > 0 } ?: return@before
                nominalIconSize.setInt(container, width)
            }
        }

        measureIcon.hook {
            before {
                val current = binding ?: return@before
                val view = instanceOrNull<View>() ?: return@before
                val width = current.slot.widthFor(view) ?: return@before
                if (view.parent !== current.container()) return@before
                args[0] = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
            }
        }

        drawIcon.hook {
            before {
                val current = binding ?: return@before
                val view = instanceOrNull<View>() ?: return@before
                val width = current.slot.widthFor(view) ?: return@before
                if (view.parent !== current.container()) return@before
                if (width == 0) {
                    result = null
                    return@before
                }
                val canvas = args[0] as? Canvas ?: return@before
                runCatching {
                    val chip = current.chip
                    // Leave the native overflow dot and hidden states to System UI.
                    if (getVisibleState.invoke(view) != 0 || view.width != chip.width || view.height != chip.height) {
                        return@runCatching
                    }
                    val tint = getTint.invoke(null, current.darkAreas, view, current.darkColor) as? Int ?: current.darkColor
                    chip.applyTint(tint)
                    chip.drawInSlot(canvas, view)
                    result = null
                }
            }
        }

        attach.hook {
            after {
                val instance = instanceOrNull<Any>() ?: return@after
                runCatching {
                    val root = viewField.get(instance) as? View ?: return@runCatching
                    if (root.display?.displayId != Display.DEFAULT_DISPLAY) return@runCatching
                    val iconAreaId = root.resources.getIdentifier("notification_icon_area", "id", SYSTEM_UI)
                    val containerId = root.resources.getIdentifier("notificationIcons", "id", SYSTEM_UI)
                    if (iconAreaId == 0 || containerId == 0) return@runCatching
                    val iconArea = root.findViewById<View>(iconAreaId) ?: return@runCatching
                    val dispatcher = dispatcherField.get(instance) ?: return@runCatching
                    clearBinding()
                    val chip = NotificationProgressChip(root.context)
                    val receiver = callback(receiverType) { method, args ->
                        if (method == "onDarkChanged") {
                            val current = binding?.takeIf { it.controller === instance }
                            if (current != null) {
                                current.darkAreas = args?.getOrNull(0) as? Collection<*> ?: emptyList<Any?>()
                                current.darkColor = args?.getOrNull(2) as? Int ?: current.darkColor
                                current.slot.host?.invalidate()
                            }
                        }
                    }
                    val current = Binding(instance, chip, dispatcher, receiver, iconArea, containerId)
                    current.slot.onClick = { view ->
                        runCatching {
                            if (binding !== current || view !== current.slot.host || !view.isShown ||
                                view.parent !== current.container() || getVisibleState.invoke(view) != 0 ||
                                view.width != chip.width || view.height != chip.height || !canShow(chip)
                            ) return@runCatching
                            val user = activeUser()
                            if (user == null || user != selectedUser) return@runCatching
                            val task = current.tasks.singleOrNull()
                            if (task != null) {
                                val live = pipeline?.let { entryByKey.invoke(it, task.key) }
                                if (taskOf(live, user) != null) {
                                    val row = getRow.invoke(live) as? View
                                    if (row?.isEnabled == true && row.hasOnClickListeners() && row.performClick()) {
                                        return@runCatching
                                    }
                                }
                            }
                            queue?.let { openShade.invoke(it) }
                        }
                    }
                    binding = current
                    addDarkReceiver.invoke(dispatcher, receiver)
                    refreshLater()
                }.onFailure {
                    if (binding?.controller === instance) clearBinding()
                    YLog.warn("Notification progress chip could not attach: ${it.message}")
                }
            }
        }
        detach.hook {
            after {
                if (binding?.controller === instanceOrNull<Any>()) clearBinding()
            }
        }
    }

    private fun callback(type: Class<*>, onCall: (String, Array<out Any?>?) -> Unit): Any =
        Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { proxy, method, args ->
            when (method.name) {
                "equals" -> proxy === args?.firstOrNull()
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "LuckyTool ${type.simpleName}"
                else -> {
                    onCall(method.name, args)
                    null
                }
            }
        }

    private class Binding(
        val controller: Any,
        val chip: NotificationProgressChip,
        val dispatcher: Any,
        val receiver: Any,
        private val iconArea: View,
        private val containerId: Int,
    ) {
        var tasks = emptyList<NotificationProgressTask>()
        val slot = NotificationProgressSlot()
        var darkAreas: Collection<*> = emptyList<Any?>()
        var darkColor = Color.WHITE
        private var labeledPackage: String? = null
        private var label: CharSequence = ""

        fun container(): ViewGroup? = iconArea.findViewById(containerId)

        fun presentProgress(remeasure: Boolean) {
            if (tasks.isEmpty()) return
            chip.show(tasks, description())
            if (remeasure) container()?.requestLayout()
            slot.describe(chip.contentDescription)
            slot.host?.invalidate()
        }

        fun clearProgress() {
            tasks = emptyList()
            if (slot.host == null) return
            slot.update(null)
            container()?.requestLayout()
        }

        fun description(): CharSequence {
            val current = tasks.firstOrNull() ?: return ""
            val context = chip.context
            if (labeledPackage != current.packageName) {
                labeledPackage = current.packageName
                label = runCatching {
                    val packages = context.packageManager
                    packages.getApplicationLabel(packages.getApplicationInfo(current.packageName, 0))
                }.getOrDefault(current.packageName)
            }
            return runCatching {
                val resources = context.packageManager.getResourcesForApplication(BuildConfig.APPLICATION_ID)
                val fraction = current.fraction
                when {
                    tasks.size > 1 -> resources.getString(R.string.notification_progress_count, label, tasks.size)
                    fraction == null -> resources.getString(R.string.notification_progress_indeterminate, label)
                    else -> resources.getString(R.string.notification_progress_value, label, (fraction * 100).toInt())
                }
            }.getOrDefault(label.toString())
        }
    }

    private const val SYSTEM_UI = "com.android.systemui"
}

/** Keep the native icon views in the container so System UI retains ownership of their order. */
internal class NotificationProgressSlot {
    var host: View? = null
        private set
    var onClick: (View) -> Unit = {}
    private var width = 0
    private var folded = emptySet<View>()
    private var releaseClick: (() -> Unit)? = null
    private var stockDescription: CharSequence? = null

    fun describe(description: CharSequence?) {
        val current = host ?: return
        if (current.stateDescription != description) current.stateDescription = description
    }

    fun widthFor(view: View): Int? = when {
        view === host -> width
        view in folded -> 0
        else -> null
    }

    fun update(next: View?, nextWidth: Int = 0, nextFolded: Set<View> = emptySet()) {
        if (host === next && width == nextWidth && folded == nextFolded) return
        val changed = folded + nextFolded + listOfNotNull(host, next)
        if (host !== next) {
            host?.stateDescription = stockDescription
            stockDescription = next?.stateDescription
            releaseClick?.invoke()
            releaseClick = null
            if (next != null && !next.hasOnClickListeners()) {
                val clickable = next.isClickable
                val focusable = next.focusable
                next.setOnClickListener { onClick(it) }
                releaseClick = {
                    next.setOnClickListener(null)
                    next.isClickable = clickable
                    next.focusable = focusable
                }
            }
        }
        host = next
        width = nextWidth
        folded = nextFolded
        // Re-measure with the natural width when a view leaves the replacement slot.
        changed.forEach { view -> view.forceLayout(); view.invalidate() }
    }
}
