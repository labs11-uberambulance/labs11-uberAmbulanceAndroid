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

class EditAccountDetailsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var activity: Activity

    companion object {
        const val IMAGE_REQUEST_CODE = 47
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_account_details)
        val context: Context = this
        activity = this
        FirebaseApp.initializeApp(context)
//        AuthUI.getInstance().signOut(context).addOnCompleteListener { onStart() } //TODO remove this line after testing.  Currently forces login each time.
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

        image_edituser_driverimage.setOnClickListener {

            val imageIntent = Intent(Intent.ACTION_GET_CONTENT)
            imageIntent.type = "image/*"
            startActivityForResult(imageIntent, IMAGE_REQUEST_CODE)

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
            }
        }
    }
}

