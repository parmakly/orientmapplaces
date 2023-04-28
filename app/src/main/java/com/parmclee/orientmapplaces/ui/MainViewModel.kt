package com.parmclee.orientmapplaces.ui

import androidx.lifecycle.ViewModel
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotation
import com.mapbox.turf.TurfMeasurement
import com.parmclee.orientmapplaces.data.*
import io.realm.RealmList
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Date


class MainViewModel: ViewModel() {
    private val repository = Repository()
    val uiState = MutableStateFlow(MainUiState())
    val errorFlow = repository.errorFlow
    val isLoading = repository.isLoading
    private var mCurrentId = -1

    fun isSettingsChanged(): Boolean {
        return !uiState.value.settings.isDefault()
    }

    fun isAuthorized(): Boolean {
        return Preferences.getLogin() != null
    }

    fun toggleAddingArea() {
        mCurrentId = -1
        uiState.value = uiState.value.copy(
            isAddingArea = !uiState.value.isAddingArea,
            newMapAnnotations = uiState.value.newMapAnnotations.copy(
                pointsAnnotations = emptyList()),
            currentMapInfo = null
        )
    }

    fun toggleSearch() {
        uiState.value = uiState.value.copy(
            isSearch = !uiState.value.isSearch,
            searchString = if (uiState.value.isSearch) "" else uiState.value.searchString)
    }

    fun changeSearchString(new: String) {
        uiState.value = uiState.value.copy(searchString = new)
    }

    fun changeInitialId(id: Int?) {
        uiState.value = uiState.value.copy(initialId = id)
    }

    fun isEdit() = mCurrentId != -1

    fun addPointAnnotation(point: NewMapPointAnnotation) {
        val list = uiState.value.newMapAnnotations.pointsAnnotations.toMutableList()
        addClosestPoint(point, list)
        val newSet = uiState.value.newMapAnnotations.copy(pointsAnnotations = list)
        uiState.value = uiState.value.copy(newMapAnnotations = newSet)
    }

    private fun addClosestPoint(point: NewMapPointAnnotation, list: MutableList<NewMapPointAnnotation>) {
        if (list.size < 4) list.add(point)
        else {
            val closest = list.minBy { TurfMeasurement.distance(it.circleAnnotation.point, point.circleAnnotation.point) }
            val closestIdx = list.indexOf(closest)
            val newList = ArrayList(list).apply { remove(closest) }
            val secondClosest = newList.minBy { TurfMeasurement.distance(it.circleAnnotation.point, point.circleAnnotation.point) }
            val secondIdx = list.indexOf(secondClosest)
            when {
                closestIdx == 0 && secondIdx == list.size - 1
                        || secondIdx == 0 && closestIdx == list.size - 1 -> list.add(point)
                closestIdx < secondIdx -> list.add(closestIdx + 1, point)
                else -> list.add(closestIdx, point)
            }
        }
    }

    fun deletePoint(point: Point) {
        val list = uiState.value.newMapAnnotations.pointsAnnotations.toMutableList()
        val annotation = list.find { it.circleAnnotation.point == point }
        annotation?.let { list.remove(it) }
        val newSet = uiState.value.newMapAnnotations.copy(pointsAnnotations = list)
        uiState.value = uiState.value.copy(newMapAnnotations = newSet)
    }

    fun changeNewMapName(name: String) {
        uiState.value = uiState.value.copy(newMapName = name)
    }

    fun addMap() {
        fun onAdded(mapInfo: MapInfo) {
            val maps = uiState.value.maps.toMutableList()
            maps.find { it.id == mapInfo.id }?.let {
                maps.remove(it)
            }
            maps.add(mapInfo)
            uiState.value = uiState.value.copy(newMapName = "", newMapAnnotations = NewMapAnnotations(),
                maps = maps, settings = uiState.value.settings.copy(filter = MapDateFilter.ALL))
            Preferences.saveFilter(MapDateFilter.ALL)
        }
        if (mCurrentId == -1) {
            repository.addMap(uiState.value.newMapName, uiState.value.newMapAnnotations.getPoints()) {
                onAdded(it)
            }
        } else {
            repository.updateMap(mCurrentId, uiState.value.newMapName,
                uiState.value.newMapAnnotations.getPoints()) {
                onAdded(it)
            }
        }
    }

    fun getMaps() {
        repository.getMaps {
            Preferences.saveMapSync()
            uiState.value = uiState.value.copy(maps = it,
                settings = uiState.value.settings.copy(syncDate = Date()))
        }
    }

