package com.example.aurora_sos

import android.util.Log
import androidx.lifecycle.ViewModel
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// --- Data Classes (Estado de la Interfaz) ---

/**
 * Representa el estado completo de la interfaz de HistorialScreen.
 */
data class HistorialUiState(
    val isLoading: Boolean = true,
    val rangoSeleccionado: RangoTiempo = RangoTiempo.CINCO_DIAS,
    val datosGrafico: DatosGrafico = DatosGrafico.Vacio,
    val error: String? = null
)

/**
 * Contenedor para los datos que se van a dibujar en el gráfico.
 * Puede ser de tipo historial (Firebase) o pronóstico (API).
 */
sealed class DatosGrafico {
    data object Vacio : DatosGrafico()
    data class Historial(val datos: List<HistorialEntry>, val etiquetas: List<String>) : DatosGrafico()
    data class Pronostico(val datos: List<PronosticoEntry>) : DatosGrafico()
}

// --- Enum para los rangos de tiempo ---

enum class RangoTiempo(val texto: String) {
    ULTIMAS_24_HORAS("24 H"),
    CINCO_DIAS("5 Días"),
    TREINTA_DIAS("30 Días")
}

// --- ViewModel ---

class HistorialViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HistorialUiState())
    val uiState = _uiState.asStateFlow()

    // Cache para los datos de Firebase
    private var datosHistorialCache: Map<LocalDate, HistorialEntry>? = null

    // Cliente HTTP (Ktor)
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    init {
        // Carga inicial de datos de Firebase y luego actualiza para el rango por defecto
        obtenerHistorialFirebase {
            seleccionarRango(RangoTiempo.CINCO_DIAS)
        }
    }

    /**
     * Actualiza el rango de tiempo seleccionado y obtiene los datos correspondientes.
     */
    fun seleccionarRango(rango: RangoTiempo) {
        _uiState.update { it.copy(rangoSeleccionado = rango, isLoading = true, error = null) }

        viewModelScope.launch {
            when (rango) {
                RangoTiempo.ULTIMAS_24_HORAS, RangoTiempo.CINCO_DIAS -> obtenerPronosticoApi(rango)
                RangoTiempo.TREINTA_DIAS -> procesarDatosHistorial()
            }
        }
    }

    /**
     * Obtiene los datos del pronóstico desde la API de OpenWeatherMap.
     */
    private suspend fun obtenerPronosticoApi(rango: RangoTiempo) {
        val apiKey = "5435a01f60d70475e9294d39e22d30d0"
        val latPuno = "-15.84"
        val lonPuno = "-70.02"
        val url = "https://api.openweathermap.org/data/2.5/forecast?lat=$latPuno&lon=$lonPuno&appid=$apiKey&units=metric"

        try {
            val response: ForecastResponse = client.get(url).body()

            val itemsAProcesar = if (rango == RangoTiempo.ULTIMAS_24_HORAS) {
                response.list.take(8) // 8 items * 3h = 24h
            } else {
                response.list // La API da 40 items para 5 días
            }

            val pronosticoProcesado = itemsAProcesar.map { item ->
                PronosticoEntry(
                    timestamp = LocalDateTime.ofInstant(Instant.ofEpochSecond(item.dt), ZoneId.of("UTC")),
                    temp = item.main.temp,
                    humedad = item.main.humidity
                )
            }

            _uiState.update {
                it.copy(
                    isLoading = false,
                    datosGrafico = DatosGrafico.Pronostico(pronosticoProcesado)
                )
            }
        } catch (e: Exception) {
            Log.e("HistorialViewModel", "Error al llamar a la API de pronóstico", e)
            _uiState.update { it.copy(isLoading = false, error = "No se pudo cargar el pronóstico.") }
        }
    }

    /**
     * Obtiene y cachea los datos históricos desde Firebase. Se llama una sola vez.
     */
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
                            val entry = fechaSnapshot.getValue(HistorialEntry::class.java)
                            if (entry != null) {
                                dataMapa[fecha] = entry
                            }
                        } catch (e: Exception) {
                            Log.w("HistorialViewModel", "Error al parsear la fecha: $fechaKeyStr", e)
                        }
                    }
                    datosHistorialCache = dataMapa.toSortedMap()
                    Log.d("HistorialViewModel", "Datos de Firebase cargados: ${datosHistorialCache?.size} entradas.")
                } catch (e: Exception) {
                    Log.e("HistorialViewModel", "Error crítico al procesar datos de Firebase.", e)
                    _uiState.update { it.copy(error = "Error al leer el historial.") }
                } finally {
                    onComplete()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HistorialViewModel", "Error en Firebase: ${error.message}", error.toException())
                _uiState.update { it.copy(isLoading = false, error = "No se pudo acceder a la base de datos.") }
                onComplete()
            }
        })
    }

    /**
     * Filtra y procesa los datos del historial cacheados para el rango de 30 días.
     */
    private fun procesarDatosHistorial() {
        val cache = datosHistorialCache
        if (cache == null) {
            _uiState.update { it.copy(isLoading = false, error = "El historial aún no está disponible.") }
            // Reintentar la carga por si falló la primera vez
            obtenerHistorialFirebase { procesarDatosHistorial() }
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
            it.copy(
                isLoading = false,
                datosGrafico = DatosGrafico.Historial(datosParaGrafico, etiquetas)
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        client.close()
    }
}
