package com.app.heremapdemo.heremap

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.app.heremapdemo.R
import com.here.sdk.core.*
import com.here.sdk.core.errors.InstantiationErrorException
import com.here.sdk.gestures.GestureState
import com.here.sdk.gestures.LongPressListener
import com.here.sdk.gestures.TapListener
import com.here.sdk.mapview.*
import com.here.sdk.mapview.MapMarker
import com.here.sdk.mapview.MapViewBase.PickMapItemsCallback
import com.here.sdk.search.*
import java.util.*

class Search(private val context: Context, private val mapView: MapView) {
    private val camera: MapCamera = mapView.camera
    private val mapMarkerList: MutableList<MapMarker> = ArrayList()
    private val autosuggestCallback = SuggestCallback { searchError, list ->
        if (searchError != null) {
            Log.d(LOG_TAG, "Autosuggest Error: " + searchError.name)
            return@SuggestCallback
        }

        // If error is null, list is guaranteed to be not empty.
        Log.d(LOG_TAG, "Autosuggest results: " + list!!.size)
        for (autoSuggestResult in list) {
            var addressText = "Not a place."
            val place = autoSuggestResult.place
            if (place != null) {
                addressText = place.address.addressText
            }
            Log.d(
                LOG_TAG, "Autosuggest result: " + autoSuggestResult.title +
                        " addressText: " + addressText
            )
        }
    }
    private var searchEngine: SearchEngine? = null
    private val addressSearchCallback = SearchCallback { searchError, list ->
        if (searchError != null) {
            showDialog("Reverse geocoding", "Error: $searchError")
            return@SearchCallback
        }

        // If error is null, list is guaranteed to be not empty.
        showDialog("Reverse geocoded address:", list!![0].address.addressText)
    }
    private val querySearchCallback = SearchCallback { searchError, list ->
        if (searchError != null) {
            showDialog("Search", "Error: $searchError")
            return@SearchCallback
        }

        // If error is null, list is guaranteed to be not empty.
        showDialog("Search", "Results: " + list!!.size)

        // Add new marker for each search result on map.
        for (searchResult in list) {
            val metadata = Metadata()
            metadata.setCustomValue("key_search_result", SearchResultMetadata(searchResult))
            addPoiMapMarker(searchResult.coordinates, metadata)
        }
    }
    private val geocodeAddressSearchCallback = SearchCallback { searchError, list ->
        if (searchError != null) {
            showDialog("Geocoding", "Error: $searchError")
            return@SearchCallback
        }
        for (geoCodingResult in list!!) {
            val geoCoordinates = geoCodingResult.coordinates
            val address = geoCodingResult.address
            val locationDetails = (address.addressText
                    + ". GeoCoordinates: " + geoCoordinates.latitude
                    + ", " + geoCoordinates.longitude)
            Log.d(LOG_TAG, "GeocodingResult: $locationDetails")
            addPoiMapMarker(geoCoordinates)
        }
        showDialog("Geocoding result", "Size: " + list.size)
    }

    fun onSearchButtonClicked() {
        // Search for "Pizza" and show the results on the map.
        searchExample()

        // Search for auto suggestions and log the results to the console.
        //   autoSuggestExample()
    }

    fun onGeocodeButtonClicked() {
        // Search for the location that belongs to an address and show it on the map.
        geocodeAnAddress()
    }

    private fun searchExample() {
        val searchTerm = "Pizza"
        Toast.makeText(context, "Searching in viewport: $searchTerm", Toast.LENGTH_LONG).show()
        searchInViewport(searchTerm)
    }

    private fun geocodeAnAddress() {
        // Set map near to expected location.
        val geoCoordinates = GeoCoordinates(28.6156042, 77.3723252)
        camera.lookAt(geoCoordinates, (1000 * 7).toDouble())
        val queryString = "Mobcoder LLC"
        Toast.makeText(
            context,
            "Finding locations for: " + queryString
                    + ". Tap marker to see the coordinates. Check the logs for the address.",
            Toast.LENGTH_LONG
        ).show()
        geocodeAddressAtLocation(queryString, geoCoordinates)
    }

    private fun setTapGestureHandler() {
        mapView.gestures.tapListener =
            TapListener { touchPoint: Point2D -> pickMapMarker(touchPoint) }
    }

    private fun setLongPressGestureHandler() {
        mapView.gestures.longPressListener =
            LongPressListener { gestureState: GestureState, touchPoint: Point2D? ->
                if (gestureState == GestureState.BEGIN) {
                    val geoCoordinates =
                        mapView.viewToGeoCoordinates(touchPoint!!) ?: return@LongPressListener
                    addPoiMapMarker(geoCoordinates)
                    getAddressForCoordinates(geoCoordinates)
                }
            }
    }

    private fun getAddressForCoordinates(geoCoordinates: GeoCoordinates) {
        val maxItems = 1
        val reverseGeocodingOptions = SearchOptions(LanguageCode.EN_US, maxItems)
        searchEngine!!.search(geoCoordinates, reverseGeocodingOptions, addressSearchCallback)
    }

