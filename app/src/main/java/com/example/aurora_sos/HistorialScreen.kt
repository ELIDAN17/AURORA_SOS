package com.example.aurora_sos

import android.util.Log
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

// --- IMPORTS PARA FIREBASE ---
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

// --- IMPORTS PARA CANVAS, TEXTO Y FECHAS ---
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.TextStyle
import java.time.LocalDate // Para filtrar fechas
import java.time.format.DateTimeFormatter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight

// --- ¡NUEVO! IMPORTS PARA LA API (KTOR) ---
// (Estos imports ahora encontrarán las clases en PrincipalScreen.kt)
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

// --- Clase de datos para el Historial (JSON) ---
data class HistorialEntry(
    val tmin: Float = 0f,
    val tmax: Float = 0f,
    val precip: Float = 0f
)

// --- ¡NUEVO! Clase de datos para el Pronóstico de 24H (API) ---
data class PronosticoEntry(
    val hora: String,
    val temp: Float,
    val humedad: Float
)

// --- Composable para la Leyenda ---
@Composable
fun GraficoLeyenda(esPronostico: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (esPronostico) {
            Box(modifier = Modifier.size(10.dp).background(Color(0xFFFF9800), CircleShape))
            Text(" Temp (°C)", fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))

            Box(modifier = Modifier.size(10.dp).background(Color(0xFF90CAF9), CircleShape))
            Text(" Humedad (%)", fontSize = 12.sp)
        } else {
            Box(modifier = Modifier.size(10.dp).background(Color(0xFFFF9800), CircleShape))
            Text(" TMax", fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))

            Box(modifier = Modifier.size(10.dp).background(Color.Blue, CircleShape))
            Text(" TMin", fontSize = 12.sp, modifier = Modifier.padding(end = 8.dp))

            Box(modifier = Modifier.size(10.dp).background(Color(0xFF90CAF9), CircleShape))
            Text(" Precip (mm)", fontSize = 12.sp)
        }
    }
}

