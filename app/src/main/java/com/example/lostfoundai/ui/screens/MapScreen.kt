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
import androidx.compose.ui.graphics.nativeCanvas
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
    var pendingInnerWalls by remember { mutableStateOf<List<List<PointF>>>(emptyList()) }

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
    var isDrawingInnerWall by remember { mutableStateOf(false) }
    var innerWallVertices by remember { mutableStateOf(emptyList<PointF>()) }
    
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
    var lastValidGhostRotation by remember { mutableFloatStateOf(0f) }
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
                    val adaptedInnerWalls = remember(drawingVertices, baselineBoundaryVertices, roomBoundary.innerWalls) {
                        if (isDrawingBoundary && baselineBoundaryVertices != null) {
                            com.example.lostfoundai.utils.BoundaryUtils.adaptInnerWallsToNewBoundary(
                                innerWalls = roomBoundary.innerWalls,
                                oldBoundary = baselineBoundaryVertices!!,
                                newBoundary = drawingVertices
                            )
                        } else {
                            roomBoundary.innerWalls
                        }
                    }

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
                            onWalkPathPointMove = { idx, newPos ->
                                mapViewModel.updateWalkPathPoint(idx, newPos.x, newPos.y)
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
                            onBoundaryShapeUpdate = { newVerts ->
                                drawingVertices = removeCollinearPoints(newVerts)
                            },
                            onInnerWallTap = { idx ->
                                val wallToEdit = roomBoundary.innerWalls[idx]
                                val newWalls = roomBoundary.innerWalls.toMutableList()
                                newWalls.removeAt(idx)
                                mapViewModel.updateInnerWalls(newWalls)
                                innerWallVertices = wallToEdit
                                isDrawingInnerWall = true
                            },
                            onInnerWallVertexMove = { idx, newPos ->
                                val verts = innerWallVertices.toMutableList()
                                val old = verts[idx]
                                verts[idx] = newPos
                                
                                if (idx > 0) {
                                    val prevIdx = idx - 1
                                    if (verts[prevIdx].y == old.y) verts[prevIdx] = PointF(verts[prevIdx].x, newPos.y)
                                    else verts[prevIdx] = PointF(newPos.x, verts[prevIdx].y)
                                }
                                if (idx < verts.size - 1) {
                                    val nextIdx = idx + 1
                                    if (verts[nextIdx].x == old.x) verts[nextIdx] = PointF(newPos.x, verts[nextIdx].y)
                                    else verts[nextIdx] = PointF(verts[nextIdx].x, newPos.y)
                                }
                                innerWallVertices = removeCollinearPoints(verts)
                            },
                            onInnerWallShapeUpdate = { newVerts ->
                                innerWallVertices = removeCollinearPoints(newVerts)
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
                            innerWalls = adaptedInnerWalls,
                            isDrawingInnerWall = isDrawingInnerWall,
                            innerWallVertices = innerWallVertices,
                            isSpecifyingLocation = isSpecifyingLocation,
                            isRecordingWalkPath = isRecordingWalkPath,
                            specifiedLocation = specifiedLocation,
                            onCanvasTap = { tapOffsetDp ->
                                val effectiveBoundary = pendingBoundaryVertices ?: baselineBoundaryVertices ?: roomBoundary.vertices
                                when {
                                    isDrawingBoundary -> {
                                        // This shouldn't be called directly for drag, handled by onDrawingVertexAdd
                                    }
                                    isDrawingInnerWall -> {
                                        // Handled by onDrawingVertexAdd
                                    }
                                    isSpecifyingLocation -> {
                                        specifiedLocation = tapOffsetDp
                                        isSpecifyingLocation = false
                                        showSearchSheet = true
                                    }
                                    isRecordingWalkPath -> mapViewModel.addWalkPathPoint(tapOffsetDp.x, tapOffsetDp.y)
                                }
                            },
                            onDrawingVertexAdd = { point, prepend ->
                                if (isDrawingBoundary) {
                                    drawingVertices = if (prepend) listOf(point) + drawingVertices else drawingVertices + point
                                } else if (isDrawingInnerWall) {
                                    innerWallVertices = if (prepend) listOf(point) + innerWallVertices else innerWallVertices + point
                                }
                            },
                            onInnerWallComplete = {
                                if (innerWallVertices.isNotEmpty()) {
                                    mapViewModel.setRoomBoundary(roomBoundary.copy(innerWalls = roomBoundary.innerWalls + listOf(innerWallVertices)))
                                    innerWallVertices = emptyList()
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
                            onObjectUpdate = { obj ->
                                mapViewModel.updateMapObject(obj)
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
                if (isEditing && !isDrawingBoundary && !isDrawingInnerWall && !isRecordingWalkPath && !showBoundarySheet) {
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
                                        val isBoundaryObject = droppedType == MapObjectType.WINDOW || droppedType == MapObjectType.DOOR_LEFT || droppedType == MapObjectType.DOOR_RIGHT
                                        
                                        var finalW = dims.first
                                        var finalH = dims.second
                                        
                                        if (isBoundaryObject) {
                                            val rem = lastValidGhostRotation % 180f
                                            val isOrthogonalSwap = (kotlin.math.abs(rem) == 90f || kotlin.math.abs(rem) == 270f)
                                            if (isOrthogonalSwap) {
                                                finalW = dims.second
                                                finalH = dims.first
                                            }
                                        }
                                        
                                        mapViewModel.addMapObject(
                                                MapObject(
                                                        type = droppedType,
                                                        x = lastValidGhostX,
                                                        y = lastValidGhostY,
                                                        width = finalW,
                                                        height = finalH,
                                                        rotation = lastValidGhostRotation
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
                        val isBoundaryObject = draggingType == MapObjectType.WINDOW || draggingType == MapObjectType.DOOR_LEFT || draggingType == MapObjectType.DOOR_RIGHT
                        if (!isBoundaryObject) {
                            if (!BoundaryUtils.rectInPolygon(px, py, rawWidth, rawHeight, effectiveBoundary)) return false
                            
                            val intersectsInnerWall = roomBoundary.innerWalls.any { wall ->
                                (0 until wall.size - 1).any { i ->
                                    BoundaryUtils.rectIntersectsLine(px, py, rawWidth, rawHeight, wall[i], wall[i+1])
                                }
                            }
                            if (intersectsInnerWall) return false
                        } else {
                            return true
                        }
                        
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
                    // Update last valid position
                    val isBoundaryObject = draggingType == MapObjectType.WINDOW || draggingType == MapObjectType.DOOR_LEFT || draggingType == MapObjectType.DOOR_RIGHT
                    val allWallSegments = mutableListOf<Pair<PointF, PointF>>()
                    if (effectiveBoundary.size >= 2) {
                        for (i in effectiveBoundary.indices) {
                            allWallSegments.add(effectiveBoundary[i] to effectiveBoundary[(i + 1) % effectiveBoundary.size])
                        }
                    }
                    roomBoundary.innerWalls.forEach { wall ->
                        for (i in 0 until wall.size - 1) {
                            allWallSegments.add(wall[i] to wall[i + 1])
                        }
                    }

                    if (isBoundaryObject && allWallSegments.isNotEmpty()) {
                        val cx = virtualXStr + rawWidth / 2f
                        val cy = virtualYStr + rawHeight / 2f
                        var bestDistSq = Float.MAX_VALUE
                        var bestPos = PointF(virtualXStr, virtualYStr)
                        var bestRot = 0f
                        for ((p1, p2) in allWallSegments) {
                            val dx = p2.x - p1.x
                            val dy = p2.y - p1.y
                            val lenSq = dx*dx + dy*dy
                            if (lenSq == 0f) continue
                            val len = kotlin.math.sqrt(lenSq.toDouble()).toFloat()
                            val t = ((cx - p1.x)*dx + (cy - p1.y)*dy) / lenSq
                            val ct = t.coerceIn(0f, 1f)
                            val nx = p1.x + ct * dx
                            val ny = p1.y + ct * dy
                            val distSq = (cx - nx)*(cx - nx) + (cy - ny)*(cy - ny)
                            if (distSq < bestDistSq) {
                                bestDistSq = distSq
                                if (draggingType == MapObjectType.WINDOW) {
                                    bestPos = PointF(nx - rawWidth / 2f, ny - rawHeight / 2f)
                                    bestRot = (kotlin.math.atan2(dy, dx) * 180f / kotlin.math.PI.toFloat())
                                } else {
                                    val N = PointF(-dy/len, dx/len)
                                    val dot = (cx - nx) * N.x + (cy - ny) * N.y
                                    val usePosNormal = dot > 0
                                    val finalCx = if (usePosNormal) nx + N.x * rawHeight / 2f else nx - N.x * rawHeight / 2f
                                    val finalCy = if (usePosNormal) ny + N.y * rawHeight / 2f else ny - N.y * rawHeight / 2f
                                    bestPos = PointF(finalCx - rawWidth / 2f, finalCy - rawHeight / 2f)
                                    bestRot = if (usePosNormal) kotlin.math.atan2(-dy, -dx) * 180f / kotlin.math.PI.toFloat() else kotlin.math.atan2(dy, dx) * 180f / kotlin.math.PI.toFloat()
                                }
                            }
                        }
                        lastValidGhostX = bestPos.x
                        lastValidGhostY = bestPos.y
                        lastValidGhostRotation = bestRot
                        hasValidGhostPos = true
                    } else {
                        lastValidGhostX = ghostX
                        lastValidGhostY = ghostY
                        lastValidGhostRotation = 0f
                        hasValidGhostPos = true
                    }

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
                    ) { MapObjectVisuals(type = draggingType!!, modifier = Modifier.fillMaxSize().rotate(lastValidGhostRotation)) }
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
                                                val adaptedInnerWalls = com.example.lostfoundai.utils.BoundaryUtils.adaptInnerWallsToNewBoundary(
                                                    roomBoundary.innerWalls, baselineBoundaryVertices!!, finalVertices
                                                )
                                                val savedId = roomBoundary.savedBoundaryId
                                                if (savedId != null) {
                                                    // 【需求三】自訂邊界：原地更新
                                                    mapViewModel.updateSavedBoundary(savedId, finalVertices)
                                                    mapViewModel.updateInnerWalls(adaptedInnerWalls)
                                                    mapViewModel.setRoomBoundary(
                                                        RoomBoundary(
                                                            preset = RoomShapePreset.CUSTOM,
                                                            vertices = finalVertices,
                                                            savedBoundaryId = savedId,
                                                            innerWalls = adaptedInnerWalls
                                                        )
                                                    )
                                                } else {
                                                    // 【需求四】預設邊界：另存為新的自訂邊界，觸發命名對話框
                                                    pendingBoundaryVertices = finalVertices
                                                    pendingInnerWalls = adaptedInnerWalls
                                                    showNameDialog = true
                                                }
                                                mapViewModel.relocateObjectsIntoBoundary(finalVertices)
                                            } else {
                                                // ── 全新繪製完成：一般存檔流程 ───────────────
                                                pendingBoundaryVertices = finalVertices
                                                pendingInnerWalls = emptyList()
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
                            Text("取消微調", color = Color.White)
                        }
                    }
                } else if (isDrawingInnerWall) {
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
                                    if (innerWallVertices.isNotEmpty()) {
                                        innerWallVertices = innerWallVertices.dropLast(1)
                                    }
                                },
                                enabled = innerWallVertices.isNotEmpty(),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
                            ) { Text("上一步") }
                            OutlinedButton(
                                onClick = { innerWallVertices = emptyList() },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
                            ) { Text("清除重畫") }
                            Button(
                                    onClick = {
                                        if (innerWallVertices.size >= 2) {
                                            val finalWall = removeCollinearPoints(innerWallVertices)
                                            mapViewModel.updateInnerWalls(roomBoundary.innerWalls + listOf(finalWall))
                                        }
                                        innerWallVertices = emptyList()
                                    },
                                    enabled = innerWallVertices.size >= 2,
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
                            ) { Text("完成此牆") }
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
                            isDrawingInnerWall = false
                            innerWallVertices = emptyList()
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
                                onClick = { 
                                    if (isRecordingWalkPath) mapViewModel.toggleWalkPathRecording()
                                    showBoundarySheet = true 
                                },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
                            ) { Text("邊界管理") }
                            OutlinedButton(
                                onClick = { mapViewModel.toggleWalkPathRecording() },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
                            ) { Text("路線編輯") }
                            Button(
                                onClick = {
                                    if (isRecordingWalkPath) {
                                        mapViewModel.toggleWalkPathRecording()
                                    }
                                    isEditing = false
                                },
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
                                onClick = { mapViewModel.undoWalkPathPoint() },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                modifier = Modifier.height(28.dp)
                            ) { Text("上一步", color = Color(0xFF1A1C2E)) }
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
                        (predictionResult.points.isNotEmpty() || predictionResult.lines.isNotEmpty() || (walkPath.isNotEmpty() && isWalkPathVisible) || searchItems.any { it.manualX != null })) {
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
                            onClick = { 
                                if (isRecordingWalkPath) mapViewModel.toggleWalkPathRecording()
                                coroutineScope.launch { drawerState.open() } 
                            },
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
                    
                    // Center Coordinate Display
                    if (canvasSize.width > 0f && canvasSize.height > 0f) {
                        val centerVirtualX = (-mapOffset.x / mapScale + canvasSize.width / 2f) / density
                        val centerVirtualY = (-mapOffset.y / mapScale + canvasSize.height / 2f) / density
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp, bottom = 100.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            color = SurfaceColor.copy(alpha = 0.8f),
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = String.format("中心座標: X: %.1f, Y: %.1f", centerVirtualX, centerVirtualY),
                                style = MaterialTheme.typography.bodySmall,
                                color = PrimaryIndigo,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
                }
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(modifier = Modifier.weight(1f), onClick = {
                        showBoundarySheet = false
                        innerWallVertices = emptyList()
                        isDrawingInnerWall = true
                    }) {
                        Text("繪製牆面")
                    }
                    Button(modifier = Modifier.weight(1f), onClick = {
                        showBoundarySheet = false
                        // 進入微調模式：用 isDrawingBoundary 合併狀態，並記錄基線底圖
                        val fallbackBoundary = if (roomBoundary.vertices.isNotEmpty()) roomBoundary.vertices else {
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
                        baselineBoundaryVertices = fallbackBoundary
                        drawingVertices = fallbackBoundary
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
                ) { Text("自訂繪製邊界") }
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
                                                savedBoundaryId = saved.id,
                                                innerWalls = pendingInnerWalls
                                        )
                                )
                                mapViewModel.updateInnerWalls(pendingInnerWalls)
                                mapViewModel.relocateObjectsIntoBoundary(pendingBoundaryVertices)
                                showNameDialog = false
                                pendingBoundaryVertices = emptyList()
                                pendingInnerWalls = emptyList()
                            }
                    ) { Text("儲存") }
                },
                dismissButton = {
                    TextButton(
                            onClick = {
                                showNameDialog = false
                                pendingBoundaryVertices = emptyList()
                                pendingInnerWalls = emptyList()
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
        innerWalls: List<List<PointF>> = emptyList(),
        isDrawingInnerWall: Boolean = false,
        innerWallVertices: List<PointF> = emptyList(),
        isSpecifyingLocation: Boolean,
        isRecordingWalkPath: Boolean = false,
        specifiedLocation: PointF?,
        onCanvasTap: (PointF) -> Unit,
        onDrawingVertexAdd: (PointF, Boolean) -> Unit = { _, _ -> },
        onInnerWallTap: (Int) -> Unit = {},
        onInnerWallComplete: () -> Unit = {},
        onFurnitureTap: (MapObject) -> Unit = {},
        onDrawingVertexMove: (Int, PointF) -> Unit,
        onBoundaryVertexMove: (Int, PointF) -> Unit = { _, _ -> },
        onBoundaryShapeUpdate: (List<PointF>) -> Unit = {},
        onBoundaryMidpointMove: (Int, PointF) -> Unit = { _, _ -> },
        onInnerWallVertexMove: (Int, PointF) -> Unit = { _, _ -> },
        onInnerWallShapeUpdate: (List<PointF>) -> Unit = {},
        onWalkPathPointMove: (Int, PointF) -> Unit = { _, _ -> },
        onObjectMove: (String, Float, Float) -> Unit,
        onObjectUpdate: (MapObject) -> Unit = {},
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

    // Drag Drawing State
    var activeDragStartPoint by remember { mutableStateOf<PointF?>(null) }
    var activeDragCurrentPoint by remember { mutableStateOf<PointF?>(null) }
    var activeDragIsInvalid by remember { mutableStateOf(false) }
    var activeDragStartIdx by remember { mutableStateOf<Int?>(null) } // 0 for head, lastIndex for tail
    var activeDragLClosePos by remember { mutableStateOf<List<PointF>?>(null) }
    var activeDragWasSnapped by remember { mutableStateOf<Boolean>(false) }

    // Whole Shape Dragging State
    var isDraggingWholeShape by remember { mutableStateOf(false) }
    var wholeShapeInitialVertices by remember { mutableStateOf<List<PointF>?>(null) }
    var wholeShapeDragStartPoint by remember { mutableStateOf<PointF?>(null) }

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
    val currentWalkPath by rememberUpdatedState(walkPath)
    val currentOnScaleChange by rememberUpdatedState(onScaleChange)
    val currentOnOffsetChange by rememberUpdatedState(onOffsetChange)
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

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
                            .pointerInput(isDrawingBoundary, isSpecifyingLocation, isRecordingWalkPath, isDrawingInnerWall, drawingVertices, innerWallVertices) {
                                if (isDrawingBoundary || isDrawingInnerWall) {
                                    detect1FingerDrawGestures(
                                        onDragStart = { tapOffset ->
                                            val dens = density.toFloat()
                                            val centerX = size.width / 2f
                                            val centerY = size.height / 2f
                                            val mapPxX = (tapOffset.x - currentMapOffset.x - centerX) / currentMapScale + centerX
                                            val mapPxY = (tapOffset.y - currentMapOffset.y - centerY) / currentMapScale + centerY
                                            val virtualX = mapPxX / dens
                                            val virtualY = mapPxY / dens
                                            
                                            val targetVertices = if (isDrawingBoundary) drawingVertices else innerWallVertices
                                            
                                            if (targetVertices.isEmpty()) {
                                                activeDragStartIdx = null
                                                activeDragStartPoint = PointF(virtualX, virtualY)
                                                activeDragCurrentPoint = PointF(virtualX, virtualY)
                                            } else {
                                                var hit = false
                                                for (i in 0 until targetVertices.size - 1) {
                                                    val dist = com.example.lostfoundai.utils.BoundaryUtils.distanceToSegment(virtualX, virtualY, targetVertices[i], targetVertices[i+1])
                                                    if (dist < 20f) { hit = true; break }
                                                }
                                                // For boundary, check closing segment if there are at least 3 vertices
                                                if (!hit && isDrawingBoundary && targetVertices.size >= 3) {
                                                    val dist = com.example.lostfoundai.utils.BoundaryUtils.distanceToSegment(virtualX, virtualY, targetVertices.last(), targetVertices.first())
                                                    if (dist < 20f) { hit = true }
                                                }
                                                
                                                if (hit) {
                                                    isDraggingWholeShape = true
                                                    wholeShapeInitialVertices = targetVertices
                                                    wholeShapeDragStartPoint = PointF(virtualX, virtualY)
                                                    dragPreviewVertices = targetVertices
                                                    activeDragStartIdx = null
                                                    activeDragStartPoint = null
                                                } else {
                                                    activeDragStartIdx = null
                                                    activeDragStartPoint = null
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            if (isDraggingWholeShape) {
                                                if (dragPreviewVertices != null) {
                                                    if (isDrawingBoundary) {
                                                        onBoundaryShapeUpdate(dragPreviewVertices!!)
                                                    } else if (isDrawingInnerWall) {
                                                        onInnerWallShapeUpdate(dragPreviewVertices!!)
                                                    }
                                                }
                                                isDraggingWholeShape = false
                                                wholeShapeDragStartPoint = null
                                                wholeShapeInitialVertices = null
                                                return@detect1FingerDrawGestures
                                            }

                                            var wasSnapped = false
                                            if (activeDragStartPoint != null && activeDragCurrentPoint != null && !activeDragIsInvalid) {
                                                val targetVertices = if (isDrawingBoundary) drawingVertices else innerWallVertices
                                                if (targetVertices.isEmpty()) {
                                                    onDrawingVertexAdd(activeDragStartPoint!!, false)
                                                }
                                                val prepend = activeDragStartIdx == 0
                                                onDrawingVertexAdd(activeDragCurrentPoint!!, prepend)
                                                if (activeDragLClosePos != null) {
                                                    if (prepend) {
                                                        activeDragLClosePos!!.reversed().forEach { p -> onDrawingVertexAdd(p, true) }
                                                    } else {
                                                        activeDragLClosePos!!.forEach { p -> onDrawingVertexAdd(p, false) }
                                                    }
                                                }
                                                // Check if it was an inner wall that snapped
                                                // Removed auto-completion so user can continue operating
                                                // if (isDrawingInnerWall && activeDragWasSnapped) {
                                                //     wasSnapped = true
                                                // }
                                            }
                                            activeDragStartPoint = null
                                            activeDragCurrentPoint = null
                                            activeDragIsInvalid = false
                                            activeDragLClosePos = null
                                            activeDragStartIdx = null
                                            activeDragWasSnapped = false
                                            
                                            if (wasSnapped) {
                                                onInnerWallComplete()
                                            }
                                        },
                                        onDragCancel = {
                                            isDraggingWholeShape = false
                                            wholeShapeDragStartPoint = null
                                            wholeShapeInitialVertices = null
                                            
                                            activeDragStartPoint = null
                                            activeDragCurrentPoint = null
                                            activeDragIsInvalid = false
                                            activeDragLClosePos = null
                                            activeDragStartIdx = null
                                        },
                                        onDrag = { tapOffset ->
                                            if (isDraggingWholeShape && wholeShapeDragStartPoint != null && wholeShapeInitialVertices != null) {
                                                val dens = density.toFloat()
                                                val centerX = size.width / 2f
                                                val centerY = size.height / 2f
                                                val mapPxX = (tapOffset.x - currentMapOffset.x - centerX) / currentMapScale + centerX
                                                val mapPxY = (tapOffset.y - currentMapOffset.y - centerY) / currentMapScale + centerY
                                                val virtualX = mapPxX / dens
                                                val virtualY = mapPxY / dens
                                                val dx = virtualX - wholeShapeDragStartPoint!!.x
                                                val dy = virtualY - wholeShapeDragStartPoint!!.y
                                                
                                                val newVertices = wholeShapeInitialVertices!!.map { PointF(it.x + dx, it.y + dy) }.toMutableList()
                                                
                                                if (isDrawingInnerWall && baselineBoundaryVertices != null && baselineBoundaryVertices.size >= 3) {
                                                    val firstNode = wholeShapeInitialVertices!!.first()
                                                    val lastNode = wholeShapeInitialVertices!!.last()
                                                    
                                                    fun slideSnappedNode(nodeIdx: Int, originalNode: PointF) {
                                                        for (i in 0 until baselineBoundaryVertices.size) {
                                                            val p1 = baselineBoundaryVertices[i]
                                                            val p2 = baselineBoundaryVertices[(i+1)%baselineBoundaryVertices.size]
                                                            val dist = com.example.lostfoundai.utils.BoundaryUtils.distanceToSegment(originalNode.x, originalNode.y, p1, p2)
                                                            // If original node was snapped (dist < 5f), force the new node onto this segment
                                                            if (dist < 5f) {
                                                                newVertices[nodeIdx] = com.example.lostfoundai.utils.BoundaryUtils.projectPointOntoSegment(newVertices[nodeIdx].x, newVertices[nodeIdx].y, p1, p2)
                                                                break
                                                            }
                                                        }
                                                    }
                                                    
                                                    slideSnappedNode(0, firstNode)
                                                    slideSnappedNode(newVertices.lastIndex, lastNode)
                                                }
                                                
                                                dragPreviewVertices = newVertices
                                                return@detect1FingerDrawGestures
                                            }

                                            if (activeDragStartPoint == null) return@detect1FingerDrawGestures
                                            val dens = density.toFloat()
                                            val centerX = size.width / 2f
                                            val centerY = size.height / 2f
                                            val mapPxX = (tapOffset.x - currentMapOffset.x - centerX) / currentMapScale + centerX
                                            val mapPxY = (tapOffset.y - currentMapOffset.y - centerY) / currentMapScale + centerY
                                            var virtualX = mapPxX / dens
                                            var virtualY = mapPxY / dens
                                            
                                            val targetVertices = if (isDrawingBoundary) drawingVertices else innerWallVertices
                                            
                                            // Enforce orthogonal
                                            val startP = activeDragStartPoint!!
                                            val dx = kotlin.math.abs(virtualX - startP.x)
                                            val dy = kotlin.math.abs(virtualY - startP.y)
                                            if (dx > dy) {
                                                virtualY = startP.y
                                            } else {
                                                virtualX = startP.x
                                            }
                                            var currentP = PointF(virtualX, virtualY)
                                            
                                            // 1. Anti-Crossing Check
                                            var isInvalid = false
                                            if (targetVertices.size >= 2) {
                                                for (i in 0 until targetVertices.size - 1) {
                                                    // Don't check against the segment we are directly extending from
                                                    if (activeDragStartIdx == 0 && i == 0) continue
                                                    if (activeDragStartIdx == targetVertices.lastIndex && i == targetVertices.size - 2) continue
                                                    
                                                    val p3 = targetVertices[i]
                                                    val p4 = targetVertices[i+1]
                                                    if (com.example.lostfoundai.utils.BoundaryUtils.lineIntersectsLine(startP, currentP, p3, p4)) {
                                                        isInvalid = true
                                                        break
                                                    }
                                                }
                                            }
                                            
                                            // 2. L-shape Auto Completion (Boundary)
                                            var lClosePos: List<PointF>? = null
                                            if (isDrawingBoundary && targetVertices.size >= 3) {
                                                val targetCloseNode = if (activeDragStartIdx == 0) targetVertices.last() else targetVertices.first()
                                                val distToClose = kotlin.math.sqrt(((currentP.x - targetCloseNode.x) * (currentP.x - targetCloseNode.x) + (currentP.y - targetCloseNode.y) * (currentP.y - targetCloseNode.y)).toDouble()).toFloat()
                                                
                                                if (distToClose < 50f) {
                                                    // Generate L-shape to targetCloseNode
                                                    // Since current segment is startP -> currentP, the next must be currentP -> ... -> targetCloseNode
                                                    // We can just set currentP to align with targetCloseNode on the non-moving axis
                                                    if (startP.x == currentP.x) {
                                                        // Dragging vertically, align Y with targetCloseNode
                                                        currentP = PointF(startP.x, targetCloseNode.y)
                                                        lClosePos = listOf(PointF(startP.x, targetCloseNode.y), targetCloseNode)
                                                    } else {
                                                        // Dragging horizontally, align X with targetCloseNode
                                                        currentP = PointF(targetCloseNode.x, startP.y)
                                                        lClosePos = listOf(PointF(targetCloseNode.x, startP.y), targetCloseNode)
                                                    }
                                                }
                                            }
                                            
                                            // 3. Boundary Snap (Inner Wall)
                                            var snappedFlag = false
                                            if (isDrawingInnerWall && boundaryVertices.size >= 2) {
                                                var minSnapDist = 50f
                                                var snapped = currentP
                                                val isHoriz = startP.y == currentP.y
                                                val rayDir = if (isHoriz) kotlin.math.sign(currentP.x - startP.x) else kotlin.math.sign(currentP.y - startP.y)
                                                
                                                if (rayDir != 0f) {
                                                    for (i in boundaryVertices.indices) {
                                                        val p1 = boundaryVertices[i]
                                                        val p2 = boundaryVertices[(i + 1) % boundaryVertices.size]
                                                        
                                                        if (isHoriz) {
                                                            if ((p1.y > currentP.y) != (p2.y > currentP.y)) {
                                                                val intersectX = p1.x + (currentP.y - p1.y) * (p2.x - p1.x) / (p2.y - p1.y)
                                                                val dist = kotlin.math.abs(intersectX - currentP.x)
                                                                val dir = kotlin.math.sign(intersectX - startP.x)
                                                                if (dist < minSnapDist && (dir == rayDir || kotlin.math.abs(intersectX - startP.x) < dist)) {
                                                                    minSnapDist = dist
                                                                    snapped = PointF(intersectX, currentP.y)
                                                                    snappedFlag = true
                                                                }
                                                            }
                                                        } else {
                                                            if ((p1.x > currentP.x) != (p2.x > currentP.x)) {
                                                                val intersectY = p1.y + (currentP.x - p1.x) * (p2.y - p1.y) / (p2.x - p1.x)
                                                                val dist = kotlin.math.abs(intersectY - currentP.y)
                                                                val dir = kotlin.math.sign(intersectY - startP.y)
                                                                if (dist < minSnapDist && (dir == rayDir || kotlin.math.abs(intersectY - startP.y) < dist)) {
                                                                    minSnapDist = dist
                                                                    snapped = PointF(currentP.x, intersectY)
                                                                    snappedFlag = true
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                currentP = snapped
                                            }
                                            
                                            if (lClosePos != null && activeDragLClosePos == null) {
                                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                            }
                                            
                                            activeDragCurrentPoint = currentP
                                            activeDragIsInvalid = isInvalid
                                            activeDragLClosePos = lClosePos
                                            activeDragWasSnapped = snappedFlag
                                        }
                                    )
                                }
                            }
                            .pointerInput(isDrawingBoundary, isSpecifyingLocation, isRecordingWalkPath, isDrawingInnerWall, drawingVertices, innerWallVertices, isEditing, mapObjects, innerWalls) {
                                detectTapGestures(
                                            onDoubleTap = {
                                                if (isDrawingInnerWall) {
                                                    onInnerWallComplete()
                                                }
                                            },
                                            onTap = { tapOffset ->
                                                val dens = density.toFloat()
                                                val centerX = size.width / 2f
                                                val centerY = size.height / 2f
                                                val mapPxX = (tapOffset.x - currentMapOffset.x - centerX) / currentMapScale + centerX
                                                val mapPxY = (tapOffset.y - currentMapOffset.y - centerY) / currentMapScale + centerY
                                                val virtualX = mapPxX / dens
                                                val virtualY = mapPxY / dens

                                                if (isSpecifyingLocation || isRecordingWalkPath) {
                                                    onCanvasTap(PointF(virtualX, virtualY))
                                                } else if (isEditing && !isDrawingBoundary && innerWallVertices.isEmpty()) {
                                                    var tappedWallIndex = -1
                                                    for ((index, wall) in innerWalls.withIndex()) {
                                                        for (i in 0 until wall.size - 1) {
                                                            val p1 = wall[i]
                                                            val p2 = wall[i+1]
                                                            val l2 = (p1.x - p2.x)*(p1.x - p2.x) + (p1.y - p2.y)*(p1.y - p2.y)
                                                            val t = if (l2 == 0f) 0f else Math.max(0f, Math.min(1f, ((virtualX - p1.x) * (p2.x - p1.x) + (virtualY - p1.y) * (p2.y - p1.y)) / l2))
                                                            val projX = p1.x + t * (p2.x - p1.x)
                                                            val projY = p1.y + t * (p2.y - p1.y)
                                                            val dist = kotlin.math.sqrt(((virtualX - projX)*(virtualX - projX) + (virtualY - projY)*(virtualY - projY)).toDouble()).toFloat()
                                                            if (dist < 20f) {
                                                                tappedWallIndex = index
                                                                break
                                                            }
                                                        }
                                                        if (tappedWallIndex != -1) break
                                                    }
                                                    
                                                    if (tappedWallIndex != -1) {
                                                        onInnerWallTap(tappedWallIndex)
                                                    } else {
                                                        selectedObjectId = null
                                                    }
                                                } else if (!isEditing) {
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
            val gridAlpha by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isDrawingBoundary || isDrawingInnerWall || isEditing) 1f else if (gridEnabled) 1f else 0f
            )
            if (gridAlpha > 0f) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val gridSpacing = 20f * density
                    val gridColor = Color(0xFFDDDDDD).copy(alpha = gridAlpha)

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
            
            // Inner Walls
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dens = density
                innerWalls.forEach { wall ->
                    if (wall.size >= 2) {
                        val path = Path()
                        path.moveTo(wall[0].x * dens, wall[0].y * dens)
                        for (i in 1 until wall.size) {
                            path.lineTo(wall[i].x * dens, wall[i].y * dens)
                        }
                        drawPath(path, color = Color.Black, style = Stroke(width = 2f * dens))
                    }
                }
            }
            
            // Drawing Inner Wall Preview
            if (isDrawingInnerWall && innerWallVertices.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val dens = density
                    val path = Path()
                    path.moveTo(innerWallVertices[0].x * dens, innerWallVertices[0].y * dens)
                    for (i in 1 until innerWallVertices.size) {
                        path.lineTo(innerWallVertices[i].x * dens, innerWallVertices[i].y * dens)
                    }
                    drawPath(path, color = Color.Blue.copy(alpha = 0.8f), style = Stroke(width = 2f * dens))
                }
            }

            // Drawing preview — show in-progress vertices and lines
            val targetPreview = if (isDrawingBoundary) (dragPreviewVertices ?: drawingVertices) else if (isDrawingInnerWall) innerWallVertices else emptyList()
            if (targetPreview.isNotEmpty() || activeDragStartPoint != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val dens = density
                    // Draw confirmed lines
                    if (targetPreview.isNotEmpty()) {
                        val path = Path()
                        path.moveTo(targetPreview[0].x * dens, targetPreview[0].y * dens)
                        for (i in 1 until targetPreview.size) {
                            path.lineTo(targetPreview[i].x * dens, targetPreview[i].y * dens)
                        }
                        drawPath(
                                path,
                                color = if (isDrawingBoundary) Color.Red.copy(alpha = 0.8f) else Color.Blue.copy(alpha = 0.8f),
                                style = Stroke(width = 2f * dens)
                        )
                        
                        // Draw highlights on head and tail if not currently dragging
                        if (activeDragStartPoint == null) {
                            val head = targetPreview.first()
                            val tail = targetPreview.last()
                            drawCircle(Color(0xFF4CAF50), radius = 8f * dens, center = Offset(head.x * dens, head.y * dens), style = Stroke(width = 2f * dens))
                            if (targetPreview.size > 1) {
                                drawCircle(Color(0xFF4CAF50), radius = 8f * dens, center = Offset(tail.x * dens, tail.y * dens), style = Stroke(width = 2f * dens))
                            }
                        }
                    }
                    
                    // Draw dashed close line preview for boundary
                    if (isDrawingBoundary && targetPreview.size >= 3 && activeDragStartPoint == null) {
                        drawLine(
                                color = Color.Red.copy(alpha = 0.4f),
                                start = Offset(targetPreview.last().x * dens, targetPreview.last().y * dens),
                                end = Offset(targetPreview[0].x * dens, targetPreview[0].y * dens),
                                strokeWidth = 1.5f * dens
                        )
                    }
                    
                    // Draw active drag line
                    if (activeDragStartPoint != null && activeDragCurrentPoint != null) {
                        val sp = activeDragStartPoint!!
                        val ep = activeDragCurrentPoint!!
                        drawLine(
                            color = if (activeDragIsInvalid) Color.Red else Color(0xFF2196F3),
                            start = Offset(sp.x * dens, sp.y * dens),
                            end = Offset(ep.x * dens, ep.y * dens),
                            strokeWidth = 3f * dens
                        )
                        
                        // Draw live length text
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.DKGRAY
                            textSize = 14f * dens
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        val dist = kotlin.math.sqrt(((ep.x - sp.x) * (ep.x - sp.x) + (ep.y - sp.y) * (ep.y - sp.y)).toDouble())
                        val midX = (sp.x + ep.x) / 2f
                        val midY = (sp.y + ep.y) / 2f
                        drawContext.canvas.nativeCanvas.drawText(
                            String.format("%.1fm", dist / 100.0), // Assuming 100dp = 1m for display purposes
                            midX * dens,
                            (midY - 10f) * dens,
                            paint
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
                    val isEndNode = baselineBoundaryVertices == null && (idx == 0 || idx == targetVertices.lastIndex)
                    var virtualMapX by androidx.compose.runtime.remember(idx, currentDrawingVertices.getOrNull(idx)) { androidx.compose.runtime.mutableFloatStateOf(currentDrawingVertices.getOrNull(idx)?.x ?: 0f) }
                    var virtualMapY by androidx.compose.runtime.remember(idx, currentDrawingVertices.getOrNull(idx)) { androidx.compose.runtime.mutableFloatStateOf(currentDrawingVertices.getOrNull(idx)?.y ?: 0f) }

                    ExtrudeHandleView(
                        x = handlePxX,
                        y = handlePxY,
                        dens = dens,
                        isEndNode = isEndNode,
                        isSnapped = false,
                        color = if (baselineBoundaryVertices == null && idx == targetVertices.lastIndex) Color(0xFFFF5722)
                                else if (baselineBoundaryVertices != null) Color(0xFF1E88E5)
                                else Color.Red,
                        onCenterDragStart = {
                            val startPos = currentDrawingVertices[idx]
                            virtualMapX = startPos.x
                            virtualMapY = startPos.y
                            isDraggingHandle = true
                            dragPreviewVertices = currentDrawingVertices
                        },
                        onCenterDrag = { dragAmount ->
                            virtualMapX += dragAmount.x / dens
                            virtualMapY += dragAmount.y / dens

                            if (baselineBoundaryVertices == null) {
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
                        },
                        onCenterDragEnd = {
                            isDraggingHandle = false
                            if (baselineBoundaryVertices != null) {
                                dragPreviewVertices?.let { preview ->
                                    if (idx < preview.size) onBoundaryVertexMove(idx, PointF(virtualMapX, virtualMapY))
                                }
                            } else {
                                dragPreviewVertices?.let { preview ->
                                    if (idx < preview.size) onDrawingVertexMove(idx, preview[idx])
                                }
                            }
                        },
                        onExtrudeDragStart = {
                            activeDragStartIdx = idx
                            activeDragStartPoint = targetVertices[idx]
                            activeDragCurrentPoint = targetVertices[idx]
                        },
                        onExtrudeDrag = { dragAmount ->
                            if (activeDragStartPoint == null || activeDragCurrentPoint == null) return@ExtrudeHandleView
                            
                            val startP = activeDragStartPoint!!
                            var vx = activeDragCurrentPoint!!.x + dragAmount.x / dens
                            var vy = activeDragCurrentPoint!!.y + dragAmount.y / dens
                            
                            val dx = kotlin.math.abs(vx - startP.x)
                            val dy = kotlin.math.abs(vy - startP.y)
                            if (dx > dy) vy = startP.y else vx = startP.x
                            var currentP = PointF(vx, vy)
                            
                            var isInvalid = false
                            if (targetVertices.size >= 2) {
                                for (i in 0 until targetVertices.size - 1) {
                                    if (activeDragStartIdx == 0 && i == 0) continue
                                    if (activeDragStartIdx == targetVertices.lastIndex && i == targetVertices.size - 2) continue
                                    val p3 = targetVertices[i]
                                    val p4 = targetVertices[i+1]
                                    if (com.example.lostfoundai.utils.BoundaryUtils.lineIntersectsLine(startP, currentP, p3, p4)) {
                                        isInvalid = true
                                        break
                                    }
                                }
                            }
                            
                            var lClosePos: List<PointF>? = null
                            if (targetVertices.size >= 3) {
                                val targetCloseNode = if (activeDragStartIdx == 0) targetVertices.last() else targetVertices.first()
                                val distToClose = kotlin.math.sqrt(((currentP.x - targetCloseNode.x) * (currentP.x - targetCloseNode.x) + (currentP.y - targetCloseNode.y) * (currentP.y - targetCloseNode.y)).toDouble()).toFloat()
                                if (distToClose < 50f) {
                                    if (startP.x == currentP.x) {
                                        currentP = PointF(startP.x, targetCloseNode.y)
                                        lClosePos = listOf(PointF(startP.x, targetCloseNode.y), targetCloseNode)
                                    } else {
                                        currentP = PointF(targetCloseNode.x, startP.y)
                                        lClosePos = listOf(PointF(targetCloseNode.x, startP.y), targetCloseNode)
                                    }
                                }
                            }
                            
                            activeDragCurrentPoint = currentP
                            activeDragIsInvalid = isInvalid
                            activeDragLClosePos = lClosePos
                        },
                        onExtrudeDragEnd = {
                            var wasClosed = false
                            if (activeDragStartPoint != null && activeDragCurrentPoint != null && !activeDragIsInvalid) {
                                val prepend = activeDragStartIdx == 0
                                onDrawingVertexAdd(activeDragCurrentPoint!!, prepend)
                                if (activeDragLClosePos != null) {
                                    if (prepend) {
                                        activeDragLClosePos!!.reversed().forEach { p -> onDrawingVertexAdd(p, true) }
                                    } else {
                                        activeDragLClosePos!!.forEach { p -> onDrawingVertexAdd(p, false) }
                                    }
                                    wasClosed = true
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                }
                            }
                            activeDragStartPoint = null
                            activeDragCurrentPoint = null
                            activeDragIsInvalid = false
                            activeDragLClosePos = null
                            activeDragStartIdx = null
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

            // Inner Wall Drawing mode: draggable handles on top of the canvas
            if (isDrawingInnerWall) {
                val dens = density
                val targetVertices = dragPreviewVertices ?: innerWallVertices
                
                targetVertices.forEachIndexed { idx, v ->
                    val handlePxX = v.x * dens
                    val handlePxY = v.y * dens
                    val isEndNode = idx == 0 || idx == targetVertices.lastIndex
                    var isOnBoundary = false
                    if (isEndNode && effectiveBoundary.size >= 2) {
                        for (i in effectiveBoundary.indices) {
                            val p1 = effectiveBoundary[i]
                            val p2 = effectiveBoundary[(i + 1) % effectiveBoundary.size]
                            val minX = kotlin.math.min(p1.x, p2.x) - 1f
                            val maxX = kotlin.math.max(p1.x, p2.x) + 1f
                            val minY = kotlin.math.min(p1.y, p2.y) - 1f
                            val maxY = kotlin.math.max(p1.y, p2.y) + 1f
                            if (v.x in minX..maxX && v.y in minY..maxY) {
                                if (p1.x == p2.x && kotlin.math.abs(v.x - p1.x) < 1f) isOnBoundary = true
                                if (p1.y == p2.y && kotlin.math.abs(v.y - p1.y) < 1f) isOnBoundary = true
                            }
                        }
                    }
                    var virtualMapX by androidx.compose.runtime.remember(idx, innerWallVertices.getOrNull(idx)) { androidx.compose.runtime.mutableFloatStateOf(innerWallVertices.getOrNull(idx)?.x ?: 0f) }
                    var virtualMapY by androidx.compose.runtime.remember(idx, innerWallVertices.getOrNull(idx)) { androidx.compose.runtime.mutableFloatStateOf(innerWallVertices.getOrNull(idx)?.y ?: 0f) }
                    
                    ExtrudeHandleView(
                        x = handlePxX,
                        y = handlePxY,
                        dens = dens,
                        isEndNode = isEndNode,
                        isSnapped = isOnBoundary,
                        color = if (idx == targetVertices.lastIndex) Color(0xFFFF5722) else Color.Red,
                        onCenterDragStart = {
                            val startPos = innerWallVertices[idx]
                            virtualMapX = startPos.x
                            virtualMapY = startPos.y
                            isDraggingHandle = true
                            dragPreviewVertices = innerWallVertices
                        },
                        onCenterDrag = { dragAmount ->
                            virtualMapX += dragAmount.x / dens
                            virtualMapY += dragAmount.y / dens

                            var snapped = PointF(virtualMapX, virtualMapY)
                            // 1D Constraint if on boundary
                            if (isEndNode && effectiveBoundary.size >= 2) {
                                val startPos = innerWallVertices[idx]
                                var isCurrentlyOnBoundary = false
                                var boundP1: PointF? = null
                                var boundP2: PointF? = null
                                
                                for (i in effectiveBoundary.indices) {
                                    val p1 = effectiveBoundary[i]
                                    val p2 = effectiveBoundary[(i + 1) % effectiveBoundary.size]
                                    val minX = kotlin.math.min(p1.x, p2.x) - 1f
                                    val maxX = kotlin.math.max(p1.x, p2.x) + 1f
                                    val minY = kotlin.math.min(p1.y, p2.y) - 1f
                                    val maxY = kotlin.math.max(p1.y, p2.y) + 1f
                                    if (startPos.x in minX..maxX && startPos.y in minY..maxY) {
                                        if (p1.x == p2.x && kotlin.math.abs(startPos.x - p1.x) < 1f) {
                                            isCurrentlyOnBoundary = true; boundP1 = p1; boundP2 = p2; break
                                        }
                                        if (p1.y == p2.y && kotlin.math.abs(startPos.y - p1.y) < 1f) {
                                            isCurrentlyOnBoundary = true; boundP1 = p1; boundP2 = p2; break
                                        }
                                    }
                                }
                                
                                if (isCurrentlyOnBoundary && boundP1 != null && boundP2 != null) {
                                    // Calculate tear-off distance
                                    val distToLine = if (boundP1.x == boundP2.x) kotlin.math.abs(virtualMapX - boundP1.x) else kotlin.math.abs(virtualMapY - boundP1.y)
                                    if (distToLine < 30f) { // Tear-off threshold
                                        if (boundP1.x == boundP2.x) {
                                            val minY = kotlin.math.min(boundP1.y, boundP2.y)
                                            val maxY = kotlin.math.max(boundP1.y, boundP2.y)
                                            snapped = PointF(boundP1.x, virtualMapY.coerceIn(minY, maxY))
                                        } else {
                                            val minX = kotlin.math.min(boundP1.x, boundP2.x)
                                            val maxX = kotlin.math.max(boundP1.x, boundP2.x)
                                            snapped = PointF(virtualMapX.coerceIn(minX, maxX), boundP1.y)
                                        }
                                    }
                                }
                            } else if (idx > 0 && idx < innerWallVertices.lastIndex) {
                                val prev = innerWallVertices[idx - 1]
                                val ddx = kotlin.math.abs(virtualMapX - prev.x)
                                val ddy = kotlin.math.abs(virtualMapY - prev.y)
                                snapped = if (ddx > ddy) PointF(virtualMapX, prev.y) else PointF(prev.x, virtualMapY)
                            }
                            
                            val verts = innerWallVertices.toMutableList()
                            if (idx < verts.size) verts[idx] = snapped
                            dragPreviewVertices = verts
                        },
                        onCenterDragEnd = {
                            isDraggingHandle = false
                            dragPreviewVertices?.let { preview ->
                                if (idx < preview.size) onInnerWallVertexMove(idx, preview[idx])
                            }
                        },
                        onExtrudeDragStart = {
                            activeDragStartIdx = idx
                            activeDragStartPoint = targetVertices[idx]
                            activeDragCurrentPoint = targetVertices[idx]
                        },
                        onExtrudeDrag = { dragAmount ->
                            if (activeDragStartPoint == null || activeDragCurrentPoint == null) return@ExtrudeHandleView
                            
                            val startP = activeDragStartPoint!!
                            var vx = activeDragCurrentPoint!!.x + dragAmount.x / dens
                            var vy = activeDragCurrentPoint!!.y + dragAmount.y / dens
                            
                            val dx = kotlin.math.abs(vx - startP.x)
                            val dy = kotlin.math.abs(vy - startP.y)
                            if (dx > dy) vy = startP.y else vx = startP.x
                            var currentP = PointF(vx, vy)
                            
                            // Check boundary snap
                            var wasSnappedToBoundary = false
                            if (effectiveBoundary.size >= 2) {
                                for (i in effectiveBoundary.indices) {
                                    val p1 = effectiveBoundary[i]
                                    val p2 = effectiveBoundary[(i + 1) % effectiveBoundary.size]
                                    
                                    val minX = kotlin.math.min(p1.x, p2.x) - 1f
                                    val maxX = kotlin.math.max(p1.x, p2.x) + 1f
                                    val minY = kotlin.math.min(p1.y, p2.y) - 1f
                                    val maxY = kotlin.math.max(p1.y, p2.y) + 1f
                                    
                                    // Distance to segment
                                    val dist = if (p1.x == p2.x) kotlin.math.abs(currentP.x - p1.x) else kotlin.math.abs(currentP.y - p1.y)
                                    if (dist < 50f) { // Snap radius
                                        val withinSegment = if (p1.x == p2.x) currentP.y in minY..maxY else currentP.x in minX..maxX
                                        if (withinSegment) {
                                            if (p1.x == p2.x) currentP = PointF(p1.x, currentP.y)
                                            else currentP = PointF(currentP.x, p1.y)
                                            wasSnappedToBoundary = true
                                            break
                                        }
                                    }
                                }
                            }
                            
                            activeDragCurrentPoint = currentP
                            activeDragWasSnapped = wasSnappedToBoundary
                        },
                        onExtrudeDragEnd = {
                            var wasClosed = false
                            if (activeDragStartPoint != null && activeDragCurrentPoint != null && !activeDragIsInvalid) {
                                val prepend = activeDragStartIdx == 0
                                onDrawingVertexAdd(activeDragCurrentPoint!!, prepend)
                                if (activeDragWasSnapped) wasClosed = true
                            }
                            activeDragStartPoint = null
                            activeDragCurrentPoint = null
                            activeDragIsInvalid = false
                            activeDragStartIdx = null
                            activeDragWasSnapped = false
                            
                            if (wasClosed) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                // Inner wall snaps hide arrows but stay editable until Done button.
                            }
                        }
                    )
                }
            }

            // Walk Path Recording mode: draggable handles on top of the canvas
            if (isRecordingWalkPath) {
                val dens = density
                walkPath.forEachIndexed { idx, v ->
                    val handlePxX = v.x * dens
                    val handlePxY = v.y * dens
                    Box(
                        modifier = Modifier
                            .offset {
                                androidx.compose.ui.unit.IntOffset(
                                    (handlePxX - 12f * dens).toInt(),
                                    (handlePxY - 12f * dens).toInt()
                                )
                            }
                            .size(24.dp)
                            .background(Color(0xFFFFD600), shape = androidx.compose.foundation.shape.CircleShape)
                            .pointerInput(idx) {
                                var virtualMapX = 0f
                                var virtualMapY = 0f
                                detectDragGestures(
                                    onDragStart = {
                                        val startPos = currentWalkPath[idx]
                                        virtualMapX = startPos.x
                                        virtualMapY = startPos.y
                                    },
                                    onDragEnd = { /* no-op */ }
                                ) { change, dragAmount ->
                                    change.consume()
                                    virtualMapX += dragAmount.x / dens
                                    virtualMapY += dragAmount.y / dens
                                    onWalkPathPointMove(idx, PointF(virtualMapX, virtualMapY))
                                }
                            }
                    )
                }
            }

            val isObjectsInteractive = isEditing && !isDrawingBoundary && !isRecordingWalkPath
            val sortedObjects = mapObjects.sortedBy {
                if (it.type == MapObjectType.WINDOW || it.type == MapObjectType.DOOR_LEFT || it.type == MapObjectType.DOOR_RIGHT) 1 else 0
            }
            sortedObjects.forEach { obj ->
                val forceSnapToBoundary = { currentObj: MapObject ->
                    val allWallSegments = mutableListOf<Pair<PointF, PointF>>()
                    if (effectiveBoundary.size >= 2) {
                        for (i in effectiveBoundary.indices) {
                            allWallSegments.add(effectiveBoundary[i] to effectiveBoundary[(i + 1) % effectiveBoundary.size])
                        }
                    }
                    innerWalls.forEach { wall ->
                        for (i in 0 until wall.size - 1) {
                            allWallSegments.add(wall[i] to wall[i + 1])
                        }
                    }
                    if (allWallSegments.isNotEmpty()) {
                        val objWidth = currentObj.width * currentObj.scale
                        val objHeight = currentObj.height * currentObj.scale
                        val cx = currentObj.x + objWidth / 2f
                        val cy = currentObj.y + objHeight / 2f
                        var bestDistSq = Float.MAX_VALUE
                        var bestPos = PointF(currentObj.x, currentObj.y)
                        var bestRot = currentObj.rotation
                        var bestWidth = currentObj.width
                        var bestHeight = currentObj.height
                        for ((p1, p2) in allWallSegments) {
                            val dx = p2.x - p1.x
                            val dy = p2.y - p1.y
                            val lenSq = dx*dx + dy*dy
                            if (lenSq == 0f) continue
                            val len = kotlin.math.sqrt(lenSq.toDouble()).toFloat()
                            val t = ((cx - p1.x)*dx + (cy - p1.y)*dy) / lenSq
                            val ct = t.coerceIn(0f, 1f)
                            val nx = p1.x + ct * dx
                            val ny = p1.y + ct * dy
                            val distSq = (cx - nx)*(cx - nx) + (cy - ny)*(cy - ny)
                            if (distSq < bestDistSq) {
                                bestDistSq = distSq
                                if (currentObj.type == MapObjectType.WINDOW) {
                                    bestRot = (kotlin.math.atan2(dy, dx) * 180f / kotlin.math.PI.toFloat())
                                    val rem = bestRot % 180f
                                    val isOrthogonalSwap = (kotlin.math.abs(rem) == 90f || kotlin.math.abs(rem) == 270f)
                                    val defaultDims = currentObj.type.getDefaultDimensions()
                                    if (isOrthogonalSwap) {
                                        bestWidth = defaultDims.second
                                        bestHeight = defaultDims.first
                                    } else {
                                        bestWidth = defaultDims.first
                                        bestHeight = defaultDims.second
                                    }
                                    bestPos = PointF(nx - (bestWidth * currentObj.scale) / 2f, ny - (bestHeight * currentObj.scale) / 2f)
                                } else {
                                    val N = PointF(-dy/len, dx/len)
                                    val dot = (cx - nx) * N.x + (cy - ny) * N.y
                                    val usePosNormal = dot > 0
                                    
                                    bestRot = if (usePosNormal) kotlin.math.atan2(-dy, -dx) * 180f / kotlin.math.PI.toFloat() else kotlin.math.atan2(dy, dx) * 180f / kotlin.math.PI.toFloat()
                                    val rem = bestRot % 180f
                                    val isOrthogonalSwap = (kotlin.math.abs(rem) == 90f || kotlin.math.abs(rem) == 270f)
                                    val defaultDims = currentObj.type.getDefaultDimensions()
                                    if (isOrthogonalSwap) {
                                        bestWidth = defaultDims.second
                                        bestHeight = defaultDims.first
                                    } else {
                                        bestWidth = defaultDims.first
                                        bestHeight = defaultDims.second
                                    }
                                    
                                    val scaledWidth = bestWidth * currentObj.scale
                                    val scaledHeight = bestHeight * currentObj.scale
                                    
                                    val finalCx = if (usePosNormal) nx + N.x * scaledHeight / 2f else nx - N.x * scaledHeight / 2f
                                    val finalCy = if (usePosNormal) ny + N.y * scaledHeight / 2f else ny - N.y * scaledHeight / 2f
                                    bestPos = PointF(finalCx - scaledWidth / 2f, finalCy - scaledHeight / 2f)
                                }
                            }
                        }
                        onObjectUpdate(currentObj.copy(x = bestPos.x, y = bestPos.y, rotation = bestRot, width = bestWidth, height = bestHeight))
                    }
                }

                MapObjectView(
                        obj = obj,
                        isInteractive = isObjectsInteractive,
                        isSelected = (selectedObjectId == obj.id),
                        mapScale = mapScale,
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
                                val isBoundaryObject = obj.type == MapObjectType.WINDOW || obj.type == MapObjectType.DOOR_LEFT || obj.type == MapObjectType.DOOR_RIGHT
                                if (!isBoundaryObject) {
                                    if (!BoundaryUtils.rectInPolygon(px, py, objWidth, objHeight, effectiveBoundary)) return false
                                    val intersectsInnerWall = innerWalls.any { wall ->
                                        (0 until wall.size - 1).any { i ->
                                            BoundaryUtils.rectIntersectsLine(px, py, objWidth, objHeight, wall[i], wall[i+1])
                                        }
                                    }
                                    if (intersectsInnerWall) return false
                                } else {
                                    return true
                                }
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
                                val isBoundaryObject = obj.type == MapObjectType.WINDOW || obj.type == MapObjectType.DOOR_LEFT || obj.type == MapObjectType.DOOR_RIGHT
                                val allWallSegments = mutableListOf<Pair<PointF, PointF>>()
                                if (effectiveBoundary.size >= 2) {
                                    for (i in effectiveBoundary.indices) {
                                        allWallSegments.add(effectiveBoundary[i] to effectiveBoundary[(i + 1) % effectiveBoundary.size])
                                    }
                                }
                                innerWalls.forEach { wall ->
                                    for (i in 0 until wall.size - 1) {
                                        allWallSegments.add(wall[i] to wall[i + 1])
                                    }
                                }
                                
                                if (isBoundaryObject && allWallSegments.isNotEmpty()) {
                                    val cx = finalX + objWidth / 2f
                                    val cy = finalY + objHeight / 2f
                                    var bestDistSq = Float.MAX_VALUE
                                    var bestPos = PointF(finalX, finalY)
                                    var bestRot = obj.rotation
                                    var bestWidth = obj.width
                                    var bestHeight = obj.height
                                    for ((p1, p2) in allWallSegments) {
                                        val dx = p2.x - p1.x
                                        val dy = p2.y - p1.y
                                        val lenSq = dx*dx + dy*dy
                                        if (lenSq == 0f) continue
                                        val len = kotlin.math.sqrt(lenSq.toDouble()).toFloat()
                                        val t = ((cx - p1.x)*dx + (cy - p1.y)*dy) / lenSq
                                        val ct = t.coerceIn(0f, 1f)
                                        val nx = p1.x + ct * dx
                                        val ny = p1.y + ct * dy
                                        val distSq = (cx - nx)*(cx - nx) + (cy - ny)*(cy - ny)
                                        if (distSq < bestDistSq) {
                                            bestDistSq = distSq
                                            if (obj.type == MapObjectType.WINDOW) {
                                                bestRot = (kotlin.math.atan2(dy, dx) * 180f / kotlin.math.PI.toFloat())
                                                val rem = bestRot % 180f
                                                val isOrthogonalSwap = (kotlin.math.abs(rem) == 90f || kotlin.math.abs(rem) == 270f)
                                                val defaultDims = obj.type.getDefaultDimensions()
                                                if (isOrthogonalSwap) {
                                                    bestWidth = defaultDims.second
                                                    bestHeight = defaultDims.first
                                                } else {
                                                    bestWidth = defaultDims.first
                                                    bestHeight = defaultDims.second
                                                }
                                                bestPos = PointF(nx - (bestWidth * obj.scale) / 2f, ny - (bestHeight * obj.scale) / 2f)
                                            } else {
                                                val N = PointF(-dy/len, dx/len)
                                                val dot = (cx - nx) * N.x + (cy - ny) * N.y
                                                val usePosNormal = dot > 0
                                                
                                                bestRot = if (usePosNormal) kotlin.math.atan2(-dy, -dx) * 180f / kotlin.math.PI.toFloat() else kotlin.math.atan2(dy, dx) * 180f / kotlin.math.PI.toFloat()
                                                val rem = bestRot % 180f
                                                val isOrthogonalSwap = (kotlin.math.abs(rem) == 90f || kotlin.math.abs(rem) == 270f)
                                                val defaultDims = obj.type.getDefaultDimensions()
                                                if (isOrthogonalSwap) {
                                                    bestWidth = defaultDims.second
                                                    bestHeight = defaultDims.first
                                                } else {
                                                    bestWidth = defaultDims.first
                                                    bestHeight = defaultDims.second
                                                }
                                                
                                                val scaledWidth = bestWidth * obj.scale
                                                val scaledHeight = bestHeight * obj.scale
                                                
                                                val finalCx = if (usePosNormal) nx + N.x * scaledHeight / 2f else nx - N.x * scaledHeight / 2f
                                                val finalCy = if (usePosNormal) ny + N.y * scaledHeight / 2f else ny - N.y * scaledHeight / 2f
                                                bestPos = PointF(finalCx - scaledWidth / 2f, finalCy - scaledHeight / 2f)
                                            }
                                        }
                                    }
                                    if (bestDistSq < 10000f) {
                                        onObjectUpdate(obj.copy(x = bestPos.x, y = bestPos.y, rotation = bestRot, width = bestWidth, height = bestHeight))
                                    } else {
                                        onObjectMove(obj.id, finalX - obj.x, finalY - obj.y)
                                    }
                                } else {
                                    onObjectMove(obj.id, finalX - obj.x, finalY - obj.y)
                                }
                            }
                        },
                        onDragEnd = {
                            val currentObj = mapObjects.find { it.id == obj.id }
                            if (currentObj != null) {
                                val isBoundaryObject = currentObj.type == MapObjectType.WINDOW || currentObj.type == MapObjectType.DOOR_LEFT || currentObj.type == MapObjectType.DOOR_RIGHT
                                if (isBoundaryObject) {
                                    forceSnapToBoundary(currentObj)
                                }
                            }
                        }
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
                MapObjectType.BOOKSHELF -> R.drawable.bookshelf
                MapObjectType.DINING_CHAIR -> R.drawable.dining_chair
                MapObjectType.KITCHEN_ISLAND -> R.drawable.kitchen_island
                MapObjectType.LARGE_INDOOR_PLANT -> R.drawable.large_indoor_plant
                MapObjectType.OFFICE_CHAIR -> R.drawable.office_chair
                MapObjectType.OFFICE_DESK -> R.drawable.office_desk
                MapObjectType.PIANO -> R.drawable.piano
                MapObjectType.RECTANGULAR_COFFEE_TABLE -> R.drawable.rectangular_coffee_table
                MapObjectType.RECTANGULAR_DINING_TABLE -> R.drawable.rectangular_dining_table
                MapObjectType.ROUND_DINING_TABLE -> R.drawable.round_dining_table
                MapObjectType.SHOWER_CABIN -> R.drawable.shower_cabin
                MapObjectType.SINGLE_SOFA -> R.drawable.single_sofa
                MapObjectType.STOVE_COUNTER -> R.drawable.stove_counter
                MapObjectType.DRESSING_TABLE -> R.drawable.dressing_table
                MapObjectType.TRIPLE_SOFA -> R.drawable.triple_sofa
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
        mapScale: Float,
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
        val defaultDims = obj.type.getDefaultDimensions()
        val isBoundaryObject = obj.type == MapObjectType.WINDOW || obj.type == MapObjectType.DOOR_LEFT || obj.type == MapObjectType.DOOR_RIGHT

        val rem = obj.rotation % 180f
        val isOrthogonalSwap = (rem == 90f || rem == -90f)
        
        val imageWidth = if (isBoundaryObject) defaultDims.first else if (isOrthogonalSwap) obj.height else obj.width
        val imageHeight = if (isBoundaryObject) defaultDims.second else if (isOrthogonalSwap) obj.width else obj.height

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
                                    .requiredSize(24.dp),
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
                                    .requiredSize(24.dp),
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

@Composable
fun ExtrudeHandleView(
    x: Float, // Map px
    y: Float, // Map px
    dens: Float,
    isEndNode: Boolean,
    isSnapped: Boolean,
    color: Color = Color.Red,
    onCenterDragStart: () -> Unit,
    onCenterDrag: (Offset) -> Unit,
    onCenterDragEnd: () -> Unit,
    onExtrudeDragStart: () -> Unit,
    onExtrudeDrag: (Offset) -> Unit,
    onExtrudeDragEnd: () -> Unit
) {
    val currentOnCenterDragStart by androidx.compose.runtime.rememberUpdatedState(onCenterDragStart)
    val currentOnCenterDrag by androidx.compose.runtime.rememberUpdatedState(onCenterDrag)
    val currentOnCenterDragEnd by androidx.compose.runtime.rememberUpdatedState(onCenterDragEnd)
    val currentOnExtrudeDragStart by androidx.compose.runtime.rememberUpdatedState(onExtrudeDragStart)
    val currentOnExtrudeDrag by androidx.compose.runtime.rememberUpdatedState(onExtrudeDrag)
    val currentOnExtrudeDragEnd by androidx.compose.runtime.rememberUpdatedState(onExtrudeDragEnd)

    val handleSize = 64.dp // Large hit area for extrude arrows
    val centerNodeSize = 24.dp
    
    Box(
        modifier = Modifier
            .offset { 
                androidx.compose.ui.unit.IntOffset(
                    (x - 32f * dens).toInt(), 
                    (y - 32f * dens).toInt()
                ) 
            }
            .size(handleSize)
    ) {
        // Extrude area (arrows)
        if (isEndNode && !isSnapped) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { currentOnExtrudeDragStart() },
                            onDragEnd = { currentOnExtrudeDragEnd() },
                            onDragCancel = { currentOnExtrudeDragEnd() }
                        ) { change, dragAmount ->
                            change.consume()
                            currentOnExtrudeDrag(dragAmount)
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cw = size.width
                    val ch = size.height
                    val cx = cw / 2f
                    val cy = ch / 2f
                    val arrowDist = 18.dp.toPx()
                    val arrowSize = 6.dp.toPx()
                    
                    val arrowColor = Color(0xFF4CAF50) // Green for extrude
                    
                    // Helper to draw a triangle arrow pointing outwards
                    fun drawArrow(px: Float, py: Float, angleDegrees: Float) {
                        val path = Path()
                        path.moveTo(px, py)
                        val angleRad = Math.toRadians(angleDegrees.toDouble())
                        // The base of the arrow
                        val leftX = px - arrowSize * kotlin.math.cos(angleRad + Math.PI/2).toFloat()
                        val leftY = py - arrowSize * kotlin.math.sin(angleRad + Math.PI/2).toFloat()
                        val rightX = px - arrowSize * kotlin.math.cos(angleRad - Math.PI/2).toFloat()
                        val rightY = py - arrowSize * kotlin.math.sin(angleRad - Math.PI/2).toFloat()
                        val tipX = px + arrowSize * 1.5f * kotlin.math.cos(angleRad).toFloat()
                        val tipY = py + arrowSize * 1.5f * kotlin.math.sin(angleRad).toFloat()
                        
                        path.moveTo(tipX, tipY)
                        path.lineTo(leftX, leftY)
                        path.lineTo(rightX, rightY)
                        path.close()
                        drawPath(path, arrowColor)
                    }
                    
                    // Top, Bottom, Left, Right
                    drawArrow(cx, cy - arrowDist, -90f)
                    drawArrow(cx, cy + arrowDist, 90f)
                    drawArrow(cx - arrowDist, cy, 180f)
                    drawArrow(cx + arrowDist, cy, 0f)
                }
            }
        }
        
        // Center node
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(centerNodeSize)
                .background(color, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { currentOnCenterDragStart() },
                        onDragEnd = { currentOnCenterDragEnd() },
                        onDragCancel = { currentOnCenterDragEnd() }
                    ) { change, dragAmount ->
                        change.consume()
                        currentOnCenterDrag(dragAmount)
                    }
                }
        )
    }
}
