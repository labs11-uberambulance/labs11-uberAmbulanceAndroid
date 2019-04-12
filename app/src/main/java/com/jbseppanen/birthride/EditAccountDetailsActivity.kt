package com.jbseppanen.birthride

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.android.gms.maps.model.LatLng
import kotlinx.android.synthetic.main.activity_edit_account_details.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json


class EditAccountDetailsActivity : AppCompatActivity() {

    private lateinit var activity: Activity
    private lateinit var context: Context
    private lateinit var user: User
    private var updatePhoto = false
    private var newUser = false

    companion object {
        const val IMAGE_REQUEST_CODE = 3
        const val LOCATION_REQUEST_CODE = 10

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_account_details)

        activity = this
        context = this

        val userString: String? = intent.getStringExtra(WelcomeActivity.USER_KEY)
        if (userString != null) {
            user = Json.nonstrict.parse(User.serializer(), userString)
        } else {
            CoroutineScope(Dispatchers.IO + Job()).launch {
                val returnedUser = ApiDao.getCurrentUser()
                if (returnedUser != null) {
                    user = returnedUser
                } else {
                    startActivity(Intent(context, WelcomeActivity::class.java))
                    finish()
                }
            }
            while (!::user.isInitialized) {
                Thread.sleep(50)
            }
        }
        progress_edituser.visibility = View.INVISIBLE
        ccp_edituser_ccp.registerCarrierNumberEditText(edit_edituser_phone)

