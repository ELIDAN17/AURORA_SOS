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
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// --- Data Classes (Estado de la Interfaz) ---

data class HistorialUiState(
    val isLoading: Boolean = true,
    val rangoSeleccionado: RangoTiempo = RangoTiempo.CINCO_DIAS,
    val datosGrafico: DatosGrafico = DatosGrafico.Vacio,
    val error: String? = null
)

sealed class DatosGrafico {
    data object Vacio : DatosGrafico()
    data class Historial(val datos: List<HistorialEntry>, val etiquetas: List<String>) : DatosGrafico()
    data class Pronostico(val datos: List<PronosticoEntry>) : DatosGrafico()
}

enum class RangoTiempo(val texto: String) {
    ULTIMAS_24_HORAS("24 H"),
    CINCO_DIAS("5 Días"),
    TREINTA_DIAS("30 Días")
}

// --- ViewModel ---

class HistorialViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState = _uiState.asStateFlow()

    private val dataStoreManager = DataStoreManager(application)

    private var datosHistorialCache: Map<LocalDate, HistorialEntry>? = null

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    init {
        obtenerHistorialFirebase {
            seleccionarRango(RangoTiempo.CINCO_DIAS)
        }
    }

    fun seleccionarRango(rango: RangoTiempo) {
        _uiState.update { it.copy(rangoSeleccionado = rango, isLoading = true, error = null) }
        viewModelScope.launch {
            when (rango) {
                RangoTiempo.ULTIMAS_24_HORAS, RangoTiempo.CINCO_DIAS -> obtenerPronosticoApi(rango)
                RangoTiempo.TREINTA_DIAS -> procesarDatosHistorial()
            }
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

    private fun obtenerHistorialFirebase(onComplete: () -> Unit) {
        val database = Firebase.database("https://aurorasos-default-rtdb.firebaseio.com/")
        val historialRef = database.getReference("historial")
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        historialRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val dataMapa = mutableMapOf<LocalDate, HistorialEntry>()
                    snapshot.children.forEach { fechaSnapshot ->
                        val fechaKeyStr = fechaSnapshot.key ?: return@forEach
                        try {
                            val fecha = LocalDate.parse(fechaKeyStr, formatter)
                            val tmin = (fechaSnapshot.child("tmin").value as? Number)?.toDouble() ?: 0.0
                            val tmax = (fechaSnapshot.child("tmax").value as? Number)?.toDouble() ?: 0.0
                            val precip = (fechaSnapshot.child("precip").value as? Number)?.toDouble() ?: 0.0
                            dataMapa[fecha] = HistorialEntry(tmin, tmax, precip)
                        } catch (e: Exception) {
                            Log.w("HistorialViewModel", "Error al parsear datos para $fechaKeyStr", e)
                        }
                    }
                    datosHistorialCache = dataMapa.toSortedMap()
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Error al leer el historial.") }
                } finally {
                    onComplete()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                _uiState.update { it.copy(isLoading = false, error = "No se pudo acceder a la base de datos.") }
                onComplete()
            }
        })
    }

    private fun procesarDatosHistorial() {
        val cache = datosHistorialCache
        if (cache == null) {
            _uiState.update { it.copy(isLoading = false, error = "El historial aún no está disponible.") }
            return
        }

        val hoy = LocalDate.now()
        val fechaLimite = hoy.minusDays(30)

        val datosFiltrados = cache
            .filterKeys { it.isAfter(fechaLimite) && !it.isAfter(hoy) }
            .toSortedMap()

        val datosParaGrafico = datosFiltrados.values.toList()
        val etiquetas = datosFiltrados.keys.map { it.format(DateTimeFormatter.ofPattern("dd/MM")) }

        _uiState.update {
            it.copy(isLoading = false, datosGrafico = DatosGrafico.Historial(datosParaGrafico, etiquetas))
        }
    }

    override fun onCleared() {
        super.onCleared()
        client.close()
    }
}
