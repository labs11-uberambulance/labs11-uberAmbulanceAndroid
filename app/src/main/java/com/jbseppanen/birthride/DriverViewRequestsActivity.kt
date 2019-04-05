package com.jbseppanen.birthride

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
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
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_driver_view_requests.*
import kotlinx.coroutines.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

class DriverViewRequestsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var markerPoints = ArrayList<LatLng>()
    private lateinit var activity: DriverViewRequestsActivity
    private var user: User? = null
    private var requestedDriver: RequestedDriver? = null
    private lateinit var receiver: BroadcastReceiver

    enum class PointType(val type: String) {
        START("Start Point"), PICKUP("Pickup Point"), DROPOFF("Dropoff Point")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_view_requests)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_driverview) as SupportMapFragment
        mapFragment.getMapAsync(this)
        activity = this
        val context: Context = this
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)


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
            }

        }

        CoroutineScope(Dispatchers.IO + Job()).launch {
            user = ApiDao.getCurrentUser()
            if (user != null) {
                val status = user!!.driverData?.active
                if (status != null) {
                    withContext(Dispatchers.Main) {
                        setStatusButton(status, context)
                    }
                }
            }
        }

        receiver = object : BroadcastReceiver() {
            override fun onReceive(contxt: Context?, receivedIntent: Intent?) {
                when (receivedIntent?.action) {
                    PushNotificationService.SERVICE_BROADCAST_KEY -> {
                        val jsonString =
                            receivedIntent.getStringExtra(PushNotificationService.SERVICE_MESSAGE_KEY)
                        try {
                            val rideRequest =
                                Json.nonstrict.parse(RideRequestData.serializer(), jsonString)
                            text_driverview_name.text = rideRequest.name
                            text_driverview_phone.text = rideRequest.phone
                            text_driverview_pickuptime.text =
                                ""//TODO calculate this or make it go away.
                            text_driverview_fare.text = rideRequest.price.toString()
                        } catch (e: SerializationException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                /*                if (!task.isSuccessful) {
                                    Log.w("ServiceTag", "getInstanceId failed", task.exception)
                                    return@OnCompleteListener
                                }

                                // Get new Instance ID token
                                val token = task.result?.token

                                // Log and toast
                                val msg = getString(R.string.msg_token_fmt, token)
                                Log.d("ServiceTag", msg)
                                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()*/
            })

        button_driverview_togglestatus.setOnClickListener {
            if (user != null) {
                var status = user!!.driverData?.active
                if (status != null) {
                    status = !status
                    setStatusButton(status, context)
                    user?.driverData?.active = status
                    CoroutineScope(Dispatchers.IO + Job()).launch {
                        ApiDao.updateCurrentUser(user!!, false)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        LocalBroadcastManager.getInstance(this)
            .registerReceiver(receiver, IntentFilter(PushNotificationService.SERVICE_BROADCAST_KEY))
    }

    override fun onStop() {
        super.onStop()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
//        mMap.addMarker(MarkerOptions().position(Constants.defaultMapCenter).title("Marker in Uganda"))
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(Constants.defaultMapCenter))
        CoroutineScope(Dispatchers.IO + Job()).launch {
            if (user?.userData?.location != null) {
                var split: List<String>? = user!!.userData.location?.latlng?.split(",")
                if (split != null && split.size == 2) {
                    val userLatLng: LatLng? = LatLng(split[0].toDouble(), split[1].toDouble())
                    if (userLatLng != null) {
                        withContext(Dispatchers.Main) {
                            //                            setPoint(userLatLng)
                        }
                    }
                }
                split = user!!.motherData?.destination?.latlng?.split(",")
                if (split != null && split.size == 2) {
                    val userLatLng: LatLng? = LatLng(split[0].toDouble(), split[1].toDouble())
                    if (userLatLng != null) {
                        withContext(Dispatchers.Main) {
                            //                            setPoint(userLatLng)
                        }
                    }
                }
            }
        }

        mMap.setOnMapClickListener { latLng ->
            //            setPoint(latLng)
        }
    }

    private fun setPoint(latLng: LatLng, title: PointType) {

/*        if (markerPoints.size > 1) {
            markerPoints.clear()
            mMap.clear()
        }*/
        markerPoints.add(latLng)

        // Creating MarkerOptions
        val options = MarkerOptions()
        options.position(latLng).title(title.type)
        when (title.type) {
            PointType.START.type -> {
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            }
            PointType.PICKUP.type -> {
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            }
            PointType.DROPOFF.type -> {
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            }
        }

        // Add new marker to the Google Map Android API V2
        mMap.addMarker(options)

        // Checks, whether start and end locations are captured
        if (markerPoints.size >= 2) {
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

    fun setStatusButton(status: Boolean, context: Context) {
        if (status) {
            button_driverview_togglestatus.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.colorButtonGreen
                )
            )
            button_driverview_togglestatus.text = "Online"
        } else {
            button_driverview_togglestatus.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.colorButtonRed
                )
            )
            button_driverview_togglestatus.text = "Offline"
        }
    }
}
