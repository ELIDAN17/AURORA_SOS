package com.example.aurora_sos

import android.app.Application
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.aurora_sos.ui.theme.*
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrincipalScreen(
    navController: NavController,
    viewModel: PrincipalViewModel = viewModel(factory = ViewModelFactory(LocalContext.current.applicationContext as Application))
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val notificationService = remember { NotificationService(context) }

    val configData by dataStoreManager.preferencesFlow.collectAsState(
        initial = UserPreferences(2.0, true, -12.0464, -77.0428, "Lima", "PE")
    )
    val umbralCritico = configData.umbralHelada
    val notificacionesActivas = configData.notificacionesActivas

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.lanzarLlamadaApi()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(uiState.temperaturaApi, umbralCritico) {
        viewModel.actualizarAlertaApi(umbralCritico)
    }

    LaunchedEffect(uiState.temperaturaSensor, umbralCritico, uiState.datosClimaticosSensor.puntoRocio) {
        viewModel.actualizarAlertaSensor(umbralCritico, uiState.datosClimaticosSensor.puntoRocio)
    }

    LaunchedEffect(uiState.alertaApi, notificacionesActivas) {
        if (uiState.alertaApi is Alerta.Helada && notificacionesActivas) {
            notificationService.showNotification(uiState.temperaturaApi)
        }
    }

    LaunchedEffect(uiState.alertaSensor, notificacionesActivas) {
        if (uiState.alertaSensor is Alerta.Helada && notificacionesActivas) {
            notificationService.showNotification(uiState.temperaturaSensor)
        }
    }

    LaunchedEffect(uiState.alertaPredictivaApi, notificacionesActivas) {
        uiState.alertaPredictivaApi?.let {
            if (notificacionesActivas) {
                notificationService.showPredictiveNotification(it)
            }
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.lanzarLlamadaApi()
            pullToRefreshState.endRefresh()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(pullToRefreshState.nestedScrollConnection)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(paddingValues)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    SeccionClima(
                        titulo = uiState.nombreCiudad,
                        temperatura = uiState.temperaturaApi,
                        alerta = uiState.alertaApi,
                        datosClimaticos = {
                            DatoClimatico("Humedad", "${uiState.datosClimaticosApi.humedad.toInt()}%")
                            DatoClimatico("Pto. Rocío", "${String.format(Locale.US, "%.1f", uiState.datosClimaticosApi.puntoRocio)}°C")
                            DatoClimatico("Temp. Suelo", "${String.format(Locale.US, "%.1f", uiState.datosClimaticosApi.soilTemperature)}°C")
                        },
                        pronosticoPorHoras = {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(uiState.pronosticoPorHorasApi) { pronostico ->
                                    PronosticoHoraItem(pronostico = pronostico)
                                }
                            }
                        },
                        pronosticoHelada = {
                            uiState.pronosticoHeladaApi?.let { pronostico ->
                                PronosticoView(pronostico = pronostico)
                            } ?: uiState.error?.let {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.error,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SeccionClima(
                        titulo = "Sensor Local",
                        temperatura = uiState.temperaturaSensor,
                        alerta = uiState.alertaSensor,
                        datosClimaticos = {
                            DatoClimatico("Humedad", "${uiState.datosClimaticosSensor.humedad.toInt()}%")
                            DatoClimatico("Lluvia", "${uiState.datosClimaticosSensor.lluvia} mm")
                            DatoClimatico("Pto. Rocío", "${String.format(Locale.US, "%.1f", uiState.datosClimaticosSensor.puntoRocio)}°C")
                        },
                        pronosticoPorHoras = {},
                        pronosticoHelada = {},
                        tendenciaMessage = uiState.indicadorRiesgo.message 
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    var menuHistorialVisible by remember { mutableStateOf(false) }

                    Box {
                        ElevatedButton(
                            onClick = { menuHistorialVisible = true }
                        ) {
                            Text("Historial")
                        }
                        DropdownMenu(
                            expanded = menuHistorialVisible,
                            onDismissRequest = { menuHistorialVisible = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Pronóstico API") },
                                onClick = {
                                    navController.navigate(Screen.Historial.route)
                                    menuHistorialVisible = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Historial Sensor") },
                                onClick = {
                                    navController.navigate(Screen.SensorHistorial.route)
                                    menuHistorialVisible = false
                                }
                            )
                        }
                    }

                    ElevatedButton(
                        onClick = { navController.navigate(Screen.Configuracion.route) }
                    ) {
                        Text("Configuración")
                    }
                }
            }

            PullToRefreshContainer(
                modifier = Modifier.align(Alignment.TopCenter),
                state = pullToRefreshState,
            )
        }
    }
}

@Composable
fun SeccionClima(
    titulo: String,
    temperatura: Double,
    alerta: Alerta,
    datosClimaticos: @Composable RowScope.() -> Unit,
    pronosticoPorHoras: @Composable () -> Unit,
    pronosticoHelada: @Composable () -> Unit,
    tendenciaMessage: String? = null
) {
    val cardContentColor = Color.Black

    val colorFondo by animateColorAsState(
        targetValue = when (alerta) {
            is Alerta.Helada -> AlertRed
            is Alerta.Moderado -> AlertOrange
            is Alerta.Estable -> AlertGreen
        },
        animationSpec = tween(1000),
        label = "colorFondoSeccion"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorFondo),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = titulo,
                fontSize = 20.sp,
                color = cardContentColor.copy(alpha = 0.9f)
            )

            AnimatedContent(
                targetState = temperatura.toInt(),
                label = "tempAnimSeccion",
                transitionSpec = {
                    slideInVertically(animationSpec = tween(500)) { h -> h } togetherWith
                            slideOutVertically(animationSpec = tween(500)) { h -> -h }
                }
            ) { temp ->
                Text(text = "$temp°C", fontSize = 60.sp, color = cardContentColor)
            }

            tendenciaMessage?.let {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = it,
                    color = Color.Black,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                content = datosClimaticos
            )

            Spacer(modifier = Modifier.height(16.dp))

            pronosticoPorHoras()

            pronosticoHelada()
        }
    }
}

