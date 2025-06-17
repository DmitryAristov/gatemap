package com.daristov.checkpoint.screens.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavHostController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "О приложении",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate("settings")
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = "Назад"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding) // ✅ Учитываем отступы от Scaffold
                .verticalScroll(rememberScrollState())
                .padding(16.dp),  // ✅ Внутренний отступ контента
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SectionCard(
                title = "📬 Обратная связь",
                description = "Отправьте сообщение, идею или баг с логами.",
                onClick = {
                    navController.navigate("feedback")
                }
            )

            SectionCard(
                title = "💡 Предложения разработчика",
                description = "Посмотрите, над чем мы работаем или что планируем.",
                onClick = { navController.navigate("suggestions") }
            )

            SectionCard(
                title = "📹 Инструкция",
                description = "Пошаговая визуальная инструкция по использованию приложения.",
                onClick = { navController.navigate("instruction") }
            )

            Spacer(modifier = Modifier.weight(1f))

            DeveloperInfo()
        }
    }
}

@Composable
fun SectionCard(
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun DeveloperInfo() {
    val versionName = "1.0.0"
    val buildDate = "2025-06-14"
    val author = "Аристов Дмитрий"
    val license = "MIT License"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("Автор: $author", style = MaterialTheme.typography.bodyMedium)
        Text("Версия: $versionName", style = MaterialTheme.typography.bodyMedium)
        Text("Сборка: $buildDate", style = MaterialTheme.typography.bodyMedium)
        Text("Лицензия: $license", style = MaterialTheme.typography.bodyMedium)
    }
}

