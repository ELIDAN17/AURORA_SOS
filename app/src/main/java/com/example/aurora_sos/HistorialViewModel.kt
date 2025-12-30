package com.example.aurora_sos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.LocalDateTime

class HistorialViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState = _uiState.asStateFlow()

    private val dataStoreManager = DataStoreManager(application)

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    init {
        seleccionarRango(RangoTiempo.SIETE_DIAS)
    }

    fun seleccionarRango(rango: RangoTiempo) {
        _uiState.update { it.copy(rangoSeleccionado = rango, isLoading = true, error = null) }
        viewModelScope.launch {
            obtenerPronosticoApi(rango)
        }
    }

    private suspend fun obtenerPronosticoApi(rango: RangoTiempo) {
        try {
            val config = dataStoreManager.preferencesFlow.first()
            val lat = config.latitud
            val lon = config.longitud

            val forecastDays = if (rango == RangoTiempo.SIETE_DIAS) 7 else 1
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                      "&hourly=temperature_2m,relative_humidity_2m,dew_point_2m&forecast_days=$forecastDays"

            val response: OpenMeteoResponse = client.get(url).body()

            response.hourly?.let { hourlyData ->
                val pronosticoProcesado = hourlyData.time.mapIndexed { index, timeString ->
                    PronosticoEntry(
                        timestamp = LocalDateTime.parse(timeString),
                        temp = hourlyData.temperature[index],
                        humedad = hourlyData.humidity[index].toDouble()
                    )
                }
                _uiState.update { it.copy(isLoading = false, datosGrafico = DatosGrafico.Pronostico(pronosticoProcesado)) }
            } ?: run {
                _uiState.update { it.copy(isLoading = false, error = "No se recibieron datos de pronóstico.") }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = "No se pudo cargar el pronóstico.") }
        }
    }

    override fun onCleared() {
        super.onCleared()
        client.close()
    }
}
