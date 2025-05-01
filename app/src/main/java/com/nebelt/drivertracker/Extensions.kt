package com.nebelt.drivertracker

fun Double.roundCoordinates(decimals: Int = 3): Double {
    return "%.${decimals}f".format(this)
        .replace(',', '.')
        .toDouble()
}