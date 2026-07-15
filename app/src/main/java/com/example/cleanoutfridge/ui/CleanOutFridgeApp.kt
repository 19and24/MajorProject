package com.example.cleanoutfridge.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cleanoutfridge.data.FoodViewModel
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
                if (showNotifications) {
                    NotificationScreen(items = viewModel.items, onBack = { showNotifications = false })
                } else {
                    when (tab) {
                        Tab.HOME -> HomeScreen(items = viewModel.items, onNotificationsClick = { showNotifications = true })
                        Tab.FRIDGE -> FridgeScreen(items = viewModel.items, onDelete = viewModel::deleteItem)
                        Tab.ADD -> AddScreen(
                            onAdd = viewModel::addItem,
                            onDone = { addFullScreen = false; tab = Tab.HOME },
                            onFullScreenChange = { addFullScreen = it },
                        )
                        Tab.STATS -> StatsScreen(items = viewModel.items)
                        Tab.CLEANOUT -> CleanOutScreen(items = viewModel.items)
                    }
                }
            }
            if (!showNotifications && (tab != Tab.ADD || !addFullScreen)) {
                BottomNav(active = tab, onNav = { tab = it })
            }
        }
    }
}
