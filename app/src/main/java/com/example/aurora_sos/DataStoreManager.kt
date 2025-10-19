// Archivo: DataStoreManager.kt

package com.example.aurora_sos
//https://aurorasos-default-rtdb.firebaseio.com/
import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extensión para acceder al DataStore de forma singleton
// El nombre del archivo de preferencias será "configuracion_aurora"
val Context.dataStore by preferencesDataStore(name = "configuracion_aurora")

class DataStoreManager(context: Context) {

    // 1. Definición de las claves de las preferencias
    private object PreferencesKeys {
        val UMBRAL_HELADA = doublePreferencesKey("umbral_helada")
        val NOTIFICACIONES_ACTIVAS = booleanPreferencesKey("notificaciones_activas")
    }

    private val dataStore = context.dataStore

    // 2. Función para obtener las preferencias (Flow)
    // Devuelve un Flow de un Pair<Double, Boolean>
    val preferencesFlow: Flow<Pair<Double, Boolean>> = dataStore.data
        .map { preferences ->
            // Usa 2.0 y true como valores por defecto si no existe
            val umbral = preferences[PreferencesKeys.UMBRAL_HELADA] ?: 2.0
            val notificaciones = preferences[PreferencesKeys.NOTIFICACIONES_ACTIVAS] ?: true
            Pair(umbral, notificaciones)
        }

    // 3. Función de Guardado (Suspender Function)
    suspend fun saveConfiguracion(umbral: Double, notificaciones: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.UMBRAL_HELADA] = umbral
            preferences[PreferencesKeys.NOTIFICACIONES_ACTIVAS] = notificaciones
        }
    }
}