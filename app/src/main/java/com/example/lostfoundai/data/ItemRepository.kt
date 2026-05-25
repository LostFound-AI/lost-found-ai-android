package com.example.lostfoundai.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

interface ItemRepository {
    fun getAllItems(): Flow<List<MissingItem>>
    fun getItemById(id: String): MissingItem?
    fun addItem(item: MissingItem)
    fun updateItem(item: MissingItem)
    fun deleteItem(id: String)
    fun getMapObjects(): Flow<List<MapObject>>
    fun updateMapObject(obj: MapObject)
    fun addMapObject(obj: MapObject)
    fun removeMapObject(id: String)
    fun clearRoom()
    fun getRoomBoundary(): Flow<RoomBoundary>
    fun setRoomBoundary(boundary: RoomBoundary)
    fun getSavedBoundaries(): Flow<List<SavedBoundary>>
    fun addSavedBoundary(boundary: SavedBoundary)
    fun removeSavedBoundary(id: String)
    fun renameSavedBoundary(id: String, newName: String)
    fun updateSavedBoundary(id: String, newVertices: List<PointF>)

    // Walk Path
    fun getWalkPath(): Flow<List<PointF>>
    fun setWalkPath(path: List<PointF>)
    fun isWalkPathVisible(): Flow<Boolean>
    fun setWalkPathVisibility(visible: Boolean)

    // Room Management
    fun getRooms(): Flow<List<RoomData>>
    fun addRoom(name: String)
    fun deleteRoom(roomId: String)
    fun switchRoom(roomId: String)

    fun isGridEnabled(): Flow<Boolean>
    fun setGridEnabled(enabled: Boolean)
}

class InMemoryItemRepository : ItemRepository {
    private val itemsFlow = MutableStateFlow<List<MissingItem>>(
        // Preload some predefined items according to user rules
        listOf(
            MissingItem(name = "戒指", category = ItemCategory.ACCESSORY, size = ItemSize.VERY_SMALL, physicalTraits = "易滾動、易掉入縫隙", defaultWeightLevel = "High"),
            MissingItem(name = "護照", category = ItemCategory.PAPER, size = ItemSize.SMALL, physicalTraits = "輕盈、易受風吹移動", defaultWeightLevel = "Medium")
        )
    )

    private val mapObjectsFlow = MutableStateFlow<List<MapObject>>(emptyList())

    override fun getAllItems(): Flow<List<MissingItem>> = itemsFlow

    override fun getItemById(id: String): MissingItem? {
        return itemsFlow.value.find { it.id == id }
    }

    override fun addItem(item: MissingItem) {
        val current = itemsFlow.value.toMutableList()
        current.add(0, item)
        itemsFlow.value = current
    }

    override fun updateItem(item: MissingItem) {
        val current = itemsFlow.value.toMutableList()
        val idx = current.indexOfFirst { it.id == item.id }
        if (idx != -1) {
            current[idx] = item
            itemsFlow.value = current
        }
    }

    override fun deleteItem(id: String) {
        val current = itemsFlow.value.toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx != -1) {
            current.removeAt(idx)
            itemsFlow.value = current
        }
    }

    override fun getMapObjects(): Flow<List<MapObject>> = mapObjectsFlow

    override fun updateMapObject(obj: MapObject) {
        val current = mapObjectsFlow.value.toMutableList()
        val idx = current.indexOfFirst { it.id == obj.id }
        if (idx != -1) {
            current[idx] = obj
            mapObjectsFlow.value = current
        }
    }

    override fun addMapObject(obj: MapObject) {
        val current = mapObjectsFlow.value.toMutableList()
        current.add(obj)
        mapObjectsFlow.value = current
    }

    override fun removeMapObject(id: String) {
        val current = mapObjectsFlow.value.toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx != -1) {
            current.removeAt(idx)
            mapObjectsFlow.value = current
        }
    }

    override fun clearRoom() {
        mapObjectsFlow.value = emptyList()
        roomBoundaryFlow.value = RoomBoundary()
    }

    private val roomBoundaryFlow = MutableStateFlow(RoomBoundary())

    override fun getRoomBoundary(): Flow<RoomBoundary> = roomBoundaryFlow

    override fun setRoomBoundary(boundary: RoomBoundary) {
        roomBoundaryFlow.value = boundary
    }

    private val savedBoundariesFlow = MutableStateFlow<List<SavedBoundary>>(emptyList())

    override fun getSavedBoundaries(): Flow<List<SavedBoundary>> = savedBoundariesFlow

    override fun addSavedBoundary(boundary: SavedBoundary) {
        savedBoundariesFlow.value = savedBoundariesFlow.value + boundary
    }

    override fun removeSavedBoundary(id: String) {
        savedBoundariesFlow.value = savedBoundariesFlow.value.filter { it.id != id }
    }

    override fun renameSavedBoundary(id: String, newName: String) {
        savedBoundariesFlow.value = savedBoundariesFlow.value.map {
            if (it.id == id) it.copy(name = newName) else it
        }
    }

    override fun updateSavedBoundary(id: String, newVertices: List<PointF>) {
        savedBoundariesFlow.value = savedBoundariesFlow.value.map {
            if (it.id == id) it.copy(vertices = newVertices) else it
        }
    }

    private val gridEnabledFlow = MutableStateFlow(true)
    override fun isGridEnabled(): Flow<Boolean> = gridEnabledFlow
    override fun setGridEnabled(enabled: Boolean) {
        gridEnabledFlow.value = enabled
    }
    
    private val walkPathFlow = MutableStateFlow<List<PointF>>(emptyList())
    override fun getWalkPath(): Flow<List<PointF>> = walkPathFlow
    override fun setWalkPath(path: List<PointF>) {
        walkPathFlow.value = path
    }
    
    private val walkPathVisibleFlow = MutableStateFlow(true)
    override fun isWalkPathVisible(): Flow<Boolean> = walkPathVisibleFlow
    override fun setWalkPathVisibility(visible: Boolean) {
        walkPathVisibleFlow.value = visible
    }
    private val roomsFlow = MutableStateFlow<List<RoomData>>(
        listOf(RoomData("default", "預設房間"))
    )

    override fun getRooms(): Flow<List<RoomData>> = roomsFlow

    override fun addRoom(name: String) {
        val newId = java.util.UUID.randomUUID().toString()
        roomsFlow.value = roomsFlow.value + RoomData(newId, name)
    }

    override fun deleteRoom(roomId: String) {
        if (roomId == "default") return
        roomsFlow.value = roomsFlow.value.filter { it.id != roomId }
    }

    override fun switchRoom(roomId: String) {
        // Simple mock
    }
}
