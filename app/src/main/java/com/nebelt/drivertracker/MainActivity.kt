package com.nebelt.drivertracker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.nebelt.drivertracker.databinding.ActivityMainBinding  // Автоматически генерируется

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Показываем текст при выборе роли
        binding.btnDriver.setOnClickListener {
            binding.tvStatus.text = "Режим водителя: отправка координат..."
            startActivity(Intent(this, DriverActivity::class.java))
        }

        binding.btnObserver.setOnClickListener {
            binding.tvStatus.text = "Режим наблюдателя: отслеживание..."
            startActivity(Intent(this, ObserverActivity::class.java))
        }
    }
}