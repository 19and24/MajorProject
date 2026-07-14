package com.example.majorproject.food.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.majorproject.food.model.FoodItem
import com.example.majorproject.food.model.Tab
import com.example.majorproject.food.model.fmtDate
import com.example.majorproject.food.model.getDays
import com.example.majorproject.food.model.getStatus
import com.example.majorproject.food.ui.theme.AppFonts
import com.example.majorproject.food.ui.theme.FoodColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(items: List<FoodItem>, onNavigate: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = when {
        hour < 12 -> "좋은 아침이에요"
        hour < 18 -> "안녕하세요"
        else -> "좋은 저녁이에요"
    }
    val expired = remember(items) { items.filter { getDays(it.expiryDate) < 0 } }
    val urgent = remember(items) { items.filter { getDays(it.expiryDate) in 0..2 } }
    val thisWeek = remember(items) { items.filter { getDays(it.expiryDate) in 0..7 } }
    val fresh = remember(items) { items.filter { getDays(it.expiryDate) > 5 } }
    val dateLabel = remember { SimpleDateFormat("M월 d일 (EEE)", Locale.KOREA).format(Date()) }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item { HomeHeader(dateLabel = dateLabel, greeting = greeting, itemCount = items.size) }
        item { StatCardsRow(total = items.size, urgent = urgent.size, expired = expired.size) }
        if (urgent.isNotEmpty()) {
            item { UrgentSection(urgent = urgent, onNavigate = onNavigate) }
        }
        if (expired.isNotEmpty()) {
            item { ExpiredAlert(count = expired.size, names = expired.joinToString(", ") { it.name }) }
        }
        if (thisWeek.isNotEmpty()) {
            item { ThisWeekSection(items = thisWeek.take(4), onNavigate = onNavigate) }
        }
        item { FreshBanner(count = fresh.size) }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun HomeHeader(dateLabel: String, greeting: String, itemCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(listOf(FoodColors.primary, FoodColors.primaryDark)))
            .padding(start = 20.dp, end = 20.dp, top = 48.dp, bottom = 24.dp),
    ) {
        Box(
            modifier = Modifier
                .offset(x = 40.dp, y = (-40).dp)
                .size(160.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f)),
        )
        Column {
            Text(
                dateLabel.uppercase(),
                fontFamily = AppFonts.dmMono,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.8f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "$greeting 👋",
                fontFamily = AppFonts.nunito,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                color = Color.White,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "관리 중인 식자재 ${itemCount}개",
                fontFamily = AppFonts.dmSans,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun StatCardsRow(total: Int, urgent: Int, expired: Int) {
    val stats = listOf(
        Triple("전체", total, FoodColors.primary) to "🥗",
        Triple("임박", urgent, Color(0xFFEA580C)) to "🔥",
        Triple("만료", expired, Color(0xFFDC2626)) to "⚠️",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .offset(y = (-16).dp)
            .clip(RoundedCornerShape(20.dp))
            .background(FoodColors.card)
            .border(1.dp, FoodColors.border, RoundedCornerShape(20.dp))
            .padding(vertical = 16.dp),
    ) {
        stats.forEach { (data, emoji) ->
            val (label, value, color) = data
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(emoji, fontSize = 20.sp)
                Text("$value", fontFamily = AppFonts.nunito, fontWeight = FontWeight.Black, fontSize = 22.sp, color = color)
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = FoodColors.mutedForeground)
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, iconBg: Color, iconTint: Color, title: String, actionLabel: String?, onAction: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(8.dp)).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(13.dp))
            }
            Text(title, fontFamily = AppFonts.nunito, fontWeight = FontWeight.Black, fontSize = 14.sp, color = FoodColors.foreground)
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = FoodColors.primary)
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = FoodColors.primary, modifier = Modifier.size(13.dp))
            }
        }
    }
}

@Composable
private fun UrgentSection(urgent: List<FoodItem>, onNavigate: (Tab) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        SectionHeader(Icons.Filled.LocalFireDepartment, Color(0xFFFFF7ED), Color(0xFFEA580C), "지금 드세요", "전체", { onNavigate(Tab.FOODS) })
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            urgent.forEach { item ->
                val days = getDays(item.expiryDate)
                val status = getStatus(days)
                Column(
                    modifier = Modifier
                        .width(100.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(status.bg)
                        .border(1.5.dp, status.ring, RoundedCornerShape(20.dp))
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(item.emoji, fontSize = 30.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.name,
                        fontFamily = AppFonts.nunito,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = FoodColors.foreground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(6.dp))
                    Box(modifier = Modifier.clip(CircleShape).background(status.bar).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(status.label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpiredAlert(count: Int, names: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFEF2F2))
            .border(1.5.dp, Color(0xFFFECACA), RoundedCornerShape(20.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFEE2E2)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
        }
        Column {
            Text("유통기한 초과 ${count}개", fontFamily = AppFonts.nunito, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFFB91C1C))
            Text(names, fontSize = 12.sp, color = Color(0xFFEF4444))
        }
    }
}

@Composable
private fun ThisWeekSection(items: List<FoodItem>, onNavigate: (Tab) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
        SectionHeader(Icons.Filled.Schedule, Color(0xFFFEF3C7), Color(0xFFD97706), "이번 주 만료 예정", "전체보기", { onNavigate(Tab.FOODS) })
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { item ->
                val days = getDays(item.expiryDate)
                val status = getStatus(days)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(FoodColors.card)
                        .border(1.dp, FoodColors.border, RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(status.bg), contentAlignment = Alignment.Center) {
                        Text(item.emoji, fontSize = 18.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, fontFamily = AppFonts.nunito, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FoodColors.foreground)
                        Text(fmtDate(item.expiryDate), fontFamily = AppFonts.dmMono, fontSize = 11.sp, color = FoodColors.mutedForeground)
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(status.bg)
                            .border(1.dp, status.ring, CircleShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text(status.label, fontSize = 12.sp, fontWeight = FontWeight.Black, color = status.color)
                    }
                }
            }
        }
    }
}

@Composable
private fun FreshBanner(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFF0FDF4), Color(0xFFDCFCE7))))
            .border(1.5.dp, Color(0xFFBBF7D0), RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(16.dp)).background(FoodColors.primary), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text("신선한 식자재 ${count}개", fontFamily = AppFonts.nunito, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF166534))
            Text("6일 이상 여유 있어요", fontFamily = AppFonts.dmSans, fontSize = 12.sp, color = Color(0xFF4ADE80))
        }
        Text("🥗", fontSize = 22.sp)
    }
}
