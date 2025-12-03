package com.example.aurora_sos

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Define una clase de datos para encapsular todas las preferencias
data class UserPreferences(
    val umbralHelada: Double,
    val notificacionesActivas: Boolean,
    val latitud: Double,
    val longitud: Double,
    val nombreCiudad: String,
    val codigoPais: String
)

// Extensión para acceder al DataStore
val Context.dataStore by preferencesDataStore(name = "configuracion_aurora")

class DataStoreManager(context: Context) {

    private val dataStore = context.dataStore

    // Claves para cada una de las preferencias
    private object PreferencesKeys {
        val UMBRAL_HELADA = doublePreferencesKey("umbral_helada")
        val NOTIFICACIONES_ACTIVAS = booleanPreferencesKey("notificaciones_activas")
        val LATITUD = doublePreferencesKey("latitud")
        val LONGITUD = doublePreferencesKey("longitud")
        val NOMBRE_CIUDAD = stringPreferencesKey("nombre_ciudad")
        val CODIGO_PAIS = stringPreferencesKey("codigo_pais")
    }

    // Flow que emite el objeto UserPreferences completo
    val preferencesFlow: Flow<UserPreferences> = dataStore.data.map { preferences ->
        UserPreferences(
            umbralHelada = preferences[PreferencesKeys.UMBRAL_HELADA] ?: 2.0,
            notificacionesActivas = preferences[PreferencesKeys.NOTIFICACIONES_ACTIVAS] ?: true,
            latitud = preferences[PreferencesKeys.LATITUD] ?: -12.0464, // Lima Lat
            longitud = preferences[PreferencesKeys.LONGITUD] ?: -77.0428, // Lima Lon
            nombreCiudad = preferences[PreferencesKeys.NOMBRE_CIUDAD] ?: "Lima",
            codigoPais = preferences[PreferencesKeys.CODIGO_PAIS] ?: "PE"
        )
    }

    // Función para guardar todas las preferencias
    suspend fun saveConfiguracion(prefs: UserPreferences) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.UMBRAL_HELADA] = prefs.umbralHelada
            preferences[PreferencesKeys.NOTIFICACIONES_ACTIVAS] = prefs.notificacionesActivas
            preferences[PreferencesKeys.LATITUD] = prefs.latitud
            preferences[PreferencesKeys.LONGITUD] = prefs.longitud
            preferences[PreferencesKeys.NOMBRE_CIUDAD] = prefs.nombreCiudad
            preferences[PreferencesKeys.CODIGO_PAIS] = prefs.codigoPais
        }
    }
}
