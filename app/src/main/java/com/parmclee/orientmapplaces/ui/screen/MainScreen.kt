package com.parmclee.orientmapplaces.ui.screen

import android.Manifest
import android.content.Context
import android.view.Gravity
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.mapbox.geojson.Point
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.Annotation
import com.mapbox.maps.plugin.annotation.AnnotationConfig
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.compass.compass
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.locationcomponent.location
import ru.orientmapplaces.R
import com.parmclee.orientmapplaces.data.AnnotationManagersBundle
import com.parmclee.orientmapplaces.data.MapAnnotationsId
import com.parmclee.orientmapplaces.data.MapInfo
import com.parmclee.orientmapplaces.ui.*
import com.parmclee.orientmapplaces.ui.theme.BorderPurple
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.launch


@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterialApi::class,
    ExperimentalComposeUiApi::class)
@Composable
fun MainScreen(permissionHelper: LocationPermissionHelper,
               viewModel: MainViewModel,
               onSettings: () -> Unit) {
    val mapView = rememberMapViewWithLifecycle()
    val permissionState =
        rememberPermissionState(permission = Manifest.permission.ACCESS_FINE_LOCATION)
    val isLocationClicked = remember { mutableStateOf(false) }
    val mapboxMap = remember { mutableStateOf<MapboxMap?>(null) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val menuExpanded = remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bottomSheetScaffoldState = rememberBottomSheetScaffoldState()
    fun setCurrentMap(mapInfo: MapInfo) {
        val bounds = mapInfo.getBounds()
        if (bounds != null) {
            val opts = mapboxMap.value?.cameraForCoordinateBounds(
                bounds,
                padding = EdgeInsets(20.0, 20.0, 20.0, 20.0))
            if (opts != null && mapboxMap.value != null) {
                mapboxMap.value?.flyTo(opts)
            }
        }
        coroutineScope.launch {
            if (bottomSheetScaffoldState.bottomSheetState.isExpanded) {
                bottomSheetScaffoldState.bottomSheetState.collapse()
            }
            viewModel.selectMap(mapInfo)
            awaitFrame()
            bottomSheetScaffoldState.bottomSheetState.expand()
        }
    }
    val annotationsManagers = remember {
        instantiateAnnotationManagers(mapView, viewModel, ::setCurrentMap).also {
            it.circleAnnotationManager.addDragListener(object: SimpleCircleAnnotationDragListener() {
                override fun onAnnotationDrag(annotation: Annotation<*>) {
                    viewModel.onDrag(annotation as CircleAnnotation, it)
                }

                override fun onAnnotationDragStarted(annotation: Annotation<*>) {
                    viewModel.onDragStarted(annotation as CircleAnnotation)
                }

                override fun onAnnotationDragFinished(annotation: Annotation<*>) {
                    viewModel.onDragEnd()
                }
            })
        }
    }
    val (showLoginDialog, setShowLoginDialog) = remember { mutableStateOf(false) }
    val (showLogoutDialog, setShowLogoutDialog) = remember { mutableStateOf(false) }
    val dialogTitles = listOf(stringResource(id = R.string.logout), stringResource(id = R.string.deletion))
    val dialogMessages = listOf(stringResource(id = R.string.logout_confirm),
        stringResource(id = R.string.delete_map_confirm),
        stringResource(id = R.string.delete_competition_confirm))
    fun collapseCardIfNeeded() {
        if (bottomSheetScaffoldState.bottomSheetState.isExpanded) {
            coroutineScope.launch {
                bottomSheetScaffoldState.bottomSheetState.collapse()
            }
        }
    }
    val dialogActions = listOf(
        { viewModel.logout() },
        {
            collapseCardIfNeeded()
            viewModel.deleteMap()
        }, { viewModel.deleteCompetition() }
    )
    val dialogTitle = remember{ mutableStateOf("") }
    val dialogMessage = remember{ mutableStateOf("") }
    val dialogAction= remember { mutableStateOf(dialogActions[0]) }
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val mapClickListener = remember {
        OnMapClickListener {
            if (uiState.isAddingArea) {
                val annotation = onMapClicked(annotationsManagers, context, it)
                if (annotation != null) {
                    viewModel.addPointAnnotation(annotation)
                }
            } else {
                collapseCardIfNeeded()
            }
            true
        }
    }
    val isMovedToMapsBorders = remember { mutableStateOf(false) }
    val focusRequester = remember{ FocusRequester() }
    val keyboardHelper = LocalSoftwareKeyboardController.current
    LaunchedEffect(uiState.newMapAnnotations.getPoints().size) {
        val line = recreatePoly(annotationsManagers, viewModel.uiState.value.newMapAnnotations)
        viewModel.addPolyAnnotations(line)
        annotationsManagers.pointAnnotationManager.annotations
            .filter { !uiState.newMapAnnotations.getPoints().contains(it.point) }
            .forEach {
                annotationsManagers.pointAnnotationManager.delete(it)
            }
        annotationsManagers.circleAnnotationManager.annotations
            .filter { !uiState.newMapAnnotations.getPoints().contains(it.point) }
            .forEach {
                annotationsManagers.circleAnnotationManager.delete(it)
            }
    }
    LaunchedEffect(uiState.maps) {
        recreateMapsPoly(annotationsManagers, viewModel)
        val bounds = uiState.getBounds()
        if (uiState.maps.isNotEmpty() && !isMovedToMapsBorders.value && bounds != null) {
            val opts = mapboxMap.value?.cameraForCoordinateBounds(
                bounds,
                padding = EdgeInsets(20.0, 20.0, 20.0, 20.0))
            if (opts != null && mapboxMap.value != null) {
                mapboxMap.value?.flyTo(opts)
                isMovedToMapsBorders.value = true
            }
        }
        if (uiState.initialId != null) {
            uiState.maps.find { it.id == uiState.initialId }?.let {
                setCurrentMap(it)
            }
        }
    }
    LaunchedEffect(uiState.settings) {
        recreateMapsPoly(annotationsManagers, viewModel)
    }
    LaunchedEffect(mapboxMap.value) {
        if (mapboxMap.value != null) {
            mapboxMap.value?.addOnMapClickListener(mapClickListener)
        }
    }
    LaunchedEffect(permissionState.status) {
        if (permissionState.status.isGranted) {
            enableLocationPuck(mapView)
        }
    }
    LaunchedEffect(Unit) {
        viewModel.getMaps()
    }
    MapInfoScreen(mapInfo = uiState.currentMapInfo,
        bottomSheetScaffoldState = bottomSheetScaffoldState,
        onCompetitionDeleteClicked = {
            dialogAction.value = dialogActions[2]
            dialogMessage.value = dialogMessages[2]
            dialogTitle.value = dialogTitles[1]
            viewModel.setCurrentId(it.id)
            setShowLogoutDialog(true)
        }, onMapDeleteClicked = {
            dialogAction.value = dialogActions[1]
            dialogMessage.value = dialogMessages[1]
            dialogTitle.value = dialogTitles[1]
            viewModel.setCurrentId(uiState.currentMapInfo?.id ?: 0)
            setShowLogoutDialog(true)
        }, onNewCompetition = { date, name ->
            viewModel.addCompetition(date, name)
        }, onMapEditClicked = {
            coroutineScope.launch {
                bottomSheetScaffoldState.bottomSheetState.collapse()
            }
            val annotations = uiState.currentMapInfo?.points?.mapNotNull {
                onMapClicked(annotationsManagers,
                    context,
                    it.getPoint())
            } ?: emptyList()
            val line = recreatePoly(annotationsManagers, NewMapAnnotations(annotations))
            viewModel.startMapEdit(NewMapAnnotations(annotations, line))
            uiState.currentMapInfo?.let { mapInfo ->
                val annotationIds = uiState.mapIdToAnnotationId.find { it.mapId == mapInfo.id }
                if (annotationIds != null) {
                    annotationsManagers.polygonAnnotationManager.annotations.find {
                        it.id == annotationIds.polygonId
                    }?.let {
                        annotationsManagers.polygonAnnotationManager.delete(it)
                    }
                    annotationsManagers.polylineAnnotationManager.annotations.find {
                        it.id == annotationIds.polylineId
                    }?.let {
                        annotationsManagers.polylineAnnotationManager.delete(it)
                    }
                }
            }
        }, onUploadMap = {
            viewModel.uploadMap(it)
        }, onImageClicked = {
            viewModel.selectImage(it)
        }) {
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(it)) {
            MapViewContainer(mapView, permissionState, isLocationClicked) { map ->
                mapboxMap.value = map
                map?.loadStyleUri(Style.OUTDOORS)
                mapView.compass.position = Gravity.BOTTOM or Gravity.END
            }
            MapButtons(
                map = mapView,
                onLocation = {
                    if (permissionState.status.isGranted) {
                        isLocationClicked.value = true
                    } else {
                        permissionHelper.checkPermissions {
                            enableLocationPuck(mapView)
                            isLocationClicked.value = true
                        }
                    }
                }
            )
            Box(modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 12.dp)) {
                ActionButton(iconRes = if (uiState.isAddingArea || uiState.isSearch) R.drawable.close_40px
                    else R.drawable.more_vert_40px,
                    modifier = Modifier.align(Alignment.TopEnd),
                    onClick = {
                        when {
                            uiState.isAddingArea -> {
                                viewModel.toggleAddingArea()
                            }
                            uiState.isSearch -> {
                                viewModel.toggleSearch()
                            }
                            else -> {
                                menuExpanded.value = !menuExpanded.value
                                collapseCardIfNeeded()
                            }
                        }
                    }
                )
                Menu(isExpanded = menuExpanded,
                    viewModel = viewModel,
                    onAddShape = {
                        viewModel.toggleAddingArea()
                    },
                    onSettings = onSettings,
                    onLogIn = {
                        if (viewModel.isAuthorized()) {
                            dialogAction.value = dialogActions[0]
                            dialogMessage.value = dialogMessages[0]
                            dialogTitle.value = dialogTitles[0]
                            setShowLogoutDialog(true)
                        } else {
                            setShowLoginDialog(true)
                        }
                    }, onSearch = {
                        viewModel.toggleSearch()
                    })
            }
            if (uiState.isAddingArea) {
                SaveAreaView(nameText = uiState.newMapName,
                    onNameChanged = {
                        viewModel.changeNewMapName(it)
                    },
                    onSave = {
                        viewModel.addMap()
                        viewModel.toggleAddingArea()
                    },
                    onClose = {
                        if (viewModel.isEdit()) {
                            uiState.currentMapInfo?.let {
                                val id = createPoly(annotationsManagers, viewModel, it)
                                viewModel.addMapIdToAnnotationId(id)
                            }
                        }
                        viewModel.toggleAddingArea()
                    },
                    canSave = uiState.newMapAnnotations.getPoints().size > 3 && uiState.newMapName.isNotEmpty(),
                    needExplanation = uiState.newMapAnnotations.getPoints().isEmpty())
            } else if (uiState.isSearch) {
                Column(Modifier.fillMaxWidth()) {
                    SearchView(Modifier
                        .fillMaxWidth()
                        .padding(end = 56.dp),
                        focusRequester,
                        searchString = uiState.searchString,
                        onSearchChanged = {
                            viewModel.changeSearchString(it)
                        }
                    )
                    val maps = uiState.filterMapsBySearchString()
                    SearchItems(maps) {
                        setCurrentMap(it)
                    }
                    LaunchedEffect(Unit) {
                        awaitFrame()
                        focusRequester.requestFocus()
                        awaitFrame()
                        awaitFrame()
                        keyboardHelper?.show()
                    }
                }
            }
            if (isLoading && !uiState.isSearch) {
                CircularProgressIndicator(Modifier
                    .padding(top = 12.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.6f))
                    .padding(9.dp)
                    .size(30.dp)
                    .align(Alignment.TopCenter),
                    strokeWidth = 3.dp)
            }
            LoginDialog(showLoginDialog, setShowLoginDialog, onSaveClicked = { login, password ->
                viewModel.login(login, password, onSuccess = {
                    Toast.makeText(context,
                        context.getString(R.string.welcome_template, it),
                        Toast.LENGTH_LONG).show()
                })
            })
            SimpleAlertDialog(showLogoutDialog, setShowLogoutDialog,
                dialogTitle.value,
                dialogMessage.value,
                positiveClickListener = dialogAction.value)
        }
    }
    uiState.selectedImage?.let {
        PictureScreen(picture = it) {
            viewModel.selectImage(null)
        }
    }
}

