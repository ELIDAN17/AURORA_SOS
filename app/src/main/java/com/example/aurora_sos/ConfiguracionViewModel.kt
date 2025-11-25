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

data class ConfiguracionUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val guardadoConExito: Boolean = false,
    val ciudadEncontrada: String? = null, // Nuevo
    val paisEncontrado: String? = null     // Nuevo
)

class ConfiguracionViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ConfiguracionUiState())
    val uiState = _uiState.asStateFlow()

    private val dataStoreManager = DataStoreManager(application)

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    fun guardarConfiguracion(
        umbral: String,
        ciudad: String,
        pais: String,
        notificacionesActivas: Boolean
    ) {
        _uiState.update { it.copy(isLoading = true, error = null, guardadoConExito = false) }
        viewModelScope.launch {
            try {
                val apiKey = "5435a01f60d70475e9294d39e22d30d0"
                val url = "https://api.openweathermap.org/geo/1.0/direct?q=$ciudad,$pais&limit=1&appid=$apiKey"
                val results: List<GeocodingResult> = client.get(url).body()

                if (results.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, error = "No se encontró la ciudad.") }
                    return@launch
                }

                val bestResult = results.first()

                val currentConfig = dataStoreManager.preferencesFlow.first()
                val newConfig = currentConfig.copy(
                    umbralHelada = umbral.toDoubleOrNull() ?: currentConfig.umbralHelada,
                    notificacionesActivas = notificacionesActivas,
                    latitud = bestResult.lat,
                    longitud = bestResult.lon,
                    nombreCiudad = bestResult.name,
                    codigoPais = bestResult.country
                )
                dataStoreManager.saveConfiguracion(newConfig)

                _uiState.update { it.copy(isLoading = false, guardadoConExito = true) }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error al guardar la configuración.") }
            }
        }
    }

    // --- ¡NUEVA FUNCIÓN! ---
    fun buscarCiudadPorCoordenadas(lat: Double, lon: Double) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val apiKey = "5435a01f60d70475e9294d39e22d30d0"
                val url = "https://api.openweathermap.org/geo/1.0/reverse?lat=$lat&lon=$lon&limit=1&appid=$apiKey"
                val results: List<GeocodingResult> = client.get(url).body()

                if (results.isNotEmpty()) {
                    val bestResult = results.first()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            ciudadEncontrada = bestResult.name,
                            paisEncontrado = bestResult.country
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "No se pudo determinar la ciudad.") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Error de red al buscar la ciudad.") }
            }
        }
    }

    fun resetState() {
        _uiState.value = ConfiguracionUiState()
    }

    override fun onCleared() {
        super.onCleared()
        client.close()
    }
}