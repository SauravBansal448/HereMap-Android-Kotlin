package com.app.heremapdemo

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.app.heremapdemo.heremap.MapMarker
import com.app.heremapdemo.heremap.PermissionsRequester
import com.app.heremapdemo.heremap.Routing
import com.app.heremapdemo.heremap.Search
import com.here.sdk.mapview.MapScheme
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {

    private var permissionRequest: PermissionsRequester? = null
    private val TAG = MainActivity::class.java.simpleName
    private var mapMarkerExample: MapMarker? = null
    private var routingExample: Routing? = null
    private var searchExample: Search? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapView.onCreate(savedInstanceState)

        handleAndroidPermissions()
        initListeners()
    }

    private fun initListeners() {
        btnAnchored.setOnClickListener { anchoredMapMarkersButtonClicked() }
        btnCenter.setOnClickListener { centeredMapMarkersButtonClicked() }
        btnClear.setOnClickListener { clearMapButtonClicked() }
        btnAddRoute.setOnClickListener { addRouteBtnClicked() }
        btnAddWayPoints.setOnClickListener { addWaypointsButtonClicked() }
        btnClearMap.setOnClickListener { clearMapButtonClicked() }
        btnSearch.setOnClickListener { searchExampleButtonClicked() }
        btnGeocoding.setOnClickListener { geocodeAnAddressButtonClicked() }
    }

    private fun handleAndroidPermissions() {
        permissionRequest = PermissionsRequester(this)
        permissionRequest!!.request(object : PermissionsRequester.ResultListener {
            override fun permissionsGranted() {
                loadMapScene()
            }

            override fun permissionsDenied() {
                Log.e(TAG, "Permissions denied by user.")
            }
        })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        permissionRequest!!.onRequestPermissionsResult(requestCode, grantResults)
    }


    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }


    /*  private fun loadMapScene() {
          // Load a scene from the HERE SDK to render the map with a map scheme.
          mapView.mapScene.loadScene(MapScheme.NORMAL_DAY) { mapError ->
              if (mapError == null) {
                  val distanceInMeters = (1000 * 10).toDouble()
                  mapView.camera.lookAt(
                          GeoCoordinates(28.6432382, 77.3764952), distanceInMeters)
              } else {
                  Log.d(this.toString(), "Loading map failed: mapError: " + mapError.name)
              }
          }
      }*/

    private fun loadMapScene() {
        mapView.mapScene.loadScene(MapScheme.NORMAL_DAY) { mapError ->
            if (mapError == null) {
                mapMarkerExample = MapMarker(this, mapView)
                routingExample = Routing(this, mapView)
                searchExample = Search(this, mapView)
            } else {
                Log.d(TAG, "onLoadScene failed: $mapError")
            }
        }
    }

    //region MapMarker/POI
    private fun anchoredMapMarkersButtonClicked() {
        mapMarkerExample?.showAnchoredMapMarkers()
    }

    private fun centeredMapMarkersButtonClicked() {
        mapMarkerExample?.showCenteredMapMarkers()
    }

    private fun clearMapButtonClicked() {
        mapMarkerExample?.clearMap()
        routingExample?.clearMap()
    }
    //endregion


    //region Routing
    private fun addRouteBtnClicked() {
        routingExample?.addRoute()
    }

    private fun addWaypointsButtonClicked() {
        routingExample?.addWayPoints()
    }
    //endregion


    //region Search
    fun searchExampleButtonClicked() {
        searchExample!!.onSearchButtonClicked()
    }

    fun geocodeAnAddressButtonClicked() {
        searchExample!!.onGeocodeButtonClicked()
    }
    //endregion
}