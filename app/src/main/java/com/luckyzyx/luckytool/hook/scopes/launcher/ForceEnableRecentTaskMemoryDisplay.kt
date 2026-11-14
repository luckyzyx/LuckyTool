package com.luckyzyx.luckytool.hook.scopes.launcher

import android.widget.TextView
import androidx.core.view.isVisible
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class ForceEnableRecentTaskMemoryDisplay(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

    private companion object {
        const val PANEL = "com.oplus.quickstep.views.OplusClearAllPanelView"
    }

    override fun onHook() {
        //Source MemoryInfoManager
        // 这一行显示与否只看这个类里的展示开关：旧版是两个方法，17.x 起合并成一个布尔字段；
        // DexKit 桥在本回调返回后就关闭，所有查询都必须在这里做完，hook 里只能用手上的解析结果
        dexKitBridge.findClass {
            matcher {
                usingStrings("MemoryInfoManager")
            }
        }.apply {
            checkDataList("MemoryInfoManager")

            val memoryManager = single().name
            val legacy = findMethod {
                matcher {
                    name("needMemoryDetail")
                }
            }
            if (legacy.isNotEmpty()) {
                legacy.single().getMethodInstance(hostClassLoader!!).hook { intercept(true) }
                findMethod {
                    matcher {
                        name("isAllowMemoryInfoDisplay")
                    }
                }.apply {
                    checkDataList("isAllowMemoryInfoDisplay")
                }.single().getMethodInstance(hostClassLoader!!).hook { intercept(true) }
                return
            }

            // 17.x：面板通过 getIsMem 读这个布尔字段，决定要不要留出内存那一行
            val displaySwitch = dexKitBridge.findField {
                matcher {
                    declaredClass(memoryManager)
                    addReadMethod {
                        declaredClass(PANEL)
                        name("getIsMem")
                    }
                }
            }.apply {
                checkDataList("displaySwitch")
            }.single().name
            val switchField = memoryManager.toClass().resolve().firstField { name = displaySwitch }

            // 单例可能在本 hook 安装前就创建好，先定位它（类里唯一的自引用静态字段）
            val singletonSwitch = dexKitBridge.findField {
                matcher {
                    declaredClass(memoryManager)
                    type(memoryManager.toClass())
                }
            }.apply {
                checkDataList("memoryManagerSingleton")
            }.singleOrNull()?.name?.let { name ->
                memoryManager.toClass().resolve().firstField { this.name = name }
            }

            fun forceSwitch() =
                singletonSwitch?.get<Any?>()?.let { switchField.copy().of(it).set(true) }

            // 内存信息对象与其中那串文本：方法按 boolean 入参定位，返回类里取唯一的 String 字段
            val memInfo = dexKitBridge.findMethod {
                matcher {
                    declaredClass(memoryManager)
                    paramTypes(classOf<Boolean>())
                }
            }.singleOrNull()?.let { method ->
                val owner = memoryManager.toClass().resolve()
                owner.firstMethod { name = method.methodName; parameters(Boolean::class) } to
                        method.returnTypeName.removePrefix("L").removeSuffix(";").replace('/', '.')
            }
            val memInfoText = memInfo?.second?.let { infoClass ->
                dexKitBridge.findField {
                    matcher {
                        declaredClass(infoClass)
                        type(classOf<String>())
                    }
                }.singleOrNull()?.name?.let { name ->
                    infoClass.toClass().resolve().firstField { this.name = name }
                }
            }

            val panelUpdate = dexKitBridge.findMethod {
                matcher {
                    declaredClass(PANEL)
                    paramTypes(classOf<Boolean>())
                    returnType(Void.TYPE)
                }
            }.singleOrNull()?.getMethodInstance(hostClassLoader!!)
            val memText = dexKitBridge.findField {
                matcher {
                    declaredClass(PANEL)
                    type(classOf<TextView>())
                }
            }.singleOrNull()?.name?.let { name ->
                PANEL.toClass().resolve().firstField { this.name = name }
            }

            fun showMemoryInfo(panel: Any) {
                forceSwitch()
                val tv = memText?.copy()?.of(panel)?.get<TextView>() ?: return
                if (tv.visibility == 0) return
                val instance = singletonSwitch?.get<Any?>()
                val text = if (instance != null && memInfo != null) {
                    val info = memInfo.first.copy().of(instance).invoke(false)
                    info?.let { memInfoText?.copy()?.of(it)?.get<String>() }
                } else null
                if (!text.isNullOrEmpty()) {
                    tv.text = text
                    tv.isVisible = true
                } else panelUpdate?.invoke(false)
            }

            // 宿主只在面板重建时填文本，已存在的面板会一直隐藏，所以读开关与显示动画时各补一次
            dexKitBridge.findMethod {
                matcher {
                    declaredClass(PANEL)
                    name("getIsMem")
                }
            }.singleOrNull()?.getMethodInstance(hostClassLoader!!)?.hook {
                before { showMemoryInfo(instance<Any>()) }
            }
            dexKitBridge.findMethod {
                matcher {
                    declaredClass(PANEL)
                    name("setVisibilityAlpha")
                }
            }.singleOrNull()?.getMethodInstance(hostClassLoader!!)?.hook {
                after { showMemoryInfo(instance<Any>()) }
            }

        }
    }
}