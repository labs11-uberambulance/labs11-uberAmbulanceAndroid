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
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var markerPoints = ArrayList<LatLng>()
    private lateinit var activity: LocationSelectionActivity
    private lateinit var context: Context
    private var numOfPoints = 1
    private var locLatLng = Constants.defaultMapCenter
    private val pointColors = arrayOf(
        BitmapDescriptorFactory.HUE_RED,
        BitmapDescriptorFactory.HUE_GREEN,
        BitmapDescriptorFactory.HUE_BLUE,
        BitmapDescriptorFactory.HUE_YELLOW
    )

    companion object {
        const val RETURN_POINTS_KEY = "return points key"
        const val INPUT_NUMBER_OF_POINTS_KEY = "num of points key"
        const val INPUT_POINTS_KEY = "input points key"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_selection)

        activity = this
        context = this
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_locationselection) as SupportMapFragment
        mapFragment.getMapAsync(this)

        numOfPoints = intent.getIntExtra(INPUT_NUMBER_OF_POINTS_KEY, 1)

        button_locationselection_setlocations.setOnClickListener {
            if (markerPoints.size == numOfPoints) {
                val intent = Intent()
                intent.putExtra(RETURN_POINTS_KEY, markerPoints)
                setResult(Activity.RESULT_OK, intent)
                finish()
            } else {
                Toast.makeText(context, "Not enough points.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun addPoint(latLng: LatLng) {
        if (markerPoints.size == numOfPoints) {
            markerPoints.clear()
            mMap.clear()
            button_locationselection_setlocations.isEnabled = false
        } else if (!Constants.mapBounds.contains(latLng)) {
            Toast.makeText(context, "Outside of bounds.  Try again", Toast.LENGTH_SHORT).show()
        } else {
            markerPoints.add(latLng)

            val options = MarkerOptions()

            if (markerPoints.size == numOfPoints) {
                button_locationselection_setlocations.isEnabled = true
            }

            options.icon(
                BitmapDescriptorFactory.defaultMarker(
                    pointColors[when (markerPoints.size <= pointColors.size) {
                        true -> markerPoints.size - 1
                        false -> pointColors.size - 1
                    }]
                )
            )
            options.position(latLng)
            if (markerPoints.size == 1) {
                options.title("Start")
            } else if (markerPoints.size == 2) {
                options.title("End")
            }
            mMap.addMarker(options).showInfoWindow()


            // Checks, whether start and end locations are captured
            if (markerPoints.size >= 2) {
                val origin = markerPoints[0]
                val dest = markerPoints[1]
                CoroutineScope(Dispatchers.IO + Job()).launch {
                    val path = ApiDao.getDirections(activity, origin, dest)
                    withContext(Dispatchers.Main) {
                        for (i in 0 until path.size) {
                            mMap.addPolyline(
                                PolylineOptions().addAll(path[i]).width(10f).color(
                                    Color.RED
                                )
                            )
                        }
                    }
                }
            }
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap
        mMap.setLatLngBoundsForCameraTarget(Constants.mapBounds)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(locLatLng))
        mMap.setOnMapClickListener { latLng ->
            addPoint(latLng)
        }

        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) run {
            Toast.makeText(
                context,
                "Need to grant permission to use current location.",
                Toast.LENGTH_SHORT
            )
                .show()
            return
        } else {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                locLatLng = LatLng(location.latitude, location.longitude)
                if (!Constants.mapBounds.contains(locLatLng)) {
                    locLatLng = Constants.defaultMapCenter
                    Toast.makeText(
                        context,
                        "Your current location is outside of bounds.  Using default map center.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                mMap.animateCamera(CameraUpdateFactory.newLatLng(
                    locLatLng
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
            }
        }
        val inputPoints = intent.extras?.getParcelableArrayList<LatLng>(INPUT_POINTS_KEY)
        inputPoints?.forEach {addPoint(it)}
    }
}