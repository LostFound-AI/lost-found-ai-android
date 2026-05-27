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
                val otherW = other.width * other.scale
                val otherH = other.height * other.scale
                px < other.x + otherW && px + w > other.x &&
                py < other.y + otherH && py + h > other.y
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

    /**
     * Check if a rectangle intersects a line segment (p1 -> p2).
     * Assumes the line is perfectly horizontal or vertical (orthogonal).
     */
    fun rectIntersectsLine(x: Float, y: Float, w: Float, h: Float, p1: PointF, p2: PointF): Boolean {
        val minX = min(p1.x, p2.x)
        val maxX = max(p1.x, p2.x)
        val minY = min(p1.y, p2.y)
        val maxY = max(p1.y, p2.y)

        val rectLeft = x
        val rectRight = x + w
        val rectTop = y
        val rectBottom = y + h

        // If line is horizontal
        if (p1.y == p2.y) {
            val isYOverlapping = (p1.y >= rectTop && p1.y <= rectBottom)
            val isXOverlapping = (rectLeft <= maxX && rectRight >= minX)
            return isYOverlapping && isXOverlapping
        }
        
        // If line is vertical
        if (p1.x == p2.x) {
            val isXOverlapping = (p1.x >= rectLeft && p1.x <= rectRight)
            val isYOverlapping = (rectTop <= maxY && rectBottom >= minY)
            return isXOverlapping && isYOverlapping
        }

        // For non-orthogonal lines (fallback)
        // A simple bounding box check
        if (rectRight < minX || rectLeft > maxX || rectBottom < minY || rectTop > maxY) return false
        
        // Check if endpoints are inside rect
        if (p1.x in rectLeft..rectRight && p1.y in rectTop..rectBottom) return true
        if (p2.x in rectLeft..rectRight && p2.y in rectTop..rectBottom) return true

        return false // Detailed segment-segment intersection is skipped for simplicity as we enforce orthogonal inner walls.
    }

    /**
     * Check if two line segments (p1->p2 and p3->p4) intersect.
     */
    fun lineIntersectsLine(p1: PointF, p2: PointF, p3: PointF, p4: PointF): Boolean {
        fun ccw(A: PointF, B: PointF, C: PointF): Boolean {
            return (C.y - A.y) * (B.x - A.x) > (B.y - A.y) * (C.x - A.x)
        }
        val intersect = ccw(p1, p3, p4) != ccw(p2, p3, p4) && ccw(p1, p2, p3) != ccw(p1, p2, p4)
        
        // Collinear check for overlapping orthogonal lines
        if (p1.x == p2.x && p2.x == p3.x && p3.x == p4.x) {
            val min1 = min(p1.y, p2.y); val max1 = max(p1.y, p2.y)
            val min2 = min(p3.y, p4.y); val max2 = max(p3.y, p4.y)
            return max1 > min2 && max2 > min1
        }
        if (p1.y == p2.y && p2.y == p3.y && p3.y == p4.y) {
            val min1 = min(p1.x, p2.x); val max1 = max(p1.x, p2.x)
            val min2 = min(p3.x, p4.x); val max2 = max(p3.x, p4.x)
            return max1 > min2 && max2 > min1
        }
        return intersect
    }

    private infix fun Float.to(other: Float): PointF = PointF(this, other)

    /**
     * Project a point (px, py) onto a line segment (a -> b) and return the closest point on the segment.
     */
    fun projectPointOntoSegment(px: Float, py: Float, a: PointF, b: PointF): PointF {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val lenSq = dx * dx + dy * dy
        if (lenSq == 0f) return PointF(a.x, a.y) // Segment is just a point

        // Calculate the parameter t of the projection on the line
        var t = ((px - a.x) * dx + (py - a.y) * dy) / lenSq
        t = max(0f, min(1f, t)) // Clamp to segment

        return PointF(a.x + t * dx, a.y + t * dy)
    }

    /**
     * Calculate the shortest distance from a point (px, py) to a line segment (a -> b).
     */
    fun distanceToSegment(px: Float, py: Float, a: PointF, b: PointF): Float {
        val proj = projectPointOntoSegment(px, py, a, b)
        val dx = px - proj.x
        val dy = py - proj.y
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Adapt inner walls when the boundary shape changes.
     */
    fun adaptInnerWallsToNewBoundary(
        innerWalls: List<List<PointF>>,
        oldBoundary: List<PointF>,
        newBoundary: List<PointF>
    ): List<List<PointF>> {
        if (newBoundary.size < 3) return innerWalls
        return innerWalls.map { wall ->
            wall.map { node ->
                var wasSnapped = false
                if (oldBoundary.size >= 3) {
                    for (i in oldBoundary.indices) {
                        val p1 = oldBoundary[i]
                        val p2 = oldBoundary[(i + 1) % oldBoundary.size]
                        if (distanceToSegment(node.x, node.y, p1, p2) < 5f) {
                            wasSnapped = true
                            break
                        }
                    }
                }

                if (wasSnapped || !pointInPolygon(node.x, node.y, newBoundary)) {
                    // Find nearest segment on new boundary
                    var minDistance = Float.MAX_VALUE
                    var nearestPoint = node
                    for (i in newBoundary.indices) {
                        val np1 = newBoundary[i]
                        val np2 = newBoundary[(i + 1) % newBoundary.size]
                        val proj = projectPointOntoSegment(node.x, node.y, np1, np2)
                        val dx = node.x - proj.x
                        val dy = node.y - proj.y
                        val dist = sqrt(dx * dx + dy * dy)
                        if (dist < minDistance) {
                            minDistance = dist
                            nearestPoint = proj
                        }
                    }
                    nearestPoint
                } else {
                    node
                }
            }
        }
    }
}
