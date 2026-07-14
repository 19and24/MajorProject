package com.example.majorproject.food.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.majorproject.food.model.CATEGORIES
import com.example.majorproject.food.ui.theme.AppFonts
import com.example.majorproject.food.ui.theme.FoodColors

private data class SettingsItemSpec(
    val icon: String,
    val label: String,
    val desc: String?,
    val trailing: @Composable () -> Unit,
)

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    var notifEnabled by remember { mutableStateOf(true) }
    var notifDays by remember { mutableStateOf(3) }
    var darkMode by remember { mutableStateOf(false) }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(top = 48.dp, bottom = 20.dp)) {
                Text("설정", fontFamily = AppFonts.nunito, fontWeight = FontWeight.Black, fontSize = 24.sp, color = FoodColors.foreground)
                Text("앱 환경을 설정해요", fontFamily = AppFonts.dmMono, fontSize = 12.sp, color = FoodColors.mutedForeground)
            }
        }

        item {
            SettingsSection(
                title = "알림 설정",
                items = listOf(
                    SettingsItemSpec("🔔", "만료 알림", "유통기한 임박 시 알림") {
                        Toggle(checked = notifEnabled, onToggle = { notifEnabled = !notifEnabled })
                    },
                    SettingsItemSpec("📅", "알림 기준일", "만료 ${notifDays}일 전에 알려드려요") {
                        Stepper(value = notifDays, onDecrement = { notifDays = (notifDays - 1).coerceAtLeast(1) }, onIncrement = { notifDays = (notifDays + 1).coerceAtMost(14) })
                    },
                ),
            )
        }

        item {
            SettingsSection(
                title = "화면",
                items = listOf(
                    SettingsItemSpec("🌙", "다크 모드", "어두운 테마 사용") {
                        Toggle(checked = darkMode, onToggle = { darkMode = !darkMode })
                    },
                ),
            )
        }

        item {
            SettingsSection(
                title = "카테고리별 기본 유통기한",
                items = CATEGORIES.map { cat ->
                    SettingsItemSpec(cat.emoji, cat.label, null) {
                        val label = if (cat.defaultDays >= 60) "${Math.round(cat.defaultDays / 30.0)}개월" else "${cat.defaultDays}일"
                        Text(label, fontFamily = AppFonts.dmMono, fontWeight = FontWeight.Black, fontSize = 12.sp, color = FoodColors.primary)
                    }
                },
            )
        }

        item {
            SettingsSection(
                title = "앱 정보",
                items = listOf(
                    SettingsItemSpec("📱", "버전", null) {
                        Text("1.0.0", fontFamily = AppFonts.dmMono, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = FoodColors.mutedForeground)
                    },
                    SettingsItemSpec("📝", "오픈소스 라이선스", null) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = FoodColors.mutedForeground)
                    },
                    SettingsItemSpec("💌", "피드백 보내기", null) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = FoodColors.mutedForeground)
                    },
                ),
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(FoodColors.primary, FoodColors.primaryDark)))
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Text("🥗", fontSize = 30.sp)
                }
                Column {
                    Text("냉장고 지킴이", fontFamily = AppFonts.nunito, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                    Text("식자재 낭비 제로 도전 중 🎯", fontFamily = AppFonts.dmSans, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SettingsSection(title: String, items: List<SettingsItemSpec>) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            title.uppercase(),
            fontFamily = AppFonts.dmMono, fontWeight = FontWeight.Bold, fontSize = 11.sp,
            color = FoodColors.mutedForeground,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(FoodColors.card)
                .border(1.dp, FoodColors.border, RoundedCornerShape(20.dp)),
        ) {
            items.forEachIndexed { i, spec ->
                if (i > 0) HorizontalDivider(color = FoodColors.border)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(spec.icon, fontSize = 18.sp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(spec.label, fontFamily = AppFonts.nunito, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FoodColors.foreground)
                        if (spec.desc != null) {
                            Text(spec.desc, fontFamily = AppFonts.dmSans, fontSize = 11.sp, color = FoodColors.mutedForeground)
                        }
                    }
                    spec.trailing()
                }
            }
        }
    }
}

@Composable
private fun Toggle(checked: Boolean, onToggle: () -> Unit) {
    val thumbOffset by animateDpAsState(if (checked) 24.dp else 4.dp, label = "toggle-thumb")
    Box(
        modifier = Modifier
            .width(44.dp)
            .height(24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (checked) FoodColors.primary else FoodColors.muted)
            .clickable { onToggle() },
    ) {
        Box(
            modifier = Modifier
                .padding(start = thumbOffset, top = 4.dp)
                .size(16.dp)
                .clip(CircleShape)
                .background(Color.White),
        )
    }
}

@Composable
private fun Stepper(value: Int, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(FoodColors.muted).clickable { onDecrement() },
            contentAlignment = Alignment.Center,
        ) {
            Text("−", fontWeight = FontWeight.Black, color = FoodColors.foreground)
        }
        Text("$value", fontFamily = AppFonts.dmMono, fontWeight = FontWeight.Black, fontSize = 14.sp, color = FoodColors.foreground, modifier = Modifier.width(24.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape).background(FoodColors.muted).clickable { onIncrement() },
            contentAlignment = Alignment.Center,
        ) {
            Text("+", fontWeight = FontWeight.Black, color = FoodColors.foreground)
        }
    }
}
