package com.luckyzyx.luckytool.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.media.MediaScannerConnection
import android.os.Environment
import androidx.core.content.FileProvider
import com.luckyzyx.luckytool.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 桌面布局备份/恢复的共享纯逻辑（app 进程与 launcher 进程的 hook 共用，不碰 shell、不用 root）。
 *
 * 数据流（用户已确认的方案：备份文件落本地，dataChannel 只广播 URI）：
 * - 布局真源是 launcher 的 databases/launcher.db，只把六张布局表白名单导出成 JSON（不直接拷 db 文件，
 *   避免同 uid 拷贝时拿到写入中途的撕裂数据）。
 * - 隐藏应用状态从 app_hidden_prefs 的 <int> 读出成 JSON map（键 pkg#userId，值是该条的 rank 原样），
 *   不再按原文拷 prefs 文件：拷文件对恢复没有意义（进程内是缓存），状态必须走 DPAM 的入口改。
 * - 备份容器是标准 zip（扩展名 .zip），内含 meta.json（概要）与 layout.json（布局数据）两个条目，
 *   经 FileProvider URI 交给 launcher 进程写/读：
 *   SAF 授权只给 app 的 uid，所以 app 必须先 grantUriPermission 把读/写权限授予桌面包名。
 * - 恢复由 hook 在 launcher 进程内完成，写完调 LauncherModel.forceReload() 让桌面重读库
 *   （同进程写库不会触发 LauncherProvider.reloadLauncherIfExternal），不杀进程。
 */
object LauncherBackupUtils {

    /** 本地备份份数上限（超过时先删最旧的 auto） */
    const val MAX_SNAPSHOTS = 20

    /** 备份文件后缀（内容即标准 zip，压缩工具可直接打开） */
    const val FILE_SUFFIX = ".ltbackup"

    /** 名字是否本工具的备份文件 */
    fun isSnapshotFileName(name: String): Boolean = name.endsWith(FILE_SUFFIX)

    /** 备份目录（相对外部存储根）：Download/LuckyTool/launcher_backup */
    const val RELATIVE_DIR = "Download/LuckyTool/launcher_backup"

    /**
     * 子目录名（[RELATIVE_DIR] 的最后一段）。
     * 宿主退到自己的外部媒体目录时用同一个名字：`Android/media/<桌面包名>/launcher_backup`，
     * app 侧列快照前会把里面的文件搬回 [RELATIVE_DIR]，两边名字一致才认得出哪些是本工具的快照。
     */
    const val MEDIA_SUBDIR = "launcher_backup"

    /** 容器格式版本（meta.json 的 version）；恢复时版本不符直接拒绝，避免读旧格式半截数据 */
    const val PAYLOAD_VERSION = 1

    /**
     * 参与备份/恢复的布局表白名单：三种桌面模式（Standard 0 / Drawer 2 / Simple 3）各一套。
     * 只碰这六张布局表：绝不 DELETE 白名单外的表。
     * 不含 desktopappedit（编辑态草稿）与 recommendmarketinfo（网络拉来的市场推荐，非布局数据），
     * 恢复时对它们 DELETE 全表再回灌没有任何收益，只会制造副作用。
     */
    val LAYOUT_TABLES = listOf(
        "singledesktopitems",
        "singledesktopitems_draw",
        "singledesktopitems_simple",
        "singledesktopscreens",
        "singledesktopscreens_draw",
        "singledesktopscreens_simple",
    )

    /**
     * launcher 包名候选：本机实测 com.android.launcher；老 ColorOS 为 com.oppo.launcher
     * （与 hook/hookers/MainHook.kt 加载的作用域包名保持一致，否则 app 侧下发指令会直接找不到人）。
     */
    val LAUNCHER_CANDIDATES = listOf("com.android.launcher", "com.oplus.launcher", "com.oppo.launcher")

    /** app → hook 的指令 pref 键 */
    const val KEY_COMMAND = "launcher_backup_command"

    const val ACTION_BACKUP = "backup"
    const val ACTION_RESTORE = "restore"

    val ACTIONS = listOf(ACTION_BACKUP, ACTION_RESTORE)

    private const val TIME_FORMAT = "yyyyMMdd_HHmmss"

    // ---------------- 快照命名 ----------------

    /** 快照基名：yyyyMMdd_HHmmss 加下划线加 tag（只含数字、下划线与 ASCII 标签，不含空格与中文） */
    fun snapshotName(now: Long, tag: LauncherSnapshotTag): String =
        SimpleDateFormat(TIME_FORMAT, Locale.US).format(Date(now)) + "_" + tag.id

    /** 备份文件名：基名加 .ltbackup（实际落盘用这个） */
    fun backupFileName(now: Long, tag: LauncherSnapshotTag): String =
        snapshotName(now, tag) + FILE_SUFFIX

    /** 解析备份文件名（带不带后缀都认）；不是本工具的文件返回 null */
    fun parseSnapshotName(name: String?): LauncherSnapshot? {
        val text = name?.trim().orEmpty().removeSuffix(FILE_SUFFIX)
        if (text.isEmpty()) return null
        val tag = LauncherSnapshotTag.entries.firstOrNull { text.endsWith("_" + it.id) } ?: return null
        val stamp = text.removeSuffix("_" + tag.id)
        val time = runCatching {
            SimpleDateFormat(TIME_FORMAT, Locale.US).parse(stamp)?.time
        }.getOrNull() ?: return null
        return LauncherSnapshot(text, tag, time)
    }

