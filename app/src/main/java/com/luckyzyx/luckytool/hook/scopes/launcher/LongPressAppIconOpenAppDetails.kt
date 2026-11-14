package com.luckyzyx.luckytool.hook.scopes.launcher

import android.content.Intent
import android.view.View
import android.widget.TextView
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.AppUtils
import com.luckyzyx.luckytool.utils.SDK
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object LongPressAppIconOpenAppDetails : YukiBaseHooker() {

    private const val TASK_HEADER = "com.oplus.quickstep.views.OplusTaskHeaderView"

    override fun onHook() {
        // 任务卡头部换成 OplusTaskHeaderView 后，绑定 Task 的入口是它的 setTitle(Task)；
        // 没有这个入口的版本仍走 OplusTaskViewImpl.setIcon
        val taskHeader = TASK_HEADER.toClassOrNull()?.takeIf {
            it.resolve().firstMethodOrNull { name = "setTitle"; parameterCount = 1 } != null
        }
        if (taskHeader != null) {
            // 标题没变化时宿主内部会提前返回，但 after 仍然执行，
            // 长按监听可以在这里按当前 Task 重新绑定
            taskHeader.resolve().apply {
                firstMethod { name = "setTitle"; parameterCount = 1 }.hook {
                    after {
                        val task = firstArg().get() ?: return@after
                        val iconView = firstMethod { name = "getTaskIcon" }.of(instance)
                            .invoke<View>() ?: return@after
                        val titleView = firstMethod { name = "getTitleTv" }.of(instance)
                            .invoke<TextView>() ?: return@after
                        val key = task.asResolver().firstField { name = "key" }.get()
                            ?: return@after
                        val intent = key.asResolver().firstField { type = Intent::class }
                            .get<Intent>() ?: return@after
                        val packName = intent.component?.packageName ?: intent.`package`
                        val userId = task.asResolver().firstMethodOrNull { name = "getUserId" }
                            ?.invoke<Int>()
                            ?: key.asResolver().firstField { name = "userId" }.get<Int>()
                        iconView.setLongClick(packName, userId)
                        titleView.setLongClick(packName, userId)
                    }
                }
            }
        } else {
            //Source OplusTaskViewImpl
            "com.android.quickstep.views.OplusTaskViewImpl".toClass().resolve().apply {
                firstMethod {
                    name = "setIcon"
                    parameterCount { it in 1..2 }
                }.hook {
                    after {
                        val headerView = firstMethod { name = "getHeaderView" }.of(instance)
                            .invoke() ?: return@after
                        val iconView = headerView.asResolver().firstMethod { name = "getTaskIcon" }
                            .invoke<View>() ?: return@after
                        val titleView = headerView.asResolver().firstField {
                            name = if (SDK >= A13) "titleTv" else "mTitleView"
                        }.get<TextView>() ?: return@after
                        val task = firstMethod { name = "getTask"; superclass() }
                            .of(instance).invoke() ?: return@after
                        val key = task.asResolver().firstField { name = "key" }.get()
                            ?: return@after
                        val intent = key.asResolver().firstField { type = Intent::class }
                            .get<Intent>() ?: return@after
                        val packName = intent.component?.packageName ?: intent.`package`
                        val userId = task.asResolver().firstMethodOrNull { name = "getUserId" }
                            ?.invoke<Int>()
                            ?: key.asResolver().firstField { name = "userId" }.get<Int>()
                        iconView.setLongClick(packName, userId)
                        titleView.setLongClick(packName, userId)
                    }
                }
            }
        }

        //Source DockIconView
        "com.oplus.quickstep.dock.DockIconView".toClass().resolve().apply {
            firstMethod {
                name = "setIcon"
                parameterCount = 1
            }.hook {
                after {
                    val task =
                        firstMethod { name = "getTask" }.of(instance).invoke() ?: return@after
                    val key = task.asResolver().firstField { name = "key" }.get() ?: return@after
                    val intent = key.asResolver().firstField { type = Intent::class }.get<Intent>()
                        ?: return@after
                    val packName = intent.component?.packageName ?: intent.`package`
                    val userId =
                        task.asResolver().firstMethodOrNull { name = "getUserId" }?.invoke<Int>()
                            ?: key.asResolver().firstField { name = "userId" }.get<Int>()
                    instance<View>().setLongClick(packName, userId)
                }
            }
        }
    }

    private fun View.setLongClick(packName: String?, userId: Int? = 0) {
        setOnLongClickListener {
            packName?.let { its -> AppUtils(it.context).openAppDetailIntent(its, userId) }
            true
        }
    }
}