package com.example.majorproject.food.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.majorproject.food.model.Filter
import com.example.majorproject.food.model.FoodItem
import com.example.majorproject.food.model.getDays
import com.example.majorproject.food.ui.components.FoodCard
import com.example.majorproject.food.ui.theme.AppFonts
import com.example.majorproject.food.ui.theme.FoodColors

@Composable
fun FoodsScreen(
    items: List<FoodItem>,
    onDelete: (String) -> Unit,
    initialFilter: Filter = Filter.ALL,
    modifier: Modifier = Modifier,
) {
    var filter by rememberSaveable { mutableStateOf(initialFilter) }
    var search by rememberSaveable { mutableStateOf("") }

    val counts = remember(items) {
        mapOf(
            Filter.ALL to items.size,
            Filter.URGENT to items.count { getDays(it.expiryDate) in 0..5 },
            Filter.EXPIRED to items.count { getDays(it.expiryDate) < 0 },
            Filter.FRESH to items.count { getDays(it.expiryDate) > 5 },
        )
    }

    val filtered = remember(items, filter, search) {
        items.filter { item ->
            val d = getDays(item.expiryDate)
            when (filter) {
                Filter.URGENT -> d in 0..5
                Filter.EXPIRED -> d < 0
                Filter.FRESH -> d > 5
                Filter.ALL -> true
            }
        }.filter { it.name.contains(search) || it.category.contains(search) }
            .sortedBy { getDays(it.expiryDate) }
    }

    val filterConfig = listOf(
        Triple(Filter.ALL, "전체", "🥗"),
        Triple(Filter.URGENT, "임박", "🔥"),
        Triple(Filter.EXPIRED, "만료", "⚠️"),
        Triple(Filter.FRESH, "신선", "✨"),
    )

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 48.dp, bottom = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("냉장고 현황", fontFamily = AppFonts.nunito, fontWeight = FontWeight.Black, fontSize = 24.sp, color = FoodColors.foreground)
                    Box(modifier = Modifier.clip(CircleShape).background(FoodColors.primary).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text("${items.size}개", fontFamily = AppFonts.dmMono, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("식품명 또는 카테고리...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (search.isNotEmpty()) {
                            IconButton(onClick = { search = "" }) {
                                Icon(Icons.Filled.Close, contentDescription = "지우기")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FoodColors.primary,
                        unfocusedBorderColor = FoodColors.border,
                        focusedContainerColor = FoodColors.card,
                        unfocusedContainerColor = FoodColors.card,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    filterConfig.forEach { (id, label, emoji) ->
                        val selected = filter == id
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (selected) FoodColors.primary else FoodColors.card)
                                .border(1.5.dp, if (selected) FoodColors.primary else FoodColors.border, CircleShape)
                                .clickable { filter = id }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(emoji, fontSize = 11.sp)
                            Text(
                                label,
                                fontFamily = AppFonts.nunito,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (selected) Color.White else FoodColors.mutedForeground,
                            )
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(if (selected) Color.White.copy(alpha = 0.25f) else FoodColors.muted),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${counts[id]}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (selected) Color.White else FoodColors.mutedForeground,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("🍽️", fontSize = 48.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (search.isNotEmpty()) "\"$search\" 검색 결과 없음" else "식자재가 없어요",
                        fontFamily = AppFonts.nunito,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = FoodColors.mutedForeground,
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { item ->
                FoodCard(
                    item = item,
                    onDelete = { onDelete(item.id) },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 5.dp),
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
