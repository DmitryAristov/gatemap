package com.daristov.checkpoint

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.daristov.checkpoint.screens.AboutScreen
import com.daristov.checkpoint.screens.alarm.AlarmCameraScreen
import com.daristov.checkpoint.screens.CheckpointListScreen
import com.daristov.checkpoint.screens.mapscreen.MapScreen
import com.daristov.checkpoint.screens.PermissionsScreen
import com.daristov.checkpoint.screens.settings.SettingsScreen
import org.opencv.android.OpenCVLoader
import org.osmdroid.config.Configuration

@Composable
fun App() {
    if (!OpenCVLoader.initDebug())
        Log.e("OpenCV", "Unable to load OpenCV!");
    else
        Log.d("OpenCV", "OpenCV loaded Successfully!");

    val context = LocalContext.current
    val prefs = context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
    Configuration.getInstance().load(context, prefs)

    var permissionsGranted by remember { mutableStateOf(false) }

    if (!permissionsGranted) {
        PermissionsScreen(onPermissionsGranted = {
            permissionsGranted = true
        })
    } else {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "map") {
            composable("map") { MapScreen(navController) }
            composable("alarm") { AlarmCameraScreen(navController) }
            composable("checkpoints") { CheckpointListScreen(navController) }
            composable("settings") { SettingsScreen(navController) }
            composable("about") { AboutScreen(navController) }
        }
    }
}
