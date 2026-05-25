package com.example.lostfoundai.ai

import com.example.lostfoundai.data.ItemCategory
import com.example.lostfoundai.data.ItemSize
import com.example.lostfoundai.data.MapObject
import com.example.lostfoundai.data.MapObjectType
import com.example.lostfoundai.data.MissingItem
import com.example.lostfoundai.data.PointF
import com.example.lostfoundai.data.PredictionResult
import com.example.lostfoundai.data.chineseName
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig // 確保有 import 這個
import org.json.JSONObject
import kotlin.math.pow
import kotlin.math.sqrt

class PredictionEngine {

    // Returns PredictionResult containing likely points and lines in dp coordinates.
    // All inputs must also be in dp.
    suspend fun calculatePrediction(
        item: MissingItem,
        mapObjects: List<MapObject>,
        walkPath: List<PointF>,
        boundary: List<PointF>
    ): PredictionResult {
        val apiKey = GeminiConfig.API_KEY
        if (apiKey.isBlank()) {
            android.util.Log.w("PredictionEngine", "Gemini API key is blank, running fallback prediction")
            return calculateFallback(item, mapObjects, walkPath)
        }

        try {
            val model = GenerativeModel(
                modelName = "gemini-3.5-flash",
                apiKey = apiKey
                // 這是讓模型 100% 穩定吐出 JSON 的終極大招，不論換什麼模型都不會因為格式而 Crash！
//                generationConfig = generationConfig {
//                    responseMimeType = "application/json"
//                }
            )

            val roomVerticesDescription = boundary.joinToString("\n") { "Point(x=${it.x}, y=${it.y})" }
            val furnitureDescription = mapObjects.joinToString("\n") { obj ->
                "Object: ${obj.type.name} (DisplayName: ${obj.type.chineseName()}), Coordinates: x=${obj.x}, y=${obj.y}, Width: ${obj.width}, Height: ${obj.height}, Scale: ${obj.scale}"
            }
            val walkPathDescription = walkPath.joinToString("\n") { "PathPoint(x=${it.x}, y=${it.y})" }

            val prompt = """
                You are an AI assistant helping a user locate a lost item in a room.
                We have a 2D map of the room represented in virtual density-independent pixels (dp).
                Please analyze the room layout, furniture locations, the user's walk path, and the missing item's details to suggest likely spots where the item could be.

                Coordinate space details:
                - Coordinate system has X and Y in dp.
                - Width ranges from 0 to 800 dp, height ranges from 0 to 1200 dp.

                Room Boundary vertices (in dp):
                $roomVerticesDescription

                Furniture and room objects on the map:
                $furnitureDescription

                User's Walk Path coordinates (in order, in dp):
                $walkPathDescription

                Missing Item details:
                - Name: ${item.name}
                - Category: ${item.category.name}
                - Size: ${item.size.name}
                - Physical Traits: ${item.physicalTraits}
                - Last Known Location Description: ${item.lastKnownLocationDesc}

                Please perform the following spatial analysis:
                1. Recommend specific spots (points) where the item is highly likely to be. For example, on a table, on a bed, or a specific location where it could slip.
                2. Recommend path ranges or lines (from a starting point x1, y1 to an ending point x2, y2) where the item might be lost. For example, along a specific walk path segment, or along the edge of a bed or couch, or behind a cabinet.
                3. Keep all suggested coordinates inside the room boundary. Do not place objects inside solid furniture structures (unless it makes sense to be on top of/inside them like a cabinet/table).
                4. Provide up to 5 points and up to 3 lines.

                Output the result STRICTLY as a JSON object with this exact structure:
                {
                  "points": [
                    {"x": 120.0, "y": 350.0, "reason": "Reason in Traditional Chinese"}
                  ],
                  "lines": [
                    {"x1": 100.0, "y1": 200.0, "x2": 300.0, "y2": 200.0, "reason": "Reason in Traditional Chinese"}
                  ]
                }
                Do not include any markdown formatting (like ```json ... ```) or extra text outside the JSON. Return only the raw JSON string.
            """.trimIndent()

            val response = model.generateContent(
                content {
                    text(prompt)
                }
            )

            val raw = response.text ?: throw Exception("Gemini returned empty response")
            val startIdx = raw.indexOf('{')
            val endIdx = raw.lastIndexOf('}')
            val jsonStr = if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                raw.substring(startIdx, endIdx + 1)
            } else {
                raw
            }

            val json = JSONObject(jsonStr)
            val pointsArray = json.optJSONArray("points")
            val linesArray = json.optJSONArray("lines")

            val points = mutableListOf<PointF>()
            if (pointsArray != null) {
                for (i in 0 until pointsArray.length()) {
                    val obj = pointsArray.getJSONObject(i)
                    val x = obj.getDouble("x").toFloat()
                    val y = obj.getDouble("y").toFloat()
                    points.add(PointF(x, y))
                }
            }

            val lines = mutableListOf<Pair<PointF, PointF>>()
            if (linesArray != null) {
                for (i in 0 until linesArray.length()) {
                    val obj = linesArray.getJSONObject(i)
                    val x1 = obj.getDouble("x1").toFloat()
                    val y1 = obj.getDouble("y1").toFloat()
                    val x2 = obj.getDouble("x2").toFloat()
                    val y2 = obj.getDouble("y2").toFloat()
                    lines.add(Pair(PointF(x1, y1), PointF(x2, y2)))
                }
            }

            return PredictionResult(points = points, lines = lines)
        } catch (e: Exception) {
            android.util.Log.e("PredictionEngine", "Gemini prediction failed, running fallback", e)
            return calculateFallback(item, mapObjects, walkPath)
        }
    }

    private fun calculateFallback(
        item: MissingItem,
        mapObjects: List<MapObject>,
        walkPath: List<PointF>,
        topN: Int = 3
    ): PredictionResult {
        val spots = mutableListOf<Pair<Pair<Float, Float>, Float>>()

        // Grid in dp — covers up to 800×1200dp
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
        val points = spots.take(topN).map { PointF(it.first.first, it.first.second) }

        val lines = mutableListOf<Pair<PointF, PointF>>()
        if (walkPath.size >= 2) {
            for (i in 0 until minOf(3, walkPath.size - 1)) {
                lines.add(Pair(walkPath[i], walkPath[i + 1]))
            }
        }

        return PredictionResult(points = points, lines = lines)
    }

    private fun distance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt((x1 - x2).pow(2) + (y1 - y2).pow(2))
    }
}
