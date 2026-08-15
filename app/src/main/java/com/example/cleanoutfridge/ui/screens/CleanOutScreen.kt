package com.example.cleanoutfridge.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cleanoutfridge.data.CleanOutViewModel
import com.example.cleanoutfridge.data.RecommendationState
import com.example.cleanoutfridge.data.SelectableItemsState
import com.example.cleanoutfridge.data.remote.dto.OwnershipStatus
import com.example.cleanoutfridge.data.remote.dto.RecipeRecommendationDto
import com.example.cleanoutfridge.data.remote.dto.SelectableFridgeItemDto
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import com.example.cleanoutfridge.ui.theme.AppFonts
import com.example.cleanoutfridge.ui.theme.FoodColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CleanOutScreen(viewModel: CleanOutViewModel = viewModel(), modifier: Modifier = Modifier) {
    val selectableState = viewModel.selectableState
    val recommendationState = viewModel.recommendationState
    val selectedIds = viewModel.selectedFridgeItemIds

    LazyColumn(
        modifier = modifier.fillMaxSize().background(FoodColors.background),
        contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 24.dp),
    ) {
        item {
            Column {
                Text("냉털", color = FoodColors.foreground, fontFamily = AppFonts.fraunces, fontSize = 26.sp, fontWeight = FontWeight(700))
                Text(
                    "가진 재료로 만들 수 있는 요리를 추천해 드려요",
                    color = FoodColors.mutedForeground,
                    fontFamily = AppFonts.inter,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        item {
            Column(modifier = Modifier.padding(top = 24.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("재료 선택", color = FoodColors.foreground, fontFamily = AppFonts.fraunces, fontSize = 18.sp, fontWeight = FontWeight(700))
                    Text(
                        "  ${selectedIds.size}개 선택",
                        color = FoodColors.mutedForeground,
                        fontFamily = AppFonts.mono,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 2.dp),
                    )
                }
                Spacer(12.dp)

                when (selectableState) {
                    is SelectableItemsState.Loading -> {
                        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = FoodColors.primary)
                        }
                    }
                    is SelectableItemsState.Error -> {
                        Text(selectableState.message, color = FoodColors.urgent, fontFamily = AppFonts.inter, fontSize = 13.sp)
                    }
                    is SelectableItemsState.Success -> {
                        if (selectableState.items.isEmpty()) {
                            Text("냉장고에 등록된 재료가 없어요", color = FoodColors.mutedForeground, fontFamily = AppFonts.inter, fontSize = 13.sp)
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                selectableState.items.forEach { row ->
                                    IngredientChip(
                                        row = row,
                                        selected = row.fridgeItemId in selectedIds,
                                        onClick = { if (row.selectable) viewModel.toggleSelection(row.fridgeItemId) },
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(20.dp)
                Button(
                    onClick = { viewModel.requestRecommendations() },
                    enabled = selectedIds.isNotEmpty() && recommendationState !is RecommendationState.Loading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FoodColors.primary,
                        disabledContainerColor = FoodColors.primary.copy(alpha = 0.4f),
                    ),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text("레시피 추천 받기", color = Color.White, fontFamily = AppFonts.fraunces, fontSize = 15.sp, fontWeight = FontWeight(700))
                }
            }
        }

        item {
            Text(
                "이런 요리는 어때요?",
                color = FoodColors.foreground,
                fontFamily = AppFonts.fraunces,
                fontSize = 16.sp,
                fontWeight = FontWeight(700),
                modifier = Modifier.padding(top = 24.dp),
            )
        }

        when (recommendationState) {
            is RecommendationState.Idle -> {
                item { InfoCard("재료를 선택하고 레시피 추천을 받아보세요") }
            }
            is RecommendationState.Loading -> {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = FoodColors.primary)
                    }
                }
            }
            is RecommendationState.Error -> {
                item { InfoCard(recommendationState.message) }
            }
            is RecommendationState.Success -> {
                if (recommendationState.recommendations.isEmpty()) {
                    item {
                        InfoCard(recommendationState.emptyReason ?: "선택한 재료로 만들 수 있는 레시피가 없어요")
                    }
                } else {
                    recommendationState.recommendations.forEach { recommendation ->
                        item { RecipeCard(recommendation, modifier = Modifier.padding(top = 12.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Spacer(height: androidx.compose.ui.unit.Dp) {
    androidx.compose.foundation.layout.Spacer(Modifier.height(height))
}

@Composable
private fun InfoCard(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .background(FoodColors.card, RoundedCornerShape(14.dp))
            .border(1.dp, FoodColors.border, RoundedCornerShape(14.dp))
            .padding(20.dp),
    ) {
        Text(message, color = FoodColors.mutedForeground, fontFamily = AppFonts.inter, fontSize = 13.sp)
    }
}

@Composable
private fun IngredientChip(row: SelectableFridgeItemDto, selected: Boolean, onClick: () -> Unit) {
    val disabled = !row.selectable
    val label = row.ingredient.name + if (row.status == com.example.cleanoutfridge.data.remote.dto.FridgeStockStatus.EXPIRING) " ⏰" else ""
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .clip(CircleShape)
            .background(
                when {
                    disabled -> FoodColors.muted
                    selected -> FoodColors.primarySoft
                    else -> FoodColors.card
                },
            )
            .border(
                1.5.dp,
                when {
                    disabled -> FoodColors.border
                    selected -> FoodColors.primary
                    else -> FoodColors.border
                },
                CircleShape,
            )
            .clickable(enabled = !disabled) { onClick() }
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            label,
            color = when {
                disabled -> FoodColors.mutedForeground
                selected -> FoodColors.primary
                else -> FoodColors.foreground
            },
            fontFamily = AppFonts.inter,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun RecipeCard(recommendation: RecipeRecommendationDto, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val recipe = recommendation.recipe

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(FoodColors.card, RoundedCornerShape(14.dp))
            .border(1.dp, FoodColors.border, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                recipe.title,
                color = FoodColors.foreground,
                fontFamily = AppFonts.fraunces,
                fontSize = 16.sp,
                fontWeight = FontWeight(700),
                modifier = Modifier.weight(1f),
            )
            if (recommendation.expiringTargeted) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(FoodColors.urgentSoft)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("임박재료 활용", color = FoodColors.urgent, fontFamily = AppFonts.inter, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        recommendation.recommendationReason?.let {
            Text(it, color = FoodColors.mutedForeground, fontFamily = AppFonts.inter, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 6.dp))
        }

        Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            recipe.method?.let { Text(it, color = FoodColors.mutedForeground, fontFamily = AppFonts.mono, fontSize = 11.sp) }
            recipe.cooktime?.let { Text(it, color = FoodColors.mutedForeground, fontFamily = AppFonts.mono, fontSize = 11.sp) }
            recipe.portion?.let { Text(it, color = FoodColors.mutedForeground, fontFamily = AppFonts.mono, fontSize = 11.sp) }
        }

        if (recommendation.ingredients.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                recommendation.ingredients.take(5).forEach { ingredient ->
                    val (bg, fg) = when (ingredient.ownershipStatus) {
                        OwnershipStatus.AVAILABLE -> FoodColors.primarySoft to FoodColors.primary
                        OwnershipStatus.EXPIRING -> FoodColors.urgentSoft to FoodColors.urgent
                        OwnershipStatus.MISSING -> FoodColors.muted to FoodColors.mutedForeground
                    }
                    Row(modifier = Modifier.clip(CircleShape).background(bg).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(ingredient.name, color = fg, fontFamily = AppFonts.inter, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        recommendation.externalSearch?.let { external ->
            Text(
                "만개의레시피에서 보기 ↗",
                color = FoodColors.primary,
                fontFamily = AppFonts.inter,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 14.dp)
                    .clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(external.url)))
                    },
            )
        }
    }
}
