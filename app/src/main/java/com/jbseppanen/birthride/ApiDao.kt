package com.jbseppanen.birthride

import android.app.Activity
import android.graphics.Bitmap
import android.support.annotation.WorkerThread
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.PolyUtil
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject


const val baseUrl = "https://birthrider-backend.herokuapp.com/api"

@WorkerThread
object ApiDao {

    fun getCurrentUser(): User? {
        val tokenString = getToken()
        val (success, result) = NetworkAdapter.httpRequest(
            stringUrl = "$baseUrl/users",
            requestType = NetworkAdapter.GET,
            jsonBody = null,
            headerProperties = mapOf(
                "Authorization" to "$tokenString",
                "Content-Type" to "application/json",
                "Accept" to "application/json"
            )
        )
        var user: User? = null
        if (success) {
            user = Json.nonstrict.parse(User.serializer(), result)
        }
        return user
    }

    fun updateCurrentUser(user: User, newUser: Boolean): Boolean {
        var success = true
        if (newUser) {
            success = postNewUser(user)
        }
        if (success) {
            success = putCurrentUser(user)
        }
        return success
    }

    private fun getToken(): String? {
        return FirebaseAuth.getInstance().getAccessToken(false).result?.token
    }

    private fun postNewUser(user: User): Boolean {
        val tokenString = getToken()
        val json = when (user.userData.user_type) {
            UserTypeSelectionActivity.MOTHER -> "{\"user_type\":\"mother\",\"motherData\":${Json.stringify(
                MotherData.serializer(),
                user.motherData!!
            )}}"
            UserTypeSelectionActivity.DRIVER -> "{\"user_type\":\"driver\",\"driverData\":${Json.stringify(
                DriverData.serializer(),
                user.driverData!!
            )}}"
            else -> ""
        }
        val (success, result) = NetworkAdapter.httpRequest(
            stringUrl = "$baseUrl/users/onboard/${user.userData.id}",
            requestType = NetworkAdapter.POST,
            jsonBody = json,
            headerProperties = mapOf(
                "Authorization" to "$tokenString",
                "Content-Type" to "application/json",
                "Accept" to "application/json"
            )
        )
        return success
    }

    private fun putCurrentUser(user: User): Boolean {
        val tokenString = getToken()
        val json = Json.stringify(User.serializer(), user).replace("motherData", "mother")
            .replace("driverData", "driver")
        val (success, result) = NetworkAdapter.httpRequest(
            stringUrl = "$baseUrl/users/update/${user.userData.id}",
            requestType = NetworkAdapter.PUT,
            jsonBody = json,
            headerProperties = mapOf(
                "Authorization" to "$tokenString",
                "Content-Type" to "application/json",
                "Accept" to "application/json"
            )
        )
        return success
    }

    fun getDrivers(location: LatLng): ArrayList<RequestedDriver> {
        val tokenString = getToken()
        val json = "{\"location\":\"${location.latitude},${location.longitude}\"}"
        val (success, result) = NetworkAdapter.httpRequest(
            stringUrl = "$baseUrl/rides/drivers",
            requestType = NetworkAdapter.POST,
            jsonBody = json,
            headerProperties = mapOf(
                "Authorization" to "$tokenString",
                "Content-Type" to "application/json",
                "Accept" to "application/json"
            )
        )
        val drivers = ArrayList<RequestedDriver>()
        if (success) {
            val jsonArray = JSONArray(result)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                try {
                    drivers.add(
                        Json.nonstrict.parse(
                            RequestedDriver.serializer(),
                            jsonObject.toString()
                        )
                    )
                } catch (e: SerializationException) {
                    e.printStackTrace()
                }
            }
        }
        return drivers
    }

    fun getDirections(activity: Activity, start: LatLng, end: LatLng): MutableList<List<LatLng>> {
        val path: MutableList<List<LatLng>> = ArrayList()
//        val key = activity.applicationContext.resources.getString(R.string.google_api_key)
        val key = activity.applicationContext.resources.getString(R.string.gKey)
        val url =
            "https://maps.googleapis.com/maps/api/directions/json?origin=${start.latitude},${start.longitude}&destination=${end.latitude},${end.longitude}4&key=$key"
        val (success, response) = NetworkAdapter.httpRequest(url, NetworkAdapter.GET, null, null)
        if (success) {
            val jsonResponse = JSONObject(response)
            // Get routes
            val routes = jsonResponse.getJSONArray("routes")
            val legs = routes.getJSONObject(0).getJSONArray("legs")
            val steps = legs.getJSONObject(0).getJSONArray("steps")
            for (i in 0 until steps.length()) {
                val points = steps.getJSONObject(i).getJSONObject("polyline").getString("points")
                path.add(PolyUtil.decode(points))
            }
        }
        return path
    }

    fun getRideById(id: Int) {
        val (success, response) = NetworkAdapter.httpRequest(
            stringUrl = "$baseUrl/rides",
            requestType = NetworkAdapter.GET,
            jsonBody = "{\"rideId\": $id}",
            headerProperties = mapOf(
                "Authorization" to "${getToken()}",
                "Content-Type" to "application/json",
                "Accept" to "application/json"
            )
        )
        println(response)
    }

    fun postRideRequest(user: User, driverFbaseId: String): Boolean {
        val json =
            "{\"end\":\"${user.motherData?.destination?.latlng}\", \"start\":\"${user.motherData?.start?.latlng}\", \"name\":\"${user.userData.name}\", \"phone\":\"${user.userData.phone}\"}"
        val (success, response) = NetworkAdapter.httpRequest(
            stringUrl = "$baseUrl/rides/request/driver/$driverFbaseId",
            requestType = NetworkAdapter.POST,
            jsonBody = json,
            headerProperties = mapOf(
                "Authorization" to "${getToken()}",
                "Content-Type" to "application/json",
                "Accept" to "application/json"
            )
        )
        return success
    }

    private fun uploadDriverPhoto(bitmap: Bitmap): String {
        return "url"
    }
    fun acceptRejectRide(rideId: Long, accept: Boolean) :Boolean {
        val urlParam = when (accept) {
            true -> "accepts"
            false -> "rejects"
        }
        val (success, response) = NetworkAdapter.httpRequest(
            stringUrl = "$baseUrl/rides/driver/$urlParam/$rideId",
            requestType = NetworkAdapter.GET,
            jsonBody = null,
            headerProperties = mapOf(
                "Authorization" to "${getToken()}",
                "Content-Type" to "application/json",
                "Accept" to "application/json"
            )
        )
        return success
    }
}