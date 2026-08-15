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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cleanoutfridge.data.RecipeDetailState
import com.example.cleanoutfridge.data.RecipeDetailViewModel
import com.example.cleanoutfridge.data.remote.dto.RecipeIngredientDto
import com.example.cleanoutfridge.ui.theme.AppFonts
import com.example.cleanoutfridge.ui.theme.FoodColors

@Composable
fun RecipeDetailScreen(
    recipeId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeDetailViewModel = viewModel(),
) {
    val context = LocalContext.current
    LaunchedEffect(recipeId) { viewModel.load(recipeId) }
    val state = viewModel.state

    Column(modifier = modifier.fillMaxSize().background(FoodColors.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = FoodColors.foreground)
            }
            Text(
                "레시피",
                color = FoodColors.foreground,
                fontFamily = AppFonts.fraunces,
                fontSize = 20.sp,
                fontWeight = FontWeight(700),
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        when (state) {
            is RecipeDetailState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FoodColors.primary)
                }
            }
            is RecipeDetailState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = FoodColors.mutedForeground, fontFamily = AppFonts.inter)
                }
            }
            is RecipeDetailState.Success -> {
                val recipe = state.recipe
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item {
                        Column {
                            Text(recipe.title, color = FoodColors.foreground, fontFamily = AppFonts.fraunces, fontSize = 22.sp, fontWeight = FontWeight(700))
                            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                recipe.method?.let { InfoTag(it) }
                                recipe.cooktime?.let { InfoTag(it) }
                                recipe.portion?.let { InfoTag(it) }
                            }
                        }
                    }

                    if (recipe.ingredients.isNotEmpty()) {
                        item {
                            Text("재료", color = FoodColors.foreground, fontFamily = AppFonts.fraunces, fontSize = 16.sp, fontWeight = FontWeight(700))
                        }
                        items(recipe.ingredients, key = { it.ingredient.id }) { ingredient ->
                            IngredientRow(ingredient)
                        }
                    } else if (!recipe.ingredientAll.isNullOrBlank()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(FoodColors.card, RoundedCornerShape(14.dp))
                                    .border(1.dp, FoodColors.border, RoundedCornerShape(14.dp)),
                            ) {
                                Text(recipe.ingredientAll, color = FoodColors.mutedForeground, fontFamily = AppFonts.inter, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
                            }
                        }
                    }

                    item {
                        Text(
                            "만개의레시피에서 보기 ↗",
                            color = FoodColors.primary,
                            fontFamily = AppFonts.inter,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clickable {
                                    val query = java.net.URLEncoder.encode(recipe.title, "UTF-8")
                                    val url = "https://www.10000recipe.com/recipe/list.html?q=$query"
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoTag(text: String) {
    Row(
        modifier = Modifier
            .background(FoodColors.primarySoft, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Text(text, color = FoodColors.primary, fontFamily = AppFonts.mono, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun IngredientRow(ingredient: RecipeIngredientDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(FoodColors.card, RoundedCornerShape(12.dp))
            .border(1.dp, FoodColors.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            (ingredient.ingredient.emoji?.let { "$it " } ?: "") + ingredient.ingredient.name,
            color = FoodColors.foreground,
            fontFamily = AppFonts.inter,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
        )
        ingredient.amount?.let {
            Text(it, color = FoodColors.mutedForeground, fontFamily = AppFonts.mono, fontSize = 12.sp)
        }
    }
}
