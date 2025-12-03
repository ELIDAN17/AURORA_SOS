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
import kotlin.math.abs

// --- Estado de la Interfaz --- //

data class PrincipalUiState(
    // Datos de la API
    val temperaturaApi: Double = 30.0,
    val pronosticoHeladaApi: PronosticoHeladaApi? = null,
    val alertaApi: Alerta = Alerta.Estable,
    val nombreCiudad: String = "",
    val datosClimaticosApi: DatosClimaticosApi = DatosClimaticosApi(0.0, 0.0, 0.0, 0.0),
    val pronosticoPorHorasApi: List<PronosticoHoraApi> = emptyList(),
    val alertaPredictivaApi: PronosticoHeladaApi? = null,

    // Datos del Sensor
    val temperaturaSensor: Double = 30.0,
    val alertaSensor: Alerta = Alerta.Estable,
    val datosClimaticosSensor: DatosClimaticosSensor = DatosClimaticosSensor(0.0, 0, 0.0),
    val indicadorRiesgo: IndicadorRiesgo = IndicadorRiesgo.Estable(),

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
    val velocidadViento: Double,
    val puntoRocio: Double,
    val soilTemperature: Double
)

data class DatosClimaticosSensor(
    val humedad: Double,
    val lluvia: Long,
    val puntoRocio: Double
)

data class PronosticoHoraApi(
    val hora: LocalDateTime,
    val temperatura: Double,
    val dewPoint: Double,
    val soilTemperature: Double
)

sealed class Alerta {
    data object Estable : Alerta() // Verde
    data object Moderado : Alerta() // Amarillo
    data object Helada : Alerta()   // Rojo
}

sealed class IndicadorRiesgo {
    abstract val message: String
    data class Estable(override val message: String = "Tendencia Estable") : IndicadorRiesgo()
    data class Riesgo(override val message: String) : IndicadorRiesgo()
    data class Peligro(override val message: String) : IndicadorRiesgo()
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

    private var datosSensorAnteriores: SensorData? = null
    private var tiempoLecturaAnterior: Long = 0L

    init {
        conectarAFirebase()
    }

