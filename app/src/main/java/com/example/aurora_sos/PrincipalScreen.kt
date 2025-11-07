package com.example.aurora_sos

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
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
import androidx.compose.runtime.DisposableEffect

// --- IMPORTS PARA ANIMACIÓN ---
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

// --- IMPORTS PARA EL "TRABAJADOR" DE INTERNET (KTOR) ---
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.call.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName
import java.util.Calendar

// --- Tu función 'getAlertaData' no cambia ---
fun getAlertaData(temperatura: Double, umbralCritico: Double): Pair<Color, String> {
    val colorRojo = Color.Red
    val colorNaranja = Color(0xFFFF9800)
    val colorVerde = Color(0xFF8BC34A)

    return when {
        temperatura <= umbralCritico -> Pair(colorRojo, "Alerta: Riesgo de Helada")
        temperatura <= 10.0 -> Pair(colorNaranja, "Alerta: Riesgo moderado")
        else -> Pair(colorVerde, "Alerta: Estable")
    }
}


@Composable
fun PrincipalScreen(navController: NavController) {

    // --- Lógica de DataStore (no cambia) ---
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val notificationService = remember { NotificationService(context) }

    val configData by dataStoreManager.preferencesFlow.collectAsState(
        initial = Pair(2.0, true)
    )
    val umbralCritico = configData.first
    val notificacionesActivas = configData.second

    // --- NUEVOS ESTADOS para la predicción ---
    var temperaturaActual by remember { mutableStateOf(30.0) } // Tu valor por defecto
    var prediccionMinima by remember { mutableStateOf<Double?>(null) } // ¡Nuevo!
    var errorMessage by remember { mutableStateOf<String?>(null) } // Para errores

    val coroutineScope = rememberCoroutineScope()
    val database = Firebase.database("https://aurorasos-default-rtdb.firebaseio.com/")
    val tempRef = database.getReference("aurora/temperatura")

    // --- TRABAJADOR 1: PIDE EL CLIMA Y LA PREDICCIÓN (CON 2 LLAMADAS) ---
    LaunchedEffect(Unit) {

        val client = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true // ¡Esto sigue siendo vital!
                })
            }
        }

        val apiKey = "5435a01f60d70475e9294d39e22d30d0" // Tu llave "Default"
        val latPuno = "-15.84"
        val lonPuno = "-70.02"

        // URLs para las dos APIs gratuitas
        val urlClimaActual = "https://api.openweathermap.org/data/2.5/weather?lat=${latPuno}&lon=${lonPuno}&appid=${apiKey}&units=metric"
        val urlPronostico = "https://api.openweathermap.org/data/2.5/forecast?lat=${latPuno}&lon=${lonPuno}&appid=${apiKey}&units=metric"

        try {
            errorMessage = null

            // --- LLAMADA 1: Obtener clima actual ---
            val responseActual: WeatherResponse = client.get(urlClimaActual).body()
            val tempDeOWM = responseActual.main.temp

            // Escribimos el dato actual en Firebase (como antes)
            coroutineScope.launch {
                tempRef.setValue(tempDeOWM)
            }

            // --- LLAMADA 2: Obtener pronóstico ---
            val responsePronostico: ForecastResponse = client.get(urlPronostico).body()

            // --- Lógica para encontrar la mínima de mañana ---
            val manana = Calendar.getInstance()
            manana.add(Calendar.DAY_OF_YEAR, 1) // Sumamos 1 día
            val diaDeManana = manana.get(Calendar.DAY_OF_YEAR)

            var tempMinimaManana = 100.0 // Empezamos con un número alto

            // Recorremos la lista del pronóstico
            for (item in responsePronostico.list) {
                val itemCalendar = Calendar.getInstance()
                itemCalendar.timeInMillis = item.dt * 1000L // 'dt' está en segundos
                val diaDelItem = itemCalendar.get(Calendar.DAY_OF_YEAR)

                // Si el item es de "mañana" y su temp_min es más baja...
                if (diaDelItem == diaDeManana && item.main.temp_min < tempMinimaManana) {
                    tempMinimaManana = item.main.temp_min
                }
            }
            prediccionMinima = tempMinimaManana


        } catch (e: Exception) {
            Log.e("PrincipalScreen", "Error al llamar a OWM: ${e.message}")
            errorMessage = e.message
        }

        client.close()
    }


    // --- TRABAJADOR 2: ESCUCHA FIREBASE (SIEMPRE) ---
    // (Este código no cambia)
    DisposableEffect(Unit) {
        val valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val temp = snapshot.getValue(Double::class.java)
                if (temp != null) {
                    temperaturaActual = temp
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al leer la temperatura", error.toException())
            }
        }
        tempRef.addValueEventListener(valueEventListener)
        onDispose {
            tempRef.removeEventListener(valueEventListener)
        }
    }

    // --- El resto de tu código de UI y Animaciones (no cambia) ---
    val (colorObjetivo, mensajeAlerta) = getAlertaData(temperaturaActual, umbralCritico)

    val fondoColorAnimado by animateColorAsState(
        targetValue = colorObjetivo, label = "ColorDeFondoAnimado",
        animationSpec = tween(durationMillis = 1000)
    )

    LaunchedEffect(temperaturaActual, notificacionesActivas) {
        if (temperaturaActual <= umbralCritico && notificacionesActivas) {
            notificationService.showNotification(temperaturaActual)
        }
    }

    Scaffold(
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(fondoColorAnimado),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {

                // Si hay un error, lo muestra
                if (!errorMessage.isNullOrEmpty()) {
                    Text(
                        text = "ERROR: $errorMessage",
                        color = Color.Red,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Animación de Temperatura Actual
                AnimatedContent(
                    targetState = temperaturaActual.toInt(),
                    label = "AnimacionTemperatura",
                    transitionSpec = {
                        slideInVertically(animationSpec = tween(500)) { it } togetherWith
                                slideOutVertically(animationSpec = tween(500)) { -it }
                    }
                ) { temperaturaInt ->
                    Text(
                        text = "$temperaturaInt° C",
                        fontSize = 120.sp,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Animación de Mensaje de Alerta
                AnimatedContent(
                    targetState = mensajeAlerta,
                    label = "AnimacionMensajeAlerta",
                    transitionSpec = {
                        slideInVertically(animationSpec = tween(500)) { it } togetherWith
                                slideOutVertically(animationSpec = tween(500)) { -it }
                    }
                ) { mensaje ->
                    Text(
                        text = mensaje,
                        fontSize = 24.sp,
                        color = Color.Black
                    )
                }

                // --- ¡TEXTO DE PREDICCIÓN! ---
                Spacer(modifier = Modifier.height(16.dp))
                // Solo se muestra si 'prediccionMinima' tiene un valor
                if (prediccionMinima != null) {
                    val prediccionColor = if (prediccionMinima!! <= umbralCritico) Color.Red else Color.Black

                    Text(
                        text = "Predicción Mañana: Mín. ${prediccionMinima!!.toInt()}° C",
                        fontSize = 20.sp,
                        color = prediccionColor,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                // --- FIN DEL TEXTO ---


                Spacer(modifier = Modifier.weight(1f))

                // Botones de Navegación
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val buttonColors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF0D47A1)
                    )
                    ElevatedButton(
                        onClick = { navController.navigate(Screen.Historial.route) },
                        colors = buttonColors
                    ) {
                        Text("Historial de temperatura")
                    }
                    ElevatedButton(
                        onClick = { navController.navigate(Screen.Configuracion.route) },
                        colors = buttonColors
                    ) {
                        Text("Configuración")
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = Color.Transparent) {}
        }
    )
}

// --- Preview (no cambia) ---
@Preview(showBackground = true)
@Composable
fun PrincipalScreenPreview() {
    PrincipalScreen(navController = rememberNavController())
}
@Serializable
data class WeatherResponse(
    val main: MainData
)

@Serializable
data class MainData(
    val temp: Double
)

// --- Clases para la API "forecast" (Pronóstico) ---
// ¡Usadas por PrincipalScreen Y HistorialScreen!
@Serializable
data class ForecastResponse(
    val list: List<ForecastItem>
)

@Serializable
data class ForecastItem(
    val main: ForecastMainData,
    val dt: Long
)

// ¡ESTA ES LA CLASE UNIFICADA!
// Tiene todos los campos que ambas pantallas necesitan
@Serializable
data class ForecastMainData(
    val temp: Double,
    val temp_min: Double,
    val humidity: Double
)