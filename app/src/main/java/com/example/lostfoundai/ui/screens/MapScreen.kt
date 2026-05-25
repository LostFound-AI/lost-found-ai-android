package com.example.lostfoundai.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
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
import com.example.lostfoundai.data.PredictionResult
import com.example.lostfoundai.data.SavedBoundary
import com.example.lostfoundai.data.chineseName
import com.example.lostfoundai.data.defaultHeight
import com.example.lostfoundai.data.defaultWidth
import com.example.lostfoundai.data.emoji
import com.example.lostfoundai.data.getDefaultDimensions
import com.example.lostfoundai.ui.components.Toolbar
import com.example.lostfoundai.ui.components.KeyboardAccessoryProvider
import com.example.lostfoundai.ui.components.AccessoryOutlinedTextField
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
    val predictionResult by mapViewModel.predictionResult.collectAsState()
    val isPredicting by mapViewModel.isPredicting.collectAsState()
    val searchItems by searchViewModel.filteredItems.collectAsState(initial = emptyList())
    val roomBoundary by mapViewModel.roomBoundary.collectAsState()
    val savedBoundaries by mapViewModel.savedBoundaries.collectAsState()
    val gridEnabled by mapViewModel.gridEnabled.collectAsState()
    val walkPath by mapViewModel.walkPath.collectAsState()
    val isRecordingWalkPath by mapViewModel.isRecordingWalkPath.collectAsState()
    val isWalkPathVisible by mapViewModel.isWalkPathVisible.collectAsState()

    var isEditing by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(true) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showBoundarySheet by remember { mutableStateOf(false) }
    var isDrawingBoundary by remember { mutableStateOf(false) }
    // baselineBoundaryVertices: 進入微調模式時記下的原始邊界，作為常駐底圖參考
    var baselineBoundaryVertices by remember { mutableStateOf<List<PointF>?>(null) }
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
    var pendingSizeIndex by remember { mutableStateOf<Int?>(null) }

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
        if (pendingSizeIndex != null) {
            val idx = pendingSizeIndex!!.coerceIn(0, sizes.size - 1)
            selectedSize = sizes[idx]
            pendingSizeIndex = null
        } else if (selectedSize !in sizes) {
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

    val currentDrawingVertices by androidx.compose.runtime.rememberUpdatedState(drawingVertices)
    val currentIsDrawingBoundary by androidx.compose.runtime.rememberUpdatedState(isDrawingBoundary)
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            if (currentIsDrawingBoundary && currentDrawingVertices.isNotEmpty()) {
                mapViewModel.setRoomBoundary(
                    com.example.lostfoundai.data.RoomBoundary(
                        preset = com.example.lostfoundai.data.RoomShapePreset.CUSTOM,
                        vertices = currentDrawingVertices
                    )
                )
            }
            mapViewModel.clearPredictedSpots()
        }
    }
    var mapOffset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    var hasCenteredBoundary by remember(roomBoundary.savedBoundaryId) { mutableStateOf(false) }

    LaunchedEffect(isRecordingWalkPath) {
        if (isRecordingWalkPath && !isWalkPathVisible) {
            mapViewModel.toggleWalkPathVisibility()
        }
    }

    LaunchedEffect(roomBoundary, canvasSize) {
        if (!hasCenteredBoundary && canvasSize != androidx.compose.ui.unit.IntSize.Zero && roomBoundary.vertices.isNotEmpty()) {
            val verts = roomBoundary.vertices
            val minX = verts.minOf { it.x }
            val maxX = verts.maxOf { it.x }
            val minY = verts.minOf { it.y }
            val maxY = verts.maxOf { it.y }

            val boundaryWidthDp = maxX - minX
            val boundaryHeightDp = maxY - minY
            val bCenterX_dp = (minX + maxX) / 2
            val bCenterY_dp = (minY + maxY) / 2

            val canvasWidthDp = canvasSize.width / density
            val canvasHeightDp = canvasSize.height / density

            val scaleX = if (boundaryWidthDp > 0) canvasWidthDp / boundaryWidthDp else 1f
            val scaleY = if (boundaryHeightDp > 0) canvasHeightDp / boundaryHeightDp else 1f
            
            val targetScale = kotlin.math.min(scaleX, scaleY) * 0.8f
            mapScale = targetScale.coerceIn(0.5f, 3f)

            val bCenterX_px = bCenterX_dp * density
            val bCenterY_px = bCenterY_dp * density
            val centerX_px = canvasSize.width / 2f
            val centerY_px = canvasSize.height / 2f
            
            mapOffset = Offset(
                (centerX_px - bCenterX_px) * mapScale,
                (centerY_px - bCenterY_px) * mapScale
            )
            hasCenteredBoundary = true
        }
    }

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
                                "請辨識圖片中的主要物品名稱、類別與相對大小，以JSON格式回應，格式如下：\n" +
                                "{\"name\":\"物品名稱，用繁體中文2-5個字\",\"category\":\"類別\",\"size\":0}\n" +
                                "category 必須是以下其中一個：飾品、隨身物品、電子產品、紙本、衛浴用品、家電配件、衣物\n" +
                                "size 必須是整數：0 (小), 1 (中), 2 (大)。請依據物品在該類別中的常見相對大小判斷。"
                            )
                        }
                    )
                    val raw = response.text ?: return@launch
                    val jsonStr = Regex("""\{[^}]+\}""").find(raw)?.value ?: return@launch
                    val json = org.json.JSONObject(jsonStr)
                    val detectedName = json.optString("name", "")
                    val detectedCat = json.optString("category", "其他")
                    val detectedSize = if (json.has("size")) json.optInt("size", 0) else null
                    isAnalyzingPhoto = false
                    if (detectedName.isNotEmpty()) {
                        detectedChips = listOf(detectedName)
                        if (itemName.isEmpty()) itemName = detectedName
                    }
                    if (detectedCat in categories) {
                        selectedCategory = detectedCat
                        Log.d("MapScreen", "目前的物品類別是: $detectedCat")
                        if (detectedSize != null) {
                            pendingSizeIndex = detectedSize
                            Log.d("MapScreen", "目前的物品大小是: $detectedSize")
                        }
                    }
                } catch (e: Exception) {
                    isAnalyzingPhoto = false
                    Log.e("MapScreen", "Gemini analysis failed", e)
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
                    Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("顯示行走路線")
                        Switch(
                                checked = isWalkPathVisible,
                                onCheckedChange = { mapViewModel.toggleWalkPathVisibility() }
                        )
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
                            onBoundaryVertexMove = { idx, newPos ->
                                val verts = drawingVertices.toMutableList()
                                val old = verts[idx]
                                verts[idx] = newPos // 更新當前拖曳的頂點
                                
                                val prevIdx = (idx - 1 + verts.size) % verts.size
                                val nextIdx = (idx + 1) % verts.size
                                
                                // 【推導邏輯】
                                // 為了維持相鄰線段的「正交(Orthogonal)」特性：
                                // 若原本「前一個點」到「當前點」是水平線 (Y座標相同)，則拖曳當前點後，前一個點必須跟著改變 Y 座標才能維持水平。
                                // 若原本是垂直線 (X座標相同)，前一個點必須跟著改變 X 座標。
                                if (verts[prevIdx].y == old.y) verts[prevIdx] = PointF(verts[prevIdx].x, newPos.y)
                                else verts[prevIdx] = PointF(newPos.x, verts[prevIdx].y)
                                
                                // 同理應用於「下一個點」：
                                if (verts[nextIdx].x == old.x) verts[nextIdx] = PointF(newPos.x, verts[nextIdx].y)
                                else verts[nextIdx] = PointF(verts[nextIdx].x, newPos.y)
                                
                                // 【需求一】移除因頂點移動產生的共線節點
                                drawingVertices = removeCollinearPoints(verts)
                            },
                            onBoundaryMidpointMove = { idx, newPos ->
                                val verts = drawingVertices.toMutableList()
                                val nextIdx = (idx + 1) % verts.size
                                val p1 = verts[idx]
                                val p2 = verts[nextIdx]
                                val isHoriz = p1.y == p2.y
                                
                                // 【推導邏輯】
                                // 拖曳中點代表整條線段的平移。
                                // 若線段為水平 (Y座標相同)，則拖曳時只允許改變 Y 軸（上下平移）。因此我們只取 newPos.y 更新兩端點。
                                // 若線段為垂直 (X座標相同)，則拖曳時只允許改變 X 軸（左右平移）。我們只取 newPos.x 更新兩端點。
                                if (isHoriz) {
                                    verts[idx] = PointF(p1.x, newPos.y)
                                    verts[nextIdx] = PointF(p2.x, newPos.y)
                                } else {
                                    verts[idx] = PointF(newPos.x, p1.y)
                                    verts[nextIdx] = PointF(newPos.x, p2.y)
                                }
                                // 【需求一】移除因線段平移產生的共線節點
                                drawingVertices = removeCollinearPoints(verts)
                            },
                            mapObjects = mapObjects,
                            predictionResult = predictionResult,
                            storedItems = searchItems,
                            walkPath = if (isWalkPathVisible) walkPath else emptyList(),
                            isEditing = isEditing,
                            mapScale = mapScale,
                            onScaleChange = { mapScale = it },
                            mapOffset = mapOffset,
                            onOffsetChange = { mapOffset = it },
                            onCanvasSize = { canvasSize = it },
                            boundaryVertices = roomBoundary.vertices,
                            isDrawingBoundary = isDrawingBoundary,
                            baselineBoundaryVertices = baselineBoundaryVertices,
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
                if (isEditing && !isDrawingBoundary && !isRecordingWalkPath && !showBoundarySheet) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(bottom = 100.dp)
                    ) {
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

                // Overlay for AI Prediction Loading
                if (isPredicting) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.6f))
                            .pointerInput(Unit) {
                                detectTapGestures { } // Block interactions
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "正在尋找...",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
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
                                onClick = {
                                    if (drawingVertices.isNotEmpty()) {
                                        drawingVertices = drawingVertices.dropLast(1)
                                    }
                                },
                                enabled = drawingVertices.isNotEmpty(),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
                            ) { Text("上一步") }
                            OutlinedButton(
                                onClick = { drawingVertices = emptyList() },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
                            ) { Text("清除重畫") }
                            Button(
                                    onClick = {
                                        if (drawingVertices.size >= 3) {
                                            val first = drawingVertices.first()
                                            var finalVertices = drawingVertices.toMutableList()
                                            val currentLast = finalVertices.last()
                                            if (finalVertices.size >= 2) {
                                                val secondLast = finalVertices[finalVertices.size - 2]
                                                if (secondLast.x == currentLast.x) {
                                                    finalVertices[finalVertices.size - 1] = PointF(currentLast.x, first.y)
                                                } else {
                                                    finalVertices[finalVertices.size - 1] = PointF(first.x, currentLast.y)
                                                }
                                            }
                                            // 【需求一】移除共線多餘節點
                                            finalVertices = removeCollinearPoints(finalVertices).toMutableList()

                                            if (baselineBoundaryVertices != null) {
                                                // ── 微調完成 ──────────────────────────────────────
                                                val savedId = roomBoundary.savedBoundaryId
                                                if (savedId != null) {
                                                    // 【需求三】自訂邊界：原地更新
                                                    mapViewModel.updateSavedBoundary(savedId, finalVertices)
                                                    mapViewModel.setRoomBoundary(
                                                        RoomBoundary(
                                                            preset = RoomShapePreset.CUSTOM,
                                                            vertices = finalVertices,
                                                            savedBoundaryId = savedId
                                                        )
                                                    )
                                                } else {
                                                    // 【需求四】預設邊界：另存為新的自訂邊界，觸發命名對話框
                                                    pendingBoundaryVertices = finalVertices
                                                    showNameDialog = true
                                                }
                                                mapViewModel.relocateObjectsIntoBoundary(finalVertices)
                                            } else {
                                                // ── 全新繪製完成：一般存檔流程 ───────────────
                                                pendingBoundaryVertices = finalVertices
                                                showNameDialog = true
                                            }
                                        }
                                        isDrawingBoundary = false
                                        drawingVertices = emptyList()
                                        baselineBoundaryVertices = null
                                    },
                                    enabled = drawingVertices.size >= 3,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
                            ) {
                                val label = if (baselineBoundaryVertices != null) "完成微調" else "完成 (${drawingVertices.size} 個頂點)"
                                Text(label)
                            }
                        }
                    }
                    Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp),
                            color = Color(0xFF546E7A)
                    ) {
                        TextButton(onClick = {
                            isDrawingBoundary = false
                            drawingVertices = emptyList()
                            baselineBoundaryVertices = null  // 清除底圖
                        }) {
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
                            OutlinedButton(
                                onClick = { mapViewModel.toggleWalkPathRecording() },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
                            ) { Text("路線編輯") }
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
                                    "路線編輯中 (${walkPath.size})",
                                    color = Color(0xFF1A1C2E),
                                    style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(
                                onClick = { mapViewModel.clearWalkPath() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp)
                            ) { Text("清除路線", color = Color(0xFF1A1C2E)) }
                            Button(
                                onClick = { mapViewModel.toggleWalkPathRecording() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1A1C2E))
                            ) { Text("完成", color = Color.White) }
                        }
                    }
                }

                // Map legend (bottom-right, normal view only)
                if (!isDrawingBoundary && !isEditing &&
                        (predictionResult.points.isNotEmpty() || predictionResult.lines.isNotEmpty() || walkPath.isNotEmpty() || searchItems.any { it.manualX != null })) {
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
                            if (predictionResult.points.isNotEmpty() || predictionResult.lines.isNotEmpty()) LegendRow(Color(0xFFE53935), "AI 預測")
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

                // Center Top Prediction Title
                if (predictionResult.itemId.isNotEmpty()) {
                    val predictedItem = searchItems.find { it.id == predictionResult.itemId }
                    if (predictedItem != null) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 16.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                            color = Color.White,
                            shadowElevation = 4.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryIndigo, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("尋找: ${predictedItem.name}", style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
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
                            if (predictionResult.points.isNotEmpty() || predictionResult.lines.isNotEmpty() || specifiedLocation != null) {
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
                    modifier = Modifier.padding(top = 64.dp),
                    sheetState = sheetState,
                    containerColor = Color.White,
                    dragHandle = null
            ) {
                KeyboardAccessoryProvider {
                    Column(
                            modifier =
                                    Modifier.fillMaxWidth()
                                            .padding(horizontal = 24.dp, vertical = 16.dp)
                    ) {
                        // 自訂拖曳把手區塊 (Drag Handle)
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .background(Color.LightGray, CircleShape)
                                .align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    if (showHistory) {
                        // ... History View ...
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                    "尋找紀錄",
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

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                        ) {
                            items(searchItems) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "新增物品",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.Black
                            )
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
                            ) { Text("尋找紀錄", color = Color.White) }
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

                        AccessoryOutlinedTextField(
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
                            AccessoryOutlinedTextField(
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
                            AccessoryOutlinedTextField(
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
                            AccessoryOutlinedTextField(
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
                            AccessoryOutlinedTextField(
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

                                    var triggerAIPredictionForId: String? = null
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
                                        if (specifiedLocation == null) triggerAIPredictionForId = editingItem!!.id
                                    } else {
                                        val newId = searchViewModel.addMissingItem(
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
                                        if (specifiedLocation == null) triggerAIPredictionForId = newId
                                    }
                                    editingItem = null
                                    specifiedLocation = null
                                    itemName = ""
                                    capturedPhotoPath = null
                                    detectedChips = emptyList()
                                    preLinkFurnitureId = null
                                    
                                    if (triggerAIPredictionForId != null) {
                                        mapViewModel.startAIPrediction(triggerAIPredictionForId)
                                        showSearchSheet = false
                                    } else {
                                        showHistory = true
                                    }
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("選擇邊界形狀", style = MaterialTheme.typography.titleLarge, color = Color.Black)
                    Button(onClick = {
                        showBoundarySheet = false
                        // 進入微調模式：用 isDrawingBoundary 合併狀態，並記錄基線底圖
                        baselineBoundaryVertices = roomBoundary.vertices
                        drawingVertices = roomBoundary.vertices
                        isDrawingBoundary = true
                    }) {
                        Text("微調目前邊界")
                    }
                }
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
                    AccessoryOutlinedTextField(
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
                    AccessoryOutlinedTextField(
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

/**
 * 【需求一】共線節點移除：
 * 若連續三個點中，中間那個點與前後點同 X（垂直共線）或同 Y（水平共線），
 * 表示中間點是多餘的，予以移除。重複遍歷直到沒有多餘點為止。
 */
fun removeCollinearPoints(vertices: List<PointF>): List<PointF> {
    if (vertices.size <= 2) return vertices
    var result = vertices.toMutableList()
    var changed = true
    while (changed) {
        changed = false
        val cleaned = mutableListOf<PointF>()
        cleaned.add(result[0])
        for (i in 1 until result.size - 1) {
            val prev = result[i - 1]
            val cur  = result[i]
            val next = result[i + 1]
            // 共線判斷：若前→當前 與 當前→後 方向相同（同軸），跳過中間點
            val isHorizPrev = prev.y == cur.y
            val isHorizNext = cur.y == next.y
            val isVertPrev  = prev.x == cur.x
            val isVertNext  = cur.x == next.x
            val isCollinear = (isHorizPrev && isHorizNext) || (isVertPrev && isVertNext)
            if (!isCollinear) cleaned.add(cur) else changed = true
        }
        cleaned.add(result.last())
        result = cleaned
    }
    return result
}

@Composable
fun MapCanvas(
        mapObjects: List<MapObject>,
        predictionResult: PredictionResult,
        storedItems: List<MissingItem> = emptyList(),
        walkPath: List<PointF> = emptyList(),
        isEditing: Boolean,
        mapScale: Float,
        onScaleChange: (Float) -> Unit,
        mapOffset: Offset,
        onOffsetChange: (Offset) -> Unit,
        onCanvasSize: (androidx.compose.ui.unit.IntSize) -> Unit,
        boundaryVertices: List<PointF>,
        isDrawingBoundary: Boolean,
        baselineBoundaryVertices: List<PointF>? = null,
        drawingVertices: List<PointF>,
        isSpecifyingLocation: Boolean,
        isRecordingWalkPath: Boolean = false,
        specifiedLocation: PointF?,
        onCanvasTap: (PointF) -> Unit,
        onFurnitureTap: (MapObject) -> Unit = {},
        onDrawingVertexMove: (Int, PointF) -> Unit,
        onBoundaryVertexMove: (Int, PointF) -> Unit = { _, _ -> },
        onBoundaryMidpointMove: (Int, PointF) -> Unit = { _, _ -> },
        onObjectMove: (String, Float, Float) -> Unit,
        onObjectRotate: (String) -> Unit,
        onDeleteObject: (String) -> Unit,
        onObjectScaleChange: (String, Float) -> Unit,
        gridEnabled: Boolean
) {
    var canvasSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }

    var selectedObjectId by remember { mutableStateOf<String?>(null) }
    // dragPreviewVertices: 拖曳中即時計算的預覽節點列表
    var dragPreviewVertices by remember { mutableStateOf<List<PointF>?>(null) }
    // isDraggingHandle: 是否正在拖曳任一控制點
    var isDraggingHandle by remember { mutableStateOf(false) }

    // 【需求二】常駐預覽：進入微調模式（有 baselineBoundaryVertices）後，
    // 若無正在進行的拖曳，預覽邊界就跟隨 drawingVertices（即最新確認後的形狀）。
    // 拖曳中則由手勢直接更新 dragPreviewVertices（每幀即時刷新）。
    LaunchedEffect(isDrawingBoundary, baselineBoundaryVertices, drawingVertices, isDraggingHandle) {
        if (isDrawingBoundary && !isDraggingHandle) {
            if (baselineBoundaryVertices != null) {
                dragPreviewVertices = drawingVertices
            } else {
                dragPreviewVertices = null
            }
        } else if (!isDrawingBoundary) {
            dragPreviewVertices = null
        }
    }

    // If edit mode ends, clear selection
    LaunchedEffect(isEditing) {
        if (!isEditing) {
            selectedObjectId = null
        }
    }

    val currentMapScale by rememberUpdatedState(mapScale)
    val currentMapOffset by rememberUpdatedState(mapOffset)
    val currentDrawingVertices by rememberUpdatedState(drawingVertices)
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
            // 【問題一修正】即時預覽：使用 dragPreviewVertices (拖曳中) 或 drawingVertices (平時)
            val activePreviewVertices = dragPreviewVertices ?: drawingVertices
            if (isDrawingBoundary && activePreviewVertices.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val dens = density
                    // Draw lines
                    val path = Path()
                    path.moveTo(activePreviewVertices[0].x * dens, activePreviewVertices[0].y * dens)
                    for (i in 1 until activePreviewVertices.size) {
                        path.lineTo(activePreviewVertices[i].x * dens, activePreviewVertices[i].y * dens)
                    }
                    drawPath(
                            path,
                            color = Color.Red.copy(alpha = 0.8f),
                            style = Stroke(width = 2f * dens)
                    )
                    // Draw dashed close line preview
                    if (activePreviewVertices.size >= 3) {
                        drawLine(
                                color = Color.Red.copy(alpha = 0.4f),
                                start =
                                        Offset(
                                                activePreviewVertices.last().x * dens,
                                                activePreviewVertices.last().y * dens
                                        ),
                                end =
                                        Offset(
                                                activePreviewVertices[0].x * dens,
                                                activePreviewVertices[0].y * dens
                                        ),
                                strokeWidth = 1.5f * dens
                        )
                    }
                }
            }

            // 常駐底圖：只要進入繪製模式且有基線，就將「修改前原始形狀」用灰色虛線常駐顯示
            if (isDrawingBoundary && baselineBoundaryVertices != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val dens = density
                    val baseline = baselineBoundaryVertices!!
                    if (baseline.size >= 2) {
                        val path = Path()
                        path.moveTo(baseline[0].x * dens, baseline[0].y * dens)
                        for (i in 1 until baseline.size) {
                            path.lineTo(baseline[i].x * dens, baseline[i].y * dens)
                        }
                        path.close()
                        drawPath(
                                path,
                                color = Color.Gray.copy(alpha = 0.35f),
                                style = Stroke(
                                        width = 2f * dens,
                                        pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                                )
                        )
                    }
                }
            }

            // Boundary Drawing mode: draggable handles on top of the canvas
            if (isDrawingBoundary) {
                val dens = density
                val targetVertices = dragPreviewVertices ?: drawingVertices
                
                targetVertices.forEachIndexed { idx, v ->
                    val handlePxX = v.x * dens
                    val handlePxY = v.y * dens
                    // Render each vertex as a draggable Box overlay
                    // 【問題二修正】編輯點置中：扣除半徑轉換為像素 (20dp * dens)
                    Box(
                        modifier = Modifier
                            .offset {
                                androidx.compose.ui.unit.IntOffset(
                                    (handlePxX - 12f * dens).toInt(),
                                    (handlePxY - 12f * dens).toInt()
                                )
                            }
                            .size(24.dp)
                            .background(
                                if (baselineBoundaryVertices == null && idx == targetVertices.lastIndex) Color(0xFFFF5722)
                                else if (baselineBoundaryVertices != null) Color(0xFF1E88E5)
                                else Color.Red,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                            .pointerInput(idx) {
                                var virtualMapX = 0f
                                var virtualMapY = 0f
                                detectDragGestures(
                                    onDragStart = {
                                        val startPos = currentDrawingVertices[idx]
                                        virtualMapX = startPos.x
                                        virtualMapY = startPos.y
                                        isDraggingHandle = true
                                        dragPreviewVertices = currentDrawingVertices
                                    },
                                    onDragEnd = {
                                        isDraggingHandle = false
                                        if (baselineBoundaryVertices != null) {
                                            // 微調模式：承認修改到 drawingVertices
                                            onBoundaryVertexMove(idx, PointF(virtualMapX, virtualMapY))
                                        } else {
                                            // 繪製模式：承認預覽修改
                                            dragPreviewVertices?.let { preview ->
                                                if (idx < preview.size) {
                                                    onDrawingVertexMove(idx, preview[idx])
                                                }
                                            }
                                        }
                                        // isDraggingHandle=false 觸發 LaunchedEffect 根據新的 drawingVertices 更新預覽
                                    },
                                    onDragCancel = {
                                        isDraggingHandle = false
                                        dragPreviewVertices = currentDrawingVertices // 回復預覽至首次狀態
                                    }
                                ) { change, dragAmount ->
                                    change.consume()
                                    // dragAmount is in raw screen pixels. Convert to map dp:
                                    virtualMapX += dragAmount.x / dens
                                    virtualMapY += dragAmount.y / dens

                                    if (baselineBoundaryVertices == null) {
                                        // For drawing mode, update directly as before (instant snap)
                                        val snapped = if (idx > 0) {
                                            val prev = currentDrawingVertices[idx - 1]
                                            val ddx = kotlin.math.abs(virtualMapX - prev.x)
                                            val ddy = kotlin.math.abs(virtualMapY - prev.y)
                                            if (ddx > ddy) PointF(virtualMapX, prev.y)
                                            else PointF(prev.x, virtualMapY)
                                        } else PointF(virtualMapX, virtualMapY)
                                        
                                        val verts = currentDrawingVertices.toMutableList()
                                        if (idx < verts.size) verts[idx] = snapped
                                        dragPreviewVertices = verts
                                    } else {
                                        // For Canva style editing, compute the orthogonal changes on the preview state
                                        val verts = currentDrawingVertices.toMutableList()
                                        val old = verts[idx]
                                        val newPos = PointF(virtualMapX, virtualMapY)
                                        verts[idx] = newPos

                                        val prevIdx = (idx - 1 + verts.size) % verts.size
                                        val nextIdx = (idx + 1) % verts.size

                                        if (verts[prevIdx].y == old.y) verts[prevIdx] = PointF(verts[prevIdx].x, newPos.y)
                                        else verts[prevIdx] = PointF(newPos.x, verts[prevIdx].y)

                                        if (verts[nextIdx].x == old.x) verts[nextIdx] = PointF(newPos.x, verts[nextIdx].y)
                                        else verts[nextIdx] = PointF(verts[nextIdx].x, newPos.y)

                                        dragPreviewVertices = verts
                                    }
                                }
                            }
                    )
                }

                // 微調模式（有基線）：顯示線段中點脇囊
                if (baselineBoundaryVertices != null && targetVertices.size >= 3) {
                    for (idx in targetVertices.indices) {
                        val p1 = targetVertices[idx]
                        val p2 = targetVertices[(idx + 1) % targetVertices.size]
                        val midX = (p1.x + p2.x) / 2f
                        val midY = (p1.y + p2.y) / 2f
                        val isHoriz = p1.y == p2.y
                        
                        val handlePxX = midX * dens
                        val handlePxY = midY * dens
                        
                        // 【問題二修正】編輯點置中：根據是水平或垂直，扣除一半寬高的像素值 (30dp/15dp * dens)
                        Box(
                            modifier = Modifier
                                .offset {
                                    androidx.compose.ui.unit.IntOffset(
                                        (handlePxX - (if (isHoriz) 30f else 15f) * dens).toInt(),
                                        (handlePxY - (if (isHoriz) 15f else 30f) * dens).toInt()
                                    )
                                }
                                .size(
                                    width = if (isHoriz) 60.dp else 30.dp,
                                    height = if (isHoriz) 30.dp else 60.dp
                                )
                                .background(Color(0xFF43A047).copy(alpha = 0.8f), shape = androidx.compose.foundation.shape.RoundedCornerShape(15.dp))
                                .pointerInput(idx) {
                                    var virtualMapX = 0f
                                    var virtualMapY = 0f
                                    detectDragGestures(
                                        onDragStart = {
                                            val p1 = currentDrawingVertices[idx]
                                            val p2 = currentDrawingVertices[(idx + 1) % currentDrawingVertices.size]
                                            virtualMapX = (p1.x + p2.x) / 2f
                                            virtualMapY = (p1.y + p2.y) / 2f
                                            isDraggingHandle = true
                                            dragPreviewVertices = currentDrawingVertices
                                        },
                                        onDragEnd = {
                                            isDraggingHandle = false
                                            onBoundaryMidpointMove(idx, PointF(virtualMapX, virtualMapY))
                                            // isDraggingHandle=false 觸發 LaunchedEffect 更新預覽
                                        },
                                        onDragCancel = {
                                            isDraggingHandle = false
                                            dragPreviewVertices = currentDrawingVertices // 回復預覽
                                        }
                                    ) { change, dragAmount ->
                                        change.consume()
                                        virtualMapX += dragAmount.x / dens
                                        virtualMapY += dragAmount.y / dens

                                        // Compute preview locally
                                        val verts = currentDrawingVertices.toMutableList()
                                        val nextIdx = (idx + 1) % verts.size
                                        val p1 = verts[idx]
                                        val p2 = verts[nextIdx]
                                        val isHoriz = p1.y == p2.y

                                        if (isHoriz) {
                                            verts[idx] = PointF(p1.x, virtualMapY)
                                            verts[nextIdx] = PointF(p2.x, virtualMapY)
                                        } else {
                                            verts[idx] = PointF(virtualMapX, p1.y)
                                            verts[nextIdx] = PointF(virtualMapX, p2.y)
                                        }
                                        dragPreviewVertices = verts
                                    }
                                }
                        )
                    }
                }
            }

            val isObjectsInteractive = isEditing && !isDrawingBoundary && !isRecordingWalkPath
            mapObjects.forEach { obj ->
                MapObjectView(
                        obj = obj,
                        isInteractive = isObjectsInteractive,
                        isSelected = (selectedObjectId == obj.id),
                        onClick = { if (isObjectsInteractive) selectedObjectId = obj.id },
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

                            // --- Move towards idealX and idealY independently ---
                            val diffX = idealX - obj.x
                            var finalX = obj.x
                            if (diffX != 0f) {
                                if (isPosValid(idealX, obj.y)) {
                                    finalX = idealX
                                } else {
                                    val collidingX = mapObjects.find { other ->
                                        val otherW = other.width * other.scale
                                        val otherH = other.height * other.scale
                                        other.id != obj.id &&
                                                idealX < other.x + otherW &&
                                                idealX + objWidth > other.x &&
                                                obj.y < other.y + otherH &&
                                                obj.y + objHeight > other.y
                                    }
                                    var snapped = false
                                    if (collidingX != null) {
                                        val snapX = if (diffX > 0) collidingX.x - objWidth else collidingX.x + (collidingX.width * collidingX.scale)
                                        if ((if (diffX > 0) snapX >= obj.x else snapX <= obj.x) && isPosValid(snapX, obj.y)) {
                                            finalX = snapX
                                            snapped = true
                                        }
                                    }
                                    if (!snapped) {
                                        var low = 0f
                                        var high = diffX
                                        for (i in 0..8) {
                                            val mid = (low + high) / 2
                                            if (isPosValid(obj.x + mid, obj.y)) low = mid else high = mid
                                        }
                                        finalX = obj.x + low
                                    }
                                }
                            }

                            val diffY = idealY - obj.y
                            var finalY = obj.y
                            if (diffY != 0f) {
                                if (isPosValid(finalX, idealY)) {
                                    finalY = idealY
                                } else {
                                    val collidingY = mapObjects.find { other ->
                                        val otherW = other.width * other.scale
                                        val otherH = other.height * other.scale
                                        other.id != obj.id &&
                                                finalX < other.x + otherW &&
                                                finalX + objWidth > other.x &&
                                                idealY < other.y + otherH &&
                                                idealY + objHeight > other.y
                                    }
                                    var snapped = false
                                    if (collidingY != null) {
                                        val snapY = if (diffY > 0) collidingY.y - objHeight else collidingY.y + (collidingY.height * collidingY.scale)
                                        if ((if (diffY > 0) snapY >= obj.y else snapY <= obj.y) && isPosValid(finalX, snapY)) {
                                            finalY = snapY
                                            snapped = true
                                        }
                                    }
                                    if (!snapped) {
                                        var low = 0f
                                        var high = diffY
                                        for (i in 0..8) {
                                            val mid = (low + high) / 2
                                            if (isPosValid(finalX, obj.y + mid)) low = mid else high = mid
                                        }
                                        finalY = obj.y + low
                                    }
                                }
                            }

                            if (finalX != obj.x || finalY != obj.y) {
                                onObjectMove(obj.id, finalX - obj.x, finalY - obj.y)
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
                    pathObj.moveTo(walkPath[0].x * dens, walkPath[0].y * dens)
                    for (i in 1 until walkPath.size) {
                        pathObj.lineTo(walkPath[i].x * dens, walkPath[i].y * dens)
                    }
                    drawPath(pathObj, color = Color(0xFFFFD600), style = Stroke(width = 3f * dens))
                }
                walkPath.forEach { pt ->
                    drawCircle(
                            color = Color(0xFFFFD600),
                            radius = 5f * dens,
                            center = Offset(pt.x * dens, pt.y * dens)
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
                predictionResult.points.forEach { spot ->
                    drawCircle(
                            color = Color(0xFFE53935).copy(alpha = 0.4f),
                            radius = 14f * dens,
                            center = Offset(spot.x * dens, spot.y * dens)
                    )
                    drawCircle(
                            color = Color(0xFFE53935).copy(alpha = 0.7f),
                            radius = 6f * dens,
                            center = Offset(spot.x * dens, spot.y * dens)
                    )
                }

                // AI predictions — red dashed lines (dp coords)
                predictionResult.lines.forEach { line ->
                    drawLine(
                            color = Color(0xFFE53935).copy(alpha = 0.6f),
                            start = Offset(line.first.x * dens, line.first.y * dens),
                            end = Offset(line.second.x * dens, line.second.y * dens),
                            strokeWidth = 3f * dens,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                    )
                    // Draw end point indicators
                    drawCircle(
                            color = Color(0xFFE53935).copy(alpha = 0.6f),
                            radius = 4f * dens,
                            center = Offset(line.first.x * dens, line.first.y * dens)
                    )
                    drawCircle(
                            color = Color(0xFFE53935).copy(alpha = 0.6f),
                            radius = 4f * dens,
                            center = Offset(line.second.x * dens, line.second.y * dens)
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
        isInteractive: Boolean,
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
                                if (isInteractive) {
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

        if (isSelected && isInteractive) {
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
                Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Object",
                        tint = Color(0xFFE53935),
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
