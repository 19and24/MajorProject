package com.example.majorproject.food.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.majorproject.food.model.FoodItem
import com.example.majorproject.food.model.fmtDate
import com.example.majorproject.food.model.getDays
import com.example.majorproject.food.model.getStatus
import com.example.majorproject.food.ui.theme.AppFonts
import com.example.majorproject.food.ui.theme.FoodColors

@Composable
fun FoodCard(item: FoodItem, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    val days = getDays(item.expiryDate)
    val status = getStatus(days)
    val shape = RoundedCornerShape(20.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(FoodColors.card)
            .border(1.dp, FoodColors.border, shape),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(status.bg),
                contentAlignment = Alignment.Center,
            ) {
                Text(item.emoji, fontSize = 20.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = item.name,
                        fontFamily = AppFonts.nunito,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = FoodColors.foreground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(FoodColors.muted)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(item.category, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = FoodColors.mutedForeground)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(fmtDate(item.expiryDate), fontFamily = AppFonts.dmMono, fontSize = 11.sp, color = FoodColors.mutedForeground)
                    Text(status.label, fontFamily = AppFonts.nunito, fontWeight = FontWeight.Black, fontSize = 11.sp, color = status.color)
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(status.dot))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "삭제", tint = FoodColors.mutedForeground, modifier = Modifier.size(14.dp))
                }
            }
        }

        if (days < 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFEF2F2))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = Color(0xFFB91C1C), modifier = Modifier.size(11.dp))
                Text("유통기한이 지났어요. 상태를 확인하세요.", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFB91C1C))
            }
        } else if (days <= 2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFFFF7ED))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = Color(0xFFC2410C), modifier = Modifier.size(11.dp))
                Text(if (days == 0) "오늘 드세요!" else "${days}일 후 만료돼요", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFC2410C))
            }
        }
    }
}
