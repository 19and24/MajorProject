package com.example.cleanoutfridge.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cleanoutfridge.data.FoodUiState
import com.example.cleanoutfridge.data.FoodViewModel
import com.example.cleanoutfridge.model.FoodItem
import com.example.cleanoutfridge.model.Tab
import com.example.cleanoutfridge.ui.components.BottomNav
import com.example.cleanoutfridge.ui.screens.AddScreen
import com.example.cleanoutfridge.ui.screens.CleanOutScreen
import com.example.cleanoutfridge.ui.screens.FridgeScreen
import com.example.cleanoutfridge.ui.screens.HomeScreen
import com.example.cleanoutfridge.ui.screens.NotificationScreen
import com.example.cleanoutfridge.ui.screens.StatsScreen
import com.example.cleanoutfridge.ui.theme.FoodColors

@Composable
fun CleanOutFridgeApp(viewModel: FoodViewModel = viewModel()) {
    var tab by remember { mutableStateOf(Tab.HOME) }
    var addFullScreen by remember { mutableStateOf(false) }
    var showNotifications by remember { mutableStateOf(false) }

    Surface(color = FoodColors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (val state = viewModel.uiState) {
                    is FoodUiState.Loading -> LoadingState()
                    is FoodUiState.Error -> ErrorState(message = state.message, onRetry = viewModel::refresh)
                    is FoodUiState.Success -> FoodContent(
                        items = state.items,
                        tab = tab,
                        showNotifications = showNotifications,
                        onNotificationsClick = { showNotifications = true },
                        onBackFromNotifications = { showNotifications = false },
                        onAdd = viewModel::addItem,
                        onDelete = viewModel::deleteItem,
                        onAddDone = { addFullScreen = false; tab = Tab.HOME },
                        onAddFullScreenChange = { addFullScreen = it },
                    )
                }
            }
            if (!showNotifications && (tab != Tab.ADD || !addFullScreen)) {
                BottomNav(active = tab, onNav = { tab = it })
            }
        }
    }
}

@Composable
private fun FoodContent(
    items: List<FoodItem>,
    tab: Tab,
    showNotifications: Boolean,
    onNotificationsClick: () -> Unit,
    onBackFromNotifications: () -> Unit,
    onAdd: (FoodItem) -> Unit,
    onDelete: (String) -> Unit,
    onAddDone: () -> Unit,
    onAddFullScreenChange: (Boolean) -> Unit,
) {
    if (showNotifications) {
        NotificationScreen(items = items, onBack = onBackFromNotifications)
    } else {
        when (tab) {
            Tab.HOME -> HomeScreen(items = items, onNotificationsClick = onNotificationsClick)
            Tab.FRIDGE -> FridgeScreen(items = items, onDelete = onDelete)
            Tab.ADD -> AddScreen(
                onAdd = onAdd,
                onDone = onAddDone,
                onFullScreenChange = onAddFullScreenChange,
            )
            Tab.STATS -> StatsScreen(items = items)
            Tab.CLEANOUT -> CleanOutScreen(items = items)
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = FoodColors.primary)
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("문제가 발생했어요", color = FoodColors.foreground)
            Text(message, color = FoodColors.mutedForeground)
            Button(onClick = onRetry) { Text("다시 시도") }
        }
    }
}