        var userType = user.userData.user_type
        if (userType == UserTypeSelectionActivity.CAREGIVER) {
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

        if (userType == UserTypeSelectionActivity.DRIVER) {
            button_edituser_pick.text = "Pick home location"
        } else {
            button_edituser_pick.text = "Pick home and dropoff locations"
        }


        image_edituser_driverimagetext.setOnClickListener {
            getImage()
        }

        image_edituser_driverimage.setOnClickListener {
            getImage()
        }

        edit_edituser_name.setText(user.userData.name)
        if (user.userData.phone != null) {
            ccp_edituser_ccp.fullNumber = user.userData.phone
        }
        when (user.userData.user_type) {
            UserTypeSelectionActivity.MOTHER -> {
                if (user.motherData == null) {
                    newUser = true
                    user.motherData = MotherData(firebase_id = user.userData.firebase_id)
                }
                edit_edituser_caregivername.setText(user.motherData?.caretaker_name)
            }
            UserTypeSelectionActivity.DRIVER -> {
                if (user.driverData == null) {
                    newUser = true
                    user.driverData = DriverData(firebase_id = user.userData.firebase_id)
                } else {
                    edit_edituser_driverbio.setText(user.driverData?.bio)
                    val photoUrl = user.driverData?.photo_url
                    if (photoUrl != null) {
                        Glide
                            .with(this)
                            .load(photoUrl)
                            .apply(RequestOptions().centerCrop())
                            .listener(object : RequestListener<Drawable> {
                                override fun onLoadFailed(
                                    e: GlideException?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    return false
                                }

                                override fun onResourceReady(
                                    resource: Drawable?,
                                    model: Any?,
                                    target: Target<Drawable>?,
                                    dataSource: DataSource?,
                                    isFirstResource: Boolean
                                ): Boolean {
                                    return false
                                }
                            })
                            .into(image_edituser_driverimage)
                        image_edituser_driverimage.visibility = View.VISIBLE
                        image_edituser_driverimagetext.visibility = View.GONE
                    }
                }
                if (user.driverData?.price.toString() != "0") {
                    edit_edituser_driverprice.setText(user.driverData?.price.toString())
                }

            }
        }

        button_edituser_pick.setOnClickListener {
            val requestIntent = Intent(context, LocationSelectionActivity::class.java)
            val markerPoints = ArrayList<LatLng>()
            var userLocation: Location? = user.userData.location
            if (userLocation != null) {
                markerPoints.add(userLocation.asLatLng())
            }
            if (user.userData.user_type == UserTypeSelectionActivity.DRIVER) {
                requestIntent.putExtra(LocationSelectionActivity.INPUT_NUMBER_OF_POINTS_KEY, 1)
            } else {
/*                userLocation = user.motherData?.start
                if (userLocation != null) {
                    markerPoints.add(userLocation.asLatLng())
                }*/
                userLocation = user.motherData?.destination
                if (userLocation != null) {
                    markerPoints.add(userLocation.asLatLng())
                }
                requestIntent.putExtra(LocationSelectionActivity.INPUT_NUMBER_OF_POINTS_KEY, 2)
            }
            requestIntent.putExtra(LocationSelectionActivity.INPUT_POINTS_KEY, markerPoints)
            startActivityForResult(
                requestIntent,
                LOCATION_REQUEST_CODE
            )
        }

        button_edituser_save.setOnClickListener {
            progress_edituser.visibility = View.VISIBLE
            user.userData.name = edit_edituser_name.text.toString()
            user.userData.phone = ccp_edituser_ccp.fullNumberWithPlus
            when (user.userData.user_type) {
                UserTypeSelectionActivity.MOTHER -> {
                    user.motherData?.caretaker_name = edit_edituser_caregivername.text.toString()
//                    user.motherData?.due_date = "${date_edituser_duedate.year}-${date_edituser_duedate.month + 1}-${date_edituser_duedate.dayOfMonth}"
//                    user.motherData?.hospital = edit_edituser_hospitalname.text.toString()
                    updateUser()
                }
                UserTypeSelectionActivity.DRIVER -> {
                    user.driverData?.price =
                        edit_edituser_driverprice.text.toString().toDouble().toInt()
                    user.driverData?.bio = edit_edituser_driverbio.text.toString()
                    if (updatePhoto) {
                        val imageView: ImageView = image_edituser_driverimage
                        val bitmap = (imageView.drawable as BitmapDrawable).bitmap
                        val photoUrl = user.driverData?.photo_url
                        ApiDao.uploadDriverPhoto(bitmap, photoUrl, object : ResultCallback {
                            override fun returnResult(result: String?) {
                                if (result != null) {
                                    user.driverData?.photo_url = result
                                    updateUser()
                                }
                            }
                        })
                    } else {
                        updateUser()
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
                    image_edituser_driverimage.setImageDrawable(drawable)
                    image_edituser_driverimage.visibility = View.VISIBLE
                    image_edituser_driverimagetext.visibility = View.GONE
                    updatePhoto = true
                }
            } else if (requestCode == LOCATION_REQUEST_CODE) {
                val locations =
                    data?.extras?.getParcelableArrayList<LatLng>(LocationSelectionActivity.RETURN_POINTS_KEY)
                if (locations != null) {
                    user.userData.location =
                        Location(null, "${locations[0].latitude},${locations[0].longitude}", null)
                    if (locations.size > 1) {
                        user.motherData?.destination =
                            Location(
                                null,
                                "${locations[1].latitude},${locations[1].longitude}",
                                null
                            )
                    }
                }
            }
        }
    }

    fun getImage() {
        val imageIntent = Intent(Intent.ACTION_GET_CONTENT)
        imageIntent.type = "image/*"
        startActivityForResult(imageIntent, IMAGE_REQUEST_CODE)
    }

    fun updateUser() {
        CoroutineScope(Dispatchers.IO + Job()).launch {
            val success = ApiDao.updateCurrentUser(user, newUser)
            if (success) {
                when (user.userData.user_type) {
                    UserTypeSelectionActivity.MOTHER -> {
                        startActivity(Intent(context, MotherOptionsActivity::class.java))
                    }
                    UserTypeSelectionActivity.DRIVER -> {
                        startActivity(Intent(context, DriverViewRequestsActivity::class.java))
                    }
                }
                finish()
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Failed to save. Try again or log out and then retry.",
                        Toast.LENGTH_LONG
                    ).show()
                    progress_edituser.visibility = View.INVISIBLE
                }
            }
        }
    }
}


