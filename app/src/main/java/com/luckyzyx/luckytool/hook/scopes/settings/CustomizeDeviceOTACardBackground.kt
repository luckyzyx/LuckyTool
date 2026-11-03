package com.luckyzyx.luckytool.hook.scopes.settings

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaPlayer
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.TextureView
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
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.dp
import com.luckyzyx.luckytool.utils.getOSVersionCode
import com.luckyzyx.luckytool.utils.safeOfNull
import org.lsposed.lsparanoid.Obfuscate
import java.io.File
import java.lang.ref.WeakReference

@Obfuscate
object CustomizeDeviceOTACardBackground : YukiBaseHooker() {

    //拦截 MediaPlayer.setOnCompletionListener 时，内部还要再调一次真注册，用它挡住自递归
    private var installingCompletionListener = false

    private var backgroundPath = ""
    private var videoPath = ""

    //解码大图很贵，按路径与文件修改时间缓存，避免每次 bind 都在主线程重复解码
    private var cachedBitmapPath = ""
    private var cachedBitmapModifiedTime = 0L
    private var cachedBitmap: Bitmap? = null

    //官方视频/彩蛋的回调里拿不到 itemView，这里记住最近一次 bind 出来的顶部视图
    private var topBgView: WeakReference<ImageView>? = null
    private var topMaskView: WeakReference<ImageView>? = null
    //视频播完的收尾要在监听器回调里调官方方法，回调里拿不到 preference 实例，bind 时记下来
    private var topPreference: WeakReference<Any>? = null

    //文件可能在 hook 加载之后被删掉，每次要用路径前都现查一遍
    private fun hasPlayableVideo(): Boolean = videoPath.isNotEmpty() && File(videoPath).exists()

