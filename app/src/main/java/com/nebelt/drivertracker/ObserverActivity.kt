package com.nebelt.drivertracker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.nebelt.drivertracker.databinding.ActivityObserverBinding // Автоматически генерируется

class ObserverActivity : AppCompatActivity() {
    private lateinit var binding: ActivityObserverBinding
    private val database = Firebase.database.reference
    private val driverId = "driver_location"
    private var lastLat: Double = 0.0
    private var lastLng: Double = 0.0
    private var lastTimestamp: Long = 0
    private val stationaryThreshold = 10000 // 1секунда = 1000

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityObserverBinding.inflate(layoutInflater)
        setContentView(binding.root)
        showTestNotification()
        // Удалите все обращения к синтетическому binding (например, binding.textView)

        database.child("drivers").child(driverId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lat = snapshot.child("lat").getValue(Double::class.java) ?: 0.0
                    val lng = snapshot.child("lng").getValue(Double::class.java) ?: 0.0
                    val timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0

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
                    Toast.makeText(this@ObserverActivity, "Ошибка базы данных", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        android.location.Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0]
    }

    private fun showAlertDialog() {
        AlertDialog.Builder(this)
            .setTitle("Внимание!")
            .setMessage("Водитель не двигается более 5 минут. Позвонить?")
            .setPositiveButton("Позвонить") { _, _ ->
                val intent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:+79211234567") // Замените на реальный номер
                }
                startActivity(intent)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun isSameLocation(newLat: Double, newLng: Double): Boolean {
        val roundedLastLat = lastLat.roundCoordinates()
        val roundedLastLng = lastLng.roundCoordinates()
        val roundedNewLat = newLat.roundCoordinates()
        val roundedNewLng = newLng.roundCoordinates()

        return roundedLastLat == roundedNewLat &&
                roundedLastLng == roundedNewLng
    }

    private fun showTestNotification() {
        val notification = NotificationCompat.Builder(this, "location_channel")
            .setContentTitle("Тест")
            .setContentText("Уведомление работает!")
            .setSmallIcon(R.drawable.ic_notification)
            .build()

        NotificationManagerCompat.from(this).notify(1, notification)
    }
}