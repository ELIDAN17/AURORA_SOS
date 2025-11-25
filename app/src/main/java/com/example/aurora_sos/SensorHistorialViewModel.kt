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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SensorHistorialViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SensorHistorialUiState())
    val uiState = _uiState.asStateFlow()

    private var datosHistorialCache: Map<LocalDate, SensorHistorial>? = null

    init {
        obtenerHistorialFirebase { 
            seleccionarRango(RangoTiempoSensor.SIETE_DIAS)
        }
    }

    fun seleccionarRango(rango: RangoTiempoSensor) {
        _uiState.update { it.copy(rangoSeleccionado = rango, isLoading = true, error = null) }
        viewModelScope.launch {
            procesarDatosHistorial(rango.dias)
        }
    }

    private fun obtenerHistorialFirebase(onComplete: () -> Unit) {
        val database = Firebase.database("https://aurorasos-default-rtdb.firebaseio.com/")
        val historialRef = database.getReference("historial")
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        historialRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val dataMapa = mutableMapOf<LocalDate, SensorHistorial>()
                    snapshot.children.forEach { fechaSnapshot ->
                        val fechaKeyStr = fechaSnapshot.key ?: return@forEach
                        try {
                            val fecha = LocalDate.parse(fechaKeyStr, formatter)
                            val historial = fechaSnapshot.getValue(SensorHistorial::class.java)
                            if (historial != null) {
                                dataMapa[fecha] = historial
                            }
                        } catch (e: Exception) {
                            Log.w("SensorHistorialViewModel", "Error al parsear datos para $fechaKeyStr", e)
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

    private fun procesarDatosHistorial(dias: Long) {
        val cache = datosHistorialCache
        if (cache == null) {
            _uiState.update { it.copy(isLoading = false, error = "El historial aún no está disponible.") }
            return
        }

        val hoy = LocalDate.now()
        val fechaLimite = hoy.minusDays(dias)

        val datosFiltrados = cache
            .filterKeys { it.isAfter(fechaLimite) && !it.isAfter(hoy) }
            .toSortedMap()

        // --- CORRECCIÓN AQUÍ ---
        val datosParaGrafico = datosFiltrados.values.toList().map { 
            HistorialEntry(it.tmin, it.tmax, it.precip.toDouble(), it.rocioMin)
        }
        val etiquetas = datosFiltrados.keys.map { it.format(DateTimeFormatter.ofPattern("dd/MM")) }

        _uiState.update {
            it.copy(isLoading = false, datosGrafico = DatosGrafico.Historial(datosParaGrafico, etiquetas))
        }
    }
}
