package com.jbseppanen.birthride

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.activity_ride_status.*
import kotlinx.coroutines.*
import java.util.ArrayList

class MotherRideStatusActivity : MainActivity() {

    private var refreshing = true
    private lateinit var context:Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        context = this
        val frameLayout: FrameLayout = findViewById(R.id.content_frame)
        frameLayout.addView(
            LayoutInflater.from(context).inflate(
                R.layout.activity_ride_status,
                null
            )
        )
        super.onCreateDrawer()

        button_ridestatus_refresh.setOnClickListener {
            updateStatus()
        }
    }

    private fun updateStatus() {
        CoroutineScope(Dispatchers.IO + Job()).launch {
            withContext(Dispatchers.Main) {
                progress_ridestatus.visibility = View.VISIBLE
            }
            val userRides: ArrayList<Ride> = ApiDao.getUserRides()
            var statusText = "No rides found"
            if (userRides.size > 0) {
                val rides =
                    userRides.sortedWith(compareBy { it.id }).reversed() as ArrayList<Ride>
                statusText = rides[0].ride_status.replace("_", " ").capitalize()
            }
            withContext(Dispatchers.Main) {
                text_ridestatus_status.text = statusText
                progress_ridestatus.visibility = View.INVISIBLE
            }
        }
    }

    private fun autoRefresh() {
        refreshing = true
        CoroutineScope(Dispatchers.IO + Job()).launch {
            while (refreshing) {
                updateStatus()
                // auto update once a minute while on this screen.
                delay(60000)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        refreshing = false
    }

    override fun onResume() {
        super.onResume()
        autoRefresh()
    }
}
