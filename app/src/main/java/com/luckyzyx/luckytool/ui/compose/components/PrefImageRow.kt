package com.luckyzyx.luckytool.ui.compose.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

/**
 * 行尾缩略图的默认行高：图片与视频共用，改这一个数两行一起变；
 * 需要单独调整时在调用点传 [PrefImageRow] / [PrefVideoRow] 的 `previewHeight`。
 */
private val DefaultPreviewHeight = 40.dp

/**
 * 图片路径预览缩略图：把本地绝对路径解码成一张圆角缩略图，用在 [PrefRow] 的 trailing 槽位（行尾）；
 * 行首留给条目标题/图标。
 *
 * 高度固定为 [height]、宽度按原图比例算，所以同一行既能显示 1.4:1 的宽图，也能显示 2.65:1 的窄图，
 * 都不会把行高撑起来。路径为空或解码失败时什么都不画（槽位宽度为 0，行退化成纯文本行）。
 * 解码按行高降采样（inSampleSize），不把原图整张读进内存再缩放。
 */
@Composable
fun PrefImagePreview(
    path: String,
    modifier: Modifier = Modifier,
    height: Dp = DefaultPreviewHeight,
    shape: Shape = RoundedCornerShape(6.dp),
) {
    val targetPx = with(LocalDensity.current) { height.roundToPx() }.coerceAtLeast(1)
    val bitmap = previewImage(path, targetPx).value ?: return
    Box(
        modifier = modifier
            .height(height)
            .aspectRatio(bitmap.width.toFloat() / bitmap.height)
            .clip(shape)
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * 图片路径条目：整行可点，行尾是 [PrefImagePreview] 缩略图，摘要默认显示路径（空路径显示 `Null`）。
 *
 * 与颜色预览色块（[PrefColorPreview]）同一定位：`custom()` 逃生舱里的行渲染原语，只管「标题 + 预览 + 点击」；
 * 选图 / 裁剪这些依赖 Activity Result launcher 的逻辑留在页面侧，本文件不引入 activity 依赖。
 */
@Composable
fun PrefImageRow(
    title: String,
    path: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    previewHeight: Dp = DefaultPreviewHeight,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    PrefRowInset(modifier) {
        PrefRow(
            title = title,
            summary = summary ?: path.ifBlank { "Null" },
            enabled = enabled,
            trailing = { PrefImagePreview(path = path, height = previewHeight) },
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}

/** 缩略图解码结果缓存：滑出/滑回视口、或行重组都不会重新读盘解码。24 张 40dp 缩略图的量级。 */
private val previewCache = LruCache<String, ImageBitmap>(24)

/**
 * 缩略图的解码状态。
 *
 * 命中缓存时首帧就拿到位图；未命中时在 [Dispatchers.IO] 上解码，主线程只做一次 map 查询。
 * 原先用 remember { decodePreviewImage(...) } 在组合期同步解码（一次 bounds 读 + 一次整图解码，
 * PNG 还是整张解出来再降采样），行第一次进入视口的那一帧被这段读盘+解码占住 ——
 * 快速滑动滑到自定义 OTA 卡片时的那一下停顿就来自这里。
 */
@Composable
private fun previewImage(path: String, targetPx: Int): State<ImageBitmap?> {
    val key = if (path.isBlank()) null else "$targetPx@$path"
    val cached = remember(key) { key?.let { previewCache.get(it) } }
    return produceState<ImageBitmap?>(initialValue = cached, path, targetPx) {
        if (key == null) {
            value = null
            return@produceState
        }
        value = previewCache.get(key)
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) { decodePreviewImage(path, targetPx) }
            ?.also { previewCache.put(key, it) }
    }
}

/** 按目标高度降采样解码本地图片；读不到或不是图片时返回 null（不抛异常、不打日志）。 */
private fun decodePreviewImage(path: String, targetPx: Int): ImageBitmap? {
    if (path.isBlank()) return null
    return runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            null
        } else {
            var sample = 1
            while (bounds.outHeight / (sample * 2) >= targetPx) sample *= 2
            BitmapFactory.decodeFile(
                path,
                BitmapFactory.Options().apply { inSampleSize = sample },
            )?.asImageBitmap()
        }
    }.getOrNull()
}

/**
 * 视频路径预览缩略图：取第一帧画成圆角缩略图，中间叠一个播放标记，让这一行一眼能看出是视频而不是图片。
 *
 * 尺寸策略与 [PrefImagePreview] 一致：高度取 [height]（默认与图片预览同为 [DefaultPreviewHeight]，需要时可从调用点传大），
 * 宽度按视频（算上 rotation 元数据后）的比例算，
 * 路径为空或取帧失败时什么都不画（槽位宽度为 0，行退化成纯文本行）。
 */
@Composable
fun PrefVideoPreview(
    path: String,
    modifier: Modifier = Modifier,
    height: Dp = DefaultPreviewHeight,
    shape: Shape = RoundedCornerShape(6.dp),
) {
    val targetPx = with(LocalDensity.current) { height.roundToPx() }.coerceAtLeast(1)
    val bitmap = previewVideoFrame(path, targetPx).value ?: return
    Box(
        modifier = modifier
            .height(height)
            .aspectRatio(bitmap.width.toFloat() / bitmap.height)
            .clip(shape)
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.fillMaxSize(0.6f),
            )
        }
    }
}

