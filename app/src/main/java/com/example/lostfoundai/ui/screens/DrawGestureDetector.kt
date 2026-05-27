package com.example.lostfoundai.ui.screens

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged

suspend fun PointerInputScope.detect1FingerDrawGestures(
    onDragStart: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onDrag: (Offset) -> Unit
) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var isDrawing = false
        var isPanning = false

        do {
            val event = awaitPointerEvent()
            if (event.changes.size >= 2) {
                isPanning = true
                if (isDrawing) {
                    onDragCancel()
                    isDrawing = false
                }
                break
            }

            if (!isPanning && !isDrawing) {
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change != null && change.positionChanged()) {
                    val distance = (change.position - down.position).getDistance()
                    if (distance > viewConfiguration.touchSlop) {
                        isDrawing = true
                        onDragStart(down.position)
                    }
                }
            }

            if (isDrawing) {
                val change = event.changes.firstOrNull { it.id == down.id }
                if (change != null && change.positionChanged()) {
                    onDrag(change.position)
                    change.consume()
                }
            }
        } while (event.changes.any { it.pressed })

        if (isDrawing) {
            onDragEnd()
        }
    }
}
