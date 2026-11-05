package com.luckyzyx.luckytool.hook.scopes.systemui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CredentialShortcutPolicyTest {
    @Test
    fun `the credential shortcut uses the slot opposite the camera`() {
        assertEquals(
            CredentialShortcutPolicy.Slot.START,
            CredentialShortcutPolicy.credentialSlot(null, "camera"),
        )
        assertEquals(
            CredentialShortcutPolicy.Slot.END,
            CredentialShortcutPolicy.credentialSlot("camera", "flashlight"),
        )
    }

    @Test
    fun `unknown or ambiguous camera placement keeps the native entry`() {
        assertNull(CredentialShortcutPolicy.credentialSlot(null, null))
        assertNull(CredentialShortcutPolicy.credentialSlot("flashlight", "wallet"))
        assertNull(CredentialShortcutPolicy.credentialSlot("camera", "camera"))
    }
}
