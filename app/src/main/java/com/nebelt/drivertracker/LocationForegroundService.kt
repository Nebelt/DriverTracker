// LocationForegroundService.kt
package com.nebelt.drivertracker

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.math.BigDecimal
import java.math.RoundingMode

class LocationForegroundService : Service() {

    private lateinit var locationManager: LocationManager
    private val database = Firebase.database
    private val driverRef = database.getReference("drivers/driver_location")

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            val roundedLat = location.latitude.roundToHundredths()
            val roundedLng = location.longitude.roundToHundredths()
            Log.d("LocationService", "New location: $roundedLat, $roundedLng")
            saveLocation(roundedLat, roundedLng)
        }

        override fun onProviderDisabled(provider: String) {
            Log.w("LocationService", "Provider disabled: $provider")
        }

        override fun onProviderEnabled(provider: String) {
            Log.i("LocationService", "Provider enabled: $provider")
        }
    }

    override fun onCreate() {
        super.onCreate()
        initNotificationChannel()
        startLocationUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification()
        startForeground(1, notification)
        return START_STICKY
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("Отслеживание местоположения")
            .setContentText("Идет передача координат водителя")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location_channel",
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracking driver location"
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun startLocationUpdates() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        try {
            if (checkPermission()) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000,
                    5f,
                    locationListener
                )
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    5000,
                    10f,
                    locationListener
                )

                // Получаем последнее известное местоположение
                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                lastLocation?.let {
                    saveLocation(
                        it.latitude.roundToHundredths(),
                        it.longitude.roundToHundredths()
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("LocationService", "Location update error", e)
        }
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun saveLocation(lat: Double, lng: Double) {
        driverRef.setValue(
            mapOf(
                "lat" to lat,
                "lng" to lng,
                "timestamp" to ServerValue.TIMESTAMP
            )
        ).addOnFailureListener { e ->
            Log.e("LocationService", "Save failed", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: Exception) {
            Log.e("LocationService", "Error removing updates", e)
        }
    }

    private fun Double.roundToHundredths(): Double {
        return BigDecimal(this).setScale(2, RoundingMode.HALF_UP).toDouble()
    }
}