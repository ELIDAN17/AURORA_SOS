package com.example.aurora_sos

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    navController: NavController,
    viewModel: HistorialViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    val fondoClaro = Color(0xFF87CEEB)
    val colorTarjeta = Color.White
    val colorLetraAzul = Color(0xFF0D47A1)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial y Pronóstico") },
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

            // --- TARJETA PRINCIPAL DEL GRÁFICO ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = colorTarjeta),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Leyenda del gráfico (se adapta al rango seleccionado)
                    GraficoLeyenda(rangoSeleccionado = uiState.rangoSeleccionado.texto)

                    // Contenedor del gráfico
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            uiState.isLoading -> {
                                CircularProgressIndicator()
                            }
                            uiState.error != null -> {
                                Text(
                                    text = uiState.error ?: "Error desconocido",
                                    color = Color.Red,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            else -> {
                                // Dibuja el gráfico según el tipo de datos
                                when (val datos = uiState.datosGrafico) {
                                    is DatosGrafico.Historial -> {
                                        if (datos.datos.isNotEmpty()) {
                                            GraficoLineasManual(
                                                modifier = Modifier.fillMaxSize(),
                                                datosHistorial = datos.datos,
                                                etiquetasXHistorial = datos.etiquetas,
                                                rangoSeleccionado = uiState.rangoSeleccionado.texto
                                            )
                                        } else {
                                            Text("No hay datos de historial para mostrar.")
                                        }
                                    }
                                    is DatosGrafico.Pronostico -> {
                                        if (datos.datos.isNotEmpty()) {
                                            GraficoLineasManual(
                                                modifier = Modifier.fillMaxSize(),
                                                datosPronostico = datos.datos,
                                                rangoSeleccionado = uiState.rangoSeleccionado.texto
                                            )
                                        } else {
                                            Text("No hay datos de pronóstico para mostrar.")
                                        }
                                    }
                                    is DatosGrafico.Vacio -> {
                                        Text("Seleccione un rango para ver los datos.")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- BOTONES DE SELECCIÓN DE RANGO ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RangoTiempo.values().forEach { rango ->
                    OutlinedButton(
                        onClick = { viewModel.seleccionarRango(rango) },
                        colors = if (rango == uiState.rangoSeleccionado) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Blue.copy(alpha = 0.1f),
                                contentColor = Color.Blue
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors(containerColor = Color.White)
                        }
                    ) {
                        Text(rango.texto)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- BOTONES DE NAVEGACIÓN ---
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

// --- COMPOSABLES AUXILIARES (GRÁFICO Y LEYENDA) ---

@Composable
fun GraficoLeyenda(rangoSeleccionado: String) {
    val esHistorial = rangoSeleccionado == RangoTiempo.TREINTA_DIAS.texto

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(10.dp).background(Color(0xFFFF9800), RoundedCornerShape(2.dp)))
        Text(" TMax/Temp", fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, end = 8.dp))

        if (esHistorial) {
            Box(modifier = Modifier.size(10.dp).background(Color.Blue, RoundedCornerShape(2.dp)))
            Text(" TMin", fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, end = 8.dp))

            Box(modifier = Modifier.size(10.dp).background(Color(0xFF90CAF9), RoundedCornerShape(2.dp)))
            Text(" Precip.", fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
        } else {
            Box(modifier = Modifier.size(10.dp).background(Color(0xFF90CAF9), RoundedCornerShape(2.dp)))
            Text(" Humedad", fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
fun GraficoLineasManual(
    modifier: Modifier = Modifier,
    datosHistorial: List<HistorialEntry> = emptyList(),
    datosPronostico: List<PronosticoEntry> = emptyList(),
    etiquetasXHistorial: List<String> = emptyList(),
    rangoSeleccionado: String
) {
    val esPronostico = rangoSeleccionado != RangoTiempo.TREINTA_DIAS.texto

    if (esPronostico && datosPronostico.isEmpty()) return
    if (!esPronostico && datosHistorial.isEmpty()) return

    val minTemp: Double
    val maxTemp: Double

    if (esPronostico) {
        minTemp = datosPronostico.minOf { it.temp }
        maxTemp = datosPronostico.maxOf { it.temp }
    } else {
        minTemp = datosHistorial.minOf { minOf(it.tmin, it.tmax) }
        maxTemp = datosHistorial.maxOf { maxOf(it.tmin, it.tmax) }
    }

    val paddedMinTemp = (minTemp - 2).roundToInt().toDouble()
    val paddedMaxTemp = (maxTemp + 2).roundToInt().toDouble()
    val rangoTemp = (paddedMaxTemp - paddedMinTemp).coerceAtLeast(1.0)

    val datosSize = if (esPronostico) datosPronostico.size else datosHistorial.size
    if (datosSize < 2) return

    val colorLinea1 = Color(0xFFFF9800)
    val colorLinea2 = Color.Blue
    val colorBarra = Color(0xFF90CAF9)

    val textMeasurer = rememberTextMeasurer()
    val paddingEjeY_Izquierda = 40.dp
    val paddingEjeY_Derecha = 40.dp
    val paddingEjeX_Abajo = 40.dp

    Canvas(modifier = modifier) {
        val anchoCanvas = size.width - paddingEjeY_Izquierda.toPx() - paddingEjeY_Derecha.toPx()
        val altoCanvas = size.height - paddingEjeX_Abajo.toPx()
        val anchoPaso = anchoCanvas / (datosSize - 1)

        // Dibuja el contenido del gráfico desplazado a la derecha para el padding izquierdo
        translate(left = paddingEjeY_Izquierda.toPx()) {
            // --- EJE Y (IZQUIERDA) - TEMPERATURA ---
            val estiloEtiquetaY = TextStyle(fontSize = 10.sp, color = Color.Gray, textAlign = TextAlign.End)
            val numeroDeEtiquetasY = 5
            val pasoEtiquetaTemp = rangoTemp / numeroDeEtiquetasY

            (0..numeroDeEtiquetasY).forEach { i ->
                val temp = paddedMinTemp + (i * pasoEtiquetaTemp)
                val y = altoCanvas - ((i.toFloat() / numeroDeEtiquetasY) * altoCanvas)

                drawText(
                    textMeasurer,
                    "${temp.roundToInt()}°",
                    topLeft = Offset(-paddingEjeY_Izquierda.toPx() + 8.dp.toPx(), y - 8.sp.toPx()),
                    style = estiloEtiquetaY
                )
                drawLine(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    start = Offset(0f, y),
                    end = Offset(anchoCanvas, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }

            // --- EJE X (ABAJO) --- 
            val estiloEtiquetaX = TextStyle(fontSize = 10.sp, color = Color.Black, textAlign = TextAlign.Center)
            if (esPronostico) {
                val formatterHora = DateTimeFormatter.ofPattern("HH:mm")
                val formatterFecha = DateTimeFormatter.ofPattern("dd/MM")
                val saltoEtiqueta = if (rangoSeleccionado == RangoTiempo.CINCO_DIAS.texto) 8 else 2

                datosPronostico.forEachIndexed { index, entry ->
                    if (index % saltoEtiqueta == 0) {
                        val x = index * anchoPaso
                        val textoCompleto = "${entry.timestamp.format(formatterHora)}\n${entry.timestamp.toLocalDate().format(formatterFecha)}"
                        drawText(
                            textMeasurer,
                            textoCompleto,
                            topLeft = Offset(x - (textMeasurer.measure(textoCompleto, estiloEtiquetaX).size.width / 2), altoCanvas + 4.dp.toPx()),
                            style = estiloEtiquetaX
                        )
                    }
                }
            } else { // Historial
                val saltoEtiqueta = if (etiquetasXHistorial.size > 10) 5 else 1
                etiquetasXHistorial.forEachIndexed { index, etiqueta ->
                    if (index % saltoEtiqueta == 0) {
                        val x = index * anchoPaso
                        drawText(
                            textMeasurer,
                            etiqueta,
                            topLeft = Offset(x - (textMeasurer.measure(etiqueta, estiloEtiquetaX).size.width / 2), altoCanvas + 4.dp.toPx()),
                            style = estiloEtiquetaX
                        )
                    }
                }
            }

            // --- DIBUJAR LÍNEAS Y BARRAS ---
            val pathLinea1 = Path()
            val pathLinea2 = Path()
            val anchoBarra = (anchoPaso * 0.6f).coerceAtMost(20f)

            if (esPronostico) {
                datosPronostico.forEachIndexed { index, entry ->
                    val x = index * anchoPaso
                    val yTemp = (altoCanvas - (((entry.temp - paddedMinTemp) / rangoTemp) * altoCanvas)).toFloat()
                    val altoBarraHum = ((entry.humedad / 100.0) * altoCanvas).toFloat()

                    drawRect(
                        color = colorBarra.copy(alpha = 0.5f),
                        topLeft = Offset(x - anchoBarra / 2, altoCanvas - altoBarraHum),
                        size = Size(anchoBarra, altoBarraHum)
                    )
                    if (index == 0) pathLinea1.moveTo(x, yTemp) else pathLinea1.lineTo(x, yTemp)
                }
            } else { // Historial
                val maxPrecip = datosHistorial.maxOfOrNull { it.precip }?.coerceAtLeast(1.0) ?: 1.0
                datosHistorial.forEachIndexed { index, entry ->
                    val x = index * anchoPaso
                    val yTMax = (altoCanvas - (((entry.tmax - paddedMinTemp) / rangoTemp) * altoCanvas)).toFloat()
                    val yTMin = (altoCanvas - (((entry.tmin - paddedMinTemp) / rangoTemp) * altoCanvas)).toFloat()
                    
                    if (entry.precip > 0) {
                        val altoBarraPrecip = ((entry.precip / maxPrecip) * (altoCanvas / 2)).toFloat()
                        drawRect(
                            color = colorBarra.copy(alpha = 0.5f),
                            topLeft = Offset(x - anchoBarra / 2, altoCanvas - altoBarraPrecip),
                            size = Size(anchoBarra, altoBarraPrecip)
                        )
                    }

                    if (index == 0) {
                        pathLinea1.moveTo(x, yTMax)
                        pathLinea2.moveTo(x, yTMin)
                    } else {
                        pathLinea1.lineTo(x, yTMax)
                        pathLinea2.lineTo(x, yTMin)
                    }
                }
                drawPath(path = pathLinea2, color = colorLinea2, style = Stroke(width = 5f))
            }
            drawPath(path = pathLinea1, color = colorLinea1, style = Stroke(width = 5f))
        }
    }
}
