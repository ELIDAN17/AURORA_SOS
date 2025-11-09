package com.example.aurora_sos

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class NotificationService(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val CHANNEL_ID = "aurora_sos_channel_alarm"

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // URI del sonido de la alarma
            val soundUri = Uri.parse("android.resource://" + context.packageName + "/" + R.raw.alarm)

            // Atributos de audio para que suene como una alarma
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Alertas Críticas de Helada",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para notificaciones críticas de riesgo de helada con sonido de alarma."
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                setSound(soundUri, audioAttributes)
                // Permite que la notificación interrumpa el modo No Molestar
                setBypassDnd(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(temperatura: Double) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("NotificationService", "No tiene permiso para mostrar notificaciones.")
                return
            }
        }

        val soundUri = Uri.parse("android.resource://" + context.packageName + "/" + R.raw.alarm)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("❄️ ALERTA DE HELADA CRÍTICA ❄️")
            .setContentText("¡RIESGO INMINENTE! La temperatura ha caído a ${temperatura}°C.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Asegúrate de que este ícono exista
            .setSound(soundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM) // Importante para alarmas
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
