package com.example.lostfoundai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.platform.LocalContext
import com.example.lostfoundai.ai.PredictionEngine
import com.example.lostfoundai.data.SharedPrefsItemRepository
import com.example.lostfoundai.ui.screens.MapScreen
import com.example.lostfoundai.ui.screens.MapViewModel
import com.example.lostfoundai.ui.screens.SearchScreen
import com.example.lostfoundai.ui.screens.SearchViewModel
import com.example.lostfoundai.ui.theme.LostFoundAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LostFoundAITheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LostFoundApp()
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        if (ev?.action == android.view.MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v != null) {
                val rect = android.graphics.Rect()
                v.getGlobalVisibleRect(rect)
                if (!rect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    v.clearFocus()
                    val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
                    imm.hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }
}

@Composable
fun LostFoundApp() {
    val navController = rememberNavController()

    // Shared simple dependencies
    val context = LocalContext.current
    val repository = remember { SharedPrefsItemRepository(context) }
    val predictionEngine = remember { PredictionEngine() }

    val mapViewModel = remember { MapViewModel(repository, predictionEngine) }
    val searchViewModel = remember { SearchViewModel(repository) }

    val rooms by repository.getRooms().collectAsState(initial = emptyList())

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            com.example.lostfoundai.ui.screens.HomeScreen(
                rooms = rooms,
                onEnterRoom = { roomId -> 
                    repository.switchRoom(roomId)
                    navController.navigate("map") 
                },
                onAddRoom = { name -> repository.addRoom(name) },
                onDeleteRoom = { id -> repository.deleteRoom(id) },
                onRenameRoom = { id, name -> repository.renameRoom(id, name) }
            )
        }
        composable("map") {
            MapScreen(
                mapViewModel = mapViewModel,
                searchViewModel = searchViewModel,
                onNavigateHome = {
                    navController.navigate("home") {
                        popUpTo(0)
                    }
                }
            )
        }
    }
}