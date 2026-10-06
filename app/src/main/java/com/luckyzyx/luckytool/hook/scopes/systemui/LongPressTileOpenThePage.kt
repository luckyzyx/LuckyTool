package com.luckyzyx.luckytool.hook.scopes.systemui

import android.content.Intent
import android.os.Handler
import android.telephony.SubscriptionManager
import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.kavaref.extension.VariousClass
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.utils.A13
import com.luckyzyx.luckytool.utils.SDK
import org.lsposed.lsparanoid.Obfuscate
import java.lang.ref.WeakReference

@Obfuscate
object LongPressTileOpenThePage : YukiBaseHooker() {
    override fun onHook() {
        if (SDK == A13) loadHooker(LongPressTileV13)
        else loadHooker(LongPressTile)

        loadHooker(HookCellularTileIntent)
    }

    @Obfuscate
    object LongPressTile : YukiBaseHooker() {
        override fun onHook() {
            //QSTileImpl
            "com.android.systemui.qs.tileimpl.QSTileImpl".toClass().resolve().apply {
                firstMethod { name = "longClick";parameterCount = 1 }.hook {
                    before {
                        firstField { name = "mHandler" }.of(instance).get<Handler>()
                            ?.obtainMessage(4, 0, 0, WeakReference(firstArg().get()))
                            ?.sendToTarget()
                        result = null
                    }
                }
            }
        }
    }

    @Obfuscate
    object LongPressTileV13 : YukiBaseHooker() {
        override fun onHook() {
            //QSTileImpl
            "com.android.systemui.qs.tileimpl.QSTileImpl".toClass().resolve().apply {
                firstMethod { name = "longClick";parameterCount = 1 }.hook {
                    before {
                        firstField { name = "mClickHandler" }.of(instance).get<Handler>()
                            ?.sendEmptyMessage(4)
                        result = null
                    }
                }
            }
        }
    }

    @Obfuscate
    object HookCellularTileIntent : YukiBaseHooker() {
        override fun onHook() {
            //Source OplusCellularTile
            VariousClass(
                "com.oplusos.systemui.qs.tiles.OplusCellularTile", //C13
                "com.oplus.systemui.qs.tiles.OplusCellularTile" //C14
            ).toClass().resolve().apply {
                firstMethod { name = "getLongClickIntent" }.hook {
                    before {
                        val mLockSimState =
                            firstField { name = "mLockSimState" }.of(instance).get<Boolean>()
                                ?: false
                        if (mLockSimState) return@before
                        val intent = Intent("android.settings.NETWORK_OPERATOR_SETTINGS")
                        val subId = SubscriptionManager.getDefaultDataSubscriptionId()
                        if (subId != -1) intent.putExtra("android.provider.extra.SUB_ID", subId)
                        result = intent
                    }
                }
            }
        }
    }
}