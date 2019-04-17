package com.jbseppanen.birthride

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.Toast
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import kotlinx.android.synthetic.main.activity_driver_ride_status.*
import kotlinx.coroutines.*

class DriverRideStatusActivity : MainActivity(), OnMapReadyCallback {

    companion object {
        const val DRIVER_RIDE_STATUS_KEY = "Driver ride status key"
    }

    private lateinit var context: Context
    var rideId = -1L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_ride_status)

        context = this
        val frameLayout: FrameLayout = findViewById(R.id.content_frame)
        frameLayout.addView(
            LayoutInflater.from(context).inflate(
                R.layout.activity_driver_view_requests,
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

    }

    override fun onMapReady(p0: GoogleMap?) {

    }


}
