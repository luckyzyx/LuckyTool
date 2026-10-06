package com.luckyzyx.luckytool.hook.scopes.games

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object RemoveSomeVipLimit : YukiBaseHooker() {
    override fun onHook() {
        //network_speed_vip -> oppo_acc

        //Source VipInfoBean -> VipInfosDTO
        //<string name="magic_voice_buy_vip_tip">开启游戏变声，尽享全部变声效果</string>
        "com.oplus.games.account.bean.VipInfoBean\$VipInfosDTO".toClass().resolve().apply {
            firstMethod { name = "getVip" }.hook {
                intercept(true)
            }
            firstMethod { name = "getExpiredVip" }.hook {
                intercept(false)
            }
            firstMethod { name = "getExpireTime" }.hook {
                intercept("2999-12-31")
            }
            firstMethod { name = "getSign" }.hook {
                intercept(true)
            }
        }
        //Source VipAccelearateResponse
        "com.oplus.games.account.bean.VipAccelearateResponse".toClass().resolve().apply {
            firstMethod { name = "getSuperBooster" }.hook {
                intercept(true)
            }
            firstMethod { name = "isSuperBooster" }.hook {
                intercept(true)
            }
        }
        //Source VIPStateBean
        "com.oplus.games.account.bean.VIPStateBean".toClass().resolve().apply {
            firstMethod { name = "getVipState" }.hook {
                intercept(5)
            }
            firstMethod { name = "getExpireTime" }.hook {
                intercept("2999-12-31")
            }
        }
        //Source UserInfo
        "com.coloros.gamespaceui.module.magicvoice.oplus.data.UserInfo".toClass().resolve().apply {
            firstMethod { name = "getExpireTime" }.hook {
                intercept("2999-12-31")
            }
            firstMethod { name = "getHasTrialQualifications" }.hook {
                intercept(true)
            }
            firstMethod { name = "getUserIdentity" }.hook {
                intercept(3)
            }
        }
    }
}