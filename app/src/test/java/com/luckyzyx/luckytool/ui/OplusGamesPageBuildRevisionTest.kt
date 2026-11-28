package com.luckyzyx.luckytool.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.os.Bundle
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.luckyzyx.luckytool.ui.compose.components.PrefPageBuilder
import com.luckyzyx.luckytool.ui.compose.scopes.apps.OplusGamesPage
import com.luckyzyx.luckytool.utils.ModulePrefs
import com.luckyzyx.luckytool.utils.PrefState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 冻结「DSL 构建 lambda 不得写入 prefs」这一语义（游戏助手页点进去卡死的根因）。
 *
 * PageScreen 每次重组都会重跑构建（`builder.content()`），并且订阅 `PrefState.revision`；
 * 而 remote prefs（libxposed RemotePreferences）的变化回调不做值比较 —— 只要发生一次 commit，
 * 每个被写入的键都会通知监听者，revision 随 +1，重组再跑一次构建。游戏助手页在构建里调用
 * `AppUtils.getAppVerInfo()`（`save` 默认 true）把版本信息写回 SP：写 → revision 自增 → 重组 → 再写，
 * 主线程无限重组，表现为点进页面就卡死、页面永远打不开。
 *
 * 注意这里必须用 [NonDedupPrefs]：本地 SharedPreferences 会把「值没变」的 commit 直接吞掉
 * （`commitToMemory()` 去重，回调都不发），所以在真机（走 remote prefs）上才会死循环的现象，
 * 拿本地 SP 是测不出来的。修复后同一个版本信息重复读取不再产生写入：第一次构建仍落盘一次
 * （多一次重组即稳定），第二次构建不得再让 revision 前进。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OplusGamesPageBuildRevisionTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var fake: SharedPreferences
    private lateinit var pageContext: Context

    @Before
    fun setUp() {
        val raw = context.getSharedPreferences(ModulePrefs, Context.MODE_PRIVATE)
        raw.edit().clear().commit()
        PrefState.invalidate(ModulePrefs)
        fake = NonDedupPrefs(raw)
        pageContext = PrefsOverrideContext(context, ModulePrefs, fake)
        // getAppVerInfo 需要 metaData 里的 versionCommit/versionDate 才会走到写回分支
        Shadows.shadowOf(context.packageManager).installPackage(
            PackageInfo().apply {
                packageName = "com.oplus.games"
                versionName = "15.0.0"
                longVersionCode = 150000000L
                applicationInfo = ApplicationInfo().apply {
                    packageName = "com.oplus.games"
                    metaData = Bundle().apply { putString("versionCommit", "testcommit") }
                }
            },
        )
    }

    /** 复刻 PageScreen 的构建流程，返回本轮结束后的 revision */
    private fun buildOnce(state: PrefState): Long {
        val builder = PrefPageBuilder(state)
        builder.context = pageContext
        builder.beginBuild()
        OplusGamesPage.spec.content(builder)
        builder.computeSegments()
        // prefs 回调可能被 post 到主线程，先跑完再读 revision
        Shadows.shadowOf(Looper.getMainLooper()).idle()
        return state.revision.value
    }

    @Test
    fun `repeated build must not bump revision again`() {
        val state = PrefState.ofProvider { fake }

        buildOnce(state)
        assertTrue(
            "第一次构建应把版本信息写进 SP，否则本用例是空跑",
            fake.getStringSet("com.oplus.games", emptySet())?.isNotEmpty() == true,
        )

        val afterFirst = state.revision.value
        buildOnce(state)
        assertEquals(
            "第二次构建写入了相同的版本信息：revision 会随重组无限自增，游戏助手页点进去即卡死",
            afterFirst,
            state.revision.value,
        )
    }
}

/**
 * 只做一件事的 prefs 替身：把 [delegate] 的读写原样转发，但 commit/apply 后**不比较值**，
 * 一律按写入的键通知监听者 —— 复刻 libxposed RemotePreferences.doUpdate() 的语义。
 */
private class NonDedupPrefs(private val delegate: SharedPreferences) : SharedPreferences {

    private val listeners =
        CopyOnWriteArrayList<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): MutableMap<String, *> = delegate.all

    override fun getString(key: String, defValue: String?): String? =
        delegate.getString(key, defValue)

    override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? =
        delegate.getStringSet(key, defValues)

    override fun getInt(key: String, defValue: Int): Int = delegate.getInt(key, defValue)

    override fun getLong(key: String, defValue: Long): Long = delegate.getLong(key, defValue)

    override fun getFloat(key: String, defValue: Float): Float = delegate.getFloat(key, defValue)

    override fun getBoolean(key: String, defValue: Boolean): Boolean =
        delegate.getBoolean(key, defValue)

    override fun contains(key: String): Boolean = delegate.contains(key)

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) {
        listeners.add(listener)
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener,
    ) {
        listeners.remove(listener)
    }

    private inner class Editor : SharedPreferences.Editor {

        private val target = delegate.edit()
        private val touched = LinkedHashSet<String>()
        private var cleared = false

        override fun putString(key: String, value: String?) = apply {
            target.putString(key, value)
            touched.add(key)
        }

        override fun putStringSet(key: String, values: MutableSet<String>?) = apply {
            target.putStringSet(key, values)
            touched.add(key)
        }

        override fun putInt(key: String, value: Int) = apply {
            target.putInt(key, value)
            touched.add(key)
        }

        override fun putLong(key: String, value: Long) = apply {
            target.putLong(key, value)
            touched.add(key)
        }

        override fun putFloat(key: String, value: Float) = apply {
            target.putFloat(key, value)
            touched.add(key)
        }

        override fun putBoolean(key: String, value: Boolean) = apply {
            target.putBoolean(key, value)
            touched.add(key)
        }

        override fun remove(key: String) = apply {
            target.remove(key)
            touched.add(key)
        }

        override fun clear() = apply {
            target.clear()
            cleared = true
        }

        override fun commit(): Boolean {
            val ok = target.commit()
            notifyListeners(ok)
            return ok
        }

        override fun apply() {
            target.apply()
            notifyListeners(true)
        }

        private fun notifyListeners(committed: Boolean) {
            if (!committed) return
            val owner = this@NonDedupPrefs
            if (cleared) listeners.forEach { it.onSharedPreferenceChanged(owner, null) }
            touched.forEach { key -> listeners.forEach { it.onSharedPreferenceChanged(owner, key) } }
        }
    }
}

/** 只把 [ModulePrefs] 的取用改到替身上，其余（包管理器、资源等）仍走真实 Context */
private class PrefsOverrideContext(
    base: Context,
    private val prefsName: String,
    private val prefs: SharedPreferences,
) : ContextWrapper(base) {
    override fun getSharedPreferences(name: String, mode: Int): SharedPreferences =
        if (name == prefsName) prefs else super.getSharedPreferences(name, mode)
}
