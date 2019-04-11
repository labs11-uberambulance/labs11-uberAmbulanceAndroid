package com.jbseppanen.birthride

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.firebase.ui.auth.AuthUI
import kotlinx.android.synthetic.main.activity_user_type_selection.*

class UserTypeSelectionActivity : AppCompatActivity() {

    companion object {
        const val USER_TYPE_KEY = "UserTypeKey"
        const val MOTHER = "mothers"
        const val DRIVER = "drivers"
        const val CAREGIVER = "caregivers"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_type_selection)

        val intent = Intent()

        button_usertype_selection_mother.setOnClickListener {
            intent.putExtra(USER_TYPE_KEY,MOTHER)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        button_usertype_selection_driver.setOnClickListener {
            intent.putExtra(USER_TYPE_KEY, DRIVER)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        button_usertype_selection_caregiver.setOnClickListener {
            intent.putExtra(USER_TYPE_KEY, CAREGIVER)
            setResult(Activity.RESULT_OK, intent)
            finish()
        }

        button_usertype_logout.setOnClickListener {
            AuthUI.getInstance().signOut(this).addOnCompleteListener {
                startActivity(Intent(this, WelcomeActivity::class.java))
                finish()
            }
        }
    }
}
