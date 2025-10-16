// Archivo: HistorialScreen.kt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(navController: NavController) {
    // 1. Estado para seleccionar el rango de tiempo
    val rangos = listOf("24 H", "7 Días", "30 Días")
    var rangoSeleccionado by rememberSaveable { mutableStateOf(rangos.first()) }

    // Colores de la interfaz
    val fondoClaro = Color(0xFF87CEEB) // Fondo azul claro (similar al prototipo)
    val colorTarjeta = Color.White

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

            // --- Contenedor del Gráfico (Card) ---
            // Card: Componente para agrupar y destacar el contenido (el gráfico).
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f), // Ocupa el espacio restante
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = colorTarjeta),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                // Simulación del Gráfico Vectorial
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Text: Usado como un placeholder visual para las "líneas vectoriales"
                    // En un proyecto real, esto se reemplazaría con una librería de gráficos (ej. Compose Chart)
                    Text(
                        text = "GRÁFICO VECTORIAL DE TEMPERATURA\n($rangoSeleccionado)",
                        color = Color.Gray,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Botones de Rango de Tiempo (Row) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // OutlinedButton: Botón de énfasis intermedio para las acciones de filtrado
                rangos.forEach { rango ->
                    OutlinedButton(
                        onClick = { rangoSeleccionado = rango },
                        // Estilo dinámico para destacar el botón seleccionado
                        colors = if (rango == rangoSeleccionado) {
                            ButtonDefaults.outlinedButtonColors(containerColor = Color.Blue.copy(alpha = 0.1f), contentColor = Color.Blue)
                        } else {
                            ButtonDefaults.outlinedButtonColors()
                        }
                    ) {
                        Text(rango)
                    }
                }
            }
        }
    }
}