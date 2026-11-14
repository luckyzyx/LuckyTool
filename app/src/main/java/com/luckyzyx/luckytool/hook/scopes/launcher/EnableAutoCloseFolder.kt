package com.luckyzyx.luckytool.hook.scopes.launcher

import android.view.ViewGroup
import androidx.core.view.children
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.isNotSubclassOf
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object EnableAutoCloseFolder : YukiBaseHooker() {
    /** 宿主没有声明文件夹类型位时的取值，与 Folder.isOfType 的判据（type and 1）一致 */
    private const val FOLDER_TYPE = 1

    override fun onHook() {
        //Source AbstractFloatingView
        val floatingViewClass = "com.android.launcher3.AbstractFloatingView".toClass()
        floatingViewClass.resolve().apply {
            firstMethod {
                name = "closeOpenViews"
                parameterCount = 4
            }.hook {
                before {
                    val activityContext = firstArg().get() ?: return@before
                    val animate = arg(1).get<Boolean>() ?: false
                    val type = arg(2).get<Int>() ?: 0

                    // 调用方没要求关文件夹时不做处理
                    if ((type and FOLDER_TYPE) == 0) return@before

                    val dragLayer = activityContext.asResolver().firstMethod {
                        name = "getDragLayer"; superclass()
                    }.invoke<ViewGroup>() ?: return@before
                    dragLayer.children.forEachIndexed { _, view ->
                        if (view::class isNotSubclassOf floatingViewClass) return@forEachIndexed
                        val isOfType = view.asResolver().firstMethod {
                            name = "isOfType"; superclass()
                        }.invoke<Boolean>(FOLDER_TYPE) ?: false
                        if (isOfType) {
                            view.asResolver().firstMethod {
                                name = "close"; parameterCount = 1; superclass()
                            }.invoke(animate)
                        }
                    }
                }
            }
        }
    }
}