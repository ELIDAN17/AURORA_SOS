package com.example.aurora_sos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    // Definimos el color que quieres usar
    val fondoClaro = Color(0xFF87CEEB)

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
        // Usamos tu color personalizado en lugar del color del tema
        color = fondoClaro
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "AURORA SOS",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 9.dp)
            )
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = Color.White
            )
        }
    }
}
