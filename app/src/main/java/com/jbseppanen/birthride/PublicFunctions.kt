package com.jbseppanen.birthride

import android.content.Context
import android.preference.PreferenceManager
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

fun refreshRequests(context: Context) {
    val requests = ArrayList<HashMap<String, String>>()
    val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    val rideIdsAsString =
        sharedPrefs.getString(PushNotificationService.STORED_REQUESTS_KEY, null)
    val map = HashMap<String, String>()
    if (rideIdsAsString != null) {
        val requestIds = rideIdsAsString.split(",")
        for (requestId in requestIds) {
            val requestData = sharedPrefs.getString(requestId, null)
            if (requestData != null) {
                val requestArray = requestData.split(",") as MutableList<String>
                requestArray.forEachIndexed { index, item ->
                    val itemArray = item.replace("{", "").replace("}", "").split("=")
                    map[itemArray[0].trim()] = itemArray[1]
                }
                requests.add(map)
            }
        }
    }
}


fun removeFromSharedPrefs(map: HashMap<String, String>, context: Context) {
    val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    val id = map["ride_id"] as String
    sharedPrefs.edit().remove(id).apply()
    val rideIds = sharedPrefs.getString(PushNotificationService.STORED_REQUESTS_KEY, null)
    if (rideIds != null) {
        val idArray = rideIds.split(",")
        val newIdArray = ArrayList<String>()
        idArray.forEach {
            if (it != id) {
                newIdArray.add(it)
            }
        }
        sharedPrefs.edit()
            .putString(
                PushNotificationService.STORED_REQUESTS_KEY,
                idArray.toString().removePrefix("[").removeSuffix("]")
            ).apply()
    }
}