package com.example.cleanoutfridge.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cleanoutfridge.data.DashboardState
import com.example.cleanoutfridge.data.DashboardViewModel
import com.example.cleanoutfridge.data.remote.dto.DashboardRecommendedRecipeDto
import com.example.cleanoutfridge.model.FoodItem
import com.example.cleanoutfridge.model.fmtDate
import com.example.cleanoutfridge.model.getDays
import com.example.cleanoutfridge.ui.theme.AppFonts
import com.example.cleanoutfridge.ui.theme.FoodColors
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    items: List<FoodItem>,
    today: LocalDate = LocalDate.now(),
    onNotificationsClick: () -> Unit = {},
    onRecipesClick: () -> Unit = {},
    onRecipeClick: (Int) -> Unit = {},
    dashboardViewModel: DashboardViewModel = viewModel(),
) {
    val foodsWithDays = remember(items, today) { items.map { it to getDays(it.expiryDate) } }
    val urgent = foodsWithDays.filter { it.second in 0..3 }
    val expiringSoon = foodsWithDays.filter { it.second in 0..7 }
    val expired = foodsWithDays.filter { it.second < 0 }
    val thisWeek = foodsWithDays.filter { it.second in 0..7 }
    val fresh = foodsWithDays.count { it.second >= 8 }
    val dashboardState = dashboardViewModel.state
    val upcomingNotificationCount = (dashboardState as? DashboardState.Success)?.summary?.upcomingNotificationCount ?: 0

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(FoodColors.background),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item { Header(today, upcomingNotificationCount, onNotificationsClick) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("전체", items.size, FoodColors.foreground, Modifier.weight(1f))
                StatCard("임박", expiringSoon.size, FoodColors.urgent, Modifier.weight(1f))
                StatCard("만료", expired.size, FoodColors.expired, Modifier.weight(1f))
            }
        }

        item {
            RecommendedRecipesSection(
                state = dashboardState,
                onRecipesClick = onRecipesClick,
                onRecipeClick = onRecipeClick,
            )
        }

        if (items.isEmpty()) {
            item { EmptyCard("아직 등록된 식재료가 없어요") }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionHeading(title = "지금 드세요", trailing = "전체 보기", onTrailingClick = null)
                    if (urgent.isEmpty()) {
                        EmptyCard("임박한 식재료가 없어요")
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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

            item { SectionHeading(title = "이번 주 만료 예정", trailing = null, onTrailingClick = null) }
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
private fun SectionHeading(title: String, trailing: String?, onTrailingClick: (() -> Unit)?) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(3.dp).height(14.dp).background(FoodColors.primary))
        Text(
            title,
            color = FoodColors.foreground,
            fontFamily = AppFonts.fraunces,
            fontSize = 17.sp,
            fontWeight = FontWeight(700),
            modifier = Modifier.padding(start = 8.dp).weight(1f),
        )
        if (trailing != null) {
            Text(
                trailing,
                color = FoodColors.primary,
                fontFamily = AppFonts.inter,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.let { if (onTrailingClick != null) it.clickable { onTrailingClick() } else it },
            )
        }
    }
}

