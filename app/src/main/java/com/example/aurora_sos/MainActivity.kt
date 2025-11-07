package com.example.aurora_sos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize // Import
import androidx.compose.material3.MaterialTheme // Import
import androidx.compose.material3.Surface // Import
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier // Import
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// --- ¡AHORA ESTE IMPORT ES CORRECTO! ---
import com.example.aurora_sos.ui.theme.AURORA_SOSTheme // <-- Usa tu nombre real

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notificationService = NotificationService(this)
        notificationService.createNotificationChannel()
        enableEdgeToEdge()
        setContent {

            // --- ¡Y ESTA FUNCIÓN AHORA ES CORRECTA! ---
            AURORA_SOSTheme { // <-- Usa tu nombre real
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation() // Tu navegación va aquí adentro
                }
            }
        }
    }
}
@Composable
fun AppNavigation() {
    // (Tu código de AppNavigation estaba perfecto)
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }
        composable(Screen.Principal.route) {
            PrincipalScreen(navController)
        }
        composable(Screen.Configuracion.route) {
            ConfiguracionScreen(navController)
        }
        composable(Screen.Historial.route) {
            HistorialScreen(navController)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ToDoListExamplePreview() {
    // (Arreglamos el Preview también)
    AURORA_SOSTheme { // <-- Usa tu nombre real
        AppNavigation()
    }
}