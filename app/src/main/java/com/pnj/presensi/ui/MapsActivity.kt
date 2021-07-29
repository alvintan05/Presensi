package com.pnj.presensi.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
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
import com.pnj.presensi.R
import com.pnj.presensi.databinding.ActivityMapsBinding
import com.pnj.presensi.databinding.CustomAlertDialogBinding
import com.pnj.presensi.network.ApiRequest
import com.pnj.presensi.network.RetrofitServer
import com.pnj.presensi.ui.face_recognition.FaceRecognitionActivity
import pub.devrel.easypermissions.EasyPermissions
import java.util.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, EasyPermissions.PermissionCallbacks {

    private lateinit var binding: ActivityMapsBinding
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mMap: GoogleMap
    private lateinit var location: Location
    private lateinit var service: ApiRequest
    private lateinit var lokasiKerja: String //WFO atau WFH
    private lateinit var jam: String
    private lateinit var jenis: String //Datang atau Pulang

    private val TAG = "MapsActivity"
    private val RC_LOCATION_PERM = 123
    private val RC_LOCATION_SETTING = 0

    //private val marker = LatLng(-6.371450, 106.824392) // pnj
    private val radius = 259.0
    private val marker = LatLng(-6.345355, 106.868694) //rumah

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        service = RetrofitServer.apiRequest

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pengecekan Lokasi"

        val bundle = intent.extras
        jam = bundle?.getString("jam") ?: ""
        lokasiKerja = bundle?.getString("lokasi_kerja") ?: ""
        jenis = bundle?.getString("jenis") ?: ""

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        binding.btnCheckLocation.setOnClickListener {
            if (this::location.isInitialized) {
                checkForGeoFenceEntry(location, marker.latitude, marker.longitude)
            } else {
                Toast.makeText(this, "Lokasi belum didapatkan, harap coba lagi", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        if (hasLocationPermission()) {
            mMap.isMyLocationEnabled = true
            checkMyLocation()
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        EasyPermissions.requestPermissions(
            this,
            getString(R.string.rationale_location),
            RC_LOCATION_PERM,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    private fun hasLocationPermission(): Boolean {
        return EasyPermissions.hasPermissions(
            this@MapsActivity,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    }

    @SuppressLint("MissingPermission")
    private fun checkMyLocation() {
        if (isLocationEnabled()) {

            // this function for receiving last saved location on user devices
            fusedLocationProviderClient.lastLocation.addOnSuccessListener { location ->
                if (location == null) {
                    Log.d(TAG, "locationTask: Location null")
                    requestNewLocationData()
                } else {
                    this.location = location
                    // checking fake gps
                    if (!checkMockLocations() && !isLocationPlausible(location)) {
                        buildAlertMessage(getString(R.string.dialog_fake_gps_detected), false)
                    } else {
                        Log.d(
                            TAG,
                            "locationTask: Location ada Latitude: ${location.latitude} Longitude: ${location.longitude}"
                        )
                        mMap.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(
                                    location.latitude,
                                    location.longitude
                                ), 16f
                            )
                        )
                        binding.btnCheckLocation.visibility = View.VISIBLE
                    }
                }
            }
        } else {
            //location not enabled open settings
            buildAlertMessageNoGps()
        }
    }

    // method to check
// if location is enabled
    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {
        val locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 3000
        locationRequest.numUpdates

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            mLocationCallback,
            Looper.getMainLooper()
        )
    }

    private val mLocationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            location = locationResult.lastLocation

            // Checking fake GPS
            if (!checkMockLocations() && !isLocationPlausible(location)) {
                buildAlertMessage(getString(R.string.dialog_fake_gps_detected), false)
            } else {
                Log.d(
                    TAG,
                    "onLocationResult: lokasi didapatkan Latitude: ${location.latitude} Longitude: ${location.longitude}"
                )
                mMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            location.latitude,
                            location.longitude
                        ), 16f
                    )
                )
                binding.btnCheckLocation.visibility = View.VISIBLE
            }
            //stopLocationUpdates()
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

    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        enableMyLocation()
    }

    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        requestLocationPermission()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_LOCATION_SETTING) {
            checkMyLocation()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(marker, 16F))
        addCircle(marker)
        enableMyLocation()
