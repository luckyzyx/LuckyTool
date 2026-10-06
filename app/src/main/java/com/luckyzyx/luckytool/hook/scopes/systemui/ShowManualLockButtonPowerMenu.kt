package com.luckyzyx.luckytool.hook.scopes.systemui

import android.annotation.SuppressLint
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.provider.Settings
import com.highcapable.kavaref.KavaRef.Companion.asResolver
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.classOf
import com.highcapable.kavaref.extension.createInstance
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.utils.sysui.DependencyUtils
import com.luckyzyx.luckytool.hook.utils.sysui.FlavorOneFeatureUtils
import com.luckyzyx.luckytool.utils.getOSVersionCode
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object ShowManualLockButtonPowerMenu : YukiBaseHooker() {

    val OsBinderCacheUtils = "com.android.systemui.oplusutils.OsBinderCacheUtils"
    val LockPatternUtils = "com.android.internal.widget.LockPatternUtils"

    override fun onHook() {
        val osCode = getOSVersionCode
        if (osCode >= 35) loadHooker(ManualLockButton)
        else loadHooker(ManualLockButtonV14)
    }

    @Obfuscate
    object ManualLockButton : YukiBaseHooker() {
        @SuppressLint("MissingPermission")
        override fun onHook() {
            //Source ShutdownViewControl
            "com.oplus.systemui.shutdown.ShutdownViewControl".toClass().resolve().apply {
                firstConstructor { parameters(Context::class) }.hook {
                    after {
                        if (FlavorOneFeatureUtils(hostClassLoader!!).isFlavorOneDevice() == true) return@after
                        val context = firstArg().get<Context>() ?: return@after
                        val isManuallyLockedOn = getManuallyLockedStatus() ?: return@after
                        val getCurrentUserId = OsBinderCacheUtils.toClass().resolve().firstMethod {
                            name = "getCurrentUserId"
                        }.invoke<Int>()
                        val strongAuthForUser =
                            LockPatternUtils.toClass().createInstance(context, isPublic = false)
                                .asResolver()
                                .firstMethod { name = "getStrongAuthForUser" }
                                .invoke<Int>(getCurrentUserId)
                        val isChildren = Settings.Global.getInt(
                            context.contentResolver, "children_mode_on", 0
                        ) == 1
                        val isStudy = Settings.Global.getInt(
                            context.contentResolver, "STUDY_CENTER_MODE", 0
                        ) == 1
                        if (manuallyLockCanBeSeen(context) && !isManuallyLockedOn && strongAuthForUser != 1 && (!isChildren || !isStudy)) {
                            firstField { name = "mShouldShowManuallyLock" }.of(instance).set(true)
                            firstField { name = "mOplusShutdownView" }.of(instance).get()
                                ?.asResolver()
                                ?.firstMethod {
                                    name = "setManuallyLockEnable";parameters(Boolean::class)
                                }?.invoke(true)
                        } else {
                            firstField { name = "mShouldShowManuallyLock" }.of(instance).set(false)
                            firstField { name = "mOplusShutdownView" }.of(instance).get()
                                ?.asResolver()
                                ?.firstMethod { name = "setManuallyLockEnable" }?.invoke(false)
                        }
                    }
                }
            }

            //Source OplusGlobalActionsDialog
            "com.oplus.systemui.shutdown.OplusGlobalActionsDialog".toClass().resolve().apply {
                firstMethod { name = "showOrHideDialog" }.hook {
                    after {
                        if (FlavorOneFeatureUtils(hostClassLoader!!).isFlavorOneDevice() == true) return@after
                        val mExt = firstField { name = "mExt" }.of(instance).get() ?: return@after
                        val listener = mExt.asResolver().firstField { name = "mOnManuallyLock" }.get()

                        val mLockPatternUtils =
                            firstField { name = "mLockPatternUtils" }.of(instance).get()
                        mExt.asResolver().firstField { name = "mLockPatternUtils" }
                            .set(mLockPatternUtils)
                        val mDialog = firstField { name = "mDialog" }.of(instance).get()
                        mExt.asResolver().firstField { name = "mDialog" }.set(mDialog)

                        val mShutdownViewControl =
                            firstField { name = "mShutdownViewControl" }.of(instance).get()
                        mShutdownViewControl?.asResolver()?.firstMethod {
                            name = "setOnManuallyLockListener"
                        }?.invoke(listener)
                    }
                }
            }

            //Source KeyguardUpdateMonitor
            "com.android.keyguard.KeyguardUpdateMonitor".toClass().resolve().apply {
                firstConstructor().hook {
                    after {
                        if (FlavorOneFeatureUtils(hostClassLoader!!).isFlavorOneDevice() == true) return@after
                        val mContext = firstField { name = "mContext" }.of(instance).get<Context>()
                            ?: return@after
                        val mHandler = firstField { name = "mHandler" }.of(instance).get<Handler>()
                            ?: return@after
                        getOplusManuallyLock()?.asResolver()?.firstMethod { name = "onCreate" }
                            ?.invoke(mContext, mHandler)
                    }
                }
                firstMethod { name = "setKeyguardGoingAway" }.hook {
                    before {
                        if (FlavorOneFeatureUtils(hostClassLoader!!).isFlavorOneDevice() == true) return@before
                        val isManuallyLockedOn = getManuallyLockedStatus() ?: return@before
                        val bool = firstArg().get<Boolean>() ?: false
                        if (bool && isManuallyLockedOn) setManuallyLockedStatus(false)
                    }
                }
            }

            //Source KeyguardViewMediator
            "com.android.systemui.keyguard.KeyguardViewMediator".toClass().resolve().apply {
                firstMethod { name = "handleStartKeyguardExitAnimation" }.hook {
                    after {
                        if (FlavorOneFeatureUtils(hostClassLoader!!).isFlavorOneDevice() == true) return@after
                        val isManuallyLockedOn = getManuallyLockedStatus() ?: return@after
                        if (isManuallyLockedOn) setManuallyLockedStatus(false)
                    }
                }
            }
        }
    }

    @Obfuscate
    object ManualLockButtonV14 : YukiBaseHooker() {
        override fun onHook() {
            //Source ShutdownViewControl
            "com.oplus.systemui.shutdown.ShutdownViewControl".toClass().resolve().apply {
                firstMethod { name = "initManuallyLock" }.hook {
                    after {
                        if (FlavorOneFeatureUtils(hostClassLoader!!).isFlavorOneDevice() == true) return@after
                        val context = firstArg().get<Context>() ?: return@after
                        val manuallyLockCanBeSeen =
                            firstMethod { name = "manuallyLockCanBeSeen" }.of(instance)
                                .invoke<Boolean>(context) ?: return@after
                        val isManuallyLockedOn = getManuallyLockedStatus() ?: return@after
                        val getCurrentUserId = OsBinderCacheUtils.toClass().resolve().firstMethod {
                            name = "getCurrentUserId"
                        }.invoke<Int>()
                        val strongAuthForUser =
                            LockPatternUtils.toClass().createInstance(context, isPublic = false)
                                .asResolver()
                                .firstMethod { name = "getStrongAuthForUser" }
                                .invoke<Int>(getCurrentUserId)
                        val isChildren = Settings.Global.getInt(
                            context.contentResolver, "children_mode_on", 0
                        ) == 1
                        val isStudy = Settings.Global.getInt(
                            context.contentResolver, "STUDY_CENTER_MODE", 0
                        ) == 1
                        if (manuallyLockCanBeSeen && !isManuallyLockedOn && strongAuthForUser != 1 && (!isChildren || !isStudy)) {
                            firstField { name = "mShouldShowManuallyLock" }.of(instance).set(true)
                            firstField { name = "mOplusShutdownView" }.of(instance).get()
                                ?.asResolver()
                                ?.firstMethod {
                                    name = "setManuallyLockEnable";parameters(Boolean::class)
                                }?.invoke(true)
                        } else {
                            firstField { name = "mShouldShowManuallyLock" }.of(instance).set(false)
                            firstField { name = "mOplusShutdownView" }.of(instance).get()
                                ?.asResolver()
                                ?.firstMethod { name = "setManuallyLockEnable" }?.invoke(false)
                        }
                    }
                }
            }

            //Source OplusGlobalActionsDialog
            "com.oplus.systemui.shutdown.OplusGlobalActionsDialog".toClass().resolve().apply {
                firstMethod { name = "showOrHideDialog" }.hook {
                    after {
                        if (FlavorOneFeatureUtils(hostClassLoader!!).isFlavorOneDevice() == true) return@after
                        val mExt = firstField { name = "mExt" }.of(instance).get() ?: return@after
                        val mLockPatternUtils =
                            firstField { name = "mLockPatternUtils" }.of(instance).get()
                        val mDialog = firstField { name = "mDialog" }.of(instance).get()
                        val mShutdownViewControl =
                            firstField { name = "mShutdownViewControl" }.of(instance).get()
                        mExt.asResolver().firstMethod { name = "setForManuallyLock" }
                            .invoke(mLockPatternUtils, mDialog, mShutdownViewControl)
                        mExt.asResolver().firstMethod { name = "registerForManuallyLock" }.invoke()
                    }
                }
            }

            //Source KeyguardUpdateMonitor
            "com.android.keyguard.KeyguardUpdateMonitor".toClass().resolve().apply {
                firstConstructor().hook {
                    after {
                        if (FlavorOneFeatureUtils(hostClassLoader!!).isFlavorOneDevice() == true) return@after
                        val mContext =
                            firstField { name = "mContext" }.of(instance).get<Context>()
                                ?: return@after
                        val mHandler =
                            firstField { name = "mHandler" }.of(instance).get<Handler>()
                                ?: return@after
                        getOplusManuallyLock()?.asResolver()?.firstMethod { name = "onCreate" }
                            ?.invoke(mContext, mHandler)
                    }
                }
                firstMethod { name = "setKeyguardGoingAway" }.hook {
                    before {
                        if (FlavorOneFeatureUtils(hostClassLoader!!).isFlavorOneDevice() == true) return@before
                        val isManuallyLockedOn = getManuallyLockedStatus() ?: return@before
                        val bool = firstArg().get<Boolean>() ?: false
                        if (bool && isManuallyLockedOn) setManuallyLockedStatus(false)
                    }
                }
            }

            //Source KeyguardViewMediator
            "com.android.systemui.keyguard.KeyguardViewMediator".toClass().resolve().apply {
                firstMethod { name = "handleStartKeyguardExitAnimation" }.hook {
                    after {
                        if (FlavorOneFeatureUtils(hostClassLoader!!).isFlavorOneDevice() == true) return@after
                        val isManuallyLockedOn = getManuallyLockedStatus() ?: return@after
                        if (isManuallyLockedOn) setManuallyLockedStatus(false)
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    fun manuallyLockCanBeSeen(context: Context): Boolean {
        val currentUserId = OsBinderCacheUtils.toClass().resolve().firstMethod {
            name = "getCurrentUserId"
        }.invoke<Int>() ?: 0
        val faceManager = context.getSystemService("face")
        val hasEnrolledTemplates = faceManager.asResolver().firstMethod {
            name = "hasEnrolledTemplates";parameters(Int::class)
        }.invoke<Boolean>(currentUserId) ?: false

        val z = faceManager != null && hasEnrolledTemplates && Settings.Secure.getInt(
            context.contentResolver, "oplus_customize_face_unlock_switch", -1
        ) == 1
        val i2 = Settings.Secure.getInt(
            context.contentResolver, "oplus_customize_fingerprint_unlock_switch", -1
        )
        val fingerprintManager = context.getSystemService("fingerprint")
        val isHardwareDetected = fingerprintManager.asResolver().firstMethod {
            name = "isHardwareDetected";emptyParameters()
        }.invoke<Boolean>() ?: false
        val hasEnrolledFingerprints = fingerprintManager.asResolver().firstMethod {
            name = "hasEnrolledFingerprints";parameters(Int::class)
        }.invoke<Boolean>(currentUserId) ?: false
        return z || (isHardwareDetected && hasEnrolledFingerprints && i2 == 1 && !isFpDisabledByDPM(
            context,
            currentUserId
        ))
    }

    private fun isFpDisabledByDPM(context: Context, userId: Int): Boolean {
        val service = context.getSystemService(classOf<DevicePolicyManager>())
        val getKeyguardDisabledFeatures = service.asResolver().firstMethod {
            name = "getKeyguardDisabledFeatures";parameters(ComponentName::class, Int::class)
        }.invoke<Int>(null, userId) ?: 0
        return (getKeyguardDisabledFeatures and 32) != 0
    }

    private fun getOplusManuallyLock(): Any? {
        val shutDownDependency = "com.android.systemui.shutdown.ShutDownDependencyEx".toClass()
        val dependency = DependencyUtils(hostClassLoader!!, true).getDependency(shutDownDependency)
        return dependency?.asResolver()?.firstMethod { name = "getOplusManuallyLockEx" }?.invoke()
    }

    private fun getManuallyLockedStatus(): Boolean? {
        return getOplusManuallyLock()?.asResolver()?.firstMethod {
            name = "isManuallyLockedOn"
        }?.invoke<Boolean>()
    }

    @Suppress("SameParameterValue")
    private fun setManuallyLockedStatus(status: Boolean) {
        getOplusManuallyLock()?.asResolver()?.firstMethod {
            name = "setManuallyLockedOn"
        }?.invoke(status)
    }
}