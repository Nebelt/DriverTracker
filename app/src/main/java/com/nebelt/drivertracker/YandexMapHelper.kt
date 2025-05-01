package com.nebelt.drivertracker


import android.content.Context
import android.graphics.PointF
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider

class YandexMapHelper(private val context: Context, private val apiKey: String) {

    private lateinit var mapView: MapView

    fun initialize() {
        MapKitFactory.setApiKey(apiKey)
        MapKitFactory.initialize(context)
    }

    fun setupMapView(mapView: MapView) {
        this.mapView = mapView
        moveToDefaultPosition()
    }

    fun moveToPosition(lat: Double, lng: Double) {
        val cameraPosition = CameraPosition(
            Point(lat, lng), // Точка
            15f,            // Зум
            0f,             // Азимут
            0f              // Наклон
        )

        mapView.map.move(
            cameraPosition,
            Animation(Animation.Type.SMOOTH, 0.5f),
            null
        )

        updateMarker(lat, lng)
    }

    private fun moveToDefaultPosition() {
        val cameraPosition = CameraPosition(
            Point(55.751244, 37.618423), // Москва
            5f,  // Зум для всей России
            0f,
            0f
        )

        mapView.map.move(cameraPosition)
    }

    private fun updateMarker(lat: Double, lng: Double) {
        mapView.map.mapObjects.clear()

        mapView.map.mapObjects.addPlacemark(Point(lat, lng)).apply {
            setIcon(
                ImageProvider.fromResource(
                    context,
                    R.drawable.blue_dot
                )
            )
            setIconStyle(
                IconStyle().apply {
                    anchor = PointF(0.5f, 0.5f)
                    scale = 1.0f
                }
            )
        }
    }

    fun onStart() {
        MapKitFactory.getInstance().onStart()
        mapView.onStart()
    }

    fun onStop() {
        mapView.onStop()
        MapKitFactory.getInstance().onStop()
    }
}