private fun enableLocationPuck(mapView: MapView) {
    mapView.location.updateSettings {
        this.enabled = true
    }
}

private fun onMapClicked(annotationManagers: AnnotationManagersBundle,
                         context: Context,
                         point: Point): NewMapPointAnnotation? {
    val pin = addAnnotationToMap(context, annotationManagers.pointAnnotationManager, point)
    val circle = addCircleToMap(annotationManagers.circleAnnotationManager, point)
    if (pin != null) {
        return NewMapPointAnnotation(pin, circle)
    }
    return null
}

private fun recreatePoly(annotationManagers: AnnotationManagersBundle,
                         newMapAnnotations: NewMapAnnotations): PolylineAnnotation {
    newMapAnnotations.polylineAnnotation?.let {
        annotationManagers.polylineAnnotationManager.delete(it)
    }
    val points = newMapAnnotations.getPoints()
    val options = PolylineAnnotationOptions()
        .withPoints(points.circuitPoints())
        .withLineColor(android.graphics.Color.RED)
        .withLineWidth(3.0)
    return annotationManagers.polylineAnnotationManager.create(options)
}

private fun recreateMapsPoly(annotationManagers: AnnotationManagersBundle,
                             viewModel: MainViewModel) {
    annotationManagers.polygonAnnotationManager.annotations.forEach {
        annotationManagers.polygonAnnotationManager.delete(it)
    }
    annotationManagers.polylineAnnotationManager.annotations.forEach {
        if (it != viewModel.uiState.value.newMapAnnotations.polylineAnnotation) {
            annotationManagers.polylineAnnotationManager.delete(it)
        }
    }
    val idList = arrayListOf<MapAnnotationsId>()
    viewModel.uiState.value.filteredMaps().forEach {
        idList.add(createPoly(annotationManagers, viewModel, it))
    }
    viewModel.setMapIdToAnnotationId(idList)
}

