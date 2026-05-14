package com.example.lostfoundai.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lostfoundai.R
import com.example.lostfoundai.ai.GeminiConfig
import com.example.lostfoundai.data.MapObject
import com.example.lostfoundai.data.MapObjectType
import com.example.lostfoundai.data.MissingItem
import com.example.lostfoundai.data.PointF
import com.example.lostfoundai.data.RoomBoundary
import com.example.lostfoundai.data.RoomShapePreset
import com.example.lostfoundai.data.SavedBoundary
import com.example.lostfoundai.data.chineseName
import com.example.lostfoundai.data.defaultHeight
import com.example.lostfoundai.data.defaultWidth
import com.example.lostfoundai.data.emoji
import com.example.lostfoundai.data.getDefaultDimensions
import com.example.lostfoundai.ui.components.Toolbar
import com.example.lostfoundai.ui.theme.ErrorRed
import com.example.lostfoundai.ui.theme.MapBackground
import com.example.lostfoundai.ui.theme.OnSurfaceVariantColor
import com.example.lostfoundai.ui.theme.PrimaryIndigo
import com.example.lostfoundai.ui.theme.SurfaceColor
import com.example.lostfoundai.ui.theme.WarnYellow
import com.example.lostfoundai.utils.BoundaryUtils
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MapScreen(
        mapViewModel: MapViewModel,
        searchViewModel: SearchViewModel,
        onNavigateHome: () -> Unit = {}
) {
    val mapObjects by mapViewModel.mapObjects.collectAsState(initial = emptyList())
    val predictedSpots by mapViewModel.predictedSpots.collectAsState()
    val searchItems by searchViewModel.filteredItems.collectAsState(initial = emptyList())
    val roomBoundary by mapViewModel.roomBoundary.collectAsState()
    val savedBoundaries by mapViewModel.savedBoundaries.collectAsState()
    val gridEnabled by mapViewModel.gridEnabled.collectAsState()
    val walkPath by mapViewModel.walkPath.collectAsState()
    val isRecordingWalkPath by mapViewModel.isRecordingWalkPath.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(true) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showBoundarySheet by remember { mutableStateOf(false) }
    var isDrawingBoundary by remember { mutableStateOf(false) }
    var drawingVertices by remember { mutableStateOf<List<PointF>>(emptyList()) }
    // Naming dialog for custom boundaries
    var showNameDialog by remember { mutableStateOf(false) }
    var pendingBoundaryVertices by remember { mutableStateOf<List<PointF>>(emptyList()) }

    // Manual specify location
    var isSpecifyingLocation by remember { mutableStateOf(false) }
    var specifiedLocation by remember { mutableStateOf<PointF?>(null) }
    var editingItem by remember { mutableStateOf<com.example.lostfoundai.data.MissingItem?>(null) }

    // Form States hoisted
    var itemName by remember { mutableStateOf("") }
    var categoryExpanded by remember { mutableStateOf(false) }
    val categories = listOf("飾品", "隨身物品", "電子產品", "紙本", "衛浴用品", "家電配件", "衣物", "自訂")
    var selectedCategory by remember { mutableStateOf(categories[0]) }
    var customCategory by remember { mutableStateOf("") }

    var sizeExpanded by remember { mutableStateOf(false) }
    val sizes =
            when (selectedCategory) {
                "飾品" -> listOf("極小 (< 2cm)", "小 (2-5cm)", "中 (5-10cm)", "自訂")
                "隨身物品" -> listOf("小 (5-15cm)", "中 (15-30cm)", "大 (> 30cm)", "自訂")
                "電子產品" -> listOf("小 (手機/滑鼠)", "中 (平板/鍵盤)", "大 (筆電/螢幕)", "自訂")
                "紙本" -> listOf("小 (名片/票據)", "中 (A5/筆記本)", "大 (A4/文件夾)", "自訂")
                "衛浴用品" -> listOf("小 (牙刷/刮鬍刀)", "中 (瓶罐/毛巾)", "大 (浴巾/臉盆)", "自訂")
                "家電配件" -> listOf("小 (遙控器/線材)", "中 (吹風機/快煮壺)", "大 (風扇/吸塵器)", "自訂")
                "衣物" -> listOf("小 (襪子/內衣)", "中 (上衣/褲子)", "大 (外套/大衣)", "自訂")
                else -> listOf("極小", "小", "中", "大", "自訂")
            }
    var selectedSize by remember { mutableStateOf(sizes[0]) }

    LaunchedEffect(selectedCategory) {
        if (selectedSize !in sizes) {
            selectedSize = sizes[0]
        }
    }
    var customSize by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val isKeyboardOpen = WindowInsets.isImeVisible

    if (showSearchSheet) {
        BackHandler {
            if (isKeyboardOpen) {
                keyboardController?.hide()
                focusManager.clearFocus()
            } else {
                showSearchSheet = false
                editingItem = null
            }
        }
    }

    // Rename dialog state
    var renamingBoundary by remember { mutableStateOf<SavedBoundary?>(null) }
    var renameText by remember { mutableStateOf("") }

    var showClearRoomDialog by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val density = LocalDensity.current.density

    var mapScale by remember { mutableFloatStateOf(1f) }
    var mapOffset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    // Global Drag & Drop State
    var draggingType by remember { mutableStateOf<MapObjectType?>(null) }
    var draggingPos by remember { mutableStateOf(Offset.Zero) }
    var contentRootOffset by remember { mutableStateOf(Offset.Zero) }
    // Track the ghost's last valid (non-overlapping, within-bounds) position
    var lastValidGhostX by remember { mutableFloatStateOf(0f) }
    var lastValidGhostY by remember { mutableFloatStateOf(0f) }
    var hasValidGhostPos by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    // Furniture drill-down state
    var selectedFurniture by remember { mutableStateOf<MapObject?>(null) }
    var showFurnitureSheet by remember { mutableStateOf(false) }

    // Gemini Vision / photo capture state
    var preLinkFurnitureId by remember { mutableStateOf<String?>(null) }
    var capturedPhotoPath by remember { mutableStateOf<String?>(null) }
    var isAnalyzingPhoto by remember { mutableStateOf(false) }
    var detectedChips by remember { mutableStateOf<List<String>>(emptyList()) }
    val context = LocalContext.current
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        bitmap?.let { bmp ->
            val file = java.io.File(context.filesDir, "item_${System.currentTimeMillis()}.jpg")
            java.io.FileOutputStream(file).use { out ->
                bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            capturedPhotoPath = file.absolutePath
            isAnalyzingPhoto = true
            detectedChips = emptyList()
            coroutineScope.launch {
                try {
                    val model = GenerativeModel(
                        modelName = "gemini-2.5-flash",
                        apiKey = GeminiConfig.API_KEY
                    )
                    val response = model.generateContent(
                        content {
                            image(bmp)
                            text(
                                "請辨識圖片中的主要物品名稱與類別，以JSON格式回應，格式如下：\n" +
                                "{\"name\":\"物品名稱，用繁體中文2-5個字\",\"category\":\"類別\"}\n" +
                                "category 必須是以下其中一個：飾品、隨身物品、電子產品、紙本、衛浴用品、家電配件、衣物"
                            )
                        }
                    )
                    val raw = response.text ?: return@launch
                    val jsonStr = Regex("""\{[^}]+\}""").find(raw)?.value ?: return@launch
                    val json = org.json.JSONObject(jsonStr)
                    val detectedName = json.optString("name", "")
                    val detectedCat = json.optString("category", "其他")
                    isAnalyzingPhoto = false
                    if (detectedName.isNotEmpty()) {
                        detectedChips = listOf(detectedName)
                        if (itemName.isEmpty()) itemName = detectedName
                    }
                    if (detectedCat in categories) selectedCategory = detectedCat
                } catch (_: Exception) {
                    isAnalyzingPhoto = false
                }
            }
        }
    }

    ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                    Spacer(Modifier.height(32.dp))
                    Text(
                            "應用程式設定",
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.titleLarge
                    )
                    HorizontalDivider()
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("顯示背景網格")
                        Switch(
                                checked = gridEnabled,
                                onCheckedChange = { mapViewModel.setGridEnabled(it) }
                        )
                    }
                    HorizontalDivider()
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("記錄行走路線")
                        Switch(
                                checked = isRecordingWalkPath,
                                onCheckedChange = { mapViewModel.toggleWalkPathRecording() }
                        )
                    }
                    if (walkPath.isNotEmpty()) {
                        TextButton(
                                onClick = { mapViewModel.clearWalkPath() },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        ) {
                            Text("清除路線 (${walkPath.size} 個點)")
                        }
                    }
                    TextButton(
                            onClick = { showClearRoomDialog = true },
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) { Text("清除房間", color = MaterialTheme.colorScheme.error) }
                    TextButton(
                            onClick = onNavigateHome,
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) { Text("返回主頁") }
                }
            }
    ) {
        if (showClearRoomDialog) {
            AlertDialog(
                    onDismissRequest = { showClearRoomDialog = false },
                    title = { Text("確認清除房間") },
                    text = { Text("您確定要清除所有房間物件並重設邊界嗎？此操作無法復原。") },
                    confirmButton = {
                        TextButton(
                                onClick = {
                                    mapViewModel.clearRoom()
                                    showClearRoomDialog = false
                                }
                        ) { Text("確認刪除", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearRoomDialog = false }) { Text("取消") }
                    }
            )
        }

        Scaffold { padding ->
            Box(
                    modifier =
                            Modifier.fillMaxSize().padding(padding).onGloballyPositioned { coords ->
                                contentRootOffset = coords.positionInRoot()
                            }
            ) {
                // Main Map Area (Full Size)
                Box(modifier = Modifier.fillMaxSize().background(MapBackground)) {
                    MapCanvas(
                            onDrawingVertexMove = { idx, newPos ->
                                val verts = drawingVertices.toMutableList()
                                verts[idx] = newPos
                                // Maintain orthogonality for next vertex
                                if (idx + 1 < verts.size) {
                                    val next = verts[idx + 1]
                                    val isHoriz = verts[idx].y == (if (idx > 0) verts[idx - 1] else verts[idx]).y
                                    verts[idx + 1] = if (isHoriz) PointF(next.x, verts[idx].y) else PointF(verts[idx].x, next.y)
                                }
                                drawingVertices = verts
                            },
                            mapObjects = mapObjects,
                            predictedSpots = predictedSpots,
                            storedItems = searchItems,
                            walkPath = walkPath,
                            isEditing = isEditing,
                            mapScale = mapScale,
                            onScaleChange = { mapScale = it },
                            mapOffset = mapOffset,
                            onOffsetChange = { mapOffset = it },
                            onCanvasSize = { canvasSize = it },
                            boundaryVertices = roomBoundary.vertices,
                            isDrawingBoundary = isDrawingBoundary,
                            drawingVertices = drawingVertices,
                            isSpecifyingLocation = isSpecifyingLocation,
                            isRecordingWalkPath = isRecordingWalkPath,
                            specifiedLocation = specifiedLocation,
                            onCanvasTap = { tapOffsetDp ->
                                when {
                                    isDrawingBoundary -> {
                                        if (drawingVertices.isNotEmpty()) {
                                            val lastPoint = drawingVertices.last()
                                            val dx = kotlin.math.abs(tapOffsetDp.x - lastPoint.x)
                                            val dy = kotlin.math.abs(tapOffsetDp.y - lastPoint.y)
                                            val snappedPoint = if (dx > dy) {
                                                PointF(tapOffsetDp.x, lastPoint.y)
                                            } else {
                                                PointF(lastPoint.x, tapOffsetDp.y)
                                            }
                                            drawingVertices = drawingVertices + snappedPoint
                                        } else {
                                            drawingVertices = drawingVertices + tapOffsetDp
                                        }
                                    }
                                    isSpecifyingLocation -> {
                                        specifiedLocation = tapOffsetDp
                                        isSpecifyingLocation = false
                                        showSearchSheet = true
                                    }
                                    isRecordingWalkPath -> mapViewModel.addWalkPathPoint(tapOffsetDp.x, tapOffsetDp.y)
                                }
                            },
                            onFurnitureTap = { obj ->
                                selectedFurniture = obj
                                showFurnitureSheet = true
                            },
                            onObjectMove = { id, dx, dy ->
                                val obj = mapObjects.find { it.id == id }
                                if (obj != null) {
                                    mapViewModel.updateMapObject(
                                            obj.copy(x = obj.x + dx, y = obj.y + dy)
                                    )
                                }
                            },
                            onObjectRotate = { id ->
                                val obj = mapObjects.find { it.id == id }
                                if (obj != null) {
                                    val newRotation = (obj.rotation + 90f) % 360f
                                    mapViewModel.updateMapObject(
                                            obj.copy(
                                                    rotation = newRotation,
                                                    width = obj.height,
                                                    height = obj.width
                                            )
                                    )
                                }
                            },
                            onDeleteObject = { id -> mapViewModel.removeMapObject(id) },
                            onObjectScaleChange = { id, newScale ->
                                mapViewModel.updateObjectScale(id, newScale)
                            },
                            gridEnabled = gridEnabled
                    )
                }

                // Toolbar as Overlay
                if (isEditing) {
                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        Toolbar(
                                onDragStart = { type, offset ->
                                    draggingType = type
                                    draggingPos = offset
                                },
                                onDrag = { dragAmount -> draggingPos += dragAmount },
                                onDragEnd = { droppedType ->
                                    // Always place at the ghost's displayed position
                                    if (hasValidGhostPos) {
                                        val dims = droppedType.getDefaultDimensions()
                                        mapViewModel.addMapObject(
                                                MapObject(
                                                        type = droppedType,
                                                        x = lastValidGhostX,
                                                        y = lastValidGhostY,
                                                        width = dims.first,
                                                        height = dims.second
                                                )
                                        )
                                    }
                                    draggingType = null
                                    hasValidGhostPos = false
                                }
                        )
                    }
                }

                // Ghost Dragging Overlay
                if (draggingType != null) {
                    val dims = draggingType!!.getDefaultDimensions()
                    val rawWidth = dims.first
                    val rawHeight = dims.second

                    // Compute effective boundary vertices for polygon check
                    val effectiveBoundary =
                            if (roomBoundary.vertices.isNotEmpty()) roomBoundary.vertices
                            else {
                                val pad = 32f
                                val cw = canvasSize.width / density
                                val ch = canvasSize.height / density
                                listOf(
                                        PointF(pad, pad),
                                        PointF(cw - pad, pad),
                                        PointF(cw - pad, ch - pad),
                                        PointF(pad, ch - pad)
                                )
                            }

                    val localDragX = draggingPos.x - contentRootOffset.x
                    val localDragY = draggingPos.y - contentRootOffset.y

                    // Inverse project: screen coords → map virtual dp
                    val centerX = canvasSize.width / 2f
                    val centerY = canvasSize.height / 2f
                    val mapPxX = (localDragX - mapOffset.x - centerX) / mapScale + centerX
                    val mapPxY = (localDragY - mapOffset.y - centerY) / mapScale + centerY
                    val virtualXStr = mapPxX / density - rawWidth / 2f
                    val virtualYStr = mapPxY / density - rawHeight / 2f


                    // Per-axis sliding collision: boundary + existing objects treated the same way.
                    // This allows the ghost to slide along walls when blocked in one direction.
                    var ghostX = virtualXStr
                    var ghostY = virtualYStr
                    val prevX = if (hasValidGhostPos) lastValidGhostX else virtualXStr
                    val prevY = if (hasValidGhostPos) lastValidGhostY else virtualYStr
                    val stepGhostX = virtualXStr - prevX
                    val stepGhostY = virtualYStr - prevY

                    fun isGhostValid(px: Float, py: Float): Boolean {
                        if (!BoundaryUtils.rectInPolygon(px, py, rawWidth, rawHeight, effectiveBoundary))
                            return false
                        return mapObjects.none { other ->
                            px < other.x + (other.width * other.scale) &&
                                    px + rawWidth > other.x &&
                                    py < other.y + (other.height * other.scale) &&
                                    py + rawHeight > other.y
                        }
                    }

                    // --- Try target position directly (teleport to finger if clear) ---
                    if (isGhostValid(virtualXStr, virtualYStr)) {
                        ghostX = virtualXStr
                        ghostY = virtualYStr
                    } else {
                        // --- Resolve X axis ---
                        var targetGhostX = prevX + stepGhostX
                        var canMoveGhostX = true
                        if (!isGhostValid(targetGhostX, prevY)) {
                            var low = 0f
                            var high = stepGhostX
                            for (i in 0..5) {
                                val mid = (low + high) / 2
                                if (isGhostValid(prevX + mid, prevY)) low = mid else high = mid
                            }
                            targetGhostX = prevX + low
                            if (targetGhostX == prevX) canMoveGhostX = false
                        }
                        val resolvedGhostX = if (canMoveGhostX) targetGhostX else prevX

                        // --- Resolve Y axis ---
                        var targetGhostY = prevY + stepGhostY
                        var canMoveGhostY = true
                        if (!isGhostValid(resolvedGhostX, targetGhostY)) {
                            var low = 0f
                            var high = stepGhostY
                            for (i in 0..5) {
                                val mid = (low + high) / 2
                                if (isGhostValid(resolvedGhostX, prevY + mid)) low = mid else high = mid
                            }
                            targetGhostY = prevY + low
                            if (targetGhostY == prevY) canMoveGhostY = false
                        }
                        ghostX = resolvedGhostX
                        ghostY = if (canMoveGhostY) targetGhostY else prevY
                    }

                    // Update last valid position
                    lastValidGhostX = ghostX
                    lastValidGhostY = ghostY
                    hasValidGhostPos = true

                    // Forward project: map virtual dp → screen px
                    val testPxX = ghostX * density
                    val testPxY = ghostY * density
                    val renderOffsetX = centerX + (testPxX - centerX) * mapScale + mapOffset.x
                    val renderOffsetY = centerY + (testPxY - centerY) * mapScale + mapOffset.y

                    Box(
                            modifier =
                                    Modifier.offset {
                                        androidx.compose.ui.unit.IntOffset(
                                                renderOffsetX.toInt(),
                                                renderOffsetY.toInt()
                                        )
                                    }
                                            .size(
                                                    (rawWidth * mapScale).dp,
                                                    (rawHeight * mapScale).dp
                                            )
                                            .graphicsLayer { alpha = 0.7f }
                    ) { MapObjectVisuals(type = draggingType!!, modifier = Modifier.fillMaxSize()) }
                }

                // Buttons Overlay
                if (isDrawingBoundary) {
                    // Drawing mode controls — capsule style
                    Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 28.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp),
                            shadowElevation = 8.dp,
                            color = SurfaceColor
                    ) {
                        Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { drawingVertices = emptyList() },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
                            ) { Text("清除重畫") }
                            Button(
                                    onClick = {
                                        if (drawingVertices.size >= 3) {
                                            val first = drawingVertices.first()
                                            val last = drawingVertices.last()
                                            var finalVertices = drawingVertices
                                            if (first.x != last.x && first.y != last.y) {
                                                val dx = kotlin.math.abs(first.x - last.x)
                                                val dy = kotlin.math.abs(first.y - last.y)
                                                val extraPoint = if (dx > dy) PointF(first.x, last.y) else PointF(last.x, first.y)
                                                finalVertices = finalVertices + extraPoint
                                            }
                                            pendingBoundaryVertices = finalVertices
                                            showNameDialog = true
                                        }
                                        isDrawingBoundary = false
                                        drawingVertices = emptyList()
                                    },
                                    enabled = drawingVertices.size >= 3,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
                            ) { Text("完成 (${drawingVertices.size} 個頂點)") }
                        }
                    }
                    Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp),
                            color = Color(0xFF546E7A)
                    ) {
                        TextButton(onClick = { isDrawingBoundary = false; drawingVertices = emptyList() }) {
                            Text("取消繪製", color = Color.White)
                        }
                    }
                } else if (isEditing && !showBoundarySheet) {
                    Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 28.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp),
                            shadowElevation = 8.dp,
                            color = SurfaceColor
                    ) {
                        Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showBoundarySheet = true },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
                            ) { Text("邊界管理") }
                            Button(
                                onClick = { isEditing = false },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
                            ) { Text("完成編輯") }
                        }
                    }
                }

                // Walk path recording banner
                if (isRecordingWalkPath) {
                    Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp),
                            color = WarnYellow,
                            shadowElevation = 4.dp
                    ) {
                        Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(Color(0xFFFF3D00), CircleShape)
                            )
                            Text(
                                    "路線記錄中，點地圖新增路徑點",
                                    color = Color(0xFF1A1C2E),
                                    style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                // Map legend (bottom-right, normal view only)
                if (!isDrawingBoundary && !isEditing &&
                        (predictedSpots.isNotEmpty() || walkPath.isNotEmpty() || searchItems.any { it.manualX != null })) {
                    Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 12.dp, bottom = 90.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                            color = SurfaceColor.copy(alpha = 0.92f),
                            shadowElevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (walkPath.isNotEmpty()) LegendRow(Color(0xFFFFD600), "行走路線")
                            if (searchItems.any { it.manualX != null }) LegendRow(Color(0xFF4CAF50), "物品位置")
                            if (predictedSpots.isNotEmpty()) LegendRow(Color(0xFFE53935), "AI 預測")
                        }
                    }
                }

                // Settings button (Material Icon FAB)
                if (!isDrawingBoundary) {
                    Surface(
                            onClick = { coroutineScope.launch { drawerState.open() } },
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                                .size(44.dp)
                                .shadow(4.dp, CircleShape),
                            shape = CircleShape,
                            color = SurfaceColor
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                    Icons.Filled.Settings,
                                    contentDescription = "Settings",
                                    tint = PrimaryIndigo,
                                    modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                if (!isDrawingBoundary && !isEditing) {
                    // Bottom capsule: clear predictions | 新增物品 | 尋找
                    Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 28.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp),
                            shadowElevation = 8.dp,
                            color = SurfaceColor
                    ) {
                        Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (predictedSpots.isNotEmpty() || specifiedLocation != null) {
                                IconButton(
                                        onClick = {
                                            mapViewModel.clearPredictedSpots()
                                            specifiedLocation = null
                                        },
                                        modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "清除預測",
                                            tint = Color.Gray, modifier = Modifier.size(18.dp))
                                }
                            }
                            Button(
                                    onClick = {
                                        showHistory = false
                                        showSearchSheet = true
                                    },
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                            ) { Text("新增物品", fontSize = 14.sp) }
                            FilledTonalButton(
                                    onClick = {
                                        showHistory = true
                                        showSearchSheet = true
                                    },
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
                            ) { Text("尋找", fontSize = 14.sp) }
                        }
                    }

                    // Top right edit FAB
                    Surface(
                            onClick = { isEditing = true },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .size(44.dp)
                                .shadow(4.dp, CircleShape),
                            shape = CircleShape,
                            color = SurfaceColor
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = "Edit",
                                    tint = PrimaryIndigo,
                                    modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSearchSheet) {

        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                            onTap = {
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                            }
                                    )
                                }
                                .background(Color.Black.copy(alpha = 0.5f))
        ) {
            ModalBottomSheet(
                    onDismissRequest = { showSearchSheet = false },
                    sheetState = sheetState,
                    containerColor = Color.White
            ) {
                Column(
                        modifier =
                                Modifier.fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    if (showHistory) {
                        // ... History View ...
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                    "查詢紀錄",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.Black
                            )
                            IconButton(onClick = { showHistory = false }) {
                                Icon(
                                        Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "返回表單",
                                        modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                            items(searchItems) { item ->
                                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                    Row(
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                                modifier =
                                                        Modifier.weight(1f).clickable {
                                                            if (item.manualX != null &&
                                                                            item.manualY != null
                                                            ) {
                                                                mapViewModel.clearPredictedSpots()
                                                                specifiedLocation =
                                                                        PointF(
                                                                                item.manualX,
                                                                                item.manualY
                                                                        )
                                                            } else {
                                                                mapViewModel.startAIPrediction(
                                                                        item.id
                                                                )
                                                                specifiedLocation = null
                                                            }
                                                            showSearchSheet = false
                                                        }
                                        ) {
                                            Text(
                                                    text =
                                                            item.name +
                                                                    if (item.manualX != null &&
                                                                                    item.manualY !=
                                                                                            null
                                                                    )
                                                                            " (已指定)"
                                                                    else "",
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                    text =
                                                            "類別: ${item.category.name} | 大小: ${getSizeDisplay(item.category, item.size)}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color =
                                                            MaterialTheme.colorScheme
                                                                    .onSurfaceVariant
                                            )
                                            // Linked furniture badge
                                            if (item.linkedFurnitureId != null) {
                                                val linkedObj = mapObjects.find { it.id == item.linkedFurnitureId }
                                                if (linkedObj != null) {
                                                    Text(
                                                            text = "${linkedObj.type.emoji()} ${linkedObj.type.chineseName()}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color(0xFF4CAF50)
                                                    )
                                                }
                                            }
                                            // Photo thumbnail
                                            if (item.photoPath != null) {
                                                val photoBmp = remember(item.photoPath) {
                                                    BitmapFactory.decodeFile(item.photoPath)
                                                }
                                                photoBmp?.let { bmp ->
                                                    Spacer(Modifier.height(4.dp))
                                                    androidx.compose.foundation.Image(
                                                            bitmap = bmp.asImageBitmap(),
                                                            contentDescription = null,
                                                            modifier = Modifier
                                                                .size(40.dp)
                                                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
                                                            contentScale = ContentScale.Crop
                                                    )
                                                }
                                            }
                                        }
                                        Row {
                                            IconButton(
                                                    onClick = {
                                                        // Edit Item
                                                        editingItem = item
                                                        itemName = item.name
                                                        // Simple mapping, fallback to custom
                                                        val mappedCategory =
                                                                when (item.category) {
                                                                    com.example.lostfoundai.data
                                                                            .ItemCategory
                                                                            .ACCESSORY -> "飾品"
                                                                    com.example.lostfoundai.data
                                                                            .ItemCategory
                                                                            .BELONGINGS -> "隨身物品"
                                                                    com.example.lostfoundai.data
                                                                            .ItemCategory
                                                                            .ELECTRONICS -> "電子產品"
                                                                    com.example.lostfoundai.data
                                                                            .ItemCategory.PAPER ->
                                                                            "紙本"
                                                                    com.example.lostfoundai.data
                                                                            .ItemCategory
                                                                            .BATHROOM -> "衛浴用品"
                                                                    com.example.lostfoundai.data
                                                                            .ItemCategory
                                                                            .APPLIANCE -> "家電配件"
                                                                    else -> "自訂"
                                                                }
                                                        selectedCategory = mappedCategory

                                                        val mappedSize =
                                                                when (item.size) {
                                                                    com.example.lostfoundai.data
                                                                            .ItemSize.VERY_SMALL ->
                                                                            "極小 (< 2cm)"
                                                                    com.example.lostfoundai.data
                                                                            .ItemSize.SMALL ->
                                                                            "小 (5-10cm)"
                                                                    com.example.lostfoundai.data
                                                                            .ItemSize.MEDIUM ->
                                                                            "中 (15-20cm)"
                                                                    com.example.lostfoundai.data
                                                                            .ItemSize.LARGE ->
                                                                            "大 (> 20cm)"
                                                                }
                                                        selectedSize = mappedSize

                                                        specifiedLocation =
                                                                if (item.manualX != null &&
                                                                                item.manualY != null
                                                                )
                                                                        PointF(
                                                                                item.manualX,
                                                                                item.manualY
                                                                        )
                                                                else null
                                                        showHistory = false
                                                    }
                                            ) {
                                                Text(
                                                        "✏️",
                                                        style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            IconButton(
                                                    onClick = {
                                                        searchViewModel.deleteItem(item.id)
                                                    }
                                            ) {
                                                Text(
                                                        "🗑️",
                                                        style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    } else {
                        // --- Form View ---

                        Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                        ) {
                            Button(
                                    onClick = {
                                        showHistory = true
                                        editingItem = null
                                        itemName = ""
                                        specifiedLocation = null
                                        capturedPhotoPath = null
                                        detectedChips = emptyList()
                                        preLinkFurnitureId = null
                                    },
                                    modifier = Modifier.fillMaxWidth(0.5f)
                            ) { Text("查詢紀錄", color = Color.White) }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Pre-linked furniture card
                        if (preLinkFurnitureId != null) {
                            val linkedObj = mapObjects.find { it.id == preLinkFurnitureId }
                            if (linkedObj != null) {
                                Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                                ) {
                                    Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                                "${linkedObj.type.emoji()} 關聯至${linkedObj.type.chineseName()}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color(0xFF2E7D32)
                                        )
                                        TextButton(onClick = { preLinkFurnitureId = null }) {
                                            Text("移除", color = Color.Gray, style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                                value = itemName,
                                onValueChange = { itemName = it },
                                label = { Text("物品名稱", color = Color(0xFF2A2A2A)) },
                                colors =
                                        androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = Color.Black,
                                                unfocusedTextColor = Color.Black
                                        ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions =
                                        KeyboardActions(
                                                onDone = {
                                                    keyboardController?.hide()
                                                    focusManager.clearFocus()
                                                    println("Keyboard Debug: Done Pressed")
                                                }
                                        ),
                                modifier =
                                        Modifier.fillMaxWidth().onPreviewKeyEvent {
                                            if (it.key == Key.Enter || it.key == Key.NumPadEnter) {
                                                keyboardController?.hide()
                                                focusManager.clearFocus()
                                                true
                                            } else false
                                        }
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Camera + AI suggestions
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                    onClick = { cameraLauncher.launch(null) },
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                            ) { Text("📷 拍照辨識") }

                            if (capturedPhotoPath != null) {
                                val photoBmp = remember(capturedPhotoPath) {
                                    BitmapFactory.decodeFile(capturedPhotoPath)
                                }
                                photoBmp?.let { bmp ->
                                    Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = "Captured photo",
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                    )
                                }
                            }

                            if (isAnalyzingPhoto) {
                                CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                )
                                Text("AI 辨識中…", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            }
                        }

                        // Suggestion chips
                        if (detectedChips.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                detectedChips.forEach { chip ->
                                    SuggestionChip(
                                            onClick = { itemName = chip },
                                            label = { Text(chip) }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category Dropdown
                        ExposedDropdownMenuBox(
                                expanded = categoryExpanded,
                                onExpandedChange = { categoryExpanded = it }
                        ) {
                            OutlinedTextField(
                                    value = selectedCategory,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("物品類別", color = Color(0xFF2A2A2A)) },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                                expanded = categoryExpanded
                                        )
                                    },
                                    colors =
                                            androidx.compose.material3.OutlinedTextFieldDefaults
                                                    .colors(
                                                            focusedTextColor = Color.Black,
                                                            unfocusedTextColor = Color.Black
                                                    ),
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                    expanded = categoryExpanded,
                                    onDismissRequest = { categoryExpanded = false },
                                    modifier = Modifier.background(Color.White)
                            ) {
                                categories.forEach { option ->
                                    DropdownMenuItem(
                                            text = { Text(option, color = Color.Black) },
                                            onClick = {
                                                selectedCategory = option
                                                categoryExpanded = false
                                            }
                                    )
                                }
                            }
                        }
                        if (selectedCategory == "自訂") {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                    value = customCategory,
                                    onValueChange = { customCategory = it },
                                    label = { Text("請輸入自訂類別", color = Color(0xFF2A2A2A)) },
                                    colors =
                                            androidx.compose.material3.OutlinedTextFieldDefaults
                                                    .colors(
                                                            focusedTextColor = Color.Black,
                                                            unfocusedTextColor = Color.Black
                                                    ),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions =
                                            KeyboardActions(
                                                    onDone = {
                                                        keyboardController?.hide()
                                                        focusManager.clearFocus()
                                                        println("Keyboard Debug: Done Pressed")
                                                    }
                                            ),
                                    modifier =
                                            Modifier.fillMaxWidth().onPreviewKeyEvent {
                                                if (it.key == Key.Enter || it.key == Key.NumPadEnter
                                                ) {
                                                    keyboardController?.hide()
                                                    focusManager.clearFocus()
                                                    true
                                                } else false
                                            }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Size Dropdown
                        ExposedDropdownMenuBox(
                                expanded = sizeExpanded,
                                onExpandedChange = { sizeExpanded = it }
                        ) {
                            OutlinedTextField(
                                    value = selectedSize,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("物品大小", color = Color(0xFF2A2A2A)) },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                                expanded = sizeExpanded
                                        )
                                    },
                                    colors =
                                            androidx.compose.material3.OutlinedTextFieldDefaults
                                                    .colors(
                                                            focusedTextColor = Color.Black,
                                                            unfocusedTextColor = Color.Black
                                                    ),
                                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable, true).fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                    expanded = sizeExpanded,
                                    onDismissRequest = { sizeExpanded = false },
                                    modifier = Modifier.background(Color.White)
                            ) {
                                sizes.forEach { option ->
                                    DropdownMenuItem(
                                            text = { Text(option, color = Color.Black) },
                                            onClick = {
                                                selectedSize = option
                                                sizeExpanded = false
                                            }
                                    )
                                }
                            }
                        }
                        if (selectedSize == "自訂") {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                    value = customSize,
                                    onValueChange = { customSize = it },
                                    label = { Text("請輸入自訂大小", color = Color(0xFF2A2A2A)) },
                                    colors =
                                            androidx.compose.material3.OutlinedTextFieldDefaults
                                                    .colors(
                                                            focusedTextColor = Color.Black,
                                                            unfocusedTextColor = Color.Black
                                                    ),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions =
                                            KeyboardActions(
                                                    onDone = {
                                                        keyboardController?.hide()
                                                        focusManager.clearFocus()
                                                        println("Keyboard Debug: Done Pressed")
                                                    }
                                            ),
                                    modifier =
                                            Modifier.fillMaxWidth().onPreviewKeyEvent {
                                                if (it.key == Key.Enter || it.key == Key.NumPadEnter
                                                ) {
                                                    keyboardController?.hide()
                                                    focusManager.clearFocus()
                                                    true
                                                } else false
                                            }
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        if (specifiedLocation == null) {
                            Text(
                                    text = "若沒有指定位置將會預設為 AI 尋找",
                                    color = Color.Gray,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier =
                                            Modifier.padding(bottom = 8.dp)
                                                    .align(Alignment.CenterHorizontally)
                            )
                        }

                        if (specifiedLocation != null) {
                            Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                        onClick = { specifiedLocation = null },
                                        modifier = Modifier.weight(1f)
                                ) { Text("取消指定", color = Color(0xFF1A1A1A)) }
                                OutlinedButton(
                                        onClick = {
                                            showSearchSheet = false
                                            isSpecifyingLocation = true
                                        },
                                        modifier = Modifier.weight(1f)
                                ) { Text("重新指定", color = Color(0xFF1A1A1A)) }
                            }
                        } else {
                            OutlinedButton(
                                    onClick = {
                                        showSearchSheet = false
                                        isSpecifyingLocation = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                            ) { Text("指定位置", color = Color(0xFF1A1A1A)) }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                                onClick = {
                                    val cat =
                                            when (selectedCategory) {
                                                "飾品" ->
                                                        com.example.lostfoundai.data.ItemCategory
                                                                .ACCESSORY
                                                "隨身物品" ->
                                                        com.example.lostfoundai.data.ItemCategory
                                                                .BELONGINGS
                                                "電子產品" ->
                                                        com.example.lostfoundai.data.ItemCategory
                                                                .ELECTRONICS
                                                "紙本" ->
                                                        com.example.lostfoundai.data.ItemCategory
                                                                .PAPER
                                                "衛浴用品" ->
                                                        com.example.lostfoundai.data.ItemCategory
                                                                .BATHROOM
                                                "家電配件" ->
                                                        com.example.lostfoundai.data.ItemCategory
                                                                .APPLIANCE
                                                "衣物" ->
                                                        com.example.lostfoundai.data.ItemCategory
                                                                .CLOTHING
                                                else ->
                                                        com.example.lostfoundai.data.ItemCategory
                                                                .OTHER
                                            }
                                    val sz =
                                            when {
                                                selectedSize.startsWith("極小") ->
                                                        com.example.lostfoundai.data.ItemSize
                                                                .VERY_SMALL
                                                selectedSize.startsWith("小") ->
                                                        com.example.lostfoundai.data.ItemSize.SMALL
                                                selectedSize.startsWith("中") ->
                                                        com.example.lostfoundai.data.ItemSize.MEDIUM
                                                selectedSize.startsWith("大") ->
                                                        com.example.lostfoundai.data.ItemSize.LARGE
                                                else -> com.example.lostfoundai.data.ItemSize.MEDIUM
                                            }

                                    if (editingItem != null) {
                                        searchViewModel.updateItem(
                                                editingItem!!.copy(
                                                        name = itemName.ifEmpty { "新物品" },
                                                        category = cat,
                                                        size = sz,
                                                        manualX = specifiedLocation?.x,
                                                        manualY = specifiedLocation?.y,
                                                        linkedFurnitureId = preLinkFurnitureId ?: editingItem!!.linkedFurnitureId,
                                                        photoPath = capturedPhotoPath ?: editingItem!!.photoPath
                                                )
                                        )
                                    } else {
                                        searchViewModel.addMissingItem(
                                                name = itemName.ifEmpty { "新物品" },
                                                category = cat,
                                                size = sz,
                                                traits = "自訂特徵",
                                                weight = "Medium",
                                                location = "地圖",
                                                manualX = specifiedLocation?.x,
                                                manualY = specifiedLocation?.y,
                                                linkedFurnitureId = preLinkFurnitureId,
                                                photoPath = capturedPhotoPath
                                        )
                                    }
                                    editingItem = null
                                    specifiedLocation = null
                                    itemName = ""
                                    capturedPhotoPath = null
                                    detectedChips = emptyList()
                                    preLinkFurnitureId = null
                                    showHistory = true
                                },
                                modifier =
                                        Modifier.fillMaxWidth(0.6f)
                                                .align(Alignment.CenterHorizontally)
                        ) { Text(if (editingItem != null) "完成編輯" else "完成新增", color = Color.White) }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // Boundary Shape Selection Sheet
    if (showBoundarySheet) {
        // Pending boundary confirm state (for both presets and saved boundaries)
        var pendingConfirmPreset by remember { mutableStateOf<Pair<RoomShapePreset, String>?>(null) }
        var pendingSavedBoundary by remember { mutableStateOf<com.example.lostfoundai.data.SavedBoundary?>(null) }
        var deletingBoundary by remember { mutableStateOf<com.example.lostfoundai.data.SavedBoundary?>(null) }

        // --- Confirm: apply preset ---
        if (pendingConfirmPreset != null) {
            val (preset, label) = pendingConfirmPreset!!
            AlertDialog(
                    onDismissRequest = { pendingConfirmPreset = null },
                    title = { Text("套用「$label」邊界？") },
                    text = { Text("套用後，原邊界外的物件將被移入新邊界內。確定要繼續嗎？") },
                    confirmButton = {
                        TextButton(onClick = {
                            val canvasWidthDp = canvasSize.width / density
                            val canvasHeightDp = canvasSize.height / density
                            val vertices = mapViewModel.generatePresetVertices(preset, canvasWidthDp, canvasHeightDp)
                            mapViewModel.setRoomBoundary(RoomBoundary(preset = preset, vertices = vertices))
                            mapViewModel.relocateObjectsIntoBoundary(vertices)
                            pendingConfirmPreset = null
                            showBoundarySheet = false
                        }) { Text("確定") }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingConfirmPreset = null }) { Text("取消") }
                    }
            )
        }

        // --- Confirm: apply saved boundary ---
        if (pendingSavedBoundary != null) {
            val saved = pendingSavedBoundary!!
            AlertDialog(
                    onDismissRequest = { pendingSavedBoundary = null },
                    title = { Text("套用「${saved.name}」邊界？") },
                    text = { Text("套用後，原邊界外的物件將被移入新邊界內。確定要繼續嗎？") },
                    confirmButton = {
                        TextButton(onClick = {
                            mapViewModel.setRoomBoundary(
                                    RoomBoundary(
                                            preset = RoomShapePreset.CUSTOM,
                                            vertices = saved.vertices,
                                            savedBoundaryId = saved.id
                                    )
                            )
                            mapViewModel.relocateObjectsIntoBoundary(saved.vertices)
                            pendingSavedBoundary = null
                            showBoundarySheet = false
                        }) { Text("確定") }
                    },
                    dismissButton = {
                        TextButton(onClick = { pendingSavedBoundary = null }) { Text("取消") }
                    }
            )
        }

        // --- Confirm: delete saved boundary ---
        if (deletingBoundary != null) {
            val saved = deletingBoundary!!
            AlertDialog(
                    onDismissRequest = { deletingBoundary = null },
                    title = { Text("刪除「${saved.name}」？") },
                    text = { Text("此邊界將永久刪除，無法復原。確定要刪除嗎？") },
                    confirmButton = {
                        TextButton(onClick = {
                            mapViewModel.removeSavedBoundary(saved.id)
                            if (roomBoundary.savedBoundaryId == saved.id) {
                                val canvasWidthDp = canvasSize.width / density
                                val canvasHeightDp = canvasSize.height / density
                                val verts = mapViewModel.generatePresetVertices(
                                        RoomShapePreset.RECTANGLE, canvasWidthDp, canvasHeightDp
                                )
                                mapViewModel.setRoomBoundary(
                                        RoomBoundary(preset = RoomShapePreset.RECTANGLE, vertices = verts)
                                )
                                mapViewModel.relocateObjectsIntoBoundary(verts)
                            }
                            deletingBoundary = null
                        }) { Text("刪除", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { deletingBoundary = null }) { Text("取消") }
                    }
            )
        }

        ModalBottomSheet(
                onDismissRequest = { showBoundarySheet = false },
                containerColor = Color.White
        ) {
            Column(
                    modifier =
                            Modifier.fillMaxWidth()
                                    .heightIn(max = (canvasSize.height / density * 0.66f).dp)
                                    .padding(horizontal = 24.dp, vertical = 16.dp)
                                    .verticalScroll(rememberScrollState())
            ) {
                Text("選擇邊界形狀", style = MaterialTheme.typography.titleLarge, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))

                // --- Preset shapes ---
                Text("預設形狀", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))

                val presets =
                        listOf(
                                RoomShapePreset.RECTANGLE to "矩形",
                                RoomShapePreset.L_SHAPE to "L 形",
                                RoomShapePreset.T_SHAPE to "T 形",
                                RoomShapePreset.U_SHAPE to "U 形"
                        )

                presets.forEach { (preset, label) ->
                    val isSelected = roomBoundary.preset == preset
                    OutlinedButton(
                            onClick = { pendingConfirmPreset = preset to label },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            colors =
                                    if (isSelected)
                                            ButtonDefaults.outlinedButtonColors(
                                                    containerColor =
                                                            MaterialTheme.colorScheme.primaryContainer
                                            )
                                    else ButtonDefaults.outlinedButtonColors()
                    ) { Text(label, color = Color.Black) }
                }

                // --- Saved custom boundaries ---
                if (savedBoundaries.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                            "已儲存的自訂邊界",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(savedBoundaries) { saved ->
                            val isSelected = roomBoundary.savedBoundaryId == saved.id
                            Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                        onClick = { pendingSavedBoundary = saved },
                                        modifier = Modifier.weight(1f),
                                        colors =
                                                if (isSelected)
                                                        ButtonDefaults.outlinedButtonColors(
                                                                containerColor =
                                                                        MaterialTheme.colorScheme.primaryContainer
                                                        )
                                                else ButtonDefaults.outlinedButtonColors()
                                ) { Text(saved.name, color = Color.Black) }
                                // Rename button
                                IconButton(
                                        onClick = {
                                            renamingBoundary = saved
                                            renameText = saved.name
                                        }
                                ) { Text("✏️", style = MaterialTheme.typography.bodyMedium) }
                                // Delete button (with confirmation)
                                IconButton(
                                        onClick = { deletingBoundary = saved }
                                ) { Text("🗑️", style = MaterialTheme.typography.bodyMedium) }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                        onClick = {
                            showBoundarySheet = false
                            isDrawingBoundary = true
                            drawingVertices = emptyList()
                        },
                        modifier = Modifier.fillMaxWidth()
                ) { Text("自訂繪製") }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // --- Naming dialog for new custom boundary ---
    if (showNameDialog) {
        var nameText by remember { mutableStateOf("自訂邊界 ${savedBoundaries.size + 1}") }
        AlertDialog(
                onDismissRequest = { showNameDialog = false },
                title = { Text("為邊界命名") },
                text = {
                    OutlinedTextField(
                            value = nameText,
                            onValueChange = { nameText = it },
                            label = { Text("邊界名稱") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                val saved =
                                        SavedBoundary(
                                                name = nameText,
                                                vertices = pendingBoundaryVertices
                                        )
                                mapViewModel.addSavedBoundary(saved)
                                mapViewModel.setRoomBoundary(
                                        RoomBoundary(
                                                preset = RoomShapePreset.CUSTOM,
                                                vertices = pendingBoundaryVertices,
                                                savedBoundaryId = saved.id
                                        )
                                )
                                mapViewModel.relocateObjectsIntoBoundary(pendingBoundaryVertices)
                                showNameDialog = false
                                pendingBoundaryVertices = emptyList()
                            }
                    ) { Text("儲存") }
                },
                dismissButton = {
                    TextButton(
                            onClick = {
                                showNameDialog = false
                                pendingBoundaryVertices = emptyList()
                            }
                    ) { Text("取消") }
                }
        )
    }

    // --- Rename dialog ---
    if (renamingBoundary != null) {
        AlertDialog(
                onDismissRequest = { renamingBoundary = null },
                title = { Text("重新命名邊界") },
                text = {
                    OutlinedTextField(
                            value = renameText,
                            onValueChange = { renameText = it },
                            label = { Text("新名稱") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                            onClick = {
                                mapViewModel.renameSavedBoundary(renamingBoundary!!.id, renameText)
                                renamingBoundary = null
                            }
                    ) { Text("確認") }
                },
                dismissButton = { TextButton(onClick = { renamingBoundary = null }) { Text("取消") } }
        )
    }

    if (showFurnitureSheet && selectedFurniture != null) {
        FurnitureDetailSheet(
            furniture = selectedFurniture!!,
            items = searchItems.filter { it.linkedFurnitureId == selectedFurniture!!.id },
            onDismiss = {
                showFurnitureSheet = false
                selectedFurniture = null
            },
            onAddItem = {
                preLinkFurnitureId = selectedFurniture!!.id
                showFurnitureSheet = false
                selectedFurniture = null
                showHistory = false
                showSearchSheet = true
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FurnitureDetailSheet(
    furniture: MapObject,
    items: List<MissingItem>,
    onDismiss: () -> Unit,
    onAddItem: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(64.dp).background(Color(0xFFE8EAF6), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(furniture.type.emoji(), fontSize = 32.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(furniture.type.chineseName(), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("放置於此的物品 (${items.size})", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onAddItem) {
                    Text("+ 新增物品")
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            if (items.isEmpty()) {
                Text("目前沒有物品關聯至此家具。", color = Color.Gray, modifier = Modifier.padding(32.dp))
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
                    items(items) { item ->
                        ListItem(
                            headlineContent = { Text(item.name) },
                            supportingContent = { Text(item.lastKnownLocationDesc) },
                            leadingContent = {
                                Box(
                                    modifier = Modifier.size(40.dp).background(Color(0xFFF5F5F5), androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item.photoPath != null) {
                                        val photoBmp = remember(item.photoPath) {
                                            BitmapFactory.decodeFile(item.photoPath)
                                        }
                                        photoBmp?.let { bmp ->
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize().clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    } else {
                                        Text(furniture.type.emoji())
                                    }
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun LegendRow(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Text(text, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariantColor)
    }
}

fun labelToChineseName(label: String): String {
    return when (label) {
        // Just return the string directly since Gemini now outputs Chinese
        else -> label
    }
}

@Composable
fun MapCanvas(
        mapObjects: List<MapObject>,
        predictedSpots: List<Pair<Float, Float>>,
        storedItems: List<MissingItem> = emptyList(),
        walkPath: List<Pair<Float, Float>> = emptyList(),
        isEditing: Boolean,
        mapScale: Float,
        onScaleChange: (Float) -> Unit,
        mapOffset: Offset,
        onOffsetChange: (Offset) -> Unit,
        onCanvasSize: (androidx.compose.ui.unit.IntSize) -> Unit,
        boundaryVertices: List<PointF>,
        isDrawingBoundary: Boolean,
        drawingVertices: List<PointF>,
        isSpecifyingLocation: Boolean,
        isRecordingWalkPath: Boolean = false,
        specifiedLocation: PointF?,
        onCanvasTap: (PointF) -> Unit,
        onFurnitureTap: (MapObject) -> Unit = {},
        onDrawingVertexMove: (Int, PointF) -> Unit,
        onObjectMove: (String, Float, Float) -> Unit,
        onObjectRotate: (String) -> Unit,
        onDeleteObject: (String) -> Unit,
        onObjectScaleChange: (String, Float) -> Unit,
        gridEnabled: Boolean
) {
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    var selectedObjectId by remember { mutableStateOf<String?>(null) }

    // If edit mode ends, clear selection
    LaunchedEffect(isEditing) {
        if (!isEditing) {
            selectedObjectId = null
        }
    }

    val currentMapScale by rememberUpdatedState(mapScale)
    val currentMapOffset by rememberUpdatedState(mapOffset)
    val currentOnScaleChange by rememberUpdatedState(onScaleChange)
    val currentOnOffsetChange by rememberUpdatedState(onOffsetChange)

    Box(
            modifier =
                    Modifier.fillMaxSize()
                            .onSizeChanged {
                                canvasSize = it
                                onCanvasSize(it)
                            }
                            .pointerInput(Unit) { // Always allow pan/zoom
                                detectTransformGestures { _, pan, zoom, _ ->
                                    currentOnScaleChange(
                                            (currentMapScale * zoom).coerceIn(0.5f, 3f)
                                    )
                                    currentOnOffsetChange(currentMapOffset + pan)
                                }
                            }
                            .pointerInput(isDrawingBoundary, isSpecifyingLocation, isRecordingWalkPath) {
                                detectTapGestures(
                                        onTap = { tapOffset ->
                                            // Convert screen tap to map virtual dp coordinates
                                            val dens = density.toFloat()
                                            val centerX = size.width / 2f
                                            val centerY = size.height / 2f
                                            val mapPxX =
                                                    (tapOffset.x -
                                                            currentMapOffset.x -
                                                            centerX) / currentMapScale + centerX
                                            val mapPxY =
                                                    (tapOffset.y -
                                                            currentMapOffset.y -
                                                            centerY) / currentMapScale + centerY
                                            val virtualX = mapPxX / dens
                                            val virtualY = mapPxY / dens

                                            if (isDrawingBoundary || isSpecifyingLocation || isRecordingWalkPath) {
                                                onCanvasTap(PointF(virtualX, virtualY))
                                            } else if (!isEditing) {
                                                // Check if a furniture object was tapped
                                                val tappedObj = mapObjects.find { obj ->
                                                    val objW = obj.width * obj.scale
                                                    val objH = obj.height * obj.scale
                                                    virtualX >= obj.x && virtualX <= obj.x + objW &&
                                                            virtualY >= obj.y && virtualY <= obj.y + objH
                                                }
                                                if (tappedObj != null) {
                                                    onFurnitureTap(tappedObj)
                                                } else {
                                                    selectedObjectId = null
                                                }
                                            } else {
                                                selectedObjectId = null
                                            }
                                        }
                                )
                            }
    ) {
        val density = LocalDensity.current.density
        val effectiveBoundary =
                if (boundaryVertices.isNotEmpty()) boundaryVertices
                else {
                    val pad = 32f
                    val cw = canvasSize.width / density
                    val ch = canvasSize.height / density
                    listOf(
                            PointF(pad, pad),
                            PointF(cw - pad, pad),
                            PointF(cw - pad, ch - pad),
                            PointF(pad, ch - pad)
                    )
                }

        Box(
                modifier =
                        Modifier.fillMaxSize()
                                .graphicsLayer(
                                        scaleX = mapScale,
                                        scaleY = mapScale,
                                        translationX = mapOffset.x,
                                        translationY = mapOffset.y
                                )
        ) {
            if (gridEnabled) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridSpacing = 20f * density
                    val gridColor = Color(0xFFDDDDDD)

                    // Calculate visible range in map space
                    // mapOffset is screen-space translation.
                    // centerX/Y is scaling origin.
                    val dens = density
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f

                    // Visible range calculation (DP)
                    val leftDp = (-mapOffset.x - centerX) / mapScale / dens + centerX / dens
                    val topDp = (-mapOffset.y - centerY) / mapScale / dens + centerY / dens
                    val rightDp = leftDp + (size.width / mapScale / dens)
                    val bottomDp = topDp + (size.height / mapScale / dens)

                    // Add some buffer
                    val buffer = gridSpacing / dens
                    val startX =
                            (kotlin.math.floor(leftDp / (gridSpacing / dens)) *
                                            (gridSpacing / dens))
                                    .toFloat()
                    val endX = rightDp + buffer
                    val startY =
                            (kotlin.math.floor(topDp / (gridSpacing / dens)) * (gridSpacing / dens))
                                    .toFloat()
                    val endY = bottomDp + buffer

                    var gx = startX
                    while (gx <= endX) {
                        drawLine(
                                gridColor,
                                Offset(gx * dens, topDp * dens),
                                Offset(gx * dens, bottomDp * dens),
                                strokeWidth = 1f / mapScale
                        )
                        gx += gridSpacing / dens
                    }
                    var gy = startY
                    while (gy <= endY) {
                        drawLine(
                                gridColor,
                                Offset(leftDp * dens, gy * dens),
                                Offset(rightDp * dens, gy * dens),
                                strokeWidth = 1f / mapScale
                        )
                        gy += gridSpacing / dens
                    }
                }
            }
            // Boundary Polygon
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dens = density
                val verts = effectiveBoundary
                if (verts.size >= 2) {
                    val path = Path()
                    path.moveTo(verts[0].x * dens, verts[0].y * dens)
                    for (i in 1 until verts.size) {
                        path.lineTo(verts[i].x * dens, verts[i].y * dens)
                    }
                    path.close()
                    drawPath(path, color = Color.Black, style = Stroke(width = 2f * dens))
                }
            }

            // Drawing preview — show in-progress vertices and lines
            if (isDrawingBoundary && drawingVertices.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val dens = density
                    // Draw lines
                    val path = Path()
                    path.moveTo(drawingVertices[0].x * dens, drawingVertices[0].y * dens)
                    for (i in 1 until drawingVertices.size) {
                        path.lineTo(drawingVertices[i].x * dens, drawingVertices[i].y * dens)
                    }
                    drawPath(
                            path,
                            color = Color.Red.copy(alpha = 0.8f),
                            style = Stroke(width = 2f * dens)
                    )
                    // Draw dashed close line preview
                    if (drawingVertices.size >= 3) {
                        drawLine(
                                color = Color.Red.copy(alpha = 0.4f),
                                start =
                                        Offset(
                                                drawingVertices.last().x * dens,
                                                drawingVertices.last().y * dens
                                        ),
                                end =
                                        Offset(
                                                drawingVertices[0].x * dens,
                                                drawingVertices[0].y * dens
                                        ),
                                strokeWidth = 1.5f * dens
                        )
                    }
                }
            }

            // Drawing mode: draggable vertex handles on top of the canvas
            if (isDrawingBoundary) {
                val dens = density
                drawingVertices.forEachIndexed { idx, v ->
                    val handlePxX = v.x * dens
                    val handlePxY = v.y * dens
                    // Render each vertex as a draggable Box overlay
                    Box(
                        modifier = Modifier
                            .offset {
                                androidx.compose.ui.unit.IntOffset(
                                    (handlePxX - 20f).toInt(),
                                    (handlePxY - 20f).toInt()
                                )
                            }
                            .size(40.dp)
                            .background(
                                if (idx == drawingVertices.lastIndex) Color(0xFFFF5722) else Color.Red,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .pointerInput(idx) {
                                detectDragGestures { change, _ ->
                                    change.consume()
                                    val cX = canvasSize.width / 2f
                                    val cY = canvasSize.height / 2f
                                    val rawPxX = (change.position.x + handlePxX - 20f)
                                    val rawPxY = (change.position.y + handlePxY - 20f)
                                    val mapPxX2 = (rawPxX - currentMapOffset.x - cX) / currentMapScale + cX
                                    val mapPxY2 = (rawPxY - currentMapOffset.y - cY) / currentMapScale + cY
                                    val targetX = mapPxX2 / dens
                                    val targetY = mapPxY2 / dens
                                    // Snap orthogonally from previous vertex
                                    val snapped = if (idx > 0) {
                                        val prev = drawingVertices[idx - 1]
                                        val ddx = kotlin.math.abs(targetX - prev.x)
                                        val ddy = kotlin.math.abs(targetY - prev.y)
                                        if (ddx > ddy) PointF(targetX, prev.y)
                                        else PointF(prev.x, targetY)
                                    } else PointF(targetX, targetY)
                                    onDrawingVertexMove(idx, snapped)
                                }
                            }
                    )
                }
            }

            mapObjects.forEach { obj ->
                MapObjectView(
                        obj = obj,
                        isEditing = isEditing,
                        isSelected = (selectedObjectId == obj.id),
                        onClick = { if (isEditing) selectedObjectId = obj.id },
                        onDelete = {
                            onDeleteObject(obj.id)
                            selectedObjectId = null
                        },
                        onRotate = { onObjectRotate(obj.id) },
                        onDrag = { vDx: Float, vDy: Float, sDx: Float, sDy: Float ->
                            val objWidth = obj.width * obj.scale
                            val objHeight = obj.height * obj.scale

                            // Convert cumulative virtual delta to target position (finger-tracking)
                            val idealX = obj.x + (vDx / density)
                            val idealY = obj.y + (vDy / density)

                            fun isPosValid(px: Float, py: Float): Boolean {
                                if (!BoundaryUtils.rectInPolygon(px, py, objWidth, objHeight, effectiveBoundary))
                                    return false
                                return mapObjects.none { other ->
                                    val otherW = other.width * other.scale
                                    val otherH = other.height * other.scale
                                    other.id != obj.id &&
                                            px < other.x + otherW &&
                                            px + objWidth > other.x &&
                                            py < other.y + otherH &&
                                            py + objHeight > other.y
                                }
                            }

                            // --- PRIORITY: Teleport to finger position if the spot is clear ---
                            if (isPosValid(idealX, idealY)) {
                                if (idealX != obj.x || idealY != obj.y) {
                                    onObjectMove(obj.id, idealX - obj.x, idealY - obj.y)
                                }
                            } else {
                                // Finger position is blocked: fall back to incremental sliding per axis
                                val stepXDp = sDx / density
                                val stepYDp = sDy / density

                                // --- Try moving along X ---
                                var targetX = obj.x + stepXDp
                                var canMoveX = true

                                if (!isPosValid(targetX, obj.y)) {
                                    val collidingX = mapObjects.find { other ->
                                        val otherW = other.width * other.scale
                                        val otherH = other.height * other.scale
                                        other.id != obj.id &&
                                                targetX < other.x + otherW &&
                                                targetX + objWidth > other.x &&
                                                obj.y < other.y + otherH &&
                                                obj.y + objHeight > other.y
                                    }
                                    if (collidingX != null) {
                                        val snapX =
                                            if (stepXDp > 0) collidingX.x - objWidth
                                            else collidingX.x + (collidingX.width * collidingX.scale)
                                        if (isPosValid(snapX, obj.y)) targetX = snapX
                                        else canMoveX = false
                                    } else {
                                        // Blocked by boundary — binary search to snap to edge
                                        var low = 0f
                                        var high = stepXDp
                                        for (i in 0..5) {
                                            val mid = (low + high) / 2
                                            if (isPosValid(obj.x + mid, obj.y)) low = mid else high = mid
                                        }
                                        targetX = obj.x + low
                                        if (targetX == obj.x) canMoveX = false
                                    }
                                }
                                val finalX = if (canMoveX) targetX else obj.x

                                // --- Try moving along Y ---
                                var targetY = obj.y + stepYDp
                                var canMoveY = true

                                if (!isPosValid(finalX, targetY)) {
                                    val collidingY = mapObjects.find { other ->
                                        val otherW = other.width * other.scale
                                        val otherH = other.height * other.scale
                                        other.id != obj.id &&
                                                finalX < other.x + otherW &&
                                                finalX + objWidth > other.x &&
                                                targetY < other.y + otherH &&
                                                targetY + objHeight > other.y
                                    }
                                    if (collidingY != null) {
                                        val snapY =
                                            if (stepYDp > 0) collidingY.y - objHeight
                                            else collidingY.y + (collidingY.height * collidingY.scale)
                                        if (isPosValid(finalX, snapY)) targetY = snapY
                                        else canMoveY = false
                                    } else {
                                        // Blocked by boundary — binary search to snap to edge
                                        var low = 0f
                                        var high = stepYDp
                                        for (i in 0..5) {
                                            val mid = (low + high) / 2
                                            if (isPosValid(finalX, obj.y + mid)) low = mid else high = mid
                                        }
                                        targetY = obj.y + low
                                        if (targetY == obj.y) canMoveY = false
                                    }
                                }
                                val finalY = if (canMoveY) targetY else obj.y

                                if (finalX != obj.x || finalY != obj.y) {
                                    onObjectMove(obj.id, finalX - obj.x, finalY - obj.y)
                                }
                            }
                        },
                        onDragEnd = { /* No-op */}
                )
            }

            // Draw Predictions, Manual Location, Walk Path, and Stored Item Pins
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dens = density

                // Walk path — yellow line + dots
                if (walkPath.size >= 2) {
                    val pathObj = Path()
                    pathObj.moveTo(walkPath[0].first * dens, walkPath[0].second * dens)
                    for (i in 1 until walkPath.size) {
                        pathObj.lineTo(walkPath[i].first * dens, walkPath[i].second * dens)
                    }
                    drawPath(pathObj, color = Color(0xFFFFD600), style = Stroke(width = 3f * dens))
                }
                walkPath.forEach { pt ->
                    drawCircle(
                            color = Color(0xFFFFD600),
                            radius = 5f * dens,
                            center = Offset(pt.first * dens, pt.second * dens)
                    )
                }

                // Stored items — green pins
                storedItems.forEach { item ->
                    if (item.manualX != null && item.manualY != null) {
                        drawCircle(
                                color = Color(0xFF4CAF50).copy(alpha = 0.7f),
                                radius = 8f * dens,
                                center = Offset(item.manualX * dens, item.manualY * dens)
                        )
                        drawCircle(
                                color = Color(0xFF4CAF50),
                                radius = 8f * dens,
                                center = Offset(item.manualX * dens, item.manualY * dens),
                                style = Stroke(width = 1.5f * dens)
                        )
                    }
                }

                // AI predictions — red pulsing circles (dp coords)
                predictedSpots.forEach { spot ->
                    drawCircle(
                            color = Color(0xFFE53935).copy(alpha = 0.4f),
                            radius = 14f * dens,
                            center = Offset(spot.first * dens, spot.second * dens)
                    )
                    drawCircle(
                            color = Color(0xFFE53935).copy(alpha = 0.7f),
                            radius = 6f * dens,
                            center = Offset(spot.first * dens, spot.second * dens)
                    )
                }

                // Manual specified location — blue pin
                specifiedLocation?.let { loc ->
                    drawCircle(
                            color = Color(0xFF1565C0).copy(alpha = 0.5f),
                            radius = 12f * dens,
                            center = Offset(loc.x * dens, loc.y * dens)
                    )
                    drawCircle(
                            color = Color(0xFF1565C0),
                            radius = 5f * dens,
                            center = Offset(loc.x * dens, loc.y * dens)
                    )
                }
            }
        }

        // Slider for scaling selected object
        if (isEditing && selectedObjectId != null) {
            val selectedObj = mapObjects.find { it.id == selectedObjectId }
            if (selectedObj != null) {
                Box(
                        modifier =
                                Modifier.align(Alignment.BottomCenter)
                                        .padding(bottom = 80.dp)
                                        .scale(0.75f)
                                        .background(
                                                Color.White.copy(alpha = 0.9f),
                                                shape = MaterialTheme.shapes.medium
                                        )
                                        .padding(16.dp)
                                        .fillMaxWidth(0.8f)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                                "調整大小比例: ${String.format("%.1f", selectedObj.scale)}x",
                                color = Color.Black
                        )
                        Slider(
                                value = selectedObj.scale,
                                onValueChange = { newScale ->
                                    onObjectScaleChange(selectedObj.id, newScale)
                                },
                                valueRange = 0.5f..2.5f,
                                steps = 19 // increments of 0.1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MapObjectVisuals(
        type: MapObjectType,
        showName: Boolean = false,
        modifier: Modifier = Modifier
) {
    val drawableRes =
            when (type) {
                MapObjectType.BED -> R.drawable.bed
                MapObjectType.DOUBLE_BED -> R.drawable.double_bed
                MapObjectType.HIGH_CABINET_L -> R.drawable.high_cabinet_l
                MapObjectType.HIGH_CABINET_S -> R.drawable.high_cabinet_s
                MapObjectType.BATHTUB -> R.drawable.bathtub
                MapObjectType.BATHROOM_SINK -> R.drawable.bathroom_sink
                MapObjectType.TOILET -> R.drawable.toilet
                MapObjectType.CHAIR_1 -> R.drawable.chair_1
                MapObjectType.CHAIR_2 -> R.drawable.chair_2
                MapObjectType.DOOR_LEFT -> R.drawable.door_left
                MapObjectType.DOOR_RIGHT -> R.drawable.door_right
                MapObjectType.DOUBLE_SOFA -> R.drawable.double_sofa
                MapObjectType.REFRIGERATOR -> R.drawable.refrigerator
                MapObjectType.TABLE_L -> R.drawable.table_l
                MapObjectType.TABLE_S -> R.drawable.table_s
                MapObjectType.TABLE_L_SHAPE -> R.drawable.table_l_shape
                MapObjectType.WINDOW -> R.drawable.window
                else -> null
            }

    val isImage = drawableRes != null

    // Select color for non-image objects
    val baseColor = if (isImage) Color.Transparent else Color.LightGray

    Box(
            modifier =
                    modifier.then(
                            if (isImage) Modifier.background(Color.Transparent)
                            else Modifier.background(baseColor).border(1.dp, Color.Black)
                    ),
            contentAlignment = Alignment.Center
    ) {
        if (showName) {
            Text(text = type.name, color = Color.Black, style = MaterialTheme.typography.labelSmall)
        } else if (drawableRes != null) {
            Image(
                    painter = painterResource(id = drawableRes),
                    contentDescription = type.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
fun MapObjectView(
        obj: MapObject,
        isEditing: Boolean,
        isSelected: Boolean,
        onClick: () -> Unit,
        onDelete: () -> Unit,
        onRotate: () -> Unit,
        onDrag: (Float, Float, Float, Float) -> Unit,
        onDragEnd: () -> Unit
) {
    val density = LocalDensity.current.density
    val currentObj by rememberUpdatedState(obj)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    Box(
            modifier =
                    Modifier.offset {
                        androidx.compose.ui.unit.IntOffset(
                                (obj.x * density).toInt(),
                                (obj.y * density).toInt()
                        )
                    }
                            .size(
                                    width = (obj.width * obj.scale).dp,
                                    height = (obj.height * obj.scale).dp
                            )
                            .run {
                                if (isEditing) {
                                    this
                                            .pointerInput(isSelected) {
                                                detectTapGestures(onTap = { onClick() })
                                            }
                                            .pointerInput(Unit) {
                                                var virtualX = 0f
                                                var virtualY = 0f
                                                detectDragGestures(
                                                        onDragStart = {
                                                            virtualX = currentObj.x * density
                                                            virtualY = currentObj.y * density
                                                        },
                                                        onDragEnd = { currentOnDragEnd() },
                                                        onDragCancel = { currentOnDragEnd() }
                                                ) { change, dragAmount ->
                                                    change.consume()
                                                    virtualX += dragAmount.x
                                                    virtualY += dragAmount.y
                                                    currentOnDrag(
                                                            virtualX - (currentObj.x * density),
                                                            virtualY - (currentObj.y * density),
                                                            dragAmount.x,
                                                            dragAmount.y
                                                    )
                                                }
                                            }
                                } else this
                            }
    ) {
        val imageWidth = if (obj.rotation % 180 != 0f) obj.height else obj.width
        val imageHeight = if (obj.rotation % 180 != 0f) obj.width else obj.height

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MapObjectVisuals(
                    type = obj.type,
                    modifier =
                            Modifier.requiredSize((imageWidth * obj.scale).dp, (imageHeight * obj.scale).dp)
                                    .rotate(obj.rotation)
            )
        }

        if (isSelected && isEditing) {
            Surface(
                    modifier =
                            Modifier.align(Alignment.TopStart)
                                    .offset(x = (-12).dp, y = (-12).dp)
                                    .size(24.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp,
                    onClick = { onDelete() }
            ) {
                Image(
                        painter = painterResource(id = R.drawable.delete),
                        contentDescription = "Delete Object",
                        modifier = Modifier.fillMaxSize().padding(4.dp)
                )
            }

            Surface(
                    modifier =
                            Modifier.align(Alignment.TopEnd)
                                    .offset(x = 12.dp, y = (-12).dp)
                                    .size(24.dp),
                    shape = androidx.compose.foundation.shape.CircleShape,
                    color = Color.White,
                    shadowElevation = 4.dp,
                    onClick = { onRotate() }
            ) {
                androidx.compose.material3.Icon(
                        painter = painterResource(id = android.R.drawable.ic_menu_rotate),
                        contentDescription = "Rotate Object",
                        modifier = Modifier.fillMaxSize().padding(4.dp),
                        tint = Color.Black
                )
            }
        }
    }
}

fun getSizeDisplay(
        category: com.example.lostfoundai.data.ItemCategory,
        size: com.example.lostfoundai.data.ItemSize
): String {
    return when (category) {
        com.example.lostfoundai.data.ItemCategory.ACCESSORY ->
                when (size) {
                    com.example.lostfoundai.data.ItemSize.VERY_SMALL -> "極小 (< 2cm)"
                    com.example.lostfoundai.data.ItemSize.SMALL -> "小 (2-5cm)"
                    com.example.lostfoundai.data.ItemSize.MEDIUM -> "中 (5-10cm)"
                    else -> "中 (5-10cm)"
                }
        com.example.lostfoundai.data.ItemCategory.BELONGINGS ->
                when (size) {
                    com.example.lostfoundai.data.ItemSize.SMALL -> "小 (5-15cm)"
                    com.example.lostfoundai.data.ItemSize.MEDIUM -> "中 (15-30cm)"
                    com.example.lostfoundai.data.ItemSize.LARGE -> "大 (> 30cm)"
                    else -> "小 (5-15cm)"
                }
        com.example.lostfoundai.data.ItemCategory.ELECTRONICS ->
                when (size) {
                    com.example.lostfoundai.data.ItemSize.SMALL -> "小 (手機/滑鼠)"
                    com.example.lostfoundai.data.ItemSize.MEDIUM -> "中 (平板/鍵盤)"
                    com.example.lostfoundai.data.ItemSize.LARGE -> "大 (筆電/螢幕)"
                    else -> "小 (手機/滑鼠)"
                }
        com.example.lostfoundai.data.ItemCategory.PAPER ->
                when (size) {
                    com.example.lostfoundai.data.ItemSize.SMALL -> "小 (名片/票據)"
                    com.example.lostfoundai.data.ItemSize.MEDIUM -> "中 (A5/筆記本)"
                    com.example.lostfoundai.data.ItemSize.LARGE -> "大 (A4/文件夾)"
                    else -> "小 (名片/票據)"
                }
        com.example.lostfoundai.data.ItemCategory.BATHROOM ->
                when (size) {
                    com.example.lostfoundai.data.ItemSize.SMALL -> "小 (牙刷/刮鬍刀)"
                    com.example.lostfoundai.data.ItemSize.MEDIUM -> "中 (瓶罐/毛巾)"
                    com.example.lostfoundai.data.ItemSize.LARGE -> "大 (浴巾/臉盆)"
                    else -> "小 (牙刷/刮鬍刀)"
                }
        com.example.lostfoundai.data.ItemCategory.APPLIANCE ->
                when (size) {
                    com.example.lostfoundai.data.ItemSize.SMALL -> "小 (遙控器/線材)"
                    com.example.lostfoundai.data.ItemSize.MEDIUM -> "中 (吹風機/快煮壺)"
                    com.example.lostfoundai.data.ItemSize.LARGE -> "大 (風扇/吸塵器)"
                    else -> "小 (遙控器/線材)"
                }
        com.example.lostfoundai.data.ItemCategory.CLOTHING ->
                when (size) {
                    com.example.lostfoundai.data.ItemSize.SMALL -> "小 (襪子/內衣)"
                    com.example.lostfoundai.data.ItemSize.MEDIUM -> "中 (上衣/褲子)"
                    com.example.lostfoundai.data.ItemSize.LARGE -> "大 (外套/大衣)"
                    else -> "小 (襪子/內衣)"
                }
        else ->
                when (size) {
                    com.example.lostfoundai.data.ItemSize.VERY_SMALL -> "極小"
                    com.example.lostfoundai.data.ItemSize.SMALL -> "小"
                    com.example.lostfoundai.data.ItemSize.MEDIUM -> "中"
                    com.example.lostfoundai.data.ItemSize.LARGE -> "大"
                }
    }
}
