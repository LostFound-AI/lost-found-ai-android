package com.example.lostfoundai.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

class SharedPrefsItemRepository(context: Context) : ItemRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("lostfoundai_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private var currentRoomId: String = "default"

    private val roomsFlow = MutableStateFlow<List<RoomData>>(emptyList())
    private val itemsFlow = MutableStateFlow<List<MissingItem>>(emptyList())
    private val mapObjectsFlow = MutableStateFlow<List<MapObject>>(emptyList())
    private val roomBoundaryFlow = MutableStateFlow(RoomBoundary())
    private val walkPathFlow = MutableStateFlow<List<PointF>>(emptyList())
    private val walkPathVisibleFlow = MutableStateFlow(true)
    private val savedBoundariesFlow = MutableStateFlow<List<SavedBoundary>>(emptyList())
    private val gridEnabledFlow = MutableStateFlow(prefs.getBoolean("is_grid_enabled", true))

    init {
        migrateLegacyData()
        roomsFlow.value = loadRooms()
        
        // Load data for initial room
        reloadRoomData()
    }

    private fun migrateLegacyData() {
        if (!prefs.contains("rooms") && (prefs.contains("missing_items") || prefs.contains("map_objects"))) {
            // Migrate
            val legacyItems = prefs.getString("missing_items", null)
            val legacyObjects = prefs.getString("map_objects", null)
            val legacyBoundary = prefs.getString("room_boundary", null)
            val legacyWalkPath = prefs.getString("walk_path", null)

            prefs.edit().apply {
                if (legacyItems != null) putString("default_missing_items", legacyItems)
                if (legacyObjects != null) putString("default_map_objects", legacyObjects)
                if (legacyBoundary != null) putString("default_room_boundary", legacyBoundary)
                if (legacyWalkPath != null) putString("default_walk_path", legacyWalkPath)
                remove("missing_items")
                remove("map_objects")
                remove("room_boundary")
                remove("walk_path")
            }.apply()
        }
    }

    private fun reloadRoomData() {
        itemsFlow.value = loadItems()
        mapObjectsFlow.value = loadMapObjects()
        roomBoundaryFlow.value = loadRoomBoundary()
        walkPathFlow.value = loadWalkPath()
        walkPathVisibleFlow.value = prefs.getBoolean("${currentRoomId}_walk_path_visible", true)
        savedBoundariesFlow.value = loadSavedBoundaries()
    }

    // --- Room Management ---

    private fun loadRooms(): List<RoomData> {
        val json = prefs.getString("rooms", null)
        return if (json != null) {
            val type = object : TypeToken<List<RoomData>>() {}.type
            gson.fromJson(json, type)
        } else {
            val defaultRoom = RoomData("default", "預設房間")
            saveRooms(listOf(defaultRoom))
            listOf(defaultRoom)
        }
    }

    private fun saveRooms(rooms: List<RoomData>) {
        prefs.edit().putString("rooms", gson.toJson(rooms)).apply()
        roomsFlow.value = rooms
    }

    override fun getRooms(): Flow<List<RoomData>> = roomsFlow

    override fun addRoom(name: String) {
        val newId = UUID.randomUUID().toString()
        val current = roomsFlow.value.toMutableList()
        current.add(RoomData(newId, name))
        saveRooms(current)
    }

    override fun deleteRoom(roomId: String) {
        if (roomId == "default") return // Cannot delete default room
        val current = roomsFlow.value.filter { it.id != roomId }
        saveRooms(current)
        
        // Cleanup room data from prefs
        prefs.edit().apply {
            remove("${roomId}_missing_items")
            remove("${roomId}_map_objects")
            remove("${roomId}_room_boundary")
            remove("${roomId}_walk_path")
        }.apply()
    }

    override fun renameRoom(roomId: String, newName: String) {
        val current = roomsFlow.value.map {
            if (it.id == roomId) it.copy(name = newName) else it
        }
        saveRooms(current)
    }

    override fun switchRoom(roomId: String) {
        currentRoomId = roomId
        reloadRoomData()
    }

    // --- Items ---

    private fun loadItems(): List<MissingItem> {
        val json = prefs.getString("${currentRoomId}_missing_items", null)
        return if (json != null) {
            val type = object : TypeToken<List<MissingItem>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    private fun saveItems(items: List<MissingItem>) {
        prefs.edit().putString("${currentRoomId}_missing_items", gson.toJson(items)).apply()
        itemsFlow.value = items
    }

    override fun getAllItems(): Flow<List<MissingItem>> = itemsFlow

    override fun getItemById(id: String): MissingItem? {
        return itemsFlow.value.find { it.id == id }
    }

    override fun addItem(item: MissingItem) {
        val current = itemsFlow.value.toMutableList()
        current.add(0, item)
        saveItems(current)
    }

    override fun updateItem(item: MissingItem) {
        val current = itemsFlow.value.toMutableList()
        val idx = current.indexOfFirst { it.id == item.id }
        if (idx != -1) {
            current[idx] = item
            saveItems(current)
        }
    }

    override fun deleteItem(id: String) {
        val current = itemsFlow.value.toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx != -1) {
            current.removeAt(idx)
            saveItems(current)
        }
    }

    // --- Map Objects ---

    private fun loadMapObjects(): List<MapObject> {
        val json = prefs.getString("${currentRoomId}_map_objects", null)
        return if (json != null) {
            val type = object : TypeToken<List<MapObject>>() {}.type
            val objects: List<MapObject> = gson.fromJson(json, type) ?: emptyList()
            objects.filter { it.x.isFinite() && it.y.isFinite() }
        } else {
            emptyList()
        }
    }

    private fun saveMapObjects(objects: List<MapObject>) {
        prefs.edit().putString("${currentRoomId}_map_objects", gson.toJson(objects)).apply()
        mapObjectsFlow.value = objects
    }

    override fun getMapObjects(): Flow<List<MapObject>> = mapObjectsFlow

    override fun updateMapObject(obj: MapObject) {
        if (!obj.x.isFinite() || !obj.y.isFinite()) return
        val current = mapObjectsFlow.value.toMutableList()
        val idx = current.indexOfFirst { it.id == obj.id }
        if (idx != -1) {
            current[idx] = obj
            saveMapObjects(current)
        }
    }

    override fun addMapObject(obj: MapObject) {
        if (!obj.x.isFinite() || !obj.y.isFinite()) return
        val current = mapObjectsFlow.value.toMutableList()
        current.add(obj)
        saveMapObjects(current)
    }

    override fun removeMapObject(id: String) {
        val current = mapObjectsFlow.value.toMutableList()
        val idx = current.indexOfFirst { it.id == id }
        if (idx != -1) {
            current.removeAt(idx)
            saveMapObjects(current)
        }
    }

    override fun clearRoom() {
        saveMapObjects(emptyList())
        saveRoomBoundary(RoomBoundary())
        saveWalkPath(emptyList())
    }

    // --- Room Boundary ---

    private fun loadRoomBoundary(): RoomBoundary {
        val json = prefs.getString("${currentRoomId}_room_boundary", null)
        return if (json != null) {
            val parsed = gson.fromJson(json, RoomBoundary::class.java)
            if (parsed.innerWalls == null) parsed.copy(innerWalls = emptyList()) else parsed
        } else {
            RoomBoundary()
        }
    }

    private fun saveRoomBoundary(boundary: RoomBoundary) {
        prefs.edit().putString("${currentRoomId}_room_boundary", gson.toJson(boundary)).apply()
        roomBoundaryFlow.value = boundary
    }

    override fun getRoomBoundary(): Flow<RoomBoundary> = roomBoundaryFlow

    override fun setRoomBoundary(boundary: RoomBoundary) {
        saveRoomBoundary(boundary)
    }

    // --- Walk Path ---

    private fun loadWalkPath(): List<PointF> {
        val json = prefs.getString("${currentRoomId}_walk_path", null)
        return if (json != null) {
            val type = object : TypeToken<List<PointF>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    private fun saveWalkPath(path: List<PointF>) {
        prefs.edit().putString("${currentRoomId}_walk_path", gson.toJson(path)).apply()
        walkPathFlow.value = path
    }

    override fun getWalkPath(): Flow<List<PointF>> = walkPathFlow

    override fun setWalkPath(path: List<PointF>) {
        saveWalkPath(path)
    }

    override fun isWalkPathVisible(): Flow<Boolean> = walkPathVisibleFlow

    override fun setWalkPathVisibility(visible: Boolean) {
        prefs.edit().putBoolean("${currentRoomId}_walk_path_visible", visible).apply()
        walkPathVisibleFlow.value = visible
    }

    // --- Saved Boundaries (Global across rooms) ---

    private fun loadSavedBoundaries(): List<SavedBoundary> {
        val json = prefs.getString("saved_boundaries", null)
        return if (json != null) {
            val type = object : TypeToken<List<SavedBoundary>>() {}.type
            val parsed: List<SavedBoundary>? = gson.fromJson(json, type)
            parsed?.map { if (it.innerWalls == null) it.copy(innerWalls = emptyList()) else it } ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun saveSavedBoundaries(boundaries: List<SavedBoundary>) {
        prefs.edit().putString("saved_boundaries", gson.toJson(boundaries)).apply()
        savedBoundariesFlow.value = boundaries
    }

    override fun getSavedBoundaries(): Flow<List<SavedBoundary>> = savedBoundariesFlow

    override fun addSavedBoundary(boundary: SavedBoundary) {
        saveSavedBoundaries(savedBoundariesFlow.value + boundary)
    }

    override fun removeSavedBoundary(id: String) {
        saveSavedBoundaries(savedBoundariesFlow.value.filter { it.id != id })
    }

    override fun renameSavedBoundary(id: String, newName: String) {
        saveSavedBoundaries(savedBoundariesFlow.value.map {
            if (it.id == id) it.copy(name = newName) else it
        })
    }

    override fun updateSavedBoundary(id: String, newVertices: List<PointF>, newInnerWalls: List<List<PointF>>) {
        val updated = savedBoundariesFlow.value.map {
            if (it.id == id) it.copy(vertices = newVertices, innerWalls = newInnerWalls) else it
        }
        saveSavedBoundaries(updated)
    }

    // --- Grid Enabled (Global) ---

    override fun isGridEnabled(): Flow<Boolean> = gridEnabledFlow

    override fun setGridEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_grid_enabled", enabled).apply()
        gridEnabledFlow.value = enabled
    }
}
