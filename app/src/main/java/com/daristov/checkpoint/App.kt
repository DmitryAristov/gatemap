package com.daristov.checkpoint

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.daristov.checkpoint.screens.about.AboutScreen
import com.daristov.checkpoint.screens.alarm.AlarmScreen
import com.daristov.checkpoint.screens.CustomsListScreen
import com.daristov.checkpoint.screens.mapscreen.MapScreen
import com.daristov.checkpoint.screens.PermissionsScreen
import com.daristov.checkpoint.screens.about.items.FeedbackScreen
import com.daristov.checkpoint.screens.about.items.InstructionScreen
import com.daristov.checkpoint.screens.about.items.SuggestionsScreen
import com.daristov.checkpoint.screens.settings.SettingsScreen
import org.opencv.android.OpenCVLoader

@Composable
fun App() {
    if (!OpenCVLoader.initDebug())
        Log.e("OpenCV", "Unable to load OpenCV!")
    else
        Log.d("OpenCV", "OpenCV loaded Successfully!")

    var permissionsGranted by remember { mutableStateOf(false) }

    if (!permissionsGranted) {
        PermissionsScreen(onPermissionsGranted = {
            permissionsGranted = true
        })
    } else {
        val navController = rememberNavController()
        val saveableStateHolder = rememberSaveableStateHolder()
        NavHost(navController = navController, startDestination = "map") {
            composable("map") {
                saveableStateHolder.SaveableStateProvider("map") {
                    MapScreen(navController)
                }
            }
            composable("alarm") { AlarmScreen(navController) }
            composable("customs") { CustomsListScreen(navController) }
            composable("settings") { SettingsScreen(navController) }
            composable("about") { AboutScreen(navController) }
            composable("feedback") { FeedbackScreen(navController, onSend = { message, tag, email, sendLogs -> return@FeedbackScreen }) }
            composable("suggestions") { SuggestionsScreen(navController) }
            composable("instruction") { InstructionScreen(navController) }
        }
    }
}
