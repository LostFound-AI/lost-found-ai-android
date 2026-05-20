package com.example.lostfoundai.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.lostfoundai.data.ItemCategory
import com.example.lostfoundai.data.ItemSize
import com.example.lostfoundai.ui.components.KeyboardAccessoryProvider
import com.example.lostfoundai.ui.components.AccessoryOutlinedTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onNavigateToAI: (String) -> Unit
) {
    val items by viewModel.filteredItems.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("查詢與新增物品") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, "新增物品")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("歷史紀錄與清單", style = MaterialTheme.typography.titleMedium)
            
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(items) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onNavigateToAI(item.id) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(text = item.name, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "類別: ${item.category.name} | 大小: ${item.size.name}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, cat, size, traits, weight, loc ->
                viewModel.addMissingItem(name, cat, size, traits, weight, loc)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun AddItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, ItemCategory, ItemSize, String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    // Hardcoded defaults for demo
    var category by remember { mutableStateOf(ItemCategory.ACCESSORY) }
    var size by remember { mutableStateOf(ItemSize.VERY_SMALL) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新增物品") },
        text = {
            KeyboardAccessoryProvider {
                Column {
                    AccessoryOutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("物品名稱") }
                    )
                    // Need dropdowns for Category/Size here in real app
                    Text("預設為 飾品(極小) 方便測試", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(name, category, size, "易滾動", "High", "臥室")
                }
            ) {
                Text("新增")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
