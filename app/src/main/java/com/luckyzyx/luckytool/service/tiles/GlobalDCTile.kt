package com.luckyzyx.luckytool.service.tiles

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import org.lsposed.lsparanoid.Obfuscate
import com.luckyzyx.luckytool.ITileServiceController
import com.luckyzyx.luckytool.service.TilesService
import com.luckyzyx.luckytool.utils.GlobalKeyValue.keyGlobalDCMode
import com.luckyzyx.luckytool.utils.SettingsPrefs
import com.luckyzyx.luckytool.utils.putBoolean

@Obfuscate
class GlobalDCTile : TileService() {
    private var controller: ITileServiceController? = null

    override fun onStartListening() {
        TilesService.get(this) {
            controller = it
            refreshData()
        }
    }

    override fun onClick() {
        when (qsTile.state) {
            Tile.STATE_INACTIVE -> {
                controller?.globalDCMode = true
                putBoolean(SettingsPrefs, keyGlobalDCMode, true)
            }

            Tile.STATE_ACTIVE -> {
                controller?.globalDCMode = false
                putBoolean(SettingsPrefs, keyGlobalDCMode, false)
            }

            Tile.STATE_UNAVAILABLE -> {}
        }
        refreshData()
    }

    private fun refreshData() {
        qsTile.state = if (controller == null) Tile.STATE_UNAVAILABLE
        else if (!controller!!.checkGlobalDCMode()) Tile.STATE_UNAVAILABLE
        else if (controller!!.globalDCMode) Tile.STATE_ACTIVE
        else Tile.STATE_INACTIVE
        qsTile.updateTile()
        if (qsTile.state == Tile.STATE_UNAVAILABLE) putBoolean(
            SettingsPrefs, keyGlobalDCMode, false
        )
    }
}
