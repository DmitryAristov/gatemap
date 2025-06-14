package com.daristov.checkpoint.screens.mapscreen

import android.view.ViewTreeObserver
import androidx.compose.foundation.background
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
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.daristov.checkpoint.R
import com.daristov.checkpoint.screens.mapscreen.overlay.CustomLocationOverlay
import com.daristov.checkpoint.screens.mapscreen.overlay.CustomRotationOverlay
import com.daristov.checkpoint.screens.mapscreen.viewmodel.MapViewModel
import com.daristov.checkpoint.screens.settings.SettingsViewModel
import com.daristov.checkpoint.util.MapsScreenUtils.createBoundingBoxAround
import com.daristov.checkpoint.util.MapsScreenUtils.handleInitialZoomAndSurvey
import com.daristov.checkpoint.util.MapsScreenUtils.setupInteractionListeners
import com.daristov.checkpoint.util.MapsScreenUtils.showCustoms
import kotlinx.coroutines.delay
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavHostController,
              viewModel: MapViewModel = viewModel(),
              settingsViewModel: SettingsViewModel = viewModel()) {
    val context = LocalContext.current
    val mapView = rememberConfiguredMapView()

    val isMapInitialized = remember { mutableStateOf(false) }
    val isAnimatedToInitialLocation = remember { mutableStateOf(false) }
    val isNearestCustomFound = remember { mutableStateOf(false) }
    val isInitialZoomDone = remember { mutableStateOf(false) }
    val needToShowSurvey = remember { mutableStateOf(false) }

    val location by viewModel.location.collectAsState()
    val nearestCustom by viewModel.nearestCustom.collectAsState()
    val distance by viewModel.distanceToNearestCustom.collectAsState()
    val isFollowUserLocation by viewModel.isFollowUserLocation.collectAsState()

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
            needToShowSurvey = needToShowSurvey,
            isMapInitialized = isMapInitialized)
    }

    LaunchedEffect(Unit) {
        viewModel.loadCustomsInVisibleArea(mapView.boundingBox, mapView.zoomLevelDouble)
    }

    val customs by viewModel.customs.collectAsState()
    val theme by settingsViewModel.themeMode.collectAsState()
    LaunchedEffect(customs, isMapInitialized) {
        if (isMapInitialized.value && customs.isNotEmpty()) {
            mapView.showCustoms(customs, context, theme)
        }
    }

    LaunchedEffect(location) {
        location?.let {
            if (!isFollowUserLocation) {
                val locationOverlay = mapView.overlays.filterIsInstance<CustomLocationOverlay>().firstOrNull()
                locationOverlay?.update(it)
                mapView.invalidate()
            }
            if (!isAnimatedToInitialLocation.value) {
                val point = GeoPoint(it)
                mapView.controller.animateTo(point)
                viewModel.loadCustomsInVisibleArea(point.createBoundingBoxAround(100.0), mapView.zoomLevelDouble)
                isAnimatedToInitialLocation.value = true
            }
        }
    }

    LaunchedEffect(location, customs) {
        if (isNearestCustomFound.value || customs.isEmpty()) return@LaunchedEffect
        val userPoint = location?.let { GeoPoint(it) } ?: return@LaunchedEffect
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
                 needToShowSurvey: MutableState<Boolean>,
                 isMapInitialized: MutableState<Boolean>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
    ) {
        val locationFlow = viewModel.location
        val isFollowFlow = viewModel.isFollowUserLocation
        LaunchedEffect(Unit) {
            val animator = SmoothCameraAnimator(
                mapView = mapView,
                scope = this,
                locationFlow = locationFlow,
                isFollowFlow = isFollowFlow
            )
            animator.start()
        }

        AndroidView(
            factory = {
                mapView.apply {
                    viewTreeObserver.addOnGlobalLayoutListener(object :
                        ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            if (!isMapInitialized.value) {
                                isMapInitialized.value = true
                                viewTreeObserver.removeOnGlobalLayoutListener(this)
                            }
                        }
                    })
                }
            }, modifier = Modifier
                .fillMaxSize()
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
                    viewModel.location.value?.let {
                        viewModel.enableFollow()
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
fun rememberConfiguredMapView(viewModel: MapViewModel = viewModel(),
                              settingsViewModel: SettingsViewModel = viewModel()
): MapView {
    val context = LocalContext.current
    val customs by viewModel.customs.collectAsState()
    val theme by settingsViewModel.themeMode.collectAsState()
    val mapView = remember {
        MapView(context).apply {
            id = R.id.map
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(false)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            controller.zoomTo(12.0)
        }
    }

    val locationOverlay = remember {
        CustomLocationOverlay().apply {
            icon = ContextCompat.getDrawable(context, R.drawable.ic_current_location)
        }
    }
    val rotationOverlay = remember {
        CustomRotationOverlay(viewModel)
    }
    mapView.overlays.add(locationOverlay)
    mapView.overlays.add(rotationOverlay)

    mapView.setupInteractionListeners(viewModel, context, customs, theme)
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
