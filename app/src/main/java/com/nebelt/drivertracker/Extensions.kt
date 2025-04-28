package com.nebelt.drivertracker

fun Double.roundCoordinates(decimals: Int = 2): Double {
    return "%.${decimals}f".format(this)
        .replace(',', '.')
        .toDouble()
}