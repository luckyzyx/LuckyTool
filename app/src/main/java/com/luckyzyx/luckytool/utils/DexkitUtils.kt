package com.luckyzyx.luckytool.utils

import android.annotation.SuppressLint
import com.highcapable.yukihookapi.hook.log.YLog
import org.lsposed.lsparanoid.Obfuscate
import org.luckypray.dexkit.DexKitBridge
import org.luckypray.dexkit.result.ClassDataList
import org.luckypray.dexkit.result.FieldDataList
import org.luckypray.dexkit.result.MethodDataList

@Obfuscate
object DexkitUtils {
    val tag = "LuckyTool"

    /**
     * 创建Dexkit实例
     * @param appPath String
     * @return DexKitBridge?
     */
    fun create(appPath: String): DexKitBridge {
        System.loadLibrary("dexkit")
        return DexKitBridge.create(appPath)
    }

    /**
     * 创建Dexkit安全实例
     * @param appPath String
     * @param result Function1<DexKitBridge, Unit>
     */
    @SuppressLint("DuplicateCreateDexKit")
    fun create(appPath: String, result: (DexKitBridge) -> Unit) {
        System.loadLibrary("dexkit")
        DexKitBridge.create(appPath).use { result(it) }
    }

    /**
     * 检查搜索到的类列表并打印LOG
     * @receiver ClassDataList
     * @param instance String
     * @param onlyOne Boolean
     * @param isDebug Boolean
     * @return ClassDataList
     */
    fun ClassDataList.checkDataList(
        instance: String, onlyOne: Boolean = true, isDebug: Boolean = false
    ): ClassDataList {
        when {
            isNullOrEmpty() -> YLog.error("$instance -> findClass isNullOrEmpty", tag = tag)
            size != 1 && (isDebug || onlyOne) -> {
                if (isDebug) {
                    YLog.debug(
                        "$instance -> findClass size ($size) | onlyOne: $onlyOne", tag = tag
                    )
                } else YLog.error("$instance -> findClass size ($size)", tag = tag)
                if (isDebug) forEachIndexed { index, it ->
                    YLog.debug("$instance -> findClass ($index) | ${it.name}", tag = tag)
                }
            }

            size == 1 -> if (isDebug) YLog.debug(
                "$instance -> findClass ${single().name}", tag = tag
            )
        }
        return this
    }

    /**
     * 检查搜索到的方法列表并打印LOG
     * @receiver MethodDataList
     * @param instance String
     * @param onlyOne Boolean
     * @param isDebug Boolean
     * @return MethodDataList
     */
    fun MethodDataList.checkDataList(
        instance: String, onlyOne: Boolean = true, isDebug: Boolean = false
    ): MethodDataList {
        when {
            isNullOrEmpty() -> YLog.error("$instance -> findMethod isNullOrEmpty", tag = tag)
            size != 1 && (isDebug || onlyOne) -> {
                if (isDebug) {
                    YLog.debug(
                        "$instance -> findMethod size ($size) | onlyOne: $onlyOne", tag = tag
                    )
                } else YLog.error("$instance -> findMethod size ($size)", tag = tag)
                if (isDebug) forEachIndexed { index, it ->
                    YLog.debug(
                        "$instance -> findMethod ($index) | ${it.className} | ${it.methodName}",
                        tag = tag
                    )
                }
            }

            size == 1 -> if (isDebug) {
                YLog.debug(
                    "$instance -> findMethod Method -> ${single().className} | ${single().methodName}",
                    tag = tag
                )
                YLog.debug(
                    "$instance -> findMethod Type -> ${single().paramTypeNames} | ${single().returnTypeName}",
                    tag = tag
                )
            }
        }
        return this
    }

    /**
     * 检查搜索到的Field列表并打印LOG
     * @receiver FieldDataList
     * @param instance String
     * @param onlyOne Boolean
     * @param isDebug Boolean
     * @return FieldDataList
     */
    fun FieldDataList.checkDataList(
        instance: String, onlyOne: Boolean = true, isDebug: Boolean = false
    ): FieldDataList {
        when {
            isNullOrEmpty() -> YLog.error("$instance -> findField isNullOrEmpty", tag = tag)
            size != 1 && (isDebug || onlyOne) -> {
                if (isDebug) {
                    YLog.debug(
                        "$instance -> findField size ($size) | onlyOne: $onlyOne", tag = tag
                    )
                } else YLog.error("$instance -> findField size ($size)", tag = tag)
                if (isDebug) forEachIndexed { index, it ->
                    YLog.debug(
                        "$instance -> findField ($index) | ${it.className} | ${it.fieldName} | ${it.typeName}",
                        tag = tag
                    )
                }
            }

            size == 1 -> if (isDebug) {
                YLog.debug("$instance -> findField Class -> ${single().className}", tag = tag)
                YLog.debug(
                    "$instance -> findField Field -> ${single().fieldName} | ${single().typeName}",
                    tag = tag
                )
            }
        }
        return this
    }
}