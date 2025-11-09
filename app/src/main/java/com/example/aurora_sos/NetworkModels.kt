// Archivo: NetworkModels.kt
package com.example.aurora_sos
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNames

@OptIn(InternalSerializationApi::class)
@Serializable
data class WeatherResponse(
    val main: MainData
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class MainData(
    val temp: Double
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class ForecastResponse(
    val list: List<ForecastItem>
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class ForecastItem(
    val main: ForecastMainData,
    val dt: Long
)

@OptIn(InternalSerializationApi::class)
@Serializable
data class ForecastMainData(
    val temp: Double,
    @JsonNames("temp_min")
    val tempMin: Double,
    val humidity: Double
)
