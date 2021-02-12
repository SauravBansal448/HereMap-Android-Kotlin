package com.app.heremapdemo.heremap

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import com.app.heremapdemo.R
import com.here.sdk.core.Color
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.core.GeoPolyline
import com.here.sdk.core.Point2D
import com.here.sdk.core.errors.InstantiationErrorException
import com.here.sdk.mapview.*
import com.here.sdk.mapview.MapMarker
import com.here.sdk.routing.*
import java.util.*

class Routing(private val context: Context, private val mapView: MapView) {
    private val mapMarkerList: MutableList<MapMarker> = ArrayList()
    private val mapPolylines: MutableList<MapPolyline> = ArrayList()
    private var routingEngine: RoutingEngine? = null
    private var startGeoCoordinates: GeoCoordinates? = null
    private var destinationGeoCoordinates: GeoCoordinates? = null

    fun addRoute() {
        clearMap()
            startGeoCoordinates = GeoCoordinates(28.6156042, 77.3723252)
            destinationGeoCoordinates = GeoCoordinates(28.616761, 77.3863656)
            val startWayPoint = Waypoint(startGeoCoordinates!!)
            val destinationWayPoint = Waypoint(destinationGeoCoordinates!!)
      /*  startGeoCoordinates = createRandomGeoCoordinatesAroundMapCenter()
        destinationGeoCoordinates = createRandomGeoCoordinatesAroundMapCenter()
        val startWayPoint = Waypoint(startGeoCoordinates!!)
        val destinationWayPoint = Waypoint(destinationGeoCoordinates!!)*/

        val wayPoints: List<Waypoint> = ArrayList(listOf(startWayPoint, destinationWayPoint))
        routingEngine!!.calculateRoute(
            wayPoints,
            CarOptions()
        ) { routingError, routes ->
            if (routingError == null) {
                val route = routes!![0]
                showRouteDetails(route)
                showRouteOnMap(route)
                logRouteViolations(route)
            } else {
                showDialog("Error while calculating a route:", routingError.toString())
            }
        }
    }

    // A route may contain several warnings, for example, when a certain route option could not be fulfilled.
    // An implementation may decide to reject a route if one or more violations are detected.
    private fun logRouteViolations(route: Route) {
        for (section in route.sections) {
            for (notice in section.notices) {
                Log.e(
                    this@Routing.toString(),
                    "This route contains the following warning: " + notice.code.toString()
                )
            }
        }
    }

    private fun showRouteDetails(route: Route) {
        val estimatedTravelTimeInSeconds = route.durationInSeconds.toLong()
        val lengthInMeters = route.lengthInMeters
        val routeDetails = ("Travel Time: " + formatTime(estimatedTravelTimeInSeconds)
                + ", Length: " + formatLength(lengthInMeters))
        showDialog("Route Details", routeDetails)
    }

    private fun formatTime(sec: Long): String {
        val hours = (sec / 3600).toInt()
        val minutes = (sec % 3600 / 60).toInt()
        return String.format(Locale.getDefault(), "%02d:%02d", hours, minutes)
    }

    private fun formatLength(meters: Int): String {
        val kilometers = meters / 1000
        val remainingMeters = meters % 1000
        return String.format(Locale.getDefault(), "%02d.%02d km", kilometers, remainingMeters)
    }

    private fun showRouteOnMap(route: Route) {
        // Show route as polyline.
        val routeGeoPolyline: GeoPolyline
        routeGeoPolyline = try {
            GeoPolyline(route.polyline)
        } catch (e: InstantiationErrorException) {
            // It should never happen that a route polyline contains less than two vertices.
            return
        }
        val widthInPixels = 10f
        val routeMapPolyline = MapPolyline(
            routeGeoPolyline,
            widthInPixels.toDouble(),
            Color.valueOf(0f, 0.56f, 0.54f, 0.63f)
        ) // RGBA
        mapView.mapScene.addMapPolyline(routeMapPolyline)
        mapPolylines.add(routeMapPolyline)

        // Draw a circle to indicate starting point and destination.
        addCircleMapMarker(startGeoCoordinates, R.drawable.green_dot)
        addCircleMapMarker(destinationGeoCoordinates, R.drawable.green_dot)

        // Log maneuver instructions per route section.
        val sections = route.sections
        for (section in sections) {
            logManeuverInstructions(section)
        }
    }

