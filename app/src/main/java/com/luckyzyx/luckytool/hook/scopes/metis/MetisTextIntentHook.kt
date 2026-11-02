package com.luckyzyx.luckytool.hook.scopes.metis

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.extension.kavaref.intercept
import com.highcapable.yukihookapi.extension.kavaref.toClass
import org.lsposed.lsparanoid.Obfuscate

/**
 * 卡片点击重定向的 hook 层。
 *
 * 三个入口互为备份，某个类在当前机型 / 版本不存在就跳过并记日志，绝不让进程崩：
 * - TextIntent.invoke(Context)：主路径，改 bean 字段后照样放行原调用；
 * - SchemeData / WebData.parseIntent(Context)：包名被硬编码成 com.heytap.browser 的产生点，
 *   先取返回值再改，卡片上显示的图标与名字才会跟着变对；
 * - ContextImpl.startActivity(...)：兜底，部分机型卡片根本不走前两条，只能在真正启动前改 Intent。
 *
 * 所有入口都必须原样放行原实现（proceed()），这里只负责把「打开哪个 App」换掉。
 */
@Obfuscate
object MetisTextIntentHook : YukiBaseHooker() {

    private const val TEXT_INTENT = "com.oplus.textintent.distribution.bean.TextIntent"
    private const val SCHEME_DATA = "com.oplus.textintent.distribution.bean.SchemeData"
    private const val WEB_DATA = "com.oplus.textintent.distribution.bean.WebData"
    private const val CONTEXT_IMPL = "android.app.ContextImpl"

    override fun onHook() {
        //hooker 自身就是 PackageParam，拦到之后再取会影响判定的时效性，这里先捕获下来
        val param = this
        hookTextIntent(param)
        hookParseIntent(param, SCHEME_DATA, "SchemeData.parseIntent")
        hookParseIntent(param, WEB_DATA, "WebData.parseIntent")
        hookContextImpl(param)
    }

    /** A 主路径：卡片解析完成后直接改 bean，改完放行原调用 */
    private fun hookTextIntent(param: PackageParam) {
        runCatching {
            TEXT_INTENT.toClass().resolve().apply {
                firstMethod {
                    name = "invoke"
                    parameters(Context::class)
                }.intercept {
                    val bean = instanceOrNull<Any>()
                    proceed().also {
                        if (bean != null) MetisEngine.rewriteBean(param, bean)
                    }
                }
            }
        }.onFailure { MetisLog.warn("TextIntent.invoke", "hook 安装失败: $it") }
    }

    /**
     * B 冗余入口：parseIntent 返回的就是刚造好的 TextIntent。
     * 必须先把返回值取出来再改，改 bean 本身才有意义（改方法的返回值副本没用）。
     */
    private fun hookParseIntent(param: PackageParam, className: String, from: String) {
        runCatching {
            className.toClass().resolve().apply {
                firstMethod {
                    name = "parseIntent"
                    parameters(Context::class)
                }.intercept {
                    val bean = proceed()
                    MetisEngine.rewriteBean(param, bean)
                    bean
                }
            }
        }.onFailure { MetisLog.warn(from, "hook 安装失败: $it") }
    }

    /** C 兜底入口：两个重载都要挂，少了任意一个都会有一半点击漏改 */
    private fun hookContextImpl(param: PackageParam) {
        runCatching {
            CONTEXT_IMPL.toClass().resolve().apply {
                firstMethod {
                    name = "startActivity"
                    parameters(Intent::class)
                }.intercept {
                    MetisEngine.rewriteIntent(param, arg(0) as? Intent)
                    proceed()
                }
                firstMethod {
                    name = "startActivity"
                    parameters(Intent::class, Bundle::class)
                }.intercept {
                    MetisEngine.rewriteIntent(param, arg(0) as? Intent)
                    proceed()
                }
            }
        }.onFailure { MetisLog.warn("ContextImpl.startActivity", "hook 安装失败: $it") }
    }
}