    private fun conectarAFirebase() {
        auroraRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sensorData = snapshot.getValue(SensorData::class.java)
                if (sensorData != null) {
                    val datosSensorActuales = sensorData
                    _uiState.update { state ->
                        state.copy(
                            temperaturaSensor = datosSensorActuales.temperatura,
                            datosClimaticosSensor = DatosClimaticosSensor(
                                humedad = datosSensorActuales.humedad.toDouble(),
                                lluvia = datosSensorActuales.lluvia,
                                puntoRocio = datosSensorActuales.puntoRocio
                            )
                        )
                    }
                    viewModelScope.launch {
                        val umbralHelada = dataStoreManager.preferencesFlow.first().umbralHelada
                        actualizarAlertaSensor(umbralHelada, datosSensorActuales.puntoRocio)
                        analizarTendenciaSensor(datosSensorActuales, umbralHelada)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PrincipalViewModel", "Error al leer Firebase", error.toException())
                _uiState.update { it.copy(error = "No se pudo conectar al sensor.") }
            }
        })
    }

    private fun analizarTendenciaSensor(datosNuevos: SensorData, umbralHelada: Double) {
        val datosAntiguos = datosSensorAnteriores
        val tiempoLecturaActual = System.currentTimeMillis()
        var nuevoIndicador: IndicadorRiesgo = IndicadorRiesgo.Estable()

        if (datosAntiguos != null && tiempoLecturaAnterior > 0) {
            val deltaTiempoMin = (tiempoLecturaActual - tiempoLecturaAnterior) / (1000.0 * 60.0)

            if (deltaTiempoMin > 0.5) { // Analizar solo si ha pasado un tiempo razonable (30s)
                val caidaTemperatura = datosAntiguos.temperatura - datosNuevos.temperatura
                val velocidadCaidaPorMin = caidaTemperatura / deltaTiempoMin

                if (velocidadCaidaPorMin > 0.05) { // Si la temperatura baja a un ritmo notable
                    val tempRestante = datosNuevos.temperatura - umbralHelada
                    if (tempRestante > 0) {
                        val minutosEstimados = (tempRestante / velocidadCaidaPorMin).toInt()
                        nuevoIndicador = if (minutosEstimados < 120) {
                            IndicadorRiesgo.Peligro("Helada posible en ~${minutosEstimados} min si la tendencia continúa.")
                        } else {
                            IndicadorRiesgo.Riesgo("La temperatura está bajando. ¡Abrígate!")
                        }
                    }
                } else if (velocidadCaidaPorMin < -0.1) { // Si la temperatura sube
                     nuevoIndicador = IndicadorRiesgo.Estable("La temperatura está subiendo.")
                } 
            }
        }

        _uiState.update { it.copy(indicadorRiesgo = nuevoIndicador) }
        datosSensorAnteriores = datosNuevos
        tiempoLecturaAnterior = tiempoLecturaActual
    }


    fun lanzarLlamadaApi() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val config = dataStoreManager.preferencesFlow.first()
                val lat = config.latitud
                val lon = config.longitud
                _uiState.update { it.copy(nombreCiudad = config.nombreCiudad) }

                val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
                        "&current=temperature_2m,relative_humidity_2m,dew_point_2m,wind_speed_10m,soil_temperature_0cm" +
                        "&hourly=temperature_2m,dew_point_2m,soil_temperature_0cm&forecast_days=2"

                val response: OpenMeteoResponse = client.get(url).body()

                response.current?.let { currentData ->
                    _uiState.update { state ->
                        state.copy(
                            temperaturaApi = currentData.temperature,
                            datosClimaticosApi = DatosClimaticosApi(
                                humedad = currentData.humidity.toDouble(),
                                velocidadViento = currentData.windSpeed,
                                puntoRocio = currentData.dewPoint,
                                soilTemperature = currentData.soilTemperature
                            )
                        )
                    }
                    actualizarAlertaApi(config.umbralHelada)
                }

                response.hourly?.let { hourlyData ->
                    val ahora = LocalDateTime.now()
                    val limite24h = ahora.plusHours(24)

                    val indiceActual = hourlyData.time.indexOfFirst { timeString ->
                        LocalDateTime.parse(timeString).isAfter(ahora)
                    }.takeIf { it != -1 } ?: 0

                    val pronosticoProximas8h = hourlyData.time.subList(indiceActual, (indiceActual + 8).coerceAtMost(hourlyData.time.size))
                        .mapIndexed { index, timeString ->
                            val actualIndex = indiceActual + index
                            PronosticoHoraApi(
                                hora = LocalDateTime.parse(timeString),
                                temperatura = hourlyData.temperature[actualIndex],
                                dewPoint = hourlyData.dewPoint[actualIndex],
                                soilTemperature = hourlyData.soilTemperature[actualIndex]
                            )
                        }

                    val pronosticoProximas24h = hourlyData.time
                        .mapIndexed { index, timeString -> Pair(LocalDateTime.parse(timeString), hourlyData.temperature[index]) }
                        .filter { it.first.isAfter(ahora) && it.first.isBefore(limite24h) }

                    val proximaHelada = pronosticoProximas24h.minByOrNull { it.second }
                    val alertaPredictiva = pronosticoProximas24h.firstOrNull { it.second <= config.umbralHelada }

                    _uiState.update { state ->
                        state.copy(
                            pronosticoPorHorasApi = pronosticoProximas8h,
                            pronosticoHeladaApi = proximaHelada?.let { (hora, temp) -> PronosticoHeladaApi(temp, hora) },
                            alertaPredictivaApi = alertaPredictiva?.let { (hora, temp) -> PronosticoHeladaApi(temp, hora) }
                        )
                    }
                }

                if (response.current == null && response.hourly == null) {
                    _uiState.update { it.copy(error = "Respuesta de la API inválida.") }
                }

            } catch (e: Exception) {
                Log.e("PrincipalViewModel", "Error al llamar a Open-Meteo", e)
                _uiState.update { it.copy(error = "No se pudo obtener el pronóstico.") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun actualizarAlertaApi(umbralCritico: Double) {
        val temp = _uiState.value.temperaturaApi
        val nuevaAlerta = when {
            temp <= umbralCritico -> Alerta.Helada
            temp <= 10.0 -> Alerta.Moderado
            else -> Alerta.Estable
        }
        _uiState.update { it.copy(alertaApi = nuevaAlerta) }
    }

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
