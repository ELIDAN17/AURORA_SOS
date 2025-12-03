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
fun SensorHistorialScreen(
    navController: NavController,
    viewModel: SensorHistorialViewModel = viewModel(factory = ViewModelFactory(LocalContext.current.applicationContext as Application))
) {
    val uiState by viewModel.uiState.collectAsState()

    val colorLineaTMax = Color(0xFFFF9800)
    val colorLineaTMin = Color.Blue
    val colorBarraPrecip = Color(0xFF90CAF9)
    val etiquetaTextColor = MaterialTheme.colorScheme.onSurface

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial del Sensor", color = MaterialTheme.colorScheme.onBackground) },
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
                    Text(
                        text = "Historial de ${uiState.rangoSeleccionado.texto}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    GraficoLeyendaHistorial(tmaxColor = colorLineaTMax, tminColor = colorLineaTMin, precipColor = colorBarraPrecip, textColor = etiquetaTextColor)

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
                                    is DatosGrafico.Historial -> {
                                        if (datos.datos.isNotEmpty()) {
                                            GraficoLineasHistorial(
                                                modifier = Modifier.fillMaxSize(),
                                                datosHistorial = datos.datos,
                                                etiquetasX = datos.etiquetas,
                                                lineaTMaxColor = colorLineaTMax,
                                                lineaTMinColor = colorLineaTMin,
                                                barraPrecipColor = colorBarraPrecip,
                                                etiquetaTextColor = etiquetaTextColor
                                            )
                                        } else {
                                            Text("No hay datos de historial para mostrar.")
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
                RangoTiempoSensor.entries.forEach { rango ->
                    OutlinedButton(
                        onClick = { viewModel.seleccionarRango(rango) },
                        colors = if (rango == uiState.rangoSeleccionado) {
                            ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            ButtonDefaults.outlinedButtonColors()
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
                ElevatedButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Text("Principal")
                }
            }
        }
    }
}

@Composable
fun GraficoLeyendaHistorial(tmaxColor: Color, tminColor: Color, precipColor: Color, textColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(10.dp).background(tmaxColor, RoundedCornerShape(2.dp)))
        Text(" TMax", fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, end = 8.dp), color = textColor)

        Box(modifier = Modifier.size(10.dp).background(tminColor, RoundedCornerShape(2.dp)))
        Text(" TMin", fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, end = 8.dp), color = textColor)

        Box(modifier = Modifier.size(10.dp).background(precipColor, RoundedCornerShape(2.dp)))
        Text(" Precip. (mm)", fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp), color = textColor)
    }
}

@Composable
fun GraficoLineasHistorial(
    modifier: Modifier = Modifier,
    datosHistorial: List<HistorialEntry>,
    etiquetasX: List<String>,
    lineaTMaxColor: Color,
    lineaTMinColor: Color,
    barraPrecipColor: Color,
    etiquetaTextColor: Color
) {
    if (datosHistorial.isEmpty()) return

    val minTemp = datosHistorial.minOfOrNull { minOf(it.tmin, it.tmax) } ?: 0.0
    val maxTemp = datosHistorial.maxOfOrNull { maxOf(it.tmin, it.tmax) } ?: 0.0
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
        val anchoPaso = if (datosHistorial.size > 1) anchoCanvas / (datosHistorial.size - 1) else anchoCanvas

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
        }

        val estiloEtiquetaPrecip = TextStyle(fontSize = 10.sp, color = barraPrecipColor.copy(alpha = 0.8f), textAlign = TextAlign.Start)
        val maxPrecip = datosHistorial.maxOfOrNull { it.precip }?.toFloat() ?: 0f
        if (maxPrecip > 0f) {
            (0..4).forEach { i ->
                val precipValor = (i / 4f) * maxPrecip
                val y = altoCanvas - ((i.toFloat() / 4f) * altoCanvas)
                val measuredText = textMeasurer.measure("${String.format("%.1f", precipValor)} mm", style = estiloEtiquetaPrecip)
                drawText(
                    measuredText,
                    topLeft = Offset(anchoTotal - paddingEjeYDerecha.toPx() + 4.dp.toPx(), y - measuredText.size.height / 2f)
                )
            }
        }

        translate(left = paddingEjeYIzquierda.toPx()) {
            val estiloEtiquetaX = TextStyle(fontSize = 10.sp, color = etiquetaTextColor, textAlign = TextAlign.Center)
            val saltoEtiqueta = if (etiquetasX.size > 10) 5 else 1
            etiquetasX.forEachIndexed { index, etiqueta ->
                if (index % saltoEtiqueta == 0) {
                    val x = index * anchoPaso
                    val measuredText = textMeasurer.measure(etiqueta, style = estiloEtiquetaX)
                    drawText(
                        measuredText,
                        topLeft = Offset(x - (measuredText.size.width / 2), altoCanvas + 4.dp.toPx())
                    )
                }
            }

            val pathLineaTMax = Path()
            val pathLineaTMin = Path()
            val anchoBarra = (anchoPaso * 0.6f).coerceAtMost(20f)

            datosHistorial.forEachIndexed { index, entry ->
                val x = index * anchoPaso
                val yTMax = (altoCanvas - (((entry.tmax - paddedMinTemp) / rangoTemp) * altoCanvas)).toFloat()
                val yTMin = (altoCanvas - (((entry.tmin - paddedMinTemp) / rangoTemp) * altoCanvas)).toFloat()

                if (entry.precip > 0) {
                    val maxPrecipValue = maxPrecip.coerceAtLeast(0.001f)
                    val altoBarraPrecip = ((entry.precip.toFloat() / maxPrecipValue) * altoCanvas)
                    drawRect(
                        color = barraPrecipColor.copy(alpha = 0.5f),
                        topLeft = Offset(x - anchoBarra / 2, altoCanvas - altoBarraPrecip),
                        size = Size(anchoBarra, altoBarraPrecip)
                    )
                }

                if (index == 0) {
                    pathLineaTMax.moveTo(x, yTMax)
                    pathLineaTMin.moveTo(x, yTMin)
                } else {
                    pathLineaTMax.lineTo(x, yTMax)
                    pathLineaTMin.lineTo(x, yTMin)
                }
            }
            drawPath(path = pathLineaTMin, color = lineaTMinColor, style = Stroke(width = 4f))
            drawPath(path = pathLineaTMax, color = lineaTMaxColor, style = Stroke(width = 5f))
        }
    }
}
