package com.example.majorproject.food.ui

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
import com.example.majorproject.food.data.FoodViewModel
import com.example.majorproject.food.model.Tab
import com.example.majorproject.food.ui.components.BottomNav
import com.example.majorproject.food.ui.screens.AddScreen
import com.example.majorproject.food.ui.screens.FoodsScreen
import com.example.majorproject.food.ui.screens.HomeScreen
import com.example.majorproject.food.ui.screens.SettingsScreen
import com.example.majorproject.food.ui.screens.StatsScreen
import com.example.majorproject.food.ui.theme.FoodColors

@Composable
fun FoodTrackerApp(viewModel: FoodViewModel = viewModel()) {
    var tab by remember { mutableStateOf(Tab.HOME) }
    var addFullScreen by remember { mutableStateOf(false) }

    Surface(color = FoodColors.background, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                when (tab) {
                    Tab.HOME -> HomeScreen(items = viewModel.items, onNavigate = { tab = it })
                    Tab.FOODS -> FoodsScreen(items = viewModel.items, onDelete = viewModel::deleteItem)
                    Tab.ADD -> AddScreen(
                        onAdd = viewModel::addItem,
                        onDone = { addFullScreen = false; tab = Tab.HOME },
                        onFullScreenChange = { addFullScreen = it },
                    )
                    Tab.STATS -> StatsScreen(items = viewModel.items)
                    Tab.SETTINGS -> SettingsScreen()
                }
            }
            if (tab != Tab.ADD || !addFullScreen) {
                BottomNav(active = tab, onNav = { tab = it })
            }
        }
    }
}
