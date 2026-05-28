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
    TABLE_L_SHAPE,
    BOOKSHELF,
    DINING_CHAIR,
    KITCHEN_ISLAND,
    LARGE_INDOOR_PLANT,
    OFFICE_CHAIR,
    OFFICE_DESK,
    PIANO,
    RECTANGULAR_COFFEE_TABLE,
    RECTANGULAR_DINING_TABLE,
    ROUND_DINING_TABLE,
    SHOWER_CABIN,
    SINGLE_SOFA,
    STOVE_COUNTER,
    DRESSING_TABLE,
    TRIPLE_SOFA
}

data class MapObject(
    val id: String = UUID.randomUUID().toString(),
    val type: MapObjectType,
    var x: Float,
    var y: Float,
    val rotation: Float = 0f,
    val width: Float = 100f,
    val height: Float = 100f,
    val scale: Float = 1.0f,
    val colliders: List<Collider> = emptyList()
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
        MapObjectType.BOOKSHELF -> "書架"
        MapObjectType.DINING_CHAIR -> "餐椅"
        MapObjectType.KITCHEN_ISLAND -> "中島"
        MapObjectType.LARGE_INDOOR_PLANT -> "大型盆栽"
        MapObjectType.OFFICE_CHAIR -> "辦公椅"
        MapObjectType.OFFICE_DESK -> "辦公桌"
        MapObjectType.PIANO -> "鋼琴"
        MapObjectType.RECTANGULAR_COFFEE_TABLE -> "長方茶几"
        MapObjectType.RECTANGULAR_DINING_TABLE -> "長方餐桌"
        MapObjectType.ROUND_DINING_TABLE -> "圓形餐桌"
        MapObjectType.SHOWER_CABIN -> "淋浴間"
        MapObjectType.SINGLE_SOFA -> "單人沙發"
        MapObjectType.STOVE_COUNTER -> "廚房檯面"
        MapObjectType.DRESSING_TABLE -> "梳妝台"
        MapObjectType.TRIPLE_SOFA -> "三人沙發"
    }
}

fun MapObjectType.getDefaultDimensions(): Pair<Float, Float> {
    return when (this) {
        MapObjectType.BED -> Pair(90f, 175.5f)
        MapObjectType.DOUBLE_BED -> Pair(187.5f, 185.5f)
        MapObjectType.BATHTUB -> Pair(196.5f, 102.5f)
        MapObjectType.CHAIR_1 -> Pair(134.5f, 132.5f)
        MapObjectType.CHAIR_2 -> Pair(133.5f, 157f)
        MapObjectType.DOOR_LEFT, MapObjectType.DOOR_RIGHT -> Pair(66.25f, 61.75f)
        MapObjectType.DOUBLE_SOFA -> Pair(228f, 102f)
        MapObjectType.HIGH_CABINET_L -> Pair(186.5f, 66f)
        MapObjectType.HIGH_CABINET_S -> Pair(125f, 64.5f)
        MapObjectType.REFRIGERATOR -> Pair(140f, 155f)
        MapObjectType.TABLE_L -> Pair(188.5f, 65f)
        MapObjectType.TABLE_S -> Pair(126f, 64.5f)
        MapObjectType.TABLE_L_SHAPE -> Pair(124f, 125f)
        MapObjectType.BATHROOM_SINK -> Pair(160.5f, 113.5f)
        MapObjectType.BOOKSHELF -> Pair(215.5f, 67f)
        MapObjectType.DINING_CHAIR -> Pair(65f, 75.5f)
        MapObjectType.KITCHEN_ISLAND -> Pair(301f, 85.5f)
        MapObjectType.LARGE_INDOOR_PLANT -> Pair(166.5f, 165.5f)
        MapObjectType.OFFICE_CHAIR -> Pair(113f, 115.5f)
        MapObjectType.OFFICE_DESK -> Pair(220.5f, 184f)
        MapObjectType.PIANO -> Pair(245f, 128.5f)
        MapObjectType.RECTANGULAR_COFFEE_TABLE -> Pair(154.5f, 84f)
        MapObjectType.RECTANGULAR_DINING_TABLE -> Pair(187.5f, 150f)
        MapObjectType.ROUND_DINING_TABLE -> Pair(182f, 182.5f)
        MapObjectType.SHOWER_CABIN -> Pair(159.5f, 153.5f)
        MapObjectType.SINGLE_SOFA -> Pair(110.5f, 114.5f)
        MapObjectType.STOVE_COUNTER -> Pair(193f, 108.5f)
        MapObjectType.DRESSING_TABLE -> Pair(143.5f, 135.5f)
        MapObjectType.TRIPLE_SOFA -> Pair(255f, 115f)
        MapObjectType.TOILET -> Pair(108f, 171f)
        MapObjectType.WINDOW -> Pair(64.5f, 16f)
    }
}

