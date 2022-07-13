package com.example.geofancingdemo

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*


class MainActivity2 : AppCompatActivity(),
    OnMapReadyCallback,
    GoogleMap.OnMapClickListener,
    GoogleMap.OnMarkerClickListener,
    GoogleApiClient.ConnectionCallbacks,
    GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {
    var mapFragment: MapFragment? = null
    var map: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var googleApiClient: GoogleApiClient? = null
    private var lastLocation: Location? = null
    private var UPDATE_INTERVAL: Long? = 1000
    private var FASTEST_INTERVAL: Long? = 900

    private val GEO_DURATION = (60 * 60 * 1000).toLong()
    private val GEOFENCE_REQ_ID = "My Geofence"
    private val GEOFENCE_RADIUS = 500.0f // in meters

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        checkPermission()
    }

    fun checkPermission(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            initGMaps()
            return true
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ), 1234
            )
            return false
        }
    }

    private fun createGoogleApi() {
        if (googleApiClient == null) {
            googleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()
        }
        googleApiClient!!.connect()
    }

    private fun initGMaps() {
        mapFragment = fragmentManager.findFragmentById(R.id.mapFragment) as MapFragment
        mapFragment!!.getMapAsync(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1234 && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location Permission is granted", Toast.LENGTH_SHORT).show()
                initGMaps()
            } else {
                Toast.makeText(this, "Location Permission is Denied", Toast.LENGTH_SHORT).show()
            }
            if (grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location Permission is granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Location Permission is Denied", Toast.LENGTH_SHORT).show()
            }
            if (grantResults[2] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location Permission is granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Location Permission is Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

//    fun startUpdated() {
//        locationRequest = LocationRequest.create()
//            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
//            .setInterval(UPDATE_INTERVAL!!)
//            .setFastestInterval(FASTEST_INTERVAL!!)
//        if (checkPermission()){
//            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient,locationRequest,this)
//        }
//    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap;
        map!!.setOnMapClickListener(this)
        map!!.setOnMarkerClickListener(this)
        createGoogleApi()
    }

    override fun onStart() {
        super.onStart()

    }

    override fun onMapClick(latLng: LatLng) {
        Log.e("MainActivity", "onMapClick($latLng)");
        Toast.makeText(
            this,
            "${latLng!!.latitude + latLng!!.longitude}",
            Toast.LENGTH_SHORT
        ).show()
        markerForGeofence(latLng)
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        Log.e("MainActivity", "onMapClick(" + marker.position + ")")
        return true
    }

    fun getLastKnownLocation() {
        if (checkPermission()) {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient!!)
            if (lastLocation != null) {
                Log.e(
                    "MainActicityLocation",
                    "${lastLocation!!.latitude + lastLocation!!.longitude}"
                )
                Toast.makeText(
                    this,
                    "${lastLocation!!.latitude + lastLocation!!.longitude}",
                    Toast.LENGTH_SHORT
                ).show()
                markerLocation(LatLng(lastLocation!!.latitude, lastLocation!!.longitude))
            } else {
                Toast.makeText(this, "Location is null", Toast.LENGTH_SHORT).show()
            }
        }
    }

    var locationMarker: Marker? = null
    fun markerLocation(latLng: LatLng) {
        var markerOption =
            MarkerOptions().position(latLng).title("${latLng.latitude + latLng.longitude}")
        if (map != null) {
            // Remove the anterior marker
            if (locationMarker != null) locationMarker!!.remove()
            locationMarker = map!!.addMarker(markerOption)!!
            val zoom = 14f
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom)
            map!!.animateCamera(cameraUpdate)
        }

    }

    var geoFenceMarker: Marker? = null
    private fun markerForGeofence(latLng: LatLng) {
        val title = latLng.latitude.toString() + ", " + latLng.longitude
        // Define marker options
        val markerOptions = MarkerOptions()
            .position(latLng)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
            .title(title)
        if (map != null) {
            if (geoFenceMarker != null)
                geoFenceMarker!!.remove()
            geoFenceMarker = map!!.addMarker(markerOptions)
            startGeofence()
        }
    }

    private fun createGeofence(latLng: LatLng, radius: Float): Geofence? {
        Toast.makeText(this, "Create Geo Fence", Toast.LENGTH_SHORT).show()
        return Geofence.Builder()
            .setRequestId(GEOFENCE_REQ_ID)
            .setCircularRegion(latLng.latitude, latLng.longitude, radius)
            .setExpirationDuration(GEO_DURATION)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER
                        or Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .build()

    }

    private fun createGeofenceRequest(geofence: Geofence): GeofencingRequest? {
        Toast.makeText(this, "Create Geo Fence Request", Toast.LENGTH_SHORT).show()
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

    }

    private val geoFencePendingIntent: PendingIntent? = null
    private val GEOFENCE_REQ_CODE = 0
    private fun createGeofencePendingIntent(): PendingIntent? {
        if (geoFencePendingIntent != null) return geoFencePendingIntent
        val intent = Intent(this, GeofenceTrasitionService::class.java)
        return PendingIntent.getService(
            this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    fun addGeoFence(geofencingRequest: GeofencingRequest) {
        if (checkPermission()) {
            LocationServices.GeofencingApi.addGeofences(
                googleApiClient!!, geofencingRequest, createGeofencePendingIntent()!!
            ).setResultCallback(this)
        }
    }


    override fun onResume() {
        super.onResume()
    }

    override fun onConnected(p0: Bundle?) {
        getLastKnownLocation()
    }

    override fun onConnectionSuspended(p0: Int) {
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
    }

    override fun onResult(status: Status) {
        if (status.isSuccess()) {
            drawGeofence();
        } else {
            Toast.makeText(this, "Geo fence Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startGeofence() {
        if (locationMarker != null) {
            val geofence = createGeofence(geoFenceMarker!!.position, GEOFENCE_RADIUS)
            val geofenceRequest = createGeofenceRequest(geofence!!)
            addGeoFence(geofenceRequest!!)
        } else {
        }
    }

    private var geoFenceLimits: Circle? = null
    private fun drawGeofence() {
        if (geoFenceLimits != null) geoFenceLimits!!.remove()
        val circleOptions = CircleOptions()
            .center(geoFenceMarker!!.getPosition())
            .strokeColor(Color.argb(50, 70, 70, 70))
            .fillColor(Color.argb(100, 150, 150, 150))
            .radius(GEOFENCE_RADIUS.toDouble())
        geoFenceLimits = map!!.addCircle(circleOptions)
    }

}