package com.jbseppanen.birthride

import android.graphics.Bitmap
import android.support.annotation.WorkerThread
import com.google.firebase.auth.FirebaseAuth
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

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
/*            val json = JSONObject(result)
            val userJson: JSONObject = json.getJSONObject("user")
            userType = userJson["user_type"] as String?
            if (userType == "mothers") {*/
            user = Json.nonstrict.parse(User.serializer(), result)
//            }
        }
        return user
    }

    fun updateCurrentUser(user:User):Boolean {
        return if ((user.motherData?.id == null) && (user.driverData?.id == null)) {
            postNewUser(user)
        } else {
            putCurrentUser(user)
        }
    }

    private fun getToken(): String? {
        return FirebaseAuth.getInstance().getAccessToken(false).result?.token
    }

    private fun postNewUser(user: User) :Boolean {
        val tokenString = getToken()
        val userType:String = user.userData.user_type!!
        val jsonString = Json.stringify(String.serializer(), userType)
        val json = jsonString + Json.stringify(User.serializer(), user)

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
        val (success, result) = NetworkAdapter.httpRequest(
            stringUrl = "$baseUrl/users/update/${user.userData.id}",
            requestType = NetworkAdapter.PUT,
            jsonBody = Json.stringify(User.serializer(), user),
            headerProperties = mapOf(
                "Authorization" to "$tokenString",
                "Content-Type" to "application/json",
                "Accept" to "application/json"
            )
        )
        return success
    }

    private fun uploadDriverPhoto(bitmap: Bitmap):String {
        return "url"
    }
}