package com.luckyzyx.luckytool.ui.activity

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.highcapable.betterandroid.ui.extension.component.startActivity
import com.luckyzyx.luckytool.ui.compose.PageHost
import com.luckyzyx.luckytool.ui.compose.PageRoute
import com.luckyzyx.luckytool.ui.compose.pages.ThemeScreen
import com.luckyzyx.luckytool.ui.theme.AppTheme
import com.luckyzyx.luckytool.utils.ThemeUtils
import org.lsposed.lsparanoid.Obfuscate

/**
 * 二级页宿主：主题设置页与全部功能页共用一个 Activity，
 * 由 intent extra 决定本次实例渲染哪个页面；子页跳转再叠一个同类实例压栈，
 * 返回即 finish()，转场交给系统 Activity 动画。
 */
@Obfuscate
class PageActivity : AppCompatActivity() {

    companion object {

        private const val EXTRA_PAGE_TYPE = "page_type"
        private const val EXTRA_PAGE_KEY = "page_key"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_SCROLL_KEY = "scroll_key"
        private const val EXTRA_SCROLL_POSITION = "scroll_position"

        private const val PAGE_THEME = "theme"
        private const val PAGE_FEATURE = "feature"

        /** 打开主题设置页。 */
        fun openTheme(activity: Context) {
            activity.startActivity<PageActivity> {
                putExtra(EXTRA_PAGE_TYPE, PAGE_THEME)
            }
        }

        /** 打开一个功能页。 */
        fun openPage(activity: Context, pageKey: String, title: String) {
            openPage(activity, PageRoute(pageKey, title))
        }

        /** 打开一个功能页（带搜索定位参数）。 */
        fun openPage(activity: Context, route: PageRoute) {
            activity.startActivity<PageActivity> {
                putExtra(EXTRA_PAGE_TYPE, PAGE_FEATURE)
                putExtra(EXTRA_PAGE_KEY, route.pageKey)
                putExtra(EXTRA_TITLE, route.title)
                putExtra(EXTRA_SCROLL_KEY, route.scrollKey)
                putExtra(EXTRA_SCROLL_POSITION, route.scrollPosition)
            }
        }
    }

    @SuppressLint("ReplaceWithSystemBarsController")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeUtils.initTheme(this)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false

        val route = PageRoute(
            pageKey = intent.getStringExtra(EXTRA_PAGE_KEY).orEmpty(),
            title = intent.getStringExtra(EXTRA_TITLE).orEmpty(),
            scrollKey = intent.getStringExtra(EXTRA_SCROLL_KEY).orEmpty(),
            scrollPosition = intent.getIntExtra(EXTRA_SCROLL_POSITION, -1),
        )
        setContent {
            AppTheme {
                when (intent.getStringExtra(EXTRA_PAGE_TYPE)) {
                    PAGE_THEME -> ThemeScreen(onBack = { finish() })
                    else -> PageHost(
                        route = route,
                        onBack = { finish() },
                        onNavigate = { key, title ->
                            openPage(
                                this@PageActivity,
                                key,
                                title.orEmpty()
                            )
                        },
                    )
                }
            }
        }
    }
}
