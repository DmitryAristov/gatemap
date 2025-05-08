package com.daristov.checkpoint

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.daristov.checkpoint.ui.screens.AboutScreen
import com.daristov.checkpoint.ui.screens.AlarmCameraScreen
import com.daristov.checkpoint.ui.screens.CheckpointListScreen
import com.daristov.checkpoint.ui.screens.MainScreen
import com.daristov.checkpoint.ui.screens.SettingsScreen

@Composable
fun App() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "map") {
        composable("map") { MainScreen(navController) }
        composable("alarm") { AlarmCameraScreen(navController) }
        composable("checkpoints") { CheckpointListScreen(navController) }
        composable("settings") { SettingsScreen(navController) }
        composable("about") { AboutScreen(navController) }
    }
}
