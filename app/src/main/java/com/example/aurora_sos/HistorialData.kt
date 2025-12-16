package com.example.aurora_sos

import java.time.LocalDateTime

// --- MODELOS DE DATOS PARA LOS GRÁFICOS ---

data class HistorialEntry(
    val fecha: String,
    val tmin: Double,
    val tmax: Double,
    val precip: Double,
    val rocioMin: Double = 0.0
)

data class PronosticoEntry(
    val timestamp: LocalDateTime,
    val temp: Double,
    val humedad: Double
)

sealed class DatosGrafico {
    data object Vacio : DatosGrafico()
    data class Historial(val datos: List<HistorialEntry>, val etiquetas: List<String>) : DatosGrafico()
    data class Pronostico(val datos: List<PronosticoEntry>) : DatosGrafico()
}

// --- MODELOS PARA PANTALLA DE PRONÓSTICO (API) ---

enum class RangoTiempo(val texto: String) {
    ULTIMAS_24_HORAS("24 H"),
    SIETE_DIAS("7 Días")
}

data class HistorialUiState(
    val isLoading: Boolean = true,
    val rangoSeleccionado: RangoTiempo = RangoTiempo.SIETE_DIAS,
    val datosGrafico: DatosGrafico = DatosGrafico.Vacio,
    val error: String? = null
)

// --- MODELOS PARA PANTALLA DE HISTORIAL (SENSOR) ---

enum class RangoTiempoSensor(val texto: String, val dias: Long) {
    SIETE_DIAS("7 Días", 7),
    TREINTA_DIAS("30 Días", 30)
}
