package com.daristov.checkpoint.screens.mapscreen

import android.content.Context
import android.location.Location
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.Toast
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
import com.daristov.checkpoint.screens.mapscreen.overlay.CustomLocationProvider
import com.daristov.checkpoint.screens.mapscreen.overlay.CustomRotationOverlay
import com.daristov.checkpoint.screens.mapscreen.viewmodel.MapViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(navController: NavHostController, viewModel: MapViewModel = viewModel()) {
    val context = LocalContext.current
    val mapView = rememberConfiguredMapView()

    val isMapInitialized = remember { mutableStateOf(false) }
    val isAnimatedToInitialLocation = remember { mutableStateOf(false) }
    val isNearestCheckpointFound = remember { mutableStateOf(false) }
    val isInitialZoomDone = remember { mutableStateOf(false) }
    val needToShowSurvey = remember { mutableStateOf(false) }

    val location by viewModel.location.collectAsState()
    val nearestCheckpoint by viewModel.nearestCheckpoint.collectAsState()
    val distance by viewModel.distanceToNearestCheckpoint.collectAsState()

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
        viewModel.loadCheckpointsInVisibleArea(mapView.boundingBox, mapView.zoomLevelDouble)
    }

    val checkpoints by viewModel.checkpoints.collectAsState()
    LaunchedEffect(checkpoints, isMapInitialized) {
        if (isMapInitialized.value && checkpoints.isNotEmpty()) {
            mapView.showCheckpoints(checkpoints, context)
        }
    }

    LaunchedEffect(location) {
        location?.let {
            mapView.updateUserLocation(it)
            if (!isAnimatedToInitialLocation.value) {
                val point = GeoPoint(it)
                mapView.controller.animateTo(point)
                viewModel.loadCheckpointsInVisibleArea(point.createBoundingBoxAround(100.0), mapView.zoomLevelDouble)
                isAnimatedToInitialLocation.value = true
            }
        }
    }

    LaunchedEffect(location, checkpoints) {
        if (isNearestCheckpointFound.value || checkpoints.isEmpty()) return@LaunchedEffect
        val userPoint = location?.let { GeoPoint(it) } ?: return@LaunchedEffect
        viewModel.findAndSetNearestCheckpoint(userPoint)
        isNearestCheckpointFound.value = true
    }

    LaunchedEffect(location, nearestCheckpoint) {
        if (isInitialZoomDone.value || location == null || nearestCheckpoint == null) return@LaunchedEffect
        mapView.handleInitialZoomAndSurvey(location!!, nearestCheckpoint!!, distance, needToShowSurvey, viewModel)
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
        LaunchedEffect(Unit) {
            startSmoothFollowCamera(
                mapView = mapView,
                locationFlow = viewModel.location,
                isFollowFlow = viewModel.isFollowUserLocation
            )
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
                        mapView.controller.animateTo(GeoPoint(it), 18.0, 1000L)
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
fun rememberConfiguredMapView(viewModel: MapViewModel = viewModel()): MapView {
    val context = LocalContext.current
    val checkpoints by viewModel.checkpoints.collectAsState()
    val mapView = remember {
        MapView(context).apply {
            id = R.id.map
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(false)
            controller.setZoom(10.0)
        }
    }

    val locationOverlay = remember {
        CustomLocationOverlay(CustomLocationProvider(), mapView).apply {
            enableMyLocation()
        }
    }
    val rotationOverlay = remember {
        CustomRotationOverlay(viewModel)
    }
    mapView.overlays.add(locationOverlay)
    mapView.overlays.add(rotationOverlay)

    mapView.setupInteractionListeners(viewModel, context, checkpoints)
    return mapView
}

private fun MapView.setupInteractionListeners(
    viewModel: MapViewModel,
    context: Context,
    checkpoints: List<MapObject>
) {
    val minIntervalMs = 500L
    var lastCheckTime = 0L

    this.addMapListener(object : MapListener {
        override fun onScroll(event: ScrollEvent?): Boolean {
            val now = System.currentTimeMillis()
            if (now - lastCheckTime < minIntervalMs) return false
            lastCheckTime = now

            viewModel.loadCheckpointsInVisibleArea(boundingBox, zoomLevelDouble)
            showCheckpoints(checkpoints, context)
            return true
        }

        override fun onZoom(event: ZoomEvent?): Boolean {
            val now = System.currentTimeMillis()
            if (now - lastCheckTime < minIntervalMs) return false
            lastCheckTime = now

            viewModel.loadCheckpointsInVisibleArea(boundingBox, zoomLevelDouble)
            showCheckpoints(checkpoints, context)
            return true
        }
    })
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

fun MapView.showCheckpoints(checkpoints: List<MapObject>, context: Context) {
    val visibleBox = this.boundingBox
    overlays.removeAll { it is Marker }

    for (checkpoint in checkpoints) {
        val point = GeoPoint(checkpoint.latitude, checkpoint.longitude)
        if (visibleBox.contains(point)) {
            val marker = Marker(this).apply {
                position = point
                title = checkpoint.name
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(context, R.drawable.ic_checkpoint) // или стандартная иконка
                isDraggable = false
                setOnMarkerClickListener { marker, _ ->
                    //TODO
                    Toast.makeText(context, "КПП: ${marker.title}", Toast.LENGTH_SHORT).show()
                    true
                }
            }
            overlays.add(marker)
        }
    }

    invalidate()
}

fun GeoPoint.createBoundingBoxAround(distanceKm: Double): BoundingBox {
    val latDegreeDelta = distanceKm / 111.0 // приближённо 111 км на градус широты
    // для долготы учитываем сжатие по широте
    val lonDegreeDelta = distanceKm / (111.320 * cos(Math.toRadians(latitude)))
    return BoundingBox(
        latitude + latDegreeDelta,  // north
        longitude + lonDegreeDelta,  // east
        latitude - latDegreeDelta,  // south
        longitude - lonDegreeDelta   // west
    )
}

fun MapView.zoomToCheckpoints(
    userPoint: GeoPoint, nearest: List<GeoPoint>
) {
    val bounds = BoundingBox.fromGeoPointsSafe(nearest + userPoint).increaseByMargin(0.1) // 10% запас

    val target = nearest.first()
    mapOrientation = ((computeHeading(userPoint, target) + 90 + 360) % 360).toFloat()

    this.zoomToBoundingBox(bounds, true)
}

fun computeHeading(from: GeoPoint, to: GeoPoint): Double {
    val lat1 = Math.toRadians(from.latitude)
    val lon1 = Math.toRadians(from.longitude)
    val lat2 = Math.toRadians(to.latitude)
    val lon2 = Math.toRadians(to.longitude)

    val dLon = lon2 - lon1
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    return Math.toDegrees(atan2(y, x))
}

fun BoundingBox.increaseByMargin(marginFraction: Double): BoundingBox {
    val latSpan = this.latitudeSpan
    val lonSpan = this.longitudeSpan

    val latMargin = latSpan * marginFraction
    val lonMargin = lonSpan * marginFraction

    return BoundingBox(
        this.latNorth + latMargin,
        this.lonEast + lonMargin,
        this.latSouth - latMargin,
        this.lonWest - lonMargin
    )
}

private fun MapView.updateUserLocation(location: Location) {
    location.let {
        val locationOverlay = this.overlays.filterIsInstance<CustomLocationOverlay>().firstOrNull()
        locationOverlay?.updateLocation(it)
    }
}

private fun MapView.handleInitialZoomAndSurvey(
    location: Location,
    nearestCheckpoint: MapObject,
    distance: Double,
    needToShowSurvey: MutableState<Boolean>,
    viewModel: MapViewModel
) {
    val userPoint = GeoPoint(location)
    if (distance < 5000) {
        val heading = computeHeading(userPoint, nearestCheckpoint.location)
        this.mapOrientation = ((heading + 90 + 360) % 360).toFloat()
        this.controller.setZoom(18.0)
        needToShowSurvey.value = true
        return
    }

    if (location.speed > 10) { // ~36 км/ч
        var nearest: List<MapObject> = viewModel.findNearestCheckpoints(userPoint)
        this.zoomToCheckpoints(userPoint, nearest.map { GeoPoint(it.latitude, it.longitude) })
        return
    }

    this.controller.animateTo(userPoint, 18.0, 1000L)
    viewModel.enableFollow()
}

fun CoroutineScope.startSmoothFollowCamera(
    mapView: MapView,
    locationFlow: StateFlow<Location?>,
    isFollowFlow: StateFlow<Boolean>
) {
    var lastGeoPoint: GeoPoint? = null
    var currentJob: Job? = null

    launch {
        combine(locationFlow, isFollowFlow) { loc, follow -> loc to follow }
            .filter { (loc, follow) -> loc != null && follow }
            .collect { (location, _) ->
                val newGeoPoint = GeoPoint(location!!.latitude, location.longitude)
                val bearing = (360 - location.bearing) % 360

                // Прервать текущую анимацию, если есть
                currentJob?.cancel()

                // Первый кадр или слишком далеко — прыжок
                if (lastGeoPoint == null || lastGeoPoint!!.distanceToAsDouble(newGeoPoint) > 100) {
                    mapView.controller.setCenter(newGeoPoint)
                    mapView.mapOrientation = bearing
                    mapView.invalidate()
                    lastGeoPoint = newGeoPoint
                    return@collect
                }

                // Интерполяция позиции
                val startPoint = lastGeoPoint!!
                val durationMs = 1000L
                val steps = 30
                val stepDelay = durationMs / steps

                currentJob = launch {
                    for (i in 1..steps) {
                        val t = i / steps.toDouble()
                        val lat = lerp(startPoint.latitude, newGeoPoint.latitude, t)
                        val lon = lerp(startPoint.longitude, newGeoPoint.longitude, t)
                        val interpPoint = GeoPoint(lat, lon)

                        withContext(Dispatchers.Main) {
                            mapView.controller.setCenter(interpPoint)
                            mapView.mapOrientation = bearing
                            mapView.invalidate()
                        }

                        delay(stepDelay)
                    }

                    lastGeoPoint = newGeoPoint
                }
            }
    }
}

private fun lerp(a: Double, b: Double, t: Double): Double {
    return a + (b - a) * t
}



