package com.luckyzyx.luckytool.utils

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * LauncherBackupUtils 纯逻辑单测：文件名、指令协议、zip 容器、保留策略、隐藏应用差集。
 * 只用 Robolectric 拿可用的 org.json 实现；全程不落盘、不碰 Context。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LauncherBackupUtilsTest {

    private companion object {
        /** 固定基准时间，避免测试依赖当前时区之外的时钟 */
        const val BASE = 1_767_225_600_000L
        const val HOUR = 3_600_000L
    }

    private val stampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    private fun time(text: String): Long = requireNotNull(stampFormat.parse(text)) { "bad fixture: " + text }.time

    private fun snapshot(millis: Long, tag: LauncherSnapshotTag, size: Long = 0L): LauncherSnapshot =
        LauncherSnapshot(LauncherBackupUtils.snapshotName(millis, tag), tag, millis, size)

    // ---------------- 文件名 ----------------

    @Test
    fun snapshotNameUsesTimestampAndTag() {
        val millis = time("20260102_030405")
        assertEquals("20260102_030405_manual", LauncherBackupUtils.snapshotName(millis, LauncherSnapshotTag.MANUAL))
        assertEquals("20260102_030405_auto", LauncherBackupUtils.snapshotName(millis, LauncherSnapshotTag.AUTO))
        assertEquals(
            "20260102_030405_pre_restore",
            LauncherBackupUtils.snapshotName(millis, LauncherSnapshotTag.PRE_RESTORE),
        )
        // 文件名不含空格与中文，只由数字、下划线与 ASCII 标签组成
        val name = LauncherBackupUtils.snapshotName(millis, LauncherSnapshotTag.PRE_RESTORE)
        assertTrue(name.all { it.isDigit() || it == '_' || it in 'a'..'z' })
    }

    @Test
    fun backupFileNameAppendsSuffix() {
        val millis = time("20260102_030405")
        assertEquals(
            "20260102_030405_auto.ltbackup",
            LauncherBackupUtils.backupFileName(millis, LauncherSnapshotTag.AUTO),
        )
    }

    @Test
    fun snapshotNameRoundTripsThroughParse() {
        val millis = time("20251231_235959")
        val snapshot = requireNotNull(LauncherBackupUtils.parseSnapshotName(LauncherBackupUtils.backupFileName(millis, LauncherSnapshotTag.AUTO)))
        assertEquals(LauncherSnapshotTag.AUTO, snapshot.tag)
        assertEquals(millis, snapshot.timeMillis)
        assertEquals("20251231_235959_auto", snapshot.name)
        assertEquals("20251231_235959_auto.ltbackup", snapshot.fileName)
        // 不带后缀也认
        val bare = requireNotNull(LauncherBackupUtils.parseSnapshotName("20251231_235959_manual"))
        assertEquals(LauncherSnapshotTag.MANUAL, bare.tag)
        assertEquals(millis, bare.timeMillis)
    }

    @Test
    fun parseSnapshotNameRejectsForeignNames() {
        assertNull(LauncherBackupUtils.parseSnapshotName(null))
        assertNull(LauncherBackupUtils.parseSnapshotName(""))
        assertNull(LauncherBackupUtils.parseSnapshotName("   "))
        assertNull(LauncherBackupUtils.parseSnapshotName(".ltbackup"))
        assertNull(LauncherBackupUtils.parseSnapshotName("launcher.db"))
        assertNull(LauncherBackupUtils.parseSnapshotName("20260102_030405.ltbackup"))
        assertNull(LauncherBackupUtils.parseSnapshotName("20260102_030405_nightly.ltbackup"))
        assertNull(LauncherBackupUtils.parseSnapshotName("2026_01_02_auto.ltbackup"))
    }

    @Test
    fun parseSnapshotFilesSortsByTimeDescAndKeepsSize() {
        val files = listOf(
            "20260101_000000_auto.ltbackup" to 100L,
            "20260103_000000_manual.ltbackup" to 300L,
            "random.txt" to 1L,
            "20260102_000000_pre_restore.ltbackup" to 200L,
        )
        val snapshots = LauncherBackupUtils.parseSnapshotFiles(files)
        assertEquals(3, snapshots.size)
        assertEquals(listOf("20260103_000000_manual", "20260102_000000_pre_restore", "20260101_000000_auto"), snapshots.map { it.name })
        assertEquals(listOf(300L, 200L, 100L), snapshots.map { it.sizeBytes })
    }

    // ---------------- 指令协议 ----------------

    @Test
    fun buildCommandProducesFields() {
        val backup = LauncherBackupUtils.buildBackupCommand("content://demo/a.ltbackup", LauncherSnapshotTag.AUTO, "tok")
        assertEquals("backup|content://demo/a.ltbackup|auto|tok", backup)
        val restore = LauncherBackupUtils.buildRestoreCommand("content://demo/a.ltbackup", "tok2")
        assertEquals("restore|content://demo/a.ltbackup|tok2", restore)
    }

    @Test
    fun parseCommandAcceptsValidCommands() {
        val backup = requireNotNull(LauncherBackupUtils.parseCommand("backup|content://demo/a|manual|t1"))
        assertEquals(LauncherBackupUtils.ACTION_BACKUP, backup.action)
        assertEquals("content://demo/a", backup.uri)
        assertEquals(LauncherSnapshotTag.MANUAL, backup.tag)
        assertEquals("t1", backup.token)

        val restore = requireNotNull(LauncherBackupUtils.parseCommand("  restore|content://demo/b|t2  "))
        assertEquals(LauncherBackupUtils.ACTION_RESTORE, restore.action)
        assertEquals("content://demo/b", restore.uri)
        assertNull(restore.tag)
        assertEquals("t2", restore.token)
    }

    @Test
    fun parseCommandPutsBackTheWholeRestToken() {
        // token 里理论上有竖线也要还原，宁可多留也不能截断（截断会导致去重失效、指令重复执行）
        val command = requireNotNull(LauncherBackupUtils.parseCommand("restore|content://demo/c|a|b|c"))
        assertEquals("a|b|c", command.token)
    }

    @Test
    fun parseCommandRejectsInvalidCommands() {
        assertNull(LauncherBackupUtils.parseCommand(null))
        assertNull(LauncherBackupUtils.parseCommand(""))
        assertNull(LauncherBackupUtils.parseCommand("delete|content://demo/a|t"))
        assertNull(LauncherBackupUtils.parseCommand("backup|content://demo/a|manual"))
        assertNull(LauncherBackupUtils.parseCommand("backup||manual|t"))
        assertNull(LauncherBackupUtils.parseCommand("backup|content://demo/a|nightly|t"))
        assertNull(LauncherBackupUtils.parseCommand("backup|content://demo/a|manual|"))
        assertNull(LauncherBackupUtils.parseCommand("restore|content://demo/a"))
        assertNull(LauncherBackupUtils.parseCommand("restore||t"))
        assertNull(LauncherBackupUtils.parseCommand("restore|content://demo/a|"))
    }

    @Test
    fun duplicateTokenDetection() {
        assertTrue(LauncherBackupUtils.isDuplicateToken("abc", "abc"))
        assertFalse(LauncherBackupUtils.isDuplicateToken("abc", "abd"))
        assertFalse(LauncherBackupUtils.isDuplicateToken(null, "abc"))
        assertFalse(LauncherBackupUtils.isDuplicateToken("", "abc"))
    }

    @Test
    fun newTokenIsUnique() {
        val tokens = (1..50).map { LauncherBackupUtils.newToken() }.toSet()
        assertEquals(50, tokens.size)
        assertFalse(tokens.any { it.isBlank() })
    }

    // ---------------- zip 容器 ----------------

    private fun metaJson(version: Int = LauncherBackupUtils.PAYLOAD_VERSION): String {
        val json = LauncherBackupUtils.buildMetaJson(
            tag = LauncherSnapshotTag.MANUAL,
            time = time("20260102_030405"),
            packageName = "com.android.launcher",
            model = "PLK110",
            rom = "ColorOS 17",
            launcherMode = 2,
            tableRows = linkedMapOf("singledesktopitems" to 253, "singledesktopitems_draw" to 70),
            hiddenCount = 1,
        )
        return if (version == LauncherBackupUtils.PAYLOAD_VERSION) {
            json
        } else {
            json.replace("\"version\":" + LauncherBackupUtils.PAYLOAD_VERSION, "\"version\":" + version)
        }
    }

    private fun layoutJson(): String {
        val tables = JSONObject().apply {
            put("singledesktopitems", JSONArray().apply {
                put(JSONObject().apply { put("_id", 1); put("title", "Phone") })
                put(JSONObject().apply { put("_id", 2); put("title", "Camera") })
            })
        }
        return LauncherBackupUtils.buildLayoutJson(tables, linkedMapOf("com.demo.hidden#0" to 2))
    }

    @Test
    fun metaJsonCarriesSummaryFields() {
        val json = JSONObject(LauncherBackupUtils.buildMetaJson(
            tag = LauncherSnapshotTag.AUTO,
            time = time("20260102_030405"),
            packageName = "com.android.launcher",
            model = "PLK110",
            rom = "ColorOS 17",
            launcherMode = 2,
            tableRows = linkedMapOf("singledesktopitems" to 253),
            hiddenCount = 3,
        ))
        assertEquals(1, json.getInt("version"))
        assertEquals("auto", json.getString("tag"))
        assertEquals("2026-01-02 03:04:05", json.getString("time"))
        assertEquals("com.android.launcher", json.getString("package"))
        assertEquals("PLK110", json.getString("model"))
        assertEquals("ColorOS 17", json.getString("rom"))
        assertEquals(2, json.getInt("launcherMode"))
        assertEquals(253, json.getJSONObject("tableRows").getInt("singledesktopitems"))
        assertEquals(3, json.getInt("hiddenCount"))
        assertTrue(json.getBoolean("iconIncluded"))
    }

    @Test
    fun layoutJsonCarriesTablesAndHidden() {
        val json = JSONObject(layoutJson())
        assertEquals(2, json.getJSONObject("tables").getJSONArray("singledesktopitems").length())
        assertEquals(2, json.getJSONObject("hidden").getInt("com.demo.hidden#0"))
    }

    @Test
    fun containerRoundTripsThroughZip() {
        val output = ByteArrayOutputStream()
        LauncherBackupUtils.writeContainer(output, metaJson(), layoutJson())
        val bytes = output.toByteArray()
        assertTrue("zip 容器不能为空", bytes.isNotEmpty())

        // 条目顺序：meta.json 必须写在前面（解压工具打开就能先看到概要）
        val raw = String(bytes, Charsets.ISO_8859_1)
        assertTrue(raw.indexOf("meta.json") in 0 until raw.indexOf("layout.json"))

        val entries = LauncherBackupUtils.readContainer(ByteArrayInputStream(bytes))
        assertNotNull(entries)
        assertTrue(LauncherBackupUtils.missingEntries(entries).isEmpty())
        assertEquals(
            setOf(LauncherBackupUtils.ENTRY_META, LauncherBackupUtils.ENTRY_LAYOUT),
            entries!!.keys,
        )

        val container = requireNotNull(LauncherBackupUtils.parseContainer(entries))
        assertEquals(LauncherSnapshotTag.MANUAL, container.meta.tag)
        assertEquals("2026-01-02 03:04:05", container.meta.time)
        assertEquals("com.android.launcher", container.meta.packageName)
        assertEquals(253, container.meta.tableRows["singledesktopitems"])
        assertEquals(1, container.meta.hiddenCount)
        assertEquals(2, container.tables.getJSONArray("singledesktopitems").length())
        assertEquals(2, container.hidden["com.demo.hidden#0"])
    }

    @Test
    fun containerBytesFeedTheSameReadPath() {
        // hook 侧只拿得到字节（写 MediaStore 输出流），app 侧读文件流：两边必须共用同一份格式
        val bytes = LauncherBackupUtils.containerBytes(metaJson(), layoutJson())
        assertTrue("容器字节不能为空", bytes.isNotEmpty())
        assertNull(
            "字节容器必须能通过导入校验",
            LauncherBackupUtils.validateContainer(ByteArrayInputStream(bytes)),
        )

        val entries = LauncherBackupUtils.readContainer(ByteArrayInputStream(bytes))
        assertNotNull(entries)
        assertEquals(metaJson(), entries!![LauncherBackupUtils.ENTRY_META])
        assertEquals(layoutJson(), entries[LauncherBackupUtils.ENTRY_LAYOUT])
        val container = requireNotNull(LauncherBackupUtils.parseContainer(entries))
        assertEquals(2, container.tables.getJSONArray("singledesktopitems").length())
    }

    @Test
    fun readContainerRejectsGarbage() {
        val entries = LauncherBackupUtils.readContainer(ByteArrayInputStream("not a zip".toByteArray()))
        // 要么读不出来，要么读出来也是缺条目——两种都算「备份文件不完整」
        assertTrue(entries == null || LauncherBackupUtils.missingEntries(entries).size == 2)
    }

    @Test
    fun missingEntriesNamesWhatIsAbsent() {
        val onlyMeta = mapOf(LauncherBackupUtils.ENTRY_META to "{}")
        assertEquals(listOf(LauncherBackupUtils.ENTRY_LAYOUT), LauncherBackupUtils.missingEntries(onlyMeta))
        assertEquals(
            listOf(LauncherBackupUtils.ENTRY_META, LauncherBackupUtils.ENTRY_LAYOUT),
            LauncherBackupUtils.missingEntries(null),
        )
        assertEquals(
            listOf(LauncherBackupUtils.ENTRY_LAYOUT),
            LauncherBackupUtils.missingEntries(linkedMapOf(LauncherBackupUtils.ENTRY_META to "{}", LauncherBackupUtils.ENTRY_LAYOUT to "")),
        )
    }

    @Test
    fun parseContainerRejectsUnsupportedVersion() {
        val entries = linkedMapOf(
            LauncherBackupUtils.ENTRY_META to metaJson(version = 2),
            LauncherBackupUtils.ENTRY_LAYOUT to layoutJson(),
        )
        assertNull(LauncherBackupUtils.parseContainer(entries))
    }

    @Test
    fun parseContainerRejectsBrokenLayout() {
        val entries = linkedMapOf(
            LauncherBackupUtils.ENTRY_META to metaJson(),
            LauncherBackupUtils.ENTRY_LAYOUT to "{\"hidden\":{}}",
        )
        assertNull(LauncherBackupUtils.parseContainer(entries))
    }

    @Test
    fun parseContainerIgnoresForeignHiddenKeys() {
        val layout = JSONObject().apply {
            put("tables", JSONObject())
            put("hidden", JSONObject().apply { put("garbage", 1); put("com.demo#0", 4) })
        }
        val entries = linkedMapOf(
            LauncherBackupUtils.ENTRY_META to metaJson(),
            LauncherBackupUtils.ENTRY_LAYOUT to layout.toString(),
        )
        val container = requireNotNull(LauncherBackupUtils.parseContainer(entries))
        assertEquals(setOf("com.demo#0"), container.hidden.keys)
    }

    // ---------------- 导入时的容器校验 ----------------

    @Test
    fun validateContainerAcceptsRealContainer() {
        val output = ByteArrayOutputStream()
        LauncherBackupUtils.writeContainer(output, metaJson(), layoutJson())
        // 流式校验（导入路径就这一步）：读得出来 + 两条目齐全 + version=1 + tables 完整
        assertNull(LauncherBackupUtils.validateContainer(ByteArrayInputStream(output.toByteArray())))
        // 条目 map 校验同样通过
        assertNull(
            LauncherBackupUtils.validateContainer(
                linkedMapOf(
                    LauncherBackupUtils.ENTRY_META to metaJson(),
                    LauncherBackupUtils.ENTRY_LAYOUT to layoutJson(),
                )
            )
        )
    }

    @Test
    fun validateContainerRejectsBrokenContent() {
        // 随便一个文本/图片（不是 zip）
        assertEquals(
            LauncherTaskError.INVALID_FILE,
            LauncherBackupUtils.validateContainer(ByteArrayInputStream("not a backup".toByteArray())),
        )
        assertEquals(LauncherTaskError.INVALID_FILE, LauncherBackupUtils.validateContainer(null as Map<String, String>?))
        // 只写了 meta.json，缺 layout.json
        val metaOnly = ByteArrayOutputStream()
        LauncherBackupUtils.writeContainer(metaOnly, metaJson(), "")
        assertEquals(
            LauncherTaskError.INVALID_FILE,
            LauncherBackupUtils.validateContainer(ByteArrayInputStream(metaOnly.toByteArray())),
        )
        // 条目齐全但 meta 版本不是 1
        assertEquals(
            LauncherTaskError.INVALID_FILE,
            LauncherBackupUtils.validateContainer(
                linkedMapOf(
                    LauncherBackupUtils.ENTRY_META to metaJson(version = 2),
                    LauncherBackupUtils.ENTRY_LAYOUT to layoutJson(),
                )
            ),
        )
        // 条目齐全但 layout 里没有 tables
        assertEquals(
            LauncherTaskError.INVALID_FILE,
            LauncherBackupUtils.validateContainer(
                linkedMapOf(
                    LauncherBackupUtils.ENTRY_META to metaJson(),
                    LauncherBackupUtils.ENTRY_LAYOUT to "{\"hidden\":{}}",
                )
            ),
        )
    }

    // ---------------- 保留策略 ----------------

    @Test
    fun planEvictionsKeepsEverythingUnderLimit() {
        val snapshots = (0 until 5).map { index -> snapshot(BASE + index * HOUR, LauncherSnapshotTag.MANUAL) }
        assertTrue(LauncherBackupUtils.planEvictions(snapshots, 20).isEmpty())
        assertTrue(LauncherBackupUtils.planEvictions(snapshots, 5).isEmpty())
    }

    @Test
    fun planEvictionsDeletesOldestAutoFirst() {
        // 12 份 auto + 10 份 manual，超限 2：只能删最旧的两份 auto
        val autos = (0 until 12).map { index -> snapshot(BASE + index * HOUR, LauncherSnapshotTag.AUTO) }
        val manuals = (0 until 10).map { index -> snapshot(BASE + 100 * HOUR + index * HOUR, LauncherSnapshotTag.MANUAL) }
        val picked = LauncherBackupUtils.planEvictions(autos + manuals, 20)
        assertEquals(2, picked.size)
        assertTrue(picked.all { it.tag == LauncherSnapshotTag.AUTO })
        assertEquals(autos.take(2).map { it.name }, picked.map { it.name })
    }

    @Test
    fun planEvictionsFallsBackToManualAndPreRestore() {
        // 全是 manual/pre_restore 时按年龄删最旧（不再区分标签）
        val manuals = (0 until 10).map { index -> snapshot(BASE + index * HOUR, LauncherSnapshotTag.MANUAL) }
        val preRestores = (0 until 12).map { index -> snapshot(BASE + 100 * HOUR + index * HOUR, LauncherSnapshotTag.PRE_RESTORE) }
        val picked = LauncherBackupUtils.planEvictions(manuals + preRestores, 20)
        assertEquals(2, picked.size)
        assertEquals(manuals.take(2).map { it.name }, picked.map { it.name })
    }

    @Test
    fun planEvictionsHandlesDegenerateLimits() {
        val snapshots = (0 until 5).map { index -> snapshot(BASE + index * HOUR, LauncherSnapshotTag.MANUAL) }
        assertTrue(LauncherBackupUtils.planEvictions(snapshots, 0).isEmpty())
        assertEquals(4, LauncherBackupUtils.planEvictions(snapshots, 1).size)
        assertTrue(LauncherBackupUtils.planEvictions(emptyList(), 20).isEmpty())
    }

    @Test
    fun planEvictionsNeverDeletesExcludedSnapshot() {
        val autos = (0 until 12).map { index -> snapshot(BASE + index * HOUR, LauncherSnapshotTag.AUTO) }
        val manuals = (0 until 10).map { index -> snapshot(BASE + 100 * HOUR + index * HOUR, LauncherSnapshotTag.MANUAL) }
        val excluded = setOf(autos.first().name)
        val picked = LauncherBackupUtils.planEvictions(autos + manuals, 20, excluded)
        assertEquals(2, picked.size)
        assertFalse(picked.any { it.name in excluded })
        assertEquals(autos[1].name, picked.first().name)
    }

    // ---------------- 自动备份周期 ----------------

    @Test
    fun intervalHoursFallsBackToDefault() {
        assertEquals(6, LauncherBackupPrefs.intervalHours("6"))
        assertEquals(48, LauncherBackupPrefs.intervalHours("48"))
        assertEquals(24, LauncherBackupPrefs.intervalHours(null))
        assertEquals(24, LauncherBackupPrefs.intervalHours(""))
        assertEquals(24, LauncherBackupPrefs.intervalHours("7"))
        assertEquals(24, LauncherBackupPrefs.intervalHours("abc"))
    }

    // ---------------- 隐藏应用状态 ----------------

    @Test
    fun hiddenKeyRoundTrip() {
        val key = LauncherBackupUtils.hiddenKey("com.demo.app", 0)
        assertEquals("com.demo.app#0", key)
        val parsed = requireNotNull(LauncherBackupUtils.parseHiddenKey(key))
        assertEquals("com.demo.app", parsed.packageName)
        assertEquals(0, parsed.userId)
        assertNull(LauncherBackupUtils.parseHiddenKey(null))
        assertNull(LauncherBackupUtils.parseHiddenKey("no-separator"))
        assertNull(LauncherBackupUtils.parseHiddenKey("#0"))
        assertNull(LauncherBackupUtils.parseHiddenKey("com.demo#"))
        assertNull(LauncherBackupUtils.parseHiddenKey("com.demo#abc"))
    }

    @Test
    fun parseHiddenPrefsXmlKeepsRawRank() {
        val xml = """
            <?xml version='1.0' encoding='utf-8' standalone='yes' ?>
            <map>
                <int name="com.demo.a#0" value="7" />
                <int name="com.demo.b#0" value="-1" />
                <string name="unrelated">x</string>
                <int name="garbage" value="3" />
            </map>
        """.trimIndent()
        val parsed = LauncherBackupUtils.parseHiddenPrefsXml(xml)
        assertEquals(mapOf("com.demo.a#0" to 7, "com.demo.b#0" to -1), parsed)
        assertTrue(LauncherBackupUtils.parseHiddenPrefsXml(null).isEmpty())
        assertTrue(LauncherBackupUtils.parseHiddenPrefsXml("").isEmpty())
    }

    @Test
    fun planHiddenSyncDiffsBothDirections() {
        val snapshot = mapOf("com.demo.a#0" to 1, "com.demo.b#0" to 2)
        val current = mapOf("com.demo.b#0" to 2, "com.demo.c#0" to 5)
        val sync = LauncherBackupUtils.planHiddenSync(snapshot, current)
        assertEquals(listOf("com.demo.a#0"), sync.toHide.map { LauncherBackupUtils.hiddenKey(it.packageName, it.userId) })
        assertEquals(listOf("com.demo.c#0"), sync.toUnhide.map { LauncherBackupUtils.hiddenKey(it.packageName, it.userId) })

        // rank 变化不算差异：launcher 自己会重排
        val reranked = LauncherBackupUtils.planHiddenSync(mapOf("com.demo.a#0" to 9), mapOf("com.demo.a#0" to 1))
        assertTrue(reranked.toHide.isEmpty())
        assertTrue(reranked.toUnhide.isEmpty())
    }
}