@Composable
private fun Header(today: LocalDate, upcomingNotificationCount: Int, onNotificationsClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = today.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)).uppercase(),
                color = FoodColors.mutedForeground,
                fontFamily = AppFonts.mono,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.4.sp,
            )
            Text(
                text = greeting(),
                color = FoodColors.foreground,
                fontFamily = AppFonts.fraunces,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight(700),
                modifier = Modifier.padding(top = 3.dp),
            )
        }
        Box {
            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier
                    .size(42.dp)
                    .background(FoodColors.card, RoundedCornerShape(13.dp))
                    .border(1.dp, FoodColors.border, RoundedCornerShape(13.dp)),
            ) { Text("🔔", fontSize = 17.sp) }
            if (upcomingNotificationCount > 0) {
                Surface(
                    color = FoodColors.urgent,
                    shape = CircleShape,
                    modifier = Modifier.align(Alignment.TopEnd).size(18.dp).border(2.dp, FoodColors.background, CircleShape),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            if (upcomingNotificationCount > 9) "9+" else "$upcomingNotificationCount",
                            color = Color.White,
                            fontFamily = AppFonts.mono,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendedRecipesSection(
    state: DashboardState,
    onRecipesClick: () -> Unit,
    onRecipeClick: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading(title = "오늘의 추천 레시피", trailing = "레시피 검색 ›", onTrailingClick = onRecipesClick)

        val recipes = (state as? DashboardState.Success)?.summary?.recommendedRecipes.orEmpty()
        when {
            state is DashboardState.Loading -> EmptyCard("추천 레시피를 불러오는 중이에요")
            recipes.isEmpty() -> EmptyCard("추천할 레시피가 아직 없어요")
            else -> {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(recipes, key = { it.id }) { recipe ->
                        RecommendedRecipeCard(recipe, onClick = { onRecipeClick(recipe.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun RecommendedRecipeCard(recipe: DashboardRecommendedRecipeDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(162.dp).height(98.dp).clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = FoodColors.primarySoft),
        border = BorderStroke(1.dp, FoodColors.primarySoftBorder),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Text(
                recipe.title,
                color = FoodColors.foreground,
                fontFamily = AppFonts.fraunces,
                fontSize = 14.sp,
                fontWeight = FontWeight(700),
                maxLines = 2,
            )
            recipe.score?.let {
                Text(
                    "매칭도 ${(it * 100).toInt()}%",
                    color = FoodColors.primary,
                    fontFamily = AppFonts.mono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    count: Int,
    accent: Color,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .height(88.dp)
            .background(FoodColors.card, RoundedCornerShape(14.dp))
            .border(1.dp, FoodColors.border, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(modifier = Modifier.width(18.dp).height(3.dp).background(accent))
        Column {
            Text(
                text = count.toString(),
                color = accent,
                fontFamily = AppFonts.mono,
                fontSize = 24.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(label, color = FoodColors.mutedForeground, fontFamily = AppFonts.inter, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun UrgentFoodCard(food: FoodItem, days: Int, modifier: Modifier) {
    Column(
        modifier = modifier
            .height(122.dp)
            .background(FoodColors.urgentSoft, RoundedCornerShape(14.dp))
            .border(1.dp, FoodColors.urgentSoftBorder, RoundedCornerShape(14.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(food.emoji, fontSize = 26.sp)
        Text(
            food.name,
            color = FoodColors.foreground,
            fontFamily = AppFonts.inter,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
        DayStamp(days = days, color = FoodColors.urgent)
    }
}

/** The signature "produce stamp": mono digits inside a hairline-bordered tag, like a crate label. */
@Composable
private fun DayStamp(days: Int, color: Color) {
    Box(
        modifier = Modifier
            .background(Color.White, RoundedCornerShape(5.dp))
            .border(1.dp, color, RoundedCornerShape(5.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    ) {
        Text(
            daysLabel(days),
            color = color,
            fontFamily = AppFonts.mono,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun ExpiredWarning(food: FoodItem, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FoodColors.expiredSoft, RoundedCornerShape(14.dp))
            .border(1.dp, FoodColors.expiredSoftBorder, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text("!", color = FoodColors.expired, fontFamily = AppFonts.fraunces, fontWeight = FontWeight(900), fontSize = 18.sp)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text("유통기한 초과", color = FoodColors.expired, fontFamily = AppFonts.fraunces, fontSize = 14.sp, fontWeight = FontWeight(700))
            Text(
                "${food.name}을(를) 확인하세요.${if (count > 1) " 외 ${count - 1}개" else ""}",
                color = FoodColors.expired.copy(alpha = 0.85f),
                fontFamily = AppFonts.inter,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun WeekFoodRow(food: FoodItem, days: Int) {
    val accent = if (days <= 2) FoodColors.urgent else FoodColors.warn
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FoodColors.card, RoundedCornerShape(14.dp))
            .border(1.dp, FoodColors.border, RoundedCornerShape(14.dp))
            .height(64.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(food.emoji, fontSize = 22.sp)
        Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(food.name, color = FoodColors.foreground, fontFamily = AppFonts.inter, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(fmtDate(food.expiryDate), color = FoodColors.mutedForeground, fontFamily = AppFonts.mono, fontSize = 11.sp)
        }
        DayStamp(days = days, color = accent)
    }
}

@Composable
private fun FreshFoodCard(count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FoodColors.primarySoft, RoundedCornerShape(14.dp))
            .border(1.dp, FoodColors.primarySoftBorder, RoundedCornerShape(14.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(38.dp).background(FoodColors.primary.copy(alpha = .12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("✓", color = FoodColors.primary, fontWeight = FontWeight.Black) }
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text("신선한 식자재 ${count}개", color = FoodColors.primaryDark, fontFamily = AppFonts.fraunces, fontSize = 14.sp, fontWeight = FontWeight(700))
            Text("8일 이상 남은 식자재예요", color = FoodColors.primary, fontFamily = AppFonts.inter, fontSize = 12.sp)
        }
    }
}

@Composable
private fun EmptyCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(FoodColors.card, RoundedCornerShape(14.dp))
            .border(1.dp, FoodColors.border, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, color = FoodColors.mutedForeground, fontFamily = AppFonts.inter, fontSize = 13.sp)
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
