package com.example.mylocation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.mylocation.ui.theme.MyLocationTheme
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.io.IOException

class MainActivity : ComponentActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).apply {
            setMinUpdateIntervalMillis(5000)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    setContent {
                        MaterialTheme {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                LocationScreen(location)
                            }
                        }
                    }
                }
            }
        }

        setContent {
            MyLocationTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    RequestPermissions(fusedLocationClient, locationRequest, locationCallback)
                }

            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
        }
    }

    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }


}

@Composable
fun RequestPermissions(
    fusedLocationClient: FusedLocationProviderClient,
    locationRequest: LocationRequest,
    locationCallback: LocationCallback
) {
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasLocationPermission = isGranted
        if (isGranted) {
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            } catch (e: SecurityException) {
                Log.e("Location", "Security Exception: ${e.message}")
            }
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
            } catch (e: SecurityException) {
                Log.e("Location", "Security Exception: ${e.message}")
            }
        }
    }
}

@Composable
fun LocationScreen(location: Location) {
    // Create the camera state based on the user's location
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(location.latitude, location.longitude), 12f)
    }

    val uiSettings = MapUiSettings(zoomControlsEnabled = true)

    // Marker state: List to store markers
    val markers = remember { mutableStateListOf<LatLng>() }
    markers.add(LatLng(location.latitude, location.longitude))

    val context = LocalContext.current

    // Handle map clicks to add custom markers
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        uiSettings = uiSettings,
        onMapClick = { latLng ->

            val locationInfo = getLocationInfo(context, latLng)

            // Add the marker to the list
            markers.add(latLng)
        }
    ) {
        // Display the markers on the map
        markers.forEach { markerPosition ->
            Marker(
                state = MarkerState(position = markerPosition),
                title = "Custom Location",
                snippet = getLocationInfo(LocalContext.current, markerPosition)

            )
        }
    }

    // If the location is not available, show a message
    if (location.latitude == 0.0 && location.longitude == 0.0) {
        Text(text = "Location Permission Needed")
    }
}


fun getLocationInfo(context: Context, latLng: LatLng): String {
    val geocoder = Geocoder(context)
    var locationInfo = "Address not found"
    try {
        val geocodeMatches = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        if (geocodeMatches != null && geocodeMatches.isNotEmpty()) {
            val address = geocodeMatches[0]
            locationInfo = "Address: ${address.getAddressLine(0)}\n" +
                    "City: ${address.locality}\n" +
                    "State: ${address.adminArea}\n" +
                    "Country: ${address.countryName}"
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
    return locationInfo
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {

        val mockLocation = Location("mockProvider").apply {
            latitude = 38.9072
            longitude = -77.0369
            altitude = 10.0
            speed = 0.0f
            time = System.currentTimeMillis()
        }

    MyLocationTheme {
        LocationScreen(mockLocation)
    }
}