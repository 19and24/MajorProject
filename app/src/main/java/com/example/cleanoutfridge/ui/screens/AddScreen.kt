package com.example.cleanoutfridge.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cleanoutfridge.model.AI_SCAN_SCENARIOS
import com.example.cleanoutfridge.model.CATEGORIES
import com.example.cleanoutfridge.model.FoodItem
import com.example.cleanoutfridge.model.addDays
import com.example.cleanoutfridge.model.categoryById
import com.example.cleanoutfridge.model.fmtDate
import com.example.cleanoutfridge.model.today
import com.example.cleanoutfridge.ui.theme.AppFonts
import com.example.cleanoutfridge.ui.theme.FoodColors
import kotlinx.coroutines.delay
import java.util.UUID

private enum class ScanPhase { IDLE, VIEWFINDER, ANALYZING, RESULTS }

private data class DetectedItem(
    val id: String,
    val name: String,
    val category: String,
    val emoji: String,
    val confidence: Int,
    val expiryDate: String,
    val selected: Boolean,
    val editOpen: Boolean,
)

private val analysisMessages = listOf("사진 분석 중...", "식재료 감지 중...", "유통기한 계산 중...", "완료 ✓")

@Composable
fun AddScreen(
    onAdd: (FoodItem) -> Unit,
    onDone: () -> Unit,
    onFullScreenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var phase by remember { mutableStateOf(ScanPhase.IDLE) }
    var analysisStep by remember { mutableStateOf(0) }
    val detected = remember { mutableStateListOf<DetectedItem>() }
    var bulkSuccess by remember { mutableStateOf(false) }
    var sceneLabel by remember { mutableStateOf("") }
    var manualSuccess by remember { mutableStateOf(false) }

    var formName by remember { mutableStateOf("") }
    var formCategory by remember { mutableStateOf("유제품") }
    var formEmoji by remember { mutableStateOf("🥛") }
    var formExpiry by remember { mutableStateOf(addDays(7)) }

    LaunchedEffect(phase) { onFullScreenChange(phase != ScanPhase.IDLE) }

    LaunchedEffect(phase) {
        if (phase == ScanPhase.ANALYZING) {
            analysisStep = 0
            repeat(analysisMessages.size - 1) {
                delay(650)
                analysisStep++
            }
            delay(400)
            val scenario = AI_SCAN_SCENARIOS.random()
            sceneLabel = scenario.scene
            detected.clear()
            detected.addAll(
                scenario.items.mapIndexed { i, scenarioItem ->
                    val cat = categoryById(scenarioItem.category)
                    DetectedItem(
                        id = "det-$i",
                        name = scenarioItem.name,
                        category = scenarioItem.category,
                        emoji = scenarioItem.emoji,
                        confidence = scenarioItem.confidence,
                        expiryDate = addDays(cat?.defaultDays ?: 7),
                        selected = true,
                        editOpen = false,
                    )
                },
            )
            phase = ScanPhase.RESULTS
        }
    }

    LaunchedEffect(bulkSuccess) {
        if (bulkSuccess) {
            delay(1200)
            bulkSuccess = false
            phase = ScanPhase.IDLE
            detected.clear()
            onDone()
        }
    }

    LaunchedEffect(manualSuccess) {
        if (manualSuccess) {
            delay(1000)
            manualSuccess = false
            formName = ""
            formCategory = "유제품"
            formEmoji = "🥛"
            formExpiry = addDays(7)
            onDone()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when (phase) {
            ScanPhase.VIEWFINDER -> ViewfinderPhase(
                onClose = { phase = ScanPhase.IDLE },
                onShutter = { phase = ScanPhase.ANALYZING },
            )
            ScanPhase.ANALYZING -> AnalyzingPhase(step = analysisStep, messages = analysisMessages)
            ScanPhase.RESULTS -> ResultsPhase(
                detected = detected,
                sceneLabel = sceneLabel,
                bulkSuccess = bulkSuccess,
                onClose = { phase = ScanPhase.IDLE; detected.clear() },
                onToggleSelectAll = {
                    val all = detected.all { it.selected }
                    for (i in detected.indices) detected[i] = detected[i].copy(selected = !all)
                },
                onToggleSelect = { id ->
                    val idx = detected.indexOfFirst { it.id == id }
                    if (idx >= 0) detected[idx] = detected[idx].copy(selected = !detected[idx].selected)
                },
                onToggleEdit = { id ->
                    for (i in detected.indices) {
                        detected[i] = detected[i].copy(editOpen = detected[i].id == id && !detected[i].editOpen)
                    }
                },
                onUpdateDate = { id, date ->
                    val idx = detected.indexOfFirst { it.id == id }
                    if (idx >= 0) detected[idx] = detected[idx].copy(expiryDate = date)
                },
                onUpdateCategory = { id, catId ->
                    val idx = detected.indexOfFirst { it.id == id }
                    if (idx >= 0) {
                        val cat = categoryById(catId)
                        detected[idx] = detected[idx].copy(
                            category = catId,
                            emoji = cat?.emoji ?: "🍱",
                            expiryDate = addDays(cat?.defaultDays ?: 7),
                        )
                    }
                },
                onUpdateName = { id, name ->
                    val idx = detected.indexOfFirst { it.id == id }
                    if (idx >= 0) detected[idx] = detected[idx].copy(name = name)
                },
                onDeleteDetected = { id -> detected.removeAll { it.id == id } },
                onAddManual = {
                    val cat = categoryById("기타")
                    detected.add(
                        DetectedItem(
                            id = "manual-${UUID.randomUUID()}",
                            name = "",
                            category = "기타",
                            emoji = cat?.emoji ?: "🍱",
                            confidence = -1,
                            expiryDate = addDays(cat?.defaultDays ?: 7),
                            selected = true,
                            editOpen = true,
                        ),
                    )
                },
                onRescan = { phase = ScanPhase.VIEWFINDER; detected.clear() },
                onConfirmAll = {
                    detected.filter { it.selected && it.name.isNotBlank() }.forEach {
                        onAdd(
                            FoodItem(
                                id = UUID.randomUUID().toString(),
                                ingredientId = 0,
                                name = it.name,
                                category = it.category,
                                expiryDate = it.expiryDate,
                                emoji = it.emoji,
                                addedDate = today(),
                            ),
                        )
                    }
                    bulkSuccess = true
                },
            )
            ScanPhase.IDLE -> IdlePhase(
                formName = formName,
                onNameChange = { formName = it },
                formCategory = formCategory,
                formEmoji = formEmoji,
                formExpiry = formExpiry,
                onCategorySelect = { id ->
                    val cat = categoryById(id)
                    formCategory = id
                    formEmoji = cat?.emoji ?: "🍱"
                    formExpiry = addDays(cat?.defaultDays ?: 7)
                },
                onExpiryChange = { formExpiry = it },
                manualSuccess = manualSuccess,
                onSubmit = {
                    if (formName.isNotBlank()) {
                        onAdd(
                            FoodItem(
                                id = UUID.randomUUID().toString(),
                                ingredientId = 0,
                                name = formName,
                                category = formCategory,
                                expiryDate = formExpiry,
                                emoji = formEmoji,
                                addedDate = today(),
                            ),
                        )
                        manualSuccess = true
                    }
                },
                onCameraClick = { phase = ScanPhase.VIEWFINDER },
            )
        }
    }
}

@Composable
private fun ViewfinderPhase(onClose: () -> Unit, onShutter: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF15200F), Color(0xFF0D140A), Color(0xFF120E08)))),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width = 260.dp, height = 180.dp)
                    .border(2.dp, Color(0xFFA8D48A), RoundedCornerShape(4.dp)),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 20.dp, vertical = 40.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.12f))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "닫기", tint = Color.White)
                }
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(FoodColors.urgent))
                    Text("AI 인식", fontFamily = AppFonts.mono, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                }
                Spacer(Modifier.size(44.dp))
            }
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 180.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "식재료가 모두 보이도록 찍어주세요",
                    fontFamily = AppFonts.fraunces, fontWeight = FontWeight(700), fontSize = 17.sp,
                    color = Color.White, textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "냉장고 안 · 장바구니 · 식탁 위 모두 OK",
                    fontFamily = AppFonts.inter, fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.55f), textAlign = TextAlign.Center,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(Color.Black.copy(alpha = 0.92f))
                .padding(horizontal = 40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(2.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("🖼️", fontSize = 22.sp)
            }
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .border(4.dp, Color.White, CircleShape)
                    .clickable { onShutter() }
                    .padding(6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.fillMaxSize().clip(CircleShape).background(Color.White))
            }
            Box(
                modifier = Modifier.size(54.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = "전환", tint = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun AnalyzingPhase(step: Int, messages: List<String>) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0E1309)), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(FoodColors.primary.copy(alpha = 0.15f))
                    .border(2.dp, FoodColors.primary.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.size(80.dp).clip(CircleShape).background(FoodColors.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("AI 분석 중", fontFamily = AppFonts.fraunces, fontWeight = FontWeight(900), fontSize = 24.sp, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text(
                    messages[step.coerceIn(0, messages.lastIndex)],
                    fontFamily = AppFonts.mono, fontSize = 14.sp, color = Color(0xFFA8D48A),
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.1f)),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(((step + 1f) / messages.size).coerceIn(0f, 1f))
                            .height(6.dp)
                            .background(FoodColors.primary),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    messages.forEachIndexed { i, msg ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            i < step -> FoodColors.primary
                                            i == step -> FoodColors.primary.copy(alpha = 0.3f)
                                            else -> Color.White.copy(alpha = 0.07f)
                                        },
                                    ),
                                contentAlignment = Alignment.Center,
                            ) {
                                when {
                                    i < step -> Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                    i == step -> Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFA8D48A)))
                                }
                            }
                            Text(
                                msg, fontFamily = AppFonts.inter, fontSize = 14.sp,
                                color = if (i <= step) Color.White.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.25f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsPhase(
    detected: List<DetectedItem>,
    sceneLabel: String,
    bulkSuccess: Boolean,
    onClose: () -> Unit,
    onToggleSelectAll: () -> Unit,
    onToggleSelect: (String) -> Unit,
    onToggleEdit: (String) -> Unit,
    onUpdateDate: (String, String) -> Unit,
    onUpdateCategory: (String, String) -> Unit,
    onUpdateName: (String, String) -> Unit,
    onDeleteDetected: (String) -> Unit,
    onAddManual: () -> Unit,
    onRescan: () -> Unit,
    onConfirmAll: () -> Unit,
) {
    val selectedCount = detected.count { it.selected && it.name.isNotBlank() }
    val allSelected = detected.isNotEmpty() && detected.all { it.selected }

    Column(modifier = Modifier.fillMaxSize().background(FoodColors.background)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FoodColors.card)
                .padding(horizontal = 20.dp)
                .padding(top = 44.dp, bottom = 12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("${detected.size}개 식재료 인식됨", fontFamily = AppFonts.nunito, fontWeight = FontWeight.Black, fontSize = 20.sp, color = FoodColors.foreground)
                    Text(sceneLabel, fontFamily = AppFonts.dmMono, fontSize = 12.sp, color = FoodColors.mutedForeground)
                }
                IconButton(onClick = onClose, modifier = Modifier.size(36.dp).clip(CircleShape).background(FoodColors.muted)) {
                    Icon(Icons.Filled.Close, contentDescription = "닫기", tint = FoodColors.mutedForeground, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.clickable { onToggleSelectAll() }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    if (allSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = null, tint = FoodColors.primary, modifier = Modifier.size(16.dp),
                )
                Text(
                    if (allSelected) "전체 해제" else "전체 선택",
                    fontFamily = AppFonts.nunito, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FoodColors.primary,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            items(detected, key = { it.id }) { item ->
                DetectedItemRow(item, onToggleSelect, onToggleEdit, onUpdateDate, onUpdateCategory, onUpdateName, onDeleteDetected)
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(FoodColors.primarySoft)
                        .border(2.dp, FoodColors.primary.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                        .clickable { onAddManual() }
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = FoodColors.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("재료 직접 추가하기", fontFamily = AppFonts.nunito, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FoodColors.primary)
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(FoodColors.muted)
                        .border(2.dp, FoodColors.border, RoundedCornerShape(20.dp))
                        .clickable { onRescan() }
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = FoodColors.mutedForeground, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("다시 촬영하기", fontFamily = AppFonts.nunito, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = FoodColors.mutedForeground)
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(FoodColors.card)
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 24.dp),
        ) {
            if (selectedCount > 0) {
                Text(
                    "${selectedCount}개 선택됨",
                    fontFamily = AppFonts.dmSans, fontSize = 12.sp, color = FoodColors.mutedForeground,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 8.dp),
                )
            }
            Button(
                onClick = onConfirmAll,
                enabled = selectedCount > 0 && !bulkSuccess,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (bulkSuccess) FoodColors.primaryDark else FoodColors.primary,
                    disabledContainerColor = FoodColors.primary.copy(alpha = 0.4f),
                ),
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                if (bulkSuccess) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("냉장고에 추가됐어요!", fontFamily = AppFonts.nunito, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                } else {
                    Icon(Icons.Filled.ShoppingBag, contentDescription = null, tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("${selectedCount}개 냉장고에 추가하기", fontFamily = AppFonts.nunito, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DetectedItemRow(
    item: DetectedItem,
    onToggleSelect: (String) -> Unit,
    onToggleEdit: (String) -> Unit,
    onUpdateDate: (String, String) -> Unit,
    onUpdateCategory: (String, String) -> Unit,
    onUpdateName: (String, String) -> Unit,
    onDelete: (String) -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(FoodColors.card)
            .border(2.dp, if (item.selected) FoodColors.primary else FoodColors.border, shape)
            .alpha(if (item.selected) 1f else 0.55f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleSelect(item.id) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (item.selected) FoodColors.primary else Color.Transparent)
                    .border(2.dp, if (item.selected) FoodColors.primary else FoodColors.mutedForeground, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (item.selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
            Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(FoodColors.muted), contentAlignment = Alignment.Center) {
                Text(item.emoji, fontSize = 22.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        item.name.ifBlank { "이름을 입력하세요" }, fontFamily = AppFonts.nunito, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                        color = if (item.name.isBlank()) FoodColors.mutedForeground else FoodColors.foreground,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (item.confidence >= 0) {
                        val confColor = when {
                            item.confidence >= 92 -> FoodColors.primaryDark
                            item.confidence >= 85 -> FoodColors.warn
                            else -> FoodColors.urgent
                        }
                        val confBg = when {
                            item.confidence >= 92 -> FoodColors.primarySoft
                            item.confidence >= 85 -> FoodColors.warnSoft
                            else -> FoodColors.urgentSoft
                        }
                        Box(modifier = Modifier.clip(CircleShape).background(confBg).padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text("인식률 ${item.confidence}%", fontFamily = AppFonts.dmMono, fontSize = 9.sp, fontWeight = FontWeight.Black, color = confColor)
                        }
                    }
                }
                Text(
                    buildAnnotatedString {
                        append("${item.category} · ")
                        withStyle(SpanStyle(color = FoodColors.primary, fontWeight = FontWeight.SemiBold)) { append(fmtDate(item.expiryDate)) }
                        append(" 까지")
                    },
                    fontFamily = AppFonts.dmSans, fontSize = 12.sp, color = FoodColors.mutedForeground,
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(FoodColors.muted)
                    .clickable { onToggleEdit(item.id) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (item.editOpen) Icons.Filled.ExpandLess else Icons.Filled.Edit,
                    contentDescription = "편집", tint = FoodColors.mutedForeground, modifier = Modifier.size(15.dp),
                )
            }
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(FoodColors.expiredSoft)
                    .clickable { onDelete(item.id) },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.DeleteOutline, contentDescription = "삭제", tint = FoodColors.expired, modifier = Modifier.size(16.dp))
            }
        }

        if (item.editOpen) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("이름", fontFamily = AppFonts.nunito, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = FoodColors.mutedForeground)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { onUpdateName(item.id, it) },
                    placeholder = { Text("예: 계란, 우유, 닭가슴살...") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FoodColors.primary,
                        unfocusedBorderColor = FoodColors.border,
                        focusedContainerColor = FoodColors.muted,
                        unfocusedContainerColor = FoodColors.muted,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                Text("카테고리", fontFamily = AppFonts.nunito, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = FoodColors.mutedForeground)
                Spacer(Modifier.height(8.dp))
                CategoryGrid(selectedId = item.category, onSelect = { onUpdateCategory(item.id, it) })
                Spacer(Modifier.height(16.dp))
                Text("유통기한", fontFamily = AppFonts.nunito, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = FoodColors.mutedForeground)
                Spacer(Modifier.height(8.dp))
                DateField(value = item.expiryDate, onValueChange = { onUpdateDate(item.id, it) })
            }
        }
    }
}

@Composable
private fun IdlePhase(
    formName: String,
    onNameChange: (String) -> Unit,
    formCategory: String,
    formEmoji: String,
    formExpiry: String,
    onCategorySelect: (String) -> Unit,
    onExpiryChange: (String) -> Unit,
    manualSuccess: Boolean,
    onSubmit: () -> Unit,
    onCameraClick: () -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 48.dp, bottom = 16.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF16210F), Color(0xFF1B2913), Color(0xFF1A1608))))
                    .clickable { onCameraClick() },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(18.dp)).background(FoodColors.primary), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("일괄 등록", fontFamily = AppFonts.nunito, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
                        Text("AI가 자동으로 유통기한을 추천해요", fontFamily = AppFonts.dmSans, fontSize = 12.sp, color = Color.White.copy(alpha = 0.55f))
                    }
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf("🤖" to "AI 식재료 인식", "📅" to "유통기한 자동 계산", "✅" to "일괄 등록").forEach { (icon, label) ->
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(FoodColors.primary.copy(alpha = 0.2f))
                                .border(1.dp, FoodColors.primary.copy(alpha = 0.35f), CircleShape)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(icon, fontSize = 11.sp)
                            Text(label, fontFamily = AppFonts.dmMono, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFA8D48A))
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = FoodColors.border)
                Text("직접 입력", fontFamily = AppFonts.dmMono, fontSize = 12.sp, color = FoodColors.mutedForeground)
                HorizontalDivider(modifier = Modifier.weight(1f), color = FoodColors.border)
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Column {
                    Text("식품명", fontFamily = AppFonts.nunito, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FoodColors.foreground)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = formName,
                        onValueChange = onNameChange,
                        placeholder = { Text("예: 계란, 우유, 닭가슴살...") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = FoodColors.primary,
                            unfocusedBorderColor = FoodColors.border,
                            focusedContainerColor = FoodColors.card,
                            unfocusedContainerColor = FoodColors.card,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Column {
                    Row {
                        Text("카테고리", fontFamily = AppFonts.nunito, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FoodColors.foreground)
                        Spacer(Modifier.width(6.dp))
                        Text("유통기한 자동 계산", fontSize = 11.sp, color = FoodColors.mutedForeground)
                    }
                    Spacer(Modifier.height(8.dp))
                    CategoryGrid(selectedId = formCategory, onSelect = onCategorySelect)
                }
                Column {
                    Text("유통기한", fontFamily = AppFonts.nunito, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = FoodColors.foreground)
                    Spacer(Modifier.height(8.dp))
                    DateField(value = formExpiry, onValueChange = onExpiryChange)
                }
                Button(
                    onClick = onSubmit,
                    enabled = formName.isNotBlank() && !manualSuccess,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (manualSuccess) FoodColors.primaryDark else FoodColors.primary,
                        disabledContainerColor = FoodColors.primary.copy(alpha = 0.4f),
                    ),
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                ) {
                    if (manualSuccess) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("추가됐어요!", fontFamily = AppFonts.nunito, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                    } else {
                        Text("$formEmoji ${formName.ifBlank { "식자재" }} 추가하기", fontFamily = AppFonts.nunito, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun CategoryGrid(selectedId: String, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CATEGORIES.chunked(5).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { cat ->
                    val selected = selectedId == cat.id
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) FoodColors.primarySoft else FoodColors.muted)
                            .border(2.dp, if (selected) FoodColors.primary else Color.Transparent, RoundedCornerShape(16.dp))
                            .clickable { onSelect(cat.id) }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(cat.emoji, fontSize = 20.sp)
                        Text(
                            cat.label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold,
                            color = if (selected) FoodColors.primary else FoodColors.mutedForeground,
                            maxLines = 1, textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DateField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        textStyle = LocalTextStyle.current.copy(fontFamily = AppFonts.dmMono),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = FoodColors.primary,
            unfocusedBorderColor = FoodColors.border,
            focusedContainerColor = FoodColors.muted,
            unfocusedContainerColor = FoodColors.muted,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