    private fun pickMapMarker(point2D: Point2D) {
        val radiusInPixel = 2f
        mapView.pickMapItems(
            point2D,
            radiusInPixel.toDouble(),
            PickMapItemsCallback { pickMapItemsResult ->
                if (pickMapItemsResult == null) {
                    return@PickMapItemsCallback
                }
                val mapMarkerList = pickMapItemsResult.markers
                if (mapMarkerList.size == 0) {
                    return@PickMapItemsCallback
                }
                val topmostMapMarker = mapMarkerList[0]
                val metadata = topmostMapMarker.metadata
                if (metadata != null) {
                    val customMetadataValue = metadata.getCustomValue("key_search_result")
                    if (customMetadataValue != null) {
                        val searchResultMetadata = customMetadataValue as SearchResultMetadata
                        val title = searchResultMetadata.searchResult.title
                        val vicinity = searchResultMetadata.searchResult.address.addressText
                        showDialog("Picked Search Result", "$title. Vicinity: $vicinity")
                        return@PickMapItemsCallback
                    }
                }
                showDialog(
                    "Picked Map Marker",
                    "Geographic coordinates: " +
                            topmostMapMarker.coordinates.latitude + ", " +
                            topmostMapMarker.coordinates.longitude
                )
            })
    }

    private fun searchInViewport(queryString: String) {
        clearMap()
        val viewportGeoBox = mapViewGeoBox
        val query = TextQuery(queryString, viewportGeoBox)
        val maxItems = 3
        val searchOptions = SearchOptions(LanguageCode.EN_US, maxItems)
        searchEngine!!.search(query, searchOptions, querySearchCallback)
    }

    private fun autoSuggestExample() {
        val centerGeoCoordinates = mapViewCenter
        val maxItems = 3
        val searchOptions = SearchOptions(LanguageCode.EN_US, maxItems)

        // Simulate a user typing a search term.
        searchEngine!!.suggest(
            TextQuery(
                "p",  // User typed "p".
                centerGeoCoordinates
            ),
            searchOptions,
            autosuggestCallback
        )
        searchEngine!!.suggest(
            TextQuery(
                "pi",  // User typed "pi".
                centerGeoCoordinates
            ),
            searchOptions,
            autosuggestCallback
        )
        searchEngine!!.suggest(
            TextQuery(
                "piz",  // User typed "piz".
                centerGeoCoordinates
            ),
            searchOptions,
            autosuggestCallback
        )
    }

    private fun geocodeAddressAtLocation(queryString: String, geoCoordinates: GeoCoordinates) {
        clearMap()
        val query = AddressQuery(queryString, geoCoordinates)
        val maxItems = 2
        val options = SearchOptions(LanguageCode.EN_US, maxItems)
        searchEngine!!.search(query, options, geocodeAddressSearchCallback)
    }

    private fun addPoiMapMarker(geoCoordinates: GeoCoordinates) {
        val mapMarker = createPoiMapMarker(geoCoordinates)
        mapView.mapScene.addMapMarker(mapMarker)
        mapMarkerList.add(mapMarker)
    }

    private fun addPoiMapMarker(geoCoordinates: GeoCoordinates, metadata: Metadata) {
        val mapMarker = createPoiMapMarker(geoCoordinates)
        mapMarker.metadata = metadata
        mapView.mapScene.addMapMarker(mapMarker)
        mapMarkerList.add(mapMarker)
    }

    private fun createPoiMapMarker(geoCoordinates: GeoCoordinates): MapMarker {
        val mapImage = MapImageFactory.fromResource(context.resources, R.drawable.poi)
        return MapMarker(geoCoordinates, mapImage, Anchor2D(0.5, 1.0))
    }

    // Should never happen for center coordinates.
    private val mapViewCenter: GeoCoordinates
        get() = mapView.viewToGeoCoordinates(
            Point2D(
                (mapView.width / 2).toDouble(), (mapView.height / 2).toDouble()
            )
        )
            ?: // Should never happen for center coordinates.
            throw RuntimeException("CenterGeoCoordinates are null")

    // Note: This algorithm assumes an unrotated map view.
    private val mapViewGeoBox: GeoBox
        get() {
            val mapViewWidthInPixels = mapView.width
            val mapViewHeightInPixels = mapView.height
            val bottomLeftPoint2D = Point2D(0.0, mapViewHeightInPixels.toDouble())
            val topRightPoint2D = Point2D(
                mapViewWidthInPixels.toDouble(), 0.0
            )
            val southWestCorner = mapView.viewToGeoCoordinates(bottomLeftPoint2D)
            val northEastCorner = mapView.viewToGeoCoordinates(topRightPoint2D)
            if (southWestCorner == null || northEastCorner == null) {
                throw RuntimeException("GeoBox creation failed, corners are null.")
            }

            // Note: This algorithm assumes an unrotated map view.
            return GeoBox(southWestCorner, northEastCorner)
        }

    private fun clearMap() {
        for (mapMarker in mapMarkerList) {
            mapView.mapScene.removeMapMarker(mapMarker)
        }
        mapMarkerList.clear()
    }

    private fun showDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(
            context
        )
        builder.setTitle(title)
        builder.setMessage(message)
        builder.show()
    }

    private class SearchResultMetadata(val searchResult: Place) : CustomMetadataValue {
        override fun getTag(): String {
            return "SearchResult Metadata"
        }
    }

    companion object {
        private val LOG_TAG = Search::class.java.name
    }

    init {
        val distanceInMeters = (1000 * 10).toDouble()
        camera.lookAt(GeoCoordinates(28.6156042, 77.3723252), distanceInMeters)
        searchEngine = try {
            SearchEngine()
        } catch (e: InstantiationErrorException) {
            throw RuntimeException("Initialization of SearchEngine failed: " + e.error.name)
        }
        setTapGestureHandler()
      //  setLongPressGestureHandler()
        Toast.makeText(
            context,
            "Long press on map to get the address for that position using reverse geocoding.",
            Toast.LENGTH_LONG
        ).show()
    }
}