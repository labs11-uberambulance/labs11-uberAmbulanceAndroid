package com.jbseppanen.birthride

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import com.firebase.ui.auth.AuthUI
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_edit_account_details.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class EditAccountDetailsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var activity: Activity

    companion object {
        const val IMAGE_REQUEST_CODE = 3
        const val AUTH_REQUEST_CODE = 4
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_account_details)
        val context: Context = this
        activity = this
        FirebaseApp.initializeApp(context)
        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            AuthUI.getInstance().signOut(context).addOnCompleteListener {
                auth = FirebaseAuth.getInstance()
                if (auth.currentUser == null) {
                    startActivityForResult(
                        Intent(this, FirebaseOauthActivity::class.java),
                        AUTH_REQUEST_CODE
                    )
                }
            } //TODO remove this line after testing.  Currently forces login each time.
        } else {
            getUserInfo()
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

        image_edituser_driverimage.setOnClickListener {

            val imageIntent = Intent(Intent.ACTION_GET_CONTENT)
            imageIntent.type = "image/*"
            startActivityForResult(imageIntent, IMAGE_REQUEST_CODE)

        }
    }

    fun getUserInfo() {

        val dataJob = Job()
        val dataScope = CoroutineScope(Dispatchers.IO + dataJob)
        dataScope.launch {
            val user = ApiDao.getCurrentUser()
            edit_edituser_name.setText(user?.userData?.name)
            edit_edituser_city.setText(user?.userData?.village)
            edit_edituser_address.setText(user?.userData?.address)
            edit_edituser_phone.setText(user?.userData?.phone)
            edit_edituser_email.setText(user?.userData?.email)
            edit_edituser_email.setText(user?.userData?.email)
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == IMAGE_REQUEST_CODE) {
                val imageUri = data!!.data
                if (imageUri != null) {
                    val inputStream = activity.contentResolver.openInputStream(imageUri)
                    val drawable = Drawable.createFromStream(inputStream, imageUri.toString())
                    image_edituser_driverimage.background = drawable
                    image_edituser_driverimage.text = ""
                }
            } else if (requestCode == AUTH_REQUEST_CODE) {
                getUserInfo()
/*                    val result = NetworkAdapter.httpRequest(
                        stringUrl = "https://birthrider-backend.herokuapp.com/api/users",
                        requestType = NetworkAdapter.GET,
                        jsonBody = null,
                        headerProperties = mapOf(
                            "Authorization" to "$tokenString",
                            "Content-Type" to "application/json",
                            "Accept" to "application/json"
                        )
                    )*/
            }
        }
    }
}


