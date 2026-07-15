package com.example.cleanoutfridge.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cleanoutfridge.R
import com.example.cleanoutfridge.model.FoodItem
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow

private val PageBackground = Color(0xFFF7F3EC)
private val Ink = Color(0xFF1A1410)
private val Muted = Color(0xFF7A7060)
private val Green = Color(0xFF18A05C)

private data class RecipeSuggestion(
    val title: String,
    val description: String,
    val tags: List<String>,
    val matchKeywords: List<String>,
    val imageRes: Int,
)

private val RECIPE_SUGGESTIONS = listOf(
    RecipeSuggestion(
        title = "치즈 닭가슴살 구이",
        description = "구운 닭가슴살에 치즈를 녹여 단백질은 채우고 퍽퍽함은 줄였어요.",
        tags = listOf("닭가슴살", "슬라이스 치즈"),
        matchKeywords = listOf("닭가슴살", "치즈"),
        imageRes = R.drawable.recipe_cheese_chicken_breast,
    ),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CleanOutScreen(items: List<FoodItem>, modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf(setOf<String>()) }
    var showRecommendation by remember { mutableStateOf(false) }
    val selectedNames = remember(selected, items) {
        items.filter { it.id in selected }.map { it.name }
    }
    val recommendations = remember(showRecommendation, selectedNames) {
        if (!showRecommendation) emptyList()
        else RECIPE_SUGGESTIONS.filter { r -> r.matchKeywords.any { kw -> selectedNames.any { name -> name.contains(kw) } } }
    }

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
                        "  ${selected.size}개 선택",
                        color = Muted,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 1.dp),
                    )
                }
                Spacer(12.dp)
                if (items.isEmpty()) {
                    Text("냉장고에 등록된 재료가 없어요", color = Muted, fontSize = 13.sp)
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items.forEach { food ->
                            IngredientChip(
                                label = food.name,
                                selected = food.id in selected,
                                onClick = {
                                    selected = if (food.id in selected) selected - food.id else selected + food.id
                                    showRecommendation = false
                                },
                            )
                        }
                    }
                }
                Spacer(20.dp)
                Button(
                    onClick = { showRecommendation = true },
                    enabled = selected.isNotEmpty(),
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

        if (showRecommendation) {
            item {
                Text("이런 요리는 어때요?", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 24.dp))
            }
            if (recommendations.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("선택한 재료로 만들 수 있는 레시피를 준비 중이에요", color = Muted, fontSize = 13.sp)
                        }
                    }
                }
            } else {
                recommendations.forEach { recipe ->
                    item { RecipeCard(recipe, modifier = Modifier.padding(top = 12.dp)) }
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
private fun IngredientChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .clip(CircleShape)
            .background(if (selected) Color(0xFFF0FDF4) else Color.White)
            .border(1.5.dp, if (selected) Green else Color(0x171A1410), CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Text(
            label,
            color = if (selected) Green else Ink,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

@Composable
private fun RecipeCard(recipe: RecipeSuggestion, modifier: Modifier = Modifier) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Image(
                painter = painterResource(recipe.imageRes),
                contentDescription = recipe.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(180.dp),
            )
            Column(modifier = Modifier.padding(16.dp)) {
                Text(recipe.title, color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Black)
                Text(
                    recipe.description,
                    color = Muted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Row(
                    modifier = Modifier.padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    recipe.tags.forEach { tag ->
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xFFF0FDF4))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Text(tag, color = Green, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Text(
                    "만개의레시피에서 보기 ↗",
                    color = Green,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 14.dp),
                )
            }
        }
    }
}
