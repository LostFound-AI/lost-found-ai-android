package com.example.lostfoundai.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.sp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.activity.compose.BackHandler
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.lostfoundai.R
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
import com.example.lostfoundai.ui.theme.ErrorRed
import com.example.lostfoundai.ui.theme.MapBackground
import com.example.lostfoundai.ui.theme.OnSurfaceColor
import com.example.lostfoundai.ui.theme.OnSurfaceVariantColor
import com.example.lostfoundai.ui.theme.PrimaryContainer
import com.example.lostfoundai.ui.theme.PrimaryIndigo
import com.example.lostfoundai.ui.theme.SurfaceColor
import com.example.lostfoundai.ui.theme.WarnYellow
import com.example.lostfoundai.ui.components.Toolbar
import com.example.lostfoundai.utils.BoundaryUtils

@OptIn(ExperimentalMaterial3Api::class)
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
    var showSearchSheet by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(true) }
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
    val sizes = when (selectedCategory) {
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

    BackHandler(enabled = showSearchSheet) {
        keyboardController?.hide()
        focusManager.clearFocus()
        showSearchSheet = false
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

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(Modifier.height(32.dp))
                Text(
                    "應用程式設定",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                        Text("清除路線紀錄 (${walkPath.size} 個點)")
                    }
                }
                HorizontalDivider()
                TextButton(
                    onClick = { showClearRoomDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("清除房間", color = MaterialTheme.colorScheme.error)
                }
                TextButton(
                    onClick = onNavigateHome,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("返回主頁")
                }
            }
        }
    ) {
        if (showClearRoomDialog) {
            AlertDialog(
                onDismissRequest = { showClearRoomDialog = false },
                title = { Text("確認清除房間") },
                text = { Text("您確定要清除所有房間物件並重設邊界嗎？此操作無法復原。") },
                confirmButton = {
                    TextButton(onClick = {
                        mapViewModel.clearRoom()
                        showClearRoomDialog = false
                    }) {
                        Text("確認刪除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearRoomDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }

        Scaffold { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .onGloballyPositioned { coords ->
                        contentRootOffset = coords.positionInRoot()
                    }
            ) {
                // Main Map Area (Full Size)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MapBackground)
                ) {
                    MapCanvas(
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
                                isDrawingBoundary -> drawingVertices = drawingVertices + tapOffsetDp
                                isSpecifyingLocation -> {
                                    specifiedLocation = tapOffsetDp
                                    isSpecifyingLocation = false
                                    showSearchSheet = true
                                }
                                isRecordingWalkPath -> mapViewModel.addWalkPathPoint(tapOffsetDp.x, tapOffsetDp.y)
                            }
                        },
                        onObjectMove = { id, dx, dy ->
                            val obj = mapObjects.find { it.id == id }
                            if (obj != null) {
                                mapViewModel.updateMapObject(obj.copy(x = obj.x + dx, y = obj.y + dy))
                            }
                        },
                        onDeleteObject = { id ->
                            mapViewModel.removeMapObject(id)
                        },
                        gridEnabled = gridEnabled
                    )
                }

                // Toolbar as Overlay
                if (isEditing) {
                    Box(
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Toolbar(
                            onDragStart = { type, offset ->
                                draggingType = type
                                draggingPos = offset
                            },
                            onDrag = { dragAmount ->
                                draggingPos += dragAmount
                            },
                            onDragEnd = { droppedType ->
                                // Always place at the ghost's displayed position
                                if (hasValidGhostPos) {
                                    mapViewModel.addMapObject(
                                        MapObject(
                                            type = droppedType,
                                            x = lastValidGhostX,
                                            y = lastValidGhostY,
                                            width = droppedType.defaultWidth(),
                                            height = droppedType.defaultHeight()
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
                    val rawWidth = draggingType!!.defaultWidth()
                    val rawHeight = draggingType!!.defaultHeight()
                    
                    // Compute effective boundary vertices for polygon check
                    val effectiveBoundary = if (roomBoundary.vertices.isNotEmpty()) roomBoundary.vertices else {
                        val pad = 32f
                        val cw = canvasSize.width / density
                        val ch = canvasSize.height / density
                        listOf(PointF(pad, pad), PointF(cw - pad, pad), PointF(cw - pad, ch - pad), PointF(pad, ch - pad))
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
                    
                    // Clamp to polygon boundary
                    var ghostX = virtualXStr
                    var ghostY = virtualYStr
                    val clampedPos = BoundaryUtils.clampRectToPolygon(
                        ghostX, ghostY, rawWidth, rawHeight,
                        lastValidGhostX, lastValidGhostY, hasValidGhostPos,
                        effectiveBoundary
                    )
                    ghostX = clampedPos.x
                    ghostY = clampedPos.y
                    
                    // Per-axis collision sliding with edge-snapping against existing objects
                    // 1. Resolve X axis first (using lastValidGhostY for Y to isolate X movement)
                    val refY = if (hasValidGhostPos) lastValidGhostY else ghostY
                    val collidingObjX = mapObjects.find { other ->
                        ghostX < other.x + other.width && ghostX + rawWidth > other.x &&
                        refY < other.y + other.height && refY + rawHeight > other.y
                    }
                    if (collidingObjX != null && hasValidGhostPos) {
                        val pushLeft = collidingObjX.x - rawWidth
                        val pushRight = collidingObjX.x + collidingObjX.width
                        ghostX = if (kotlin.math.abs(ghostX - pushLeft) < kotlin.math.abs(ghostX - pushRight)) pushLeft else pushRight
                        // Re-clamp to polygon after snap
                        if (!BoundaryUtils.rectInPolygon(ghostX, ghostY, rawWidth, rawHeight, effectiveBoundary)) {
                            ghostX = clampedPos.x
                        }
                    }
                    
                    // 2. Resolve Y axis (using resolved ghostX)
                    val collidingObjY = mapObjects.find { other ->
                        ghostX < other.x + other.width && ghostX + rawWidth > other.x &&
                        ghostY < other.y + other.height && ghostY + rawHeight > other.y
                    }
                    if (collidingObjY != null && hasValidGhostPos) {
                        val pushUp = collidingObjY.y - rawHeight
                        val pushDown = collidingObjY.y + collidingObjY.height
                        ghostY = if (kotlin.math.abs(ghostY - pushUp) < kotlin.math.abs(ghostY - pushDown)) pushUp else pushDown
                        // Re-clamp to polygon after snap
                        if (!BoundaryUtils.rectInPolygon(ghostX, ghostY, rawWidth, rawHeight, effectiveBoundary)) {
                            ghostY = clampedPos.y
                        }
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
                        modifier = Modifier
                            .offset { androidx.compose.ui.unit.IntOffset(
                                renderOffsetX.toInt(),
                                renderOffsetY.toInt()
                            ) }
                            .size((rawWidth * mapScale).dp, (rawHeight * mapScale).dp)
                            .graphicsLayer { alpha = 0.7f }
                    ) {
                        MapObjectVisuals(
                            type = draggingType!!,
                            isBed = draggingType == MapObjectType.BED,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Buttons Overlay
                if (isDrawingBoundary) {
                    // Drawing mode controls
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
                            ) {
                                Text("清除重畫")
                            }
                            Button(
                                onClick = {
                                    if (drawingVertices.size >= 3) {
                                        pendingBoundaryVertices = drawingVertices
                                        showNameDialog = true
                                    }
                                    isDrawingBoundary = false
                                    drawingVertices = emptyList()
                                },
                                enabled = drawingVertices.size >= 3,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
                            ) {
                                Text("完成 (${drawingVertices.size} 點)")
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
                        TextButton(
                            onClick = {
                                isDrawingBoundary = false
                                drawingVertices = emptyList()
                            }
                        ) {
                            Text("取消繪製", color = Color.White)
                        }
                    }
                } else if (isEditing) {
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
                            ) {
                                Text("設定邊界")
                            }
                            Button(
                                onClick = { isEditing = false },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
                            ) {
                                Text("完成編輯")
                            }
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
                                "路線記錄中，點擊地圖加入路徑點",
                                color = Color(0xFF1A1C2E),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                // Map legend (bottom-right, only in normal view)
                if (!isDrawingBoundary && !isEditing && (predictedSpots.isNotEmpty() || walkPath.isNotEmpty() || searchItems.any { it.manualX != null })) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 12.dp, bottom = 90.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        color = SurfaceColor.copy(alpha = 0.92f),
                        shadowElevation = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (walkPath.isNotEmpty())
                                LegendRow(Color(0xFFFFD600), "行走路線")
                            if (searchItems.any { it.manualX != null })
                                LegendRow(Color(0xFF4CAF50), "已存放位置")
                            if (predictedSpots.isNotEmpty())
                                LegendRow(Color(0xFFE53935), "AI 預測")
                        }
                    }
                }

                // Settings button (Available in all modes except drawing)
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
                    // Bottom centre — 新增物品 | 尋找 | clear-predictions X
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
                                    Icon(Icons.Filled.Close, contentDescription = "清除標記",
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
                            ) {
                                Text("新增物品", fontSize = 14.sp)
                            }
                            FilledTonalButton(
                                onClick = {
                                    showHistory = true
                                    showSearchSheet = true
                                },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
                            ) {
                                Text("尋找", fontSize = 14.sp)
                            }
                        }
                    }

                    // Top right edit icon
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
        ModalBottomSheet(
            onDismissRequest = { showSearchSheet = false },
            sheetState = sheetState,
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                if (showHistory) {
// ... History View ...
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("查詢紀錄", style = MaterialTheme.typography.titleLarge, color = Color.Black)
                        IconButton(onClick = { showHistory = false }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回表單",
                                tint = PrimaryIndigo
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
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
                                    Column(modifier = Modifier.weight(1f).clickable {
                                        if (item.manualX != null && item.manualY != null) {
                                            mapViewModel.clearPredictedSpots()
                                            specifiedLocation = PointF(item.manualX, item.manualY)
                                        } else {
                                            mapViewModel.startAIPrediction(item.id)
                                            specifiedLocation = null
                                        }
                                        showSearchSheet = false
                                    }) {
                                        Text(
                                            text = item.name + if (item.manualX != null && item.manualY != null) " (已指定)" else "",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "類別: ${item.category.name} | 大小: ${getSizeDisplay(item.category, item.size)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            // Edit Item
                                            editingItem = item
                                            itemName = item.name
                                            // Simple mapping, fallback to custom
                                            val mappedCategory = when(item.category) {
                                                com.example.lostfoundai.data.ItemCategory.ACCESSORY -> "飾品"
                                                com.example.lostfoundai.data.ItemCategory.BELONGINGS -> "隨身物品"
                                                com.example.lostfoundai.data.ItemCategory.ELECTRONICS -> "電子產品"
                                                com.example.lostfoundai.data.ItemCategory.PAPER -> "紙本"
                                                com.example.lostfoundai.data.ItemCategory.BATHROOM -> "衛浴用品"
                                                com.example.lostfoundai.data.ItemCategory.APPLIANCE -> "家電配件"
                                                else -> "自訂"
                                            }
                                            selectedCategory = mappedCategory
                                            
                                            val mappedSize = when(item.size) {
                                                com.example.lostfoundai.data.ItemSize.VERY_SMALL -> "極小 (< 2cm)"
                                                com.example.lostfoundai.data.ItemSize.SMALL -> "小 (5-10cm)"
                                                com.example.lostfoundai.data.ItemSize.MEDIUM -> "中 (15-20cm)"
                                                com.example.lostfoundai.data.ItemSize.LARGE -> "大 (> 20cm)"
                                            }
                                            selectedSize = mappedSize
                                            
                                            specifiedLocation = if (item.manualX != null && item.manualY != null) PointF(item.manualX, item.manualY) else null
                                            showHistory = false
                                        }) {
                                            Text("✏️", style = MaterialTheme.typography.bodyMedium)
                                        }
                                        IconButton(onClick = {
                                            searchViewModel.deleteItem(item.id)
                                        }) {
                                            Text("🗑️", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    // --- Form View ---

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = { 
                                showHistory = true
                                editingItem = null // reset edit state when returning to history
                                itemName = ""
                                specifiedLocation = null
                            },
                            modifier = Modifier.fillMaxWidth(0.5f)
                        ) {
                            Text("查詢紀錄", color = Color.White)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("物品名稱", color = Color(0xFF2A2A2A)) },
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                        }),
                        modifier = Modifier.fillMaxWidth()
                    )
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
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
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
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }),
                            modifier = Modifier.fillMaxWidth()
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
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sizeExpanded) },
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
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
                            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    if (specifiedLocation == null) {
                        Text(
                            text = "若沒有指定位置將會預設為 AI 尋找",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(bottom = 8.dp).align(Alignment.CenterHorizontally)
                        )
                    }

                    if (specifiedLocation != null) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { specifiedLocation = null },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("取消指定", color = Color(0xFF1A1A1A))
                            }
                            OutlinedButton(
                                onClick = {
                                    showSearchSheet = false
                                    isSpecifyingLocation = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("重新指定", color = Color(0xFF1A1A1A))
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                showSearchSheet = false
                                isSpecifyingLocation = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("指定位置", color = Color(0xFF1A1A1A))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val cat = when(selectedCategory) {
                                "飾品" -> com.example.lostfoundai.data.ItemCategory.ACCESSORY
                                "隨身物品" -> com.example.lostfoundai.data.ItemCategory.BELONGINGS
                                "電子產品" -> com.example.lostfoundai.data.ItemCategory.ELECTRONICS
                                "紙本" -> com.example.lostfoundai.data.ItemCategory.PAPER
                                "衛浴用品" -> com.example.lostfoundai.data.ItemCategory.BATHROOM
                                "家電配件" -> com.example.lostfoundai.data.ItemCategory.APPLIANCE
                                "衣物" -> com.example.lostfoundai.data.ItemCategory.CLOTHING
                                else -> com.example.lostfoundai.data.ItemCategory.OTHER
                            }
                            val sz = when {
                                selectedSize.startsWith("極小") -> com.example.lostfoundai.data.ItemSize.VERY_SMALL
                                selectedSize.startsWith("小") -> com.example.lostfoundai.data.ItemSize.SMALL
                                selectedSize.startsWith("中") -> com.example.lostfoundai.data.ItemSize.MEDIUM
                                selectedSize.startsWith("大") -> com.example.lostfoundai.data.ItemSize.LARGE
                                else -> com.example.lostfoundai.data.ItemSize.MEDIUM
                            }
                            
                            if (editingItem != null) {
                                searchViewModel.updateItem(editingItem!!.copy(
                                    name = itemName.ifEmpty { "新物品" },
                                    category = cat,
                                    size = sz,
                                    manualX = specifiedLocation?.x,
                                    manualY = specifiedLocation?.y
                                ))
                            } else {
                                searchViewModel.addMissingItem(
                                    name = itemName.ifEmpty { "新物品" },
                                    category = cat,
                                    size = sz,
                                    traits = "自訂特徵",
                                    weight = "Medium",
                                    location = "地圖",
                                    manualX = specifiedLocation?.x,
                                    manualY = specifiedLocation?.y
                                )
                            }
                            editingItem = null
                            specifiedLocation = null
                            itemName = ""
                            showHistory = true
                        },
                        modifier = Modifier.fillMaxWidth(0.6f).align(Alignment.CenterHorizontally)
                    ) {
                        Text(if (editingItem != null) "完成編輯" else "完成新增", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Boundary Shape Selection Sheet
    if (showBoundarySheet) {
        ModalBottomSheet(
            onDismissRequest = { showBoundarySheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = (canvasSize.height / density * 0.66f).dp)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text("選擇邊界形狀", style = MaterialTheme.typography.titleLarge, color = Color.Black)
                Spacer(modifier = Modifier.height(16.dp))

                // --- Preset shapes ---
                Text("預設形狀", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))

                val presets = listOf(
                    RoomShapePreset.RECTANGLE to "矩形",
                    RoomShapePreset.L_SHAPE to "L 形",
                    RoomShapePreset.T_SHAPE to "T 形",
                    RoomShapePreset.U_SHAPE to "U 形"
                )

                presets.forEach { (preset, label) ->
                    val isSelected = roomBoundary.preset == preset
                    OutlinedButton(
                        onClick = {
                            val canvasWidthDp = canvasSize.width / density
                            val canvasHeightDp = canvasSize.height / density
                            val vertices = mapViewModel.generatePresetVertices(preset, canvasWidthDp, canvasHeightDp)
                            mapViewModel.setRoomBoundary(RoomBoundary(preset = preset, vertices = vertices))
                            mapViewModel.relocateObjectsIntoBoundary(vertices)
                            showBoundarySheet = false
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        colors = if (isSelected) ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        ) else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(label, color = Color.Black)
                    }
                }

                // --- Saved custom boundaries ---
                if (savedBoundaries.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("已儲存的自訂邊界", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))

                    LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                        items(savedBoundaries) { saved ->
                            val isSelected = roomBoundary.savedBoundaryId == saved.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        mapViewModel.setRoomBoundary(
                                            RoomBoundary(
                                                preset = RoomShapePreset.CUSTOM,
                                                vertices = saved.vertices,
                                                savedBoundaryId = saved.id
                                            )
                                        )
                                        mapViewModel.relocateObjectsIntoBoundary(saved.vertices)
                                        showBoundarySheet = false
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = if (isSelected) ButtonDefaults.outlinedButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    ) else ButtonDefaults.outlinedButtonColors()
                                ) {
                                    Text(saved.name, color = Color.Black)
                                }
                                // Rename button
                                IconButton(onClick = {
                                    renamingBoundary = saved
                                    renameText = saved.name
                                }) {
                                    Text("✏️", style = MaterialTheme.typography.bodyMedium)
                                }
                                // Delete button
                                IconButton(onClick = {
                                    mapViewModel.removeSavedBoundary(saved.id)
                                    // If the currently active boundary is this one, revert to rectangle
                                    if (roomBoundary.savedBoundaryId == saved.id) {
                                        val canvasWidthDp = canvasSize.width / density
                                        val canvasHeightDp = canvasSize.height / density
                                        val verts = mapViewModel.generatePresetVertices(
                                            RoomShapePreset.RECTANGLE, canvasWidthDp, canvasHeightDp
                                        )
                                        mapViewModel.setRoomBoundary(RoomBoundary(preset = RoomShapePreset.RECTANGLE, vertices = verts))
                                        mapViewModel.relocateObjectsIntoBoundary(verts)
                                    }
                                }) {
                                    Text("🗑️", style = MaterialTheme.typography.bodyMedium)
                                }
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
                ) {
                    Text("自訂繪製")
                }
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
                TextButton(onClick = {
                    val saved = SavedBoundary(name = nameText, vertices = pendingBoundaryVertices)
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
                }) {
                    Text("儲存")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showNameDialog = false
                    pendingBoundaryVertices = emptyList()
                }) {
                    Text("取消")
                }
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
                TextButton(onClick = {
                    mapViewModel.renameSavedBoundary(renamingBoundary!!.id, renameText)
                    renamingBoundary = null
                }) {
                    Text("確認")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingBoundary = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@Composable
fun MapCanvas(
    mapObjects: List<MapObject>,
    predictedSpots: List<Pair<Float, Float>>,
    storedItems: List<MissingItem>,
    walkPath: List<Pair<Float, Float>>,
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
    isRecordingWalkPath: Boolean,
    specifiedLocation: PointF?,
    onCanvasTap: (PointF) -> Unit,
    onObjectMove: (String, Float, Float) -> Unit,
    onDeleteObject: (String) -> Unit,
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
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { 
                canvasSize = it 
                onCanvasSize(it)
            }
            .pointerInput(Unit) { // Always allow pan/zoom
                detectTransformGestures { _, pan, zoom, _ ->
                    currentOnScaleChange((currentMapScale * zoom).coerceIn(0.5f, 3f))
                    currentOnOffsetChange(currentMapOffset + pan)
                }
            }
            .pointerInput(isDrawingBoundary, isSpecifyingLocation, isRecordingWalkPath) {
                detectTapGestures(onTap = { tapOffset ->
                    if (isDrawingBoundary || isSpecifyingLocation || isRecordingWalkPath) {
                        // Convert screen tap to map virtual dp coordinates
                        val dens = density.toFloat()
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val mapPxX = (tapOffset.x - currentMapOffset.x - centerX) / currentMapScale + centerX
                        val mapPxY = (tapOffset.y - currentMapOffset.y - centerY) / currentMapScale + centerY
                        val virtualX = mapPxX / dens
                        val virtualY = mapPxY / dens
                        onCanvasTap(PointF(virtualX, virtualY))
                    } else {
                        selectedObjectId = null
                    }
                })
            }
    ) {
        val density = LocalDensity.current.density
        val effectiveBoundary = if (boundaryVertices.isNotEmpty()) boundaryVertices else {
            val pad = 32f
            val cw = canvasSize.width / density
            val ch = canvasSize.height / density
            listOf(PointF(pad, pad), PointF(cw - pad, pad), PointF(cw - pad, ch - pad), PointF(pad, ch - pad))
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
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
                    val startX = (kotlin.math.floor(leftDp / (gridSpacing / dens)) * (gridSpacing / dens)).toFloat()
                    val endX = rightDp + buffer
                    val startY = (kotlin.math.floor(topDp / (gridSpacing / dens)) * (gridSpacing / dens)).toFloat()
                    val endY = bottomDp + buffer

                    var gx = startX
                    while (gx <= endX) {
                        drawLine(gridColor, Offset(gx * dens, topDp * dens), Offset(gx * dens, bottomDp * dens), strokeWidth = 1f / mapScale)
                        gx += gridSpacing / dens
                    }
                    var gy = startY
                    while (gy <= endY) {
                        drawLine(gridColor, Offset(leftDp * dens, gy * dens), Offset(rightDp * dens, gy * dens), strokeWidth = 1f / mapScale)
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
                    drawPath(path, color = Color.Red.copy(alpha = 0.8f), style = Stroke(width = 2f * dens))
                    // Draw dashed close line preview
                    if (drawingVertices.size >= 3) {
                        drawLine(
                            color = Color.Red.copy(alpha = 0.4f),
                            start = Offset(drawingVertices.last().x * dens, drawingVertices.last().y * dens),
                            end = Offset(drawingVertices[0].x * dens, drawingVertices[0].y * dens),
                            strokeWidth = 1.5f * dens
                        )
                    }
                    // Draw vertex dots
                    drawingVertices.forEach { v ->
                        drawCircle(
                            color = Color.Red,
                            radius = 5f * dens,
                            center = Offset(v.x * dens, v.y * dens)
                        )
                    }
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
                    onDrag = { vDx: Float, vDy: Float, sDx: Float, sDy: Float ->
                        // 1. Calculate the "Ideal" finger position (clamped to boundary only)
                        val virtualXDp = obj.x + (vDx / density)
                        val virtualYDp = obj.y + (vDy / density)
                        
                        val clampedIdeal = BoundaryUtils.clampRectToPolygon(
                            virtualXDp, virtualYDp, obj.width, obj.height,
                            obj.x, obj.y, true, effectiveBoundary
                        )

                        // 2. Check if this ideal position is clear of other objects
                        val canTeleport = mapObjects.none { other ->
                            other.id != obj.id &&
                            clampedIdeal.x < other.x + other.width && clampedIdeal.x + obj.width > other.x &&
                            clampedIdeal.y < other.y + other.height && clampedIdeal.y + obj.height > other.y
                        }

                        if (canTeleport) {
                            // Target is completely open! Instant teleport to the finger position
                            if (clampedIdeal.x != obj.x || clampedIdeal.y != obj.y) {
                                onObjectMove(obj.id, clampedIdeal.x - obj.x, clampedIdeal.y - obj.y)
                            }
                        } else {
                            // Target is blocked by something. Use incremental sliding!
                            val stepXDp = sDx / density
                            val stepYDp = sDy / density

                            // --- Try moving along X ---
                            var targetX = obj.x + stepXDp
                            var canMoveX = true
                            
                            // Check boundary AND objects simultaneously
                            fun isPosValid(px: Float, py: Float): Boolean {
                                if (!BoundaryUtils.rectInPolygon(px, py, obj.width, obj.height, effectiveBoundary)) return false
                                return mapObjects.none { other ->
                                    other.id != obj.id &&
                                    px < other.x + other.width && px + obj.width > other.x &&
                                    py < other.y + other.height && py + obj.height > other.y
                                }
                            }

                            if (!isPosValid(targetX, obj.y)) {
                                // Blocked. Check object collision first
                                val collidingX = mapObjects.find { other ->
                                    other.id != obj.id &&
                                    targetX < other.x + other.width && targetX + obj.width > other.x &&
                                    obj.y < other.y + other.height && obj.y + obj.height > other.y
                                }
                                if (collidingX != null) {
                                    val snapX = if (stepXDp > 0) collidingX.x - obj.width else collidingX.x + collidingX.width
                                    if (isPosValid(snapX, obj.y)) targetX = snapX else canMoveX = false
                                } else {
                                    // Blocked by boundary. Use binary search to snap to the edge.
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
                                    other.id != obj.id &&
                                    finalX < other.x + other.width && finalX + obj.width > other.x &&
                                    targetY < other.y + other.height && targetY + obj.height > other.y
                                }
                                if (collidingY != null) {
                                    val snapY = if (stepYDp > 0) collidingY.y - obj.height else collidingY.y + collidingY.height
                                    if (isPosValid(finalX, snapY)) targetY = snapY else canMoveY = false
                                } else {
                                    // Blocked by boundary. Use binary search to snap to the edge.
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
                    onDragEnd = { /* No-op */ }
                )
            }
            
            // Draw walk path (yellow), item pins (green), AI predictions (red), manual location (blue)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val dens = density

                // Walk path — yellow dots connected by lines
                if (walkPath.size >= 2) {
                    for (i in 0 until walkPath.size - 1) {
                        drawLine(
                            color = Color(0xFFFFD600).copy(alpha = 0.7f),
                            start = Offset(walkPath[i].first * dens, walkPath[i].second * dens),
                            end = Offset(walkPath[i + 1].first * dens, walkPath[i + 1].second * dens),
                            strokeWidth = 3f * dens
                        )
                    }
                }
                walkPath.forEach { point ->
                    drawCircle(
                        color = Color(0xFFFFD600),
                        radius = 6f * dens,
                        center = Offset(point.first * dens, point.second * dens)
                    )
                }

                // Stored item pins (green) — items with a known manual location
                storedItems.filter { it.manualX != null && it.manualY != null }.forEach { item ->
                    val cx = item.manualX!! * dens
                    val cy = item.manualY!! * dens
                    drawCircle(color = Color(0xFF4CAF50), radius = 12f * dens, center = Offset(cx, cy))
                    drawCircle(color = Color.White, radius = 6f * dens, center = Offset(cx, cy))
                }

                // AI prediction spots (red)
                predictedSpots.forEach { spot ->
                    drawCircle(
                        color = Color.Red.copy(alpha = 0.6f),
                        radius = 20f * dens,
                        center = Offset(spot.first * dens, spot.second * dens)
                    )
                }

                // Manual specified location (blue)
                specifiedLocation?.let { loc ->
                    drawCircle(
                        color = Color.Blue.copy(alpha = 0.6f),
                        radius = 20f * dens,
                        center = Offset(loc.x * dens, loc.y * dens)
                    )
                }
            }
        }
    }
}

@Composable
fun MapObjectVisuals(
    type: MapObjectType,
    isBed: Boolean,
    modifier: Modifier = Modifier
) {
    val (bgColor, contentColor) = when (type) {
        MapObjectType.WALL, MapObjectType.CORNER ->
            Color(0xFF546E7A) to Color.White
        MapObjectType.DOOR ->
            Color(0xFF8D6E63) to Color.White
        MapObjectType.WINDOW ->
            Color(0xFF64B5F6) to Color.White
        MapObjectType.BED ->
            Color.Transparent to Color.Transparent
        MapObjectType.DESK ->
            Color(0xFF5C6BC0) to Color.White
        MapObjectType.SOFA ->
            Color(0xFF7E57C2) to Color.White
        MapObjectType.CABINET ->
            Color(0xFF6D4C41) to Color.White
        MapObjectType.BATHROOM_SINK ->
            Color(0xFF00ACC1) to Color.White
        MapObjectType.CHAIR ->
            Color(0xFFFF7043) to Color.White
        MapObjectType.TABLE ->
            Color(0xFF66BB6A) to Color.White
        MapObjectType.BOOKSHELF ->
            Color(0xFF43A047) to Color.White
        MapObjectType.WARDROBE ->
            Color(0xFF455A64) to Color.White
        MapObjectType.FRIDGE ->
            Color(0xFF42A5F5) to Color.White
        MapObjectType.TV_STAND ->
            Color(0xFF37474F) to Color.White
        MapObjectType.WASHING_MACHINE ->
            Color(0xFF29B6F6) to Color.White
        MapObjectType.SHOE_RACK ->
            Color(0xFFA1887F) to Color.White
        MapObjectType.TOILET ->
            Color(0xFF80CBC4) to Color.White
    }

    Box(
        modifier = modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(6.dp))
            .run {
                if (isBed) background(Color.Transparent)
                else background(bgColor).border(
                    1.dp, bgColor.copy(alpha = 0.5f),
                    androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                )
            }
    ) {
        if (isBed) {
            Image(
                painter = painterResource(id = R.drawable.bed),
                contentDescription = "Bed",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = type.emoji(),
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Text(
                    text = type.chineseName(),
                    color = contentColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 8.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 10.sp
                )
            }
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
    onDrag: (Float, Float, Float, Float) -> Unit,
    onDragEnd: () -> Unit
) {
    val density = LocalDensity.current.density
    val currentObj by rememberUpdatedState(obj)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)

    Box(
        modifier = Modifier
            .offset { androidx.compose.ui.unit.IntOffset((obj.x * density).toInt(), (obj.y * density).toInt()) }
            .size(width = obj.width.dp, height = obj.height.dp)
            .run {
                if (isEditing) {
                    this.pointerInput(isSelected) {
                        detectTapGestures(onTap = { onClick() })
                    }.pointerInput(Unit) {
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
        MapObjectVisuals(
            type = obj.type,
            isBed = obj.type == MapObjectType.BED,
            modifier = Modifier.fillMaxSize()
        )

        if (isSelected && isEditing) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-12).dp, y = (-12).dp)
                    .size(26.dp),
                shape = CircleShape,
                color = ErrorRed,
                shadowElevation = 4.dp,
                onClick = { onDelete() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Delete Object",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

fun getSizeDisplay(category: com.example.lostfoundai.data.ItemCategory, size: com.example.lostfoundai.data.ItemSize): String {
    return when (category) {
        com.example.lostfoundai.data.ItemCategory.ACCESSORY -> when (size) {
            com.example.lostfoundai.data.ItemSize.VERY_SMALL -> "極小 (< 2cm)"
            com.example.lostfoundai.data.ItemSize.SMALL -> "小 (2-5cm)"
            com.example.lostfoundai.data.ItemSize.MEDIUM -> "中 (5-10cm)"
            else -> "中 (5-10cm)"
        }
        com.example.lostfoundai.data.ItemCategory.BELONGINGS -> when (size) {
            com.example.lostfoundai.data.ItemSize.SMALL -> "小 (5-15cm)"
            com.example.lostfoundai.data.ItemSize.MEDIUM -> "中 (15-30cm)"
            com.example.lostfoundai.data.ItemSize.LARGE -> "大 (> 30cm)"
            else -> "小 (5-15cm)"
        }
        com.example.lostfoundai.data.ItemCategory.ELECTRONICS -> when (size) {
            com.example.lostfoundai.data.ItemSize.SMALL -> "小 (手機/滑鼠)"
            com.example.lostfoundai.data.ItemSize.MEDIUM -> "中 (平板/鍵盤)"
            com.example.lostfoundai.data.ItemSize.LARGE -> "大 (筆電/螢幕)"
            else -> "小 (手機/滑鼠)"
        }
        com.example.lostfoundai.data.ItemCategory.PAPER -> when (size) {
            com.example.lostfoundai.data.ItemSize.SMALL -> "小 (名片/票據)"
            com.example.lostfoundai.data.ItemSize.MEDIUM -> "中 (A5/筆記本)"
            com.example.lostfoundai.data.ItemSize.LARGE -> "大 (A4/文件夾)"
            else -> "小 (名片/票據)"
        }
        com.example.lostfoundai.data.ItemCategory.BATHROOM -> when (size) {
            com.example.lostfoundai.data.ItemSize.SMALL -> "小 (牙刷/刮鬍刀)"
            com.example.lostfoundai.data.ItemSize.MEDIUM -> "中 (瓶罐/毛巾)"
            com.example.lostfoundai.data.ItemSize.LARGE -> "大 (浴巾/臉盆)"
            else -> "小 (牙刷/刮鬍刀)"
        }
        com.example.lostfoundai.data.ItemCategory.APPLIANCE -> when (size) {
            com.example.lostfoundai.data.ItemSize.SMALL -> "小 (遙控器/線材)"
            com.example.lostfoundai.data.ItemSize.MEDIUM -> "中 (吹風機/快煮壺)"
            com.example.lostfoundai.data.ItemSize.LARGE -> "大 (風扇/吸塵器)"
            else -> "小 (遙控器/線材)"
        }
        com.example.lostfoundai.data.ItemCategory.CLOTHING -> when (size) {
            com.example.lostfoundai.data.ItemSize.SMALL -> "小 (襪子/內衣)"
            com.example.lostfoundai.data.ItemSize.MEDIUM -> "中 (上衣/褲子)"
            com.example.lostfoundai.data.ItemSize.LARGE -> "大 (外套/大衣)"
            else -> "小 (襪子/內衣)"
        }
        else -> when (size) {
            com.example.lostfoundai.data.ItemSize.VERY_SMALL -> "極小"
            com.example.lostfoundai.data.ItemSize.SMALL -> "小"
            com.example.lostfoundai.data.ItemSize.MEDIUM -> "中"
            com.example.lostfoundai.data.ItemSize.LARGE -> "大"
        }
    }
}

@Composable
private fun LegendRow(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Text(label, style = MaterialTheme.typography.labelSmall, color = OnSurfaceVariantColor)
    }
}
