package com.mangustc.mdnotes.domain.util

/**
 * Sanitizes a file name string to ensure compatibility across Windows,
 * Linux, Android, macOS, and iOS.
 *
 * @param name The original filename string to sanitize.
 * @param fallbackName The fallback name to use if the result is empty. Default is "file".
 * @return A sanitized, cross-platform safe filename.
 */
fun sanitizeFileName(name: String, fallbackName: String = "file"): String {
    val sb = StringBuilder()
    for (char in name) {
        if (isCharInvalid(char)) {
            sb.append('_')
        } else {
            sb.append(char)
        }
    }
    var cleaned = sb.toString()

    cleaned = cleaned.trimStart { it.isWhitespace() }
    cleaned = cleaned.trimEnd { it.isWhitespace() || it == '.' }

    if (cleaned.isEmpty()) {
        cleaned = fallbackName
    }

    // For Windows Compatibility
    val reservedNames = setOf(
        "CON", "PRN", "AUX", "NUL",
        "COM0", "COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
        "COM¹", "COM²", "COM³",
        "LPT0", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",
        "LPT¹", "LPT²", "LPT³"
    )
    val firstDotIndex = cleaned.indexOf('.')
    val firstSegment = if (firstDotIndex != -1) cleaned.substring(0, firstDotIndex) else cleaned
    if (reservedNames.contains(firstSegment.uppercase())) {
        cleaned = "_$cleaned"
    }

    // Enforce a maximum of 255 bytes for file names
    val maxBytes = 255
    val lastDotIndex = cleaned.lastIndexOf('.')
    val (base, ext) = if (lastDotIndex > 0) {
        cleaned.substring(0, lastDotIndex) to cleaned.substring(lastDotIndex)
    } else {
        cleaned to ""
    }

    val extBytes = ext.encodeToByteArray().size
    val finalName = if (extBytes >= maxBytes) {
        truncateStringByBytes(ext, maxBytes)
    } else {
        val allowedBaseBytes = maxBytes - extBytes
        val truncatedBase = truncateStringByBytes(base, allowedBaseBytes)
        truncatedBase + ext
    }

    return finalName.ifEmpty { fallbackName }
}

private fun isCharInvalid(c: Char): Boolean {
    // Match ASCII control characters (0-31 and 127)
    if (c.code in 0..31 || c.code == 127) return true

    // Windows/POSIX forbidden characters
    return when (c) {
        '<', '>', ':', '"', '/', '\\', '|', '?', '*' -> true
        else -> false
    }
}

private fun truncateStringByBytes(str: String, maxBytes: Int): String {
    if (maxBytes <= 0) return ""
    val sb = StringBuilder()
    var currentBytes = 0
    var i = 0
    while (i < str.length) {
        val char = str[i]
        // Determine if there is a multi-unit surrogate pair to handle together
        val charStr = if (char.isHighSurrogate() && i + 1 < str.length) {
            str.substring(i, i + 2)
        } else {
            str.substring(i, i + 1)
        }
        val charBytes = charStr.encodeToByteArray().size
        if (currentBytes + charBytes <= maxBytes) {
            sb.append(charStr)
            currentBytes += charBytes
        } else {
            break
        }
        i += charStr.length
    }
    return sb.toString()
}