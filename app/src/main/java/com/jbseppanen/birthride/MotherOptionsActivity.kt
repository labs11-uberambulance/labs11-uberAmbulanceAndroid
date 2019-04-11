package com.jbseppanen.birthride

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.activity_request_ride.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

class MotherOptionsActivity : MainActivity() {

    private lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        context = this
        val frameLayout: FrameLayout = findViewById(R.id.content_frame)
        frameLayout.addView(
            LayoutInflater.from(context).inflate(
                R.layout.activity_request_ride,
                null
            )
        )
        super.onCreateDrawer()

        button_requestride_request.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ConfirmRequestActivity::class.java
                )
            )
        }
        button_requestride_editprofile.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    RideStatusActivity::class.java
                )
            )
        }
    }
}
