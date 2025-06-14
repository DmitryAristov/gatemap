package com.daristov.checkpoint

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.daristov.checkpoint.screens.AboutScreen
import com.daristov.checkpoint.screens.alarm.AlarmScreen
import com.daristov.checkpoint.screens.CustomsListScreen
import com.daristov.checkpoint.screens.mapscreen.MapScreen
import com.daristov.checkpoint.screens.PermissionsScreen
import com.daristov.checkpoint.screens.settings.SettingsScreen
import com.daristov.checkpoint.service.LocationService
import org.opencv.android.OpenCVLoader
import org.osmdroid.config.Configuration

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
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
            startBackgroundServices(context)
        })
    } else {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "map") {
            composable("map") { MapScreen(navController) }
            composable("alarm") { AlarmScreen(navController) }
            composable("customs") { CustomsListScreen(navController) }
            composable("settings") { SettingsScreen(navController) }
            composable("about") { AboutScreen(navController) }
        }
    }
}

fun startBackgroundServices(context: Context) {
    val intent = Intent(context, LocationService::class.java)
    ContextCompat.startForegroundService(context, intent)

}
