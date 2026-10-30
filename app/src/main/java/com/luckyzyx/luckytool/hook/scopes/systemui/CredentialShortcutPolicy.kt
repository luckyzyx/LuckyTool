package com.luckyzyx.luckytool.hook.scopes.systemui

internal object CredentialShortcutPolicy {
    enum class Slot(val id: String) {
        START("bottom_start"),
        END("bottom_end"),
    }

    fun credentialSlot(startKey: String?, endKey: String?): Slot? = when {
        startKey == "camera" && endKey != "camera" -> Slot.END
        endKey == "camera" && startKey != "camera" -> Slot.START
        else -> null
    }
}
