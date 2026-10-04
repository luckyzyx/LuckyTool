package com.luckyzyx.luckytool.hook.hookers

import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.luckyzyx.luckytool.hook.scopes.market.DefaultExpandUpdateList
import com.luckyzyx.luckytool.hook.scopes.market.RemoveMarketMinePageAppRecommend
import com.luckyzyx.luckytool.hook.scopes.market.RemoveMarketSplashPageAppRecommend
import com.luckyzyx.luckytool.hook.scopes.market.RemoveMarketUpdateDownloadPageAppRecommend
import com.luckyzyx.luckytool.utils.DexkitUtils
import com.luckyzyx.luckytool.utils.ModulePrefs
import org.lsposed.lsparanoid.Obfuscate

@Obfuscate
object HookMarket : YukiBaseHooker() {
    override fun onHook() {
        DexkitUtils.create(appInfo.sourceDir) { dexKitBridge ->
            //移除商店启动页应用推荐
            if (preferences(ModulePrefs).getBoolean("remove_market_splash_page_app_recommend", false)) {
                loadHooker(RemoveMarketSplashPageAppRecommend(dexKitBridge))
            }
            //移除商店更新下载页面应用推荐
            if (preferences(ModulePrefs).getBoolean("remove_market_update_download_page_app_recommend", false)) {
                loadHooker(RemoveMarketUpdateDownloadPageAppRecommend(dexKitBridge))
            }
            //移除软件商店我的页面应用推荐
            if (preferences(ModulePrefs).getBoolean("remove_market_mine_page_app_recommend", false)) {
                loadHooker(RemoveMarketMinePageAppRecommend(dexKitBridge))
            }
            //默认展开更新列表
            if (preferences(ModulePrefs).getBoolean("default_expand_update_list", false)) {
                loadHooker(DefaultExpandUpdateList(dexKitBridge))
            }
        }
    }
}