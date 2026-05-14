package com.example.lostfoundai.ui.screens

import androidx.lifecycle.ViewModel
import com.example.lostfoundai.data.ItemCategory
import com.example.lostfoundai.data.ItemRepository
import com.example.lostfoundai.data.ItemSize
import com.example.lostfoundai.data.MissingItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class SearchViewModel(
    private val repository: ItemRepository
) : ViewModel() {

    val allItems = repository.getAllItems()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filter items based on search query
    val filteredItems = kotlinx.coroutines.flow.combine(allItems, _searchQuery) { items, query ->
        if (query.isEmpty()) {
            items
        } else {
            items.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun addMissingItem(
        name: String,
        category: ItemCategory,
        size: ItemSize,
        traits: String,
        weight: String,
        location: String,
        manualX: Float? = null,
        manualY: Float? = null,
        linkedFurnitureId: String? = null,
        photoPath: String? = null
    ): String {
        val newItem = MissingItem(
            name = name,
            category = category,
            size = size,
            physicalTraits = traits,
            defaultWeightLevel = weight,
            lastKnownLocationDesc = location,
            manualX = manualX,
            manualY = manualY,
            linkedFurnitureId = linkedFurnitureId,
            photoPath = photoPath
        )
        repository.addItem(newItem)
        return newItem.id
    }

    fun updateItem(item: MissingItem) {
        repository.updateItem(item)
    }

    fun deleteItem(id: String) {
        repository.deleteItem(id)
    }
}
