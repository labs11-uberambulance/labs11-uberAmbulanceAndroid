package com.jbseppanen.birthride

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import kotlinx.android.synthetic.main.activity_driver_ride_status.*
import kotlinx.android.synthetic.main.activity_driver_view_requests.*
import kotlinx.coroutines.*

class DriverRideStatusActivity : MainActivity(), OnMapReadyCallback {

    companion object {
        const val DRIVER_RIDE_STATUS_KEY = "Driver ride status key"
    }

    private lateinit var mMap: GoogleMap
    private lateinit var context: Context
    var rideId = -1L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        context = this
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

        val data =
            intent.getSerializableExtra(PushNotificationService.SERVICE_MESSAGE_KEY) as HashMap<*, *>?

        if (data != null) {
            for ((key, value) in data) {
                if (key is String && value is String) {
                    when (key) {
                        "hospital" -> text_driverstatus_dropoffplace.text = value
                        "name" -> text_driverstatus_name.text = value
                        "phone" -> text_driverstatus_phone.text = value
                        "price" -> text_driverstatus_fare.text = value
                        "ride_id" -> rideId = value.toLong()
                    }
                }
            }
        }

        button_driverstatus_pickup.setOnClickListener {
            updateStatus(ApiDao.StatusType.PICKUP)
        }

        button_driverstatus_dropoff.setOnClickListener {
            updateStatus(ApiDao.StatusType.DROPOFF)
        }

        button_driverstatus_directions.setOnClickListener {
            val uri = Uri.parse("google.navigation:q=40.763500,-73.979305")
            val directionsIntent = Intent(Intent.ACTION_VIEW, uri)
            directionsIntent.setPackage("com.google.android.apps.maps")
            startActivity(directionsIntent)
        }

        button_test.setOnClickListener {
            getRideInfo()
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
/*        var userLocation: Location? = user?.userData?.location
        if (userLocation != null) {
            setPoint(userLocation.asLatLng(), DriverViewRequestsActivity.PointType.PICKUP)
        }

        userLocation = user?.motherData?.destination
        if (userLocation != null) {
            setPoint(userLocation.asLatLng(), DriverViewRequestsActivity.PointType.DROPOFF)
        }*/
    }

    private fun updateStatus(status: ApiDao.StatusType) {
        if (!rideId.equals(-1)) {
            CoroutineScope(Dispatchers.IO + Job()).launch {
                val success = ApiDao.updateRideStatus(rideId, status, null)
                val message = if (success) {
                    "Ride updated!"
                } else {
                    "Failed to update ride status."
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    fun getRideInfo() {
//        rideId = 56
        if (rideId != -1L) {
            CoroutineScope(Dispatchers.IO + Job()).launch {
                ApiDao.getRideById(rideId)
            }
        }

    }
}
