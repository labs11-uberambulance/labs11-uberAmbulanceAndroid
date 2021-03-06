package com.jbseppanen.birthride

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

class Constants {
    companion object {
        const val DEFAULT_WAIT_TIME: Long = 600000  //10 minutes in ms
        val defaultMapCenter = LatLng(4.35, 32.5)
        val mapBounds = LatLngBounds(
            LatLng(-1.5, 29.55), LatLng(4.35, 34.6)
        )
    }
}