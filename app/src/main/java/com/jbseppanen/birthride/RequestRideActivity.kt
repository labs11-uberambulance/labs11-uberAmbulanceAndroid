package com.jbseppanen.birthride

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_request_ride.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json

class RequestRideActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_request_ride)
        val context: Context = this
        button_requestride_request.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ConfirmRequestActivity::class.java
                )
            )
        }
        button_requestride_editprofile.setOnClickListener {

            CoroutineScope(Dispatchers.IO + Job()).launch {
                val user = ApiDao.getCurrentUser()
                if (user!=null) {
                    val requestIntent = Intent(context, EditAccountDetailsActivity::class.java)
                    val serializer: SerializationStrategy<User> = User.serializer()
                    val extra = Json.stringify(serializer, user)
                    requestIntent.putExtra(WelcomeActivity.USER_KEY, extra)
                    startActivity(requestIntent)
                }
            }
        }
    }
}
