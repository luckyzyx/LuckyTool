package com.luckyzyx.luckytool.hook.scopes.health

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.extension.kavaref.hook
import com.highcapable.yukihookapi.extension.kavaref.toClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.log.YLog
import com.luckyzyx.luckytool.utils.DexkitUtils
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.query.enums.StringMatchType
import org.lsposed.lsparanoid.Obfuscate

/**
 * 移除健康 App 检测到设备已 Root 后弹出的对话框。
 *
 * 定位不写死类名：safety.safetycheck 包的外层类已被混淆成单字母（原 SafetyCheckManager 现在叫 g），
 * 名字每次发版都可能变。该包内只有一个「只收 Activity、返回 void、不是构造器」的方法，就是
 * check(activity)：它只负责拉起检测协程，拦掉它等于不发起检测，弹窗自然不会出现。
 */
@Obfuscate
class RemoveHealthRootCheck(private val dexKitBridge: DexKitBridge) : YukiBaseHooker() {

    override fun onHook() {
        //Source SafetyCheckManager.check(MainActivity)
        val check =
            findCheck("com.heytap.health.main.MainActivity") ?: findCheck("android.app.Activity")

        //两种签名都落空说明宿主又改了检测结构，必须留痕，否则功能静默失效
        if (check == null) {
            YLog.error("RemoveHealthRootCheck -> findMethod isNullOrEmpty", tag = DexkitUtils.tag)
            return
        }

        check.className.toClass().resolve().firstMethodOrNull {
            name = check.methodName
            parameterCount = 1
        }?.hook { intercept() }
    }

    /**
     * 按参数类型找 check(activity)。
     *
     * 宿主 6.11.7 起参数收窄成 MainActivity，更早的版本仍是 Activity，两种签名各查一次。
     * 必须排掉构造器：同包里 d 的构造器 <init>(Activity) 签名完全一样，钩上它会在弹窗出现时炸掉监听器。
     * 同一份代码可能落在多个 dex 里，按「类名+方法名」去重后再要求唯一。
     */
    private fun findCheck(paramType: String) = dexKitBridge.findMethod {
        matcher {
            declaredClass("com.heytap.health.safety.safetycheck", StringMatchType.StartsWith)
            paramTypes(paramType)
            returnType(Void.TYPE)
        }
    }.filterNot { it.methodName.startsWith("<") }
        .distinctBy { it.className to it.methodName }
        .singleOrNull()
}
