package com.luckyzyx.luckytool.hook.scopes.launcher

import android.view.View
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object CustomDesktopDefaultHomePage : YukiBaseHooker() {
    override fun onHook() {
        val page = preferences(ModulePrefs).getString("custom_desktop_default_home_page", "0")
        val targetPage = page.toIntOrNull() ?: return
        if (targetPage < 0) return

        val workspaceClass = "com.android.launcher3.Workspace".toClass().resolve()
        // 首页字段与切页方法在各版本桌面里都在，取不到说明 hook 点已变化，
        // 交给 KavaRef 抛出，避免静默不生效
        val defaultPage = workspaceClass.firstField { name = "DEFAULT_PAGE" }
        val currentPage = workspaceClass.firstFieldOrNull { name = "mCurrentPage"; superclass() }

        val pagedViewClass = "com.android.launcher3.PagedView".toClass().resolve()
        val getNextPage = pagedViewClass.firstMethod { name = "getNextPage"; emptyParameters() }
        val snapToPage = pagedViewClass.firstMethod { name = "snapToPage"; parameterCount = 1 }

        // 把当前页切到用户设定值，并回写 DEFAULT_PAGE 供宿主的其它判定读取
        // 解析器只能绑定一次实例，这里的调用点在多个回调里复用同一份解析器，
        // 所以每次都用 copy() 取新解析器，否则第二次会抛 Instance already set
        fun applyTarget(instance: Any?) {
            val next = getNextPage.copy().of(instance).invoke<Int>() ?: return
            if (next == targetPage) return
            snapToPage.copy().of(instance).invoke(targetPage)
        }

        // 启动初始化：宿主 17 之前的实现在 initWorkspace() 里定首页，新版没有这个方法
        workspaceClass.firstMethodOrNull { name = "initWorkspace"; emptyParameters() }?.hook {
            before { defaultPage.set(targetPage) }
            after {
                currentPage?.copy()?.of(instance)?.set(targetPage)
            }
        }

        // 启动落地：宿主 17 起首页在 Workspace 构造里定，构造时页面还没有测量，
        // 改在进程内首次 resume 结束后校正一次；之后再翻页不会被拽回
        var applied = false
        val launcherClass = "com.android.launcher3.Launcher".toClass().resolve()
        val getWorkspace = launcherClass.firstMethod { name = "getWorkspace"; emptyParameters() }
        launcherClass.firstMethod { name = "onResume"; emptyParameters() }.hook {
            after {
                if (applied) return@after
                applied = true
                defaultPage.set(targetPage)
                val workspace = getWorkspace.of(instance).invoke<Any>()
                (workspace as? View)?.post { applyTarget(workspace) }
            }
        }

        // 回到默认页：宿主在 moveToDefaultScreen() 内用自己的规则覆盖 DEFAULT_PAGE 并切页，
        // 所以在该方法结束后再切一次，并回写 DEFAULT_PAGE
        val moveToDefaultScreen =
            "com.android.launcher3.OplusWorkspace".toClassOrNull()?.resolve()
                ?.firstMethodOrNull { name = "moveToDefaultScreen"; emptyParameters() }
                ?: "com.android.launcher3.Workspace".toClass().resolve()
                    .firstMethod { name = "moveToDefaultScreen"; emptyParameters() }
        moveToDefaultScreen.hook {
            before { defaultPage.set(targetPage) }
            after {
                defaultPage.set(targetPage)
                applyTarget(instance)
            }
        }
    }
}