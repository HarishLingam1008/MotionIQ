package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.local.RoutePoint
import com.example.data.local.SavedRoute
import com.example.data.local.UserProfile
import com.example.data.sensor.GpsDiagnostics
import com.example.data.sensor.LocationTrackingManager
import com.example.ui.components.GlassCard
import com.example.ui.theme.CyberAccentOrange
import com.example.ui.theme.CyberBackgroundDark
import com.example.ui.theme.CyberPinkGlow
import com.example.ui.theme.CyberPrimaryCyan
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.CameraMoveStartedReason
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    userProfile: UserProfile,
    savedRoutes: List<SavedRoute>,
    isTracking: Boolean,
    isPaused: Boolean,
    sessionSeconds: Long,
    sessionSteps: Int,
    formatDistance: (Double, Boolean) -> String,
    onStartTrackingSession: (Context) -> Unit,
    onPauseTrackingSession: (Context) -> Unit,
    onResumeTrackingSession: (Context) -> Unit,
    onStopTrackingSession: (Context) -> Unit,
    onDeleteSavedRoute: (SavedRoute) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) {
            LocationTrackingManager.fetchLastLocation(context)
            LocationTrackingManager.startContinuousLocationUpdates(context)
        }
    }

    // Collect Location Tracking StateFlows
    val currentLocation by LocationTrackingManager.currentLocation.collectAsState()
    val isGpsEnabled by LocationTrackingManager.isGpsEnabled.collectAsState()
    val routePoints by LocationTrackingManager.routePoints.collectAsState()
    val currentSpeedKmh by LocationTrackingManager.currentSpeedKmh.collectAsState()
    val avgSpeedKmh by LocationTrackingManager.avgSpeedKmh.collectAsState()
    val maxSpeedKmh by LocationTrackingManager.maxSpeedKmh.collectAsState()
    val totalDistanceMeters by LocationTrackingManager.totalDistanceMeters.collectAsState()
    val gpsAccuracyMeters by LocationTrackingManager.gpsAccuracyMeters.collectAsState()
    val altitudeMeters by LocationTrackingManager.altitudeMeters.collectAsState()
    val selectedActivity by LocationTrackingManager.selectedActivity.collectAsState()
    val gpsDiagnostics by LocationTrackingManager.gpsDiagnostics.collectAsState()

    var mapType by remember { mutableStateOf(MapType.NORMAL) }
    var showHistorySheet by remember { mutableStateOf(false) }
    var showDiagnosticsSheet by remember { mutableStateOf(false) }
    var selectedPreviewRoute by remember { mutableStateOf<SavedRoute?>(null) }

    // Camera position state
    val cameraPositionState = rememberCameraPositionState()
    var hasCenteredInitially by remember { mutableStateOf(false) }
    var isMapLoaded by remember { mutableStateOf(false) }
    var isAutoCenterEnabled by remember { mutableStateOf(true) }

    // Initial GPS Fix & Continuous Updates
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            LocationTrackingManager.fetchLastLocation(context)
            LocationTrackingManager.startContinuousLocationUpdates(context)
        }
    }

    // Periodic check for Location Services (GPS) toggle state
    LaunchedEffect(Unit) {
        LocationTrackingManager.isLocationServicesEnabled(context)
    }

    // Detect user manual gestures on the map and release auto-centering lock
    LaunchedEffect(cameraPositionState.isMoving) {
        if (cameraPositionState.isMoving && cameraPositionState.cameraMoveStartedReason == CameraMoveStartedReason.GESTURE) {
            if (isAutoCenterEnabled) {
                Log.d("MotionIQ_Map", "Manual map pan gesture detected. Pausing auto-follow.")
                isAutoCenterEnabled = false
            }
        }
    }

    // Camera Auto-Centering logic on user location
    LaunchedEffect(currentLocation, isAutoCenterEnabled, isTracking) {
        val loc = currentLocation ?: return@LaunchedEffect

        // If camera has never centered or is at (0,0), immediately center on user's real coordinates with zoom 17
        if (!hasCenteredInitially || (cameraPositionState.position.target.latitude == 0.0 && cameraPositionState.position.target.longitude == 0.0)) {
            val userPos = LatLng(loc.latitude, loc.longitude)
            Log.d("MotionIQ_Map", "Initial camera centering at real GPS coordinates: lat=${loc.latitude}, lng=${loc.longitude}")
            cameraPositionState.position = CameraPosition.fromLatLngZoom(userPos, 17.0f)
            hasCenteredInitially = true
            return@LaunchedEffect
        }

        if (!isAutoCenterEnabled) return@LaunchedEffect

        val targetLatLng = LatLng(loc.latitude, loc.longitude)
        val currentTarget = cameraPositionState.position.target

        val distToTarget = FloatArray(1)
        Location.distanceBetween(
            currentTarget.latitude,
            currentTarget.longitude,
            loc.latitude,
            loc.longitude,
            distToTarget
        )

        // Smooth camera follow if user moved > 1.5m
        if (distToTarget[0] > 1.5f) {
            val heading = if (isTracking && loc.hasBearing() && loc.speed > 0.6f) loc.bearing else 0f
            val tilt = if (isTracking && loc.speed > 0.6f) 35f else 0f
            val zoom = if (isTracking) 17.5f else 17.0f

            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(targetLatLng)
                        .zoom(zoom)
                        .tilt(tilt)
                        .bearing(heading)
                        .build()
                ),
                600
            )
        }
    }

    val isImperial = userProfile.unitSystem.equals("Imperial", ignoreCase = true)
    val isPlayServicesAvailable = remember(context) { LocationTrackingManager.checkPlayServicesAvailable(context) }

    // Network check
    var isNetworkConnected by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        isNetworkConnected = cm?.activeNetworkInfo?.isConnected == true
    }

    // Dismissable GPS dialog state
    var showGpsDialog by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("map_screen_root")
    ) {
        if (!isPlayServicesAvailable) {
            // Google Play Services missing error
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CyberBackgroundDark),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    shape = RoundedCornerShape(24.dp),
                    glowColor = CyberPinkGlow,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Error",
                            tint = CyberPinkGlow,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Google Play Services Required",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Google Play Services are required to render live Google Maps and GPS route tracking.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else if (!isGpsEnabled && showGpsDialog) {
            // Location Services OFF Prompt
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CyberBackgroundDark.copy(alpha = 0.95f)),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    shape = RoundedCornerShape(28.dp),
                    glowColor = CyberPrimaryCyan,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(CyberPrimaryCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOff,
                                contentDescription = "GPS Off",
                                tint = CyberPrimaryCyan,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Location Services Disabled",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Please turn on Location Services to start real-time GPS tracking and map positioning.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showGpsDialog = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                            }

                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        LocationTrackingManager.checkLocationServicesEnabled(context)
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.2f)
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimaryCyan),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Turn On Location", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else if (hasLocationPermission) {
            val hasFinePermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val polylineLatLngs = remember(routePoints) {
                routePoints.map { LatLng(it.latitude, it.longitude) }
            }

            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                onMapLoaded = {
                    isMapLoaded = true
                    currentLocation?.let { loc ->
                        if (!hasCenteredInitially || cameraPositionState.position.target.latitude == 0.0) {
                            cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                LatLng(loc.latitude, loc.longitude),
                                17.0f
                            )
                            hasCenteredInitially = true
                        }
                    }
                },
                properties = MapProperties(
                    mapType = mapType,
                    isMyLocationEnabled = hasFinePermission,
                    isTrafficEnabled = true,
                    isBuildingEnabled = true
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false, // We supply custom cyberpunk My Location buttons
                    compassEnabled = true,
                    zoomGesturesEnabled = true,
                    rotationGesturesEnabled = true,
                    scrollGesturesEnabled = true,
                    tiltGesturesEnabled = true
                )
            ) {
                // Single Reactive Live Marker for User Location
                currentLocation?.let { loc ->
                    Marker(
                        state = MarkerState(position = LatLng(loc.latitude, loc.longitude)),
                        title = "Your Real-Time Location",
                        snippet = "Lat: ${"%.5f".format(loc.latitude)}, Lng: ${"%.5f".format(loc.longitude)} (±${"%.0f".format(loc.accuracy)}m)",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }

                // Route Polyline
                if (polylineLatLngs.size >= 2) {
                    Polyline(
                        points = polylineLatLngs,
                        color = CyberPrimaryCyan,
                        width = 12f
                    )
                }

                // Start Marker (Green)
                if (routePoints.isNotEmpty()) {
                    val startPt = routePoints.first()
                    Marker(
                        state = MarkerState(position = LatLng(startPt.latitude, startPt.longitude)),
                        title = "Start Point",
                        snippet = "Time: ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(startPt.timestamp))}",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )
                }

                // Finish Marker (Red when session ended)
                if (routePoints.size > 1 && !isTracking) {
                    val endPt = routePoints.last()
                    Marker(
                        state = MarkerState(position = LatLng(endPt.latitude, endPt.longitude)),
                        title = "Finish Line",
                        snippet = "Distance: ${formatDistance(totalDistanceMeters, isImperial)}",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }
            }

            // Map Loading Overlay
            if (!isMapLoaded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CyberBackgroundDark),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = CyberPrimaryCyan)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Locating your GPS position...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Internet Connection Warning
            if (!isNetworkConnected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 135.dp, start = 16.dp, end = 16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    GlassCard(
                        shape = RoundedCornerShape(16.dp),
                        glowColor = CyberPinkGlow
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = "Offline",
                                tint = CyberPinkGlow,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Internet connection required to stream map tiles.",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // GPS Signal Quality Banner
            if (currentLocation == null && isMapLoaded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 135.dp, start = 16.dp, end = 16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    GlassCard(
                        shape = RoundedCornerShape(16.dp),
                        glowColor = CyberAccentOrange
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = CyberAccentOrange
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Acquiring real-time GPS location fix...",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        } else {
            // Permission missing fallback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CyberBackgroundDark),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = "Location Disabled",
                        tint = CyberPinkGlow,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Location Permission Required",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To track your exact real-time GPS location and routes, please grant location access.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row {
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberPrimaryCyan)
                        ) {
                            Text("Grant Location Permission", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null)
                                    )
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("MotionIQ_Map", "Could not open app settings: ${e.message}")
                                }
                            }
                        ) {
                            Text("App Settings")
                        }
                    }
                }
            }
        }

        // TOP OVERLAY BAR: Activity selector & Diagnostics
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            GlassCard(
                shape = RoundedCornerShape(20.dp),
                glowColor = CyberPrimaryCyan
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🗺️ MotionIQ GPS Map",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Row {
                            // GPS Diagnostics Sheet Button
                            IconButton(
                                onClick = { showDiagnosticsSheet = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Analytics,
                                    contentDescription = "GPS Diagnostics",
                                    tint = CyberPrimaryCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Auto-Center Location Button
                            IconButton(
                                onClick = {
                                    isAutoCenterEnabled = true
                                    LocationTrackingManager.fetchLastLocation(context)
                                    val loc = currentLocation
                                    if (loc != null) {
                                        coroutineScope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newCameraPosition(
                                                    CameraPosition.Builder()
                                                        .target(LatLng(loc.latitude, loc.longitude))
                                                        .zoom(17.5f)
                                                        .tilt(if (isTracking) 35f else 0f)
                                                        .bearing(if (isTracking && loc.hasBearing()) loc.bearing else 0f)
                                                        .build()
                                                ),
                                                500
                                            )
                                        }
                                    } else {
                                        coroutineScope.launch {
                                            snackbarHostState.showSnackbar("Acquiring GPS location...")
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isAutoCenterEnabled) CyberPrimaryCyan.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .testTag("map_auto_center_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "Auto Center Location",
                                    tint = if (isAutoCenterEnabled) CyberPrimaryCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Map Type Toggle Button
                            IconButton(
                                onClick = {
                                    mapType = if (mapType == MapType.NORMAL) MapType.SATELLITE else MapType.NORMAL
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = "Map Style",
                                    tint = CyberPrimaryCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            // Route History Drawer Toggle Button
                            IconButton(
                                onClick = { showHistorySheet = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Saved Route History",
                                    tint = CyberPrimaryCyan,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Activity Selector Pills
                    val activities = listOf("Walking", "Running", "Jogging", "Cycling")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(activities) { act ->
                            val isSelected = selectedActivity == act
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (isSelected) CyberPrimaryCyan else MaterialTheme.colorScheme.surfaceVariant.copy(
                                            alpha = 0.4f
                                        )
                                    )
                                    .clickable {
                                        LocationTrackingManager.setSelectedActivity(act)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val iconVector = when (act) {
                                        "Running" -> Icons.AutoMirrored.Filled.DirectionsRun
                                        "Cycling" -> Icons.AutoMirrored.Filled.DirectionsBike
                                        else -> Icons.AutoMirrored.Filled.DirectionsWalk
                                    }
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = act,
                                        tint = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = act,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // FLOATING "MY LOCATION" BUTTON
        if (hasLocationPermission) {
            IconButton(
                onClick = {
                    isAutoCenterEnabled = true
                    LocationTrackingManager.fetchLastLocation(context)
                    val loc = currentLocation
                    if (loc != null) {
                        coroutineScope.launch {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newCameraPosition(
                                    CameraPosition.Builder()
                                        .target(LatLng(loc.latitude, loc.longitude))
                                        .zoom(17.5f)
                                        .tilt(if (isTracking) 35f else 0f)
                                        .bearing(if (isTracking && loc.hasBearing()) loc.bearing else 0f)
                                        .build()
                                ),
                                600
                            )
                        }
                    } else {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Waiting for GPS location...")
                        }
                    }
                },
                modifier = Modifier
                    .padding(end = 16.dp, bottom = 265.dp)
                    .size(48.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        1.5.dp,
                        if (isAutoCenterEnabled) CyberPrimaryCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "My Location",
                    tint = if (isAutoCenterEnabled) CyberPrimaryCyan else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // BOTTOM HUD DASHBOARD & TRACKING CONTROLS
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.BottomCenter)
        ) {
            GlassCard(
                shape = RoundedCornerShape(26.dp),
                glowColor = if (isTracking) CyberPrimaryCyan else Color.Transparent
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // LIVE STATS HUD MATRIX
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        HudStatItem(
                            label = "Speed",
                            value = "%.1f".format(if (isImperial) currentSpeedKmh * 0.621371f else currentSpeedKmh),
                            unit = if (isImperial) "mph" else "km/h",
                            highlight = isTracking
                        )

                        HudStatItem(
                            label = "Distance",
                            value = formatDistance(totalDistanceMeters, isImperial),
                            unit = "",
                            highlight = false
                        )

                        HudStatItem(
                            label = "Duration",
                            value = formatDuration(sessionSeconds),
                            unit = "",
                            highlight = false
                        )

                        HudStatItem(
                            label = "Steps",
                            value = "$sessionSteps",
                            unit = "steps",
                            highlight = false
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        HudStatItem(
                            label = "Avg Speed",
                            value = "%.1f".format(if (isImperial) avgSpeedKmh * 0.621371 else avgSpeedKmh),
                            unit = if (isImperial) "mph" else "km/h",
                            highlight = false
                        )

                        HudStatItem(
                            label = "Max Speed",
                            value = "%.1f".format(if (isImperial) maxSpeedKmh * 0.621371 else maxSpeedKmh),
                            unit = if (isImperial) "mph" else "km/h",
                            highlight = false
                        )

                        HudStatItem(
                            label = "Accuracy",
                            value = if (gpsAccuracyMeters > 0f) "±%.0f".format(gpsAccuracyMeters) else "--",
                            unit = if (gpsAccuracyMeters > 0f) "m" else "",
                            highlight = false
                        )

                        HudStatItem(
                            label = "Altitude",
                            value = "%.0f".format(if (isImperial) altitudeMeters * 3.28084 else altitudeMeters),
                            unit = if (isImperial) "ft" else "m",
                            highlight = false
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // ACTION CONTROL BUTTONS: START / STOP GPS TRACKING
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!isTracking) {
                            // START GPS TRACKING BUTTON
                            Button(
                                onClick = {
                                    if (!LocationTrackingManager.isLocationServicesEnabled(context)) {
                                        showGpsDialog = true
                                        return@Button
                                    }
                                    if (!hasLocationPermission) {
                                        permissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                        return@Button
                                    }
                                    isAutoCenterEnabled = true
                                    LocationTrackingManager.fetchLastLocation(context)
                                    onStartTrackingSession(context)
                                    currentLocation?.let { loc ->
                                        coroutineScope.launch {
                                            cameraPositionState.animate(
                                                CameraUpdateFactory.newCameraPosition(
                                                    CameraPosition.Builder()
                                                        .target(LatLng(loc.latitude, loc.longitude))
                                                        .zoom(17.5f)
                                                        .build()
                                                ),
                                                500
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("map_start_tracking_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPrimaryCyan),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = "Start GPS Tracking",
                                    tint = Color.Black
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "START GPS TRACKING",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                )
                            }
                        } else {
                            // PAUSE / RESUME BUTTON
                            Button(
                                onClick = {
                                    if (isPaused) onResumeTrackingSession(context) else onPauseTrackingSession(context)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isPaused) CyberPrimaryCyan else MaterialTheme.colorScheme.secondary
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (isPaused) "Resume" else "Pause",
                                    tint = Color.Black
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isPaused) "RESUME" else "PAUSE",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // STOP GPS TRACKING BUTTON
                            Button(
                                onClick = { onStopTrackingSession(context) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("map_stop_tracking_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberPinkGlow),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Tracking",
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "STOP GPS TRACKING",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // DEVELOPER DIAGNOSTICS BOTTOM SHEET
        if (showDiagnosticsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showDiagnosticsSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                tint = CyberPrimaryCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GPS Diagnostics",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        IconButton(onClick = { showDiagnosticsSheet = false }) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            DiagnosticRow("Latitude", if (gpsDiagnostics.hasGpsFix) "%.6f".format(gpsDiagnostics.latitude) else "No Fix")
                            DiagnosticRow("Longitude", if (gpsDiagnostics.hasGpsFix) "%.6f".format(gpsDiagnostics.longitude) else "No Fix")
                            DiagnosticRow("GPS Accuracy", if (gpsDiagnostics.accuracyMeters > 0f) "±%.1f m".format(gpsDiagnostics.accuracyMeters) else "N/A")
                            DiagnosticRow("Speed", "%.2f m/s (%.1f km/h)".format(gpsDiagnostics.speedMps, gpsDiagnostics.speedKmh))
                            DiagnosticRow("Altitude", "%.1f m".format(gpsDiagnostics.altitudeMeters))
                            DiagnosticRow("Location Provider", gpsDiagnostics.provider)
                            DiagnosticRow("Timestamp", if (gpsDiagnostics.timestamp > 0) SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(gpsDiagnostics.timestamp)) else "N/A")
                            DiagnosticRow("Tracking State", gpsDiagnostics.trackingState, highlight = gpsDiagnostics.trackingState == "ACTIVE")
                            DiagnosticRow("Total Points Recorded", "${gpsDiagnostics.totalPoints}")
                            DiagnosticRow("Total GPS Distance", "%.2f m (%.3f km)".format(gpsDiagnostics.distanceMeters, gpsDiagnostics.distanceMeters / 1000.0))
                            DiagnosticRow("Rejected GPS Jumps", "${gpsDiagnostics.rejectedJumpsCount}")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            LocationTrackingManager.fetchLastLocation(context)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimaryCyan),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Force Fresh GPS Poll", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // SAVED ROUTE HISTORY BOTTOM SHEET
        if (showHistorySheet) {
            ModalBottomSheet(
                onDismissRequest = { showHistorySheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "📜 Saved Route History",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (savedRoutes.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No saved routes found. Start a GPS tracking session to record your routes!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxHeight(0.7f)
                        ) {
                            items(savedRoutes) { route ->
                                SavedRouteCard(
                                    route = route,
                                    isImperial = isImperial,
                                    formatDistance = formatDistance,
                                    onSelect = {
                                        selectedPreviewRoute = route
                                        showHistorySheet = false
                                    },
                                    onDelete = { onDeleteSavedRoute(route) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // PREVIEW SAVED ROUTE MODAL
        selectedPreviewRoute?.let { route ->
            val points = remember(route) { route.parsePoints() }
            val polylinePoints = remember(points) { points.map { LatLng(it.latitude, it.longitude) } }
            val previewCameraState = rememberCameraPositionState()

            LaunchedEffect(polylinePoints) {
                if (polylinePoints.isNotEmpty()) {
                    val builder = LatLngBounds.Builder()
                    polylinePoints.forEach { builder.include(it) }
                    try {
                        previewCameraState.move(CameraUpdateFactory.newLatLngBounds(builder.build(), 100))
                    } catch (e: Exception) {
                        previewCameraState.position = CameraPosition.fromLatLngZoom(polylinePoints.first(), 15f)
                    }
                }
            }

            ModalBottomSheet(
                onDismissRequest = { selectedPreviewRoute = null }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📍 ${route.activityType} Route Summary",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        IconButton(onClick = { onDeleteSavedRoute(route); selectedPreviewRoute = null }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Route",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Text(
                        text = route.dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Route Map Preview Frame
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = previewCameraState,
                            uiSettings = MapUiSettings(zoomControlsEnabled = true)
                        ) {
                            if (polylinePoints.size >= 2) {
                                Polyline(
                                    points = polylinePoints,
                                    color = CyberPrimaryCyan,
                                    width = 10f
                                )
                                Marker(
                                    state = MarkerState(position = polylinePoints.first()),
                                    title = "Start",
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                                )
                                Marker(
                                    state = MarkerState(position = polylinePoints.last()),
                                    title = "Finish",
                                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // STATS MATRIX
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        HudStatItem(label = "Distance", value = formatDistance(route.distanceMeters, isImperial), unit = "")
                        HudStatItem(label = "Duration", value = formatDuration(route.durationSeconds), unit = "")
                        HudStatItem(label = "Avg Speed", value = "%.1f".format(if (isImperial) route.avgSpeedKmh * 0.621371 else route.avgSpeedKmh), unit = if (isImperial) "mph" else "km/h")
                        HudStatItem(label = "Calories", value = "%.0f".format(route.caloriesBurned), unit = "kcal")
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { selectedPreviewRoute = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPrimaryCyan)
                    ) {
                        Text("Close Route Preview", color = Color.Black)
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
        )
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String, highlight: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
                color = if (highlight) CyberPrimaryCyan else MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

@Composable
fun HudStatItem(
    label: String,
    value: String,
    unit: String,
    highlight: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                letterSpacing = 0.5.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (highlight) CyberPrimaryCyan else MaterialTheme.colorScheme.onSurface
                )
            )
            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SavedRouteCard(
    route: SavedRoute,
    isImperial: Boolean,
    formatDistance: (Double, Boolean) -> String,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(CyberPrimaryCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    val iconVector = when (route.activityType) {
                        "Running" -> Icons.AutoMirrored.Filled.DirectionsRun
                        "Cycling" -> Icons.AutoMirrored.Filled.DirectionsBike
                        else -> Icons.AutoMirrored.Filled.DirectionsWalk
                    }
                    Icon(
                        imageVector = iconVector,
                        contentDescription = route.activityType,
                        tint = CyberPrimaryCyan,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "${route.activityType} Route",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = route.dateString,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatDistance(route.distanceMeters, isImperial)} • ${formatDuration(route.durationSeconds)} • ${"%.0f".format(route.caloriesBurned)} kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = CyberPrimaryCyan
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Route",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }
}
