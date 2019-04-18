package com.jbseppanen.birthride

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
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
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_driver_view_requests.*
import kotlinx.coroutines.*
import org.json.JSONObject

class DriverViewRequestsActivity : MainActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var activity: DriverViewRequestsActivity
    private var user: User? = null
    private var rideId: Long = -1
    private lateinit var receiver: BroadcastReceiver
    private var requests = ArrayList<HashMap<String, String>>()
    private var mainHashMap = HashMap<String, String>()
    private lateinit var context: Context
    private var driverLatLng: LatLng? = null
    private var motherLatLng: LatLng? = null
    private var destLatLng: LatLng? = null

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

                receiver = object : BroadcastReceiver() {
                    override fun onReceive(contxt: Context?, receivedIntent: Intent?) {
                        when (receivedIntent?.action) {
                            PushNotificationService.SERVICE_BROADCAST_KEY -> {
                                refreshRequests()
                            }
                        }
                    }
                }

                LocalBroadcastManager.getInstance(this)
                    .registerReceiver(
                        receiver,
                        IntentFilter(PushNotificationService.SERVICE_BROADCAST_KEY)
                    )
            })

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

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
                driverLatLng = LatLng(location.latitude, location.longitude)
                driverLatLng = Constants.defaultMapCenter //Todo remove this hardcoded location
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
            if (rideId != -1L) {
                CoroutineScope(Dispatchers.IO + Job()).launch {
                    val success = ApiDao.updateRideStatus(rideId, ApiDao.StatusType.ACCEPT, null)
                    val message: String
                    if (success) {
                        message = "Ride Accepted!"
                        mainHashMap["accepted"] = "true"
                        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
                        sharedPrefs.edit().putString(rideId.toString(), mainHashMap.toString())
                            .apply()
                        val statusIntent = Intent(context, DriverRideStatusActivity::class.java)
                        statusIntent.putExtra(
                            DriverRideStatusActivity.DRIVER_RIDE_STATUS_KEY,
                            rideId
                        )
                        withContext(Dispatchers.Main) {
                            refreshRequests()
                            startActivity(statusIntent)
                        }
                    } else {
                        message = "Failed to accept ride."
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

        button_driverview_reject.setOnClickListener {
            if (rideId != -1L) {
                CoroutineScope(Dispatchers.IO + Job()).launch {
                    val success =
                        ApiDao.updateRideStatus(
                            rideId,
                            ApiDao.StatusType.REJECT,
                            JSONObject(mainHashMap).toString()
                        )
                    if (success) {
                        requests.remove(mainHashMap)
                        removeFromSharedPrefs(rideId.toString(), context)
//                        mainHashMap = HashMap<String, String>()
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
        }

        button_driverview_rideignore.setOnClickListener {
            startActivity(Intent(context, DriverRideStatusActivity::class.java))
        }

        button_driverview_ridestatus.setOnClickListener {
            startActivity(Intent(context, DriverRideStatusActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        if (::mMap.isInitialized) {
            refreshRequests()
        }
    }

    override fun onStop() {
        super.onStop()
        if (::receiver.isInitialized) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        refreshRequests()
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

    private fun setStatusButton(status: Boolean, context: Context) {
        if (status) {
            button_driverview_togglestatus.backgroundTintList =
                ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.colorButtonGreen)
                )
            button_driverview_togglestatus.text = getString(R.string.driver_status_true)
        } else {
            button_driverview_togglestatus.backgroundTintList =
                ColorStateList.valueOf(
                    ContextCompat.getColor(context, R.color.colorButtonRed)
                )
            button_driverview_togglestatus.text = getString(R.string.driver_status_false)
        }
    }

    fun refreshRequests() {
        progress_driverview.visibility = View.VISIBLE
        driverLatLng = null
        motherLatLng = null
        destLatLng = null
        requests = getSavedRequests(context)
        updateViews()
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

    private fun updateViews() {
        mMap.clear()
        CoroutineScope(Dispatchers.IO + Job()).launch {
            mainHashMap.clear()
            if (requests.size > 0) {
                mainHashMap = requests[0]
                var uiHashMap = HashMap<String, String>()
                requests.forEach { request ->
                    val status = request["accepted"]
                    if (status != null) {
                        if (status == "false") {
                            val timeStamp = request["timestamp"]?.toLong()
                            if (timeStamp != null) {
                                if (timeStamp > (System.currentTimeMillis() - Constants.DEFAULT_WAIT_TIME)) {
                                    if (uiHashMap.isEmpty()) {
                                        uiHashMap = request
                                    }
                                } else {
                                    //remove if older than 10 minutes
                                    val removeId = request["ride_id"]
                                    if (removeId != null) {
                                        removeFromSharedPrefs(removeId, context)
                                    }
                                }
                            }
                        }
                    }
                }

                if (uiHashMap.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        for ((key, value) in uiHashMap) {

                            when (key) {
                                "hospital" -> println(key)
                                "name" -> text_driverview_name.text = value
                                "phone" -> text_driverview_phone.text = value
                                "price" -> text_driverview_fare.text = value
                                "ride_id" -> rideId = value.toLong()
                            }
                        }
                        toggleRequestVisibility(true)
                        progress_driverview.visibility = View.INVISIBLE
                    }
                    val ride = ApiDao.getRideById(rideId)
                    if (ride != null) {
                        var latLng = toLatLng(ride.start)
                        if (latLng != null) {
                            motherLatLng = latLng
                        }
                        latLng = toLatLng(ride.destination)
                        if (latLng != null) {
                            destLatLng = latLng
                        }
                        withContext(Dispatchers.Main) {
                            updateMap()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        toggleRequestVisibility(false)
                        progress_driverview.visibility = View.INVISIBLE
                    }

                }
            }
        }
    }
}
