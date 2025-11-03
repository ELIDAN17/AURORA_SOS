// Archivo: NotificationService.kt
package com.example.aurora_sos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat

class NotificationService(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val CHANNEL_ID = "aurora_sos_channel"

    // Crear el Canal de Notificación
    fun createNotificationChannel() {
        // El canal solo se crea en Android 8.0 (API 26) o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertas de Helada", // Nombre que ve el usuario en los ajustes de la app
                NotificationManager.IMPORTANCE_HIGH // Importancia alta para que aparezca y suene
            ).apply {
                description = "Canal para notificaciones de riesgo de helada."
                enableLights(true)
                lightColor = Color.RED
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    // Función para mostrar la notificación
    fun showNotification(temperatura: Double) {
        // Sonido de notificación por defecto del sistema
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("❄️ Alerta de Helada ❄️")
            .setContentText("¡Riesgo detectado! La temperatura ha bajado a ${temperatura}°C.")
            // Icono pequeño que aparece en la barra de estado (debes agregarlo a tus recursos)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setSound(soundUri) // Asignar el sonido
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Prioridad alta
            .setAutoCancel(true) // La notificación se cierra al tocarla
            .build()

        // El ID de la notificación. Si envías otra con el mismo ID, se actualiza.
        notificationManager.notify(1, notification)
    }
}