package com.jbseppanen.birthride

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.TooltipCompat
import android.view.View
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_location_selection.*
import kotlinx.coroutines.*


class LocationSelectionActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var markerPoints = ArrayList<LatLng>()
    private lateinit var activity: LocationSelectionActivity
    private lateinit var context: Context
    private var numOfPoints = 1
    private lateinit var locLatLng: LatLng
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

        updateCurrentLocation()

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

        button_locationselection_center_map.setOnClickListener {
            progress_locationselection.visibility = View.VISIBLE
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    WelcomeActivity.LOCATION_REQUEST_CODE
                )
            }
            if (!::locLatLng.isInitialized) {
                CoroutineScope(Dispatchers.IO + Job()).launch {
                    updateCurrentLocation()
                    var timeDelay = 0L
                    while (timeDelay < 10000) { //Wait for GPS for up to 10 seconds
                        if (::locLatLng.isInitialized) {
                            break
                        }
                        delay(100)
                        timeDelay += 100
                    }

                    var message = ""
                    if (!::locLatLng.isInitialized) {
                        message =
                            "Location not found.  Check that GPS is on and location permissions have been given."
                    } else if (!Constants.mapBounds.contains(locLatLng)) {
                        message = "Your current location is outside of bounds."
                    }
                    withContext(Dispatchers.Main) {
                        if (message != "") {
                            Toast.makeText(
                                context,
                                message,
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            addPoint(locLatLng)
                        }
                        progress_locationselection.visibility = View.GONE
                    }
                }
            } else if (!Constants.mapBounds.contains(locLatLng)) {
                Toast.makeText(
                    context,
                    "Your current location is outside of bounds.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                addPoint(locLatLng)
            }
            progress_locationselection.visibility = View.GONE
        }

        TooltipCompat.setTooltipText(
            button_locationselection_center_map,
            "Click here to use your current location"
        )

    }


    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap
        mMap.setLatLngBoundsForCameraTarget(Constants.mapBounds)
        mMap.moveCamera(CameraUpdateFactory.newLatLng(Constants.defaultMapCenter))
        mMap.setOnMapClickListener { latLng ->
            addPoint(latLng)
        }

        val inputPoints = intent.extras?.getParcelableArrayList<LatLng>(INPUT_POINTS_KEY)
        inputPoints?.forEach { addPoint(it) }
    }

    private fun addPoint(latLng: LatLng) {
        if (markerPoints.size == numOfPoints) {
            markerPoints.clear()
            mMap.clear()
            button_locationselection_setlocations.isEnabled = false
        }
        if (!Constants.mapBounds.contains(latLng)) {
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


                val builder = LatLngBounds.Builder()
                for (marker in markerPoints) {
                    builder.include(marker)
                }
                val padding = (resources.displayMetrics.widthPixels*.2).toInt()
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(),padding))

                val origin = markerPoints[markerPoints.size - 2]
                val dest = markerPoints[markerPoints.size - 1]
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

    private fun updateCurrentLocation() {
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
        } else {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                locLatLng = LatLng(location.latitude, location.longitude)
//                locLatLng = Constants.defaultMapCenter  // For debugging.
            }
        }
    }
}