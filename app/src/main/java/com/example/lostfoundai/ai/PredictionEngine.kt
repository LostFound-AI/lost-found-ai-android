package com.example.lostfoundai.ai

import com.example.lostfoundai.data.ItemCategory
import com.example.lostfoundai.data.ItemSize
import com.example.lostfoundai.data.MapObject
import com.example.lostfoundai.data.MapObjectType
import com.example.lostfoundai.data.MissingItem
import com.example.lostfoundai.data.PointF
import kotlin.math.pow
import kotlin.math.sqrt

class PredictionEngine {

    // Returns top N high-probability spots in dp coordinates.
    // All inputs (mapObjects.x/y, walkPath) must also be in dp.
    fun calculatePrediction(
        item: MissingItem,
        mapObjects: List<MapObject>,
        walkPath: List<PointF>,
        topN: Int = 3
    ): List<Pair<Float, Float>> {
        val spots = mutableListOf<Pair<Pair<Float, Float>, Float>>()

        // Grid in dp — covers up to 800×1200dp (larger than any phone screen)
        for (x in 0..800 step 20) {
            for (y in 0..1200 step 20) {
                val xf = x.toFloat()
                val yf = y.toFloat()

                // Skip positions occupied by solid large furniture
                val isBlockedByFurniture = mapObjects.any { obj ->
                    val solidTypes = setOf(
                        MapObjectType.BED, MapObjectType.DOUBLE_BED,
                        MapObjectType.DOUBLE_SOFA,
                        MapObjectType.REFRIGERATOR,
                        MapObjectType.TABLE_L, MapObjectType.TABLE_S, MapObjectType.TABLE_L_SHAPE
                    )
                    obj.type in solidTypes &&
                        xf >= obj.x && xf <= obj.x + obj.width &&
                        yf >= obj.y && yf <= obj.y + obj.height
                }
                if (isBlockedByFurniture) continue

                var pBase = 1.0f
                val wSize = when (item.size) {
                    ItemSize.VERY_SMALL -> 0.5f
                    ItemSize.SMALL      -> 0.3f
                    ItemSize.MEDIUM     -> 0.1f
                    ItemSize.LARGE      -> 0.0f
                }
                var wPath = 0.0f
                var wLocation = 0.0f
                var wGravity = 0.0f

                // Rule B: Proximity to walk path (30dp radius)
                for (p in walkPath) {
                    if (distance(xf, yf, p.x, p.y) < 30f) {
                        wPath = 0.3f
                        break
                    }
                }

                for (obj in mapObjects) {
                    val dist = distance(xf, yf, obj.x + obj.width / 2f, obj.y + obj.height / 2f)

                    // Rule A: Gravity — small items fall near furniture edges (within 30dp)
                    if ((item.size == ItemSize.VERY_SMALL || item.size == ItemSize.SMALL) &&
                        obj.type in setOf(
                            MapObjectType.BED, MapObjectType.DOUBLE_BED,
                            MapObjectType.DOUBLE_SOFA,
                            MapObjectType.TABLE_L, MapObjectType.TABLE_S, MapObjectType.TABLE_L_SHAPE
                        )
                    ) {
                        if (dist < 30f) wGravity = maxOf(wGravity, 0.5f)
                    }

                    // Rule C: Category affinity (within 80dp of relevant furniture)
                    when (item.category) {
                        ItemCategory.BATHROOM ->
                            if (obj.type == MapObjectType.BATHROOM_SINK && dist < 80f) wLocation += 0.5f
                        ItemCategory.ACCESSORY ->
                            if (obj.type in setOf(
                                    MapObjectType.BED, MapObjectType.DOUBLE_BED,
                                    MapObjectType.HIGH_CABINET_L, MapObjectType.HIGH_CABINET_S
                                ) && dist < 80f
                            ) wLocation += 0.4f
                        ItemCategory.ELECTRONICS ->
                            if (obj.type in setOf(
                                    MapObjectType.TABLE_L, MapObjectType.TABLE_S,
                                    MapObjectType.TABLE_L_SHAPE,
                                    MapObjectType.BED, MapObjectType.DOUBLE_BED
                                ) && dist < 80f
                            ) wLocation += 0.4f
                        ItemCategory.CLOTHING ->
                            if (obj.type in setOf(
                                    MapObjectType.HIGH_CABINET_L, MapObjectType.HIGH_CABINET_S,
                                    MapObjectType.CHAIR_1, MapObjectType.CHAIR_2
                                ) && dist < 80f
                            ) wLocation += 0.4f
                        ItemCategory.BELONGINGS ->
                            if (obj.type in setOf(
                                    MapObjectType.TABLE_L, MapObjectType.TABLE_S,
                                    MapObjectType.TABLE_L_SHAPE,
                                    MapObjectType.HIGH_CABINET_L, MapObjectType.HIGH_CABINET_S,
                                    MapObjectType.CHAIR_1, MapObjectType.CHAIR_2
                                ) && dist < 80f
                            ) wLocation += 0.3f
                        else -> {}
                    }
                }

                val pFinal = pBase * (1 + wSize + wPath + wLocation + wGravity)
                val noise = (Math.random() * 0.01).toFloat()
                spots.add(Pair(Pair(xf, yf), pFinal + noise))
            }
        }

        spots.sortByDescending { it.second }
        return spots.take(topN).map { it.first }
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt((x1 - x2).pow(2) + (y1 - y2).pow(2))
    }
}
