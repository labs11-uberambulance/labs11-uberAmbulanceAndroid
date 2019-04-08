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
    var location:Location? = null,
    var email: String? = ""
//    val created_at: String? = null,
//    var updated_at: String? = null
)

@Serializable
data class MotherData(
//    val id: Long,
    val firebase_id: String,
    var caretaker_name: String? = "",
    var start:Location? = null,
    var destination:Location? = null
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
    val id: Long,
    val name: String,
    val firebase_id: String,
    val phone: String,
//    var user_type: String,
    var location:Location,
//    val email: String?=null,
//    val created_at: String? = null,
//    var updated_at: String? = null
    val price: Int? = 0,
    val active: Boolean,
    val bio: String?=null,
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
    val distance: String,
    val name: String,
    val phone: String,
    val price: Int,
    val ride_id: Long
)

interface UploadImageCallback {
    fun returnResult(url: String)
}