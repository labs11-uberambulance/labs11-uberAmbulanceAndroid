package com.jbseppanen.birthride

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import kotlinx.android.synthetic.main.activity_driver_details.*
import kotlinx.serialization.json.Json

class DriverDetailsActivity : AppCompatActivity() {

    companion object {
        const val DRIVER_DETAILS_KEY = "Driver Details"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_details)

        val driverString = intent.getStringExtra(DRIVER_DETAILS_KEY)
        val requestedDriver = Json.nonstrict.parse(RequestedDriver.serializer(), driverString)
        text_driverdetails_name.text = requestedDriver.driver.name
        text_driverdetails_phone.text = requestedDriver.driver.phone
        text_driverdetails_bio.text = requestedDriver.driver.bio

            Glide
                .with(this)
                .load(requestedDriver.driver.photo_url)
                .apply(RequestOptions().centerCrop())
                .listener(object: RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
//                                    Toast.makeText(applicationContext, e?.localizedMessage, Toast.LENGTH_LONG).show()
                        return false
                    }
                    override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                        return false
                    }
                })
                .into(image_driverdetails_driver)
        }
}