    /** 文件名列表（名字 + 字节数）→ 快照列表，按时间倒序；非本工具文件忽略 */
    fun parseSnapshotFiles(files: List<Pair<String, Long>>): List<LauncherSnapshot> =
        files.mapNotNull { entry ->
            val parsed = parseSnapshotName(entry.first) ?: return@mapNotNull null
            LauncherSnapshot(
                name = parsed.name,
                tag = parsed.tag,
                timeMillis = parsed.timeMillis,
                sizeBytes = entry.second,
                onDiskName = entry.first.trim(),
            )
        }.sortedWith(compareByDescending<LauncherSnapshot> { it.timeMillis }.thenByDescending { it.name })

    // ---------------- 指令协议 ----------------

    /**
     * app → hook 的指令，单行（dataChannel 只传这一条）：
     *  backup|<uri>|<tag>|<token>
     *  restore|<uri>|<token>
     * token 用于去重（同一条指令只执行一次）。
     */
    fun buildBackupCommand(uri: String, tag: LauncherSnapshotTag, token: String = newToken()): String =
        ACTION_BACKUP + "|" + uri + "|" + tag.id + "|" + token

    fun buildRestoreCommand(uri: String, token: String = newToken()): String =
        ACTION_RESTORE + "|" + uri + "|" + token

    fun newToken(): String = System.currentTimeMillis().toString() + "-" + UUID.randomUUID().toString().take(8)

    /** 解析指令；action 必须在白名单内，URI 与 token 都不能为空，否则返回 null（hook 侧一律忽略） */
    fun parseCommand(raw: String?): LauncherCommand? {
        val text = raw?.trim().orEmpty()
        if (text.isEmpty()) return null
        val parts = text.split("|")
        val action = parts[0].trim()
        if (action !in ACTIONS) return null
        return when (action) {
            ACTION_BACKUP -> {
                if (parts.size < 4) return null
                val uri = parts[1].trim()
                val tag = LauncherSnapshotTag.ofId(parts[2].trim()) ?: return null
                val token = parts.drop(3).joinToString("|").trim()
                if (uri.isEmpty() || token.isEmpty()) null else LauncherCommand(action, uri, tag, token)
            }

            ACTION_RESTORE -> {
                if (parts.size < 3) return null
                val uri = parts[1].trim()
                val token = parts.drop(2).joinToString("|").trim()
                if (uri.isEmpty() || token.isEmpty()) null else LauncherCommand(action, uri, null, token)
            }

            else -> null
        }
    }

    /** 同一条指令只执行一次：token 与上次相同即视为重复 */
    fun isDuplicateToken(lastToken: String?, token: String): Boolean =
        !lastToken.isNullOrBlank() && lastToken == token

    // ---------------- 备份容器（zip：meta.json + layout.json） ----------------

    /** 概要条目：解压时先读到它就能知道这份备份是什么 */
    const val ENTRY_META = "meta.json"

    /** 布局条目：六张布局表全列 + 隐藏应用 map（体积大，单条独占一个 zip 条目） */
    const val ENTRY_LAYOUT = "layout.json"

    /** meta.json 里时间的可读格式（排序与解析不靠它，只用文件名里的时间戳） */
    private const val META_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss"

    /**
     * meta.json 内容：
     * {version, tag, time, package, model, rom, launcherMode, tableRows:{表:行数}, hiddenCount, iconIncluded}
     */
    fun buildMetaJson(
        tag: LauncherSnapshotTag,
        time: Long,
        packageName: String,
        model: String,
        rom: String,
        launcherMode: Int,
        tableRows: Map<String, Int>,
        hiddenCount: Int,
        iconIncluded: Boolean = true,
    ): String = JSONObject().apply {
        put("version", PAYLOAD_VERSION)
        put("tag", tag.id)
        put("time", SimpleDateFormat(META_TIME_FORMAT, Locale.US).format(Date(time)))
        put("package", packageName)
        put("model", model)
        put("rom", rom)
        put("launcherMode", launcherMode)
        put("tableRows", JSONObject().apply { tableRows.forEach { (key, rows) -> put(key, rows) } })
        put("hiddenCount", hiddenCount)
        put("iconIncluded", iconIncluded)
    }.toString()

    /**
     * layout.json 内容：{tables:{表:[行]}, hidden:{pkg#userId:rank}}
     * 与 meta 分开成两个条目，写的时候 meta 在前，解压工具打开就能先看到概要。
     */
    fun buildLayoutJson(tables: JSONObject, hidden: Map<String, Int>): String =
        JSONObject().apply {
            put("tables", tables)
            put("hidden", JSONObject().apply { hidden.forEach { (key, value) -> put(key, value) } })
        }.toString()

    /**
     * 写 zip 容器：先 meta.json 再 layout.json，两段都按 UTF-8 字节写入。
     * 调用方负责 openOutputStream(uri, "wt")（覆盖写，避免旧内容尾巴留在文件里）。
     */
    fun writeContainer(output: OutputStream, metaJson: String, layoutJson: String) {
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(ENTRY_META))
            zip.write(metaJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.putNextEntry(ZipEntry(ENTRY_LAYOUT))
            zip.write(layoutJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            zip.finish()
        }
    }

