package com.luckyzyx.luckytool.hook.scopes.launcher

import android.content.ContentProviderClient
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.core.view.isVisible
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.extension.kavaref.toClassOrNull
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class ForceEnableRecentTaskMemoryDisplay(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

    private companion object {
        const val PANEL = "com.oplus.quickstep.views.OplusClearAllPanelView"
        const val ROM2RAM_METHOD = "get_rom2ram_pinned_bytes"
        const val ROM2RAM_KEY = "pinned_bytes"
    }

    private val memoryReadState = ThreadLocal<MemoryReadState>()

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

            // 17.0.1 的 getIsMem 直接读这个布尔字段；17.0.0 的 getIsMem 调一个 0 参 boolean 方法，由后者读字段
            val displaySwitch = dexKitBridge.findField {
                matcher {
                    declaredClass(memoryManager)
                    addReadMethod {
                        declaredClass(PANEL)
                        name("getIsMem")
                    }
                }
            }.singleOrNull()?.name ?: dexKitBridge.findField {
                matcher {
                    declaredClass(memoryManager)
                    type(classOf<Boolean>())
                    addReadMethod {
                        paramCount(0)
                        returnType(classOf<Boolean>())
                        addCaller {
                            declaredClass(PANEL)
                            name("getIsMem")
                        }
                    }
                }
            }.apply {
                checkDataList("displaySwitch")
            }.singleOrNull()?.name ?: return
            val switchField = memoryManager.toClass().resolve().firstField { name = displaySwitch }

            // 展开时官方 setOverviewStateEnabled 只认这个允许位，为 false 时不会触发内存重算
            val allowSwitch = dexKitBridge.findField {
                matcher {
                    declaredClass(memoryManager)
                    type(classOf<Boolean>())
                    addReadMethod { name("setOverviewStateEnabled") }
                }
            }.singleOrNull()?.name?.let { name ->
                memoryManager.toClass().resolve().firstField { this.name = name }
            }

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

            // 每次取单例都放开两个状态位，官方展开时才会自己走后台重算并刷新文本
            dexKitBridge.findMethod {
                matcher {
                    declaredClass(memoryManager)
                    paramTypes(classOf<Context>())
                    returnType(memoryManager)
                }
            }.singleOrNull()?.getMethodInstance(hostClassLoader!!)?.hook {
                after {
                    val manager = result<Any?>() ?: return@after
                    switchField.copy().of(manager).set(true)
                    allowSwitch?.copy()?.of(manager)?.set(true)
                }
            }

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

            // 官方内存计算方法：第二段可用内存已经包含内存拓展和 rom2ram
            val memoryMethod = findMethod {
                matcher {
                    paramTypes(classOf<Boolean>())
                    usingStrings(
                        "persist.sys.oplus.nandswap.condition",
                        "get_rom2ram_pinned_bytes", "MemAvailable:"
                    )
                    addInvoke {
                        name("readProcLines")
                        paramTypes(classOf<String>(), classOf<Array<String>>(), classOf<LongArray>())
                    }
                }
            }.apply {
                checkDataList("getMemoryInfo")
            }.singleOrNull() ?: return@apply

            // c(Z) 内唯一写入的 long 字段保存内存拓展 GB 数
            val expansionSwitch = findField {
                matcher {
                    declaredClass(memoryManager)
                    type(classOf<Long>())
                    addWriteMethod {
                        name(memoryMethod.methodName)
                        paramTypes(classOf<Boolean>())
                    }
                }
            }.apply {
                checkDataList("expansionSwitch")
            }.singleOrNull()?.name ?: return@apply
            val expansionField = memoryManager.toClass().resolve().firstField { name = expansionSwitch }

            val loader = hostClassLoader ?: return@apply
            val conversionMethod = "com.oplus.util.OplusUnitConversionUtils".toClassOrNull(loader)
                ?.resolve()?.firstMethodOrNull {
                    name = "getUnitValue"
                    parameters(Long::class)
                    returnType = String::class
                } ?: return@apply
            val providerCall = ContentProviderClient::class.java.resolve().firstMethodOrNull {
                name = "call"
                parameters(String::class, String::class, Bundle::class)
                returnType = Bundle::class
            } ?: return@apply

            // 捕获官方 rom2ram 返回值
            providerCall.hook {
                after {
                    val state = memoryReadState.get() ?: return@after
                    if (arg(0).get<String>() != ROM2RAM_METHOD) return@after
                    state.rom2RamBytes = result<Bundle>()?.getLong(ROM2RAM_KEY, 0L) ?: 0L
                }
            }

            // 第二段可用入参 = MemAvailable + 内存拓展 + rom2ram
            conversionMethod.hook {
                before {
                    val state = memoryReadState.get() ?: return@before
                    if (++state.conversionIndex != 2) return@before
                    val expansionBytes = expansionField.copy().of(state.manager)
                        .get<Long>()?.let { it shl 30 } ?: return@before
                    val officialBytes = firstArg().get<Long>() ?: return@before
                    firstArg().set(officialBytes - expansionBytes - state.rom2RamBytes)
                }
            }

            memoryMethod.getMethodInstance(hostClassLoader!!).hook {
                before { memoryReadState.set(MemoryReadState(instance<Any>())) }
                after { memoryReadState.remove() }
            }

        }
    }

    private class MemoryReadState(val manager: Any) {
        var rom2RamBytes = 0L
        var conversionIndex = 0
    }
}
