package com.example.aurora_sos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.aurora_sos.ui.theme.AURORA_SOSTheme
import com.example.aurora_sos.Screen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val notificationService = NotificationService(this)
        notificationService.createNotificationChannel()
        enableEdgeToEdge()
        setContent {
            AURORA_SOSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
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
        composable(Screen.SensorHistorial.route) {
            SensorHistorialScreen(navController)
        }
    }
}
