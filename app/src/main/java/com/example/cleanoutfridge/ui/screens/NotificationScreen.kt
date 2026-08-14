package com.example.cleanoutfridge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
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
import com.example.cleanoutfridge.data.NotificationUiState
import com.example.cleanoutfridge.data.NotificationViewModel
import com.example.cleanoutfridge.data.remote.dto.NotificationDto
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val PageBackground = Color(0xFFF7F3EC)
private val Ink = Color(0xFF1A1410)
private val Muted = Color(0xFF7A7060)
private val Green = Color(0xFF18A05C)

@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NotificationViewModel = viewModel(),
) {
    val state = viewModel.uiState

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

        when (state) {
            is NotificationUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Green)
                }
            }
            is NotificationUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.message, color = Muted)
                    }
                }
            }
            is NotificationUiState.Success -> {
                if (state.notifications.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("아직 받은 알림이 없어요", color = Muted)
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.notifications, key = { it.id }) { notification ->
                            NotificationCard(notification, onDelete = { viewModel.delete(notification.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationCard(notification: NotificationDto, onDelete: () -> Unit) {
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
                Text(notification.title ?: "알림", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.Black)
                notification.content?.let {
                    Text(it, color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                }
                Text(
                    notifyTimeLabel(notification.notifyTime),
                    color = Green,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "삭제", tint = Muted)
            }
        }
    }
}

private fun notifyTimeLabel(notifyTimeIso: String): String {
    val notifyTime = runCatching { LocalDateTime.parse(notifyTimeIso.take(19)) }.getOrNull()
        ?: return notifyTimeIso
    val now = LocalDateTime.now()
    val days = Duration.between(now.toLocalDate().atStartOfDay(), notifyTime.toLocalDate().atStartOfDay()).toDays()
    val dateLabel = notifyTime.format(DateTimeFormatter.ofPattern("M월 d일 HH:mm"))
    val relative = when {
        days < 0 -> "${-days}일 지남"
        days == 0L -> "오늘"
        days == 1L -> "내일"
        else -> "${days}일 후"
    }
    return "$dateLabel · $relative"
}
