package com.jbseppanen.birthride

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_ride_status.*
import kotlinx.coroutines.*
import java.util.ArrayList

class RideStatusActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ride_status)

        button_ridestatus_refresh.setOnClickListener {
            CoroutineScope(Dispatchers.IO + Job()).launch {
                val userRides: ArrayList<Ride> = ApiDao.getUserRides()
                if (userRides.size > 0) {
                    val rides =
                        userRides.sortedWith(compareBy { it.id }).reversed() as ArrayList<Ride>
                    withContext(Dispatchers.Main) {
                        text_ridestatus_status.text = rides[0].ride_status
                    }
                }
            }
        }
    }
}