    private fun logManeuverInstructions(section: Section) {
        Log.d(this@Routing.toString(), "Log maneuver instructions per route section:")
        val maneuverInstructions = section.maneuvers
        for (maneuverInstruction in maneuverInstructions) {
            val maneuverAction = maneuverInstruction.action
            val maneuverLocation = maneuverInstruction.coordinates
            val maneuverInfo = (maneuverInstruction.text
                    + ", Action: " + maneuverAction.name
                    + ", Location: " + maneuverLocation.toString())
            Log.d(this@Routing.toString(), maneuverInfo)
        }
    }

    fun addWayPoints() {
        if (startGeoCoordinates == null || destinationGeoCoordinates == null) {
            showDialog("Error", "Please add a route first.")
            return
        }
        clearWayPointMapMarker()
        clearRoute()
//        val wayPoint1 = Waypoint(createRandomGeoCoordinatesAroundMapCenter())
//        val wayPoint2 = Waypoint(createRandomGeoCoordinatesAroundMapCenter())
        val wayPoint1 = Waypoint(GeoCoordinates(28.6156042, 77.3723252))
        val wayPoint2 = Waypoint(GeoCoordinates(28.616761, 77.3863656))
        val wayPoints: List<Waypoint> = ArrayList(
            listOf(
                Waypoint(startGeoCoordinates!!),
                wayPoint1,
                wayPoint2,
                Waypoint(destinationGeoCoordinates!!)
            )
        )
        routingEngine!!.calculateRoute(
            wayPoints,
            CarOptions()
        ) { routingError, routes ->
            if (routingError == null) {
                val route = routes!![0]
                showRouteDetails(route)
                showRouteOnMap(route)
                logRouteViolations(route)

                // Draw a circle to indicate the location of the wayPoints.
                addCircleMapMarker(wayPoint1.coordinates, R.drawable.red_dot)
                addCircleMapMarker(wayPoint2.coordinates, R.drawable.red_dot)
            } else {
                showDialog("Error while calculating a route:", routingError.toString())
            }
        }
    }

    fun clearMap() {
        clearWayPointMapMarker()
        clearRoute()
    }

    private fun clearWayPointMapMarker() {
        for (mapMarker in mapMarkerList) {
            mapView.mapScene.removeMapMarker(mapMarker)
        }
        mapMarkerList.clear()
    }

    private fun clearRoute() {
        for (mapPolyline in mapPolylines) {
            mapView.mapScene.removeMapPolyline(mapPolyline)
        }
        mapPolylines.clear()
    }

    private fun createRandomGeoCoordinatesAroundMapCenter(): GeoCoordinates {
        val centerGeoCoordinates = mapView.viewToGeoCoordinates(
            Point2D((mapView.width / 2).toDouble(), (mapView.height / 2).toDouble())
        )
            ?: // Should never happen for center coordinates.
            throw RuntimeException("CenterGeoCoordinates are null")
        val lat = centerGeoCoordinates.latitude
        val lon = centerGeoCoordinates.longitude

        println("MMMMMMMMM: Lat: $lat")
        println("MMMMMMMMM: Lalont: $lon")
        /*return GeoCoordinates(
            getRandom(lat - 0.02, lat + 0.02),
            getRandom(lon - 0.02, lon + 0.02)
        )*/
        return GeoCoordinates(lat, lon)
    }

    private fun getRandom(min: Double, max: Double): Double {
        return min + Math.random() * (max - min)
    }

    private fun addCircleMapMarker(geoCoordinates: GeoCoordinates?, resourceId: Int) {
        val mapImage = MapImageFactory.fromResource(context.resources, resourceId)
        val mapMarker = MapMarker(geoCoordinates!!, mapImage)
        mapView.mapScene.addMapMarker(mapMarker)
        mapMarkerList.add(mapMarker)
    }

    private fun showDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(context)
        builder.setTitle(title)
        builder.setMessage(message)
        builder.show()
    }

    init {
        val camera = mapView.camera
        val distanceInMeters = (1000 * 10).toDouble()
        camera.lookAt(GeoCoordinates(28.6156042, 77.3723252), distanceInMeters)
        routingEngine = try {
            RoutingEngine()
        } catch (e: InstantiationErrorException) {
            throw RuntimeException("Initialization of RoutingEngine failed: " + e.error.name)
        }
    }
}