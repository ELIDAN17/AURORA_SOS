package com.example.aurora_sos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notificationService = NotificationService(this)
        notificationService.createNotificationChannel()
        enableEdgeToEdge()
        setContent {
            AppNavigation()
        }
    }
}
@Composable
fun AppNavigation() {
    // 1. Controlador de Navegación
    val navController = rememberNavController()
    // 2. NavHost: Contenedor que gestiona las pantallas según la ruta
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        // 1. Pantalla de Presentación (Splash Screen)
        composable(Screen.Splash.route) {
            SplashScreen(navController)
        }
        // 2. Módulo Principal (Tablero de Control)
        composable(Screen.Principal.route) {
            PrincipalScreen(navController)
        }
        // 3. Módulo de Configuración
        composable(Screen.Configuracion.route) {
            ConfiguracionScreen(navController)
        }
        // 4. Módulo de Historial (Pendiente de implementación)
        composable(Screen.Historial.route) {
            HistorialScreen(navController) // Se implementará a continuación
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ToDoListExamplePreview() {
    AppNavigation()
}