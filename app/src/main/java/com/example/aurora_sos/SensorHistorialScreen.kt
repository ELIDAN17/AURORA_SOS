package com.example.aurora_sos

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt

private enum class HistorialVista(val texto: String) { 
    GRAFICO("Gráfico"), 
    BITACORA("Bitácora") 
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorHistorialScreen(
    navController: NavController,
    viewModel: SensorHistorialViewModel = viewModel(factory = ViewModelFactory(LocalContext.current.applicationContext as Application))
) {
    val uiState by viewModel.uiState.collectAsState()
    var vistaSeleccionada by remember { mutableStateOf(HistorialVista.GRAFICO) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial del Sensor y Bitacora", color = MaterialTheme.colorScheme.onBackground) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = {
             Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ElevatedButton(
                    onClick = { navController.popBackStack() }
                ) {
                    Text("Principal")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                HistorialVista.entries.forEach { vista ->
                    SegmentedButton(
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.secondary,
                            activeContentColor = MaterialTheme.colorScheme.onSecondary,
                            inactiveContainerColor = MaterialTheme.colorScheme.surface,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = SegmentedButtonDefaults.itemShape(index = vista.ordinal, count = HistorialVista.entries.size),
                        onClick = { vistaSeleccionada = vista },
                        selected = vistaSeleccionada == vista
                    ) {
                        Text(vista.texto)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(targetState = vistaSeleccionada, label = "vista_historial") { vista ->
                when (vista) {
                    HistorialVista.GRAFICO -> VistaGrafico(uiState = uiState, viewModel = viewModel)
                    HistorialVista.BITACORA -> VistaBitacora(uiState = uiState)
                }
            }
        }
    }
}

@Composable
private fun VistaGrafico(uiState: SensorHistorialUiState, viewModel: SensorHistorialViewModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxHeight()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
             if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (uiState.error != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(uiState.error!!, color = Color.Red, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp)) }
            } else if (uiState.datosGrafico is DatosGrafico.Historial && uiState.datosGrafico.datos.isNotEmpty()) {
                 GraficoLineasHistorial(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    datosHistorial = uiState.datosGrafico.datos,
                    etiquetasX = uiState.datosGrafico.etiquetas
                )
            } else {
                 Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No hay datos de historial para mostrar.") }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RangoTiempoSensor.entries.forEach { rango ->
                FilterChip(
                    selected = uiState.rangoSeleccionado == rango,
                    onClick = { viewModel.seleccionarRango(rango) },
                    label = { Text(rango.texto) }
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}

@Composable
private fun VistaBitacora(uiState: SensorHistorialUiState) {
    if (uiState.isLoading && uiState.eventosTendencia.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }
    if (uiState.eventosTendencia.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No hay eventos de tendencia registrados.", textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(uiState.eventosTendencia) { evento ->
            val fechaHora = try {
                LocalDateTime.parse(evento.timestamp, DateTimeFormatter.ISO_DATE_TIME)
                    .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
            } catch (e: Exception) {
                evento.timestamp
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = fechaHora,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.weight(0.4f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = evento.mensaje,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun GraficoLineasHistorial(
    modifier: Modifier = Modifier,
    datosHistorial: List<HistorialEntry>,
    etiquetasX: List<String>,
    lineaTMaxColor: Color = Color(0xFFFF9800),
    lineaTMinColor: Color = Color.Blue,
    barraPrecipColor: Color = Color(0xFF90CAF9),
    etiquetaTextColor: Color = MaterialTheme.colorScheme.onSurface
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
        val maxPrecip = datosHistorial.maxOfOrNull { it.precip.toFloat() } ?: 0f
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
