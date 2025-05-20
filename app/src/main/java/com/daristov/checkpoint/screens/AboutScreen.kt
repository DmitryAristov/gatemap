package com.daristov.checkpoint.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavHostController) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("О приложении") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                }
            })
    }) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Text("Версия: 1.0.0", modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("Автор: ...", modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
