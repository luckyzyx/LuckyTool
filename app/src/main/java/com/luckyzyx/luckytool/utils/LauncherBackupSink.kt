package com.luckyzyx.luckytool.utils

import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Process
import android.os.UserHandle
import android.provider.MediaStore
import com.highcapable.yukihookapi.hook.log.YLog
import java.io.File

/**
 * 宿主（launcher）进程侧的自动备份落盘：按顺序试多种写法，用第一份真正落盘的。
 *
 * 为什么不是一个写法：真机上 MediaStore 投稿报 `Unknown URL content://media/external_primary/downloads`
 * ——ContentResolver 在 acquireProvider 返回 null 时才抛这句，跟 uri 怎么拼无关，是宿主进程拿不到
 * media provider；而裸文件写公共 Download 又是 Permission denied（launcher uid 没有
 * MANAGE_EXTERNAL_STORAGE / WRITE_EXTERNAL_STORAGE）。加重试解决不了「写法本身不通」，
 * 所以这里改成换写法，并把每种写法的失败原文留在日志里：真机一条日志就能看出不通在哪一层。
 *
 * 写法顺序按「用户能不能直接在文件管理器里看到」排：
 *  1. MediaStore Downloads（外部主卷 / 老式外部卷 / Files 集合）；
 *  2. 宿主自己的 `Android/media/<pkg>/launcher_backup`（不需要任何存储权限，文件管理器可见，
 *     app 侧列快照时会把它搬回正式备份目录，见 LauncherBackupTransfer.adoptHostFallback）；
 *  3. 宿主的 `Android/data/<pkg>/files`（最后兜底，一定写得进去；Android 11+ 别的应用读不到，只能靠日志定位）。
 */
object LauncherBackupSink {

    private const val TAG = "LauncherBackupSink"

    /**
     * 投稿时声明的 MIME 不能写 application/zip：媒体库建文件走 FileUtils.splitFileName，
     * 名字里的扩展名与该 MIME 注册的扩展名（zip）对不上时会被替换成 MIME 的扩展名，
     * 于是 <快照>.ltbackup 落盘变成 <快照>.zip，而 app 侧只认 .ltbackup，这份自动备份就列不出来。
     * 换一个没注册扩展名的 MIME，媒体库才保留原文件名。
     */
    private const val MIME_TYPE = "application/x-luckytool-backup"

    /** 兜底目录里的子目录名：与备份目录最后一段同名，app 侧才认得出哪些文件要搬回去 */
    private const val SNAPSHOT_DIR = LauncherBackupUtils.MEDIA_SUBDIR

    /** 宿主侧 media provider 的模块包名候选（AOSP 走 module，老版本走独立 apk） */
    private val MEDIA_PROVIDER_PACKAGES = listOf(
        "com.google.android.providers.media.module",
        "com.android.providers.media.module",
        "com.android.providers.media",
    )

    /**
     * 一次成功落盘的位置。
     * [collection] 非空表示走 MediaStore（保留策略按行删），[file] 非空表示走文件目录（按文件删）。
     */
    data class Placement(
        val where: String = "",
        val uri: Uri? = null,
        val collection: Uri? = null,
        val file: File? = null,
    )

    /**
     * 写一份自动备份，返回真正落盘的位置。
     * 所有写法都失败时抛 [IllegalStateException]，message 是每种写法的失败原文拼接。
     */
    fun write(context: Context, displayName: String, bytes: ByteArray): Placement {
        logEnvironment(context)
        val failures = LinkedHashMap<String, String>()
        for ((where, attempt) in strategies(context, displayName, bytes)) {
            val placement = runCatching { attempt() }.getOrElse { throwable ->
                val reason = throwable.message ?: throwable.javaClass.simpleName
                failures[where] = throwable.javaClass.simpleName + ": " + reason
                LogUtils.d(TAG, "write", "写法 $where 失败: $reason")
                YLog.debug("$TAG 写法 $where 落盘失败", throwable)
                null
            } ?: continue
            LogUtils.d(
                TAG, "write",
                "落盘成功，写法 " + where + " -> " + (placement.uri ?: placement.file),
            )
            return placement.copy(where = where)
        }
        throw IllegalStateException(
            failures.entries.joinToString("；") { entry -> entry.key + "=" + entry.value }
        )
    }

    /** 拟尝试的写法：名字用于日志，lambda 成功时返回落盘位置、失败时抛异常 */
    @Suppress("DEPRECATION")
    private fun strategies(
        context: Context,
        displayName: String,
        bytes: ByteArray,
    ): List<Pair<String, () -> Placement>> {
        val resolver = context.contentResolver
        val primaryDownloads = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        @Suppress("DEPRECATION")
        val legacyDownloads = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        val primaryFiles = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        return buildList {
            add("mediaStore:downloads:external_primary" to {
                mediaStorePlacement(resolver, primaryDownloads, displayName, bytes)
            })
            add("mediaStore:downloads:external" to {
                mediaStorePlacement(resolver, legacyDownloads, displayName, bytes)
            })
            add("mediaStore:files:external_primary" to {
                mediaStorePlacement(resolver, primaryFiles, displayName, bytes)
            })
            add("file:external_media" to {
                filePlacement(context.externalMediaDirs?.firstOrNull(), displayName, bytes)
            })
            add("file:external_files" to {
                filePlacement(context.getExternalFilesDir(null), displayName, bytes)
            })
        }
    }

