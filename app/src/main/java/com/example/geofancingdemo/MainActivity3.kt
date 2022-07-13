package com.example.geofancingdemo

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task

class MainActivity3 : AppCompatActivity(), OnCompleteListener<Void> {
    private var mGeofencingClient: GeofencingClient? = null
    private val MY_PERMISSIONS_REQUEST_LOCATION = 42
    private var mGeofenceList: ArrayList<Geofence>? = null
    private var mGeofencePendingIntent: PendingIntent? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main3)
    }


    private fun addGeofences() {
        mGeofenceList = ArrayList<Geofence>()

        var listPlaces = ArrayList<MyLocation>()
        listPlaces.add(MyLocation("Place1", 63.153409, -7.294953))
        listPlaces.add(MyLocation("Place2", 63.213090, -7.340183))
        listPlaces.add(MyLocation("Place3", 63.294233, -7.381343))
        listPlaces.add(MyLocation("Place4", 63.336492, -7.438219))
        listPlaces.add(MyLocation("Place5", 63.383416, -7.491321))

        for (location: MyLocation in listPlaces) {
            mGeofenceList?.add(
                Geofence.Builder()
                    .setRequestId(location.key)
                    .setCircularRegion(
                        location.latitude,
                        location.longitude,
                        100f
                    )
                    .setNotificationResponsiveness(1000)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build()
            )
        }

        createGeofencingClient()
    }

    private fun createGeofencingClient() {
        mGeofencePendingIntent = null

        mGeofencingClient = LocationServices.getGeofencingClient(this)

        createGeoFencePendingIntent()?.let { mGeofencePendingIntent ->
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return
            }
            mGeofencingClient?.addGeofences(createGeofencingRequest(), mGeofencePendingIntent)
                ?.addOnCompleteListener(this)
        }
    }

    private fun createGeofencingRequest(): GeofencingRequest {
        val builder = GeofencingRequest.Builder()
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
        mGeofenceList?.let { builder.addGeofences(it) }
        return builder.build()
    }

    private fun createGeoFencePendingIntent(): PendingIntent? {

        mGeofencePendingIntent?.let {
            return it
        }

        val intent = Intent(this, MyGeofenceTransitionsIntentService::class.java)
        mGeofencePendingIntent =
            PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        return mGeofencePendingIntent

    }

    override fun onComplete(task: Task<Void>) {
        if (task.isSuccessful) {
            Toast.makeText(this, "Geofencing Successful", Toast.LENGTH_SHORT).show()
        } else {
            val errorMessage =
                task.exception?.let { MyGeofenceErrorMessages.getErrorString(this, it) }
            if (errorMessage != null) {
                Log.e("Geofencing Failed: ", errorMessage)
            }
        }
    }
}