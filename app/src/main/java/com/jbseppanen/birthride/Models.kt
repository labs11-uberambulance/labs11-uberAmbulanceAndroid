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
    val id: Long,
    var name: String? = "",
    var phone: String? = "",
    val firebase_id: String,
    var user_type: String? = null,
    var location: Location? = null,
    var email: String? = ""
//    val created_at: String? = null,
//    var updated_at: String? = null
)

@Serializable
data class MotherData(
//    val id: Long,
    val firebase_id: String,
    var caretaker_name: String? = "",
    var start: Location? = null,
    var destination: Location? = null
//    val created_at: String? = null,
//    var updated_at: String? = null
)

@Serializable
data class DriverData(
    //    val id: Long,
    val firebase_id: String,
    var price: Int? = 0,
    var active: Boolean? = false,
    var bio: String? = "",
    var photo_url: String? = null
//    val created_at: String? = null,
//    var updated_at: String? = null
)

@Serializable
data class Location(
    var descr: String?,
    var latlng: String,
    var name: String?
)


@Serializable
data class Ride(
    val id: Long,
    val mother_id: String,
    val driver_id: String,
    val start: String,
    val destination: String,
    val rejected_drivers: Any? = null,
    val ride_status: String
//    val created_at: String,
//    val updated_at: String
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
    val id: Long,
    val name: String,
    val firebase_id: String,
    val phone: String,
//    var user_type: String,
    var location: Location,
//    val email: String?=null,
//    val created_at: String? = null,
//    var updated_at: String? = null
    val price: Int? = 0,
    val active: Boolean,
    val bio: String? = null,
    val photo_url: String
)

@Serializable
data class LatLngJson(
    val lat: Double,
    val long: Double
)


@Serializable
@SerialName("data")
data class RideRequestData(
    val distance: String="",
    val name: String="",
    val hospital: String="",
    val phone: String="",
    val price: Int=0,
    val ride_id: Long=-1L
)

interface ResultCallback {
    fun returnResult(result: String?)
}