    /** MediaStore 投稿一份：先挂 IS_PENDING，写完再发布，回读大小确认真的落盘 */
    private fun mediaStorePlacement(
        resolver: ContentResolver,
        collection: Uri,
        displayName: String,
        bytes: ByteArray,
    ): Placement {
        val pending = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            //RELATIVE_PATH 必须以 / 结尾
            put(MediaStore.MediaColumns.RELATIVE_PATH, LauncherBackupUtils.RELATIVE_DIR + "/")
            put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val target = resolver.insert(collection, pending) ?: error("insert 返回 null")
        runCatching {
            resolver.openOutputStream(target)?.use { output -> output.write(bytes) }
                ?: error("openOutputStream 返回 null")
            //写完再解除 pending，否则别的应用会先看到半份文件
            val publish = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(target, publish, null, null)
            val landed = mediaNameAndSize(resolver, target)
            if (landed.second <= 0L) error("写入后回读大小为 0")
            // 名字必须还是我们要的那个：被媒体库改写过的文件 app 侧认不出（只列 .ltbackup），
            // 与其留一份看不见的备份，不如按失败处理，交给下一档按原名落盘
            landed.first?.takeIf { it != displayName }?.let { error("文件名被改写为 " + it) }
        }.onFailure { throwable ->
            //失败的残行用户看不到，但会一直占配额
            runCatching { resolver.delete(target, null, null) }
                .onFailure { YLog.debug("$TAG 清理投稿残行失败: $target", it) }
        }.getOrThrow()
        return Placement(uri = target, collection = collection)
    }

    /** 文件写法：目录不可用或长度对不上一律算失败，交给下一个写法 */
    private fun filePlacement(root: File?, displayName: String, bytes: ByteArray): Placement {
        val base = root ?: error("目录不可用")
        val dir = File(base, SNAPSHOT_DIR)
        if (!dir.isDirectory && !dir.mkdirs()) error("无法创建目录 " + dir.absolutePath)
        val file = File(dir, displayName)
        file.outputStream().use { output -> output.write(bytes) }
        if (file.length() != bytes.size.toLong()) {
            error("写入长度不一致 " + file.length() + "/" + bytes.size)
        }
        return Placement(file = file)
    }

    /** 回读 MediaStore 行的名字与大小；读不到名字返回 null、大小按 0 处理 */
    private fun mediaNameAndSize(resolver: ContentResolver, uri: Uri): Pair<String?, Long> = runCatching {
        resolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.SIZE),
            null, null, null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null to 0L
            val name = if (cursor.isNull(0)) null else cursor.getString(0)
            val size = if (cursor.isNull(1)) 0L else cursor.getLong(1)
            name to size
        } ?: (null to 0L)
    }.getOrDefault(null to 0L)

    private var environmentLogged = false

    /**
     * 宿主进程的落盘环境，每个进程只记一次：
     * media provider 到底对桌面进程可不可见、运行在哪个 uid / 用户下、外部存储是什么状态。
     * 这三种信息决定了「换哪种写法能成」，真机日志里有这一条就不用再猜。
     */
    private fun logEnvironment(context: Context) {
        if (environmentLogged) return
        environmentLogged = true
        runCatching {
            val manager = context.packageManager
            val mediaProvider = runCatching {
                manager.resolveContentProvider(
                    MediaStore.AUTHORITY,
                    PackageManager.MATCH_ALL or PackageManager.GET_META_DATA,
                )
            }.getOrNull()
            val mediaModule = MEDIA_PROVIDER_PACKAGES.firstNotNullOfOrNull { pkg ->
                runCatching { manager.getPackageInfo(pkg, 0).packageName }.getOrNull()
            } ?: "unresolved"
            LogUtils.d(
                TAG, "logEnvironment",
                "uid=" + Process.myUid() +
                    " user=" + UserHandle.getUserHandleForUid(Process.myUid()) +
                    " pkg=" + context.packageName +
                    " process=" + processName() +
                    " mediaProvider=" + (mediaProvider?.packageName ?: "null") +
                    " mediaModule=" + mediaModule +
                    " storageState=" + Environment.getExternalStorageState(),
            )
        }.onFailure { YLog.debug("$TAG 落盘环境探测失败", it) }
    }

    private fun processName(): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        runCatching { Application.getProcessName() }.getOrDefault("?")
    } else "?"
}
