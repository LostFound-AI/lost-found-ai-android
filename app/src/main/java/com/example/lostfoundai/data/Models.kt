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
    val manualY: Float? = null,
    val linkedFurnitureId: String? = null,
    val photoPath: String? = null
)

// Represents objects defined locally on the 2D Map (furniture, walls)
enum class MapObjectType {
    BED,
    DOUBLE_BED,
    HIGH_CABINET_L,
    BATHROOM_SINK,
    WINDOW,
    TOILET,
    BATHTUB,
    CHAIR_1,
    CHAIR_2,
    DOOR_LEFT,
    DOOR_RIGHT,
    DOUBLE_SOFA,
    HIGH_CABINET_S,
    REFRIGERATOR,
    TABLE_L,
    TABLE_S,
    TABLE_L_SHAPE
}

data class MapObject(
    val id: String = UUID.randomUUID().toString(),
    val type: MapObjectType,
    var x: Float,
    var y: Float,
    val rotation: Float = 0f,
    val width: Float = 100f,
    val height: Float = 100f,
    val scale: Float = 1.0f
)

fun MapObjectType.getDisplayName(): String {
    return when (this) {
        MapObjectType.BED -> "單人床"
        MapObjectType.DOUBLE_BED -> "雙人床"
        MapObjectType.HIGH_CABINET_L -> "高櫃(大)"
        MapObjectType.BATHROOM_SINK -> "洗手台"
        MapObjectType.WINDOW -> "窗戶"
        MapObjectType.TOILET -> "馬桶"
        MapObjectType.BATHTUB -> "浴缸"
        MapObjectType.CHAIR_1 -> "椅子 1"
        MapObjectType.CHAIR_2 -> "椅子 2"
        MapObjectType.DOOR_LEFT -> "門(左開)"
        MapObjectType.DOOR_RIGHT -> "門(右開)"
        MapObjectType.DOUBLE_SOFA -> "雙人沙發"
        MapObjectType.HIGH_CABINET_S -> "高櫃(小)"
        MapObjectType.REFRIGERATOR -> "冰箱"
        MapObjectType.TABLE_L -> "桌子(長)"
        MapObjectType.TABLE_S -> "桌子(短)"
        MapObjectType.TABLE_L_SHAPE -> "L型桌"
    }
}

fun MapObjectType.getDefaultDimensions(): Pair<Float, Float> {
    return when (this) {
        MapObjectType.BED -> Pair(77.5f, 148.5f)
        MapObjectType.DOUBLE_BED -> Pair(124f, 147f)
        MapObjectType.BATHTUB -> Pair(189f, 84f)
        MapObjectType.CHAIR_1 -> Pair(57.6f, 62f)
        MapObjectType.CHAIR_2 -> Pair(57.6f, 62f)
        MapObjectType.DOOR_LEFT, MapObjectType.DOOR_RIGHT -> Pair(64.5f, 64f)
        MapObjectType.DOUBLE_SOFA -> Pair(189.5f, 91.5f)
        MapObjectType.HIGH_CABINET_L -> Pair(186.5f, 66f)
        MapObjectType.HIGH_CABINET_S -> Pair(125f, 64.5f)
        MapObjectType.REFRIGERATOR -> Pair(64.5f, 65f)
        MapObjectType.TABLE_L -> Pair(188.5f, 65f)
        MapObjectType.TABLE_S -> Pair(126f, 64.5f)
        MapObjectType.TABLE_L_SHAPE -> Pair(124f, 125f)
        MapObjectType.BATHROOM_SINK -> Pair(91f, 66f)
        MapObjectType.TOILET -> Pair(89f, 64f)
        MapObjectType.WINDOW -> Pair(64.5f, 16f)
    }
}

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
    MapObjectType.BED            -> "🛏"
    MapObjectType.DOUBLE_BED     -> "🛏"
    MapObjectType.HIGH_CABINET_L -> "🗄"
    MapObjectType.HIGH_CABINET_S -> "🗄"
    MapObjectType.BATHROOM_SINK  -> "🚿"
    MapObjectType.WINDOW         -> "🪟"
    MapObjectType.TOILET         -> "🚽"
    MapObjectType.BATHTUB        -> "🛁"
    MapObjectType.CHAIR_1        -> "🪑"
    MapObjectType.CHAIR_2        -> "🪑"
    MapObjectType.DOOR_LEFT      -> "🚪"
    MapObjectType.DOOR_RIGHT     -> "🚪"
    MapObjectType.DOUBLE_SOFA    -> "🛋"
    MapObjectType.REFRIGERATOR   -> "🧊"
    MapObjectType.TABLE_L        -> "🍽"
    MapObjectType.TABLE_S        -> "🍽"
    MapObjectType.TABLE_L_SHAPE  -> "🍽"
}

fun MapObjectType.chineseName(): String = when (this) {
    MapObjectType.BED            -> "單人床"
    MapObjectType.DOUBLE_BED     -> "雙人床"
    MapObjectType.HIGH_CABINET_L -> "高櫃(大)"
    MapObjectType.HIGH_CABINET_S -> "高櫃(小)"
    MapObjectType.BATHROOM_SINK  -> "洗手台"
    MapObjectType.WINDOW         -> "窗戶"
    MapObjectType.TOILET         -> "馬桶"
    MapObjectType.BATHTUB        -> "浴缸"
    MapObjectType.CHAIR_1        -> "椅子 1"
    MapObjectType.CHAIR_2        -> "椅子 2"
    MapObjectType.DOOR_LEFT      -> "門(左開)"
    MapObjectType.DOOR_RIGHT     -> "門(右開)"
    MapObjectType.DOUBLE_SOFA    -> "雙人沙發"
    MapObjectType.REFRIGERATOR   -> "冰箱"
    MapObjectType.TABLE_L        -> "桌子(長)"
    MapObjectType.TABLE_S        -> "桌子(短)"
    MapObjectType.TABLE_L_SHAPE  -> "L型桌"
}

fun MapObjectType.defaultWidth(): Float = getDefaultDimensions().first
fun MapObjectType.defaultHeight(): Float = getDefaultDimensions().second
