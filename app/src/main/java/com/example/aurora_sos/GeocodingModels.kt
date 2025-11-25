package com.example.aurora_sos

import kotlinx.serialization.Serializable

@Serializable
data class GeocodingResult(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String
)
