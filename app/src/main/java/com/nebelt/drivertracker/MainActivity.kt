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
        setContentView(binding.root)  // Устанавливаем корневое View

        binding.btnDriver.setOnClickListener {
            startActivity(Intent(this, DriverActivity::class.java))
        }

        binding.btnObserver.setOnClickListener {
            startActivity(Intent(this, ObserverActivity::class.java))
        }
    }
}