package com.daristov.checkpoint.screens.settings

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Tonality
import androidx.compose.material.icons.filled._3dRotation
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.unit.dp
import com.daristov.checkpoint.R
import com.daristov.checkpoint.updateLocale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = viewModel()
) {
    val isFirstLaunch by viewModel.isFirstLaunch.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            )
        }
    ) { padding ->
        SettingsContainer(viewModel, padding)
    }

    BackHandler { onBack() }

    //TODO move to MainActivity
    LaunchedEffect(isFirstLaunch) {
        if (!isFirstLaunch) return@LaunchedEffect
        viewModel.initDefaults()
        viewModel.changeFirstLaunch(false)
    }
}

@Composable
fun SettingsContainer(
    viewModel: SettingsViewModel,
    padding: PaddingValues
) {
    val context = LocalContext.current
    val theme by viewModel.themeMode.collectAsState()
    val language by viewModel.language.collectAsState()
    val autoDayNightDetect by viewModel.autoDayNightDetect.collectAsState()
    val is3DEnabled by viewModel.is3DEnabled.collectAsState()
    val stableSensitivity by viewModel.stableTrajectorySensitivity.collectAsState()
    val verticalSensitivity by viewModel.verticalMovementSensitivity.collectAsState()
    val horizontalSensitivity by viewModel.horizontalCompressionSensitivity.collectAsState()
    val selectedAlarmUri = viewModel.selectedAlarmUri
    val allowStats by viewModel.allowStats.collectAsState()

    Column(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = stringResource(R.string.common_settings),
            style = MaterialTheme.typography.titleLarge
        )

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {

            val themeLabels = AppThemeMode.entries.associateWith { stringResource(it.labelRes()) }
            SettingRowDropdown(
                icon = Icons.Default.DarkMode,
                title = stringResource(R.string.app_theme),
                value = stringResource(theme.labelRes()),
                options = AppThemeMode.entries.map { stringResource(it.labelRes()) },
                onSelect = { label ->
                    themeLabels.entries.firstOrNull { it.value == label }?.key?.let {
                        viewModel.changeTheme(it)
                    }
                }
            )

            SettingRowDropdown(
                icon = Icons.Default.Language,
                title = stringResource(R.string.interface_language),
                value = language.label,
                options = AppLanguage.entries.map { it.label },
                onSelect = { label ->
                    AppLanguage.entries.firstOrNull { it.label == label }
                        ?.let {
                            viewModel.changeLanguage(it)
                            context.updateLocale(language)
                        }
                }
            )

            SettingRowSwitch(
                icon = Icons.Default.BarChart,
                title = stringResource(R.string.anonymous_stats),
                checked = allowStats,
                onCheckedChange = { viewModel.changeAllowStats(it) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.alarm_settings),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { viewModel.resetAlarmSettingsToDefault() }) {
                Text(stringResource(R.string.reset))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AlarmRingtoneSetting(
                selectedAlarmUri = selectedAlarmUri,
                onPicked = { viewModel.changeAlarmUri(it) }
            )

            SettingRowSlider(
                icon = Icons.Default.Sensors,
                title = stringResource(R.string.motion_sensitivity),
                value = stableSensitivity.toFloat(),
                onValueChange = { viewModel.changeStableTrajectoryRatio(it.toInt()) }
            )

            SettingRowSlider(
                icon = Icons.Default.Sensors,
                title = stringResource(R.string.vertical_sensitivity),
                value = verticalSensitivity.toFloat(),
                onValueChange = { viewModel.changeVerticalMovementSensitivity(it.toInt()) }
            )

            SettingRowSlider(
                icon = Icons.Default.Sensors,
                title = stringResource(R.string.horizontal_sensitivity),
                value = horizontalSensitivity.toFloat(),
                onValueChange = { viewModel.changeHorizontalCompressionSensitivity(it.toInt()) }
            )

            SettingRowSwitch(
                icon = Icons.Default.Tonality,
                title = stringResource(R.string.auto_day_night),
                checked = autoDayNightDetect,
                onCheckedChange = { viewModel.changeAutoDayNightDetect(it) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.map_settings),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = { viewModel.resetMapSettingsToDefault() }) {
                Text(stringResource(R.string.reset))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingRowSwitch(
                icon = Icons.Default._3dRotation,
                title = stringResource(R.string.map_3d_mode),
                checked = is3DEnabled,
                onCheckedChange = { viewModel.change3DEnabled(it) }
            )
        }
    }

}

@Composable
fun SettingRowDropdown(
    icon: ImageVector,
    title: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val dropdownWidth = remember { mutableIntStateOf(0) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .onGloballyPositioned { coordinates ->
                dropdownWidth.intValue = coordinates.size.width
            }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(text = value)
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(with(LocalDensity.current) {
                    dropdownWidth.intValue.toDp() })
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                )

                if (index != options.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingRowSwitch(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = checked,
                onCheckedChange = null
            )
        }
    }
}

@Composable
fun SettingRowSlider(
    icon: ImageVector,
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..100f,
    steps: Int = 0,
    valueSuffix: String? = null,
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.fillMaxWidth()
            )

            val valueText = "${value.toInt()}${valueSuffix ?: "%"}"
            val description = when {
                value < 35 -> stringResource(R.string.low)
                value < 70 -> stringResource(R.string.medium)
                else -> stringResource(R.string.high)
            }

            Text(
                text = "$valueText  ($description)",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun AlarmRingtoneSetting(
    selectedAlarmUri: Uri?,
    onPicked: (Uri) -> Unit
) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        if (uri != null) onPicked(uri)
    }

    val notSelected = stringResource(R.string.alarm_not_selected)
    val ringtoneTitle = remember(selectedAlarmUri) {
        RingtoneManager.getRingtone(context, selectedAlarmUri)?.getTitle(context) ?: notSelected
    }

    val chooseRingtone = stringResource(R.string.choose_ringtone)
    SettingRowNavigation(
        icon = Icons.Default.Notifications,
        title = stringResource(R.string.alarm_sound),
        value = ringtoneTitle,
        onClick = {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, chooseRingtone)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, selectedAlarmUri)
            }
            launcher.launch(intent)
        }
    )
}

@Composable
fun SettingRowNavigation(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(end = 16.dp)
            )
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            if (value.isNotEmpty()) {
                Text(
                    text = value,
                    modifier = Modifier
                        .padding(start = 8.dp, end = 8.dp)
                        .widthIn(max = 80.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null
            )
        }
    }
}

