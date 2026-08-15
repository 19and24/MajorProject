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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cleanoutfridge.data.RecipeSearchState
import com.example.cleanoutfridge.data.RecipeSearchViewModel
import com.example.cleanoutfridge.data.remote.dto.RecipeSummaryDto
import com.example.cleanoutfridge.ui.theme.AppFonts
import com.example.cleanoutfridge.ui.theme.FoodColors

@Composable
fun RecipeSearchScreen(
    onBack: () -> Unit,
    onRecipeClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeSearchViewModel = viewModel(),
) {
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
                "레시피 검색",
                color = FoodColors.foreground,
                fontFamily = AppFonts.fraunces,
                fontSize = 20.sp,
                fontWeight = FontWeight(700),
                modifier = Modifier.padding(start = 4.dp),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = viewModel.keyword,
                onValueChange = viewModel::onKeywordChange,
                placeholder = { Text("레시피 이름으로 검색", fontFamily = AppFonts.inter) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = AppFonts.inter, fontSize = 14.sp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = FoodColors.card,
                    unfocusedContainerColor = FoodColors.card,
                    focusedBorderColor = FoodColors.primary,
                    unfocusedBorderColor = FoodColors.border,
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { viewModel.search() }) {
                Text("검색", color = FoodColors.primary, fontFamily = AppFonts.inter, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        when (state) {
            is RecipeSearchState.Idle -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("레시피 이름을 검색해보세요", color = FoodColors.mutedForeground, fontFamily = AppFonts.inter)
                }
            }
            is RecipeSearchState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = FoodColors.primary)
                }
            }
            is RecipeSearchState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = FoodColors.mutedForeground, fontFamily = AppFonts.inter)
                }
            }
            is RecipeSearchState.Success -> {
                if (state.items.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("검색 결과가 없어요", color = FoodColors.mutedForeground, fontFamily = AppFonts.inter)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            Text(
                                "총 ${state.totalElements}개",
                                color = FoodColors.mutedForeground,
                                fontFamily = AppFonts.mono,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }
                        items(state.items, key = { it.id }) { recipe ->
                            RecipeSummaryRow(recipe, onClick = { onRecipeClick(recipe.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecipeSummaryRow(recipe: RecipeSummaryDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(FoodColors.card, RoundedCornerShape(14.dp))
            .border(1.dp, FoodColors.border, RoundedCornerShape(14.dp))
            .padding(16.dp),
    ) {
        Text(recipe.title, color = FoodColors.foreground, fontFamily = AppFonts.fraunces, fontSize = 15.sp, fontWeight = FontWeight(700))
        Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            recipe.method?.let { Text(it, color = FoodColors.mutedForeground, fontFamily = AppFonts.mono, fontSize = 11.sp) }
            recipe.cooktime?.let { Text(it, color = FoodColors.mutedForeground, fontFamily = AppFonts.mono, fontSize = 11.sp) }
            recipe.portion?.let { Text(it, color = FoodColors.mutedForeground, fontFamily = AppFonts.mono, fontSize = 11.sp) }
        }
    }
}
