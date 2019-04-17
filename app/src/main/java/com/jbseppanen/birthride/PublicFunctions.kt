package com.jbseppanen.birthride

import com.google.android.gms.maps.model.LatLng

fun toLatLng(latLngString: String): LatLng? {
    var latLng: LatLng?
    try {
        latLng = LatLng(
            latLngString.split(",")[0].toDouble(),
            latLngString.split(",")[1].toDouble()
        )
    } catch (e: IndexOutOfBoundsException) {
        latLng = null
    }
    return latLng
}