// --- Composable del Gráfico (¡CÓDIGO CORREGIDO Y MEJORADO!) ---
@Composable
fun GraficoLineasManual(
    modifier: Modifier = Modifier,
    datosHistorial: List<HistorialEntry>,
    datosPronostico: List<PronosticoEntry>,
    esPronostico: Boolean
) {
    // 1. Encontrar valores
    val minTemp: Float
    val maxTemp: Float

    if (esPronostico) {
        if (datosPronostico.isEmpty()) {
            Box(modifier = modifier, contentAlignment = Alignment.Center) { Text("Cargando pronóstico...", color = Color.Gray) }
            return
        }
        minTemp = datosPronostico.minOf { it.temp } - 2f
        maxTemp = datosPronostico.maxOf { it.temp } + 2f
    } else {
        if (datosHistorial.isEmpty()) {
            Box(modifier = modifier, contentAlignment = Alignment.Center) { Text("No hay datos para este rango", color = Color.Gray) }
            return
        }
        minTemp = datosHistorial.minOf { minOf(it.tmin, it.tmax) } - 1f
        maxTemp = datosHistorial.maxOf { maxOf(it.tmin, it.tmax) } + 1f
    }

    val rangoTemp = maxTemp - minTemp
    val (datosSize, maxEjeY) = if (esPronostico) Pair(datosPronostico.size, 100f) else Pair(datosHistorial.size, maxTemp)


    // 2. Definir los colores
    val colorLinea1 = Color(0xFFFF9800) // TMax o Temp. Pronóstico
    val colorLinea2 = Color.Blue          // TMin
    val colorBarra = Color(0xFF90CAF9)   // Precipitación o Humedad
    val colorEjes = Color.Gray

    // 3. Preparar el medidor de texto para las etiquetas
    val textMeasurer = rememberTextMeasurer()
    val paddingEjeY = 40.dp // Espacio a la izquierda
    val paddingEjeX = 20.dp // Espacio abajo

    // 4. Dibujar en el Canvas
    Canvas(modifier = modifier
        .fillMaxSize()
        .padding(start = paddingEjeY, bottom = paddingEjeX, end = 16.dp, top = 16.dp)
    ) {
        val anchoCanvas = size.width
        val altoCanvas = size.height

        // --- ¡ARREGLO DEL BUG! ---
        // Usamos "datosSize" para calcular el paso.
        val anchoPaso = if (datosSize > 0) anchoCanvas / datosSize else anchoCanvas
        val anchoBarra = (anchoPaso * 0.6f).coerceAtMost(20f)

        // --- Dibujar Ejes y Etiquetas ---
        drawLine(color = colorEjes, start = Offset(0f, 0f), end = Offset(0f, altoCanvas), strokeWidth = 2f)
        drawLine(color = colorEjes, start = Offset(0f, altoCanvas), end = Offset(anchoCanvas, altoCanvas), strokeWidth = 2f)

        val estiloEtiqueta = TextStyle(fontSize = 12.sp, color = Color.Black)
        val yMax = 0f
        drawText(textMeasurer, "${maxTemp.roundToInt()}°", topLeft = Offset(-paddingEjeY.toPx(), yMax - 8.sp.toPx()), style = estiloEtiqueta)

        val yMin = altoCanvas
        drawText(textMeasurer, "${minTemp.roundToInt()}°", topLeft = Offset(-paddingEjeY.toPx(), yMin - 8.sp.toPx()), style = estiloEtiqueta)

        // Línea de guía media (solo si no es pronóstico)
        if (!esPronostico) {
            val midTemp = (minTemp + maxTemp) / 2
            val yMid = altoCanvas / 2
            drawText(textMeasurer, "${midTemp.roundToInt()}°", topLeft = Offset(-paddingEjeY.toPx(), yMid - 8.sp.toPx()), style = estiloEtiqueta)
            drawLine(color = Color.LightGray, start = Offset(0f, yMid), end = Offset(anchoCanvas, yMid), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f)))
        }

        // --- Dibujar las líneas y Barras ---
        val pathLinea1 = Path()
        val pathLinea2 = Path()

        if (esPronostico) {
            // --- Lógica para PRONÓSTICO (Temp y Humedad) ---
            datosPronostico.forEachIndexed { index, entry ->
                // ¡X CORREGIDA! Centramos el punto en su "carril"
                val x = (index * anchoPaso) + (anchoPaso / 2)

                // Barras de Humedad (0% a 100%)
                val altoBarra = (entry.humedad / 100f) * altoCanvas
                drawRect(
                    color = colorBarra,
                    topLeft = Offset(x - anchoBarra / 2, altoCanvas - altoBarra),
                    size = Size(anchoBarra, altoBarra)
                )

                // Línea de Temperatura
                val yTemp = altoCanvas - ((entry.temp - minTemp) / rangoTemp) * altoCanvas
                if (index == 0) pathLinea1.moveTo(x, yTemp) else pathLinea1.lineTo(x, yTemp)
            }
        } else {
            // --- Lógica para HISTORIAL (TMax, TMin, Precip) ---
            datosHistorial.forEachIndexed { index, entry ->
                // ¡X CORREGIDA! Centramos el punto en su "carril"
                val x = (index * anchoPaso) + (anchoPaso / 2)

                // Barras de Precipitación (escaladas)
                if (entry.precip > 0) {
                    val altoBarra = (entry.precip / maxEjeY).coerceAtMost(1f) * altoCanvas
                    drawRect(
                        color = colorBarra,
                        topLeft = Offset(x - anchoBarra / 2, altoCanvas - altoBarra),
                        size = Size(anchoBarra, altoBarra)
                    )
                }

                // Líneas de Temperatura
                val yTMax = altoCanvas - ((entry.tmax - minTemp) / rangoTemp) * altoCanvas
                val yTMin = altoCanvas - ((entry.tmin - minTemp) / rangoTemp) * altoCanvas

                if (index == 0) {
                    pathLinea1.moveTo(x, yTMax)
                    pathLinea2.moveTo(x, yTMin)
                } else {
                    pathLinea1.lineTo(x, yTMax)
                    pathLinea2.lineTo(x, yTMin)
                }
            }
            // Dibujamos la segunda línea (TMin)
            drawPath(path = pathLinea2, color = colorLinea2, style = Stroke(width = 4f))
        }

        // Dibujamos la primera línea (TMax o Temp. Pronóstico)
        drawPath(path = pathLinea1, color = colorLinea1, style = Stroke(width = 4f))
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(navController: NavController) {
    // 1. Estado para seleccionar el rango de tiempo
    val rangos = listOf("24 H", "7 Días", "30 Días")
    var rangoSeleccionado by rememberSaveable { mutableStateOf("7 Días") }

    // --- Estados para datos y carga ---
    var isLoading by remember { mutableStateOf(false) } // 'false' por defecto

    // Almacén para el JSON (datos históricos)
    val datosCompletos = remember { mutableStateMapOf<LocalDate, HistorialEntry>() }

    // ¡NUEVO! Almacén para la API (datos de pronóstico)
    var datosParaMostrarPronostico by remember { mutableStateOf<List<PronosticoEntry>>(emptyList()) }

    // Lista final que se pasa al gráfico
    var datosParaMostrarHistorial by remember { mutableStateOf<List<HistorialEntry>>(emptyList()) }


    // --- LÓGICA PARA OBTENER DATOS DE FIREBASE (SOLO 1 VEZ) ---
    LaunchedEffect(Unit) {
        isLoading = true
        val database = Firebase.database("https://aurorasos-default-rtdb.firebaseio.com/")
        val historialRef = database.getReference("historial")
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

        historialRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    val dataMapaMutable = mutableMapOf<LocalDate, HistorialEntry>()
                    for (fechaSnapshot in snapshot.children) {
                        val valueMap = fechaSnapshot.value as? Map<String, Any>
                        val fechaKeyStr = fechaSnapshot.key ?: continue

                        if (valueMap != null) {
                            try {
                                val fechaKey = LocalDate.parse(fechaKeyStr, formatter)
                                val tmin = (valueMap["tmin"] as? Number)?.toFloat() ?: 0f
                                val tmax = (valueMap["tmax"] as? Number)?.toFloat() ?: 0f
                                val precip = (valueMap["precip"] as? Number)?.toFloat() ?: 0f
                                val entry = HistorialEntry(tmin, tmax, precip)
                                dataMapaMutable[fechaKey] = entry
                            } catch (e: Exception) {}
                        }
                    }
                    datosCompletos.putAll(dataMapaMutable)

                } catch (e: Exception) {
                    Log.e("HistorialScreen", "Error al parsear datos: ${e.message}")
                }
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HistorialScreen", "Error al leer historial: ${error.message}")
                isLoading = false
            }
        })
    }

    // --- ¡LÓGICA MEJORADA! Este Effect filtra los datos O LLAMA A LA API ---
    LaunchedEffect(rangoSeleccionado, datosCompletos.size) {

        if (rangoSeleccionado == "24 H") {
            // --- LÓGICA PARA EL BOTÓN 24 H (LLAMADA A LA API) ---
            isLoading = true
            datosParaMostrarHistorial = emptyList() // Limpiamos el gráfico de historial

            val client = HttpClient(CIO) {
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            }
            val apiKey = "5435a01f60d70475e9294d39e22d30d0"
            val latPuno = "-15.84"
            val lonPuno = "-70.02"
            val urlPronostico = "https://api.openweathermap.org/data/2.5/forecast?lat=${latPuno}&lon=${lonPuno}&appid=${apiKey}&units=metric"

            try {
                // Usamos las clases de PrincipalScreen.kt
                val response: ForecastResponse = client.get(urlPronostico).body()
                val pronosticoProcesado = response.list.take(8).map { item ->
                    // Convertimos la hora (dt) a un formato legible
                    val timestamp = LocalDateTime.ofInstant(Instant.ofEpochSecond(item.dt), ZoneId.systemDefault())
                    val horaStr = timestamp.format(DateTimeFormatter.ofPattern("HH:mm"))
                    PronosticoEntry(
                        hora = horaStr,
                        temp = item.main.temp.toFloat(),
                        humedad = item.main.humidity.toFloat()
                    )
                }
                datosParaMostrarPronostico = pronosticoProcesado
            } catch (e: Exception) {
                Log.e("HistorialScreen", "Error al llamar a OWM Forecast: ${e.message}")
                datosParaMostrarPronostico = emptyList()
            }
            client.close()
            isLoading = false

        } else {
            // --- LÓGICA PARA 7 Y 30 DÍAS (JSON) ---
            if (datosCompletos.isNotEmpty()) {
                isLoading = true
                datosParaMostrarPronostico = emptyList() // Limpiamos el gráfico de pronóstico

                val diasAFiltrar = if (rangoSeleccionado == "7 Días") 7L else 30L
                val hoy = LocalDate.now()
                val fechaLimite = hoy.minusDays(diasAFiltrar)

                val datosFiltrados = datosCompletos
                    .filterKeys { it.isAfter(fechaLimite) && it.isBefore(hoy.plusDays(1)) }
                    .toSortedMap()
                    .values
                    .toList()

                datosParaMostrarHistorial = datosFiltrados
                isLoading = false
            }
        }
    }

    // Colores de la interfaz
    val fondoClaro = Color(0xFF87CEEB)
    val colorTarjeta = Color.White
    val colorLetraAzul = Color(0xFF0D47A1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial y Pronóstico") }, // Título actualizado
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

            Spacer(modifier = Modifier.height(16.dp))

            // --- TARJETA PRINCIPAL (Ahora es dinámica) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(2.5f),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = colorTarjeta),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // La leyenda ahora cambia según el rango seleccionado
                    GraficoLeyenda(esPronostico = rangoSeleccionado == "24 H")

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator()
                        } else {
                            // ¡Dibuja el gráfico con los datos correctos!
                            GraficoLineasManual(
                                modifier = Modifier.fillMaxSize(),
                                datosHistorial = datosParaMostrarHistorial,
                                datosPronostico = datosParaMostrarPronostico,
                                esPronostico = rangoSeleccionado == "24 H"
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.1f))

            // --- Botones de Rango de Tiempo ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rangos.forEach { rango ->
                    OutlinedButton(
                        onClick = {
                            // Solo cambiamos si no está ya seleccionado
                            if (rango != rangoSeleccionado) {
                                rangoSeleccionado = rango
                            }
                        },
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

            // --- Botones de navegación (igual que antes) ---
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

                ElevatedButton(
                    onClick = { navController.popBackStack() },
                    colors = buttonColors
                ) {
                    Text("Principal")
                }
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