package com.daristov.checkpoint.screens.mapscreen

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.ViewTreeObserver
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiTransportation
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.daristov.checkpoint.R
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
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
                // Кнопка "Мое местоположение"
                IconButton(
                    onClick = {
                        viewModel.currentLocation.value?.let { mapView.controller.animateTo(GeoPoint(it)) }
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

                // FAB "Будильник"
                FloatingActionButton(
                    onClick = { navController.navigate("alarm") }
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "Будильник")
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadCheckpointsInVisibleArea(mapView.boundingBox, mapView.zoomLevelDouble)
    }

    val checkpoints by viewModel.checkpoints.collectAsState()

    LaunchedEffect(checkpoints, isMapInitialized) {
        if (isMapInitialized.value && checkpoints.isNotEmpty()) {
            mapView.overlays.clear()
            val boundingBox = mapView.boundingBox
            mapView.showCheckpoints(checkpoints, context)
            mapView.addOverlays(viewModel)
            mapView.zoomToBoundingBox(boundingBox, false)
        }
    }
}

@Composable
fun rememberMapViewWithLifecycle(viewModel: MapViewModel = viewModel()): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapView(context).apply {
            id = R.id.map
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
        }
    }
    mapView.initializeMapView(viewModel)

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return mapView
}

fun MapView.initializeMapView(
    viewModel: MapViewModel
) {
    // Центр и зум
    controller.setZoom(12.0)
//    if (viewModel.currentLocation.value != null) {
//        controller.setCenter(GeoPoint(viewModel.currentLocation.value))
//    } else {
//        controller.setCenter(GeoPoint(55.751244, 37.618423))
//    }

    addMapListener(object : MapListener {
        private var lastCheckTime = 0L
        private val minIntervalMs = 500L  // защита от частого вызова

        override fun onScroll(event: ScrollEvent?): Boolean {
            val now = System.currentTimeMillis()
            if (now - lastCheckTime < minIntervalMs) return false
            lastCheckTime = now

            viewModel.loadCheckpointsInVisibleArea(boundingBox, zoomLevelDouble)
            return true
        }

        override fun onZoom(event: ZoomEvent?): Boolean {
            val now = System.currentTimeMillis()
            if (now - lastCheckTime < minIntervalMs) return false
            lastCheckTime = now

            viewModel.loadCheckpointsInVisibleArea(boundingBox, zoomLevelDouble)
            return true
        }
    })

    this.addOverlays(viewModel)
}

fun MapView.showCheckpoints(checkpoints: List<MapObject>, context: Context) {
    Icons.Default.EmojiTransportation
    val items = checkpoints.map {
        Log.d("MapScreen", "added checkpoint ${it.name}")
        OverlayItem(it.name, it.name, it.location).apply {
            setMarker(ContextCompat.getDrawable(context, R.drawable.ic_checkpoint))
        }
    }

    val overlay = ItemizedOverlayWithFocus(
        items,
        object : ItemizedIconOverlay.OnItemGestureListener<OverlayItem> {
            override fun onItemSingleTapUp(index: Int, item: OverlayItem?): Boolean {
                Toast.makeText(context, item?.title.orEmpty(), Toast.LENGTH_SHORT).show()
                return true
            }

            override fun onItemLongPress(index: Int, item: OverlayItem?): Boolean {
                Toast.makeText(context, item?.title.orEmpty(), Toast.LENGTH_SHORT).show()
                return true
            }
        },
        context
    )

    overlays.add(overlay)
}

fun MapView.addOverlays(
    viewModel: MapViewModel
) {
    // Отображение текущего местоположения
    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(context), this).apply {
        enableMyLocation()
        //TODO
        enableFollowLocation()
        runOnFirstFix {
            if (myLocation != null) {
                Handler(Looper.getMainLooper()).post {
                    if (viewModel.currentLocation.value == null) {
                        //TODO
                        controller.animateTo(myLocation)
                        val bbox = createBoundingBoxAround(myLocation, 100.0)
                        viewModel.loadCheckpointsInVisibleArea(bbox, zoomLevelDouble)
                    } else {
                        viewModel.loadCheckpointsInVisibleArea(boundingBox, zoomLevelDouble)
                    }
                    viewModel.updateCurrentLocation(myLocation)
                 }
            }
        }
    }

    // Вращение карты двумя пальцами
    val rotationOverlay = RotationGestureOverlay(this).apply {
        isEnabled = true
    }

    overlays.add(0, rotationOverlay)
    overlays.add(1, locationOverlay)
}

fun MapView.zoomToCheckpoints(
    viewModel: MapViewModel
) {
    val myLocation = viewModel.currentLocation.value
    if (myLocation == null) {
        return
    }
    val nearest = viewModel.findNearestCheckpoints(myLocation)
    if (nearest.isEmpty())
        return

    val allPoints = nearest.map { it.location } + myLocation

    // Создаём bounding box с отступами
    val bounds = BoundingBox.fromGeoPointsSafe(allPoints).increaseByMargin(0.1) // 10% запас

    // Поворачиваем карту: поворот в сторону ближайшего КПП
    val target = nearest.first().location
    mapOrientation = ((computeHeading(myLocation, target) + 90 + 360) % 360).toFloat()

    // Центруем карту по bbox
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

fun createBoundingBoxAround(location: GeoPoint, distanceKm: Double): BoundingBox {
    val lat = location.latitude
    val lon = location.longitude

    val latDegreeDelta = distanceKm / 111.0 // приближённо 111 км на градус широты

    // для долготы учитываем сжатие по широте
    val lonDegreeDelta = distanceKm / (111.320 * cos(Math.toRadians(lat)))

    return BoundingBox(
        lat + latDegreeDelta,  // north
        lon + lonDegreeDelta,  // east
        lat - latDegreeDelta,  // south
        lon - lonDegreeDelta   // west
    )
}