    /**
     * 把两份 JSON 打包成 .ltbackup 的 zip 字节（不绑定任何 Context / 输出目标）。
     * app 侧写 FileProvider uri、hook 侧写 MediaStore uri，两边共用这一份格式代码。
     */
    fun containerBytes(metaJson: String, layoutJson: String): ByteArray {
        val buffer = ByteArrayOutputStream()
        writeContainer(buffer, metaJson, layoutJson)
        return buffer.toByteArray()
    }

    /**
     * 读 zip 容器：按条目名收集，不依赖条目顺序（zip 允许任意顺序）。
     * 不是 zip / 读失败返回 null，由调用方给出「备份文件不完整」的提示。
     */
    fun readContainer(input: InputStream): Map<String, String>? = runCatching {
        val entries = LinkedHashMap<String, String>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                //只收本工具写的两个条目，目录项与其它内容一律忽略
                if (entry.name == ENTRY_META || entry.name == ENTRY_LAYOUT) {
                    entries[entry.name] = zip.readBytes().toString(Charsets.UTF_8)
                }
                zip.closeEntry()
            }
        }
        entries
    }.getOrNull()

    /** 容器里缺失（或为空）的条目名；用来拼「备份文件不完整：缺少 meta.json」这类提示 */
    fun missingEntries(entries: Map<String, String>?): List<String> =
        listOf(ENTRY_META, ENTRY_LAYOUT).filter { entries?.get(it).isNullOrEmpty() }

    /** 解析容器：条目缺失 / meta 版本不符 / layout 结构坏掉都返回 null（调用方只报错，不猜） */
    fun parseContainer(entries: Map<String, String>): LauncherContainer? = runCatching {
        if (missingEntries(entries).isNotEmpty()) return null
        val metaObject = JSONObject(entries.getValue(ENTRY_META))
        if (metaObject.optInt("version") != PAYLOAD_VERSION) return null
        val layoutObject = JSONObject(entries.getValue(ENTRY_LAYOUT))
        val tables = layoutObject.optJSONObject("tables") ?: return null
        val hidden = LinkedHashMap<String, Int>()
        layoutObject.optJSONObject("hidden")?.let { map ->
            map.keys().forEach { key ->
                if (parseHiddenKey(key) != null) hidden[key] = map.optInt(key)
            }
        }
        val tableRows = LinkedHashMap<String, Int>()
        metaObject.optJSONObject("tableRows")?.let { map ->
            map.keys().forEach { key -> tableRows[key] = map.optInt(key) }
        }
        LauncherContainer(
            meta = LauncherMeta(
                tag = LauncherSnapshotTag.ofId(metaObject.optString("tag")) ?: LauncherSnapshotTag.MANUAL,
                time = metaObject.optString("time"),
                packageName = metaObject.optString("package"),
                model = metaObject.optString("model"),
                rom = metaObject.optString("rom"),
                launcherMode = metaObject.optInt("launcherMode", -1),
                tableRows = tableRows,
                hiddenCount = metaObject.optInt("hiddenCount", 0),
                iconIncluded = metaObject.optBoolean("iconIncluded", true),
            ),
            tables = tables,
            hidden = hidden,
        )
    }.getOrNull()

    /**
     * 校验一份容器是否真是可用的桌面布局备份：条目齐全 + meta 版本受支持 + tables 结构完整。
     * 返回 null 表示通过；否则返回 [LauncherTaskError.INVALID_FILE]（导入时据此删掉刚复制的文件）。
     */
    fun validateContainer(entries: Map<String, String>?): LauncherTaskError? {
        if (missingEntries(entries).isNotEmpty()) return LauncherTaskError.INVALID_FILE
        return if (parseContainer(entries!!) == null) LauncherTaskError.INVALID_FILE else null
    }

    /** 从文本流校验（导入时用）：读不出来 / 格式不对都算无效文件 */
    fun validateContainer(input: InputStream): LauncherTaskError? =
        validateContainer(readContainer(input))

    /**
     * 保留策略：最多 [limit] 份，超限时先删最旧的 auto，再删最旧的 manual / pre_restore。
     * 返回待删除的候选（调用方按自己的存储方式删：app 删文件，hook 删 MediaStore 行）。
     * [excluded] 里的名字永远不选（恢复时正在用的那份 / 刚写好的那份）。
     */
    fun planEvictions(
        snapshots: List<SnapshotCandidate>,
        limit: Int = MAX_SNAPSHOTS,
        excluded: Set<String> = emptySet(),
    ): List<SnapshotCandidate> {
        if (limit <= 0) return emptyList()
        // 超限份数按「包含受保护快照」的总数算，但受保护的那份永远不会被选中
        val excess = snapshots.size - limit
        if (excess <= 0) return emptyList()
        val byAge = snapshots
            .filter { it.name !in excluded }
            .sortedWith(compareBy({ it.timeMillis }, { it.name }))
        val picked = byAge.filter { it.tag == LauncherSnapshotTag.AUTO }.take(excess).toMutableList()
        if (picked.size < excess) {
            picked += byAge.filter { it.tag != LauncherSnapshotTag.AUTO }.take(excess - picked.size)
        }
        return picked
    }

    // ---------------- 隐藏应用状态 ----------------

    /**
     * 键规则来自 DeepProtectedAppsManager.k(userId, pkg)：pkg + "#" + userId。
     * 值不是开关而是隐藏项的 rank（键存在即已隐藏，-1 表示无记录）。
     */
    fun hiddenKey(packageName: String, userId: Int): String = packageName + "#" + userId

    /** 解析 <pkg>#<userId>；不含分隔符或 userId 非数字返回 null */
    fun parseHiddenKey(key: String?): HiddenApp? {
        val text = key?.trim().orEmpty()
        val index = text.lastIndexOf('#')
        if (index <= 0 || index == text.length - 1) return null
        val userId = text.substring(index + 1).toIntOrNull() ?: return null
        return HiddenApp(text.substring(0, index), userId)
    }

    /**
     * 解析共享偏好 XML 里的 <int name="pkg#0" value="2" />（进程内取不到 prefs 缓存时的兜底）。
     * 这里只做「原样取值」，绝不归一化：rank 一旦被改写就会打乱隐藏列表顺序。
     */
    fun parseHiddenPrefsXml(xml: String?): Map<String, Int> {
        val text = xml.orEmpty()
        if (text.isBlank()) return emptyMap()
        val result = LinkedHashMap<String, Int>()
        val regex = Regex("""<int\s+name="([^"]+)"\s+value="(-?\d+)"""")
        regex.findAll(text).forEach { match ->
            val key = match.groupValues[1]
            val value = match.groupValues[2].toIntOrNull() ?: return@forEach
            if (parseHiddenKey(key) != null) result[key] = value
        }
        return result
    }

    /**
     * 隐藏状态双向差集：备份里有、当前没有 → 要隐藏；当前有、备份里没有 → 要取消隐藏。
     * 值（rank）不参与比较，由 launcher 自己的 I() 入口重新排列。
     */
    fun planHiddenSync(snapshot: Map<String, Int>, current: Map<String, Int>): HiddenSync {
        val toHide = snapshot.keys
            .filter { it !in current }
            .mapNotNull { parseHiddenKey(it) }
            .sortedWith(compareBy({ it.packageName }, { it.userId }))
        val toUnhide = current.keys
            .filter { it !in snapshot }
            .mapNotNull { parseHiddenKey(it) }
            .sortedWith(compareBy({ it.packageName }, { it.userId }))
        return HiddenSync(toHide, toUnhide)
    }

    /** 探测已安装的 launcher 包名（app 侧 QUERY_ALL_PACKAGES 可见系统应用），找不到返回 null */
    fun detectLauncherPackage(context: Context): String? {
        val manager = context.packageManager ?: return null
        return LAUNCHER_CANDIDATES.firstOrNull { pkg ->
            runCatching { manager.getPackageInfo(pkg, 0) }.isSuccess
        }
    }

    /**
     * 把指令推给 hook（与其它功能同一条 dataChannel/prefs 通道）。
     * 只传一条单行指令：数据本身走文件（全量布局 JSON 有 1.2MB 量级，塞不进 Binder）。
     */
    fun sendCommand(context: Context, command: String, launcherPackage: String?): Boolean {
        val pkg = launcherPackage ?: detectLauncherPackage(context) ?: return false
        if (parseCommand(command) == null) return false
        return runCatching {
            context.sendPrefsValue(pkg, KEY_COMMAND, command)
            true
        }.getOrDefault(false)
    }
}

