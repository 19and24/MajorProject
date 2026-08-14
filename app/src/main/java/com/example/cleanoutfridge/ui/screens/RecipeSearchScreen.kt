package com.example.cleanoutfridge.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cleanoutfridge.data.RecipeSearchState
import com.example.cleanoutfridge.data.RecipeSearchViewModel
import com.example.cleanoutfridge.data.remote.dto.RecipeSummaryDto

private val PageBackground = Color(0xFFF7F3EC)
private val Ink = Color(0xFF1A1410)
private val Muted = Color(0xFF7A7060)
private val Green = Color(0xFF18A05C)

@Composable
fun RecipeSearchScreen(
    onBack: () -> Unit,
    onRecipeClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecipeSearchViewModel = viewModel(),
) {
    val state = viewModel.state

    Column(modifier = modifier.fillMaxSize().background(PageBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = Ink)
            }
            Text("레시피 검색", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 4.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = viewModel.keyword,
                onValueChange = viewModel::onKeywordChange,
                placeholder = { Text("레시피 이름으로 검색") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { viewModel.search() }) {
                Text("검색", color = Green, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }

        when (state) {
            is RecipeSearchState.Idle -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("레시피 이름을 검색해보세요", color = Muted)
                }
            }
            is RecipeSearchState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Green)
                }
            }
            is RecipeSearchState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = Muted)
                }
            }
            is RecipeSearchState.Success -> {
                if (state.items.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("검색 결과가 없어요", color = Muted)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            Text("총 ${state.totalElements}개", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 4.dp))
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
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(recipe.title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Black)
            Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                recipe.method?.let { Text(it, color = Muted, fontSize = 12.sp) }
                recipe.cooktime?.let { Text(it, color = Muted, fontSize = 12.sp) }
                recipe.portion?.let { Text(it, color = Muted, fontSize = 12.sp) }
            }
        }
    }
}
