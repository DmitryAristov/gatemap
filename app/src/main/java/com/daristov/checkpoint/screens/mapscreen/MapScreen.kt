package com.daristov.checkpoint.screens.mapscreen

import android.app.Activity
import android.location.Location
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.daristov.checkpoint.R
import com.daristov.checkpoint.screens.mapscreen.viewmodel.MIN_ZOOM_FOR_TILES_LOAD
import com.daristov.checkpoint.screens.mapscreen.viewmodel.MapViewModel
import com.daristov.checkpoint.screens.settings.AppThemeMode
import com.daristov.checkpoint.screens.settings.SettingsViewModel
import com.daristov.checkpoint.service.LocationRepository
import com.daristov.checkpoint.util.MapScreenUtils.createBoundingBoxAround
import com.daristov.checkpoint.util.MapScreenUtils.getMapLibreMapStyleURL
import com.daristov.checkpoint.util.MapScreenUtils.handleInitialZoomAndSurvey
import com.daristov.checkpoint.util.MapScreenUtils.loadLocationComponent
import com.daristov.checkpoint.util.MapScreenUtils.setupInteractionListeners
import com.daristov.checkpoint.util.MapScreenUtils.setupTrackingButton
import com.daristov.checkpoint.util.MapScreenUtils.showVisibleCustoms
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavHostController,
              viewModel: MapViewModel = viewModel(),
              settingsViewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    MapLibre.getInstance(context)

    val isStyleLoaded = remember { mutableStateOf(false) }
    val isLocationComponentLoaded = remember { mutableStateOf(false) }
    val initialCustomsLoadRequested = remember { mutableStateOf(false) }
    val isInitialZoomDone = remember { mutableStateOf(false) }
    val needToShowSurvey = remember { mutableStateOf(false) }
    val visibleMarkerIds = remember { mutableSetOf<String>() }
    val existingMarkers = remember { mutableStateMapOf<String, Marker>() }

    val theme by settingsViewModel.themeMode.collectAsState()
    val location by LocationRepository.locationFlow.collectAsState()
    val customs by viewModel.customs.collectAsState()

    val mapView = rememberConfiguredMapView(theme, isStyleLoaded, visibleMarkerIds, existingMarkers)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Карта КПП", color = MaterialTheme.colorScheme.onBackground)
                        },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(imageVector = Icons.Default.Settings,
                            tint = MaterialTheme.colorScheme.onSurface,
                            contentDescription = "Настройки"
                        )
                    }
                }
            )
        }
    ) { padding ->
        MapContainer(mapView, navController, viewModel, padding, needToShowSurvey, location)
    }

    val activity = context as? Activity
    BackHandler { activity?.finish() }

    LaunchedEffect(customs) {
        if (customs.isNotEmpty()) {
            mapView.getMapAsync { map ->
                map.showVisibleCustoms(customs, context, theme, visibleMarkerIds, existingMarkers)
            }
        }
    }

    LaunchedEffect(location) {
        if (!isStyleLoaded.value || (isLocationComponentLoaded.value && initialCustomsLoadRequested.value)) return@LaunchedEffect
        location?.let {
            mapView.getMapAsync { map ->
                if (!initialCustomsLoadRequested.value) {
                    val userLatLng = LatLng(it)
                    viewModel.loadCustomsInVisibleArea(
                        userLatLng.createBoundingBoxAround(FIRST_CUSTOMS_LOAD_BOUNDINGBOX_SIZE), MIN_ZOOM_FOR_TILES_LOAD)
                    initialCustomsLoadRequested.value = true
                }

                if (!isLocationComponentLoaded.value) {
                    map.loadLocationComponent(context)
                    isLocationComponentLoaded.value = true
                }
            }
        }
    }

    LaunchedEffect(location, customs) {
        if (isInitialZoomDone.value || customs.isEmpty()) return@LaunchedEffect
        location?.let {
            mapView.handleInitialZoomAndSurvey(it, needToShowSurvey, viewModel)
            isInitialZoomDone.value = true
        }
    }
}

@Composable
fun MapContainer(mapView: MapView,
                 navController: NavHostController,
                 viewModel: MapViewModel,
                 padding: PaddingValues,
                 needToShowSurvey: MutableState<Boolean>,
                 location: Location?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = 16.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(
                onClick = {
                    location?.let { mapView.setupTrackingButton(it) }
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    tint = MaterialTheme.colorScheme.onSurface,
                    contentDescription = "Мое местоположение",
                )
            }

            FloatingActionButton(
                onClick = { navController.navigate("alarm") }
            ) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    tint = MaterialTheme.colorScheme.onSurface,
                    contentDescription = "Будильник"
                )
            }
        }
        QueueSurveyManager(
            isUserInQueue = needToShowSurvey.value,
            onAnswer = { minutes ->
                viewModel.sendSurveyAnswer(minutes)
            },
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun rememberConfiguredMapView(
    theme: AppThemeMode,
    isStyleLoaded: MutableState<Boolean>,
    visibleMarkerIds: MutableSet<String>,
    existingMarkers: MutableMap<String, Marker>,
    viewModel: MapViewModel = viewModel()
): MapView {
    val context = LocalContext.current
    val isSystemInDarkTheme = isSystemInDarkTheme()
    val styleUrl = remember(theme) {
        return@remember context.getMapLibreMapStyleURL(theme, isSystemInDarkTheme)
    }

    val mapView = remember {
        MapView(context).apply {
            id = R.id.map
            getMapAsync { map ->
                map.setStyle(Style.Builder().fromUri(styleUrl),
                    object : OnStyleLoaded {
                        override fun onStyleLoaded(style: Style) {
                            isStyleLoaded.value = true
                        }
                    })
                map.cameraPosition = CameraPosition.Builder()
//                    .tilt(DEFAULT_TILT)
                    .zoom(DEFAULT_ZOOM)
                    .build()
                map.setupInteractionListeners(context, viewModel, theme, visibleMarkerIds, existingMarkers)
            }
        }
    }

    return mapView
}

@Composable
fun SurveyPanel(
    onAnswer: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(0, 10, 30, 60, 120)
    LaunchedEffect(Unit) {
        delay(10_000)
        onDismiss()
    }

    Box(
        modifier = modifier
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Подскажите пожалуйста сколько примерно сейчас в очереди машин?",
                color = MaterialTheme.colorScheme.onBackground
            )
            for (i in options.indices) {
                val value = options[i]
                Button(onClick = { onAnswer(value) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (i != options.lastIndex)
                        Text("$value – ${options[i + 1]}",
                            color = MaterialTheme.colorScheme.onBackground)
                    else
                        Text("> $value",
                            color = MaterialTheme.colorScheme.onBackground)
                }
            }
        }
    }
}

@Composable
fun QueueSurveyManager(
    isUserInQueue: Boolean,
    onAnswer: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSurvey by remember { mutableStateOf(false) }
    var lastShownTime by remember { mutableLongStateOf(0L) }
    var answered by remember { mutableStateOf(false) }

    LaunchedEffect(isUserInQueue, answered) {
        while (isUserInQueue && !answered) {
            val now = System.currentTimeMillis()
            if (!showSurvey && now - lastShownTime > 30_000) {
                showSurvey = true
                lastShownTime = now
                delay(10_000)
                showSurvey = false
            } else {
                delay(1_000) // частота проверок
            }
        }
    }

    if (showSurvey && !answered) {
        SurveyPanel(
            modifier = modifier,
            onAnswer = {
                answered = true
                onAnswer(it)
            },
            onDismiss = {
                showSurvey = false
            }
        )
    }
}
