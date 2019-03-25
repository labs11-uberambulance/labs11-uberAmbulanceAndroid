package com.jbseppanen.birthride

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_edit_account_details.*

class EditAccountDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_account_details)

        val parent = findViewById<ViewGroup>(R.id.layout_edituser)
        val userType = intent.getStringExtra(UserTypeSelectionActivity.USER_TYPE_KEY)

        for (i in 0 until parent.childCount) {
            val childView = parent.getChildAt(i)
            if (childView.tag!=null) {
                when {
                    childView.tag.toString().contains(userType, ignoreCase = true) -> {
                        childView.visibility = View.VISIBLE
                    }
                }
            }
        }

        button_edituser_save.setOnClickListener { startActivity(Intent(this, RequestRideActivity::class.java)) }
    }
}

