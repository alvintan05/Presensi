package com.pnj.presensi

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import com.pnj.presensi.databinding.ActivityMapsBinding
import com.pnj.presensi.databinding.CustomAlertDialogBinding
import pub.devrel.easypermissions.EasyPermissions


class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mMap: GoogleMap
    private lateinit var location: Location

    private val TAG = "MapsActivity"
    private val RC_LOCATION_PERM = 123
    private val marker = LatLng(-6.371450, 106.824392) // pnj
    //private val marker = LatLng(-6.345355, 106.868694) //rumah

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun hasLocationPermission(): Boolean {
        return EasyPermissions.hasPermissions(
            this@MapsActivity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    @SuppressLint("MissingPermission")
    private fun locationTask() {
        if (hasLocationPermission()) {
            // Have permission
            if (isLocationEnabled()) {
                //if location enabled
                fusedLocationProviderClient.lastLocation.addOnCompleteListener { task ->
                    val location = task.result
                    if (location == null) {
                        requestNewLocationData()
                    } else {
                        checkForGeoFenceEntry(location, marker.latitude, marker.longitude, 259.0)
                    }
                }
            } else {
                //location not enabled open settings
                buildAlertMessageNoGps()
            }
        } else {
            // Ask for one permission
            EasyPermissions.requestPermissions(
                this,
                getString(R.string.rationale_location),
                RC_LOCATION_PERM,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    // method to check
    // if location is enabled
    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // Dialog to turn on location
    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(this).apply {
            setMessage(getString(R.string.dialog_no_gps))
            setCancelable(false)
            setPositiveButton(getString(R.string.yes)) { dialog, which ->
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            setNegativeButton(getString(R.string.no)) { dialog, which ->
                dialog.cancel()
            }
        }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun buildAlertMessage(message: String, status: Boolean) {
        val binding = CustomAlertDialogBinding.inflate(LayoutInflater.from(this))
        val builder = AlertDialog.Builder(this).apply {
            setView(binding.root)
        }
        if (!status) {
            binding.ivError.visibility = View.VISIBLE
            binding.ivSuccess.visibility = View.GONE
        }
        binding.tvTitle.text = message
        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5
        locationRequest.fastestInterval = 0
        locationRequest.numUpdates

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            mLocationCallback,
            Looper.myLooper()
        )
    }

    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            location = locationResult.lastLocation
            checkForGeoFenceEntry(location, marker.latitude, marker.longitude, 259.0)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker, 16F))
        addCircle(marker, 259.0)
        mMap.isMyLocationEnabled = true

//        val markerOutside = MarkerOptions().position(LatLng(-6.339954, 106.870418))
//        mMap.addMarker(markerOutside)

//        val markerInside = MarkerOptions().position(LatLng(-6.344941, 106.869003))
//        mMap.addMarker(markerInside)
    }

    override fun onResume() {
        super.onResume()
        if (hasLocationPermission()) {
            locationTask()
        }
    }

    private fun checkForGeoFenceEntry(
        userLocation: Location,
        geofenceLat: Double,
        geofenceLong: Double,
        radius: Double
    ) {
        val startLatLng = LatLng(userLocation.latitude, userLocation.longitude) // User Location
        //val startLatLng = LatLng(-6.344941, 106.869003) // dalam
        //val startLatLng = LatLng(-6.339954, 106.870418) // luar
        val geofenceLatLng = LatLng(geofenceLat, geofenceLong) // Center of geofence

        val distanceInMeters = SphericalUtil.computeDistanceBetween(startLatLng, geofenceLatLng)

        if (distanceInMeters < radius) {
            // User is inside the Geo-fence
            buildAlertMessage("Anda berada di area", true)
            //Toast.makeText(this, "Anda berada di area", Toast.LENGTH_SHORT).show()
        } else {
            buildAlertMessage("Anda berada di luar area", false)
        }
    }

    private fun addCircle(latLng: LatLng, radius: Double) {
        val circleOptions = CircleOptions()
        circleOptions.center(latLng)
        circleOptions.radius(radius)
        circleOptions.strokeColor(Color.argb(255, 0, 0, 255))
        circleOptions.fillColor(Color.argb(64, 0, 0, 255))
        circleOptions.strokeWidth(4F)
        mMap.addCircle(circleOptions)
    }
}