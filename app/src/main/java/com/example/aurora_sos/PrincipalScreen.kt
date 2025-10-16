package com.example.aurora_sos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.map // Necesario si no está en DataStoreManager

fun getAlertaData(temperatura: Double, umbralCritico: Double): Pair<Color, String> {
    // Definición de colores del prototipo (Rojo, Naranja, Verde)
    val colorRojo = Color.Red
    val colorNaranja = Color(0xFFFF9800)
    val colorVerde = Color(0xFF8BC34A)

    return when {
        // 1. RIESGO MÁXIMO (ROJO): Si la temperatura es menor o igual al umbral crítico
        temperatura <= umbralCritico -> Pair(colorRojo, "Alerta: Riesgo de Helada")

        // 2. RIESGO MODERADO (NARANJA): Si está por encima del umbral pero aún baja (ej. hasta 10°C)
        temperatura <= 10.0 -> Pair(colorNaranja, "Alerta: Riesgo moderado")

        // 3. ESTABLE (VERDE): Temperatura segura
        else -> Pair(colorVerde, "Alerta: Estable")
    }
}

// --- COMPOSABLE DEL DISEÑO ---

@Composable
fun PrincipalScreen(navController: NavController) {

    // --- LÓGICA DE DATASTORE (Lectura del Umbral Crítico) ---
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val notificationService = remember { NotificationService(context) }
    // Lee el umbral de helada configurado por el usuario (por defecto: 2.0°C)
    val configData by dataStoreManager.preferencesFlow.collectAsState(
        initial = Pair(2.0, true)
    )
    val umbralCritico = configData.first
    val notificacionesActivas = configData.second
    // --- SIMULACIÓN DE DATOS DEL ESP32 ---
    // En un proyecto real, este valor se actualizaría al recibir datos del servidor
    val temperaturaActual by remember { mutableStateOf(15.0) } // Ejemplo: 19°C (Estable)

    // Obtiene el color y el mensaje de alerta usando el umbral del usuario
    val (fondoColor, mensajeAlerta) = getAlertaData(temperaturaActual, umbralCritico)

    LaunchedEffect(temperaturaActual, notificacionesActivas) {
        if (temperaturaActual <= umbralCritico && notificacionesActivas) {
            notificationService.showNotification(temperaturaActual)
        }
    }
    // Estructura principal de la pantalla, usando Scaffold
    Scaffold(
        bottomBar = {
            // Fila para los botones inferiores
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Botón Historial, usando ElevatedButton
                ElevatedButton(onClick = { navController.navigate(Screen.Historial.route) }) {
                    Text("Historial de temperatura")
                }
                // Botón Configuración, usando ElevatedButton
                ElevatedButton(onClick = { navController.navigate(Screen.Configuracion.route) }) {
                    Text("Configuración")
                }
            }
        }
    ) { paddingValues ->
        // Contenido Principal del Tablero de Control de Colores
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(fondoColor), // Fondo dinámico según la alerta
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Temperatura actual (Componente Text)
            Text(
                text = "${temperaturaActual.toInt()}° C",
                fontSize = 120.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Mensaje de alerta (Componente Text)
            Text(
                text = mensajeAlerta,
                fontSize = 24.sp,
                color = Color.Black
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PrincipalScreenPreview() {
    PrincipalScreen(navController = rememberNavController())
}