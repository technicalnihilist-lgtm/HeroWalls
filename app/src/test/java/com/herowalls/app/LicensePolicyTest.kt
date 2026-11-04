package com.herowalls.app

import com.herowalls.app.data.remote.LicensePolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LicensePolicyTest {

    @Test
    fun `public domain and cc0 are accepted`() {
        assertTrue(LicensePolicy.isReusable("pd", "Public domain"))
        assertTrue(LicensePolicy.isReusable("cc0", "CC0"))
        assertTrue(LicensePolicy.isReusable("cc-zero", "CC0 1.0"))
    }

    @Test
    fun `cc by and cc by-sa of every version are accepted`() {
        listOf("cc-by-2.0", "cc-by-2.5", "cc-by-3.0", "cc-by-4.0", "cc-by-sa-2.0", "cc-by-sa-2.5", "cc-by-sa-3.0", "cc-by-sa-4.0")
            .forEach { assertTrue("expected $it to be reusable", LicensePolicy.isReusable(it, "")) }
    }

    @Test
    fun `non commercial and no derivative licences are rejected`() {
        listOf("cc-by-nc-4.0", "cc-by-nc-sa-4.0", "cc-by-nd-4.0", "cc-by-nc-nd-4.0")
            .forEach { assertFalse("expected $it to be rejected", LicensePolicy.isReusable(it, "")) }
    }

    @Test
    fun `fair use and non free are rejected`() {
        assertFalse(LicensePolicy.isReusable("fair use", "Fair use"))
        assertFalse(LicensePolicy.isReusable("non-free", "Non-free"))
        assertFalse(LicensePolicy.isReusable("", "Non-free media"))
    }

    @Test
    fun `unknown or missing licence is rejected`() {
        assertFalse(LicensePolicy.isReusable(null, null))
        assertFalse(LicensePolicy.isReusable("", ""))
        assertFalse(LicensePolicy.isReusable("some-custom-licence", ""))
    }

    @Test
    fun `short name fallback only trusts free families`() {
        assertTrue(LicensePolicy.isReusable(null, "Creative Commons Attribution 4.0"))
        assertFalse(LicensePolicy.isReusable(null, "Creative Commons Attribution-NonCommercial 4.0"))
        assertFalse(LicensePolicy.isReusable(null, "All rights reserved"))
    }

    @Test
    fun `costume restriction produces a personality rights note`() {
        val note = LicensePolicy.restrictionNote("costume")
        assertTrue(note.contains("Personality rights"))
        assertTrue(LicensePolicy.restrictionNote("").isEmpty())
    }
}
