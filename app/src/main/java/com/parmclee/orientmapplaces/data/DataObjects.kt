package com.parmclee.orientmapplaces.data

import com.mapbox.geojson.Point
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolygonAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import io.realm.RealmList
import io.realm.RealmObject
import io.realm.annotations.PrimaryKey
import java.util.Calendar
import java.util.Date


class AnnotationManagersBundle(val pointAnnotationManager: PointAnnotationManager,
                               val circleAnnotationManager: CircleAnnotationManager,
                               val polylineAnnotationManager: PolylineAnnotationManager,
                               val polygonAnnotationManager: PolygonAnnotationManager)

open class Competition(@PrimaryKey var id: Int = 0,
                       var date: Date = Date(),
                       var name: String = "",
                       var user: User? = null) : RealmObject()

open class MapInfo(@PrimaryKey var id: Int = 0,
                   var name: String = "",
                   var points: RealmList<LatLng> = RealmList(),
                   var competitions: RealmList<Competition> = RealmList(),
                   var user: User? = null,
                   var image: String? = null) : RealmObject() {

    fun copy(id: Int = this.id,
             name: String = this.name,
             points: RealmList<LatLng> = this.points,
             competitions: RealmList<Competition> = this.competitions,
             user: User? = this.user) = MapInfo(id, name, points, competitions, user)

    private fun getEndOfDayTime(): Date {
        val endOfDay = Calendar.getInstance()
        endOfDay.set(Calendar.HOUR_OF_DAY, 23)
        endOfDay.set(Calendar.MINUTE, 59)
        return endOfDay.time
    }

    private fun getYearAgoDate(): Date {
        val cal = Calendar.getInstance()
        cal.add(Calendar.YEAR, -1)
        return cal.time
    }

    fun getLastCompetitionDate(): Date? {
        val filtered = competitions.filter { it.date.time < getEndOfDayTime().time }
        if (filtered.isNotEmpty()) {
            return filtered.maxByOrNull { it.date }?.date
        }
        return null
    }

    fun getNextCompetitionDate(): Date? {
        val filtered = competitions.filter { it.date.time > getEndOfDayTime().time }
        if (filtered.isNotEmpty()) {
            return filtered.minByOrNull { it.date }?.date
        }
        return null
    }

    fun getPreviousCompetitions(): List<Competition> {
        val today = getEndOfDayTime()
        return competitions.filter { it.date.before(today) }.sortedByDescending { it.date }
    }

    fun getPreviousYearCompetitions(): List<Competition> {
        val today = getEndOfDayTime()
        return competitions.filter {
            it.date.before(today) && it.date.after(getYearAgoDate())
        }.sortedByDescending { it.date }
    }

    fun getFutureCompetitions(): List<Competition> {
        val today = getEndOfDayTime()
        return competitions.filter { it.date.after(today) }.sortedByDescending { it.date }
    }

    fun getBounds() : CoordinateBounds? {
        val points = ArrayList(points)
        if (points.isEmpty()) return null
        var bounds = CoordinateBounds.hull(points[0].getPoint(), points[1].getPoint())
        (2 until points.size).forEach {
            bounds = bounds.extend(points[it].getPoint())
        }
        return bounds
    }
}

open class User(@PrimaryKey var id: Int = 0,
                var firstName: String = "",
                var lastName: String = "",
                var login: String? = null,
                var password: String? = null,
                var premium: Int = 0) : RealmObject() {

    fun getShortName(): String {
        return "${firstName[0]}. $lastName"
    }
}

open class BaseResponse(
    val message: String? = null
)

class AddCompetitionData(val name: String,
                         val mapId: Int,
                         val compDate: String)

open class LatLng(var lat: Double = 0.0,
                  var lng: Double = 0.0,
                  var position: Int = 0) : RealmObject() {

    fun getPoint(): Point = Point.fromLngLat(lng, lat)
}

class AddMapData(val id: Int? = null,
                 val name: String,
                 val points: List<LatLng>) {

    companion object {
        fun create(id: Int? = null, name: String, points: List<Point>): AddMapData {
            return AddMapData(id, name, points.mapIndexed { idx, it ->
                LatLng(it.latitude(), it.longitude(), idx)
            })
        }
    }
}

class MapAnnotationsId(val mapId: Int,
                       val polygonId: Long,
                       val polylineId: Long)
