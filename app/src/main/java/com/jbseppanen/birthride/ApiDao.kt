package com.jbseppanen.birthride

import android.graphics.Bitmap
import android.support.annotation.WorkerThread
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import kotlinx.serialization.json.Json


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
        val json = Json.stringify(User.serializer(), user).replace("motherData","mother").replace("driverData", "driver")
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

    fun getDrivers(location:LatLng) {
        val tokenString = getToken()
        val json = "{\"latitude\":${location.latitude}, \"longitude\":${location.longitude}"
        val (success, result) = NetworkAdapter.httpRequest(
            stringUrl = "$baseUrl/drivers",
            requestType = NetworkAdapter.POST,
            jsonBody = json,
            headerProperties = mapOf(
                "Authorization" to "$tokenString",
                "Content-Type" to "application/json",
                "Accept" to "application/json"
            )
        )
        println(result)
    }

    private fun uploadDriverPhoto(bitmap: Bitmap): String {
        return "url"
    }
}