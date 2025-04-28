package com.nebelt.drivertracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.nebelt.drivertracker.databinding.ActivityObserverBinding

class ObserverActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_NOTIFICATION = 1002
        private const val TAG = "ObserverActivity"
    }

    private lateinit var binding: ActivityObserverBinding
    private val database = Firebase.database.reference
    private val driverId = "driver_location"
    private var lastLat: Double = 0.0
    private var lastLng: Double = 0.0
    private var lastTimestamp: Long = 0
    private val stationaryThreshold = 10000 // 10 секунд

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityObserverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNotificationChannel()
        checkNotificationPermission()
        setupStatusText()

        database.child("drivers").child(driverId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lat = snapshot.child("lat").getValue(Double::class.java) ?: 0.0
                    val lng = snapshot.child("lng").getValue(Double::class.java) ?: 0.0
                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0

                    Log.d(TAG, "Received location: $lat, $lng")

                    if (lastLat != 0.0 && isSameLocation(lat, lng)) {
                        if (System.currentTimeMillis() - timestamp > stationaryThreshold) {
                            showAlertDialog()
                        }
                    }

                    lastLat = lat
                    lastLng = lng
                    lastTimestamp = timestamp
                    updateStatusText("Tracking: ${lat.roundCoordinates()}, ${lng.roundCoordinates()}")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e(TAG, "Database error: ${error.message}")
                    updateStatusText("Error: ${error.message}")
                    Toast.makeText(this@ObserverActivity, "Ошибка базы данных", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun setupStatusText() {
        binding.statusText.apply {
            text = "Ожидание данных..."
            setTextColor(ContextCompat.getColor(this@ObserverActivity, R.color.text_primary))
        }
    }

    private fun updateStatusText(message: String) {
        binding.statusText.text = message
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
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    showPermissionRationale()
                }
                else -> {
                    requestNotificationPermission()
                }
            }
        }
    }

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle("Требуются уведомления")
            .setMessage("Для оповещений о состоянии водителя нужны разрешения")
            .setPositiveButton("Разрешить") { _, _ ->
                requestNotificationPermission()
            }
            .setNegativeButton("Отмена") { _, _ -> }
            .show()
    }

    private fun requestNotificationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_CODE_NOTIFICATION
        )
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
            .setNegativeButton("Отмена") { _, _ -> }
            .show()
    }

    private fun sendNotification() {
        NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("Водитель остановился")
            .setContentText("Проверьте состояние водителя")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build().let { notification ->
                NotificationManagerCompat.from(this).notify(1, notification)
            }
    }

    private fun isSameLocation(newLat: Double, newLng: Double): Boolean {
        return lastLat.roundCoordinates() == newLat.roundCoordinates() &&
                lastLng.roundCoordinates() == newLng.roundCoordinates()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_NOTIFICATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendNotification()
                }
            }
        }
    }
}