package com.jbseppanen.birthride

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
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
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.JsonObject
import org.json.JSONObject

class DriverViewRequestsActivity : MainActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var markerPoints = ArrayList<LatLng>()
    private lateinit var activity: DriverViewRequestsActivity
    private var user: User? = null
    private var rideId: Long = -1
    private lateinit var receiver: BroadcastReceiver
    private lateinit var notificationMap: HashMap<*, *>

    enum class PointType(val type: String) {
        START("Start Point"), PICKUP("Pickup Point"), DROPOFF("Dropoff Point")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_driver_view_requests)
        setContentView(R.layout.activity_main)

        val context: Context = this

        val frameLayout:FrameLayout = findViewById(R.id.content_frame)
//        frameLayout.addView(LayoutInflater.from(context).inflate(R.layout.activity_driver_view_requests,findViewById(R.id.layout_coordinator)))
        frameLayout.addView(LayoutInflater.from(context).inflate(R.layout.activity_driver_view_requests, null))
        super.onCreateDrawer()

//        val mDrawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_driverview) as SupportMapFragment
        mapFragment.getMapAsync(this)
        activity = this

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener(OnCompleteListener { task ->
                if (!task.isSuccessful) {
                    println("getInstanceId failed $task.exception")
                    return@OnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result?.token

                if (token != null) {
                    CoroutineScope(Dispatchers.IO + Job()).launch {
                        ApiDao.updateFcmToken(token)
                    }
                }

                // Log and toast
                val msg = getString(R.string.msg_token_fmt, token)
//                Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
            })


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
           Toast.makeText(context, "Need to grant permission to use location.", Toast.LENGTH_SHORT).show()
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

                        notificationMap =
                            receivedIntent.getSerializableExtra(PushNotificationService.SERVICE_MESSAGE_KEY) as HashMap<*, *>
                        for ((key, value) in notificationMap) {
                            if (key is String && value is String) {
                                    when (key) {
                                        "hospital" -> println(key)
                                        "name" -> text_driverview_name.text = value
                                        "phone" -> text_driverview_phone.text = value
                                        "price" -> text_driverview_fare.text = value
                                        "ride_id" -> rideId = value.toLong()
                                }
                            }
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
        button_driverview_accept.setOnClickListener {
            if (!rideId.equals(-1)) {
                CoroutineScope(Dispatchers.IO + Job()).launch {
                    val success = ApiDao.acceptRejectRide(rideId, true, null)
                    val message = if (success) {
                        "Ride Accepted!"
                    } else {
                        "Failed to accept ride."
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }
            }

            //Temp lines below
/*            CoroutineScope(Dispatchers.IO + Job()).launch {
                ApiDao.getRideById(0)
            }*/

        }

        button_driverview_reject.setOnClickListener {
            if (!rideId.equals(-1)) {
                CoroutineScope(Dispatchers.IO + Job()).launch {
                    val success =
                        ApiDao.acceptRejectRide(
                            rideId,
                            false,
                            JSONObject(notificationMap).toString()
//                            notificationMap.toString()
                        )
                    if (success) {
                        notificationMap = HashMap<String, String>()
                    }
                    val message = if (success) {
                        "Ride Rejected!"
                    } else {
                        "Failed to reject ride."
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
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
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            }
            PointType.DROPOFF.type -> {
                options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
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
            button_driverview_togglestatus.text = getString(R.string.driver_status_true)
        } else {
            button_driverview_togglestatus.setBackgroundColor(
                ContextCompat.getColor(
                    context,
                    R.color.colorButtonRed
                )
            )
            button_driverview_togglestatus.text = getString(R.string.driver_status_false)
        }
    }
}