/** 等待桌面进程时的下发进度：界面据此显示「第 attempt/total 次」的分阶段提示 */
data class LauncherBackupHint(val attempt: Int, val total: Int)

/**
 * 备份文件的搬运与指令下发（只在 app 进程用；hook 侧不调这里）。
 *
 * 为什么必须授权：SAF/FileProvider 的 URI 权限按 uid 授予，launcher 进程是另一个 uid，
 * 不 grantUriPermission 直接 openInputStream 必抛 SecurityException。
 * 为什么用文件长度当 ack：备份没有回传通道，hook 把 zip 容器写进这个文件，长度从 0 变成非 0 就说明它真的跑了。
 */
object LauncherBackupTransfer {

    /** 单次下发后等 hook 写文件的上限；等不到就换新 token 再发一次 */
    const val ATTEMPT_TIMEOUT_MS = 5_000L

    /** 下发次数上限：桌面进程重启后要等它起来，5s × 6 = 30s 总窗口 */
    const val DISPATCH_ATTEMPTS = 6

    /** 单次下发内轮询文件长度的间隔 */
    private const val POLL_INTERVAL_MS = 250L

    /** 连续两次长度相同才算写完（zip 尾部与中央目录比头部晚落盘） */
    private const val STABLE_ROUNDS = 2

    private const val PROVIDER_SUFFIX = ".FileProvider"

    private val mutex = Mutex()

    private val _busy = MutableStateFlow(false)

    /** 是否有备份/恢复正在跑（界面据此禁用按钮） */
    val busy: StateFlow<Boolean> = _busy

    private val _busyHint = MutableStateFlow<LauncherBackupHint?>(null)

    /** 等待桌面进程时的下发进度；不等待时为 null */
    val busyHint: StateFlow<LauncherBackupHint?> = _busyHint

    /** 备份目录：Download/LuckyTool/launcher_backup */
    @Suppress("DEPRECATION")
    fun backupDir(): File =
        File(Environment.getExternalStorageDirectory(), LauncherBackupUtils.RELATIVE_DIR)