    fun login(login: String, password: String, onSuccess: (String) -> Unit) {
        repository.getUser(login, password, onSuccess)
    }

    fun logout() {
        Preferences.logout()
    }

    fun clearError() {
        repository.errorFlow.value = null
    }

    fun addPolyAnnotations(line: PolylineAnnotation) {
        val newPoly = uiState.value.newMapAnnotations.copy(polylineAnnotation = line)
        uiState.value = uiState.value.copy(newMapAnnotations = newPoly)
    }

    fun calculateMapColor(map: MapInfo, filter: MapDateFilter = uiState.value.settings.filter): Int {
         val deltaSeconds = if (filter == MapDateFilter.FUTURE) {
            val nextDate = map.getNextCompetitionDate() ?: return android.graphics.Color.GREEN
            nextDate.time / 1000 - System.currentTimeMillis() / 1000
        } else {
            val lastDate = map.getLastCompetitionDate() ?: return android.graphics.Color.GREEN
            System.currentTimeMillis() / 1000 - lastDate.time / 1000
        }
        val relation = deltaSeconds.toFloat() / (365 * 24 * 60 * 60)
        if (relation > 1f) return android.graphics.Color.GREEN
        return if (relation >= 0.5f) {
            android.graphics.Color.argb(255, ((2 * (1 - relation)) * 255).toInt(), 255, 0)
        } else {
            android.graphics.Color.argb(255, 255, (2 * relation * 255).toInt(), 0)
        }
    }

    fun selectMap(mapInfo: MapInfo) {
        uiState.value = uiState.value.copy(currentMapInfo = mapInfo,
            isSearch = false,
            searchString = "",
            initialId = null)
        val id = mapInfo.id
        repository.getMap(id) { actualInfo ->
            val newMaps = uiState.value.maps.toMutableList()
            newMaps.find { it.id == id }?.let {
                newMaps.remove(it)
                newMaps.add(actualInfo)
            }
            uiState.value = uiState.value.copy(currentMapInfo = actualInfo, maps = newMaps)
        }
    }

    fun setMapIdToAnnotationId(map: List<MapAnnotationsId>) {
        uiState.value = uiState.value.copy(mapIdToAnnotationId = map)
    }

    fun addMapIdToAnnotationId(mapAnnotationsId: MapAnnotationsId) {
        val map = uiState.value.mapIdToAnnotationId.toMutableList()
        map.find { it.mapId == mapAnnotationsId.mapId }?.let {
            map.remove(it)
        }
        map.add(mapAnnotationsId)
        uiState.value = uiState.value.copy(mapIdToAnnotationId = map)
    }

    fun addCompetition(date: Date, name: String) {
        uiState.value.currentMapInfo?.let { map ->
            val mapId = map.id
            repository.addCompetition(mapId, date, name) {
                val competitions = map.competitions.toMutableList()
                competitions.add(it)
                val newMap = map.copy(competitions = RealmList<Competition>().apply { addAll(competitions) })
                uiState.value = uiState.value.copy(currentMapInfo = newMap)
            }
        }
    }

    fun deleteMap() {
        repository.deleteMap(mCurrentId) {
            val maps = uiState.value.maps.toMutableList()
            maps.find { it.id == mCurrentId }?.let {
                maps.remove(it)
            }
            uiState.value = uiState.value.copy(currentMapInfo = null, maps = maps)
        }
    }

    fun deleteCompetition() {
        repository.deleteCompetition(mCurrentId) {
            val map = uiState.value.currentMapInfo
            val newCompetitions = map?.competitions?.toMutableList() ?: arrayListOf()
            newCompetitions.find { it.id == mCurrentId }?.let {
                newCompetitions.remove(it)
            }
            val newMap = map?.copy(competitions = RealmList<Competition>().apply { addAll(newCompetitions) })
            val maps = uiState.value.maps.toMutableList()
            maps.find { it.id == map?.id }?.let {
                newMap?.let {
                    maps.remove(it)
                    maps.add(it)
                }
            }
            uiState.value = uiState.value.copy(currentMapInfo = newMap, maps = maps)
        }
    }

    fun setCurrentId(id: Int) {
        mCurrentId = id
    }

    fun changeSettings(settings: Settings) {
        uiState.value = uiState.value.copy(settings = settings)
        Preferences.saveFilter(settings.filter)
    }

    fun changeCentering(checked: Boolean) {
        uiState.value = uiState.value.copy(settings = uiState.value.settings.copy(needCentering = checked))
        Preferences.saveCentering(checked)
    }