@Composable
fun DatoClimatico(nombre: String, valor: String) {
    val cardContentColor = Color.Black
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(text = nombre, fontSize = 12.sp, color = cardContentColor.copy(alpha = 0.8f))
        Text(text = valor, fontSize = 16.sp, color = cardContentColor, textAlign = TextAlign.Center)
    }
}

@Composable
private fun PronosticoHoraItem(pronostico: PronosticoHoraApi) {
    val cardContentColor = Color.Black
    val formatter = remember { DateTimeFormatter.ofPattern("h a") }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(text = pronostico.hora.format(formatter), color = cardContentColor.copy(alpha = 0.8f), fontSize = 12.sp)
        Text(text = "${pronostico.temperatura.toInt()}°C", color = cardContentColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Divider(modifier = Modifier.width(24.dp))
        Text(text = "Rocío: ${pronostico.dewPoint.toInt()}°", color = cardContentColor.copy(alpha = 0.7f), fontSize = 12.sp)
        Text(text = "Suelo: ${pronostico.soilTemperature.toInt()}°", color = cardContentColor.copy(alpha = 0.7f), fontSize = 12.sp)
    }
}

@Composable
private fun PronosticoView(pronostico: PronosticoHeladaApi) {
    val cardContentColor = Color.Black
    val formatter = remember { DateTimeFormatter.ofPattern("h:mm a", Locale("es", "ES")) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Próxima baja temp:",
            fontSize = 12.sp,
            color = cardContentColor
        )
        Text(
            text = "${pronostico.temperaturaMinima.toInt()}°C a las ${pronostico.hora.format(formatter)}",
            fontSize = 14.sp,
            color = cardContentColor.copy(alpha = 0.9f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
    }
}
