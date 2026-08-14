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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

private val PageBackground = Color(0xFFF7F3EC)
private val Ink = Color(0xFF1A1410)
private val Muted = Color(0xFF7A7060)
private val Green = Color(0xFF18A05C)
private val Orange = Color(0xFFEA580C)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CleanOutScreen(viewModel: CleanOutViewModel = viewModel(), modifier: Modifier = Modifier) {
    val selectableState = viewModel.selectableState
    val recommendationState = viewModel.recommendationState
    val selectedIds = viewModel.selectedFridgeItemIds

    LazyColumn(
        modifier = modifier.fillMaxSize().background(PageBackground),
        contentPadding = PaddingValues(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 24.dp),
    ) {
        item {
            Column {
                Text("냉털", color = Ink, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("가진 재료로 만들 수 있는 요리를 추천해 드려요", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            }
        }

        item {
            Column(modifier = Modifier.padding(top = 24.dp)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("재료 선택", color = Ink, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(
                        "  ${selectedIds.size}개 선택",
                        color = Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 1.dp),
                    )
                }
                Spacer(12.dp)

                when (selectableState) {
                    is SelectableItemsState.Loading -> {
                        Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Green)
                        }
                    }
                    is SelectableItemsState.Error -> {
                        Text(selectableState.message, color = Orange, fontSize = 13.sp)
                    }
                    is SelectableItemsState.Success -> {
                        if (selectableState.items.isEmpty()) {
                            Text("냉장고에 등록된 재료가 없어요", color = Muted, fontSize = 13.sp)
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
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Green,
                        disabledContainerColor = Green.copy(alpha = 0.4f),
                    ),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Text("레시피 추천 받기", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        item {
            Text("이런 요리는 어때요?", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 24.dp))
        }

        when (recommendationState) {
            is RecommendationState.Idle -> {
                item { InfoCard("재료를 선택하고 레시피 추천을 받아보세요") }
            }
            is RecommendationState.Loading -> {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Green)
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
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(message, color = Muted, fontSize = 13.sp)
        }
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
                    disabled -> Color(0xFFF3F3F1)
                    selected -> Color(0xFFF0FDF4)
                    else -> Color.White
                },
            )
            .border(
                1.5.dp,
                when {
                    disabled -> Color(0x171A1410)
                    selected -> Green
                    else -> Color(0x171A1410)
                },
                CircleShape,
            )
            .clickable(enabled = !disabled) { onClick() }
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            label,
            color = when {
                disabled -> Muted
                selected -> Green
                else -> Ink
            },
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun RecipeCard(recommendation: RecipeRecommendationDto, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val recipe = recommendation.recipe

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(recipe.title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
                if (recommendation.expiringTargeted) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFFFF7ED))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        Text("임박재료 활용", color = Orange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            recommendation.recommendationReason?.let {
                Text(it, color = Muted, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.padding(top = 6.dp))
            }

            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                recipe.method?.let { Text(it, color = Muted, fontSize = 12.sp) }
                recipe.cooktime?.let { Text(it, color = Muted, fontSize = 12.sp) }
                recipe.portion?.let { Text(it, color = Muted, fontSize = 12.sp) }
            }

            if (recommendation.ingredients.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    recommendation.ingredients.take(5).forEach { ingredient ->
                        val (bg, fg) = when (ingredient.ownershipStatus) {
                            OwnershipStatus.AVAILABLE -> Color(0xFFF0FDF4) to Green
                            OwnershipStatus.EXPIRING -> Color(0xFFFFF7ED) to Orange
                            OwnershipStatus.MISSING -> Color(0xFFF3F3F1) to Muted
                        }
                        Row(modifier = Modifier.clip(CircleShape).background(bg).padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(ingredient.name, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            recommendation.externalSearch?.let { external ->
                Text(
                    "만개의레시피에서 보기 ↗",
                    color = Green,
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
}
