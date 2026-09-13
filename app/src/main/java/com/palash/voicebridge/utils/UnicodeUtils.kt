package com.palash.voicebridge.utils

/**
 * Utilities for handling Unicode scripts used in tribal language education.
 *
 * Supported scripts:
 * - Devanagari (U+0900-U+097F): Hindi
 * - Ol Chiki (U+1C50-U+1C7F): Santhali primary script
 * - Warang Citi (U+118A0-U+118FF): Ho language script
 * - Latin: Romanization/pronunciation guides
 */
object UnicodeUtils {

    /** Devanagari Unicode block: U+0900-U+097F */
    fun isDevanagari(text: String): Boolean =
        text.any { it.code in 0x0900..0x097F }

    /** Ol Chiki Unicode block: U+1C50-U+1C7F */
    fun isOlChiki(text: String): Boolean =
        text.any { it.code in 0x1C50..0x1C7F }

    /** Detect the primary script of a text */
    fun detectScript(text: String): Script {
        val devanagariCount = text.count { it.code in 0x0900..0x097F }
        val olChikiCount = text.count { it.code in 0x1C50..0x1C7F }
        val latinCount = text.count { it.code in 0x0041..0x007A }

        return when {
            devanagariCount > olChikiCount && devanagariCount > latinCount -> Script.DEVANAGARI
            olChikiCount > devanagariCount -> Script.OL_CHIKI
            latinCount > 0 -> Script.LATIN
            else -> Script.UNKNOWN
        }
    }

    /** Whether a string contains only printable Unicode */
    fun isPrintable(text: String): Boolean =
        text.all { !it.isISOControl() || it == '\n' || it == '\t' }

    /** Recommended font for script rendering */
    fun getRecommendedFont(script: Script): String? = when (script) {
        Script.OL_CHIKI -> "NotoSansOlChiki" // Bundle this font
        Script.DEVANAGARI -> null // Android default handles Devanagari
        Script.LATIN -> null
        Script.UNKNOWN -> null
    }
}

enum class Script {
    DEVANAGARI,
    OL_CHIKI,
    LATIN,
    UNKNOWN
}
