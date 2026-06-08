package com.mangustc.mdnotes.ui.util

import com.ibm.icu.text.DateFormat
import com.ibm.icu.text.RelativeDateTimeFormatter
import com.ibm.icu.text.RelativeDateTimeFormatter.Direction
import com.ibm.icu.text.RelativeDateTimeFormatter.RelativeUnit
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.time.Clock

class DesktopDateFormatter : DateFormatter {
    override fun formatRelativeTime(timeMillis: Long, maxTimeMillis: Long?): String {
        val currentMillis = Clock.System.now().toEpochMilliseconds()

        if (maxTimeMillis != null && (currentMillis - timeMillis) > maxTimeMillis) {
            return formatDateTime(timeMillis)
        }

        val diffMillis = currentMillis - timeMillis
        val diffAbs = abs(diffMillis)

        val formatter = RelativeDateTimeFormatter.getInstance(
            com.ibm.icu.util.ULocale.forLocale(Locale.getDefault()),
            null,
            RelativeDateTimeFormatter.Style.SHORT,
            com.ibm.icu.text.DisplayContext.CAPITALIZATION_NONE,
        )

        val direction = if (diffMillis >= 0) Direction.LAST else Direction.NEXT

        return when {
            diffAbs < 60_000L -> {
                val seconds = (diffAbs / 1000L).toDouble()
                formatter.format(seconds, direction, RelativeUnit.SECONDS)
            }

            diffAbs < 3600_000L -> {
                val minutes = (diffAbs / 60_000L).toDouble()
                formatter.format(minutes, direction, RelativeUnit.MINUTES)
            }

            diffAbs < 86400_000L -> {
                val hours = (diffAbs / 3600_000L).toDouble()
                formatter.format(hours, direction, RelativeUnit.HOURS)
            }

            diffAbs < 604800_000L -> { // Up to 1 week limit (matches Android DateUtils normal cap)
                val days = (diffAbs / 86400_000L).toDouble()
                formatter.format(days, direction, RelativeUnit.DAYS)
            }

            else -> {
                formatDateTime(timeMillis)
            }
        }
    }

    override fun formatDateTime(timeMillis: Long, skeleton: String): String {
        // ICU4J's DateFormat directly generates localized patterns from a skeleton string!
        // This is a direct equivalent to Android's DateFormat.getBestDateTimePattern
        val formatter = DateFormat.getInstanceForSkeleton(skeleton, Locale.getDefault())
        return formatter.format(Date(timeMillis))
    }

    override fun formatDateOnly(timeMillis: Long, skeleton: String): String {
        val formatter = DateFormat.getInstanceForSkeleton(skeleton, Locale.getDefault())
        return formatter.format(Date(timeMillis))
    }

    override fun isSameDay(t1: Long, t2: Long): Boolean {
        // Pure Java is fully capable for raw, exact pattern evaluation
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        return fmt.format(Date(t1)) == fmt.format(Date(t2))
    }
}