package com.jbseppanen.birthride

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_welcome.*
import kotlinx.coroutines.*
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

class WelcomeActivity : AppCompatActivity() {

    companion object {
        const val AUTH_REQUEST_CODE = 4
        const val USER_TYPE_REQUEST_CODE = 5
        const val USER_KEY = "User Type"
        const val LOCATION_REQUEST_CODE = 1
    }

    private lateinit var context: Context
    private lateinit var user: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        context = this

        if ((ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
                    ) && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_DENIED
        ) {
            Toast.makeText(context, "Need to grant permission to use location.", Toast.LENGTH_SHORT)
                .show()
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_REQUEST_CODE
            )
        }

        if (FirebaseAuth.getInstance().currentUser != null) {
            CoroutineScope(Dispatchers.IO + Job()).launch {
                val resultUser = ApiDao.getCurrentUser()
                if (resultUser != null) {
                    user = resultUser
                    withContext(Dispatchers.Main) {
                        userTypeRedirect()
                    }
                }
            }
        } else {
            button_welcome_next.visibility = View.VISIBLE
            progress_welcome.visibility = View.INVISIBLE
        }


        button_welcome_next.setOnClickListener {
            progress_welcome.visibility = View.VISIBLE
            button_welcome_next.visibility = View.INVISIBLE
            startActivityForResult(
                Intent(this, FirebaseOauthActivity::class.java),
                AUTH_REQUEST_CODE
            )
        }
    }

    fun userTypeRedirect() {
        when {
            user.userData.user_type == null -> {
                startActivityForResult(
                    Intent(context, UserTypeSelectionActivity::class.java),
                    USER_TYPE_REQUEST_CODE
                )
            }
            user.userData.user_type == UserTypeSelectionActivity.MOTHER -> {
                startActivity(Intent(context, MotherOptionsActivity::class.java))
                finish()
            }
            user.userData.user_type == UserTypeSelectionActivity.DRIVER -> {
                startActivity(Intent(context, DriverViewRequestsActivity::class.java))
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == USER_TYPE_REQUEST_CODE) {
                val userType = data?.getStringExtra(UserTypeSelectionActivity.USER_TYPE_KEY)
                user.userData.user_type = userType
                val requestIntent = Intent(this, EditAccountDetailsActivity::class.java)
                val extra = Json.stringify(User.serializer(), user)
                requestIntent.putExtra(USER_KEY, extra)
                startActivity(requestIntent)
                finish()
            } else if (requestCode == AUTH_REQUEST_CODE) {
                CoroutineScope(Dispatchers.IO + Job()).launch {
                    val resultUser = ApiDao.getCurrentUser()
                    if (resultUser != null) {
                        user = resultUser
                        withContext(Dispatchers.Main) {
                            userTypeRedirect()
                        }
                    }
                }
            }
        }
    }
}