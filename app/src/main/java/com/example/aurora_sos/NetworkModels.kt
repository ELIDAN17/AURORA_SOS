@file:OptIn(kotlinx.serialization.InternalSerializationApi::class, kotlinx.serialization.ExperimentalSerializationApi::class)
package com.example.aurora_sos

import com.google.firebase.database.PropertyName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

// --- MODELOS PARA LA API DE OPEN-METEO (CLIMA) ---

@Serializable
data class OpenMeteoResponse(
    val current: CurrentWeatherData? = null,
    val hourly: HourlyData? = null,
    val daily: DailyData? = null
)

@Serializable
data class CurrentWeatherData(
    @JsonNames("temperature_2m")
    val temperature: Double = 0.0,
    @JsonNames("relative_humidity_2m")
    val humidity: Int = 0,
    @JsonNames("wind_speed_10m")
    val windSpeed: Double = 0.0,
    @JsonNames("dew_point_2m")
    val dewPoint: Double = 0.0,
    @JsonNames("soil_temperature_0cm")
    val soilTemperature: Double = 0.0
)

@Serializable
data class HourlyData(
    val time: List<String> = emptyList(),
    @JsonNames("temperature_2m")
    val temperature: List<Double> = emptyList(),
    @JsonNames("relative_humidity_2m")
    val humidity: List<Int> = emptyList(),
    @JsonNames("dew_point_2m")
    val dewPoint: List<Double> = emptyList(),
    @JsonNames("soil_temperature_0cm")
    val soilTemperature: List<Double> = emptyList()
)

@Serializable
data class DailyData(
    val time: List<String> = emptyList(),
    @JsonNames("temperature_2m_max")
    val temperatureMax: List<Double> = emptyList(),
    @JsonNames("temperature_2m_min")
    val temperatureMin: List<Double> = emptyList(),
    @JsonNames("precipitation_sum")
    val precipitationSum: List<Double> = emptyList()
)

// --- MODELOS PARA FIREBASE ---

data class SensorData(
    val temperatura: Double = 0.0,
    val humedad: Long = 0L,
    val lluvia: Long = 0L,
    @get:PropertyName("punto_rocio") @set:PropertyName("punto_rocio")
    var puntoRocio: Double = 0.0
)

data class SensorHistorial(
    val precip: Long = 0L,
    @get:PropertyName("rocio_min") @set:PropertyName("rocio_min")
    var rocioMin: Double = 0.0,
    val tmax: Double = 0.0,
    val tmin: Double = 0.0
)
