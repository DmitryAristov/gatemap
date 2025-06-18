package com.daristov.checkpoint.screens.menu.items

import android.util.Patterns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    onSend: (String, String?, String, Boolean) -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Обратная связь",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }
    ) { padding ->
        FeedbackContainer(onSend, padding)
    }

    BackHandler { onBack() }
}

@Composable
fun FeedbackContainer(
    onSend: (String, String?, String, Boolean) -> Unit,
    padding: PaddingValues,
) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var sendLogs by remember { mutableStateOf(true) }
    var isLoading by remember { mutableStateOf(false) }

    val isFormValid = message.isNotBlank() && selectedTag != null
    val showLogsCheckbox = selectedTag == "Ошибка"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = message,
            onValueChange = { message = it },
            label = { Text("Ваше сообщение") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            maxLines = 6
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Ошибка", "Идея", "Другое").forEach { tag ->
                FilterChip(
                    selected = selectedTag == tag,
                    onClick = { selectedTag = tag },
                    label = { Text(tag) }
                )
            }
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email (необязательно)") },
            modifier = Modifier.fillMaxWidth(),
            isError = email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()
        )

        if (showLogsCheckbox) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = sendLogs, onCheckedChange = { sendLogs = it })
                Spacer(modifier = Modifier.width(8.dp))
                Text("Отправить анонимные логи")
            }
        }

        Button(
            onClick = {
                isLoading = true
                onSend(message, selectedTag!!, email, sendLogs)
                message = ""
                selectedTag = null
                email = ""
                sendLogs = true
                isLoading = false
                Toast.makeText(context, "Отправлено!", Toast.LENGTH_SHORT).show()
            },
            enabled = isFormValid && !isLoading,
            modifier = Modifier.align(Alignment.End)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text("Отправить")
            }
        }
    }
}
