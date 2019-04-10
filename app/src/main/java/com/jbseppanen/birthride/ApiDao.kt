package com.jbseppanen.birthride

import android.app.Activity
import android.graphics.Bitmap
import android.support.annotation.WorkerThread
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GetTokenResult
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.tasks.await
import kotlinx.io.ByteArrayOutputStream
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.util.*


const val baseUrl = "https://birthrider-backend.herokuapp.com/api"

@WorkerThread
object ApiDao {

    private suspend fun getToken(): String? {
        val await: GetTokenResult = FirebaseAuth.getInstance().getAccessToken(false).await()
        return await.token
    }

    suspend fun getCurrentUser(): User? {
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

    suspend fun updateCurrentUser(user: User, newUser: Boolean): Boolean {
        var success = true
        if (newUser) {
            success = postNewUser(user)
        }
        if (success) {
            success = putCurrentUser(user)
        }
        return success
    }

    private suspend fun postNewUser(user: User): Boolean {
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

    private suspend fun putCurrentUser(user: User): Boolean {
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

/*    private fun getTokenOld():String? {
       return FirebaseAuth.getInstance().getAccessToken(false).result?.token
    }*/

    suspend fun getDrivers(location: LatLng): ArrayList<RequestedDriver> {
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

    suspend fun getRideById(id: Int) {
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

    suspend fun getUserRides(): ArrayList<Ride> {
        val tokenString = getToken()
        val (success, result) = NetworkAdapter.httpRequest(
            stringUrl = "$baseUrl/rides/mother",
            requestType = NetworkAdapter.GET,
            jsonBody = "{\"userId\": $tokenString}",
            headerProperties = mapOf(
                "Authorization" to "$tokenString",
                "Content-Type" to "application/json",
                "Accept" to "application/json"
            )
        )
        val rides = ArrayList<Ride>()
        if (success) {
            val jsonArray = JSONArray(result)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                try {

                    val ride: Ride = Json.nonstrict.parse(
                        Ride.serializer(),
                        jsonObject.toString()
                    )
                    if (!ride.ride_status.contains("complete", true) || !ride.ride_status.contains("cancel", true)) {
                        rides.add(ride)
                    }
                } catch (e: SerializationException) {
                    e.printStackTrace()
                }
            }
        }
        return rides
    }

    suspend fun postRideRequest(user: User, driverFbaseId: String): Boolean {
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

    suspend fun acceptRejectRide(rideId: Long, accept: Boolean, json: String?): Boolean {
        val urlParam = when (accept) {
            true -> "accepts"
            false -> "rejects"
        }

        //Accept  = get.  Reject = Post
        val (success, response) = NetworkAdapter.httpRequest(
            stringUrl = "$baseUrl/rides/driver/$urlParam/$rideId",
            requestType = when (accept) {
                true -> NetworkAdapter.GET
                false -> NetworkAdapter.POST
            },
            jsonBody = null,
            headerProperties = mapOf(
                "Authorization" to "${getToken()}",
                "Content-Type" to "application/json",
                "Accept" to "application/json"
            )
        )
        return success
        //key "dataDictionary", value = data
    }

    //TODO change to not repeat so much code.
    fun uploadDriverPhoto(bitmap: Bitmap, url: String? = null, callback: ResultCallback) {
        val imagesRef = FirebaseStorage.getInstance().reference
        val currentUser = FirebaseAuth.getInstance().currentUser

        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 10, baos)
        val data = baos.toByteArray()
        val profileImage =
            imagesRef.child("profile_images/${currentUser?.uid}${System.currentTimeMillis()}.jpg")

        if (url != null) {
            val referenceFromUrl: Task<Void>? =
                FirebaseStorage.getInstance().getReferenceFromUrl(url).delete()
            referenceFromUrl?.addOnSuccessListener {
                val uploadTask: UploadTask = profileImage.putBytes(data)
                uploadTask.addOnSuccessListener {
                    profileImage.downloadUrl.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            callback.returnResult(task.result.toString())
                        } else {
                            callback.returnResult("")
                        }
                    }
                }
                uploadTask.addOnFailureListener {
                    println("Failed to upload")
                }
            }
            referenceFromUrl?.addOnFailureListener {
                val uploadTask: UploadTask = profileImage.putBytes(data)
                uploadTask.addOnSuccessListener {
                    profileImage.downloadUrl.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            callback.returnResult(task.result.toString())
                        } else {
                            callback.returnResult("")
                        }
                    }
                }
                uploadTask.addOnFailureListener {
                    println("Failed to upload")
                }
            }
        } else {
            val uploadTask: UploadTask = profileImage.putBytes(data)
            uploadTask.addOnSuccessListener {
                profileImage.downloadUrl.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        callback.returnResult(task.result.toString())
                    } else {
                        callback.returnResult("")
                    }
                }
            }
            uploadTask.addOnFailureListener {
                println("Failed to upload")
            }
        }
    }

    suspend fun updateFcmToken(token:String) {
        val json =
            "{\"token\":\"$token\"}"
        val (success, response) = NetworkAdapter.httpRequest(
            stringUrl = "$baseUrl/users/notifications/refresh-token",
            requestType = NetworkAdapter.POST,
            jsonBody = json,
            headerProperties = mapOf(
                "Authorization" to "${getToken()}",
                "Content-Type" to "application/json",
                "Accept" to "application/json"
            )
        )
//        return success
    }
}
