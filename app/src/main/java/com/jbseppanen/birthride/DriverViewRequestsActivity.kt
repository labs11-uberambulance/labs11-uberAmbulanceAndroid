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
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
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
import kotlinx.coroutines.*
import org.json.JSONObject

class DriverViewRequestsActivity : MainActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var markerPoints = ArrayList<LatLng>()
    private lateinit var activity: DriverViewRequestsActivity
    private var user: User? = null
    private var rideId: Long = -1
    private lateinit var receiver: BroadcastReceiver
    private val requests = ArrayList<HashMap<*, *>>()
    private lateinit var notificationMap: HashMap<*, *>
    private lateinit var context: Context

    enum class PointType(val type: String) {
        START("Your Location"), PICKUP("Pickup Point"), DROPOFF("Drop Off Point")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        context = this
        val frameLayout: FrameLayout = findViewById(R.id.content_frame)
        frameLayout.addView(
            LayoutInflater.from(context).inflate(
                R.layout.activity_driver_view_requests,
                null
            )
        )
        super.onCreateDrawer()

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
            Toast.makeText(context, "Need to grant permission to use location.", Toast.LENGTH_SHORT)
                .show()
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
                        refreshRequests()

/*                        notificationMap =
                            receivedIntent.getSerializableExtra(PushNotificationService.SERVICE_MESSAGE_KEY) as HashMap<*, *>
                        requests.add(notificationMap)
                        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
                        val savedRideId = notificationMap["ride_id"] as String
                        sharedPrefs.edit().remove(savedRideId).apply()
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
                        toggleVisibility()*/
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

/*        CoroutineScope(Dispatchers.IO + Job()).launch {
            val userRides: ArrayList<Ride> = ApiDao.getUserRides()
            userRides.forEach {
                if (!it.ride_status.contains("waiting")) {
                    userRides.remove(it)
                }
            }
            var ride: Ride
            if (userRides.size > 0) {
                val rides =
                    userRides.sortedWith(compareBy { it.id }).reversed() as ArrayList<Ride>
                ride = userRides[0]
            }
            withContext(Dispatchers.Main) {
                progress_driverview.visibility = View.INVISIBLE

//Do something here.

            }
        }*/


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
            refreshRequests()
            //TODO Go to another view.
        }

        button_driverview_reject.setOnClickListener {
            if (!rideId.equals(-1)) {
                CoroutineScope(Dispatchers.IO + Job()).launch {
                    val success =
                        ApiDao.acceptRejectRide(
                            rideId,
                            false,
                            JSONObject(notificationMap).toString()
                        )
                    if (success) {
                        requests.remove(notificationMap)
                        removeFromSharedPrefs(notificationMap)
//                        notificationMap = HashMap<String, String>()
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
            refreshRequests()
        }

        button_driverview_refresh.setOnClickListener {
            refreshRequests()
//            println(requests)
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
        var userLocation: Location? = user?.userData?.location
        if (userLocation != null) {
            setPoint(userLocation.asLatLng(), PointType.PICKUP)
        }

        userLocation = user?.motherData?.destination
        if (userLocation != null) {
            setPoint(userLocation.asLatLng(), PointType.DROPOFF)
        }

/*        mMap.setOnMapClickListener { latLng ->
            //            setPoint(latLng)
        }*/
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

    private fun setStatusButton(status: Boolean, context: Context) {
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

    private fun toggleRequestVisibility(status: Boolean) {
        layout_driverview_default.visibility = when (status) {
            true -> View.INVISIBLE
            false -> View.VISIBLE
        }
        layout_driverview_request.visibility = when (status) {
            true -> View.VISIBLE
            false -> View.INVISIBLE
        }
    }

    private fun refreshRequests() {
        requests.clear()
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
//        sharedPrefs.edit().remove(PushNotificationService.STORED_REQUESTS_KEY).apply()
//        sharedPrefs.edit().putString(PushNotificationService.STORED_REQUESTS_KEY, "47").apply()
        val rideIdsAsString = sharedPrefs.getString(PushNotificationService.STORED_REQUESTS_KEY, null)
        val map = HashMap<String, String>()
        if (rideIdsAsString != null) {
            val requestIds = rideIdsAsString.split(",")
            for (requestId in requestIds) {
                val requestData = sharedPrefs.getString(requestId, null)
                if (requestData != null) {
                    val requestArray = requestData.split(",") as MutableList<String>
                    requestArray.forEachIndexed { index, item ->
                        val itemArray = item.replace("{", "").replace("}", "").split("=")
                        map[itemArray[0].trim()] = itemArray[1]
                    }
                    var timeStamp = map["timestamp"]?.toLong()
                    if (timeStamp != null) {
                        timeStamp += System.currentTimeMillis()
                        if (timeStamp > System.currentTimeMillis()) {
                            requests.add(map)
                        } else {
                            removeFromSharedPrefs(map)
                        }
                    }
                }
            }
        }
        updateViews()
    }

    private fun updateViews() {
        if (requests.size > 0) {
            notificationMap = requests[0]
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
            toggleRequestVisibility(true)
        } else {
            toggleRequestVisibility(false)
        }
    }

    private fun removeFromSharedPrefs(map:HashMap<*, *>) {
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        val id = map["ride_id"] as String
        sharedPrefs.edit().remove(id).apply()
        val rideIds = sharedPrefs.getString(PushNotificationService.STORED_REQUESTS_KEY, null)
        if (rideIds != null) {
            val idArray = rideIds.split(",")
            val newIdArray = ArrayList<String>()
            idArray.forEach {
                if (it != id) {
                    newIdArray.add(it)
                }
            }
            sharedPrefs.edit()
                .putString(PushNotificationService.STORED_REQUESTS_KEY, idArray.toString().removePrefix("[").removeSuffix("]")).apply()
        }
    }

}
