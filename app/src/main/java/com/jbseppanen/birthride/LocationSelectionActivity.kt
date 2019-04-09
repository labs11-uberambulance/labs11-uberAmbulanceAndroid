package com.jbseppanen.birthride

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.android.synthetic.main.activity_location_selection.*
import kotlinx.coroutines.*


class LocationSelectionActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var markerPoints = ArrayList<LatLng>()
    private lateinit var activity: LocationSelectionActivity
    var numOfPoints = 1

    companion object {
        const val LOCATIONS_KEY = "locations_key"
        const val NUMBER_OF_POINTS_KEY = "points key"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_selection)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_locationselection) as SupportMapFragment
        mapFragment.getMapAsync(this)
        activity = this
        val context: Context = this
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        numOfPoints = intent.getIntExtra(NUMBER_OF_POINTS_KEY, 1)

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) run {
            Toast.makeText(context, "Need to grant permission to use location.", Toast.LENGTH_SHORT)
                .show()
            return
        } else {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                mMap.animateCamera(CameraUpdateFactory.newLatLng(
//                    LatLng(location.latitude,location.longitude)
                    Constants.defaultMapCenter //Todo remove this hardcoded location
                ), 2000, object : GoogleMap.CancelableCallback {
                    override fun onFinish() {
                        mMap.animateCamera(
                            CameraUpdateFactory.zoomTo(10f),
                            2000,
                            object : GoogleMap.CancelableCallback {
                                override fun onFinish() {}
                                override fun onCancel() {}
                            })
                    }

                    override fun onCancel() {
                    }
                })
                val dataScope = CoroutineScope(Dispatchers.IO + Job())
                dataScope.launch {
                    ApiDao.getDrivers(Constants.defaultMapCenter)//Todo remove this hardcoded location
//                    ApiDao.getDrivers(LatLng(location.latitude, location.longitude))
                }
            }
        }

        button_locationselection_setlocations.setOnClickListener {
            val intent = Intent()
            intent.putExtra(LOCATIONS_KEY, markerPoints)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setLatLngBoundsForCameraTarget(Constants.mapBounds)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(Constants.defaultMapCenter))

        mMap.setOnMapClickListener { latLng ->
            if (markerPoints.size > numOfPoints) {
                markerPoints.clear()
                mMap.clear()
                button_locationselection_setlocations.isEnabled = false
            } else {
                markerPoints.add(latLng)

                val options = MarkerOptions()

                if (markerPoints.size == 1) {
                    options.position(latLng).title("Start")
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                } else if (markerPoints.size == 2) {
                    options.position(latLng).title("End")
                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    button_locationselection_setlocations.isEnabled = true
                }

                mMap.addMarker(options)
            }

            // Checks, whether start and end locations are captured
            if (markerPoints.size >= 2) {
                val origin = markerPoints[0] as LatLng
                val dest = markerPoints[1] as LatLng
                CoroutineScope(Dispatchers.IO + Job()).launch {
                    val path = ApiDao.getDirections(activity, origin, dest)
                    withContext(Dispatchers.Main) {
                        for (i in 0 until path.size) {
                            mMap.addPolyline(PolylineOptions().addAll(path[i]).width(5f).color(Color.RED))
                        }
                    }
                }
            }
        }
    }
}