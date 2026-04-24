package com.example.lostfoundai.utils

import com.example.lostfoundai.data.MapObject
import com.example.lostfoundai.data.PointF
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Utility functions for polygon-based boundary collision detection.
 */
object BoundaryUtils {

    /**
     * Ray-casting algorithm to determine if a point is inside a polygon.
     * Casts a horizontal ray to the right and counts edge crossings.
     */
    fun pointInPolygon(px: Float, py: Float, vertices: List<PointF>): Boolean {
        if (vertices.size < 3) return false
        var inside = false
        var j = vertices.size - 1
        for (i in vertices.indices) {
            val vi = vertices[i]
            val vj = vertices[j]
            if ((vi.y > py) != (vj.y > py) &&
                px < (vj.x - vi.x) * (py - vi.y) / (vj.y - vi.y) + vi.x
            ) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    /**
     * Check if an axis-aligned rectangle (defined by top-left corner + width/height)
     * is fully contained within the polygon.
     * Tests all four corners.
     */
    fun rectInPolygon(x: Float, y: Float, w: Float, h: Float, vertices: List<PointF>): Boolean {
        if (vertices.size < 3) return true // No boundary = always valid
        return pointInPolygon(x, y, vertices) &&
               pointInPolygon(x + w, y, vertices) &&
               pointInPolygon(x + w, y + h, vertices) &&
               pointInPolygon(x, y + h, vertices)
    }

    /**
     * Find the centroid of the polygon.
     */
    fun polygonCentroid(vertices: List<PointF>): PointF {
        if (vertices.isEmpty()) return PointF(0f, 0f)
        val cx = vertices.map { it.x }.average().toFloat()
        val cy = vertices.map { it.y }.average().toFloat()
        return cx to cy
    }

    /**
     * Find the bounding box of the polygon, returned as (minX, minY, maxX, maxY).
     */
    fun polygonBoundingBox(vertices: List<PointF>): FloatArray {
        if (vertices.isEmpty()) return floatArrayOf(0f, 0f, 0f, 0f)
        var minX = Float.MAX_VALUE; var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE; var maxY = Float.MIN_VALUE
        for (v in vertices) {
            if (v.x < minX) minX = v.x
            if (v.y < minY) minY = v.y
            if (v.x > maxX) maxX = v.x
            if (v.y > maxY) maxY = v.y
        }
        return floatArrayOf(minX, minY, maxX, maxY)
    }

    /**
     * Find the nearest position inside the polygon for a rectangle, optionally avoiding other rectangles.
     */
    fun findNearestInsidePosition(
        x: Float, y: Float, w: Float, h: Float,
        vertices: List<PointF>,
        avoidRects: List<MapObject> = emptyList()
    ): PointF {
        if (vertices.size < 3) return PointF(x, y)
        
        fun isValid(px: Float, py: Float): Boolean {
            if (!rectInPolygon(px, py, w, h, vertices)) return false
            return avoidRects.none { other ->
                px < other.x + other.width && px + w > other.x &&
                py < other.y + other.height && py + h > other.y
            }
        }

        if (isValid(x, y)) return PointF(x, y)

        val (cx, cy) = polygonCentroid(vertices)
        val rectCenterX = x + w / 2f
        val rectCenterY = y + h / 2f
        
        // 1. Iterative move towards centroid
        val dx = cx - rectCenterX
        val dy = cy - rectCenterY
        val dist = sqrt(dx * dx + dy * dy)
        if (dist > 0.01f) {
            val stepX = dx / dist
            val stepY = dy / dist
            val stepSize = 5f // dp per step
            var curX = x
            var curY = y
            val maxSteps = (dist / stepSize).toInt() + 1
            for (i in 0..maxSteps) {
                if (isValid(curX, curY)) {
                    return PointF(curX, curY)
                }
                curX += stepX * stepSize
                curY += stepY * stepSize
            }
        }

        // 2. Sampling search within bounding box (Fallback)
        val bbox = polygonBoundingBox(vertices)
        var minSqDist = Float.MAX_VALUE
        var bestPos = PointF(cx - w/2f, cy - h/2f) // fallback to centroid area
        
        val gridStep = 10f // dp
        var sx = bbox[0]
        while (sx < bbox[2] - w) {
            var sy = bbox[1]
            while (sy < bbox[3] - h) {
                if (isValid(sx, sy)) {
                    val d2 = (sx - x) * (sx - x) + (sy - y) * (sy - y)
                    if (d2 < minSqDist) {
                        minSqDist = d2
                        bestPos = PointF(sx, sy)
                    }
                }
                sy += gridStep
            }
            sx += gridStep
        }

        return bestPos
    }

    /**
     * Clamp a rectangle position so it stays inside the polygon boundary.
     * If the current position is valid, returns it as-is.
     * If invalid, returns the last valid position (or finds nearest inside).
     */
    fun clampRectToPolygon(
        x: Float, y: Float, w: Float, h: Float,
        lastValidX: Float, lastValidY: Float,
        hasLastValid: Boolean,
        vertices: List<PointF>
    ): PointF {
        if (vertices.size < 3) return PointF(x, y)
        if (rectInPolygon(x, y, w, h, vertices)) return PointF(x, y)
        // If we have a last valid position, stick to it
        if (hasLastValid) return PointF(lastValidX, lastValidY)
        // Otherwise, find the nearest inside position
        return findNearestInsidePosition(x, y, w, h, vertices)
    }

    private infix fun Float.to(other: Float): PointF = PointF(this, other)
}