    fun onDragStarted(annotation: CircleAnnotation) {
        uiState.value.newMapAnnotations.pointsAnnotations.find { it.circleAnnotation == annotation }?.let {
            it.isDragged = true
        }
    }

    fun onDrag(annotation: CircleAnnotation, managersBundle: AnnotationManagersBundle) {
        val idx = uiState.value.newMapAnnotations.pointsAnnotations.indexOfFirst { it.isDragged }
        uiState.value.newMapAnnotations.pointsAnnotations[idx].let {
            it.pinAnnotation.point = annotation.point
            managersBundle.pointAnnotationManager.update(it.pinAnnotation)
            val line = uiState.value.newMapAnnotations.polylineAnnotation
            if (line != null) {
                val polylinePoints = line.points.toMutableList()
                polylinePoints.removeAt(idx)
                polylinePoints.add(idx, annotation.point)
                if (idx == 0) {
                    polylinePoints.removeAt(polylinePoints.size - 1)
                    polylinePoints.add(annotation.point)
                }
                line.points = polylinePoints
                line.geometry = LineString.fromLngLats(polylinePoints)
                managersBundle.polylineAnnotationManager.update(line)
            }
        }
    }

    fun onDragEnd() {
        uiState.value.newMapAnnotations.pointsAnnotations.find { it.isDragged }?.let {
            it.isDragged = false
        }
    }

    fun startMapEdit(annotations: NewMapAnnotations) {
        mCurrentId = uiState.value.currentMapInfo?.id ?: -1
        uiState.value = uiState.value.copy(
            isAddingArea = true,
            newMapName = uiState.value.currentMapInfo?.name ?: "",
            newMapAnnotations = annotations)
    }

    fun uploadMap(filePath: String) {
        repository.uploadMap(uiState.value.currentMapInfo?.id ?: 0, filePath) {
            uiState.value = uiState.value.copy(currentMapInfo = it)
        }
    }

    fun selectImage(image: String?) {
        uiState.value = uiState.value.copy(selectedImage = image)
    }
}

data class MainUiState(val isAddingArea: Boolean = false,
                       val isSearch: Boolean = false,
                       val newMapAnnotations: NewMapAnnotations = NewMapAnnotations(),
                       val newMapName: String = "",
                       val maps: List<MapInfo> = emptyList(),
                       val currentMapInfo: MapInfo? = null,
                       val mapIdToAnnotationId: List<MapAnnotationsId> = emptyList(),
                       val settings: Settings = Settings(),
                       val selectedImage: String? = null,
                       val searchString: String = "",
                       val initialId: Int? = null) {

    fun getBounds(): CoordinateBounds? {
        val filtered = filteredMaps()
        var filteredMaps = if (settings.needCentering) {
            filtered.filter { it.user?.id == Preferences.getUserId() }
        } else filtered
        if (filteredMaps.isEmpty() && filtered.isNotEmpty()) {
            filteredMaps = filtered
        }
        val points = filteredMaps.flatMap { it.points }
        if (points.isEmpty()) return null
        var bounds = CoordinateBounds.hull(points[0].getPoint(), points[1].getPoint())
        (2 until points.size).forEach {
            bounds = bounds.extend(points[it].getPoint())
        }
        return bounds
    }

    fun filteredMaps(): List<MapInfo> {
        return when (settings.filter) {
            MapDateFilter.ALL -> maps
            MapDateFilter.FUTURE -> maps.filter { it.getFutureCompetitions().isNotEmpty() }
            MapDateFilter.PAST -> maps.filter { it.getPreviousYearCompetitions().isNotEmpty() }
        }
    }

    fun filterMapsBySearchString(): List<MapInfo> {
        if (searchString.length < 2) return emptyList()
        return maps.filter { it.name.contains(searchString, true) }
    }
}

class NewMapPointAnnotation(val pinAnnotation: PointAnnotation,
                            val circleAnnotation: CircleAnnotation,
                            var isDragged: Boolean = false)

data class NewMapAnnotations(val pointsAnnotations: List<NewMapPointAnnotation> = emptyList(),
                             val polylineAnnotation: PolylineAnnotation? = null) {

    fun getPoints() = pointsAnnotations.map { it.circleAnnotation.point }
}

data class Settings(val filter: MapDateFilter = Preferences.getFilter() ?: MapDateFilter.ALL,
                    val syncDate: Date? = Preferences.getMapSync(),
                    val needCentering: Boolean = Preferences.getCentering()) {

    fun isDefault(): Boolean {
        return filter == MapDateFilter.ALL
    }

    fun getDateText() = syncDate?.let { userFriendlyTimeSdf.format(it) }
}