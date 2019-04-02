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
    var email: String? = "",
    val firebase_id: String,
    val id: Long,
    var latitude: Double? = null,
    var longitude: Double? = null,
    var name: String? = "",
    var phone: String? = "",
    var updated_at: String? = null,
    var user_type: String? = null,
    var village: String? = ""
)

@Serializable
data class MotherData(
/*    @Optional
    var id: Int? = null,*/
    val firebase_id: String,
    var caretaker_name: String? = "",
    var due_date: String? = null,
    var hospital: String? = ""
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
    var wait_min: Int? = 5,
    var start_village: String? = "",
    var start_address: String? = "",
    var destination: String? = "", //500 char limit.  Required.
    var destination_address: String? = "",
    var ride_status: String? = ""
)

/*@Serializable
data class JsonBodyWrapper(
    val user_type:String,

    @Optional
    val motherData: MotherData,

    @Optional
    val driverData: DriverData
)*/
