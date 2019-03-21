package com.jbseppanen.birthride

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup

class EditAccountDetailsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_account_details)

        val parent = findViewById<ViewGroup>(R.id.layout_edituser)
        val userType = "Mother"

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

    }
}

