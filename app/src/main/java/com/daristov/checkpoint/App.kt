package com.daristov.checkpoint

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.daristov.checkpoint.screens.ChatScreen
import com.daristov.checkpoint.screens.menu.MenuScreen
import com.daristov.checkpoint.screens.alarm.AlarmScreen
import com.daristov.checkpoint.screens.mapscreen.MapScreen
import com.daristov.checkpoint.screens.PermissionsScreen
import com.daristov.checkpoint.screens.menu.items.FeedbackScreen
import com.daristov.checkpoint.screens.menu.items.InstructionScreen
import com.daristov.checkpoint.screens.menu.items.SuggestionsScreen
import com.daristov.checkpoint.screens.settings.AppLanguage
import com.daristov.checkpoint.screens.settings.SettingsScreen
import com.daristov.checkpoint.screens.settings.SettingsViewModel
import com.daristov.checkpoint.service.LocationService
import org.opencv.android.OpenCVLoader
import java.util.Locale

@Composable
fun App(viewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    if (!OpenCVLoader.initDebug())
        Log.e("OpenCV", "Unable to load OpenCV!")

    val language by viewModel.language.collectAsState()
    val currentContext = rememberUpdatedState(newValue = context)

    key(language) {
        MainScreen(context)
    }

    LaunchedEffect(language) {
        currentContext.value.updateLocale(language)
    }
}

@Composable
fun MainScreen(context: Context) {
    var permissionsGranted by remember { mutableStateOf(false) }
    if (!permissionsGranted) {
        PermissionsScreen(onPermissionsGranted = {
            permissionsGranted = true
            val intent = Intent(context, LocationService::class.java)
            context.startService(intent)
        })
    } else {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = "map"
        ) {
            mainNavGraph(navController, context as Activity?)
        }
    }
}

fun NavGraphBuilder.mainNavGraph(navController: NavHostController, activity: Activity?) {

    composable("map") {
        MapScreen(
            onBack = { activity?.finish() },
            onOpenAlarm = { navController.navigate("alarm") },
            onOpenChat = { chatId -> navController.navigate("chat/$chatId") },
            onOpenMenu = { navController.navigate("menu") }
        )
    }

    composable("alarm") {
        AlarmScreen(
            onBack = { navController.popBackStack() },
            onOpenMenu = { navController.navigate("menu") }
        )
    }

    composable("chat/{id}") { backStackEntry ->
        val id = backStackEntry.arguments?.getString("id") ?: return@composable
        ChatScreen(
            chatId = id,
            onBack = { navController.popBackStack() },
            onOpenMenu = { navController.navigate("menu") }
        )
    }

    composable("menu") {
        MenuScreen(navController)
    }

    composable("menu/settings") {
        SettingsScreen(
            onBack = { navController.popBackStack() }
        )
    }

    composable("menu/feedback") {
        FeedbackScreen(
            onBack = { navController.popBackStack() },
            onSend = { message, tag, email, sendLogs -> return@FeedbackScreen }
        )
    }

    composable("menu/suggestions") {
        SuggestionsScreen(
            onBack = { navController.popBackStack() }
        )
    }

    composable("menu/instruction") {
        InstructionScreen(
            onBack = { navController.popBackStack() }
        )
    }
}

fun Context.updateLocale(language: AppLanguage) {
    val locale = Locale(language.code)
    Locale.setDefault(locale)

    val config = resources.configuration
    config.setLocale(locale)
    resources.updateConfiguration(config, resources.displayMetrics)
}