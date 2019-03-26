package com.jbseppanen.birthride

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_request_ride.*

class RequestRideActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_ride)

        button_requestride_request.setOnClickListener { startActivity(Intent(this, ConfirmRequestActivity::class.java)) }
        button_requestride_search.setOnClickListener { startActivity(Intent(this, DriverSearchActivity::class.java)) }

    }
}
