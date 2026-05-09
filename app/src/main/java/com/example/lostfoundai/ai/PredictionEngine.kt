package com.example.lostfoundai.ai

import com.example.lostfoundai.data.ItemCategory
import com.example.lostfoundai.data.ItemSize
import com.example.lostfoundai.data.MapObject
import com.example.lostfoundai.data.MapObjectType
import com.example.lostfoundai.data.MissingItem
import kotlin.math.pow
import kotlin.math.sqrt

class PredictionEngine {

    // Returns top N high probability spots (x, y)
    fun calculatePrediction(
        item: MissingItem,
        mapObjects: List<MapObject>,
        walkPath: List<Pair<Float, Float>>,
        topN: Int = 3
    ): List<Pair<Float, Float>> {
        val spots = mutableListOf<Pair<Pair<Float, Float>, Float>>()

        // Generate a grid of points (step by 50px)
        for (x in 0..1000 step 50) {
            for (y in 0..1000 step 50) {
                var pBase = 1.0f
                var wSize = when (item.size) {
                    ItemSize.VERY_SMALL -> 0.5f
                    ItemSize.SMALL -> 0.3f
                    ItemSize.MEDIUM -> 0.1f
                    ItemSize.LARGE -> 0.0f
                }
                var wPath = 0.0f
                var wLocation = 0.0f
                var wGravity = 0.0f

                // Rule B: Inertia Path - Distance to any path coordinate < 50px
                for (p in walkPath) {
                    val dist = distance(x.toFloat(), y.toFloat(), p.first, p.second)
                    if (dist < 50f) {
                        wPath += 0.3f
                        break // cap 0.3
                    }
                }

                for (obj in mapObjects) {
                    val dist = distance(x.toFloat(), y.toFloat(), obj.x, obj.y)
                    
                    // Rule A: Gravity & Gap for small items within 30px
                    if ((item.size == ItemSize.VERY_SMALL || item.size == ItemSize.SMALL) &&
                        (obj.type == MapObjectType.BED || obj.type == MapObjectType.DOUBLE_SOFA || obj.type == MapObjectType.TABLE_S)) {
                        if (dist < 80f) {
                            wGravity = 0.5f // increased if near furniture
                        }
                    }

                    // Rule C: Affinity
                    if (item.category == ItemCategory.BATHROOM && obj.type == MapObjectType.BATHROOM_SINK && dist < 100f) {
                        wLocation += 0.5f
                    }
                    if (item.category == ItemCategory.ACCESSORY && (obj.type == MapObjectType.BED || obj.type == MapObjectType.HIGH_CABINET_S) && dist < 100f) {
                        wLocation += 0.5f
                    }
                    if (item.category == ItemCategory.ELECTRONICS && (obj.type == MapObjectType.BED || obj.type == MapObjectType.DOUBLE_SOFA) && dist < 50f) {
                        wLocation += 0.4f
                    }
                }

                val pFinal = pBase * (1 + wSize + wPath + wLocation + wGravity)
                
                // Add tiny random noise to avoid duplicate scores
                val noise = (Math.random() * 0.01).toFloat()
                spots.add(Pair(Pair(x.toFloat(), y.toFloat()), pFinal + noise))
            }
        }

        spots.sortByDescending { it.second }
        return spots.take(topN).map { it.first }
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt((x1 - x2).pow(2) + (y1 - y2).pow(2))
    }
}
