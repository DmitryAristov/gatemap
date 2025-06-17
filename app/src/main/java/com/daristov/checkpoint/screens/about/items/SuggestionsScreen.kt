package com.daristov.checkpoint.screens.about.items

import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggestionsScreen(navController: NavHostController) {
    val suggestions = listOf(
        Suggestion(
            title = "Фильтр по загруженности КПП",
            description = "Позволяет пользователям видеть только те КПП, где сейчас короткая очередь.",
            category = SuggestionCategory.Improvement
        ),
        Suggestion(
            title = "Интеграция с Telegram",
            description = "Позволит получать уведомления о движении машин в телеграм-боте.",
            category = SuggestionCategory.Idea
        ),
        Suggestion(
            title = "Анонимность данных",
            description = "Все геоданные обезличиваются и не привязываются к личности.",
            category = SuggestionCategory.Info
        )
    )

    Scaffold(topBar = {
        TopAppBar(
            title = {
                Text(
                    text = "Предложения разработчика",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.navigate("about") }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = "Назад"
                    )
                }
            }
        )
    }) { padding ->
        LazyColumn(
            contentPadding = padding,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            items(suggestions) { suggestion ->
                SuggestionCard(suggestion)
            }
        }
    }
}

@Composable
fun SuggestionCard(suggestion: Suggestion) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color = suggestion.category.color, shape = CircleShape)
            )
            Column {
                Text(
                    text = suggestion.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = suggestion.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class Suggestion(
    val title: String,
    val description: String,
    val category: SuggestionCategory
)

enum class SuggestionCategory(val color: Color) {
    Improvement(Color(0xFF4CAF50)), // Зеленый
    Idea(Color(0xFF2196F3)),       // Синий
    Info(Color(0xFFFFC107))        // Оранжевый
}
