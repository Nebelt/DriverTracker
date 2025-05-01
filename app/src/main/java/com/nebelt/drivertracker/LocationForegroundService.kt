package com.nebelt.drivertracker

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import java.util.concurrent.Executors

class LocationForegroundService : Service() {

    private lateinit var locationManager: LocationManager
    private val database = Firebase.database
    private val driverRef = database.getReference("drivers/driver_location")
    private val executor = Executors.newSingleThreadExecutor()

    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            executor.execute {
                try {
                    val lat = location.latitude.roundCoordinates()
                    val lng = location.longitude.roundCoordinates()
                    Log.d(TAG, "📍 Новые координаты: $lat,$lng")
                    saveToFirebase(lat, lng)
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Ошибка обработки локации", e)
                }
            }
        }

        override fun onProviderDisabled(provider: String) {
            Log.w(TAG, "⚡ Провайдер отключен: $provider")
        }

        override fun onProviderEnabled(provider: String) {
            Log.i(TAG, "✅ Провайдер доступен: $provider")
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "🟢 Сервис создан")
        initNotificationChannel()
        startLocationTracking()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        Log.i(TAG, "▶ Сервис запущен")
        return START_STICKY
    }

    private fun initNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Отслеживание местоположения",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Канал для фонового трекинга"
            }
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🚗 Трекинг водителя")
            .setContentText("Идет передача координат")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun startLocationTracking() {
        Log.d(TAG, "🛰 Запуск отслеживания")
        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        if (!hasLocationPermission()) {
            Log.e(TAG, "⛔ Нет разрешения на локацию")
            stopSelf()
            return
        }

        try {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.e(TAG, "🔐 Разрешения отозваны")
                stopSelf()
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_MS,
                    MIN_DISTANCE_M,
                    executor,
                    locationListener
                )
            } else {
                try {
                    @Suppress("DEPRECATION")
                    locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_MS,
                        MIN_DISTANCE_M,
                        locationListener,
                        Looper.getMainLooper()
                    )
                } catch (e: SecurityException) {
                    Log.e(TAG, "🛑 Ошибка безопасности", e)
                    stopSelf()
                    return
                }
            }

            try {
                val lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                lastLocation?.let {
                    saveToFirebase(it.latitude.roundCoordinates(), it.longitude.roundCoordinates())
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "🛑 Нет доступа к последней локации", e)
            }

        } catch (e: Exception) {
            Log.e(TAG, "💥 Ошибка трекинга", e)
            stopSelf()
        }
    }

    private fun saveToFirebase(lat: Double, lng: Double) {
        try {
            driverRef.setValue(mapOf(
                "lat" to lat,
                "lng" to lng,
                "timestamp" to ServerValue.TIMESTAMP
            )).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "🔥 Данные сохранены")
                } else {
                    Log.e(TAG, "❌ Ошибка Firebase", task.exception)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "‼ Ошибка сохранения", e)
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            locationManager.removeUpdates(locationListener)
            executor.shutdown()
            Log.w(TAG, "🔴 Сервис остановлен")
        } catch (e: Exception) {
            Log.e(TAG, "⚠ Ошибка остановки", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "LocationTracker"
        private const val NOTIFICATION_ID = 101
        private const val CHANNEL_ID = "location_channel"
        private const val MIN_TIME_MS = 3000L
        private const val MIN_DISTANCE_M = 5f

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, LocationForegroundService::class.java)
            )
        }
    }
}