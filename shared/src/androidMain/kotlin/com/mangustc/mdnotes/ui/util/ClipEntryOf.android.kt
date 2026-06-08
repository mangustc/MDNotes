package com.mangustc.mdnotes.ui.util

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry

actual fun clipEntryOf(text: String): ClipEntry =
    ClipEntry(ClipData.newPlainText(null, text))