private fun createPoly(annotationManagers: AnnotationManagersBundle,
                       viewModel: MainViewModel,
                       it: MapInfo): MapAnnotationsId {
    val color = viewModel.calculateMapColor(it)
    val polyOptions = PolygonAnnotationOptions()
        .withPoints(listOf(it.points.sortedBy { it.position }
            .map { it.getPoint() }
            .circuitPoints()))
        .withFillColor(color)
        .withFillOpacity(0.5)
    val gon = annotationManagers.polygonAnnotationManager.create(polyOptions)
    val lineOptions = PolylineAnnotationOptions()
        .withPoints(it.points.sortedBy { it.position }
            .map { it.getPoint() }
            .circuitPoints())
        .withLineWidth(2.0)
        .withLineColor(BorderPurple.toArgb())
    val line = annotationManagers.polylineAnnotationManager.create(lineOptions)
    return MapAnnotationsId(it.id, gon.id, line.id)
}

private fun instantiateAnnotationManagers(mapView: MapView,
                                          viewModel: MainViewModel,
                                          setCurrentMap: (MapInfo) -> Unit): AnnotationManagersBundle {
    return AnnotationManagersBundle(
        polygonAnnotationManager = mapView.annotations.createPolygonAnnotationManager(
            AnnotationConfig(layerId = "4", belowLayerId = "3")).also {
            it.addClickListener(OnPolygonAnnotationClickListener { annotation ->
                val mapAnnotationsId = viewModel.uiState.value.mapIdToAnnotationId.find {
                    it.polygonId == annotation.id
                }
                viewModel.uiState.value.maps.find {
                    it.id == mapAnnotationsId?.mapId
                }?.let {
                    setCurrentMap(it)
                }
                true
            })
        },
        polylineAnnotationManager = mapView.annotations.createPolylineAnnotationManager(
            AnnotationConfig(layerId = "3", belowLayerId = "2")),
        circleAnnotationManager = mapView.annotations.createCircleAnnotationManager(
            AnnotationConfig(layerId = "2", belowLayerId = "1")),
        pointAnnotationManager = mapView.annotations.createPointAnnotationManager(
            AnnotationConfig(layerId = "1")).also {
            it.addClickListener(OnPointAnnotationClickListener {
                viewModel.deletePoint(it.point)
                true
            })
        })
}

