package com.jbseppanen.birthride

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_driver_ride_status.*
import kotlinx.coroutines.*

class DriverRideStatusActivity : MainActivity(), OnMapReadyCallback {

    companion object {
        const val DRIVER_RIDE_STATUS_KEY = "Driver ride status key"
    }

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mMap: GoogleMap
    private lateinit var context: Context
    private lateinit var activity: DriverRideStatusActivity
    private var driverLatLng: LatLng? = null
    private var motherLatLng: LatLng? = null
    private var destLatLng: LatLng? = null
    private var listIndex = 0
    private var rideId = -1L
    private var rides = mutableListOf<Ride>()
    private var request = HashMap<String, String>()
    private var user: User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        context = this
        activity = this

        val frameLayout: FrameLayout = findViewById(R.id.content_frame)
        frameLayout.addView(
            LayoutInflater.from(context).inflate(
                R.layout.activity_driver_ride_status,
                null
            )
        )
        super.onCreateDrawer()
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_driverstatus) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        rideId = intent.getLongExtra(DRIVER_RIDE_STATUS_KEY, -1)

        button_driverstatus_pickup.setOnClickListener {
            updateStatus(ApiDao.StatusType.PICKUP)
            updateLocation()
        }

        button_driverstatus_dropoff.setOnClickListener {
            if (rides.size > listIndex) {
                updateStatus(ApiDao.StatusType.DROPOFF)
                removeFromSharedPrefs(rides[listIndex].id.toString(), context)
                updateLocation()
                updateViews()
            }
        }

        button_driverstatus_startdirections.setOnClickListener {
            if (motherLatLng != null) {
                val uri =
                    Uri.parse("google.navigation:q=${motherLatLng!!.latitude},${motherLatLng!!.longitude}}")
                val directionsIntent = Intent(Intent.ACTION_VIEW, uri)
                directionsIntent.setPackage("com.google.android.apps.maps")
                startActivity(directionsIntent)
            }
        }

        button_driverstatus_enddirections.setOnClickListener {
            if (destLatLng != null) {
                val uri =
                    Uri.parse("google.navigation:q=${destLatLng!!.latitude},${destLatLng!!.longitude}}")
                val directionsIntent = Intent(Intent.ACTION_VIEW, uri)
                directionsIntent.setPackage("com.google.android.apps.maps")
                startActivity(directionsIntent)
            }
        }

        button_driverstatus_next.setOnClickListener {
            if (rides.size > listIndex + 1) {
                ++listIndex
                updateViews()
            }
        }

        button_driverstatus_prev.setOnClickListener {
            if (listIndex > 0) {
                --listIndex
                updateViews()
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        CoroutineScope(Dispatchers.IO + Job()).launch {
            rides = ApiDao.getUserRides(ApiDao.UserType.DRIVER)
            //Get index of requested item
            if (rideId != -1L) {
                rides.forEachIndexed { index, r ->
                    if (r.id == rideId) {
                        listIndex = index
                    }
                }
            }
            withContext(Dispatchers.Main) {
                updateViews()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateLocation()
    }

    private fun updateViews() {
        mMap.clear()
        if (rides.size > listIndex) {
            request = getSavedRequestById(context, rides[listIndex].id)
            text_driverstatus_dropoffplace.text = rides[listIndex].dest_name
            text_driverstatus_fare.text = rides[listIndex].price
            text_driverstatus_status.text =
                rides[listIndex].ride_status.replace("_", " ").capitalize()
            if (request.isNotEmpty()) {
                for ((key, value) in request) {
                    when (key) {
                        "name" -> text_driverstatus_name.text = value
                        "phone" -> text_driverstatus_phone.text = value
                    }
                }
            }
            var latLng = toLatLng(rides[listIndex].start)
            if (latLng != null) {
                motherLatLng = latLng
            }
            latLng = toLatLng(rides[listIndex].destination)
            if (latLng != null) {
                destLatLng = latLng
            }
            updateMap()
        }
    }

    private fun updateMap() {
        if (::mMap.isInitialized) {
            mMap.clear()
            val markerPoints = ArrayList<LatLng>()

            if (driverLatLng != null) {
                setPoint(driverLatLng!!, PointType.START)
                markerPoints.add(driverLatLng!!)
            }
            if (motherLatLng != null) {
                setPoint(motherLatLng!!, PointType.PICKUP)
                markerPoints.add(motherLatLng!!)
            }
            if (destLatLng != null) {
                setPoint(destLatLng!!, PointType.PICKUP)
                markerPoints.add(destLatLng!!)
            }
            if (driverLatLng != null && motherLatLng != null) {
                drawDirections(driverLatLng!!, motherLatLng!!, Color.BLUE)
            }
            if (motherLatLng != null && destLatLng != null) {
                drawDirections(motherLatLng!!, destLatLng!!, Color.RED)
            }
            if (markerPoints.size >= 2) {
                val builder = LatLngBounds.Builder()
                for (marker in markerPoints) {
                    builder.include(marker)
                }
                val padding = (resources.displayMetrics.widthPixels * .2).toInt()
                mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), padding))
            }
        }
    }

    private fun drawDirections(start: LatLng, end: LatLng, lineColor: Int) {
        CoroutineScope(Dispatchers.IO + Job()).launch {
            val path = ApiDao.getDirections(activity, start, end)
            withContext(Dispatchers.Main) {
                for (i in 0 until path.size) {
                    mMap.addPolyline(
                        PolylineOptions().addAll(path[i]).width(10f).color(
                            lineColor
                        )
                    )
                }
            }
        }
    }

    private fun setPoint(latLng: LatLng, title: PointType) {
        // Creating MarkerOptions
        val options = MarkerOptions()
        options.position(latLng).title(title.type)
        when (title.type) {
            PointType.START.type -> {
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            }
            PointType.PICKUP.type -> {
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            }
            PointType.DROPOFF.type -> {
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            }
        }
        // Add new marker to the Google Map Android API V2
        mMap.addMarker(options)
    }

    private fun updateStatus(status: ApiDao.StatusType) {
        if (rides.size > listIndex) {
            CoroutineScope(Dispatchers.IO + Job()).launch {
                val success = ApiDao.updateRideStatus(rides[listIndex].id, status, null)
                val message: String
                if (success) {
                    message = "Ride updated!"
                    val updatedRide = ApiDao.getRideById(rides[listIndex].id)
                    if (updatedRide != null) {
                        rides[listIndex] = updatedRide
                    }
                } else {
                    message = "Failed to update ride status."
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    updateViews()
                }
            }
        }
    }

    private fun updateLocation() {
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
            Toast.makeText(context, "Need to grant permission to use location.", Toast.LENGTH_SHORT)
                .show()
        } else {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
//                driverLatLng = LatLng(location.latitude, location.longitude)  //TODO enable this line to update locations
//                driverLatLng = generateMockLocations() //Todo remove this line that uses mock data.
                CoroutineScope(Dispatchers.IO + Job()).launch {
                    if (user == null) {
                        user = ApiDao.getCurrentUser()
                    }
                    if (user != null) {
                        //user?.userData?.location = Location("","${driverLatLng!!.latitude},${driverLatLng!!.longitude}","" )//TODO enable this line to update locations
                        ApiDao.updateCurrentUser(user!!, false)
                    }
                }
            }
        }
    }

}
