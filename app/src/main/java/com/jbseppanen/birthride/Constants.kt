package com.jbseppanen.birthride

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

class Constants {
    companion object {
        val defaultMapCenter = LatLng(1.079695, 33.366965)
        val mapBounds = LatLngBounds(
            LatLng(-1.5, 29.55), LatLng(4.35, 34.6)
        )
//            { north: 4.35, south: -1.5, west: 29.55, east: 34.6}
    }
}