    /** 列本地备份文件（只认 .ltbackup，按时间倒序） */
    fun listSnapshots(): List<LauncherSnapshot> {
        val dir = backupDir()
        val entries = runCatching {
            // 0 字节的 .ltbackup 是「备份刚建文件、hook 还没写」或写失败的残骸，不能当成可用快照
            dir.listFiles()?.filter {
                it.isFile && LauncherBackupUtils.isSnapshotFileName(it.name) && it.length() > 0L
            }
                ?.map { it.name to it.length() }
                ?: emptyList()
        }.getOrDefault(emptyList())
        return LauncherBackupUtils.parseSnapshotFiles(entries)
    }

    /**
     * 把宿主（launcher）进程兜底写下的快照搬进正式备份目录。
     *
     * 为什么需要：宿主进程没有存储权限，MediaStore 投稿一旦失败（真机报
     * `Unknown URL content://media/external_primary/downloads`），它只能退到自己的外部媒体目录
     * `Android/media/<桌面包名>/launcher_backup`。那个目录不需要任何权限、文件管理器里也看得见，
     * 但不在本模块的备份目录里，界面既恢复不了也删不掉，等于一份看不见的备份。
     * app 侧有全盘访问权限，所以在这里做一次搬运：搬成功就删掉桌面侧的副本，不留两份。
     *
     * 搬运失败（读不了、删不掉、长度对不上）一律保留原文件并记日志：已有的备份绝不能因为搬运出错而消失。
     * 宿主退到 `Android/data/<桌面包名>/files` 的那一档搬不动（Android 11+ 禁止跨应用读 data 目录），
     * 那种快照只能在日志里看到位置，恢复需要手动取出——所以它排在最后。
     */
    suspend fun adoptHostFallback(context: Context): Unit = withContext(Dispatchers.IO) {
        val dir = backupDir()
        if (!dir.isDirectory && !dir.mkdirs()) return@withContext
        renameLegacyArchives(dir)
        for (source in hostFallbackDirs(context)) {
            val files = runCatching {
                source.listFiles()?.filter {
                    it.isFile && LauncherBackupUtils.isSnapshotFileName(it.name) && it.length() > 0L
                }
            }.getOrNull() ?: continue
            for (file in files) {
                val target = File(dir, file.name)
                runCatching {
                    if (!target.exists()) {
                        file.inputStream().use { input -> target.outputStream().use { input.copyTo(it) } }
                    }
                    if (target.length() != file.length()) error("长度不一致")
                    if (!file.delete()) error("无法删除桌面侧副本")
                }.onFailure { throwable ->
                    // 搬不过去的残片不能留在正式目录里冒充一份可用快照
                    if (target.isFile && target.length() != file.length()) runCatching { target.delete() }
                    LogUtils.d(
                        "LauncherBackupTransfer", "adoptHostFallback",
                        "搬运 " + file.name + " 失败: " + (throwable.message ?: throwable.javaClass.simpleName),
                    )
                }
            }
        }
    }

    /**
     * 把历史上被媒体库改写掉扩展名的 .zip 快照改回 [LauncherBackupUtils.FILE_SUFFIX]。
     *
     * 老版本自动备份投稿时声明的 MIME 是 application/zip，媒体库按 MIME 把 .ltbackup 换成了 .zip，
     * 这类文件列表与恢复都不认，等于凭空少了一份备份。只改「去掉 .zip 后仍是合法快照名」的文件，
     * 目录里的其它 zip（用户自己放的）一律不动；改不动就留在原处并记日志。
     */
    private fun renameLegacyArchives(dir: File) {
        val legacy = runCatching {
            dir.listFiles()?.filter {
                it.isFile && it.name.endsWith(".zip") && it.length() > 0L &&
                    LauncherBackupUtils.parseSnapshotName(it.name.removeSuffix(".zip")) != null
            }
        }.getOrNull() ?: return
        for (file in legacy) {
            val target = File(dir, file.name.removeSuffix(".zip") + LauncherBackupUtils.FILE_SUFFIX)
            runCatching {
                if (target.exists()) error("目标已存在 " + target.name)
                if (!file.renameTo(target)) error("重命名失败")
                LogUtils.d("LauncherBackupTransfer", "renameLegacyArchives", "改回扩展名 " + file.name)
            }.onFailure { throwable ->
                LogUtils.d(
                    "LauncherBackupTransfer", "renameLegacyArchives",
                    "改写 " + file.name + " 失败: " + (throwable.message ?: throwable.javaClass.simpleName),
                )
            }
        }
    }

    /** 宿主兜底目录候选：只列 app 侧读得到的那一档（媒体目录） */
    @Suppress("DEPRECATION")
    private fun hostFallbackDirs(context: Context): List<File> {
        val launcher = runCatching { LauncherBackupUtils.detectLauncherPackage(context) }.getOrNull()
            ?: return emptyList()
        val media = File(
            Environment.getExternalStorageDirectory(),
            "Android/media/" + launcher + "/" + LauncherBackupUtils.MEDIA_SUBDIR,
        )
        return listOf(media)
    }

