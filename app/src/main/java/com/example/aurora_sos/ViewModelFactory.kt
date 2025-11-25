package com.example.aurora_sos

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PrincipalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PrincipalViewModel(application) as T
        }
        if (modelClass.isAssignableFrom(HistorialViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HistorialViewModel(application) as T
        }
        if (modelClass.isAssignableFrom(ConfiguracionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ConfiguracionViewModel(application) as T
        }
        if (modelClass.isAssignableFrom(SensorHistorialViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SensorHistorialViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}