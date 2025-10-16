package com.example.aurora_sos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    // 1. Lógica del Temporizador y Navegación
    LaunchedEffect(key1 = true) {
        // Espera 4 segundos
        delay(4000L)
        // Navega al Módulo Principal y evita que se pueda volver a Splash
        navController.popBackStack()
        navController.navigate(Screen.Principal.route)
    }
    // 2. Diseño de la Interfaz de Usuario
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "AURORA SOS",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp)
            )
        }
    }
}