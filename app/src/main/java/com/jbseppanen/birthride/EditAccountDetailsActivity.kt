package com.jbseppanen.birthride

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_edit_account_details.*

class EditAccountDetailsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_account_details)
        val context:Context = this
        FirebaseApp.initializeApp(context)
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            startActivity(Intent(this, FirebaseOauthActivity::class.java))
        }

        val parent = findViewById<ViewGroup>(R.id.layout_edituser)
        val userType = intent.getStringExtra(UserTypeSelectionActivity.USER_TYPE_KEY)

        for (i in 0 until parent.childCount) {
            val childView = parent.getChildAt(i)
            if (childView.tag != null) {
                when {
                    childView.tag.toString().contains(userType, ignoreCase = true) -> {
                        childView.visibility = View.VISIBLE
                    }
                }
            }
        }

        button_edituser_save.setOnClickListener {
            if (userType.equals("driver", true)) {
                startActivity(Intent(this, DriverViewRequestsActivity::class.java))
            } else {
                startActivity(Intent(this, RequestRideActivity::class.java))
            }
        }
    }
}

