package com.nebelt.drivertracker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.nebelt.drivertracker.databinding.ActivityObserverBinding

class ObserverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityObserverBinding
    private lateinit var mapHelper: YandexMapHelper
    private val database = Firebase.database.reference
    private val driverId = "driver_location"
    private var lastLat: Double = 0.0
    private var lastLng: Double = 0.0
    private var lastTimestamp: Long = 0
    private val stationaryThreshold = 10000 // 10 секунд

    companion object {
        private const val REQUEST_CODE_NOTIFICATION = 1002
        private const val TAG = "ObserverActivity"
        private const val YANDEX_MAPS_API_KEY = "7d92c3ab-2d34-4d24-8872-f842d6208162"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация карт
        mapHelper = YandexMapHelper(this, YANDEX_MAPS_API_KEY)
        mapHelper.initialize()

        binding = ActivityObserverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настройка карты
        mapHelper.setupMapView(binding.mapView)

        setupNotificationChannel()
        checkNotificationPermission()
        setupStatusText()

        database.child("drivers").child(driverId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lat = snapshot.child("lat").getValue(Double::class.java) ?: 0.0
                    val lng = snapshot.child("lng").getValue(Double::class.java) ?: 0.0
                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0

                    mapHelper.moveToPosition(lat, lng)
                    updateStatusText("Текущие координаты: ${lat.roundCoordinates()}, ${lng.roundCoordinates()}")

                    if (lastLat != 0.0 && isSameLocation(lat, lng)) {
                        if (System.currentTimeMillis() - timestamp > stationaryThreshold) {
                            showAlertDialog()
                        }
                    }

                    lastLat = lat
                    lastLng = lng
                    lastTimestamp = timestamp
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Database error: ${error.message}")
                    updateStatusText("Ошибка: ${error.message}")
                    Toast.makeText(this@ObserverActivity, "Ошибка базы данных", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Разрешение уже получено
                }

                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showPermissionRationale()
                }

                else -> {
                    requestPermissions(
                        arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                        REQUEST_CODE_NOTIFICATION
                    )
                }
            }
        }
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Нужны разрешения")
            .setMessage("Для уведомлений о статусе водителя требуются разрешения")
            .setPositiveButton("OK") { _, _ ->
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_NOTIFICATION
                )
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun setupStatusText() {
        binding.statusText.apply {
            text = "Ожидание данных..."
            setTextColor(ContextCompat.getColor(this@ObserverActivity, android.R.color.white))
        }
    }

    private fun updateStatusText(message: String) {
        binding.statusText.text = message
    }

    private fun isSameLocation(newLat: Double, newLng: Double): Boolean {
        return lastLat.roundCoordinates() == newLat.roundCoordinates() &&
                lastLng.roundCoordinates() == newLng.roundCoordinates()
    }

    private fun showAlertDialog() {
        AlertDialog.Builder(this)
            .setTitle("Внимание!")
            .setMessage("Водитель не двигается более 10 секунд. Позвонить?")
            .setPositiveButton("Позвонить") { _, _ ->
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:+79211234567")
                }
                startActivity(intent)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    override fun onStart() {
        super.onStart()
        mapHelper.onStart()
    }

    override fun onStop() {
        mapHelper.onStop()
        super.onStop()
    }

    private fun setupNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location_channel",
                "Location Tracking",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when driver stops moving"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}