package com.jbseppanen.birthride

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth


class FirebaseOauthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    companion object {
        const val RC_SIGN_IN = 51
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()


        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.PhoneBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.EmailBuilder().build()
        )
        val customLayout = AuthMethodPickerLayout.Builder(R.layout.login_layout)
            .setPhoneButtonId(R.id.button_login_phone)
            .setGoogleButtonId(R.id.button_login_google)
            .setEmailButtonId(R.id.button_login_email)
            .build()
//Set a custom layout


// Create and launch sign-in intent
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setIsSmartLockEnabled(false)
                .setAvailableProviders(providers)
                .setAuthMethodPickerLayout(customLayout)
                .setTheme(R.style.AppTheme_NoActionBar)
                .build(),
            RC_SIGN_IN
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            } else {
                Toast.makeText(this, "Sign-in Failed. Try Again", Toast.LENGTH_LONG).show()
            }
        }
    }
}