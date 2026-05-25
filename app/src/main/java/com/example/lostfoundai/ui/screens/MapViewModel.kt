package com.example.lostfoundai.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lostfoundai.ai.PredictionEngine // To be created
import com.example.lostfoundai.data.ItemRepository
import com.example.lostfoundai.data.MapObject
import com.example.lostfoundai.data.MapObjectType
import com.example.lostfoundai.data.PointF
import com.example.lostfoundai.data.PredictionResult
import com.example.lostfoundai.data.RoomBoundary
import com.example.lostfoundai.data.RoomShapePreset
import com.example.lostfoundai.data.SavedBoundary
import com.example.lostfoundai.utils.BoundaryUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MapViewModel(
    private val repository: ItemRepository,
    private val predictionEngine: PredictionEngine // We'll inject this simply later
) : ViewModel() {

    val mapObjects: StateFlow<List<MapObject>> = repository.getMapObjects().stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _predictionResult = MutableStateFlow<PredictionResult>(PredictionResult())
    val predictionResult: StateFlow<PredictionResult> = _predictionResult.asStateFlow()

    private val _isPredicting = MutableStateFlow(false)
    val isPredicting: StateFlow<Boolean> = _isPredicting.asStateFlow()

    val walkPath: StateFlow<List<PointF>> = repository.getWalkPath().stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isRecordingWalkPath = MutableStateFlow(false)
    val isRecordingWalkPath: StateFlow<Boolean> = _isRecordingWalkPath.asStateFlow()

    val isWalkPathVisible: StateFlow<Boolean> = repository.isWalkPathVisible().stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun toggleWalkPathRecording() {
        _isRecordingWalkPath.value = !_isRecordingWalkPath.value
    }
    
    fun toggleWalkPathVisibility() {
        repository.setWalkPathVisibility(!isWalkPathVisible.value)
    }

    // UI states for dragging from toolbar
    var draggedObjectType: MapObjectType? = null

    // Room Boundary
    val roomBoundary: StateFlow<RoomBoundary> = repository.getRoomBoundary().stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = RoomBoundary()
    )

    // Saved custom boundaries
    val savedBoundaries: StateFlow<List<SavedBoundary>> = repository.getSavedBoundaries().stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setRoomBoundary(boundary: RoomBoundary) {
        repository.setRoomBoundary(boundary)
    }

    fun addSavedBoundary(boundary: SavedBoundary) {
        repository.addSavedBoundary(boundary)
    }

    fun removeSavedBoundary(id: String) {
        repository.removeSavedBoundary(id)
    }

    fun renameSavedBoundary(id: String, newName: String) {
        repository.renameSavedBoundary(id, newName)
    }

    fun updateSavedBoundary(id: String, newVertices: List<PointF>) {
        repository.updateSavedBoundary(id, newVertices)
    }

    fun clearRoom() {
        repository.clearRoom()
    }

    /**
     * After the boundary changes, relocate any objects that are now outside
     * the new boundary to the nearest valid position inside it.
     * Processed sequentially to avoid stacking objects on top of each other.
     */
    fun relocateObjectsIntoBoundary(vertices: List<PointF>) {
        if (vertices.size < 3) return
        val currentObjects = mapObjects.value.toMutableList()
        val movedObjects = mutableListOf<MapObject>()

        for (i in currentObjects.indices) {
            val obj = currentObjects[i]
            // We consider objects already processed as "obstacles" to avoid overlapping
            val otherProcessed = movedObjects + currentObjects.drop(i + 1)
            val objWidth = obj.width * obj.scale
            val objHeight = obj.height * obj.scale
            
            if (!BoundaryUtils.rectInPolygon(obj.x, obj.y, objWidth, objHeight, vertices)) {
                // Find nearest position that is inside boundary AND doesn't overlap with already moved objects
                val newPos = BoundaryUtils.findNearestInsidePosition(
                    obj.x, obj.y, objWidth, objHeight, vertices,
                    avoidRects = movedObjects // Only avoid those we already placed in the new boundary
                )
                
                if (newPos.x != obj.x || newPos.y != obj.y) {
                    val updatedObj = obj.copy(x = newPos.x, y = newPos.y)
                    repository.updateMapObject(updatedObj)
                    movedObjects.add(updatedObj)
                } else {
                    movedObjects.add(obj)
                }
            } else {
                movedObjects.add(obj)
            }
        }
    }

    /**
     * Generate vertices for preset room shapes.
     * The vertices are in dp, fitting within the [padding, width-padding] x [padding, height-padding] area.
     */
    fun generatePresetVertices(
        preset: RoomShapePreset,
        canvasWidthDp: Float,
        canvasHeightDp: Float,
        padding: Float = 32f
    ): List<PointF> {
        val l = padding                    // left
        val t = padding                    // top
        val r = canvasWidthDp - padding    // right
        val b = canvasHeightDp - padding   // bottom
        val midX = (l + r) / 2f
        val midY = (t + b) / 2f

        return when (preset) {
            RoomShapePreset.RECTANGLE -> listOf(
                PointF(l, t), PointF(r, t), PointF(r, b), PointF(l, b)
            )
            RoomShapePreset.L_SHAPE -> listOf(
                PointF(l, t), PointF(midX, t), PointF(midX, midY),
                PointF(r, midY), PointF(r, b), PointF(l, b)
            )
            RoomShapePreset.T_SHAPE -> listOf(
                PointF(l, t), PointF(r, t), PointF(r, midY),
                PointF(midX + (r - l) * 0.15f, midY),
                PointF(midX + (r - l) * 0.15f, b),
                PointF(midX - (r - l) * 0.15f, b),
                PointF(midX - (r - l) * 0.15f, midY),
                PointF(l, midY)
            )
            RoomShapePreset.U_SHAPE -> listOf(
                PointF(l, t), PointF(l + (r - l) * 0.3f, t),
                PointF(l + (r - l) * 0.3f, b - (b - t) * 0.4f),
                PointF(r - (r - l) * 0.3f, b - (b - t) * 0.4f),
                PointF(r - (r - l) * 0.3f, t), PointF(r, t),
                PointF(r, b), PointF(l, b)
            )
            RoomShapePreset.CUSTOM -> emptyList()
        }
    }

    fun addMapObject(obj: MapObject) {
        repository.addMapObject(obj)
    }

    fun updateMapObject(obj: MapObject) {
        repository.updateMapObject(obj)
    }

    fun removeMapObject(id: String) {
        repository.removeMapObject(id)
    }

    fun updateObjectScale(id: String, scale: Float) {
        val currentObj = mapObjects.value.find { it.id == id }
        if (currentObj != null) {
            val updated = currentObj.copy(scale = scale)
            repository.updateMapObject(updated)
        }
    }

    fun addWalkPathPoint(x: Float, y: Float) {
        val current = walkPath.value
        repository.setWalkPath(current + PointF(x, y))
    }
    
    fun clearWalkPath() {
        repository.setWalkPath(emptyList())
    }

    fun startAIPrediction(itemId: String) {
        viewModelScope.launch {
            _isPredicting.value = true
            try {
                val item = repository.getItemById(itemId) ?: return@launch
                val boundary = roomBoundary.value.vertices
                val result = predictionEngine.calculatePrediction(item, mapObjects.value, walkPath.value, boundary)
                _predictionResult.value = result.copy(itemId = item.id)
            } finally {
                _isPredicting.value = false
            }
        }
    }
    
    fun clearPredictedSpots() {
        _predictionResult.value = PredictionResult()
    }

    val gridEnabled: StateFlow<Boolean> = repository.isGridEnabled().stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    fun setGridEnabled(enabled: Boolean) {
        repository.setGridEnabled(enabled)
    }
}
