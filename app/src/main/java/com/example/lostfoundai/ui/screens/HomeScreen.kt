package com.example.lostfoundai.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.lostfoundai.data.RoomData
import com.example.lostfoundai.ui.theme.*

private val RoomAccentColors = listOf(
    Color(0xFF4361EE), Color(0xFF06D6A0), Color(0xFFFF6B6B),
    Color(0xFFFFBE0B), Color(0xFF8338EC), Color(0xFFFB5607),
)

@Composable
fun HomeScreen(
    rooms: List<RoomData>,
    onEnterRoom: (String) -> Unit,
    onAddRoom: (String) -> Unit,
    onDeleteRoom: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var newRoomName by remember { mutableStateOf("") }

    Scaffold(
        containerColor = BackgroundColor,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("新增房間") },
                containerColor = PrimaryIndigo,
                contentColor = Color.White
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(PrimaryIndigoDark, PrimaryIndigo)
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 40.dp)
            ) {
                Column {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔍", fontSize = 28.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "LostFound AI",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "智慧室內遺失物定位系統",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }

            // Section header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "我的房間",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceColor
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = PrimaryContainer
                ) {
                    Text(
                        "${rooms.size}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = OnPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (rooms.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("🏠", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "還沒有房間",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurfaceVariantColor
                    )
                    Text(
                        "點擊右下角按鈕新增第一個房間",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceVariantColor,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    itemsIndexed(rooms) { index, room ->
                        val accent = RoomAccentColors[index % RoomAccentColors.size]
                        RoomCard(
                            room = room,
                            accentColor = accent,
                            onEnter = { onEnterRoom(room.id) },
                            onDelete = if (room.id != "default") ({ onDeleteRoom(room.id) }) else null
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = {
                    showAddDialog = false
                    newRoomName = ""
                },
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text("新增房間", fontWeight = FontWeight.Bold)
                },
                text = {
                    OutlinedTextField(
                        value = newRoomName,
                        onValueChange = { newRoomName = it },
                        label = { Text("房間名稱") },
                        placeholder = { Text("例如：臥室、客廳") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newRoomName.isNotBlank()) {
                                onAddRoom(newRoomName.trim())
                                newRoomName = ""
                                showAddDialog = false
                            }
                        },
                        enabled = newRoomName.isNotBlank()
                    ) {
                        Text("新增")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showAddDialog = false
                        newRoomName = ""
                    }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
private fun RoomCard(
    room: RoomData,
    accentColor: Color,
    onEnter: () -> Unit,
    onDelete: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent bar
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(72.dp)
                    .background(accentColor, RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
            )

            // Room icon
            Box(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🏠", fontSize = 20.sp)
            }

            // Room name
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp)
            ) {
                Text(
                    text = room.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = OnSurfaceColor
                )
                Text(
                    text = "點擊進入查看地圖",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariantColor
                )
            }

            // Actions
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 12.dp)
            ) {
                FilledTonalButton(
                    onClick = onEnter,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = accentColor.copy(alpha = 0.15f),
                        contentColor = accentColor
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("進入", fontWeight = FontWeight.SemiBold)
                }
                if (onDelete != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "刪除",
                            tint = ErrorRed.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
