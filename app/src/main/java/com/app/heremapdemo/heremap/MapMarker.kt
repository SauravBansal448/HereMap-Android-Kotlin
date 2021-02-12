package com.app.heremapdemo.heremap

import android.app.AlertDialog
import android.content.Context
import android.widget.Toast
import com.app.heremapdemo.R
import com.here.sdk.core.Anchor2D
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.core.Metadata
import com.here.sdk.core.Point2D
import com.here.sdk.gestures.TapListener
import com.here.sdk.mapview.*
import com.here.sdk.mapview.MapMarker
import java.util.*

class MapMarker(private val context: Context, private val mapView: MapView) {
    private val mapMarkerList: MutableList<MapMarker> = ArrayList()
    fun showAnchoredMapMarkers() {
        for (i in 0..9) {
            val geoCoordinates = createRandomGeoCoordinatesAroundMapCenter()

            // Centered on location. Shown below the POI image to indicate the location.
            // The draw order is determined from what is first added to the map.
            addCircleMapMarker(geoCoordinates)

            // Anchored, pointing to location.
            addPOIMapMarker(geoCoordinates)
        }
    }

    fun showCenteredMapMarkers() {
        val geoCoordinates = createRandomGeoCoordinatesAroundMapCenter()

        // Centered on location.
        //  addPhotoMapMarker(geoCoordinates)

        // Centered on location. Shown above the photo marker to indicate the location.
        // The draw order is determined from what is first added to the map.
        addCircleMapMarker(geoCoordinates)
    }

    fun clearMap() {
        for (mapMarker in mapMarkerList) {
            mapView.mapScene.removeMapMarker(mapMarker)
        }
        mapMarkerList.clear()
    }

    private fun createRandomGeoCoordinatesAroundMapCenter(): GeoCoordinates {
        val centerGeoCoordinates = mapView.viewToGeoCoordinates(
            Point2D((mapView.width / 2).toDouble(), (mapView.height / 2).toDouble())
        )
            ?: // Should never happen for center coordinates.
            throw RuntimeException("CenterGeoCoordinates are null")
        val lat = centerGeoCoordinates.latitude
        val lon = centerGeoCoordinates.longitude
        return GeoCoordinates(lat, lon)
        /*   return GeoCoordinates(
               getRandom(lat - 0.02, lat + 0.02),
               getRandom(lon - 0.02, lon + 0.02)
           )*/
    }

    private fun getRandom(min: Double, max: Double): Double {
        return min + Math.random() * (max - min)
    }

    private fun setTapGestureHandler() {
        mapView.gestures.tapListener =
            TapListener { touchPoint: Point2D -> pickMapMarker(touchPoint) }
    }

    private fun pickMapMarker(touchPoint: Point2D) {
        val radiusInPixel = 2f
        mapView.pickMapItems(
            touchPoint,
            radiusInPixel.toDouble()
        ) { pickMapItemsResult: PickMapItemsResult? ->
            val mapMarkerList = pickMapItemsResult!!.markers
            if (mapMarkerList.size == 0) {
                return@pickMapItems
            }
            val topmostMapMarker = mapMarkerList[0]
            val metadata = topmostMapMarker.metadata
            if (metadata != null) {
                var message = "No message found."
                val string = metadata.getString("key_poi")
                if (string != null) {
                    message = string
                }
                showDialog("Map Marker picked", message)
                return@pickMapItems
            }
            showDialog(
                "Map marker picked:", "Location: " +
                        topmostMapMarker.coordinates.latitude + ", " +
                        topmostMapMarker.coordinates.longitude
            )
        }
    }

    private fun addPOIMapMarker(geoCoordinates: GeoCoordinates) {
        val mapImage = MapImageFactory.fromResource(context.resources, R.drawable.poi)

        // The bottom, middle position should point to the location.
        // By default, the anchor point is set to 0.5, 0.5.
        val anchor2D = Anchor2D(0.5, 1.0)
        val mapMarker = MapMarker(geoCoordinates, mapImage, anchor2D)
        val metadata = Metadata()
        metadata.setString("key_poi", "This is a POI.")
        mapMarker.metadata = metadata
        mapView.mapScene.addMapMarker(mapMarker)
        mapMarkerList.add(mapMarker)
    }

    private fun addPhotoMapMarker(geoCoordinates: GeoCoordinates) {
        val mapImage = MapImageFactory.fromResource(context.resources, R.drawable.here_car)
        val mapMarker = MapMarker(geoCoordinates, mapImage)
        mapView.mapScene.addMapMarker(mapMarker)
        mapMarkerList.add(mapMarker)
    }

    private fun addCircleMapMarker(geoCoordinates: GeoCoordinates) {
        val mapImage = MapImageFactory.fromResource(context.resources, R.drawable.circle)
        val mapMarker = MapMarker(geoCoordinates, mapImage)
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
        camera.lookAt(GeoCoordinates(28.6432382, 77.3764952), distanceInMeters)

        // Setting a tap handler to pick markers from map.
        setTapGestureHandler()
        Toast.makeText(context, "You can tap markers.", Toast.LENGTH_LONG).show()
    }
}