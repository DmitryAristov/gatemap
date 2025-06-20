package com.daristov.checkpoint.screens.mapscreen

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.daristov.checkpoint.R
import com.daristov.checkpoint.screens.settings.AppThemeMode
import com.daristov.checkpoint.screens.settings.SettingsViewModel
import com.daristov.checkpoint.service.LocationRepository
import com.daristov.checkpoint.util.MapScreenUtils.createBoundingBoxAround
import com.daristov.checkpoint.util.MapScreenUtils.getMapLibreMapStyleURL
import com.daristov.checkpoint.util.MapScreenUtils.handleInitialZoomDone
import com.daristov.checkpoint.util.MapScreenUtils.initCustomsLayer
import com.daristov.checkpoint.util.MapScreenUtils.loadLocationComponent
import com.daristov.checkpoint.util.MapScreenUtils.set3DMode
import com.daristov.checkpoint.util.MapScreenUtils.setupInteractionListeners
import com.daristov.checkpoint.util.MapScreenUtils.setupTrackingButton
import com.daristov.checkpoint.util.MapScreenUtils.showVisibleCustoms
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.maps.Style.OnStyleLoaded

const val DEFAULT_MAP_ANIMATION_DURATION = 1000
const val CUSTOMS_AREA_RADIUS = 2000
const val DEFAULT_TILT = 50.0
const val DEFAULT_ZOOM = 17.0
const val FIRST_CUSTOMS_LOAD_BOUNDINGBOX_SIZE = 100.0

enum class MapInitStep() {
    LOADING_MAP,
    LOADING_LOCATION,
    LOADING_CUSTOMS,
    DONE;

    @StringRes
    fun labelRes(): Int = when (this) {
        LOADING_MAP -> R.string.loading_map
        LOADING_LOCATION -> R.string.fetching_location
        LOADING_CUSTOMS -> R.string.searching_checkpoints
        DONE -> R.string.empty
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(onBack: () -> Unit,
              onOpenAlarm: () -> Unit,
              onOpenChat: (String) -> Unit,
              onOpenMenu: () -> Unit,
              viewModel: MapViewModel = viewModel(),
              settingsViewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    MapLibre.getInstance(context)

    val theme by settingsViewModel.themeMode.collectAsState()
    val is3DEnabled by settingsViewModel.is3DEnabled.collectAsState()
    val location by LocationRepository.locationFlow.collectAsState()
    val customs by viewModel.customs.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val isInitialZoomDone by viewModel.isInitialZoomDone.collectAsState()
    val trackingMode by settingsViewModel.trackingMode.collectAsState()

    val mapView = rememberConfiguredMapView(viewModel, theme, is3DEnabled)

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.map_screen),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    IconButton(
                        onClick = onOpenMenu
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.menu))
                    }
                }
            )
        }
    ) { padding ->
        MapContainer(mapView, onOpenAlarm, onOpenChat, viewModel, settingsViewModel, padding)
    }

    BackHandler { onBack() }

    LaunchedEffect(customs) {
        if (customs.isNotEmpty()) {
            mapView.getMapAsync { map ->
                map.showVisibleCustoms(viewModel)
            }
        }
    }

    LaunchedEffect(location) {
        location?.let {
            if (currentStep != MapInitStep.LOADING_LOCATION) return@LaunchedEffect
            mapView.getMapAsync { map ->
                val userLatLng = LatLng(it)
                map.loadLocationComponent(context, trackingMode)
                map.cameraPosition = CameraPosition.Builder()
                    .tilt(DEFAULT_TILT)
                    .zoom(DEFAULT_ZOOM)
                    .target(userLatLng)
                    .build()
                viewModel.loadCustomsInVisibleArea(
                    userLatLng.createBoundingBoxAround(FIRST_CUSTOMS_LOAD_BOUNDINGBOX_SIZE),
                    MIN_ZOOM_FOR_TILES_LOAD
                )
                viewModel.setCurrentStep(MapInitStep.LOADING_CUSTOMS)
            }
        }
    }

    LaunchedEffect(location, customs) {
        location?.let {
            if (currentStep != MapInitStep.LOADING_CUSTOMS || customs.isEmpty() || isInitialZoomDone)
                return@LaunchedEffect
            mapView.handleInitialZoomDone(it, viewModel)
            viewModel.setCurrentStep(MapInitStep.DONE)
            viewModel.setInitialZoomDone(true)
        }
    }
}

