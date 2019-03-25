package com.jbseppanen.birthride

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_user_type_selection.*

class UserTypeSelectionActivity : AppCompatActivity() {

    companion object {
        const val USER_TYPE_KEY = "UserTypeKey"
        const val MOTHER = "Mother"
        const val DRIVER = "Driver"
        const val CARETAKER = "Caregiver"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_type_selection)

        button_usertype_selection_mother.setOnClickListener {
            val intent = Intent(this, EditAccountDetailsActivity::class.java)
            intent.putExtra(USER_TYPE_KEY,MOTHER)
            startActivity(intent)
        }

        button_usertype_selection_driver.setOnClickListener {
            val intent = Intent(this, EditAccountDetailsActivity::class.java)
            intent.putExtra(USER_TYPE_KEY, DRIVER)
            startActivity(intent)
        }

        button_usertype_selection_caregiver.setOnClickListener {
            val intent = Intent(this, EditAccountDetailsActivity::class.java)
            intent.putExtra(USER_TYPE_KEY, CARETAKER)
            startActivity(intent)
        }


    }
}
