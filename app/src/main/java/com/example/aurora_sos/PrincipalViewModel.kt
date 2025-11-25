package com.example.aurora_sos

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
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
import java.time.ZoneId
import java.time.Instant

// --- Estado de la Interfaz --- //

data class PrincipalUiState(
    val temperaturaActual: Double = 30.0,
    val pronosticoHelada: PronosticoHelada? = null,
    val alerta: Alerta = Alerta.Estable,
    val error: String? = null,
    val isLoading: Boolean = true,
    val nombreCiudad: String = "",
    val datosClimaticos: DatosClimaticos = DatosClimaticos(0.0, 0.0),
    val pronosticoPorHoras: List<PronosticoHora> = emptyList(),
    val alertaPredictiva: PronosticoHelada? = null // Nuevo
)

data class PronosticoHelada(
    val temperaturaMinima: Double,
    val hora: LocalDateTime
)

data class DatosClimaticos(
    val humedad: Double,
    val velocidadViento: Double
)

data class PronosticoHora(
    val hora: LocalDateTime,
    val temperatura: Double
)

sealed class Alerta {
    data object Estable : Alerta()
    data object Moderado : Alerta()
    data object Helada : Alerta()
}

// --- ViewModel --- //

class PrincipalViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PrincipalUiState())
    val uiState = _uiState.asStateFlow()

    private val database = Firebase.database("https://aurorasos-default-rtdb.firebaseio.com/")
    private val tempRef = database.getReference("aurora/temperatura")

    private val dataStoreManager = DataStoreManager(application)

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    init {
        conectarAFirebase()
    }

    private fun conectarAFirebase() {
        tempRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val temp = snapshot.getValue(Double::class.java)
                if (temp != null) {
                    _uiState.update { it.copy(temperaturaActual = temp) }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PrincipalViewModel", "Error al leer Firebase", error.toException())
                _uiState.update { it.copy(error = "No se pudo conectar a la base de datos.") }
            }
        })
    }

    fun lanzarLlamadaApi() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val config = dataStoreManager.preferencesFlow.first()
                val lat = config.latitud
                val lon = config.longitud
                _uiState.update { it.copy(nombreCiudad = config.nombreCiudad) }

                val apiKey = "5435a01f60d70475e9294d39e22d30d0"
                val urlClimaActual = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey&units=metric"
                val urlPronostico = "https://api.openweathermap.org/data/2.5/forecast?lat=$lat&lon=$lon&appid=$apiKey&units=metric"

                val responseActual: WeatherResponse = client.get(urlClimaActual).body()
                val responsePronostico: ForecastResponse = client.get(urlPronostico).body()

                val ahora = LocalDateTime.now()
                val limite24h = ahora.plusHours(24)

                val pronosticoProximas24h = responsePronostico.list.filter { 
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(it.dt), ZoneId.systemDefault()).isAfter(ahora) &&
                    LocalDateTime.ofInstant(Instant.ofEpochSecond(it.dt), ZoneId.systemDefault()).isBefore(limite24h)
                }

                val proximaHelada = pronosticoProximas24h.minByOrNull { it.main.temp }
                val alertaPredictiva = pronosticoProximas24h.firstOrNull { it.main.temp <= config.umbralHelada }

                val pronostico24h = responsePronostico.list.take(8).map {
                    PronosticoHora(
                        hora = LocalDateTime.ofInstant(Instant.ofEpochSecond(it.dt), ZoneId.systemDefault()),
                        temperatura = it.main.temp
                    )
                }

                _uiState.update {
                    it.copy(
                        pronosticoHelada = proximaHelada?.let { p -> PronosticoHelada(p.main.temp, LocalDateTime.ofInstant(Instant.ofEpochSecond(p.dt), ZoneId.systemDefault())) },
                        datosClimaticos = DatosClimaticos(
                            humedad = responseActual.main.humidity,
                            velocidadViento = responseActual.wind.speed
                        ),
                        pronosticoPorHoras = pronostico24h,
                        alertaPredictiva = alertaPredictiva?.let { a -> PronosticoHelada(a.main.temp, LocalDateTime.ofInstant(Instant.ofEpochSecond(a.dt), ZoneId.systemDefault())) }
                    )
                }

                val tempActualApi = responseActual.main.temp
                tempRef.setValue(tempActualApi)

            } catch (e: Exception) {
                Log.e("PrincipalViewModel", "Error al llamar a OWM", e)
                _uiState.update { it.copy(error = "No se pudo obtener el pronóstico.") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun actualizarAlerta(umbralCritico: Double) {
        val temp = _uiState.value.temperaturaActual
        val nuevaAlerta = when {
            temp <= umbralCritico -> Alerta.Helada
            temp <= 10.0 -> Alerta.Moderado
            else -> Alerta.Estable
        }
        _uiState.update { it.copy(alerta = nuevaAlerta) }
    }

    override fun onCleared() {
        super.onCleared()
        client.close()
    }
}