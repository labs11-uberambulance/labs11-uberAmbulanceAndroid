package com.jbseppanen.birthride

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.activity_mother_options.*

class MotherOptionsActivity : MainActivity() {

    private lateinit var context: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        context = this
        val frameLayout: FrameLayout = findViewById(R.id.content_frame)
        frameLayout.addView(
            LayoutInflater.from(context).inflate(
                R.layout.activity_mother_options,
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
