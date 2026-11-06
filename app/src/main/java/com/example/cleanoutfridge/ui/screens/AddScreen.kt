package com.example.cleanoutfridge.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RadioButtonUnchecked
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.example.cleanoutfridge.BuildConfig
import com.example.cleanoutfridge.data.remote.ApiException
import com.example.cleanoutfridge.data.remote.NetworkUnavailableException
import com.example.cleanoutfridge.data.remote.dto.VisionScanItemDto
import com.example.cleanoutfridge.data.repository.VisionRepository
import com.example.cleanoutfridge.model.CATEGORIES
import com.example.cleanoutfridge.model.FoodItem
import com.example.cleanoutfridge.model.addDays
import com.example.cleanoutfridge.model.categoryById
import com.example.cleanoutfridge.model.fmtDate
import com.example.cleanoutfridge.model.today
import com.example.cleanoutfridge.ui.theme.AppFonts
import com.example.cleanoutfridge.ui.theme.FoodColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

private enum class ScanPhase { IDLE, ANALYZING, RESULTS }

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

private fun VisionScanItemDto.toDetectedItem(index: Int): DetectedItem {
    val cat = categoryById(category)
    return DetectedItem(
        id = "det-$index",
        name = name,
        category = if (cat != null) category else "기타",
        emoji = emoji.ifBlank { cat?.emoji ?: "🍱" },
        confidence = confidence,
        expiryDate = addDays(expiryDays),
        selected = true,
        editOpen = false,
    )
}

/** Compresses the bitmap at [uri] to a JPEG byte array sized for upload (longest side capped at 1600px). */
private fun readImageBytes(context: Context, uri: Uri): ByteArray {
    val input = context.contentResolver.openInputStream(uri)
        ?: throw IllegalStateException("이미지를 열 수 없어요.")
    val original = input.use { BitmapFactory.decodeStream(it) }
        ?: throw IllegalStateException("이미지를 읽을 수 없어요.")

    val maxSide = 1600
    val scale = maxSide.toFloat() / maxOf(original.width, original.height)
    val bitmap = if (scale < 1f) {
        Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
    } else {
        original
    }

    return ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        out.toByteArray()
    }
}

private fun createCameraOutputUri(context: Context): Uri {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(dir, "scan-${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
}

@Composable
fun AddScreen(
    onAdd: (FoodItem) -> Unit,
    onDone: () -> Unit,
    onFullScreenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val visionRepository = remember { VisionRepository() }

    var phase by remember { mutableStateOf(ScanPhase.IDLE) }
    val detected = remember { mutableStateListOf<DetectedItem>() }
    var bulkSuccess by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }
    var manualSuccess by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showSourcePicker by remember { mutableStateOf(false) }

    var formName by remember { mutableStateOf("") }
    var formCategory by remember { mutableStateOf("유제품") }
    var formEmoji by remember { mutableStateOf("🥛") }
    var formExpiry by remember { mutableStateOf(addDays(7)) }

    fun runScan(uri: Uri) {
        phase = ScanPhase.ANALYZING
        scanError = null
        scope.launch {
            try {
                val bytes = readImageBytes(context, uri)
                val items = visionRepository.scanPhoto(bytes)
                detected.clear()
                detected.addAll(items.mapIndexed { i, dto -> dto.toDetectedItem(i) })
                if (detected.isEmpty()) {
                    scanError = "사진에서 식재료를 찾지 못했어요. 다시 찍어보시겠어요?"
                    phase = ScanPhase.IDLE
                } else {
                    phase = ScanPhase.RESULTS
                }
            } catch (e: Exception) {
                scanError = e.toScanErrorMessage()
                phase = ScanPhase.IDLE
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri -> if (uri != null) runScan(uri) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success -> pendingCameraUri?.let { if (success) runScan(it) } }

    fun launchCamera() {
        val uri = createCameraOutputUri(context)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    fun launchGallery() {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    LaunchedEffect(phase) { onFullScreenChange(phase != ScanPhase.IDLE) }

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
            ScanPhase.ANALYZING -> AnalyzingPhase()
            ScanPhase.RESULTS -> ResultsPhase(
                detected = detected,
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
                onRescan = { detected.clear(); phase = ScanPhase.IDLE; showSourcePicker = true },
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
                                unit = "개",
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
                                unit = "개",
                            ),
                        )
                        manualSuccess = true
                    }
                },
                onCameraClick = { showSourcePicker = true },
                scanError = scanError,
                onDismissScanError = { scanError = null },
            )
        }

        if (showSourcePicker) {
            PhotoSourceSheet(
                onDismiss = { showSourcePicker = false },
                onTakePhoto = { showSourcePicker = false; launchCamera() },
                onPickFromGallery = { showSourcePicker = false; launchGallery() },
            )
        }
    }
}

@Composable
private fun PhotoSourceSheet(onDismiss: () -> Unit, onTakePhoto: () -> Unit, onPickFromGallery: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(FoodColors.card)
                .clickable(enabled = false) {}
                .padding(20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "사진으로 식재료 인식",
                fontFamily = AppFonts.nunito, fontWeight = FontWeight.Black, fontSize = 18.sp, color = FoodColors.foreground,
            )
            Text(
                "한 장에 여러 식재료가 보이면 모두 인식해서 유통기한까지 채워드려요",
                fontFamily = AppFonts.dmSans, fontSize = 12.sp, color = FoodColors.mutedForeground,
            )
            Spacer(Modifier.height(4.dp))
            PhotoSourceOption(
                icon = Icons.Filled.PhotoCamera,
                label = "사진 찍기",
                onClick = onTakePhoto,
            )
            PhotoSourceOption(
                icon = Icons.Filled.Image,
                label = "갤러리에서 선택",
                onClick = onPickFromGallery,
            )
        }
    }
}

@Composable
private fun PhotoSourceOption(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FoodColors.muted)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(FoodColors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Text(label, fontFamily = AppFonts.nunito, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FoodColors.foreground)
    }
}

@Composable
private fun AnalyzingPhase() {
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
                    "사진 속 식재료와 유통기한을 찾고 있어요...",
                    fontFamily = AppFonts.mono, fontSize = 14.sp, color = Color(0xFFA8D48A),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ResultsPhase(
    detected: List<DetectedItem>,
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
                    Text("AI가 자동으로 분석했어요", fontFamily = AppFonts.dmMono, fontSize = 12.sp, color = FoodColors.mutedForeground)
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
    scanError: String?,
    onDismissScanError: () -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (scanError != null) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(FoodColors.urgentSoft)
                        .clickable { onDismissScanError() }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = null, tint = FoodColors.urgent, modifier = Modifier.size(16.dp))
                    Text(scanError, fontFamily = AppFonts.dmSans, fontSize = 12.sp, color = FoodColors.urgent, modifier = Modifier.weight(1f))
                }
            }
        }
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

private fun Exception.toScanErrorMessage(): String = when (this) {
    is NetworkUnavailableException -> message ?: "네트워크 연결을 확인해주세요."
    is ApiException -> message
    is IllegalStateException -> message ?: "사진을 처리할 수 없어요."
    else -> "사진 분석 중 문제가 발생했어요. 다시 시도해주세요."
}