/**
 * 视频路径条目：整行可点，行尾是 [PrefVideoPreview] 首帧缩略图，摘要默认显示路径（空路径显示 `Null`）。
 *
 * 选文件依赖 Activity Result launcher，和 [PrefImageRow] 一样留在页面侧。
 */
@Composable
fun PrefVideoRow(
    title: String,
    path: String,
    modifier: Modifier = Modifier,
    summary: String? = null,
    enabled: Boolean = true,
    previewHeight: Dp = DefaultPreviewHeight,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    PrefRowInset(modifier) {
        PrefRow(
            title = title,
            summary = summary ?: path.ifBlank { "Null" },
            enabled = enabled,
            trailing = { PrefVideoPreview(path = path, height = previewHeight) },
            onClick = onClick,
            onLongClick = onLongClick,
        )
    }
}

/** 视频首帧的解码状态，语义与 [previewImage] 相同：命中缓存首帧就是位图，未命中在 IO 线程取帧。 */
@Composable
private fun previewVideoFrame(path: String, targetPx: Int): State<ImageBitmap?> {
    val key = if (path.isBlank()) null else "video:$targetPx@$path"
    val cached = remember(key) { key?.let { previewCache.get(it) } }
    return produceState<ImageBitmap?>(initialValue = cached, path, targetPx) {
        if (key == null) {
            value = null
            return@produceState
        }
        value = previewCache.get(key)
        if (value != null) return@produceState
        value = withContext(Dispatchers.IO) { decodeVideoFrame(path, targetPx) }
            ?.also { previewCache.put(key, it) }
    }
}

/**
 * 取视频第一帧并按目标高度缩放；读不到、不是视频时返回 null（不抛异常、不打日志）。
 *
 * 用 [MediaMetadataRetriever.getScaledFrameAtTime] 直接按目标尺寸解码，不把整张关键帧读进内存；
 * 手机竖拍的视频帧带着 rotation 元数据，缩放的基准是「旋转后的高度」，取回来再转正，
 * 否则竖拍视频会以高为宽、把整行撑起来。
 */
private fun decodeVideoFrame(path: String, targetPx: Int): ImageBitmap? {
    if (path.isBlank()) return null
    val retriever = MediaMetadataRetriever()
    return try {
        runCatching {
            retriever.setDataSource(path)
            val rotation = normalizeRotation(
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)
                    ?.toIntOrNull() ?: 0
            )
            val quarterTurn = rotation == 90 || rotation == 270
            val rawWidth = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
                ?.toIntOrNull() ?: 0
            val rawHeight =
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
                    ?.toIntOrNull() ?: 0
            val uprightHeight = if (quarterTurn) rawWidth else rawHeight
            if (rawWidth <= 0 || rawHeight <= 0 || uprightHeight <= 0) {
                null
            } else {
                val scale = targetPx.toFloat() / uprightHeight
                val frame = retriever.getScaledFrameAtTime(
                    0L,
                    MediaMetadataRetriever.OPTION_CLOSEST_SYNC,
                    (rawWidth * scale).roundToInt().coerceAtLeast(1),
                    (rawHeight * scale).roundToInt().coerceAtLeast(1),
                )
                frame?.let { if (quarterTurn) it.rotated(rotation) else it }?.asImageBitmap()
            }
        }.getOrNull()
    } finally {
        runCatching { retriever.release() }
    }
}

/** 把 rotation 元数据归一化到 [0, 360)。 */
private fun normalizeRotation(rotation: Int): Int = ((rotation % 360) + 360) % 360

/** 按 rotation 元数据把帧转正。 */
private fun Bitmap.rotated(degrees: Int): Bitmap {
    if (degrees % 360 == 0) return this
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