    /** 手动/自动备份：建空文件 → 授权 → 下发 backup → 等文件变非空 → 走保留策略 */
    suspend fun backup(context: Context, tag: LauncherSnapshotTag): LauncherTaskResult =
        mutex.withLock {
            _busy.value = true
            try {
                backupLocked(context, tag)
            } finally {
                _busy.value = false
                _busyHint.value = null
            }
        }

    /** 恢复：先补拍 pre_restore（失败即中止）→ 授权 → 下发 restore（结果看桌面提示） */
    suspend fun restore(context: Context, file: File): LauncherTaskResult = mutex.withLock {
        _busy.value = true
        try {
            if (!file.isFile || file.length() <= 0L) {
                return@withLock LauncherTaskResult.Failure(LauncherTaskError.EMPTY_FILE, file.name)
            }
            // 1) 补拍：任何恢复都必须有回滚点，补拍不出东西就中止
            val pre = backupLocked(context, LauncherSnapshotTag.PRE_RESTORE, protected = file)
            if (pre is LauncherTaskResult.Failure) {
                return@withLock LauncherTaskResult.Failure(
                    LauncherTaskError.PRE_RESTORE_FAILED,
                    pre.error.name + (if (pre.detail.isEmpty()) "" else ": " + pre.detail),
                )
            }
            // 2) 把读权限授予桌面进程并下发指令
            val launcherPackage = LauncherBackupUtils.detectLauncherPackage(context)
                ?: return@withLock LauncherTaskResult.Failure(LauncherTaskError.NO_LAUNCHER)
            dispatch(context, launcherPackage, file, LauncherBackupTag.READ, ::buildRestore)
        } finally {
            _busy.value = false
            _busyHint.value = null
        }
    }

    /** 导入：把用户从文件选择器挑中的内容复制进备份目录，之后按普通本地备份处理 */
    suspend fun importFile(context: Context, source: Uri): LauncherTaskResult = withContext(Dispatchers.IO) {
        runCatching {
            val dir = backupDir()
            if (!dir.isDirectory && !dir.mkdirs()) {
                return@runCatching LauncherTaskResult.Failure(LauncherTaskError.CREATE_FAILED, dir.absolutePath)
            }
            val target = File(dir, LauncherBackupUtils.backupFileName(System.currentTimeMillis(), LauncherSnapshotTag.MANUAL))
            if (target.exists()) target.delete()
            context.contentResolver.openInputStream(source).use { input ->
                if (input == null) return@runCatching LauncherTaskResult.Failure(LauncherTaskError.EMPTY_FILE, target.name)
                target.outputStream().use { output -> input.copyTo(output) }
            }
            // 复制完立刻校验容器：用户选错文件时绝不能留下一个「看起来正常」的快照，
            // 否则它会占掉 20 份保留名额、还让人以为有可用备份，直到恢复时才在桌面报错
            val invalid = runCatching { target.inputStream().use { LauncherBackupUtils.validateContainer(it) } }
                .getOrElse { LauncherTaskError.IO_ERROR }
            if (invalid != null) {
                // 删除失败也不留残文件：删不掉就再试一次，仍失败也照实报无效
                runCatching { target.delete() }
                return@runCatching LauncherTaskResult.Failure(invalid, target.name)
            }
            scan(context, target)
            if (target.length() <= 0L) LauncherTaskResult.Failure(LauncherTaskError.EMPTY_FILE, target.name)
            else LauncherTaskResult.Success(target)
        }.getOrElse { throwable ->
            LauncherTaskResult.Failure(LauncherTaskError.IO_ERROR, throwable.message ?: throwable.javaClass.simpleName)
        }
    }

    /**
     * 删除一份本地快照：不经过桌面进程，直接删文件（与 evict、导入失败时同样用 File.delete；
     * 备份目录里 hook 写的自动快照在本 app 的 MANAGE_EXTERNAL_STORAGE 下同样可删）。
     * 文件已经不在了按成功返回——目标状态已达成，界面重列后该行自然消失。
     */
    fun delete(context: Context, snapshot: LauncherSnapshot): LauncherTaskResult {
        val file = File(backupDir(), snapshot.fileName)
        if (!file.exists()) return LauncherTaskResult.Success(file)
        return if (runCatching { file.delete() }.getOrDefault(false)) {
            // 删完扫一次：让文件管理器里的残留条目跟着消失
            scan(context, file)
            LauncherTaskResult.Success(file)
        } else {
            LauncherTaskResult.Failure(LauncherTaskError.IO_ERROR, file.absolutePath)
        }
    }

    // ---------------- 内部实现 ----------------

    private suspend fun backupLocked(
        context: Context,
        tag: LauncherSnapshotTag,
        protected: File? = null,
    ): LauncherTaskResult = withContext(Dispatchers.IO) {
        val dir = backupDir()
        if (!dir.isDirectory && !dir.mkdirs()) {
            return@withContext LauncherTaskResult.Failure(LauncherTaskError.CREATE_FAILED, dir.absolutePath)
        }
        val file = File(dir, LauncherBackupUtils.backupFileName(System.currentTimeMillis(), tag))
        if (file.exists()) {
            return@withContext LauncherTaskResult.Failure(LauncherTaskError.FILE_EXISTS, file.name)
        }
        runCatching { file.createNewFile() }.getOrElse { throwable ->
            return@withContext LauncherTaskResult.Failure(
                LauncherTaskError.CREATE_FAILED,
                throwable.message ?: file.absolutePath,
            )
        }
        val launcherPackage = LauncherBackupUtils.detectLauncherPackage(context)
            ?: return@withContext LauncherTaskResult.Failure(LauncherTaskError.NO_LAUNCHER)
        val sent = dispatchUntilWritten(
            context = context,
            launcherPackage = launcherPackage,
            file = file,
            need = LauncherBackupTag.WRITE,
        ) { uri -> LauncherBackupUtils.buildBackupCommand(uri, tag) }
        if (sent is LauncherTaskResult.Failure) {
            // 整个窗口里没人写出内容：删掉空文件，别让它占保留名额、也别让人以为是可用快照
            runCatching { file.delete() }
            return@withContext sent
        }
        scan(context, file)
        evict(dir, file)
        LauncherTaskResult.Success(file)
    }

