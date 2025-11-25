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
    // Datos de la API
    val temperaturaApi: Double = 30.0,
    val pronosticoHeladaApi: PronosticoHeladaApi? = null,
    val alertaApi: Alerta = Alerta.Estable,
    val nombreCiudad: String = "",
    val datosClimaticosApi: DatosClimaticosApi = DatosClimaticosApi(0.0, 0.0),
    val pronosticoPorHorasApi: List<PronosticoHoraApi> = emptyList(),
    val alertaPredictivaApi: PronosticoHeladaApi? = null,

    // Datos del Sensor
    val temperaturaSensor: Double = 30.0,
    val alertaSensor: Alerta = Alerta.Estable,
    val datosClimaticosSensor: DatosClimaticosSensor = DatosClimaticosSensor(0.0, 0, 0.0),

    // Estado General
    val error: String? = null,
    val isLoading: Boolean = true
)

data class PronosticoHeladaApi(
    val temperaturaMinima: Double,
    val hora: LocalDateTime
)

data class DatosClimaticosApi(
    val humedad: Double,
    val velocidadViento: Double
)

data class DatosClimaticosSensor(
    val humedad: Double,
    val lluvia: Long,
    val puntoRocio: Double
)

data class PronosticoHoraApi(
    val hora: LocalDateTime,
    val temperatura: Double
)

sealed class Alerta {
    data object Estable : Alerta() // Verde
    data object Moderado : Alerta() // Amarillo
    data object Helada : Alerta()   // Rojo
}

// --- ViewModel --- //

class PrincipalViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PrincipalUiState())
    val uiState = _uiState.asStateFlow()

    private val database = Firebase.database("https://aurorasos-default-rtdb.firebaseio.com/")
    private val auroraRef = database.getReference("aurora")

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
        auroraRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sensorData = snapshot.getValue(SensorData::class.java)
                if (sensorData != null) {
                    _uiState.update { state ->
                        state.copy(
                            temperaturaSensor = sensorData.temperatura,
                            datosClimaticosSensor = DatosClimaticosSensor(
                                humedad = sensorData.humedad.toDouble(),
                                lluvia = sensorData.lluvia,
                                puntoRocio = sensorData.puntoRocio
                            )
                        )
                    }
                    viewModelScope.launch {
                        val umbral = dataStoreManager.preferencesFlow.first().umbralHelada
                        actualizarAlertaSensor(umbral, sensorData.puntoRocio)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PrincipalViewModel", "Error al leer Firebase", error.toException())
                _uiState.update { it.copy(error = "No se pudo conectar al sensor.") }
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
                    PronosticoHoraApi(
                        hora = LocalDateTime.ofInstant(Instant.ofEpochSecond(it.dt), ZoneId.systemDefault()),
                        temperatura = it.main.temp
                    )
                }

                _uiState.update { state ->
                    state.copy(
                        temperaturaApi = responseActual.main.temp,
                        pronosticoHeladaApi = proximaHelada?.let { p -> PronosticoHeladaApi(p.main.temp, LocalDateTime.ofInstant(Instant.ofEpochSecond(p.dt), ZoneId.systemDefault())) },
                        datosClimaticosApi = DatosClimaticosApi(
                            humedad = responseActual.main.humidity,
                            velocidadViento = responseActual.wind.speed
                        ),
                        pronosticoPorHorasApi = pronostico24h,
                        alertaPredictivaApi = alertaPredictiva?.let { a -> PronosticoHeladaApi(a.main.temp, LocalDateTime.ofInstant(Instant.ofEpochSecond(a.dt), ZoneId.systemDefault())) }
                    )
                }
                actualizarAlertaApi(config.umbralHelada)

            } catch (e: Exception) {
                Log.e("PrincipalViewModel", "Error al llamar a OWM", e)
                _uiState.update { it.copy(error = "No se pudo obtener el pronóstico.") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // Lógica de alerta para la API
    fun actualizarAlertaApi(umbralCritico: Double) {
        val temp = _uiState.value.temperaturaApi
        val nuevaAlerta = when {
            temp <= umbralCritico -> Alerta.Helada
            temp <= 10.0 -> Alerta.Moderado
            else -> Alerta.Estable
        }
        _uiState.update { it.copy(alertaApi = nuevaAlerta) }
    }

    // Lógica de alerta para el Sensor (con nuevas reglas)
    fun actualizarAlertaSensor(umbralCritico: Double, puntoRocio: Double) {
        val temp = _uiState.value.temperaturaSensor
        val nuevaAlerta = when {
            temp <= umbralCritico -> Alerta.Helada
            temp <= 10.0 && puntoRocio <= 0.0 -> Alerta.Moderado
            else -> Alerta.Estable
        }
        _uiState.update { it.copy(alertaSensor = nuevaAlerta) }
    }

    override fun onCleared() {
        super.onCleared()
        client.close()
    }
}
