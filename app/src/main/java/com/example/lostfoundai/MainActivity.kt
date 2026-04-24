package com.example.lostfoundai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
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

    NavHost(navController = navController, startDestination = "map") {
        composable("map") {
            MapScreen(
                mapViewModel = mapViewModel,
                searchViewModel = searchViewModel
            )
        }
    }
}