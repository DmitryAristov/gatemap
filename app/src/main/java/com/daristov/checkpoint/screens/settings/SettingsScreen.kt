package com.daristov.checkpoint.screens.settings

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

const val iconToTextRatio = 2.5f

@Composable
fun SettingsScreen(navController: NavHostController, viewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp) ) {

        val iconSizeDp = getIconSizeDp()

        Spacer(Modifier.height(30.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.navigate("map") },
                modifier = Modifier.size(iconSizeDp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
            }
            Text("Назад", style = MaterialTheme.typography.titleLarge)
        }
        Spacer(Modifier.height(15.dp))

        HorizontalDivider(Modifier.padding(vertical = 3.dp))
        Spacer(Modifier.height(8.dp))
        ThemeSelector(viewModel)

        HorizontalDivider(Modifier.padding(vertical = 3.dp))
        RiseSensitivitySlider(viewModel)

        HorizontalDivider(Modifier.padding(vertical = 3.dp))
        ShrinkSensitivitySlider(viewModel)

        HorizontalDivider(Modifier.padding(vertical = 3.dp))
        StableTrajectorySensitivitySlider(viewModel)

        HorizontalDivider(Modifier.padding(vertical = 3.dp))
        RingtonePicker(
            context = context,
            selectedAlarmUri = viewModel.selectedAlarmUri,
            onPicked = { viewModel.changeAlarmUri(it) }
        )

        HorizontalDivider(Modifier.padding(vertical = 3.dp))
        LanguageDropdown(viewModel)
        HorizontalDivider(Modifier.padding(vertical = 3.dp))
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp),
        verticalArrangement = Arrangement.Bottom) {
        HorizontalDivider(Modifier.padding(vertical = 3.dp))
        AboutItem(navController)
        HorizontalDivider(Modifier.padding(vertical = 3.dp))
    }
}

@Composable
fun AboutItem(navController: NavHostController) {
    SettingItem(
        icon = Icons.Default.Info,
        "О приложении"
    ) {
        navController.navigate("about")
    }
}

@Composable
fun ThemeSelector(viewModel: SettingsViewModel) {
    val current by viewModel.themeMode.collectAsState()

    Text(
        "Тема приложения",
        style = MaterialTheme.typography.titleMedium
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = current == AppThemeMode.LIGHT, onClick = { viewModel.changeTheme(AppThemeMode.LIGHT) })
        Text("Светлая", modifier = Modifier.padding(end = 8.dp))

        RadioButton(selected = current == AppThemeMode.DARK, onClick = { viewModel.changeTheme(AppThemeMode.DARK) })
        Text("Тёмная", modifier = Modifier.padding(end = 8.dp))

        RadioButton(selected = current == AppThemeMode.SYSTEM, onClick = { viewModel.changeTheme(AppThemeMode.SYSTEM) })
        Text("Система", modifier = Modifier.padding(end = 8.dp))
    }
}

@Composable
fun RingtonePicker(context: Context, selectedAlarmUri: Uri?, onPicked: (Uri) -> Unit) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (uri != null) onPicked(uri)
    }

    SettingItem(
        icon = Icons.Default.Notifications,
        "Мелодия будильника"
    ) {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Выберите мелодию")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedAlarmUri)
        }
        launcher.launch(intent)
    }
}

@Composable
fun RiseSensitivitySlider(viewModel: SettingsViewModel) {
    val sensitivity by viewModel.riseSensitivity.collectAsState()

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp)) {

        Text("Чувствительность движения вверх", style = MaterialTheme.typography.titleMedium)

        Slider(
            value = sensitivity.toFloat(),
            onValueChange = { viewModel.changeRiseSensitivity(it.toInt()) },
            valueRange = 0f..100f
        )

        val label = when {
            sensitivity < 35 -> "Низкая"
            sensitivity < 70 -> "Средняя"
            else -> "Высокая"
        }

        Text("Текущий уровень: $sensitivity ($label)")
    }
}

@Composable
fun ShrinkSensitivitySlider(viewModel: SettingsViewModel) {
    val sensitivity by viewModel.shrinkSensitivity.collectAsState()

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp)) {

        Text("Чувствительность уменьшения фонарей", style = MaterialTheme.typography.titleMedium)

        Slider(
            value = sensitivity.toFloat(),
            onValueChange = { viewModel.changeShrinkSensitivity(it.toInt()) },
            valueRange = 0f..100f
        )

        val label = when {
            sensitivity < 35 -> "Низкая"
            sensitivity < 70 -> "Средняя"
            else -> "Высокая"
        }

        Text("Текущий уровень: $sensitivity ($label)")
    }
}

@Composable
fun StableTrajectorySensitivitySlider(viewModel: SettingsViewModel) {
    val sensitivity by viewModel.stableTrajectorySensitivity.collectAsState()

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp)) {

        Text("Чувствительность стабильности траектории", style = MaterialTheme.typography.titleMedium)

        Slider(
            value = sensitivity.toFloat(),
            onValueChange = { viewModel.changeStableTrajectoryRatio(it.toInt()) },
            valueRange = 0f..100f
        )

        val label = when {
            sensitivity < 35 -> "Низкая"
            sensitivity < 70 -> "Средняя"
            else -> "Высокая"
        }

        Text("Текущий уровень: $sensitivity ($label)")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageDropdown(viewModel: SettingsViewModel) {
    val currentLang by viewModel.language.collectAsState()
    val options = listOf(AppLanguage.RU, AppLanguage.KZ, AppLanguage.EN)
    val labels = mapOf(
        AppLanguage.RU to "Русский",
        AppLanguage.KZ to "Қазақша",
        AppLanguage.EN to "English"
    )

    var expanded by remember { mutableStateOf(false) }
    val iconSizeDp = getIconSizeDp()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Language,
            contentDescription = null,
            modifier = Modifier
                .size(iconSizeDp)
                .padding(end = 16.dp)
        )

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded },
            modifier = Modifier.weight(1f)
        ) {
            TextField(
                value = labels[currentLang] ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Язык интерфейса") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                options.forEach { lang ->
                    DropdownMenuItem(
                        text = { Text(labels[lang] ?: "") },
                        onClick = {
                            viewModel.changeLanguage(lang)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
) {
    val iconSizeDp = getIconSizeDp()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier
                .size(iconSizeDp)
                .padding(end = 16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f) // ← это вытягивает текст на всё оставшееся место
        )
    }
}

@Composable
private fun getIconSizeDp(): Dp {
    val density = LocalDensity.current
    val textStyle = MaterialTheme.typography.bodyLarge
    val iconSizeDp = with(density) {
        (textStyle.fontSize * iconToTextRatio).toDp()
    }
    return iconSizeDp
}