@Composable
fun MapContainer(mapView: MapView,
                 onOpenAlarm: () -> Unit,
                 onOpenChat: (String) -> Unit,
                 viewModel: MapViewModel,
                 settingsViewModel: SettingsViewModel,
                 padding: PaddingValues) {
    val inCustomArea by viewModel.insideCustomArea.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val isInitialZoomDone by viewModel.isInitialZoomDone.collectAsState()
    val location by LocationRepository.locationFlow.collectAsState()
    val selectedCustomId by viewModel.selectedCustomId.collectAsState()
    val isSurveyVisible by viewModel.isSurveyVisible.collectAsState()
    val trackingMode by settingsViewModel.trackingMode.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        if (!isInitialZoomDone)
            MapLoadingIndicator(message = stringResource(currentStep.labelRes()))

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            if (selectedCustomId != null) {
                CustomInfoCard(
                    viewModel = viewModel,
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(12.dp),
                    customId = selectedCustomId!!
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                inCustomArea?.let {
                    IconButton(
                        onClick = {
                            onOpenChat(it.id)
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(16.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Chat,
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = stringResource(R.string.chat),
                        )
                    }
                }

                IconButton(
                    onClick = {
                        location?.let { mapView.setupTrackingButton(it, trackingMode) }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.MyLocation,
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = stringResource(R.string.my_location),
                    )
                }

                FloatingActionButton(
                    onClick = onOpenAlarm
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        tint = MaterialTheme.colorScheme.onSurface,
                        contentDescription = stringResource(R.string.motion_sensor)
                    )
                }
            }
        }

        inCustomArea?.let {
            if (isSurveyVisible)
                SurveyPanel(
                    viewModel = viewModel,
                    inCustomArea = it,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                )
        }
    }
}

@Composable
fun rememberConfiguredMapView(
    viewModel: MapViewModel,
    theme: AppThemeMode,
    is3DEnabled: Boolean
): MapView {
    val context = LocalContext.current
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val styleUrl = remember(theme) {
        return@remember context.getMapLibreMapStyleURL(theme, isSystemInDarkTheme)
    }

    val onSurfaceColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val mapView = remember {
        MapView(context).apply {
            id = R.id.map
            getMapAsync { map ->
                map.setStyle(Style.Builder().fromUri(styleUrl),
                    object : OnStyleLoaded {
                        override fun onStyleLoaded(style: Style) {
                            style.initCustomsLayer(context, theme, onSurfaceColor)
                            viewModel.setCurrentStep(MapInitStep.LOADING_LOCATION)
                        }
                    })
                map.cameraPosition = CameraPosition.Builder()
                    .zoom(DEFAULT_ZOOM)
                    .build()

                map.set3DMode(is3DEnabled)
                map.setupInteractionListeners(viewModel)
            }
        }
    }

    return mapView
}

@Composable
fun SurveyPanel(
    viewModel: MapViewModel,
    inCustomArea: CustomMapObject,
    modifier: Modifier = Modifier
) {
    var sliderValue by remember { mutableFloatStateOf(50f) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(lastInteractionTime) {
        delay(SURVEY_DISMISSAL_TIMEOUT)
        val now = System.currentTimeMillis()
        if (now - lastInteractionTime >= SURVEY_DISMISSAL_TIMEOUT) {
            viewModel.dismissSurveyTemporarily()
        }
    }

    Box(
        modifier = modifier
            .padding(16.dp)
            .background(
                MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = " ${stringResource(R.string.queue_question)} ${inCustomArea.name}?",
                style = MaterialTheme.typography.titleMedium
            )

            Slider(
                value = sliderValue,
                onValueChange = {
                    sliderValue = it
                    lastInteractionTime = System.currentTimeMillis()
                                },
                valueRange = 0f..200f,
                steps = 200,
                modifier = Modifier.fillMaxWidth()
            )

            Row(modifier = Modifier
                .fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        viewModel.sendQueueSize(sliderValue.toInt())
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .weight(0.5f)
                        .padding(horizontal = 16.dp),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Text(
                        text = "${sliderValue.toInt()} ${stringResource(R.string.cars_count)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                OutlinedButton(
                    onClick = {
                        viewModel.sendWaitTimeMinutes(sliderValue.toInt())
                              },
                    modifier = Modifier
                        .height(48.dp)
                        .weight(0.5f)
                        .padding(horizontal = 16.dp),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Text(
                        text = "${sliderValue.toInt() / 10}  ${stringResource(R.string.hours_count)}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun MapLoadingIndicator(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .wrapContentHeight(align = Alignment.Top)
            .zIndex(1f),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier
                .padding(end = 12.dp)
                .offset(y = 1.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun CustomInfoCard(
    viewModel: MapViewModel,
    modifier: Modifier,
    customId: String
) {
    val custom = viewModel.getCustomMapObject(customId)
    if (custom == null) return

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = custom.name,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${stringResource(R.string.queue)}: ${custom.queueSize} ${stringResource(R.string.cars_count)}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "${stringResource(R.string.wait_time)}: ${custom.waitTimeHours} ${stringResource(R.string.hours_count)}",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (custom.queueSize / 100f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    viewModel.openChatScreen(custom.id)
                },
                modifier = Modifier.align(Alignment.End),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Text(
                    text = stringResource(R.string.view_chat),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}
