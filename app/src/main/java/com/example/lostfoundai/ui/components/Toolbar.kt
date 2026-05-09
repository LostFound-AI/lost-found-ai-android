package com.example.lostfoundai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.lostfoundai.data.MapObjectType
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun Toolbar(
    onDragStart: (MapObjectType, androidx.compose.ui.geometry.Offset) -> Unit,
    onDrag: (androidx.compose.ui.geometry.Offset) -> Unit,
    onDragEnd: (MapObjectType) -> Unit
) {
    val items = MapObjectType.values().toList()

    Column(
        modifier = Modifier
            .width(80.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp)
    ) {
        Text("物件", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(8.dp))
        
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            items(items) { type ->
                var globalPos by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
                var showName by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.White)
                        .clickable { showName = !showName }
                        .onGloballyPositioned { coords: androidx.compose.ui.layout.LayoutCoordinates ->
                            globalPos = coords.positionInRoot()
                        }
                        .pointerInput(Unit) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { localOffset: Offset ->
                                    onDragStart(type, globalPos + localOffset)
                                },
                                onDrag = { change: PointerInputChange, dragAmount: Offset ->
                                    change.consume()
                                    onDrag(dragAmount)
                                },
                                onDragEnd = { onDragEnd(type) },
                                onDragCancel = { onDragEnd(type) }
                            )
                        }
                ) {
                    com.example.lostfoundai.ui.screens.MapObjectVisuals(
                        type = type,
                        showName = showName,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
