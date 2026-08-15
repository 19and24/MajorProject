package com.example.cleanoutfridge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cleanoutfridge.model.FoodItem
import com.example.cleanoutfridge.model.categoryById
import com.example.cleanoutfridge.model.fmtDate
import com.example.cleanoutfridge.model.getDays
import com.example.cleanoutfridge.ui.theme.AppFonts
import com.example.cleanoutfridge.ui.theme.FoodColors

@Composable
fun FridgeScreen(
    items: List<FoodItem>,
    onDelete: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedCategory by remember { mutableStateOf<String?>(null) }

    val categoryCounts = remember(items) {
        items.groupingBy { it.category }.eachCount().entries.sortedByDescending { it.value }
    }
    val filtered = remember(items, selectedCategory) {
        items.filter { selectedCategory == null || it.category == selectedCategory }
            .sortedBy { getDays(it.expiryDate) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(FoodColors.background),
        contentPadding = PaddingValues(top = 20.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("냉장고", color = FoodColors.foreground, fontFamily = AppFonts.fraunces, fontSize = 26.sp, fontWeight = FontWeight(700))
                Box(
                    modifier = Modifier.background(FoodColors.primary, RoundedCornerShape(5.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text("${items.size}개", color = FoodColors.primaryForeground, fontFamily = AppFonts.mono, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (categoryCounts.isNotEmpty()) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item {
                        CategoryCard(
                            emoji = "🥗",
                            label = "전체",
                            count = items.size,
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                        )
                    }
                    items(categoryCounts, key = { it.key }) { entry ->
                        val cat = categoryById(entry.key)
                        CategoryCard(
                            emoji = cat?.emoji ?: "🍱",
                            label = entry.key,
                            count = entry.value,
                            selected = selectedCategory == entry.key,
                            onClick = { selectedCategory = if (selectedCategory == entry.key) null else entry.key },
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                        .height(96.dp)
                        .background(FoodColors.card, RoundedCornerShape(14.dp))
                        .border(1.dp, FoodColors.border, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (selectedCategory == null) "냉장고가 비어있어요" else "${selectedCategory} 카테고리에 재료가 없어요",
                        color = FoodColors.mutedForeground,
                        fontFamily = AppFonts.inter,
                        fontSize = 13.sp,
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { food ->
                FridgeFoodRow(
                    food = food,
                    days = getDays(food.expiryDate),
                    onDelete = { onDelete(food.id) },
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(
    emoji: String,
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(76.dp)
            .background(if (selected) FoodColors.primarySoft else FoodColors.card, RoundedCornerShape(14.dp))
            .border(1.dp, if (selected) FoodColors.primary else FoodColors.border, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 20.sp)
        Text(
            label,
            color = if (selected) FoodColors.primary else FoodColors.foreground,
            fontFamily = AppFonts.inter,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(top = 5.dp),
        )
        Text(
            "${count}개",
            color = if (selected) FoodColors.primary else FoodColors.mutedForeground,
            fontFamily = AppFonts.mono,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}

@Composable
private fun FridgeFoodRow(food: FoodItem, days: Int, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val accent = when {
        days < 0 -> FoodColors.expired
        days <= 3 -> FoodColors.urgent
        else -> FoodColors.primary
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(FoodColors.card, RoundedCornerShape(14.dp))
            .border(1.dp, FoodColors.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(40.dp).background(accent.copy(alpha = .1f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center,
        ) { Text(food.emoji, fontSize = 20.sp) }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(food.name, color = FoodColors.foreground, fontFamily = AppFonts.inter, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "${food.category} · ${fmtDate(food.expiryDate)}",
                color = FoodColors.mutedForeground,
                fontFamily = AppFonts.mono,
                fontSize = 11.sp,
            )
        }
        Text(daysLabel(days), color = accent, fontFamily = AppFonts.mono, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = "삭제", tint = FoodColors.mutedForeground, modifier = Modifier.size(18.dp))
        }
    }
}

private fun daysLabel(days: Int): String = when {
    days < 0 -> "${-days}일 지남"
    days == 0 -> "오늘 만료"
    else -> "D-$days"
}
