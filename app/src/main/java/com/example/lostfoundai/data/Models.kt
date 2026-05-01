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
    CLOTHING,     // 衣物
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
    val lastKnownLocationDesc: String = "",
    val manualX: Float? = null,
    val manualY: Float? = null
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

data class RoomData(
    val id: String,
    val name: String
)

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

fun MapObjectType.emoji(): String = when (this) {
    MapObjectType.WALL -> "🧱"
    MapObjectType.CORNER -> "📐"
    MapObjectType.DOOR -> "🚪"
    MapObjectType.WINDOW -> "🪟"
    MapObjectType.BED -> "🛏"
    MapObjectType.DESK -> "💻"
    MapObjectType.SOFA -> "🛋"
    MapObjectType.CABINET -> "🗄"
    MapObjectType.BATHROOM_SINK -> "🚿"
    MapObjectType.CHAIR -> "🪑"
    MapObjectType.TABLE -> "🍽"
    MapObjectType.BOOKSHELF -> "📚"
    MapObjectType.WARDROBE -> "👔"
    MapObjectType.FRIDGE -> "🧊"
    MapObjectType.TV_STAND -> "📺"
    MapObjectType.WASHING_MACHINE -> "🫧"
    MapObjectType.SHOE_RACK -> "👟"
    MapObjectType.TOILET -> "🚽"
}

fun MapObjectType.chineseName(): String = when (this) {
    MapObjectType.WALL -> "牆壁"
    MapObjectType.CORNER -> "轉角"
    MapObjectType.DOOR -> "門"
    MapObjectType.WINDOW -> "窗戶"
    MapObjectType.BED -> "床"
    MapObjectType.DESK -> "書桌"
    MapObjectType.SOFA -> "沙發"
    MapObjectType.CABINET -> "櫃子"
    MapObjectType.BATHROOM_SINK -> "洗手台"
    MapObjectType.CHAIR -> "椅子"
    MapObjectType.TABLE -> "桌子"
    MapObjectType.BOOKSHELF -> "書架"
    MapObjectType.WARDROBE -> "衣櫃"
    MapObjectType.FRIDGE -> "冰箱"
    MapObjectType.TV_STAND -> "電視櫃"
    MapObjectType.WASHING_MACHINE -> "洗衣機"
    MapObjectType.SHOE_RACK -> "鞋架"
    MapObjectType.TOILET -> "馬桶"
}

fun MapObjectType.defaultWidth(): Float = when (this) {
    MapObjectType.WALL -> 120f
    MapObjectType.CORNER -> 20f
    MapObjectType.DOOR -> 60f
    MapObjectType.WINDOW -> 80f
    MapObjectType.BED -> 150f
    MapObjectType.DESK -> 90f
    MapObjectType.SOFA -> 150f
    MapObjectType.CABINET -> 80f
    MapObjectType.BATHROOM_SINK -> 50f
    MapObjectType.CHAIR -> 50f
    MapObjectType.TABLE -> 100f
    MapObjectType.BOOKSHELF -> 90f
    MapObjectType.WARDROBE -> 100f
    MapObjectType.FRIDGE -> 60f
    MapObjectType.TV_STAND -> 120f
    MapObjectType.WASHING_MACHINE -> 60f
    MapObjectType.SHOE_RACK -> 80f
    MapObjectType.TOILET -> 50f
}

fun MapObjectType.defaultHeight(): Float = when (this) {
    MapObjectType.WALL -> 15f
    MapObjectType.CORNER -> 20f
    MapObjectType.DOOR -> 15f
    MapObjectType.WINDOW -> 10f
    MapObjectType.BED -> 100f
    MapObjectType.DESK -> 60f
    MapObjectType.SOFA -> 70f
    MapObjectType.CABINET -> 50f
    MapObjectType.BATHROOM_SINK -> 40f
    MapObjectType.CHAIR -> 50f
    MapObjectType.TABLE -> 60f
    MapObjectType.BOOKSHELF -> 30f
    MapObjectType.WARDROBE -> 60f
    MapObjectType.FRIDGE -> 60f
    MapObjectType.TV_STAND -> 40f
    MapObjectType.WASHING_MACHINE -> 60f
    MapObjectType.SHOE_RACK -> 30f
    MapObjectType.TOILET -> 70f
}
