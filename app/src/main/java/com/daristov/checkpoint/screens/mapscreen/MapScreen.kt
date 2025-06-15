package com.daristov.checkpoint.screens.mapscreen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
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
import com.daristov.checkpoint.screens.mapscreen.viewmodel.MapViewModel
import com.daristov.checkpoint.screens.settings.AppThemeMode
import com.daristov.checkpoint.screens.settings.SettingsViewModel
import com.daristov.checkpoint.service.CustomLocationEngine
import com.daristov.checkpoint.service.LocationRepository
import com.daristov.checkpoint.util.MapScreenUtils.createBoundingBoxAround
import com.daristov.checkpoint.util.MapScreenUtils.handleInitialZoomAndSurvey
import com.daristov.checkpoint.util.MapScreenUtils.setupInteractionListeners
import com.daristov.checkpoint.util.MapScreenUtils.showVisibleCustoms
import kotlinx.coroutines.delay
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.LocationComponentOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMap.CancelableCallback
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.maps.Style.OnStyleLoaded

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavHostController,
              viewModel: MapViewModel = viewModel(),
              settingsViewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    MapLibre.getInstance(context)
    val theme by settingsViewModel.themeMode.collectAsState()
    val mapView = rememberConfiguredMapView(theme)

    val isAnimatedToInitialLocation = remember { mutableStateOf(false) }
    val isNearestCustomFound = remember { mutableStateOf(false) }
    val isInitialZoomDone = remember { mutableStateOf(false) }
    val needToShowSurvey = remember { mutableStateOf(false) }
    val visibleMarkerIds = remember { mutableSetOf<String>() }
    val existingMarkers = remember { mutableStateMapOf<String, Marker>() }

    val location by LocationRepository.locationFlow.collectAsState()
    val nearestCustom by viewModel.nearestCustom.collectAsState()
    val distance by viewModel.distanceToNearestCustom.collectAsState()
    val customs by viewModel.customs.collectAsState()

    mapView.setupInteractionListeners(context, viewModel, theme, visibleMarkerIds, existingMarkers)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Очереди на КПП", color = MaterialTheme.colorScheme.onBackground)
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
        MapContainer(mapView = mapView,
            navController = navController,
            viewModel = viewModel,
            padding = padding,
            needToShowSurvey = needToShowSurvey)
    }

    val activity = context as? Activity
    BackHandler {
        activity?.moveTaskToBack(true) // Сворачивает приложение
        // или: activity?.finish() // Закрывает приложение
    }

    LaunchedEffect(Unit) {
        mapView.getMapAsync { map ->
            val bounds = map.projection.visibleRegion.latLngBounds
            val zoom = map.cameraPosition.zoom
            viewModel.loadCustomsInVisibleArea(bounds, zoom)
        }
    }

    LaunchedEffect(customs) {
        if (customs.isNotEmpty()) {
            mapView.showVisibleCustoms(
                allCustoms = customs,
                context = context,
                theme = theme,
                visibleMarkerIds = visibleMarkerIds,
                existingMarkers = existingMarkers
            )
        }
    }

    LaunchedEffect(location) {
        location?.let {
            mapView.getMapAsync { map ->
                if (!isAnimatedToInitialLocation.value) {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    map.animateCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .tilt(50.0)
                                .zoom(17.0)
                                .target(LatLng(it))
                                .build()
                        ),
                        200
                    )
                    viewModel.loadCustomsInVisibleArea(userLatLng.createBoundingBoxAround(100.0), 14.0)
                    isAnimatedToInitialLocation.value = true
                }
            }
        }
    }

    LaunchedEffect(location, customs) {
        if (isNearestCustomFound.value || customs.isEmpty()) return@LaunchedEffect
        val userPoint = location?.let { LatLng(it) } ?: return@LaunchedEffect
        viewModel.findAndSetNearestCustom(userPoint)
        isNearestCustomFound.value = true
    }

    LaunchedEffect(location, nearestCustom) {
        if (isInitialZoomDone.value || location == null || nearestCustom == null) return@LaunchedEffect
        mapView.handleInitialZoomAndSurvey(location!!, nearestCustom!!, distance, needToShowSurvey, viewModel)
        isInitialZoomDone.value = true
    }
}