    @SuppressLint("DiscouragedApi")
    override fun onHook() {
        val osCode = getOSVersionCode
        backgroundPath = preferences(ModulePrefs)
            .getString("customize_device_ota_card_background_path", "")
        //路径只是「上次选过什么」，是否生效由视频开关决定：关着就当没设过，否则关开关后仍会播旧视频
        val videoEnabled = preferences(ModulePrefs)
            .getBoolean("customize_device_ota_card_video", false)
        //这里只取路径原文，文件是否还在交给 hasPlayableVideo() 在使用时现查，
        //否则加载之后文件被删，内存里的死路径还是会交给官方
        videoPath = if (videoEnabled) {
            preferences(ModulePrefs).getString("customize_device_ota_card_video_path", "")
        } else ""
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
                            //引用要先记下来再决定要不要画图：没设背景图时下面会提前 return，
                            //但视频播完的收尾还要靠这些引用调官方实现
                            val bgView = itemView.findViewById<ImageView>(topId)
                            val maskView = itemView.findViewById<ImageView>(maskId)
                            topPreference = WeakReference(instance<Any>())
                            if (bgView != null) topBgView = WeakReference(bgView)
                            if (maskView != null) topMaskView = WeakReference(maskView)
                            //官方把查到的路径缓存在 mCachedVideoPath 字段，文件删了以后这个旧值还会被
                            //onSurfaceTextureAvailable 拿去 setDataSource 抛 IOException，先清掉免得崩
                            if (videoPath.isNotEmpty() && !hasPlayableVideo()) {
                                runCatching {
                                    firstFieldOrNull { name = "mCachedVideoPath" }
                                        ?.of(instance<Any>())?.set(null)
                                }
                            }

                            val bitmap = loadBackgroundBitmap(backgroundPath) ?: return@after
                            val drawable = RoundedBitmapDrawableFactory.create(
                                itemView.resources, bitmap
                            )
                            drawable.cornerRadius = 12F.dp
                            bgView?.apply {
                                setImageDrawable(drawable)
                                visibility = View.VISIBLE
                            }
                            //遮罩是压在背景上的半透明纱，用自定义图时直接隐藏
                            maskView?.apply {
                                setImageDrawable(null)
                                visibility = View.GONE
                            }

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

                //官方在彩蛋/视频收敛时会把封面写回 bg，这三个方法都是 void 且自己写视图，
                //所以用拦截器表达 after 语义：先跑官方实现，再把自定义图补回去
                firstMethodOrNull { name = "getTopDrawableBg" }?.intercept {
                    proceed()
                    //此处官方可能刚把 bg 置 GONE 去露彩蛋视频，可见性按现状判断，不能硬顶上来
                    restoreTopBackground(false)
                    null
                }
                val showTopVideoLastFrame = firstMethodOrNull { name = "showTopVideoLastFrame" }
                showTopVideoLastFrame?.intercept {
                    proceed()
                    restoreTopBackground(true)
                    null
                }
                firstMethodOrNull { name = "showEasterEggVideo" }?.intercept {
                    //show=true 是刚露出彩蛋视频，show=false 才是收回静态背景
                    val show = arg<Boolean>(0) ?: false
                    proceed()
                    restoreTopBackground(!show)
                    null
                }
                if (videoEnabled) {
                    //开关开着就没有官方视频的位置：有自定义文件时放行官方流程，没有（或文件已被删）
                    //就挡掉，onBindViewHolder 于是改走 showStaticTopBackground 分支，
                    //正好落到上面 getTopDrawableBg 的收尾里，背景图照常补回去
                    firstMethodOrNull { name = "isSupportTopVideo" }?.intercept {
                        if (hasPlayableVideo()) proceed() else false
                    }
                }
                if (backgroundPath.isNotEmpty() || videoPath.isNotEmpty()) {
                    //播放结束官方只把状态置 COMPLETED、不动视图，收敛时机拿到官方回调里最稳；
                    //而官方的 OnCompletionListener 是 R8 生成的匿名类，类名不可靠，
                    //所以拦框架注册点 MediaPlayer.setOnCompletionListener，用组合监听器包住官方回调
                    "android.media.MediaPlayer".toClass().resolve().apply {
                        firstMethod {
                            name = "setOnCompletionListener"
                            parameterCount = 1
                        }.intercept {
                            val official = arg<MediaPlayer.OnCompletionListener>(0)
                            //官方的 clearListenersQuietly 会注册 null，直接放过
                            if (official == null) return@intercept proceed()
                            //替换注册要再调一次真方法，重入时直通，别自己包自己
                            if (installingCompletionListener) return@intercept proceed()
                            val mediaPlayer = instance as? MediaPlayer
                                ?: return@intercept proceed()
                            installingCompletionListener = true
                            try {
                                mediaPlayer.setOnCompletionListener { player ->
                                    //先让官方跑（它负责把 mVideoState 置 COMPLETED，后续分支依赖它）
                                    official.onCompletion(player)
                                    //有自定义背景图就换回自定义图；没有就交给官方收敛，免得冻在最后一帧
                                    if (!restoreTopBackground(true)) {
                                        topPreference?.get()?.let { preference ->
                                            runCatching {
                                                showTopVideoLastFrame?.copy()?.of(preference)
                                                    ?.invoke<Any>()
                                            }
                                        }
                                    }
                                }
                            } finally {
                                installingCompletionListener = false
                            }
                            null
                        }
                    }
                }
                if (videoEnabled) {
                    //原实现会顺带置位 mTopVideoIsWithMask，这里直接给路径并跳过原实现；
                    //文件已经没了就返回 null，让官方走「路径为空」的 showTopVideoLastFrame 分支，
                    //死路径绝不能进到 MediaPlayer.setDataSource（那里只 catch IllegalStateException）
                    firstMethodOrNull { name = "getColorOSVideoPath" }?.intercept {
                        if (hasPlayableVideo()) videoPath else null
                    }
                    //官方在 onPrepared 里 start() 之前调用它，正好用来静音，并用自己算的 centerCrop 换掉官方 Matrix
                    firstMethodOrNull { name = "applyTopVideoDisplayTransform" }?.intercept {
                        val textureView = arg<TextureView>(0) ?: return@intercept null
                        val player = firstFieldOrNull { name = "mAboutDeviceTopMediaPlayer" }
                            ?.of(instance)?.get<MediaPlayer>()
                        runCatching { player?.setVolume(0F, 0F) }
                        textureView.setTransform(createCenterCropMatrix(textureView, player))
                        null
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

    private fun loadBackgroundBitmap(path: String): Bitmap? {
        if (path.isEmpty()) return null
        val modifiedTime = runCatching { File(path).lastModified() }.getOrDefault(0L)
        if (path == cachedBitmapPath && modifiedTime == cachedBitmapModifiedTime) {
            cachedBitmap?.let { return it }
        }
        val bitmap = runCatching { BitmapFactory.decodeFile(path) }.getOrNull() ?: return null
        cachedBitmapPath = path
        cachedBitmapModifiedTime = modifiedTime
        cachedBitmap = bitmap
        return bitmap
    }

    //forceVisible=false 时只预置图不改可见性；可见时顺手把遮罩藏掉，免得半透明纱压暗自定义图
    //返回值表示是否真的写了自定义图，false 说明没有图可画，调用方可以退回官方收尾
    private fun restoreTopBackground(forceVisible: Boolean): Boolean {
        val bgView = topBgView?.get() ?: return false
        val bitmap = loadBackgroundBitmap(backgroundPath) ?: return false
        val drawable = RoundedBitmapDrawableFactory.create(bgView.resources, bitmap)
        drawable.cornerRadius = 12F.dp
        bgView.setImageDrawable(drawable)
        if (!forceVisible && bgView.visibility != View.VISIBLE) return true
        topMaskView?.get()?.visibility = View.GONE
        bgView.visibility = View.VISIBLE
        return true
    }

    //官方那套 Matrix 只适配官方素材，自定义视频自己按 centerCrop 铺满
    private fun createCenterCropMatrix(textureView: TextureView, player: MediaPlayer?): Matrix {
        val matrix = Matrix()
        val videoWidth = runCatching { player?.videoWidth ?: 0 }.getOrDefault(0)
        val videoHeight = runCatching { player?.videoHeight ?: 0 }.getOrDefault(0)
        val viewWidth = textureView.width
        val viewHeight = textureView.height
        //尺寸还没就绪时用单位矩阵，等同于把视频按 view 宽高铺满
        if (videoWidth <= 0 || videoHeight <= 0 || viewWidth <= 0 || viewHeight <= 0) {
            return matrix
        }
        val scale = maxOf(
            viewWidth.toFloat() / videoWidth, viewHeight.toFloat() / videoHeight
        )
        matrix.setScale(scale, scale)
        matrix.postTranslate(
            (viewWidth - videoWidth * scale) / 2F, (viewHeight - videoHeight * scale) / 2F
        )
        return matrix
    }
}