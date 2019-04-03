package com.jbseppanen.birthride

import kotlinx.serialization.Optional
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("user")
    val userData: UserData,

    @Optional
    var motherData: MotherData? = null,

    @Optional
    var driverData: DriverData? = null
)

@Serializable
data class UserData(
    var address: String? = "",
    val created_at: String? = null,
//    var email: String? = "",
    val firebase_id: String,
    val id: Long,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var name: String? = "",
    var phone: String? = "",
//    var updated_at: String? = null,
    var user_type: String? = null
//    var village: String? = ""
)

@Serializable
data class MotherData(
/*    @Optional
    var id: Int? = null,*/
    val firebase_id: String,
    var caretaker_name: String? = "",
    @Optional  // Todo remove this tag later when back end set up.
    var destination_address: LatLngJson = LatLngJson(Constants.defaultMapCenter.latitude, Constants.defaultMapCenter.longitude)
/*    var due_date: String? = null,
    var hospital: String? = ""*/
//    val created_at: String? = null,
//    var updated_at: String? = null
)

@Serializable
data class DriverData(
    var active: Boolean? = false,
    var bio: String? = "",
//    val created_at: String? = null,
    val firebase_id: String,
/*    @Optional
    var id: Int? = null,*/
    var photo_url: String? = null,
    var price: Int? = 0
//    var updated_at: String? = null
)

@Serializable
data class Ride(
    val id: Long?,
    var driver_id: Int?,
    val mother_id: Int,
//    var wait_min: Int? = 5,
//    var start_village: String? = "",
    var start_address: LatLngJson,
//    var destination: String? = "", //500 char limit.  Required.
    var destination_address: LatLngJson,
    var ride_status: String? = ""
)

@Serializable
data class RequestedDriver(
    val distance: Distance,
    val driver: Driver,
    val duration: Duration,
    val id: Long
)

@Serializable
data class Distance(
    @SerialName("text")
    val length: String
)

@Serializable
data class Duration(
    @SerialName("text")
    val time: String
)

@Serializable
data class Driver(
    val active: Boolean,
    val address: String,
    val bio: String,
//    val created_at: String,
//    val email: String,
//    val firebase_id: String,
    val id: Long,
    val latitude: String,
    val longitude: String,
    val name: String,
    val phone: String,
    val photo_url: String,
    val price: Int
//    val updated_at: String,
//    val user_type: String,
//    val village: String
)

@Serializable
data class LatLngJson(
    val lat: Double,
    val long: Double
)