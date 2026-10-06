package com.luckyzyx.luckytool.hook.scopes.market

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.DexkitUtils.checkDataList
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge

@Obfuscate
class DefaultExpandUpdateList(val dexKitBridge: DexKitBridge) : YukiBaseHooker() {
    override fun onHook() {
        //Source UpdateListAdapter ExpUpdateListAdapter (待更新页, 由实验开关二选一)
        listOf(
            "com.heytap.market.appmanage.core.upgrade.upgrademgr.recyclerview.UpdateListAdapter",
            "com.heytap.market.appmanage.core.upgrade.upgrademgr.recyclerview.ExpUpdateListAdapter"
        ).forEach { clazzName ->
            clazzName.toClassOrNull() ?: return@forEach
            val clazzData = dexKitBridge.findClass {
                matcher { className(clazzName) }
            }.apply {
                checkDataList("DefaultExpandUpdateList $clazzName")
            }
            //数据入口 mo78870(List): if (isMinorsMode() || list.size() <= N) expandFlag = true
            //默认仅在 <=N 条时展开, hook 其 before 把展开标志置 true 实现默认展开
            val setData = clazzData.findMethod {
                matcher {
                    paramTypes(classOf<List<*>>())
                    addInvoke { name("isMinorsMode") }
                }
            }.apply {
                checkDataList("DefaultExpandUpdateList $clazzName setData")
            }.single()
            //展开标志 boolean 字段, 仅在该方法中写入 true
            val flag = clazzData.findField {
                matcher {
                    type(classOf<Boolean>())
                    addWriteMethod { name(setData.methodName) }
                }
            }.apply {
                checkDataList("DefaultExpandUpdateList $clazzName expandFlag")
            }.single()
            setData.className.toClass().resolve().apply {
                firstMethod {
                    name = setData.methodName
                    parameters(List::class)
                }.hook {
                    before {
                        firstField { name = flag.fieldName }.of(instance).set(true)
                    }
                }
            }
        }
    }
}
