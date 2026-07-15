package com.example.cleanoutfridge.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cleanoutfridge.model.FoodItem
import com.example.cleanoutfridge.model.getDays

private val PageBackground = Color(0xFFF7F3EC)
private val Ink = Color(0xFF1A1410)
private val Muted = Color(0xFF7A7060)
private val Green = Color(0xFF18A05C)

private data class NotificationEntry(
    val id: String,
    val title: String,
    val message: String,
    val daysLeft: Int,
)

@Composable
fun NotificationScreen(items: List<FoodItem>, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val notifications = remember(items) {
        items
            .map { it to getDays(it.expiryDate) }
            .filter { it.second <= 3 }
            .sortedBy { it.second }
            .map { (food, days) ->
                NotificationEntry(
                    id = food.id,
                    title = when {
                        days < 0 -> "유통기한 초과"
                        days == 0 -> "오늘 유통기한 만료"
                        else -> "유통기한 임박"
                    },
                    message = "${food.name}의 유통기한을 확인해주세요.",
                    daysLeft = days,
                )
            }
    }

    Column(modifier = modifier.fillMaxSize().background(PageBackground)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = Ink)
            }
            Text("알림", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(start = 4.dp))
        }

        if (notifications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("아직 받은 알림이 없어요", color = Muted)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(notifications, key = { it.id }) { notification ->
                    NotificationCard(notification)
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: NotificationEntry) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(40.dp).background(Green.copy(alpha = .1f), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("🔔", fontSize = 18.sp) }
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Text(notification.title, color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
                Text(notification.message, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                Text(daysText(notification.daysLeft), color = Green, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }
}

private fun daysText(days: Int): String = when {
    days < 0 -> "${-days}일 지남"
    days == 0 -> "오늘까지"
    days == 1 -> "내일까지"
    else -> "${days}일 남음"
}
