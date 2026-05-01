package com.example.lostfoundai.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.PointerInputChange
import com.example.lostfoundai.data.MapObjectType
import com.example.lostfoundai.data.chineseName
import com.example.lostfoundai.data.emoji
import com.example.lostfoundai.ui.screens.MapObjectVisuals
import com.example.lostfoundai.ui.theme.OnSurfaceVariantColor
import com.example.lostfoundai.ui.theme.PrimaryIndigo
import com.example.lostfoundai.ui.theme.SurfaceColor

@Composable
fun Toolbar(
    onDragStart: (MapObjectType, Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: (MapObjectType) -> Unit
) {
    val items = MapObjectType.values().toList()

    Column(
        modifier = Modifier
            .width(80.dp)
            .fillMaxHeight()
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            .background(SurfaceColor)
            .padding(vertical = 8.dp, horizontal = 4.dp)
    ) {
        Text(
            "物件庫",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = PrimaryIndigo,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(items) { type ->
                var globalPos by remember { mutableStateOf(Offset.Zero) }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .onGloballyPositioned { coords: LayoutCoordinates ->
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
                        MapObjectVisuals(
                            type = type,
                            isBed = type == MapObjectType.BED,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Text(
                        text = type.chineseName(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        color = OnSurfaceVariantColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
