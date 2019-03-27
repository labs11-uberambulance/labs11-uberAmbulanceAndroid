package com.jbseppanen.birthride

abstract class User(
    val id: Long?,
    val name: String? = "",
    val firebase_id: String,
    val phone: String? = "",
    val email: String? = "",
    val user_type: String? = "",
    val address: String? = "",
    val village: String? = "",
    val latitude: Double? = 0.0,
    val longitude: Double? = 0.0
)

class Mother(
    id: Long?,
    name: String? = "",
    firebase_id: String,
    phone: String? = "",
    email: String? = "",
    user_type: String? = "",
    address: String? = "",
    village: String? = "",
    latitude: Double? = 0.0,
    longitude: Double? = 0.0,

    val motherId: Long?,
    val caretaker_name: String,
    val due_date: String  //Check out DatePickerDialog.  https://stackoverflow.com/questions/45842167/how-to-use-datepickerdialog-in-kotlin

) : User(id, name, firebase_id, phone, email, user_type, address, village, latitude, longitude)


class Driver(
    id: Long?,
    name: String? = "",
    firebase_id: String,
    phone: String? = "",
    email: String? = "",
    user_type: String? = "",
    address: String? = "",
    village: String? = "",
    latitude: Double? = 0.0,
    longitude: Double? = 0.0,

    val driverId: Long?,
    val price: Double? = 0.0,
    val active: Boolean? = false,
    val bio: String? = "",
    val photo_url: String? = ""

) : User(id, name, firebase_id, phone, email, user_type, address, village, latitude, longitude)

class Ride(
    val id: Long?,
    val driver_id: Long?,
    val mother_id: Long?,
    val wait_min: Int? = 0,
    val start_village: String? = "",
    val start_address: String? = "",
    val destination: String? = "", //500 char limit.  Required.
    val destination_address: String? = "",
    val ride_status: String? = ""
)