private fun addAnnotationToMap(context: Context,
                               pointAnnotationManager: PointAnnotationManager,
                               point: Point): PointAnnotation? {
    bitmapFromDrawableRes(context, R.drawable.pin_del)?.let {
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(it)
            .withIconAnchor(IconAnchor.BOTTOM)
            .withIconOffset(listOf(0.0, -8.0))
            .withIconSize(1.0)
        return pointAnnotationManager.create(pointAnnotationOptions)
    }
    return null
}

private fun addCircleToMap(circleAnnotationManager: CircleAnnotationManager,
                           point: Point): CircleAnnotation {
    val circleAnnotationOptions = CircleAnnotationOptions()
        .withPoint(point)
        .withCircleColor(android.graphics.Color.RED)
        .withCircleStrokeColor(android.graphics.Color.WHITE)
        .withCircleStrokeWidth(3.0)
        .withCircleRadius(7.0)
        .withDraggable(true)
    return circleAnnotationManager.create(circleAnnotationOptions)
}

@Composable
fun ActionButton(iconRes: Int,
                 onClick: () -> Unit,
                 modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(vertical = 12.dp)
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.background.copy(0.6f))
            .clickable { onClick() }) {
        Icon(painter = painterResource(id = iconRes), contentDescription = null,
            Modifier.align(Alignment.Center),
            tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun OutlineButton(modifier: Modifier,
                  @DrawableRes drawableRes: Int,
                  text: String,
                  onClick: () -> Unit) {
    Row(modifier
        .padding(top = 8.dp)
        .clip(RoundedCornerShape(40.dp))
        .clickable { onClick() }
        .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(40.dp))
        .padding(12.dp, 6.dp),
        verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.material.Icon(painter = painterResource(id = drawableRes),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(10.dp))
        Text(text = text,
            style = MaterialTheme.typography.bodyMedium.copy(MaterialTheme.colorScheme.primary))
    }
}