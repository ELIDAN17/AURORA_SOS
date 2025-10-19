package com.example.aurora_sos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(navController: NavController) {
    // 1. Estado para seleccionar el rango de tiempo
    val rangos = listOf("24 H", "7 Días", "30 Días")
    var rangoSeleccionado by rememberSaveable { mutableStateOf(rangos.first()) }

    // Colores de la interfaz
    val fondoClaro = Color(0xFF87CEEB)
    val colorTarjeta = Color.White
    val colorLetraAzul = Color(0xFF0D47A1)

    // 2. Estructura principal con Scaffold
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Temperatura") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = fondoClaro)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(fondoClaro)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(24.dp))

            // --- CAMBIO: Contenedor del Gráfico (Card) con más peso ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    // Le damos un peso mayor para que ocupe más espacio vertical
                    .weight(2.5f),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = colorTarjeta),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "GRÁFICO VECTORIAL DE TEMPERATURA\n($rangoSeleccionado)",
                        color = Color.Gray,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // --- CAMBIO: Spacer con menos peso para los botones ---
            // Esto le da menos espacio al área de botones, haciendo que el gráfico se expanda
            Spacer(modifier = Modifier.weight(0.1f))

            // --- Botones de Rango de Tiempo ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rangos.forEach { rango ->
                    OutlinedButton(
                        onClick = { rangoSeleccionado = rango },
                        colors = if (rango == rangoSeleccionado) {
                            ButtonDefaults.outlinedButtonColors(containerColor = Color.Blue.copy(alpha = 0.1f), contentColor = Color.Blue)
                        } else {
                            ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                        }
                    ) {
                        Text(rango)
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))

            // --- Botones de navegación ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val buttonColors = ButtonDefaults.elevatedButtonColors(
                    containerColor = Color.White,
                    contentColor = colorLetraAzul
                )

                // Botón "Principal"
                ElevatedButton(
                    onClick = { navController.popBackStack() },
                    colors = buttonColors
                ) {
                    Text("Principal")
                }

                // Botón Configuración
                ElevatedButton(
                    onClick = { navController.navigate(Screen.Configuracion.route) },
                    colors = buttonColors
                ) {
                    Text("Configuración")
                }
            }
        }
    }
}