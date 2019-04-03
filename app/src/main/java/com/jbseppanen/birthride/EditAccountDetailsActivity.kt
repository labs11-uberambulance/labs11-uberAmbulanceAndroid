package com.jbseppanen.birthride

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_edit_account_details.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

class EditAccountDetailsActivity : AppCompatActivity() {

    private lateinit var activity: Activity
    private lateinit var context: Context
    private lateinit var user:User

    companion object {
        const val IMAGE_REQUEST_CODE = 3
        const val LOCATION_REQUEST_CODE = 10

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_account_details)

        activity = this
        context = this

        val userString = intent.getStringExtra(WelcomeActivity.USER_KEY)
        user = Json.nonstrict.parse(User.serializer(), userString)
        var userType = user.userData.user_type
        if (userType.equals(UserTypeSelectionActivity.CAREGIVER)) {
            user.userData.user_type = UserTypeSelectionActivity.MOTHER
        } else if (user.userData.user_type == UserTypeSelectionActivity.MOTHER) {
            if (user.motherData != null) {
                if (user.motherData!!.caretaker_name != "") {
                    userType = UserTypeSelectionActivity.CAREGIVER
                }
            }
        }
        val parent = findViewById<ViewGroup>(R.id.layout_edituser)
        for (i in 0 until parent.childCount) {
            val childView = parent.getChildAt(i)
            if (childView.tag != null) {
                when {
                    childView.tag.toString().contains(userType!!, ignoreCase = true) -> {
                        childView.visibility = View.VISIBLE
                    }
                }
            }
        }


        image_edituser_driverimage.setOnClickListener {
            val imageIntent = Intent(Intent.ACTION_GET_CONTENT)
            imageIntent.type = "image/*"
            startActivityForResult(imageIntent, IMAGE_REQUEST_CODE)
        }

        var newUser = false
        if (user.motherData == null && user.driverData == null) {
            newUser = true
        }


        edit_edituser_name.setText(user.userData.name)
//        edit_edituser_city.setText(user.userData.village)
//        edit_edituser_address.setText(user.userData.address)
        edit_edituser_phone.setText(user.userData.phone)
//        edit_edituser_email.setText(user.userData.email)
        when (user.userData.user_type) {
            UserTypeSelectionActivity.MOTHER -> {
                if (user.motherData == null) {
                    user.motherData = MotherData(firebase_id = user.userData.firebase_id)
                }
                edit_edituser_caregivername.setText(user.motherData?.caretaker_name)
//                edit_edituser_hospitalname.setText(user.motherData?.hospital)
/*                val dateArray = user.motherData?.due_date?.split("-")
                if (dateArray != null) {
                    if (dateArray.size >= 3) {
                        date_edituser_duedate.updateDate(
                            dateArray[0].toInt(),
                            dateArray[1].toInt() - 1,
                            dateArray[2].substring(0, 2).toInt()
                        )
                    }
                }*/
            }
            UserTypeSelectionActivity.DRIVER -> {
                if (user.driverData == null) {
                    user.driverData = DriverData(firebase_id = user.userData.firebase_id)
                }
                if (user.driverData?.price.toString() != "0") {
                    edit_edituser_driverprice.setText(user.driverData?.price.toString())
                }
                edit_edituser_driverbio.setText(user.driverData?.bio)
            }
        }

        button_edituser_pick.setOnClickListener {
            startActivityForResult(Intent(context, LocationSelectionActivity::class.java), LOCATION_REQUEST_CODE)

        }

        button_edituser_save.setOnClickListener {
//            user.userData.address = edit_edituser_address.text.toString()
//            user.userData.email = edit_edituser_email.text.toString()
            user.userData.name = edit_edituser_name.text.toString()
            user.userData.phone = edit_edituser_phone.text.toString()
//            user.userData.village = edit_edituser_city.text.toString()
            when (user.userData.user_type) {
                UserTypeSelectionActivity.MOTHER -> {
                    user.motherData?.caretaker_name = edit_edituser_caregivername.text.toString()
//                    user.motherData?.due_date = "${date_edituser_duedate.year}-${date_edituser_duedate.month + 1}-${date_edituser_duedate.dayOfMonth}"
//                    user.motherData?.hospital = edit_edituser_hospitalname.text.toString()
                }
                UserTypeSelectionActivity.DRIVER -> {
                    user.driverData?.price = edit_edituser_driverprice.text.toString().toDouble().toInt()
                    user.driverData?.bio = edit_edituser_driverbio.text.toString()
                }
            }

            val dataJob = Job()
            val dataScope = CoroutineScope(Dispatchers.IO + dataJob)
            dataScope.launch {
                val success = ApiDao.updateCurrentUser(user, newUser)
                if (success) {
                    when (user.userData.user_type) {
                        UserTypeSelectionActivity.MOTHER -> {
                            startActivity(Intent(context, RequestRideActivity::class.java))
                        }
                        UserTypeSelectionActivity.DRIVER -> {
                            startActivity(Intent(context, DriverViewRequestsActivity::class.java))
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Failed to save. Go back and try log in again.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
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
            } else if (requestCode == LOCATION_REQUEST_CODE) {
                val locations = data?.extras?.getParcelableArrayList<LatLng>(LocationSelectionActivity.LOCATIONS_KEY)
                if(locations!=null) {
                    user.userData.location = Location(null, "${locations[0].latitude},${locations[0].longitude}",null)
                    user.motherData?.destination = Location(null, "${locations[1].latitude},${locations[1].longitude}",null)
                }
            }
        }
    }
}


