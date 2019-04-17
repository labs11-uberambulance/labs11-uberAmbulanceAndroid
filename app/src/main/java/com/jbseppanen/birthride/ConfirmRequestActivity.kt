package com.jbseppanen.birthride

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_confirm_request.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json


class ConfirmRequestActivity : MainActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
//    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var markerPoints = ArrayList<LatLng>()
    private lateinit var activity: ConfirmRequestActivity
    private lateinit var user: User
    private var requestedDriver: RequestedDriver? = null
    private lateinit var drivers: ArrayList<RequestedDriver>
    var driverIndex = 0
    private lateinit var context: Context

    companion object {
        const val LOCATION_REQUEST_CODE = 11
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        context = this
        val frameLayout: FrameLayout = findViewById(R.id.content_frame)
        frameLayout.addView(
            LayoutInflater.from(context).inflate(
                R.layout.activity_confirm_request,
                null
            )
        )
        super.onCreateDrawer()


        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_requestconfirm) as SupportMapFragment
        mapFragment.getMapAsync(this)
        activity = this
        context = this
//        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        CoroutineScope(Dispatchers.IO + Job()).launch {
            getUser()
        }


/*        if (ContextCompat.checkSelfPermission(
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
            }
        }*/

        button_requestconfirm_send.setOnClickListener {
            if (markerPoints.size != 2) {
                Toast.makeText(context, "Select start and end points on map.", Toast.LENGTH_LONG)
                    .show()
            } else {
                user.motherData?.start?.latlng =
                    "${markerPoints[0].latitude},${markerPoints[0].longitude}"
                user.motherData?.destination?.latlng =
                    "${markerPoints[1].latitude},${markerPoints[1].longitude}"
                CoroutineScope(Dispatchers.IO + Job()).launch {
                    val success =
                        ApiDao.postRideRequest(user, requestedDriver?.driver?.firebase_id!!)
                    if (success) {
                        withContext(Dispatchers.Main) {
                            startActivity(
                                Intent(
                                    context,
                                    MotherRideStatusActivity::class.java
                                )
                            )
                            finish()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Ride was not able to be requested.  That's all we know.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }

        button_requestconfirm_next.setOnClickListener {
            if (driverIndex < drivers.size - 1) {
                ++driverIndex
                updateDriverInfo()
            }
        }

        button_requestconfirm_prev.setOnClickListener {
            if (driverIndex > 0) {
                --driverIndex
                updateDriverInfo()
            }
        }

        button_requestconfirm_details.setOnClickListener {
            if (::drivers.isInitialized) {
                if (drivers.size > 0) {
                    val driverIntent = Intent(context, DriverDetailsActivity::class.java)
                    val extra = Json.stringify(RequestedDriver.serializer(), drivers[driverIndex])
                    driverIntent.putExtra(DriverDetailsActivity.DRIVER_DETAILS_KEY, extra)
                    startActivity(driverIntent)
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        CoroutineScope(Dispatchers.IO + Job()).launch {
            displayUserPoints()
        }

        mMap.setOnMapClickListener {
            /* latLng ->
                        setPoint(latLng)*/
            val requestIntent = Intent(context, LocationSelectionActivity::class.java)
            val markerPoints = ArrayList<LatLng>()
            var userLocation: Location? = user.userData.location
            if (userLocation != null) {
                markerPoints.add(userLocation.asLatLng())
            }
            if (user.userData.user_type == UserTypeSelectionActivity.DRIVER) {
                requestIntent.putExtra(LocationSelectionActivity.INPUT_NUMBER_OF_POINTS_KEY, 1)
            } else {
                userLocation = user.motherData?.destination
                if (userLocation != null) {
                    markerPoints.add(userLocation.asLatLng())
                }
                requestIntent.putExtra(LocationSelectionActivity.INPUT_NUMBER_OF_POINTS_KEY, 2)
            }
            requestIntent.putExtra(LocationSelectionActivity.INPUT_POINTS_KEY, markerPoints)
            startActivityForResult(
                requestIntent,
                EditAccountDetailsActivity.LOCATION_REQUEST_CODE
            )
        }
    }

    private fun setPoint(latLng: LatLng) {

        if (markerPoints.size > 1) {
            markerPoints.clear()
            mMap.clear()
        }
        markerPoints.add(latLng)

        // Creating MarkerOptions
        val options = MarkerOptions()

        // Setting the position of the marker
        if (markerPoints.size == 1) {
            options.position(latLng).title("Start")
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            updateDrivers(latLng)
        } else if (markerPoints.size == 2) {
            options.position(latLng).title("End")
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        }

        // Add new marker to the Google Map Android API V2
        mMap.addMarker(options)

        // Checks, whether start and end locations are captured
        if (markerPoints.size >= 2) {

            //Zoom in to scope of points
            val builder = LatLngBounds.Builder()
            for (marker in markerPoints) {
                builder.include(marker)
            }
            val padding = (resources.displayMetrics.widthPixels * .2).toInt()
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), padding))

            val origin = markerPoints[0]
            val dest = markerPoints[1]
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

    private fun updateDrivers(location: LatLng) {
        CoroutineScope(Dispatchers.IO + Job()).launch {
            drivers = ApiDao.getDrivers(LatLng(location.latitude, location.longitude))
            if (drivers.size > 0) {
                driverIndex = 0
                withContext(Dispatchers.Main) {
                    updateDriverInfo()
                }
            }
        }
    }

    private fun updateDriverInfo() {
        if (drivers.size - 1 >= driverIndex) {
            requestedDriver = drivers[driverIndex]
            text_requestconfirm_pickuptime.text = requestedDriver!!.duration.time
            text_requestconfirm_fare.text =
                "${requestedDriver!!.driver.price.toString()} USh"
//            text_requestconfirm_waittime.text = "10 mins"
            text_requestconfirm_waittime.text = requestedDriver!!.driver.name
        }
    }

    private suspend fun getUser() {
        val returnedUser = ApiDao.getCurrentUser()
        if (returnedUser != null) {
            user = returnedUser
            withContext(Dispatchers.Main) {
                button_requestconfirm_details.isEnabled = true
                button_requestconfirm_prev.isEnabled = true
                button_requestconfirm_next.isEnabled = true
                button_requestconfirm_send.isEnabled = true
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == EditAccountDetailsActivity.LOCATION_REQUEST_CODE) {
                val locations =
                    data?.extras?.getParcelableArrayList<LatLng>(LocationSelectionActivity.RETURN_POINTS_KEY)
                if (locations != null) {
                    user.userData.location =
                        Location(null, "${locations[0].latitude},${locations[0].longitude}", null)
                    if (locations.size > 1) {
                        user.motherData?.destination =
                            Location(
                                null,
                                "${locations[1].latitude},${locations[1].longitude}",
                                null
                            )
                    }
                    CoroutineScope(Dispatchers.IO + Job()).launch {
                        displayUserPoints()
                        ApiDao.updateCurrentUser(user, false)
                    }
                }
            }
        }

    }

    private suspend fun displayUserPoints() {
        if (!::user.isInitialized) {
            getUser()
        }
        val userLocation: LatLng? = user.userData.location?.asLatLng()
        if (userLocation != null) {
            withContext(Dispatchers.Main) {
                setPoint(userLocation)
            }
        }
        val userDest: LatLng? = user.motherData?.destination?.asLatLng()
        if (userDest != null) {
            withContext(Dispatchers.Main) {
                setPoint(userDest)
            }
        }
    }
}


/*    class MinutesPicker : DialogFragment() {
  text_requestconfirm_waittime.setOnClickListener {
        val fragment = MinutesPicker()
        fragment.show(supportFragmentManager, "Minutes To Wait")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: NumberPicker = NumberPicker(context)
        view.minValue = 0
        view.maxValue = 60
        return view
    }
}*/


