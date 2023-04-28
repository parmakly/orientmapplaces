package com.parmclee.orientmapplaces.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority.*
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.Point
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.dsl.cameraOptions
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.camera
import ru.orientmapplaces.R
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference


@SuppressLint("MissingPermission")
@ExperimentalPermissionsApi
@Composable
fun MapViewContainer(
    map: MapView,
    permissionState: PermissionState,
    isLocationClicked: MutableState<Boolean>,
    onMapReady: (MapboxMap?) -> Unit = {}
) {
    val mapboxMap = remember { mutableStateOf<MapboxMap?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val locationProvider = remember { LocationServices.getFusedLocationProviderClient(context) }
    val lastLocation = remember { mutableStateOf<Point?>(null) }
    AndroidView({ map }) { mapView ->
        coroutineScope.launch {
            mapboxMap.value = mapView.getMapboxMap()
            onMapReady(mapboxMap.value)
        }
    }
    if (permissionState.status == PermissionStatus.Granted) {
        LaunchedEffect(lastLocation.value.hashCode() + if (isLocationClicked.value) 1 else 0) {
            if (isLocationClicked.value && lastLocation.value != null) {
                map.camera.flyTo(cameraOptions {
                    center(lastLocation.value!!)
                })
                isLocationClicked.value = false
            }
        }
        LaunchedEffect(Unit) {
            locationProvider.lastLocation.addOnCompleteListener {
                if (it.isSuccessful) {
                    lastLocation.value =
                        it.result?.let { Point.fromLngLat(it.longitude, it.latitude) }
                }
            }
            locationProvider.requestLocationUpdates(
                LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, 3000)
                    .setMaxUpdateDelayMillis(10000)
                    .setMaxUpdateAgeMillis(Long.MAX_VALUE)
                    .setMinUpdateDistanceMeters(5f)
                    .build(),
                object : LocationCallback() {

                    override fun onLocationResult(result: LocationResult) {
                        lastLocation.value =
                            result.lastLocation?.let { Point.fromLngLat(it.longitude, it.latitude) }
                    }
                }, context.mainLooper)
        }
    }
}

/**
 * Remembers a MapView and gives it the lifecycle of the current LifecycleOwner
 */
@Composable
fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, mapView) {
        // Make MapView follow the current lifecycle
        val lifecycleObserver = getMapLifecycleObserver(mapView)
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }

    return mapView
}

private fun getMapLifecycleObserver(mapView: MapView): LifecycleEventObserver =
    LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_START -> mapView.onStart()
            Lifecycle.Event.ON_STOP -> mapView.onStop()
            Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
            else -> {}
        }
    }

@Composable
fun BoxScope.MapButtons(onLocation: () -> Unit,
                        map: MapView) {
    Column(
        Modifier
            .align(Alignment.CenterEnd)
            .padding(12.dp)) {
        ZoomButton(icon = R.drawable.add_circle_48px, map, true)
        Spacer(modifier = Modifier.height(16.dp))
        ZoomButton(icon = R.drawable.do_not_disturb_on_48px, map, false)
        Spacer(modifier = Modifier.height(16.dp))
        MapButton(icon = R.drawable.my_location_48px, onLocation)
    }
}

@Composable
fun MapButton(icon: Int,
              onClick: () -> Unit,
              iconTint: Color = Black,
              backgroundTint: Color = White) {
    Surface(shadowElevation = 6.dp,
        shape = CircleShape) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(backgroundTint)
                .clickable { onClick() }) {
            Icon(painter = painterResource(id = icon), contentDescription = null,
                Modifier.align(Alignment.Center), tint = iconTint)
        }
    }
}

@Composable
fun ZoomButton(icon: Int,
               map: MapView,
               isZoomIn: Boolean,
               iconTint: Color = Black,
               backgroundTint: Color = White) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animationCounter = remember { mutableStateOf(0) }
    val animationEndListener = object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            animationCounter.value += 1
        }
    }
    LaunchedEffect(animationCounter.value + if (isPressed) 100 else 10) {
        if (isPressed) {
            map.camera.apply {
                val zoomBy = if (isZoomIn) 2.0 else 0.5
                scaleBy(zoomBy, null,
                    animationOptions = MapAnimationOptions.mapAnimationOptions {
                        animatorListener(animationEndListener)
                        interpolator(LinearInterpolator())
                    })
            }
        }
    }
    Surface(shadowElevation = 6.dp,
        shape = CircleShape) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(backgroundTint)
                .clickable(onClick = {},
                    interactionSource = interactionSource,
                    indication = LocalIndication.current)) {
            Icon(painter = painterResource(id = icon), contentDescription = null,
                Modifier.align(Alignment.Center), tint = iconTint)
        }
    }
}

class LocationPermissionHelper(val activity: WeakReference<Activity>) {
    private lateinit var permissionsManager: PermissionsManager

    fun checkPermissions(onMapReady: () -> Unit) {
        if (PermissionsManager.areLocationPermissionsGranted(activity.get())) {
            onMapReady()
        } else {
            permissionsManager = PermissionsManager(object : PermissionsListener {
                override fun onExplanationNeeded(permissionsToExplain: List<String>) {
                    Toast.makeText(
                        activity.get(), R.string.location_need_explain,
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onPermissionResult(granted: Boolean) {
                    if (granted) {
                        onMapReady()
                    }
                }
            })
            permissionsManager.requestLocationPermissions(activity.get())
        }
    }

    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}

fun bitmapFromDrawableRes(context: Context, @DrawableRes resourceId: Int) =
    convertDrawableToBitmap(ContextCompat.getDrawable(context, resourceId))

private fun convertDrawableToBitmap(sourceDrawable: Drawable?): Bitmap? {
    if (sourceDrawable == null) {
        return null
    }
    return if (sourceDrawable is BitmapDrawable) {
        sourceDrawable.bitmap
    } else {
// copying drawable object to not manipulate on the same reference
        val constantState = sourceDrawable.constantState ?: return null
        val drawable = constantState.newDrawable().mutate()
        val bitmap: Bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth, drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bitmap
    }
}