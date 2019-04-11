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
            val dataJob = Job()
            val dataScope = CoroutineScope(Dispatchers.IO + dataJob)
            dataScope.launch {
                val resultUser = ApiDao.getCurrentUser()
                if (resultUser != null) {
                    user = resultUser
                    when {
                        user.userData.user_type == null -> {
                            startActivityForResult(
                                Intent(context, UserTypeSelectionActivity::class.java),
                                USER_TYPE_REQUEST_CODE
                            )
                        }

//                        else ->  gotoEditUser() //TODO remove this line of coded and uncomment out code below.  For demo purposes.

                        user.userData.user_type == UserTypeSelectionActivity.MOTHER -> startActivity(
                            Intent(
                                context,
//                                RideStatusActivity::class.java
                                        MotherOptionsActivity::class.java
                            )
                        )
                        user.userData.user_type == UserTypeSelectionActivity.DRIVER -> {
                            startActivity(Intent(context, DriverViewRequestsActivity::class.java))
    /*                        CoroutineScope(Dispatchers.IO + Job()).launch {
                                val user = ApiDao.getCurrentUser()
                                if (user != null) {
                                    val requestIntent =
                                        Intent(context, EditAccountDetailsActivity::class.java)
                                    val extra = Json.stringify(User.serializer(), user)
                                    requestIntent.putExtra(WelcomeActivity.USER_KEY, extra)
                                    withContext(Dispatchers.Main) {
                                        startActivity(requestIntent)
                                    }
                                }
                            }*/
                        }
                    }
                }
            }
        }

        button_welcome_next.setOnClickListener {
            startActivityForResult(
                Intent(this, FirebaseOauthActivity::class.java),
                AUTH_REQUEST_CODE
            )
        }
    }

    fun gotoEditUser() {
        val requestIntent = Intent(this, EditAccountDetailsActivity::class.java)
        val serializer: SerializationStrategy<User> = User.serializer()
        val extra = Json.stringify(serializer, user)
        requestIntent.putExtra(USER_KEY, extra)
        startActivity(requestIntent)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == USER_TYPE_REQUEST_CODE) {
                val userType = data?.getStringExtra(UserTypeSelectionActivity.USER_TYPE_KEY)
                user.userData.user_type = userType
                gotoEditUser()
            } else if (requestCode == AUTH_REQUEST_CODE) {
                recreate()
            }
        }
    }
}
