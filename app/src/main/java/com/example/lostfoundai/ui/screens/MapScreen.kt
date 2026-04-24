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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.lostfoundai.data.PointF
import com.example.lostfoundai.data.RoomBoundary
import com.example.lostfoundai.data.RoomShapePreset
import com.example.lostfoundai.data.SavedBoundary
import com.example.lostfoundai.ui.components.Toolbar
import com.example.lostfoundai.utils.BoundaryUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    mapViewModel: MapViewModel,
    searchViewModel: SearchViewModel
) {
    val mapObjects by mapViewModel.mapObjects.collectAsState(initial = emptyList())
    val predictedSpots by mapViewModel.predictedSpots.collectAsState()
    val searchItems by searchViewModel.filteredItems.collectAsState(initial = emptyList())
    val roomBoundary by mapViewModel.roomBoundary.collectAsState()
    val savedBoundaries by mapViewModel.savedBoundaries.collectAsState()
    val gridEnabled by mapViewModel.gridEnabled.collectAsState()
    
    var isEditing by remember { mutableStateOf(false) }
    var showSearchSheet by remember { mutableStateOf(false) }
    var showBoundarySheet by remember { mutableStateOf(false) }
    var isDrawingBoundary by remember { mutableStateOf(false) }
    var drawingVertices by remember { mutableStateOf<List<PointF>>(emptyList()) }
    // Naming dialog for custom boundaries
    var showNameDialog by remember { mutableStateOf(false) }
    var pendingBoundaryVertices by remember { mutableStateOf<List<PointF>>(emptyList()) }
    // Rename dialog state
    var renamingBoundary by remember { mutableStateOf<SavedBoundary?>(null) }
    var renameText by remember { mutableStateOf("") }
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
            }
        }
    ) {
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
                        .background(Color(0xFFF5F5F5))
                ) {
                    MapCanvas(
                        mapObjects = mapObjects,
                        predictedSpots = predictedSpots,
                        isEditing = isEditing,
                        mapScale = mapScale,
                        onScaleChange = { mapScale = it },
                        mapOffset = mapOffset,
                        onOffsetChange = { mapOffset = it },
                        onCanvasSize = { canvasSize = it },
                        boundaryVertices = roomBoundary.vertices,
                        isDrawingBoundary = isDrawingBoundary,
                        drawingVertices = drawingVertices,
                        onDrawingTap = { tapOffsetDp ->
                            drawingVertices = drawingVertices + tapOffsetDp
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
                                        MapObject(type = droppedType, x = lastValidGhostX, y = lastValidGhostY)
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
                    val rawWidth = 100f
                    val rawHeight = 100f
                    
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
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = {
                            drawingVertices = emptyList()
                        }) {
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
                            enabled = drawingVertices.size >= 3
                        ) {
                            Text("完成繪製 (${drawingVertices.size} 個頂點)")
                        }
                    }
                    // Cancel drawing button
                    Button(
                        onClick = {
                            isDrawingBoundary = false
                            drawingVertices = emptyList()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 16.dp)
                    ) {
                        Text("取消繪製")
                    }
                } else if (isEditing) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(onClick = { showBoundarySheet = true }) {
                            Text("邊界")
                        }
                        Button(onClick = { isEditing = false }) {
                            Text("完成")
                        }
                    }
                }
                
                // Settings button (Available in all modes except drawing)
                if (!isDrawingBoundary) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(16.dp)
                            .clickable { coroutineScope.launch { drawerState.open() } }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.setting),
                            contentDescription = "Settings",
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }

                if (!isDrawingBoundary && !isEditing) {
                    // Bottom center search
                    Button(
                        onClick = { showSearchSheet = true },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 24.dp)
                    ) {
                        Text("尋找")
                    }

                    // Top right edit icon
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .clickable { isEditing = true }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.edit),
                            contentDescription = "Edit",
                            modifier = Modifier.size(56.dp)
                        )
                    }
                }
            }
        }
    }

    if (showSearchSheet) {
        var showHistory by remember { mutableStateOf(false) }

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
                            Image(
                                painter = painterResource(id = R.drawable.return_back),
                                contentDescription = "返回表單",
                                modifier = Modifier.size(28.dp)
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
                                    .clickable {
                                        mapViewModel.startAIPrediction(item.id)
                                        showSearchSheet = false
                                    }
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        text = item.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "類別: ${item.category.name} | 大小: ${item.size.name}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                } else {
                    // --- Form View ---
                    var itemName by remember { mutableStateOf("") }
                    
                    var categoryExpanded by remember { mutableStateOf(false) }
                    val categories = listOf("飾品", "隨身物品", "電子產品", "紙本", "衛浴用品", "家電配件", "自訂")
                    var selectedCategory by remember { mutableStateOf(categories[0]) }
                    var customCategory by remember { mutableStateOf("") }

                    var sizeExpanded by remember { mutableStateOf(false) }
                    val sizes = listOf("極小 (< 2cm)", "小 (5-10cm)", "中 (15-20cm)", "大 (> 20cm)", "自訂")
                    var selectedSize by remember { mutableStateOf(sizes[0]) }
                    var customSize by remember { mutableStateOf("") }

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Button(
                            onClick = { showHistory = true },
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
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, color = Color.White) },
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
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = sizeExpanded,
                            onDismissRequest = { sizeExpanded = false }
                        ) {
                            sizes.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, color = Color.White) },
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
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                val newItemId = searchViewModel.addMissingItem(
                                    name = itemName.ifEmpty { "新物品" },
                                    category = com.example.lostfoundai.data.ItemCategory.OTHER,
                                    size = com.example.lostfoundai.data.ItemSize.MEDIUM,
                                    traits = "自訂特徵",
                                    weight = "Medium",
                                    location = "地圖"
                                )
                                mapViewModel.startAIPrediction(newItemId)
                                showSearchSheet = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("AI 尋找", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        OutlinedButton(
                            onClick = {
                                searchViewModel.addMissingItem(
                                    name = itemName.ifEmpty { "新物品" },
                                    category = com.example.lostfoundai.data.ItemCategory.OTHER,
                                    size = com.example.lostfoundai.data.ItemSize.MEDIUM,
                                    traits = "自訂特徵",
                                    weight = "Medium",
                                    location = "地圖"
                                )
                                showHistory = true
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("新增物品", color = Color(0xFF1A1A1A))
                        }
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
    isEditing: Boolean,
    mapScale: Float,
    onScaleChange: (Float) -> Unit,
    mapOffset: Offset,
    onOffsetChange: (Offset) -> Unit,
    onCanvasSize: (androidx.compose.ui.unit.IntSize) -> Unit,
    boundaryVertices: List<PointF>,
    isDrawingBoundary: Boolean,
    drawingVertices: List<PointF>,
    onDrawingTap: (PointF) -> Unit,
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
            .pointerInput(isDrawingBoundary) {
                detectTapGestures(onTap = { tapOffset ->
                    if (isDrawingBoundary) {
                        // Convert screen tap to map virtual dp coordinates
                        val dens = density.toFloat()
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        val mapPxX = (tapOffset.x - currentMapOffset.x - centerX) / currentMapScale + centerX
                        val mapPxY = (tapOffset.y - currentMapOffset.y - centerY) / currentMapScale + centerY
                        val virtualX = mapPxX / dens
                        val virtualY = mapPxY / dens
                        onDrawingTap(PointF(virtualX, virtualY))
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
                                // Blocked. Attempt tiny step or snap
                                val clampedX = BoundaryUtils.clampRectToPolygon(
                                    targetX, obj.y, obj.width, obj.height,
                                    obj.x, obj.y, true, effectiveBoundary
                                ).x
                                
                                // Check if clampedX is also blocked by objects
                                if (isPosValid(clampedX, obj.y)) {
                                    targetX = clampedX
                                } else {
                                    // Still blocked, try to find the gap near objects
                                    val collidingX = mapObjects.find { other ->
                                        other.id != obj.id &&
                                        targetX < other.x + other.width && targetX + obj.width > other.x &&
                                        obj.y < other.y + other.height && obj.y + obj.height > other.y
                                    }
                                    if (collidingX != null) {
                                        val snapX = if (stepXDp > 0) collidingX.x - obj.width else collidingX.x + collidingX.width
                                        if (isPosValid(snapX, obj.y)) targetX = snapX else canMoveX = false
                                    } else {
                                        canMoveX = false
                                    }
                                }
                            }
                            val finalX = if (canMoveX) targetX else obj.x

                            // --- Try moving along Y ---
                            var targetY = obj.y + stepYDp
                            var canMoveY = true
                            
                            if (!isPosValid(finalX, targetY)) {
                                val clampedY = BoundaryUtils.clampRectToPolygon(
                                    finalX, targetY, obj.width, obj.height,
                                    finalX, obj.y, true, effectiveBoundary
                                ).y
                                
                                if (isPosValid(finalX, clampedY)) {
                                    targetY = clampedY
                                } else {
                                    val collidingY = mapObjects.find { other ->
                                        other.id != obj.id &&
                                        finalX < other.x + other.width && finalX + obj.width > other.x &&
                                        targetY < other.y + other.height && targetY + obj.height > other.y
                                    }
                                    if (collidingY != null) {
                                        val snapY = if (stepYDp > 0) collidingY.y - obj.height else collidingY.y + collidingY.height
                                        if (isPosValid(finalX, snapY)) targetY = snapY else canMoveY = false
                                    } else {
                                        canMoveY = false
                                    }
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
            
            // Draw Predictions
            Canvas(modifier = Modifier.fillMaxSize()) {
                predictedSpots.forEach { spot ->
                    drawCircle(
                        color = Color.Red.copy(alpha = 0.6f),
                        radius = 40f,
                        center = Offset(spot.first, spot.second)
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
    // Select color based on category/type
    val baseColor = when (type) {
        MapObjectType.WALL, MapObjectType.CORNER -> Color.DarkGray
        MapObjectType.DOOR -> Color(0xFF8B4513) // SaddleBrown
        MapObjectType.WINDOW -> Color(0xFFADD8E6) // LightBlue
        MapObjectType.BED -> Color(0xFF9370DB) // MediumPurple
        MapObjectType.DESK -> Color(0xFFD2B48C) // Tan
        MapObjectType.SOFA -> Color(0xFFCD5C5C) // IndianRed
        MapObjectType.CABINET -> Color(0xFF8B4513)
        MapObjectType.BATHROOM_SINK -> Color(0xFF20B2AA) // LightSeaGreen
        MapObjectType.CHAIR -> Color(0xFFDEB887) // BurlyWood
        MapObjectType.TABLE -> Color(0xFFA0522D) // Sienna
        MapObjectType.BOOKSHELF -> Color(0xFF6B8E23) // OliveDrab
        MapObjectType.WARDROBE -> Color(0xFF708090) // SlateGray
        MapObjectType.FRIDGE -> Color(0xFFB0C4DE) // LightSteelBlue
        MapObjectType.TV_STAND -> Color(0xFF2F4F4F) // DarkSlateGray
        MapObjectType.WASHING_MACHINE -> Color(0xFF87CEEB) // SkyBlue
        MapObjectType.SHOE_RACK -> Color(0xFFBC8F8F) // RosyBrown
        MapObjectType.TOILET -> Color(0xFFE0E0E0) // LightGray
    }

    Box(
        modifier = modifier.then(
            if (isBed) Modifier.background(Color.Transparent)
            else Modifier.background(baseColor).border(1.dp, Color.Black)
        )
    ) {
        if (isBed) {
            Image(
                painter = painterResource(id = R.drawable.bed),
                contentDescription = "Bed",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = type.name,
                color = Color.Black,
                modifier = Modifier.align(Alignment.Center)
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
        }
    }
}
