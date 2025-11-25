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
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

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

            val apiKey = "5435a01f60d70475e9294d39e22d30d0"
            val url = "https://api.openweathermap.org/data/2.5/forecast?lat=$lat&lon=$lon&appid=$apiKey&units=metric"

            val response: ForecastResponse = client.get(url).body()
            val itemsAProcesar = if (rango == RangoTiempo.ULTIMAS_24_HORAS) response.list.take(8) else response.list

            val pronosticoProcesado = itemsAProcesar.map {
                PronosticoEntry(
                    timestamp = LocalDateTime.ofInstant(Instant.ofEpochSecond(it.dt), ZoneId.of("UTC")),
                    temp = it.main.temp,
                    humedad = it.main.humidity
                )
            }
            _uiState.update { it.copy(isLoading = false, datosGrafico = DatosGrafico.Pronostico(pronosticoProcesado)) }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, error = "No se pudo cargar el pronóstico.") }
        }
    }

    override fun onCleared() {
        super.onCleared()
        client.close()
    }
}
