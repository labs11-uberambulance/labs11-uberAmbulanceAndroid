package com.jbseppanen.birthride

import android.content.Context
import android.preference.PreferenceManager
import com.google.android.gms.maps.model.LatLng
import kotlin.random.Random

fun toLatLng(latLngString: String): LatLng? {
    var latLng: LatLng?
    try {
        latLng = LatLng(
            latLngString.split(",")[0].toDouble(),
            latLngString.split(",")[1].toDouble()
        )
    } catch (e: Exception) {
        latLng = null
    }
    return latLng
}

fun getSavedRequests(context: Context): ArrayList<HashMap<String, String>> {
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
    return requests
}

fun getSavedRequestById(context: Context, requestId: Long): HashMap<String, String> {
    val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    val requestData = sharedPrefs.getString(requestId.toString(), null)
    val map = HashMap<String, String>()
    if (requestData != null) {
        val requestArray = requestData.split(",") as MutableList<String>
        requestArray.forEachIndexed { index, item ->
            val itemArray = item.replace("{", "").replace("}", "").split("=")
            map[itemArray[0].trim()] = itemArray[1]
        }
    }
    return map
}


fun removeFromSharedPrefs(id: String, context: Context) {
    val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context)

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

fun generateMockLocations():LatLng {
    val lat = Random.nextDouble(Constants.mapBounds.southwest.latitude, Constants.mapBounds.northeast.latitude)
    val lon = Random.nextDouble(Constants.mapBounds.southwest.longitude, Constants.mapBounds.northeast.longitude)
    return LatLng(lat, lon)
}