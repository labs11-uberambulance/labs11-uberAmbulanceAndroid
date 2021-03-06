package com.jbseppanen.birthride

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.*
import android.preference.PreferenceManager


class PushNotificationService : FirebaseMessagingService() {

    private lateinit var broadcaster: LocalBroadcastManager

    override fun onCreate() {
        super.onCreate()
        broadcaster = LocalBroadcastManager.getInstance(this)
    }

    /**
     * Called when message is received.
     *
     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
     */
    // [START receive_message]
    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        // [START_EXCLUDE]
        // There are two types of messages data messages and notification messages. Data messages are handled
        // here in onMessageReceived whether the app is in the foreground or background. Data messages are the type
        // traditionally used with GCM. Notification messages are only received here in onMessageReceived when the app
        // is in the foreground. When the app is in the background an automatically generated notification is displayed.
        // When the user taps on the notification they are returned to the app. Messages containing both notification
        // and data payloads are treated as notification messages. The Firebase console always sends notification
        // messages. For more see: https://firebase.google.com/docs/cloud-messaging/concept-options
        // [END_EXCLUDE]
        // Not getting messages here? See why this may be: https://goo.gl/39bRNJ )

        // Check if message contains a data payload.
        remoteMessage?.data?.isNotEmpty()?.let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)

/*            if (*//* Check if data needs to be processed by long running job *//* true) {
                // For long-running tasks (10 seconds or more) use WorkManager.
                scheduleJob()
            } else {*/
            // Handle message within 10 seconds
//                handleNow()
            if (remoteMessage.data != null) {
                saveToSharedPrefs(remoteMessage.data)
                val intent = Intent(SERVICE_BROADCAST_KEY)
                val dataHashMap = HashMap<String, String>(remoteMessage.data)
                intent.putExtra(SERVICE_MESSAGE_KEY, dataHashMap)
                broadcaster.sendBroadcast(intent)
            }
//            }
        }

        // Check if message contains a notification payload.
        remoteMessage?.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
        }

        // Also if you intend on generating your own notifications as a result of a received FCM
        // message, here is where that should be initiated. See sendNotification method below.
        sendNotification("New Ride Request")
    }

    private fun saveToSharedPrefs(dataHashMap: MutableMap<String, String>) {
        val key = dataHashMap["ride_id"]
        val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (key != null) {
            dataHashMap["timestamp"] = System.currentTimeMillis().toString()
            dataHashMap["accepted"] = "false"
            sharedPrefs.edit().putString(key, dataHashMap.toString()).apply()
            val rideIds = sharedPrefs.getString(STORED_REQUESTS_KEY, null)
            val putString: String
            putString = if (rideIds == null) {
                dataHashMap.getValue("ride_id")
            } else {
                "$rideIds,${dataHashMap.getValue("ride_id")}"
            }
            sharedPrefs.edit().putString(STORED_REQUESTS_KEY,putString).apply()
        }
    }
    // [END receive_message]

    // [START on_new_token]
    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String?) {
        Log.d(TAG, "Refreshed token: $token")

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        sendRegistrationToServer(token)
    }


    /**
     * Persist token to third-party servers.
     *
     * Modify this method to associate the user's FCM InstanceID token with any server-side account
     * maintained by your application.
     *
     * @param token The new token.
     */
    private fun sendRegistrationToServer(token: String?) {
        CoroutineScope(Dispatchers.IO + Job()).launch {
            if (token != null) {
                ApiDao.updateFcmToken(token)
            }
        }
    }

    /**
     * Create and show a simple notification containing the received FCM message.
     *
     * @param messageBody FCM message body received.
     */
    private fun sendNotification(messageBody: String) {
        val intent = Intent(this, WelcomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT
        )

        val channelId = getString(R.string.request_default_channel_id)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.fui_idp_button_background_email)
            .setContentTitle(getString(R.string.fcm_message))
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Ride Request Notification Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }



    companion object {
        private const val TAG = "FirebaseMsgService"
        const val SERVICE_BROADCAST_KEY = "BirthRide Service Key"
        const val SERVICE_MESSAGE_KEY = "BirthRide Service Message Key"
        const val STORED_REQUESTS_KEY = "Stored requests"
    }
}