    private fun buildRestore(uri: String): String = LauncherBackupUtils.buildRestoreCommand(uri)

    /**
     * 反复下发备份指令，直到 hook 把文件写出来。
     *
     * 为什么不只发一次：重启（force-stop / 重载 Hooker）之后 launcher 进程还没起来，
     * prefs 里那条指令根本没人消费，一次下发几乎必然等到超时。hook 侧按 token 去重
     * （[LauncherBackupHooker] 的 isDuplicateToken），所以每次换新 token 重发就等价于
     * 「再探测一次桌面是否就绪」，而备份重复执行是安全的（同一路径覆盖写）。
     * 总窗口 = [DISPATCH_ATTEMPTS] × [ATTEMPT_TIMEOUT_MS]，期间由 [busyHint] 报第几次。
     *
     * 恢复不走这里：重复执行恢复会反复写表并重载布局，不是幂等操作。
     */
    private suspend fun dispatchUntilWritten(
        context: Context,
        launcherPackage: String,
        file: File,
        need: LauncherBackupTag,
        build: (String) -> String,
    ): LauncherTaskResult = withContext(Dispatchers.IO) {
        val uri = runCatching {
            FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + PROVIDER_SUFFIX,
                file,
            )
        }.getOrElse { throwable ->
            return@withContext LauncherTaskResult.Failure(
                LauncherTaskError.GRANT_FAILED,
                throwable.message ?: throwable.javaClass.simpleName,
            )
        }
        // 授权按 uid + URI 生效：整个窗口共用一份授权，重发只重建指令内容（token 每次都新）
        val granted = runCatching {
            context.grantUriPermission(launcherPackage, uri, need.flags)
            true
        }.getOrDefault(false)
        if (!granted) return@withContext LauncherTaskResult.Failure(LauncherTaskError.GRANT_FAILED, launcherPackage)
        for (attempt in 1..DISPATCH_ATTEMPTS) {
            _busyHint.value = LauncherBackupHint(attempt, DISPATCH_ATTEMPTS)
            if (!LauncherBackupUtils.sendCommand(context, build(uri.toString()), launcherPackage)) {
                return@withContext LauncherTaskResult.Failure(LauncherTaskError.SEND_FAILED, launcherPackage)
            }
            // 长度从 0 变成非 0 且稳定 = hook 写完了
            if (awaitWritten(file, ATTEMPT_TIMEOUT_MS)) return@withContext LauncherTaskResult.Success(file)
        }
        LauncherTaskResult.Failure(LauncherTaskError.TIMEOUT, file.name)
    }

    private suspend fun dispatch(
        context: Context,
        launcherPackage: String,
        file: File,
        need: LauncherBackupTag,
        build: (String) -> String,
    ): LauncherTaskResult = withContext(Dispatchers.IO) {
        val uri = runCatching {
            FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + PROVIDER_SUFFIX,
                file,
            )
        }.getOrElse { throwable ->
            return@withContext LauncherTaskResult.Failure(
                LauncherTaskError.GRANT_FAILED,
                throwable.message ?: throwable.javaClass.simpleName,
            )
        }
        val granted = runCatching {
            context.grantUriPermission(launcherPackage, uri, need.flags)
            true
        }.getOrDefault(false)
        if (!granted) return@withContext LauncherTaskResult.Failure(LauncherTaskError.GRANT_FAILED, launcherPackage)
        if (!LauncherBackupUtils.sendCommand(context, build(uri.toString()), launcherPackage)) {
            return@withContext LauncherTaskResult.Failure(LauncherTaskError.SEND_FAILED, launcherPackage)
        }
        LauncherTaskResult.Success(file)
    }

    /** 轮询文件长度：非 0 且连续两次不变即认为写完 */
    private suspend fun awaitWritten(file: File, timeoutMs: Long = ATTEMPT_TIMEOUT_MS): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        var lastLength = -1L
        var stable = 0
        while (System.currentTimeMillis() < deadline) {
            val length = runCatching { file.length() }.getOrDefault(0L)
            if (length > 0L) {
                if (length == lastLength) stable++ else stable = 0
                lastLength = length
                if (stable >= STABLE_ROUNDS - 1) return true
            }
            delay(POLL_INTERVAL_MS)
        }
        return false
    }

    /** 让文件管理器/相册能看见新文件（Download 目录下的文件不扫描就不会出现在「最近」里） */
    private fun scan(context: Context, file: File) {
        runCatching {
            MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
        }
    }

    /** 保留策略：按文件名列表算，protected（正在恢复的目标）永不删除 */
    private fun evict(dir: File, protected: File?) {
        val files = runCatching { dir.listFiles()?.filter { it.isFile } ?: emptyList() }.getOrDefault(emptyList())
        val snapshots = LauncherBackupUtils.parseSnapshotFiles(files.map { it.name to it.length() })
        val excluded = if (protected == null) emptySet() else setOfNotNull(
            LauncherBackupUtils.parseSnapshotName(protected.name)?.name
        )
        LauncherBackupUtils.planEvictions(snapshots, excluded = excluded).forEach { snapshot ->
            runCatching { File(dir, snapshot.fileName).delete() }
        }
    }
}

