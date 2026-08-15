package com.example.cleanoutfridge.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cleanoutfridge.model.FoodItem
import com.example.cleanoutfridge.model.categoryById
import com.example.cleanoutfridge.model.getDays
import com.example.cleanoutfridge.ui.theme.AppFonts
import com.example.cleanoutfridge.ui.theme.FoodColors

@Composable
fun StatsScreen(items: List<FoodItem>, modifier: Modifier = Modifier) {
    val expired = remember(items) { items.filter { getDays(it.expiryDate) < 0 } }
    val urgent = remember(items) { items.filter { getDays(it.expiryDate) in 0..2 } }
    val thisWeek = remember(items) { items.filter { getDays(it.expiryDate) in 0..7 } }
    val fresh = remember(items) { items.filter { getDays(it.expiryDate) > 7 } }

    val catCounts = remember(items) {
        items.groupingBy { it.category }.eachCount().entries.sortedByDescending { it.value }
    }

    val statusData = remember(items) {
        listOf(
            Triple("신선", fresh.size, FoodColors.fresh),
            Triple("이번 주", thisWeek.size - urgent.size, FoodColors.warn),
            Triple("임박", urgent.size, FoodColors.urgent),
            Triple("만료", expired.size, FoodColors.expired),
        ).filter { it.second > 0 }
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 44.dp, bottom = 20.dp)) {
                Text("소비 통계", fontFamily = AppFonts.fraunces, fontWeight = FontWeight(700), fontSize = 26.sp, color = FoodColors.foreground)
                Text("총 ${items.size}개 식재료 관리 중", fontFamily = AppFonts.mono, fontSize = 12.sp, color = FoodColors.mutedForeground)
            }
        }

        item {
            SummaryGrid(fresh = fresh.size, urgent = urgent.size, expired = expired.size, thisWeek = thisWeek.size)
        }

        if (statusData.isNotEmpty()) {
            item {
                StatsCard(title = "상태별 분포") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        statusData.forEach { (label, value, color) ->
                            Column {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(label, fontFamily = AppFonts.inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = FoodColors.foreground)
                                    Text("${value}개", fontFamily = AppFonts.mono, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = color)
                                }
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(FoodColors.muted),
                                ) {
                                    val fraction = if (items.isEmpty()) 0f else value.toFloat() / items.size
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(fraction.coerceIn(0f, 1f))
                                            .height(10.dp)
                                            .background(color),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            StatsCard(title = "카테고리별 현황") {
                if (catCounts.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        CategoryDonut(catCounts = catCounts)
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            catCounts.take(6).forEachIndexed { i, entry ->
                                val cat = categoryById(entry.key)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(modifier = Modifier.size(10.dp).background(FoodColors.pieColors[i % FoodColors.pieColors.size], RoundedCornerShape(50)))
                                    Text(
                                        "${cat?.emoji ?: ""} ${entry.key}",
                                        fontFamily = AppFonts.inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = FoodColors.foreground,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Text("${entry.value}", fontFamily = AppFonts.mono, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = FoodColors.mutedForeground)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(FoodColors.primarySoft)
                    .border(1.dp, FoodColors.primarySoftBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Text("절약 팁", fontFamily = AppFonts.fraunces, fontWeight = FontWeight(700), fontSize = 14.sp, color = FoodColors.primaryDark)
                Spacer(Modifier.height(4.dp))
                val tip = "임박한 식재료 ${urgent.size}개를 먼저 소비하면 낭비를 줄일 수 있어요." +
                    if (expired.isNotEmpty()) " 만료된 ${expired.size}개는 바로 확인해 주세요." else " 지금 상태를 잘 유지하고 있어요."
                Text(tip, fontFamily = AppFonts.inter, fontSize = 12.sp, color = FoodColors.primaryDark, lineHeight = 18.sp)
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun StatsCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(FoodColors.card)
            .border(1.dp, FoodColors.border, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text(title, fontFamily = AppFonts.fraunces, fontWeight = FontWeight(700), fontSize = 14.sp, color = FoodColors.foreground)
        Spacer(Modifier.height(16.dp))
        content()
    }
}

@Composable
private fun SummaryGrid(fresh: Int, urgent: Int, expired: Int, thisWeek: Int) {
    val cards = listOf(
        Quad("신선", fresh, FoodColors.fresh, FoodColors.freshSoft, FoodColors.freshSoftBorder),
        Quad("만료 임박", urgent, FoodColors.urgent, FoodColors.urgentSoft, FoodColors.urgentSoftBorder),
        Quad("만료됨", expired, FoodColors.expired, FoodColors.expiredSoft, FoodColors.expiredSoftBorder),
        Quad("이번 주", thisWeek, FoodColors.warn, FoodColors.warnSoft, FoodColors.warnSoftBorder),
    )
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        cards.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { card ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(card.bg)
                            .border(1.dp, card.ring, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        Box(modifier = Modifier.width(16.dp).height(3.dp).background(card.color))
                        Spacer(Modifier.height(10.dp))
                        Text("${card.count}", fontFamily = AppFonts.mono, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = card.color)
                        Spacer(Modifier.height(4.dp))
                        Text(card.label, fontFamily = AppFonts.inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = card.color)
                    }
                }
            }
        }
    }
}

private data class Quad(val label: String, val count: Int, val color: Color, val bg: Color, val ring: Color)

@Composable
private fun CategoryDonut(catCounts: List<Map.Entry<String, Int>>, modifier: Modifier = Modifier) {
    val total = catCounts.sumOf { it.value }
    Box(modifier = modifier.size(112.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(112.dp)) {
            val strokeWidth = 18.dp.toPx()
            val diameter = size.minDimension - strokeWidth
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)
            drawArc(
                color = FoodColors.muted,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth),
            )
            var startAngle = -90f
            catCounts.forEachIndexed { i, entry ->
                val sweep = if (total == 0) 0f else 360f * entry.value / total
                drawArc(
                    color = FoodColors.pieColors[i % FoodColors.pieColors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$total", fontFamily = AppFonts.mono, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = FoodColors.foreground)
            Text("전체", fontFamily = AppFonts.inter, fontSize = 9.sp, color = FoodColors.mutedForeground)
        }
    }
}