@Composable
fun MapContainer(mapView: MapView,
                 navController: NavHostController,
                 viewModel: MapViewModel,
                 padding: PaddingValues,
                 needToShowSurvey: MutableState<Boolean>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        val location by LocationRepository.locationFlow.collectAsState()

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
            IconButton(onClick = {
                        mapView.getMapAsync { map ->
                            map.animateCamera(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.Builder()
                                        .tilt(50.0)
                                        .zoom(17.0)
                                        .target(LatLng(location!!))
                                        .build()
                                ),
                                200,
                                object : CancelableCallback {
                                    override fun onFinish() {
                                        map.locationComponent.cameraMode = CameraMode.TRACKING_COMPASS
                                    }
                                    override fun onCancel() {
                                        return
                                    }
                                }
                            )
                        }
                },
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(16.dp))
            ) {
                Icon(imageVector = Icons.Default.MyLocation,
                    tint = MaterialTheme.colorScheme.onSurface,
                    contentDescription = "Мое местоположение",
                )
            }

            FloatingActionButton(
                onClick = { navController.navigate("alarm") }
            ) {
                Icon(imageVector = Icons.Default.Notifications,
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
fun rememberConfiguredMapView(theme: AppThemeMode): MapView {
    val context = LocalContext.current
    val location by LocationRepository.locationFlow.collectAsState()

    val isSystemInDarkTheme = isSystemInDarkTheme()
    val apiKey = context.getString(R.string.maptiler_api_key)
    val styleUrl = remember(theme) {
        val darkStyleUrl = "https://api.maptiler.com/maps/dataviz-dark/style.json?key=$apiKey"
        val lightStyleUrl = "https://api.maptiler.com/maps/streets/style.json?key=$apiKey"
        when (theme) {
            AppThemeMode.DARK -> darkStyleUrl
            AppThemeMode.LIGHT -> lightStyleUrl
            AppThemeMode.SYSTEM -> {
                if (isSystemInDarkTheme) {
                    return@remember darkStyleUrl
                } else {
                    return@remember lightStyleUrl
                }
            }
        }
    }

    val mapView = remember {
        MapView(context).apply {
            id = R.id.map
            getMapAsync { map ->
                map.cameraPosition = CameraPosition.Builder()
                    .zoom(17.0)
                    .tilt(50.0)
                    .build()
            }
        }
    }

    val isStyleLoaded = remember { mutableStateOf(false) }
    LaunchedEffect(location) {
        if (isStyleLoaded.value) return@LaunchedEffect
        location?.let {
            mapView.getMapAsync { map ->
                map.setStyle(
                    Style.Builder().fromUri(styleUrl),
                    OnStyleLoadedImpl(map, context)
                )
            }
            isStyleLoaded.value = true
        }
    }

    return mapView
}

class OnStyleLoadedImpl(
    private val map: MapLibreMap,
    private val context: Context
) : OnStyleLoaded {
    @SuppressLint("MissingPermission")
    override fun onStyleLoaded(style: Style) {
        map.locationComponent.apply {
            val customLocationEngine = CustomLocationEngine()
            activateLocationComponent(
                LocationComponentActivationOptions.builder(context, style)
                    .useSpecializedLocationLayer(true)
                    .locationEngine(customLocationEngine)
                    .build()
            )
            isLocationComponentEnabled = true
            renderMode = RenderMode.COMPASS
            cameraMode = CameraMode.TRACKING
            val options = LocationComponentOptions.builder(context)
                .pulseEnabled(true)
                .accuracyAnimationEnabled(true)
                .compassAnimationEnabled(true)
                .trackingGesturesManagement(false)
                .pulseMaxRadius(50f)
                .build()
            applyStyle(options)
        }
    }
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
