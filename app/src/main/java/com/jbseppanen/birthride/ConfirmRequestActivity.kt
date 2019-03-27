package com.jbseppanen.birthride

import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.NumberPicker
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_confirm_request.*

class ConfirmRequestActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_request)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_requestconfirm) as SupportMapFragment
        mapFragment.getMapAsync(this)
        button_requestconfirm_send.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    RideStatusActivity::class.java
                )
            )
        }
        text_requestconfirm_waittime.setOnClickListener {
            val fragment = MinutesPicker()
            fragment.show(supportFragmentManager, "Minutes To Wait")
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.addMarker(MarkerOptions().position(Constants.defaultMapCenter).title("Marker in Uganda"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(Constants.defaultMapCenter))
    }


    class MinutesPicker : DialogFragment() {

        override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View? {
            val view: NumberPicker = NumberPicker(context)
            view.minValue = 0
            view.maxValue = 60
            return view
        }
    }


}
