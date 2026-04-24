package com.example.lostfoundai.data

import java.util.UUID

enum class ItemSize {
    VERY_SMALL, // 極小 (< 2cm)
    SMALL,      // 小 (5-10cm)
    MEDIUM,     // 中 (15-20cm)
    LARGE       // 大 (> 20cm)
}

enum class ItemCategory {
    ACCESSORY,    // 飾品
    BELONGINGS,   // 隨身物品
    ELECTRONICS,  // 電子產品
    PAPER,        // 紙本
    BATHROOM,     // 衛浴用品
    APPLIANCE,    // 家電配件
    OTHER
}

// Data model for a lost item the user is tracking
data class MissingItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: ItemCategory,
    val size: ItemSize,
    val physicalTraits: String,
    val defaultWeightLevel: String, // "High", "Medium", "Low", "Very Low"
    val lastKnownLocationDesc: String = ""
)

// Represents objects defined locally on the 2D Map (furniture, walls)
enum class MapObjectType {
    WALL,
    CORNER,
    BED,
    DESK,
    SOFA,
    CABINET,
    BATHROOM_SINK,
    DOOR,
    WINDOW,
    CHAIR,
    TABLE,
    BOOKSHELF,
    WARDROBE,
    FRIDGE,
    TV_STAND,
    WASHING_MACHINE,
    SHOE_RACK,
    TOILET
}

data class MapObject(
    val id: String = UUID.randomUUID().toString(),
    val type: MapObjectType,
    var x: Float,
    var y: Float,
    var rotation: Float = 0f,
    var width: Float = 100f,
    var height: Float = 100f
)

// 預設房間形狀
enum class RoomShapePreset {
    RECTANGLE,    // 矩形
    L_SHAPE,      // L 形
    T_SHAPE,      // T 形
    U_SHAPE,      // U 形
    CUSTOM        // 自訂繪製
}

// 2D 座標點（單位為 dp）
data class PointF(val x: Float, val y: Float)

// 邊界多邊形：由頂點組成的閉合路徑
data class RoomBoundary(
    val preset: RoomShapePreset = RoomShapePreset.RECTANGLE,
    val vertices: List<PointF> = emptyList(),
    val savedBoundaryId: String? = null // reference to a SavedBoundary if using one
)

// 已儲存的自訂邊界
data class SavedBoundary(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val vertices: List<PointF>
)