sealed class Collider {
    abstract val offsetX: Float
    abstract val offsetY: Float
}

data class RectCollider(
    override val offsetX: Float,
    override val offsetY: Float,
    val width: Float,
    val height: Float
) : Collider()

data class CircleCollider(
    override val offsetX: Float,
    override val offsetY: Float,
    val radius: Float
) : Collider()

fun MapObjectType.getDefaultColliders(): List<Collider> {
    val (w, h) = this.getDefaultDimensions()
    return when (this) {
        MapObjectType.OFFICE_DESK -> {
            listOf(
                RectCollider(0f, -h/4f, w, h/2f),
                RectCollider(0f, h/4f, 60f, 60f)
            )
        }
        MapObjectType.ROUND_DINING_TABLE -> {
            val r = w / 3f
            val chairSize = 40f
            listOf(
                CircleCollider(0f, 0f, r),
                RectCollider(0f, -(r + chairSize/2), chairSize, chairSize),
                RectCollider(0f, (r + chairSize/2), chairSize, chairSize),
                RectCollider(-(r + chairSize/2), 0f, chairSize, chairSize),
                RectCollider((r + chairSize/2), 0f, chairSize, chairSize)
            )
        }
        MapObjectType.TABLE_L_SHAPE -> {
            val t = 30f // 桌板厚度約為 40 dp
            listOf(
                // 左側垂直桌板
                RectCollider(-w/2f + t/2f, 0f, t, h),
                // 上方水平桌板 (扣除左上角重疊部分)
                RectCollider(t/2f, -h/2f + t/2f, w - t, t)
            )
        }
        else -> emptyList()
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
    val savedBoundaryId: String? = null, // reference to a SavedBoundary if using one
    val innerWalls: List<List<PointF>> = emptyList()
)

// 已儲存的自訂邊界
data class SavedBoundary(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val vertices: List<PointF>,
    val innerWalls: List<List<PointF>> = emptyList()
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
    MapObjectType.BOOKSHELF -> "📚"
    MapObjectType.DINING_CHAIR -> "🪑"
    MapObjectType.KITCHEN_ISLAND -> "🍳"
    MapObjectType.LARGE_INDOOR_PLANT -> "🪴"
    MapObjectType.OFFICE_CHAIR -> "🪑"
    MapObjectType.OFFICE_DESK -> "💻"
    MapObjectType.PIANO -> "🎹"
    MapObjectType.RECTANGULAR_COFFEE_TABLE -> "☕"
    MapObjectType.RECTANGULAR_DINING_TABLE -> "🍽"
    MapObjectType.ROUND_DINING_TABLE -> "🍽"
    MapObjectType.SHOWER_CABIN -> "🚿"
    MapObjectType.SINGLE_SOFA -> "🛋"
    MapObjectType.STOVE_COUNTER -> "🍳"
    MapObjectType.DRESSING_TABLE -> "🪞"
    MapObjectType.TRIPLE_SOFA -> "🛋"
}

fun MapObjectType.chineseName(): String = getDisplayName()

fun MapObjectType.defaultWidth(): Float = getDefaultDimensions().first
fun MapObjectType.defaultHeight(): Float = getDefaultDimensions().second

data class PredictionResult(
    val itemId: String = "",
    val points: List<PointF> = emptyList(),
    val lines: List<Pair<PointF, PointF>> = emptyList()
)

