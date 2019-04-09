package com.jbseppanen.birthride

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_ride_status.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class RideStatusActivity : AppCompatActivity() {

    companion object {
        const val RIDE_ID_KEY = "Ride Id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ride_status)


        var motherId = intent.getLongExtra(RIDE_ID_KEY, -1L)

        //Temp
        if(motherId == -1L) {
            CoroutineScope(Dispatchers.IO + Job()).launch {
                motherId = ApiDao.getCurrentUser()!!.userData.id
            }
            }

        button_ridestatus_refresh.setOnClickListener {
            if (motherId != -1L) {
                CoroutineScope(Dispatchers.IO + Job()).launch {
                   ApiDao.getUserRides(motherId)
                }
            }
        }
    }
}