//        val markerOutside = MarkerOptions().position(LatLng(-6.339954, 106.870418))
//        mMap.addMarker(markerOutside)

//        val markerInside = MarkerOptions().position(LatLng(-6.344941, 106.869003))
//        mMap.addMarker(markerInside)
    }

    private fun checkForGeoFenceEntry(
        userLocation: Location,
        geofenceLat: Double,
        geofenceLong: Double
    ) {
        val startLatLng = LatLng(userLocation.latitude, userLocation.longitude) // User Location
        val geofenceLatLng = LatLng(geofenceLat, geofenceLong) // Center of geofence

        val distanceInMeters = SphericalUtil.computeDistanceBetween(startLatLng, geofenceLatLng)

        if (distanceInMeters <= radius) {
            // User is inside the Geo-fence
            buildAlertMessage(getString(R.string.dialog_at_pnj), true)
        } else {
            buildAlertMessage(getString(R.string.dialog_outside_pnj), false)
        }
    }

    // Dialog to turn on location
    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(this).apply {
            setMessage(getString(R.string.dialog_no_gps))
            setCancelable(false)
            setPositiveButton(getString(R.string.yes)) { dialog, which ->
                startActivityForResult(
                    Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS),
                    RC_LOCATION_SETTING
                )
            }
            setNegativeButton(getString(R.string.no)) { dialog, which ->
                dialog.cancel()
                finish()
            }
        }

        val dialog: AlertDialog = builder.create()
        dialog.show()
    }

    private fun buildAlertMessage(message: String, status: Boolean) {
        stopLocationUpdates()
        val binding = CustomAlertDialogBinding.inflate(LayoutInflater.from(this))
        val builder = AlertDialog.Builder(this).apply {
            setCancelable(false)
            setView(binding.root)
        }
        if (!status) {
            binding.ivError.visibility = View.VISIBLE
            binding.ivSuccess.visibility = View.GONE
            binding.btnDialog.text = "Tutup"
        }
        binding.tvTitle.text = message
        val dialog: AlertDialog = builder.create()
        dialog.show()

        binding.btnDialog.setOnClickListener {
            if (status) {
                intentWithData()
            } else {
                dialog.dismiss()
                finish()
            }
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(mLocationCallback)
    }

    private fun addCircle(latLng: LatLng) {
        val circleOptions = CircleOptions()
        circleOptions.center(latLng)
        circleOptions.radius(radius)
        circleOptions.strokeColor(Color.argb(255, 0, 0, 255))
        circleOptions.fillColor(Color.argb(64, 0, 0, 255))
        circleOptions.strokeWidth(4F)
        mMap.addCircle(circleOptions)
    }

    private fun intentWithData() {
        val bundle = Bundle()
        bundle.putString("lokasi_kerja", lokasiKerja)
        bundle.putString("jam", jam)
        bundle.putString("jenis", jenis)
        val intent = Intent(this, FaceRecognitionActivity::class.java)
        intent.putExtras(bundle)
        startActivity(intent)
        finish()
    }

    private fun checkMockLocations(): Boolean {
        // Starting with API level >= 18 we can (partially) rely on .isFromMockProvider()
        // (http://developer.android.com/reference/android/location/Location.html#isFromMockProvider%28%29)
        // For API level < 18 we have to check the Settings.Secure flag
        @Suppress("DEPRECATION")
        (return Build.VERSION.SDK_INT < 18 && Settings.Secure.getString(
            this.contentResolver,
            Settings.Secure.ALLOW_MOCK_LOCATION
        ) != "0")

    }

    private fun isLocationPlausible(location: Location?): Boolean {
        var lastMockLocation: Location? = null
        if (location == null) return false

        val isMock =
            checkMockLocations() || Build.VERSION.SDK_INT >= 18 && location.isFromMockProvider

        if (isMock) {
            lastMockLocation = location
        }

        // If there's nothing to compare against, we have to trust it
        if (lastMockLocation == null) return true

        // And finally, if it's more than 1km away from the last known mock, we'll trust it
        val d = location.distanceTo(lastMockLocation).toDouble()
        return d > 1000
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }
}