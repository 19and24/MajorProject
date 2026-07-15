package com.example.cleanoutfridge.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cleanoutfridge.model.FoodItem
import com.example.cleanoutfridge.model.fmtDate
import com.example.cleanoutfridge.model.getDays
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PageBackground = Color(0xFFF7F3EC)
private val Ink = Color(0xFF1A1410)
private val Muted = Color(0xFF7A7060)
private val Green = Color(0xFF18A05C)
private val Orange = Color(0xFFEA580C)
private val Red = Color(0xFFDC2626)

@Composable
fun HomeScreen(
    items: List<FoodItem>,
    today: LocalDate = LocalDate.now(),
    onNotificationsClick: () -> Unit = {},
) {
    val foodsWithDays = remember(items, today) { items.map { it to getDays(it.expiryDate) } }
    val urgent = foodsWithDays.filter { it.second in 0..3 }
    val expiringSoon = foodsWithDays.filter { it.second in 0..7 }
    val expired = foodsWithDays.filter { it.second < 0 }
    val thisWeek = foodsWithDays.filter { it.second in 0..7 }
    val fresh = foodsWithDays.count { it.second >= 8 }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(PageBackground),
        contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { Header(today, onNotificationsClick) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("▤", items.size, "전체 식자재", Green, Color(0xFFF0FDF4), Modifier.weight(1f))
                StatCard("!", expiringSoon.size, "임박", Orange, Color(0xFFFFF7ED), Modifier.weight(1f))
                StatCard("×", expired.size, "만료", Red, Color(0xFFFEF2F2), Modifier.weight(1f))
            }
        }

        if (items.isEmpty()) {
            item { EmptyCard("아직 등록된 식재료가 없어요") }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("🔥 지금 드세요", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                        Text("전체  ›", color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    if (urgent.isEmpty()) {
                        EmptyCard("임박한 식재료가 없어요")
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            urgent.take(3).forEach { (food, days) ->
                                UrgentFoodCard(food, days, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            if (expired.isNotEmpty()) {
                item { ExpiredWarning(expired.first().first, expired.size) }
            }

            item { Text("📅 이번 주 만료 예정", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Black) }
            if (thisWeek.isEmpty()) {
                item { EmptyCard("이번 주 만료 예정인 식재료가 없어요") }
            } else {
                items(thisWeek, key = { "week-${it.first.id}" }) { (food, days) ->
                    WeekFoodRow(food, days)
                }
            }

            item { FreshFoodCard(fresh) }
        }
    }
}

@Composable
private fun Header(today: LocalDate, onNotificationsClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = today.format(DateTimeFormatter.ofPattern("M월 d일", Locale.KOREAN)),
                color = Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.2.sp,
            )
            Text(
                text = "${greeting()} 👋",
                color = Ink,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        IconButton(
            onClick = onNotificationsClick,
            modifier = Modifier
                .size(40.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(1.dp, Color(0x171A1410), RoundedCornerShape(16.dp)),
        ) { Text("🔔", fontSize = 18.sp) }
    }
}

@Composable
private fun StatCard(
    icon: String,
    count: Int,
    label: String,
    accent: Color,
    background: Color,
    modifier: Modifier,
) {
    Card(
        modifier = modifier.height(129.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = background),
        border = BorderStroke(1.5.dp, accent.copy(alpha = .13f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier.size(32.dp).background(accent.copy(alpha = .13f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(icon, color = accent, fontWeight = FontWeight.Black) }
            Text(
                text = count.toString(),
                color = accent,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(top = 10.dp),
            )
            Text(label, color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun UrgentFoodCard(food: FoodItem, days: Int, modifier: Modifier) {
    Card(
        modifier = modifier.height(114.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF7ED)),
        border = BorderStroke(1.dp, Color(0xFFFED7AA)),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(food.emoji, fontSize = 28.sp)
            Text(
                food.name,
                color = Ink,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                textAlign = TextAlign.Center,
            )
            Surface(color = Color(0xFFF97316), shape = CircleShape) {
                Text(
                    daysLabel(days),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
            }
        }
    }
}

@Composable
private fun ExpiredWarning(food: FoodItem, count: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        border = BorderStroke(1.5.dp, Color(0xFFFECACA)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Text("!", color = Red, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text("유통기한 초과", color = Color(0xFFC10007), fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text(
                    "${food.name}을(를) 확인하세요.${if (count > 1) " 외 ${count - 1}개" else ""}",
                    color = Color(0xFFE7000B),
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun WeekFoodRow(food: FoodItem, days: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0x171A1410)),
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(food.emoji, fontSize = 24.sp)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(food.name, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(fmtDate(food.expiryDate), color = Color(0xFF9A9080), fontSize = 12.sp)
            }
            Text(
                daysLabel(days),
                color = if (days <= 2) Orange else Color(0xFFCA8A04),
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun FreshFoodCard(count: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
        border = BorderStroke(1.5.dp, Color(0xFFBBF7D0)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(Green.copy(alpha = .1f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("✓", color = Green, fontWeight = FontWeight.Black) }
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text("신선한 식자재 ${count}개", color = Color(0xFF16A34A), fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text("8일 이상 남은 식자재예요", color = Color(0xFF4ADE80), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EmptyCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) {
        Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            Text(message, color = Muted)
        }
    }
}

private fun greeting(hour: Int = LocalTime.now().hour): String = when (hour) {
    in 5..11 -> "좋은 아침이에요"
    in 12..17 -> "좋은 오후예요"
    else -> "좋은 저녁이에요"
}

private fun daysLabel(days: Int): String = when {
    days < 0 -> "${-days}일 지남"
    days == 0 -> "오늘 만료"
    else -> "D-$days"
}