/** 授权方向：备份要写，恢复只要读 */
enum class LauncherBackupTag(val flags: Int) {
    READ(Intent.FLAG_GRANT_READ_URI_PERMISSION),
    WRITE(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION),
}

/** app 侧一次备份/恢复的结果（界面据此给提示，Worker 只记日志） */
sealed interface LauncherTaskResult {
    data class Success(val file: File) : LauncherTaskResult
    data class Failure(val error: LauncherTaskError, val detail: String = "") : LauncherTaskResult
}

/** 失败分类：界面按它取文案，不拼英文异常串 */
enum class LauncherTaskError {
    NO_LAUNCHER,
    CREATE_FAILED,
    FILE_EXISTS,
    GRANT_FAILED,
    SEND_FAILED,
    TIMEOUT,
    EMPTY_FILE,
    PRE_RESTORE_FAILED,

    /** 选中的文件不是本工具的布局备份（缺条目 / 版本不符 / 结构坏） */
    INVALID_FILE,
    IO_ERROR,
}

/** 隐藏应用记录（键 pkg#userId，值是隐藏项的 rank） */
data class HiddenApp(val packageName: String, val userId: Int)

/** 隐藏状态需要的双向改动 */
data class HiddenSync(val toHide: List<HiddenApp>, val toUnhide: List<HiddenApp>)

/** meta.json 的解析结果（概要信息，恢复写库时不用它） */
data class LauncherMeta(
    val tag: LauncherSnapshotTag,
    val time: String,
    val packageName: String,
    val model: String,
    val rom: String,
    val launcherMode: Int,
    val tableRows: Map<String, Int>,
    val hiddenCount: Int,
    val iconIncluded: Boolean,
)

/** 备份容器解析结果：概要 + 布局数据 */
data class LauncherContainer(
    val meta: LauncherMeta,
    val tables: JSONObject,
    val hidden: Map<String, Int>,
)

/** 快照标签：auto 是自动备份，manual 是界面手动，pre_restore 是恢复前的自动补拍 */
enum class LauncherSnapshotTag(val id: String) {
    AUTO("auto"),
    MANUAL("manual"),
    PRE_RESTORE("pre_restore"),
    ;

    companion object {
        fun ofId(id: String?): LauncherSnapshotTag? = entries.firstOrNull { it.id == id }
    }
}

/**
 * 淘汰策略需要的最小信息：只要名字、标签、时间戳。
 * 文件快照（[LauncherSnapshot]）与 MediaStore 行都实现它，策略本身不关心存储形态。
 */
interface SnapshotCandidate {
    val name: String
    val tag: LauncherSnapshotTag
    val timeMillis: Long

    /** 实际文件名（基名加 .ltbackup） */
    val fileName: String get() = name + LauncherBackupUtils.FILE_SUFFIX
}

/**
 * 一份备份：基名、标签、时间、文件字节数。
 * [onDiskName] 是它在备份目录里的真实文件名（外部导入的文件可能不带后缀），
 * 恢复与淘汰都必须按真实文件名下手，否则导入进来的快照既恢复不了也删不掉。
 */
data class LauncherSnapshot(
    override val name: String,
    override val tag: LauncherSnapshotTag,
    override val timeMillis: Long,
    val sizeBytes: Long = 0L,
    val onDiskName: String? = null,
) : SnapshotCandidate {
    override val fileName: String get() = onDiskName ?: (name + LauncherBackupUtils.FILE_SUFFIX)
}

/** 一条指令：action|uri|token（backup 额外带 tag） */
data class LauncherCommand(
    val action: String,
    val uri: String,
    val tag: LauncherSnapshotTag?,
    val token: String,
)

/** 自动备份的 prefs 键与取值约定 */
object LauncherBackupPrefs {

    /** 自动备份开关 */
    const val KEY_AUTO_ENABLED = "launcher_backup_auto_enabled"

    /** 自动备份周期（小时，字符串存值，与 list 控件一致） */
    const val KEY_AUTO_INTERVAL = "launcher_backup_auto_interval"

    /** 可选周期（小时） */
    val INTERVAL_HOURS = intArrayOf(6, 12, 24, 48)

    /** 默认周期 24 小时 */
    const val DEFAULT_INTERVAL_HOURS = 24

    /** 把存储值（字符串）解析成小时；非法值回落默认 */
    fun intervalHours(value: String?): Int {
        val hours = value?.trim()?.toIntOrNull() ?: return DEFAULT_INTERVAL_HOURS
        //只接受界面提供的周期，脏值（7、-6 之类）回落默认，避免派出计划外的周期任务
        return if (hours in INTERVAL_HOURS) hours else DEFAULT_INTERVAL_HOURS
    }
}
