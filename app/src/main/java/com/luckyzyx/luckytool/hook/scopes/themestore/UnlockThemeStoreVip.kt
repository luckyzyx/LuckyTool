package com.luckyzyx.luckytool.hook.scopes.themestore

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object UnlockThemeStoreVip : YukiBaseHooker() {
    override fun onHook() {
        //Source VipUserDto
        "com.oppo.cdo.card.theme.dto.vip.VipUserDto".toClassOrNull()?.resolve()?.apply {
            firstMethod { name = "getVipStatus" }.hook {
                intercept(1)
            }
            firstMethod { name = "getVipDays" }.hook {
                intercept(999)
            }
        }

        //Source WeatherPageResponseDto
        "com.oppo.cdo.card.theme.dto.page.WeatherPageResponseDto".toClassOrNull()?.resolve()
            ?.apply {
                firstMethod { name = "getVipStatus" }.hook {
                    intercept(1)
                }
            }

        //Source ResourceItemDto
        "com.oppo.cdo.theme.domain.dto.response.ResourceItemDto".toClassOrNull()?.resolve()?.apply {
            firstMethod { name = "getIsVip" }.hook {
                intercept(1)
            }
            firstMethod { name = "getIsVipAvailable" }.hook {
                intercept(1)
            }
        }

        //Source PublishProductItemDto
        "com.oppo.cdo.theme.domain.dto.response.PublishProductItemDto".toClassOrNull()?.resolve()
            ?.apply {
                firstMethod { name = "getPrice" }.hook {
                    intercept(0.0)
                }
                firstMethod { name = "getIsVipAvailable" }.hook {
                    intercept(1)
                }
            }

        //Source SplashDto
        "com.oppo.cdo.card.theme.dto.SplashDto".toClassOrNull()?.resolve()?.apply {
            firstMethod { name = "getAdData" }.hook {
                intercept(null)
            }
            firstMethod { name = "getShowTime" }.hook {
                intercept(1)
            }
            firstMethod { name = "getIsSkip" }.hook {
                intercept(true)
            }
        }

        //Source ThemeTrialExpireReceiver
        "com.nearme.themespace.trial.ThemeTrialExpireReceiver".toClassOrNull()?.resolve()?.apply {
            firstMethod { name = "onReceive" }.hook {
                intercept()
            }
        }
    }
}