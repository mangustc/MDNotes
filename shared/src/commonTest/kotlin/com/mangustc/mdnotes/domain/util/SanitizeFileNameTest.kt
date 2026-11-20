package com.mangustc.mdnotes.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SanitizeFileNameTest {

    @Test
    fun validFileName_remainsUnchanged() {
        assertEquals("My Note 2026.md", sanitizeFileName("My Note 2026.md"))
        assertEquals("Заметка.md", sanitizeFileName("Заметка.md"))
    }

    @Test
    fun forbiddenAndControlCharacters_areReplacedWithUnderscore() {
        assertEquals("a_b_c_d_e_f_g_h_i", sanitizeFileName("""a<b>c:d"e/f\g|h?i"""))
        assertEquals("line_one_two", sanitizeFileName("line\none\u0000two"))
        assertEquals("del_char", sanitizeFileName("del\u007Fchar"))
    }

    @Test
    fun leadingWhitespaceAndTrailingDotsOrWhitespace_areTrimmed() {
        assertEquals("hello", sanitizeFileName("   hello...   "))
        assertEquals("note.md", sanitizeFileName("  note.md... "))
    }

    @Test
    fun emptyOrOnlyDotsAndWhitespace_usesFallbackName() {
        assertEquals("file", sanitizeFileName(""))
        assertEquals("file", sanitizeFileName("   "))
        assertEquals("file", sanitizeFileName("..."))
        assertEquals("untitled", sanitizeFileName("  ...  ", fallbackName = "untitled"))
    }

    @Test
    fun windowsReservedNames_arePrefixedWithUnderscoreCaseInsensitively() {
        assertEquals("_CON", sanitizeFileName("CON"))
        assertEquals("_con.md", sanitizeFileName("con.md"))
        assertEquals("_nul.txt", sanitizeFileName("nul.txt"))
        assertEquals("_COM1.md", sanitizeFileName("COM1.md"))
        assertEquals("_LPT¹.md", sanitizeFileName("LPT¹.md"))

        // Names merely starting with a reserved prefix are safe and stay untouched
        assertEquals("console.md", sanitizeFileName("console.md"))
        assertEquals("auxiliary.md", sanitizeFileName("auxiliary.md"))
    }

    @Test
    fun longAsciiName_isTruncatedTo255BytesPreservingExtension() {
        val longName = "a".repeat(300) + ".md"

        val result = sanitizeFileName(longName)

        assertTrue(result.endsWith(".md"))
        assertEquals(255, result.encodeToByteArray().size)
    }

    @Test
    fun longMultiByteUnicodeName_respects255ByteLimitWithoutCorruptingCharacters() {
        // Each Cyrillic character is 2 bytes in UTF-8
        val longCyrillic = "я".repeat(200) + ".md"

        val result = sanitizeFileName(longCyrillic)

        assertTrue(result.endsWith(".md"))
        assertTrue(result.encodeToByteArray().size <= 255)
        // (255 - 3) / 2 = 126 chars -> 252 + 3 = 255 bytes
        assertEquals("я".repeat(126) + ".md", result)
    }

    @Test
    fun longEmojiName_respects255ByteLimitWithoutSplittingSurrogatePairs() {
        // Each U+1F600 emoji is a UTF-16 surrogate pair and 4 bytes in UTF-8
        val longEmoji = "\uD83D\uDE00".repeat(100) + ".md"

        val result = sanitizeFileName(longEmoji)

        assertTrue(result.endsWith(".md"))
        assertTrue(result.encodeToByteArray().size <= 255)
        // (255 - 3) / 4 = 63 emojis -> 252 + 3 = 255 bytes
        assertEquals("\uD83D\uDE00".repeat(63) + ".md", result)
        assertValidUtf16Surrogates(result)
    }

    @Test
    fun extremelyLongExtension_isTruncatedTo255Bytes() {
        val longExt = "a." + "b".repeat(300)

        val result = sanitizeFileName(longExt)

        assertEquals(255, result.encodeToByteArray().size)
        assertTrue(result.startsWith("."))
    }

    private fun assertValidUtf16Surrogates(value: String) {
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c.isHighSurrogate()) {
                assertTrue(i + 1 < value.length && value[i + 1].isLowSurrogate())
                i += 2
            } else {
                assertFalse(c.isLowSurrogate())
                i++
            }
        }
    }
}
