package com.example.lostfoundai.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class SharedPrefsItemRepository(context: Context) : ItemRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences("lostfoundai_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val itemsFlow = MutableStateFlow<List<MissingItem>>(loadItems())
    private val mapObjectsFlow = MutableStateFlow<List<MapObject>>(loadMapObjects())

    private fun loadItems(): List<MissingItem> {
        val json = prefs.getString("missing_items", null)
        return if (json != null) {
            val type = object : TypeToken<List<MissingItem>>() {}.type
            gson.fromJson(json, type)
        } else {
            // Default initial items
            listOf(
                MissingItem(name = "戒指", category = ItemCategory.ACCESSORY, size = ItemSize.VERY_SMALL, physicalTraits = "易滾動、易掉入縫隙", defaultWeightLevel = "High"),
                MissingItem(name = "護照", category = ItemCategory.PAPER, size = ItemSize.SMALL, physicalTraits = "輕盈、易受風吹移動", defaultWeightLevel = "Medium")
            )
        }
    }

    private fun saveItems(items: List<MissingItem>) {
        prefs.edit().putString("missing_items", gson.toJson(items)).apply()
        itemsFlow.value = items
    }

    private fun loadMapObjects(): List<MapObject> {
        val json = prefs.getString("map_objects", null)
        return if (json != null) {
            val type = object : TypeToken<List<MapObject>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    private fun saveMapObjects(objects: List<MapObject>) {
        prefs.edit().putString("map_objects", gson.toJson(objects)).apply()
        mapObjectsFlow.value = objects
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

    override fun getMapObjects(): Flow<List<MapObject>> = mapObjectsFlow

    override fun updateMapObject(obj: MapObject) {
        val current = mapObjectsFlow.value.toMutableList()
        val idx = current.indexOfFirst { it.id == obj.id }
        if (idx != -1) {
            current[idx] = obj
            saveMapObjects(current)
        }
    }

    override fun addMapObject(obj: MapObject) {
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

    // --- Room Boundary ---

    private val roomBoundaryFlow = MutableStateFlow<RoomBoundary>(loadRoomBoundary())

    private fun loadRoomBoundary(): RoomBoundary {
        val json = prefs.getString("room_boundary", null)
        return if (json != null) {
            gson.fromJson(json, RoomBoundary::class.java)
        } else {
            RoomBoundary() // default RECTANGLE with no vertices
        }
    }

    private fun saveRoomBoundary(boundary: RoomBoundary) {
        prefs.edit().putString("room_boundary", gson.toJson(boundary)).apply()
        roomBoundaryFlow.value = boundary
    }

    override fun getRoomBoundary(): Flow<RoomBoundary> = roomBoundaryFlow

    override fun setRoomBoundary(boundary: RoomBoundary) {
        saveRoomBoundary(boundary)
    }

    // --- Saved Boundaries ---

    private val savedBoundariesFlow = MutableStateFlow<List<SavedBoundary>>(loadSavedBoundaries())

    private fun loadSavedBoundaries(): List<SavedBoundary> {
        val json = prefs.getString("saved_boundaries", null)
        return if (json != null) {
            val type = object : TypeToken<List<SavedBoundary>>() {}.type
            gson.fromJson(json, type)
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

    private val gridEnabledFlow = MutableStateFlow(prefs.getBoolean("is_grid_enabled", true))

    override fun isGridEnabled(): Flow<Boolean> = gridEnabledFlow

    override fun setGridEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("is_grid_enabled", enabled).apply()
        gridEnabledFlow.value = enabled
    }
}
