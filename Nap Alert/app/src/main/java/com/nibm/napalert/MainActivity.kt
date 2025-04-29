package com.nibm.napalert

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location // Add this import
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import java.util.*

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var destinationEditText: EditText
    private lateinit var setDestinationButton: Button
    private lateinit var stopAlarmButton: Button
    private lateinit var statusTextView: TextView
    private lateinit var googleMap: GoogleMap

    private var destLat = 0.0
    private var destLng = 0.0
    private var alarmPlayed = false
    private var userMarker: Marker? = null
    private var destMarker: Marker? = null

    private val handler = Handler(Looper.getMainLooper())
    private val checkInterval = 10000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        destinationEditText = findViewById(R.id.destinationEditText)
        setDestinationButton = findViewById(R.id.setDestinationButton)
        stopAlarmButton = findViewById(R.id.stopAlarmButton)
        statusTextView = findViewById(R.id.statusTextView)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setDestinationButton.setOnClickListener {
            val locationName = destinationEditText.text.toString().trim()
            if (locationName.isEmpty()) {
                Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Thread {
                try {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val addressList = geocoder.getFromLocationName(locationName, 1)
                    runOnUiThread {
                        if (!addressList.isNullOrEmpty()) {
                            val address = addressList[0]
                            destLat = address.latitude
                            destLng = address.longitude
                            alarmPlayed = false
                            showDestinationMarker()
                            startCheckingLocation()
                            statusTextView.text = "Distance: Calculating..."
                            Toast.makeText(this, "Destination set: $locationName", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(this, "Error finding location", Toast.LENGTH_SHORT).show()
                    }
                }
            }.start()
        }

        stopAlarmButton.setOnClickListener {
            stopAlarm()
            stopAlarmButton.visibility = View.GONE
            Toast.makeText(this, "Alarm stopped", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        enableMyLocation()
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        } else {
            googleMap.isMyLocationEnabled = true
        }
    }

    private fun showDestinationMarker() {
        val destLatLng = LatLng(destLat, destLng)
        destMarker?.remove()
        destMarker = googleMap.addMarker(
            MarkerOptions().position(destLatLng).title("Destination")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    userMarker?.remove()

                    val bounds = LatLngBounds.builder()
                        .include(userLatLng)
                        .include(destLatLng)
                        .build()

                    googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150))
                }
            }
        }
    }

    private fun startCheckingLocation() {
        handler.removeCallbacksAndMessages(null)
        handler.postDelayed(object : Runnable {
            override fun run() {
                checkLocation()
                handler.postDelayed(this, checkInterval)
            }
        }, checkInterval)
    }

    private fun checkLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            location?.let {
                val distance = FloatArray(1)
                Location.distanceBetween(it.latitude, it.longitude, destLat, destLng, distance)

                val distanceText = String.format(Locale.getDefault(), "Distance: %.0f meters", distance[0])
                statusTextView.text = distanceText

                if (distance[0] < 500 && !alarmPlayed) {
                    triggerAlarm()
                    alarmPlayed = true
                    stopAlarmButton.visibility = View.VISIBLE
                    Toast.makeText(this, "You're near your destination!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun triggerAlarm() {
        val intent = Intent(this, AlarmService::class.java)
        startService(intent)
    }

    private fun stopAlarm() {
        stopService(Intent(this, AlarmService::class.java))
    }
}