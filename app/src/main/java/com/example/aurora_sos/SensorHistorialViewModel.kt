package com.example.aurora_sos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// --- Modelo de UI ---
data class SensorHistorialUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val datosGrafico: DatosGrafico = DatosGrafico.Vacio,
    val rangoSeleccionado: RangoTiempoSensor = RangoTiempoSensor.SIETE_DIAS,
    val eventosTendencia: List<TendenciaEvento> = emptyList()
)

// --- ViewModel ---
class SensorHistorialViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(SensorHistorialUiState())
    val uiState = _uiState.asStateFlow()

    private val database = Firebase.database("https://aurorasos-default-rtdb.firebaseio.com/")
    private val historialRef = database.getReference("historial")
    private val eventosRef = database.getReference("eventos_tendencia")

    init {
        seleccionarRango(RangoTiempoSensor.SIETE_DIAS)
        cargarEventosDeTendencia()
    }

    fun seleccionarRango(rango: RangoTiempoSensor) {
        _uiState.update { it.copy(rangoSeleccionado = rango, isLoading = true, error = null) }
        viewModelScope.launch {
            obtenerHistorialSensor(rango)
        }
    }

    private fun cargarEventosDeTendencia() {
        eventosRef.orderByKey().limitToLast(50)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val eventos = snapshot.children.mapNotNull { it.getValue(TendenciaEvento::class.java) }
                    _uiState.update { it.copy(eventosTendencia = eventos.reversed()) }
                }

                override fun onCancelled(error: DatabaseError) {
                     _uiState.update { it.copy(error = "No se pudieron cargar los eventos de tendencia.") }
                }
            })
    }

    private fun obtenerHistorialSensor(rango: RangoTiempoSensor) {
        val diasAtras = rango.dias
        val fechaInicio = LocalDate.now().minusDays(diasAtras)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        historialRef.orderByKey().startAt(fechaInicio.format(formatter))
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val historial = snapshot.children.mapNotNull { data ->
                        val fecha = data.key ?: return@mapNotNull null
                        val entry = data.getValue(SensorHistorial::class.java) ?: return@mapNotNull null
                        // ¡CORREGIDO! Se incluye la fecha en la creación del objeto
                        HistorialEntry(fecha, entry.tmin, entry.tmax, entry.precip.toDouble(), entry.rocioMin)
                    }
                    val etiquetas = historial.map { it.fecha.substring(5) } // Ahora 'it.fecha' existe

                    _uiState.update { it.copy(
                        isLoading = false,
                        datosGrafico = DatosGrafico.Historial(historial, etiquetas)
                    ) }
                }

                override fun onCancelled(error: DatabaseError) {
                    _uiState.update { it.copy(isLoading = false, error = "No se pudo cargar el historial.") }
                }
            })
    }
}
