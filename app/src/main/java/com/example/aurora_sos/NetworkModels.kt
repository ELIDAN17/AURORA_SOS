package com.example.aurora_sos

import com.google.firebase.database.PropertyName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

// --- MODELOS PARA LA API DE OPENWEATHERMAP ---

@Serializable
data class WeatherResponse(
    val main: MainData,
    val wind: WindData
)

@Serializable
data class MainData(
    val temp: Double,
    val humidity: Double
)

@Serializable
data class WindData(
    val speed: Double
)

@Serializable
data class ForecastResponse(
    val list: List<ForecastItem>
)

@Serializable
data class ForecastItem(
    val main: ForecastMainData,
    val dt: Long
)

@Serializable
data class ForecastMainData(
    val temp: Double,
    @JsonNames("temp_min")
    val tempMin: Double,
    val humidity: Double
)

// --- MODELOS PARA FIREBASE (CORREGIDOS) ---

// Usado para el nodo "aurora"
data class SensorData(
    val temperatura: Double = 0.0,
    val humedad: Long = 0L,           // Corregido para que coincida con Firebase
    val lluvia: Long = 0L,
    @get:PropertyName("punto_rocio") @set:PropertyName("punto_rocio")
    var puntoRocio: Double = 0.0
)

// Usado para el nodo "historial"
data class SensorHistorial(
    val precip: Long = 0L,            // Corregido para que coincida con Firebase
    @get:PropertyName("rocio_min") @set:PropertyName("rocio_min")
    var rocioMin: Double = 0.0,
    val tmax: Double = 0.0,
    val tmin: Double = 0.0
)
