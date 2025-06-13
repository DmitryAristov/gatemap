package com.daristov.checkpoint.screens.mapscreen

import android.content.Context
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
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
import kotlinx.coroutines.delay
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
    var mapReady = false
    val mapView = rememberMapViewWithLifecycle()
    val isMapInitialized = remember { mutableStateOf(false) }
    var isInitialZoomDone by remember { mutableStateOf(false) }
    var isNearestCheckpointFound by remember { mutableStateOf(false) }
    val location by viewModel.location.collectAsState()
    val nearestCheckpoint by viewModel.nearestCheckpoint.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Очереди на КПП") },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            AndroidView(factory = { mapView.apply {
                viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (!isMapInitialized.value) {
                            isMapInitialized.value = true
                            viewTreeObserver.removeOnGlobalLayoutListener(this)
                        }
                    }
                })
            }}, modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    mapReady = true
                })
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
                        viewModel.location.value?.let {
                            mapView.controller.animateTo(GeoPoint(it))
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            MaterialTheme.colorScheme.background,
                            shape = RoundedCornerShape(16.dp)
                        )
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Мое местоположение", tint = Color.White)
                }

                FloatingActionButton(
                    onClick = { navController.navigate("alarm") }
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Будильник")
                }
            }
            QueueSurveyManager(
                isUserInQueue = isNearestCheckpointFound,
                onAnswer = { minutes ->
                    viewModel.sendSurveyAnswer(minutes)
                },
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
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
            val point = GeoPoint(it.latitude, it.longitude)
            mapView.overlays.filterIsInstance<CustomLocationOverlay>().firstOrNull()?.updateLocation(it)

            if (!isInitialZoomDone) {
                mapView.controller.animateTo(point)
                val bbox = point.createBoundingBoxAround(100.0)
                viewModel.loadCheckpointsInVisibleArea(bbox, mapView.zoomLevelDouble)
                isInitialZoomDone = true
            }
        }
    }

    LaunchedEffect(location, checkpoints) {
        if (isNearestCheckpointFound) return@LaunchedEffect
        val loc = location ?: return@LaunchedEffect
        if (checkpoints.isEmpty()) return@LaunchedEffect

        val userPoint = GeoPoint(loc.latitude, loc.longitude)
        val nearestCheckpoint = viewModel.findNearestCheckpoints(userPoint, 1).firstOrNull()

        if (nearestCheckpoint != null) {
            val cpPoint = GeoPoint(nearestCheckpoint.latitude, nearestCheckpoint.longitude)
            val distance = userPoint.distanceToAsDouble(cpPoint)

            viewModel.updateNearestCheckpoint(nearestCheckpoint)
            isNearestCheckpointFound = true
            Toast.makeText(
                mapView.context,
                "Ближайший КПП: ${nearestCheckpoint.name}, ${"%.0f".format(distance)} м",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    LaunchedEffect(location, nearestCheckpoint) {
        val loc = location ?: return@LaunchedEffect
        val nearest = nearestCheckpoint ?: return@LaunchedEffect

        val userPoint = GeoPoint(loc.latitude, loc.longitude)
        val cpPoint = GeoPoint(nearest.latitude, nearest.longitude)
        val distance = userPoint.distanceToAsDouble(cpPoint)

        if (distance < 60_000) {
            val heading = computeHeading(userPoint, nearest.location)
            mapView.mapOrientation = ((heading + 90 + 360) % 360).toFloat()

            mapView.overlays
                .filterIsInstance<CustomLocationOverlay>()
                .firstOrNull()
                ?.enableFollowLocation()
            return@LaunchedEffect
        }

        if (loc.speed > 10) {

        } else {

        }
    }
}

@Composable
fun rememberMapViewWithLifecycle(viewModel: MapViewModel = viewModel()): MapView {
    val context = LocalContext.current
    val checkpoints by viewModel.checkpoints.collectAsState()
    val mapView = remember {
        MapView(context).apply {
            id = R.id.map
            setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
            setMultiTouchControls(false)
            controller.setZoom(12.0)
        }
    }

    mapView.addMapListener(object : MapListener {
        private var lastCheckTime = 0L
        private val minIntervalMs = 500L

        override fun onScroll(event: ScrollEvent?): Boolean {
            val now = System.currentTimeMillis()
            if (now - lastCheckTime < minIntervalMs) return false
            lastCheckTime = now

            viewModel.loadCheckpointsInVisibleArea(mapView.boundingBox, mapView.zoomLevelDouble)
            mapView.showCheckpoints(checkpoints, context)
            return true
        }

        override fun onZoom(event: ZoomEvent?): Boolean {
            val now = System.currentTimeMillis()
            if (now - lastCheckTime < minIntervalMs) return false
            lastCheckTime = now

            viewModel.loadCheckpointsInVisibleArea(mapView.boundingBox, mapView.zoomLevelDouble)
            mapView.showCheckpoints(checkpoints, context)
            return true
        }
    })

    val locationOverlay = remember {
        CustomLocationOverlay(CustomLocationProvider(), mapView).apply {
            enableMyLocation()
        }
    }
    mapView.overlays.add(locationOverlay)
    mapView.overlays.add(CustomRotationOverlay(mapView))

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
            Text("Подскажите пожалуйста сколько примерно сейчас в очереди машин?")
            for (i in options.indices) {
                val value = options[i]
                Button(
                    onClick = { onAnswer(value) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground)
                ) {
                    if (i != options.lastIndex)
                        Text("$value – ${options[i + 1]}", color = MaterialTheme.colorScheme.background)
                    else
                        Text("> $value", color = MaterialTheme.colorScheme.background)
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
                    Toast.makeText(context, "КПП: ${marker.title}", Toast.LENGTH_SHORT).show()
                    true
                }
            }
            overlays.add(marker)
        }
    }

    invalidate()
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
