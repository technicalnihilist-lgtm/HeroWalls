package com.herowalls.app.data.remote

/**
 * Central, auditable licence gate.
 *
 * HeroWalls only ever displays a Commons file when its machine-readable licence
 * permits commercial use and derivative works: Public Domain, CC0, CC BY and
 * CC BY-SA (any version), plus the Commons "free use" markers carrying the same
 * rights. Everything else - fair use, non-free, NC, ND, unverifiable - is
 * dropped before it can reach the UI.
 *
 * The decision is deliberately exact-match based rather than substring based, so
 * a licence code such as `cc-by-nc-4.0` can never be mistaken for `cc-by-4.0`.
 */
object LicensePolicy {

    /** Licence codes accepted verbatim. */
    private val allowedExact = setOf(
        "pd", "public domain", "cc0", "cc-zero", "cc-zero-1.0",
        "attribution", "copyrighted free use",
        "gfdl", "gfdl-1.2", "gfdl-1.3", "cc-by", "cc-by-sa"
    )

    /** Codes that must always be rejected. */
    private val blockedExact = setOf(
        "fair use", "fairuse", "non-free", "nonfree", "non free",
        "all rights reserved", "copyright", "permission", "unknown", ""
    )

    /** cc-by-2.0, cc-by-3.0, cc-by-4.0, cc-by-2.5, cc-by ... */
    private val ccBy = Regex("^cc-by(-\\d(\\.\\d)?)?$")

    /** cc-by-sa-2.0, cc-by-sa-4.0, cc-by-sa ... */
    private val ccBySa = Regex("^cc-by-sa(-\\d(\\.\\d)?)?$")

    /** Anything carrying a NonCommercial / NoDerivatives element is refused. */
    private val ccRestricted = Regex("^cc-by-(-?sa-)?(nc|nd)(-sa)?(-\\d(\\.\\d)?)?$")

    /** Phrases in the human-readable name that signal a restricted licence. */
    private val blockedPhrases = listOf(
        "noncommercial", "non-commercial", "no derivative", "noderiv",
        "fair use", "non-free", "nonfree", "all rights reserved"
    )

    fun isReusable(code: String?, shortName: String?): Boolean {
        val c = code?.trim()?.lowercase().orEmpty()
        val s = shortName?.trim()?.lowercase().orEmpty()
        if (c.isBlank() && s.isBlank()) return false

        if (c.isNotBlank()) {
            if (c in blockedExact) return false
            if (ccRestricted.matches(c)) return false
            if (c in allowedExact) return true
            if (ccBy.matches(c)) return true
            if (ccBySa.matches(c)) return true
            // Unknown code: refuse rather than guess.
            return false
        }

        // Only reached when Commons gave no licence code at all.
        if (blockedPhrases.any { s.contains(it) }) return false
        return s.startsWith("public domain") || s.startsWith("cc0") ||
            s.startsWith("cc by") || s.startsWith("cc-by") ||
            s.startsWith("creative commons attribution") ||
            s.startsWith("attribution")
    }

    /** Personality-rights / trademark notices are surfaced, never silently dropped. */
    fun restrictionNote(restrictions: String?): String {
        val r = restrictions?.trim().orEmpty()
        return when {
            r.isBlank() -> ""
            r.contains("costume", ignoreCase = true) ->
                "Personality rights: this image shows a real person in costume. Reuse may need consent in addition to the image licence."
            r.contains("trademark", ignoreCase = true) ->
                "Trademark notice: characters and logos may be trademarks of their owners."
            else -> "Additional restriction flagged on Commons: $r"
        }
    }

    val explanation: String = """
        HeroWalls sources every wallpaper from Wikimedia Commons through its public API and
        only keeps files whose machine-readable licence permits reuse, including commercial
        use and derivative works.

        Accepted: Public Domain, CC0, CC BY (any version), CC BY-SA (any version).

        Rejected: fair-use material, non-free files, CC BY-NC, CC BY-ND and any file whose
        licence cannot be verified. Movie posters, promotional stills and press artwork are
        not used unless they carry one of the accepted licences.

        Attribution: creator, licence name, licence link and the original Commons file page
        appear on every wallpaper detail screen and in the credits screen. Attribution is
        never removed. HeroWalls claims no ownership of any third-party character, trademark
        or artwork; all characters remain the property of their respective owners.
    """.trimIndent()
}
