package com.jbseppanen.birthride

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.android.synthetic.main.activity_confirm_request.*
import kotlinx.coroutines.*


class ConfirmRequestActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private var markerPoints = ArrayList<LatLng>()
    private lateinit var activity: ConfirmRequestActivity


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_request)
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_requestconfirm) as SupportMapFragment
        mapFragment.getMapAsync(this)
        activity = this
        val context: Context = this
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)


        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) run {
            Toast.makeText(context, "Need to grant permission to use location.", Toast.LENGTH_SHORT)
                .show()
            return
        } else {
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                mMap.animateCamera(CameraUpdateFactory.newLatLng(
//                    LatLng(location.latitude,location.longitude)
                    Constants.defaultMapCenter //Todo remove this hardcoded location
                ), 2000, object : GoogleMap.CancelableCallback {
                    override fun onFinish() {
                        mMap.animateCamera(
                            CameraUpdateFactory.zoomTo(10f),
                            2000,
                            object : GoogleMap.CancelableCallback {
                                override fun onFinish() {}
                                override fun onCancel() {}
                            })
                    }

                    override fun onCancel() {
                    }
                })
            }

        }

        button_requestconfirm_send.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    RideStatusActivity::class.java
                )
            )
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
//        mMap.addMarker(MarkerOptions().position(Constants.defaultMapCenter).title("Marker in Uganda"))
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(Constants.defaultMapCenter))
        CoroutineScope(Dispatchers.IO + Job()).launch {
            val user = ApiDao.getCurrentUser()
            if (user?.userData?.location != null) {
                var split: List<String>? = user.userData.location?.latlng?.split(",")
                if (split != null && split.size == 2) {
                    val userLatLng: LatLng? = LatLng(split[0].toDouble(), split[1].toDouble())
                    if (userLatLng != null) {
                        withContext(Dispatchers.Main) {
                            setPoint(userLatLng)
                        }
                    }
                }
                split = user.motherData?.destination?.latlng?.split(",")
                if (split != null && split.size == 2) {
                    val userLatLng: LatLng? = LatLng(split[0].toDouble(), split[1].toDouble())
                    if (userLatLng != null) {
                        withContext(Dispatchers.Main) {
                            setPoint(userLatLng)
                        }
                    }
                }
            }
        }

        mMap.setOnMapClickListener { latLng ->
            setPoint(latLng)
        }
    }

    private fun setPoint(latLng: LatLng) {

        if (markerPoints.size > 1) {
            markerPoints.clear()
            mMap.clear()
        }
        markerPoints.add(latLng)

        // Creating MarkerOptions
        val options = MarkerOptions()

        // Setting the position of the marker
        if (markerPoints.size == 1) {
            options.position(latLng).title("Start")
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            updateDrivers(latLng)
        } else if (markerPoints.size == 2) {
            options.position(latLng).title("End")
            options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        }

        // Add new marker to the Google Map Android API V2
        mMap.addMarker(options)

        // Checks, whether start and end locations are captured
        if (markerPoints.size >= 2) {
            val origin = markerPoints[0]
            val dest = markerPoints[1]
            CoroutineScope(Dispatchers.IO + Job()).launch {
                val path = ApiDao.getDirections(activity, origin, dest)
                withContext(Dispatchers.Main) {
                    for (i in 0 until path.size) {
                        mMap.addPolyline(PolylineOptions().addAll(path[i]).width(5f).color(Color.RED))
                    }
                }
            }
        }
    }

    fun updateDrivers(location: LatLng) {
        CoroutineScope(Dispatchers.IO + Job()).launch {
            val drivers: ArrayList<RequestedDriver> =
                ApiDao.getDrivers(LatLng(location.latitude, location.longitude))
            if (drivers.size > 0) {
                val driver = drivers[0]
                withContext(Dispatchers.Main) {
                    text_requestconfirm_pickuptime.text = driver.duration.time
                    text_requestconfirm_fare.text = "${driver.driver.price.toString()} USh"
                    text_requestconfirm_waittime.text = "10 mins"
                }
            }
        }
    }


/*    class MinutesPicker : DialogFragment() {
      text_requestconfirm_waittime.setOnClickListener {
            val fragment = MinutesPicker()
            fragment.show(supportFragmentManager, "Minutes To Wait")
        }

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view: NumberPicker = NumberPicker(context)
            view.minValue = 0
            view.maxValue = 60
            return view
        }
    }*/

}
