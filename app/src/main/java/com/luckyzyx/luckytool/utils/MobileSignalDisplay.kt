package com.luckyzyx.luckytool.utils

/** Display options for ColorOS 16 and newer; the renderer checks the actual icon layout. */
internal object MobileSignalDisplay {
    private const val COLOR_OS_16 = 37
    const val KEY = "mobile_signal_display"
    const val SYSTEM = "system"
    const val STACKED = "stacked"
    const val ROTATED = "stacked_rotated"

    fun isSupported(osCode: Int) = osCode >= COLOR_OS_16
    fun isStacked(value: String?) = value == STACKED || value == ROTATED

    fun stockLevel(resourceEntry: String): Int? {
        val prefix = "stat_signal_signal_lte_single_"
        if (!resourceEntry.startsWith(prefix) || resourceEntry.length != prefix.length + 1) return null
        return resourceEntry.last().digitToIntOrNull()?.takeIf { it in 0..4 }
    }
}
