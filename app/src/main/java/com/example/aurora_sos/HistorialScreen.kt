package com.example.aurora_sos

import android.app.Application
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
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
    viewModel: HistorialViewModel = viewModel(factory = ViewModelFactory(LocalContext.current.applicationContext as Application))
) {
    val uiState by viewModel.uiState.collectAsState()

    val colorLineaTemp = Color(0xFFFF9800) // Naranja para temperatura
    val colorBarraHumedad = Color(0xFF90CAF9) // Azul para humedad
    val etiquetaTextColor = MaterialTheme.colorScheme.onSurface
    val lineaGuiaColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pronóstico del Tiempo", color = MaterialTheme.colorScheme.onBackground) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val tituloGrafico = when (uiState.rangoSeleccionado) {
                        RangoTiempo.ULTIMAS_24_HORAS -> "Pronóstico a 24 Horas"
                        RangoTiempo.SIETE_DIAS -> "Pronóstico a 7 Días"
                    }
                    Text(
                        text = tituloGrafico,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    GraficoLeyendaPronostico(
                        tempColor = colorLineaTemp,
                        humidityColor = colorBarraHumedad,
                        textColor = etiquetaTextColor
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = 8.dp, end = 8.dp, top = 16.dp, bottom = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            uiState.isLoading -> {
                                CircularProgressIndicator()
                            }
                            uiState.error != null -> {
                                Text(
                                    text = uiState.error ?: "Error desconocido",
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                            else -> {
                                when (val datos = uiState.datosGrafico) {
                                    is DatosGrafico.Pronostico -> {
                                        if (datos.datos.isNotEmpty()) {
                                            GraficoLineasPronostico(
                                                modifier = Modifier.fillMaxSize(),
                                                datosPronostico = datos.datos,
                                                rangoSeleccionado = uiState.rangoSeleccionado.texto,
                                                lineaTempColor = colorLineaTemp,
                                                barraHumedadColor = colorBarraHumedad,
                                                etiquetaTextColor = etiquetaTextColor,
                                                lineaGuiaColor = lineaGuiaColor
                                            )
                                        } else {
                                            Text("No hay datos de pronóstico para mostrar.")
                                        }
                                    }
                                    else -> {
                                        Text("Seleccione un rango para ver los datos.")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                RangoTiempo.entries.forEach { rango ->
                    OutlinedButton(
                        onClick = { viewModel.seleccionarRango(rango) },
                        colors = if (rango == uiState.rangoSeleccionado) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface)
                        }
                    ) {
                        Text(rango.texto)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val buttonColors = ButtonDefaults.elevatedButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
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

@Composable
fun GraficoLeyendaPronostico(
    tempColor: Color,
    humidityColor: Color,
    textColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(10.dp).background(tempColor, RoundedCornerShape(2.dp)))
        Text(" Temp", fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, end = 8.dp), color = textColor)

        Box(modifier = Modifier.size(10.dp).background(humidityColor, RoundedCornerShape(2.dp)))
        Text(" Humedad (%)", fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp), color = textColor)
    }
}

@Composable
fun GraficoLineasPronostico(
    modifier: Modifier = Modifier,
    datosPronostico: List<PronosticoEntry>,
    rangoSeleccionado: String,
    lineaTempColor: Color,
    barraHumedadColor: Color,
    etiquetaTextColor: Color,
    lineaGuiaColor: Color
) {
    if (datosPronostico.isEmpty()) return

    val minTemp = datosPronostico.minOfOrNull { it.temp } ?: 0.0
    val maxTemp = datosPronostico.maxOfOrNull { it.temp } ?: 0.0
    val paddedMinTemp = (minTemp - 2).roundToInt().toDouble()
    val paddedMaxTemp = (maxTemp + 2).roundToInt().toDouble()
    val rangoTemp = (paddedMaxTemp - paddedMinTemp).coerceAtLeast(1.0)

    val textMeasurer = rememberTextMeasurer()
    val paddingEjeYIzquierda = 48.dp
    val paddingEjeYDerecha = 48.dp
    val paddingEjeXAbajo = 40.dp

    Canvas(modifier = modifier) {
        val anchoTotal = size.width
        val altoTotal = size.height
        val anchoCanvas = anchoTotal - paddingEjeYIzquierda.toPx() - paddingEjeYDerecha.toPx()
        val altoCanvas = altoTotal - paddingEjeXAbajo.toPx()
        val anchoPaso = if (datosPronostico.size > 1) anchoCanvas / (datosPronostico.size - 1) else anchoCanvas

        val estiloEtiquetaTemp = TextStyle(fontSize = 10.sp, color = etiquetaTextColor, textAlign = TextAlign.End)
        val numeroDeEtiquetasY = 5
        val pasoEtiquetaTemp = rangoTemp / numeroDeEtiquetasY

        (0..numeroDeEtiquetasY).forEach { i ->
            val temp = paddedMinTemp + (i * pasoEtiquetaTemp)
            val y = altoCanvas - ((i.toFloat() / numeroDeEtiquetasY) * altoCanvas)
            val measuredText = textMeasurer.measure("${temp.roundToInt()}°", style = estiloEtiquetaTemp)
            drawText(
                measuredText,
                topLeft = Offset(paddingEjeYIzquierda.toPx() - measuredText.size.width - 4.dp.toPx(), y - measuredText.size.height / 2f)
            )
            drawLine(
                color = lineaGuiaColor,
                start = Offset(paddingEjeYIzquierda.toPx(), y),
                end = Offset(anchoTotal - paddingEjeYDerecha.toPx(), y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f))
            )
        }

        val estiloEtiquetaHumedad = TextStyle(fontSize = 10.sp, color = barraHumedadColor.copy(alpha = 0.8f), textAlign = TextAlign.Start)
        (0..4).forEach { i ->
            val porciento = i * 25
            val y = altoCanvas - ((porciento / 100f) * altoCanvas)
            val measuredText = textMeasurer.measure("$porciento%", style = estiloEtiquetaHumedad)
            drawText(
                measuredText,
                topLeft = Offset(anchoTotal - paddingEjeYDerecha.toPx() + 4.dp.toPx(), y - measuredText.size.height / 2f)
            )
        }

        translate(left = paddingEjeYIzquierda.toPx()) {
            val estiloEtiquetaX = TextStyle(fontSize = 10.sp, color = etiquetaTextColor, textAlign = TextAlign.Center)
            val formatterHora = DateTimeFormatter.ofPattern("HH:mm")
            val formatterFecha = DateTimeFormatter.ofPattern("dd/MM")
            val saltoEtiqueta = if (rangoSeleccionado == RangoTiempo.SIETE_DIAS.texto) 8 else 2

            datosPronostico.forEachIndexed { index, entry ->
                if (index % saltoEtiqueta == 0) {
                    val x = index * anchoPaso
                    val textoCompleto = "${entry.timestamp.format(formatterHora)}\n${entry.timestamp.toLocalDate().format(formatterFecha)}"
                    val measuredText = textMeasurer.measure(textoCompleto, style = estiloEtiquetaX)
                    drawText(
                        measuredText,
                        topLeft = Offset(x - (measuredText.size.width / 2), altoCanvas + 4.dp.toPx())
                    )
                }
            }

            val pathLineaTemp = Path()
            val anchoBarra = (anchoPaso * 0.6f).coerceAtMost(20f)

            datosPronostico.forEachIndexed { index, entry ->
                val x = index * anchoPaso
                val yTemp = (altoCanvas - (((entry.temp - paddedMinTemp) / rangoTemp) * altoCanvas)).toFloat()
                val altoBarraHum = ((entry.humedad / 100.0) * altoCanvas).toFloat()

                drawRect(
                    color = barraHumedadColor.copy(alpha = 0.5f),
                    topLeft = Offset(x - anchoBarra / 2, altoCanvas - altoBarraHum),
                    size = Size(anchoBarra, altoBarraHum)
                )
                if (index == 0) pathLineaTemp.moveTo(x, yTemp) else pathLineaTemp.lineTo(x, yTemp)
            }
            drawPath(path = pathLineaTemp, color = lineaTempColor, style = Stroke(width = 5f))
        }
    }
}
