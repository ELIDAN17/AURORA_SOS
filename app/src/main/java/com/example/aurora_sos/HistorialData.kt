package com.example.aurora_sos

import com.google.firebase.database.IgnoreExtraProperties
import java.time.LocalDateTime

/**
 * Representa una entrada de datos históricos de Firebase.
 * Los campos deben coincidir con la estructura de la base de datos.
 */
@IgnoreExtraProperties
data class HistorialEntry(
    val tmin: Double = 0.0,
    val tmax: Double = 0.0,
    val precip: Double = 0.0
)

/**
 * Representa una entrada de datos de pronóstico de la API de OpenWeatherMap.
 */
data class PronosticoEntry(
    val timestamp: LocalDateTime,
    val temp: Double,
    val humedad: Double
)
