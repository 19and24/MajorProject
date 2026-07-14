package com.example.majorproject.food.model

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private fun isoFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

private fun startOfDay(date: Date): Calendar {
    val cal = Calendar.getInstance()
    cal.time = date
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal
}

fun today(): String = isoFormat().format(Date())

fun addDays(n: Int): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_MONTH, n)
    return isoFormat().format(cal.time)
}

/** Whole-day difference between [expiryIso] (yyyy-MM-dd) and today; negative means already expired. */
fun getDays(expiryIso: String): Int {
    val now = startOfDay(Date())
    val exp = startOfDay(isoFormat().parse(expiryIso) ?: Date())
    val diffMs = exp.timeInMillis - now.timeInMillis
    return Math.round(diffMs / 86_400_000.0).toInt()
}

fun fmtDate(iso: String): String {
    val cal = Calendar.getInstance()
    cal.time = isoFormat().parse(iso) ?: Date()
    val month = cal.get(Calendar.MONTH) + 1
    val day = cal.get(Calendar.DAY_OF_MONTH)
    return "${month}월 ${day}일"
}

data class Status(
    val color: Color,
    val bg: Color,
    val bar: Color,
    val label: String,
    val ring: Color,
    val dot: Color,
)

fun getStatus(days: Int): Status = when {
    days < 0 -> Status(
        color = Color(0xFFDC2626), bg = Color(0xFFFEF2F2), bar = Color(0xFFEF4444),
        label = "만료됨", ring = Color(0xFFFECACA), dot = Color(0xFFFCA5A5),
    )
    days == 0 -> Status(
        color = Color(0xFFEA580C), bg = Color(0xFFFFF7ED), bar = Color(0xFFF97316),
        label = "오늘 만료", ring = Color(0xFFFED7AA), dot = Color(0xFFFDBA74),
    )
    days <= 2 -> Status(
        color = Color(0xFFEA580C), bg = Color(0xFFFFF7ED), bar = Color(0xFFF97316),
        label = "D-$days", ring = Color(0xFFFED7AA), dot = Color(0xFFFDBA74),
    )
    days <= 5 -> Status(
        color = Color(0xFFCA8A04), bg = Color(0xFFFEFCE8), bar = Color(0xFFEAB308),
        label = "D-$days", ring = Color(0xFFFEF08A), dot = Color(0xFFFDE047),
    )
    else -> Status(
        color = Color(0xFF16A34A), bg = Color(0xFFF0FDF4), bar = Color(0xFF22C55E),
        label = "D-$days", ring = Color(0xFFBBF7D0), dot = Color(0xFF86EFAC),
    )
}
