package com.example.majorproject.food.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.majorproject.food.model.Tab
import com.example.majorproject.food.ui.theme.AppFonts
import com.example.majorproject.food.ui.theme.FoodColors

@Composable
fun BottomNav(active: Tab, onNav: (Tab) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xF0F7F3EC))
            .navigationBarsPadding()
            .padding(horizontal = 12.dp)
            .padding(top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        NavItem(Tab.HOME, "홈", Icons.Filled.Home, active, onNav, Modifier.weight(1f))
        NavItem(Tab.FOODS, "냉장고", Icons.Filled.Kitchen, active, onNav, Modifier.weight(1f))

        Box(modifier = Modifier.width(64.dp), contentAlignment = Alignment.Center) {
            FloatingActionButton(
                onClick = { onNav(Tab.ADD) },
                containerColor = if (active == Tab.ADD) Color(0xFF16A34A) else FoodColors.primary,
                shape = CircleShape,
                modifier = Modifier.size(56.dp),
            ) {
                Icon(
                    imageVector = if (active == Tab.ADD) Icons.Filled.Check else Icons.Filled.Add,
                    contentDescription = "추가",
                    tint = Color.White,
                )
            }
        }

        NavItem(Tab.STATS, "통계", Icons.Filled.BarChart, active, onNav, Modifier.weight(1f))
        NavItem(Tab.SETTINGS, "설정", Icons.Filled.Settings, active, onNav, Modifier.weight(1f))
    }
}

@Composable
private fun NavItem(
    tab: Tab,
    label: String,
    icon: ImageVector,
    active: Tab,
    onNav: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (active == tab) FoodColors.primary else FoodColors.mutedForeground
    Column(
        modifier = modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onNav(tab) }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(21.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = AppFonts.nunito, color = color)
    }
}
