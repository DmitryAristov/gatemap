package com.daristov.checkpoint

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.daristov.checkpoint.ui.theme.AppTheme
import com.daristov.checkpoint.screens.settings.SettingsViewModel
import com.daristov.checkpoint.service.LocationService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val settingsViewModel: SettingsViewModel = viewModel()
            val theme by settingsViewModel.themeMode.collectAsState()

            AppTheme(themeMode = theme) {
                Surface {
                    App()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val intent = Intent(this, LocationService::class.java)
        stopService(intent)
    }
}