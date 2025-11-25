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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
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
        initial = UserPreferences(2.0, true, -15.84, -70.02, "Puno", "PE")
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

    LaunchedEffect(uiState.temperaturaActual, umbralCritico) {
        viewModel.actualizarAlerta(umbralCritico)
    }

    LaunchedEffect(uiState.alerta, notificacionesActivas) {
        if (uiState.alerta is Alerta.Helada && notificacionesActivas) {
            notificationService.showNotification(uiState.temperaturaActual)
        }
    }

    LaunchedEffect(uiState.alertaPredictiva, notificacionesActivas) {
        if (uiState.alertaPredictiva != null && notificacionesActivas) {
            notificationService.showPredictiveNotification(uiState.alertaPredictiva!!)
        }
    }

    val colorFondo by animateColorAsState(
        targetValue = when (uiState.alerta) {
            is Alerta.Helada -> Color.Red
            is Alerta.Moderado -> Color(0xFFFF9800)
            is Alerta.Estable -> Color(0xFF8BC34A)
        },
        animationSpec = tween(1000),
        label = "colorFondo"
    )

    val pullToRefreshState = rememberPullToRefreshState()
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.lanzarLlamadaApi()
        }
    }
    
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            pullToRefreshState.endRefresh()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(pullToRefreshState.nestedScrollConnection)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(colorFondo).padding(it)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = uiState.nombreCiudad,
                    fontSize = 24.sp,
                    color = Color.Black.copy(alpha = 0.9f),
                    modifier = Modifier.padding(top = 16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp)) // Espacio flexible

                AnimatedContent(
                    targetState = uiState.temperaturaActual.toInt(),
                    label = "tempAnim",
                    transitionSpec = {
                        slideInVertically(animationSpec = tween(500)) { h -> h } togetherWith
                                slideOutVertically(animationSpec = tween(500)) { h -> -h }
                    }
                ) { temp ->
                    Text(text = "$temp°C", fontSize = 120.sp, color = Color.Black)
                }
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    DatoClimatico("Humedad", "${uiState.datosClimaticos.humedad.toInt()}%")
                    DatoClimatico("Viento", "${String.format(Locale.US, "%.1f", uiState.datosClimaticos.velocidadViento)} km/h")
                }

                AnimatedContent(
                    targetState = when (uiState.alerta) {
                        is Alerta.Helada -> "Alerta: Riesgo de Helada"
                        is Alerta.Moderado -> "Alerta: Riesgo Moderado"
                        is Alerta.Estable -> "Alerta: Estable"
                    },
                    label = "alertaAnim"
                ) { msg ->
                    Text(text = msg, fontSize = 24.sp, color = Color.Black, modifier = Modifier.padding(top = 8.dp))
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (uiState.pronosticoPorHoras.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(uiState.pronosticoPorHoras) { pronostico ->
                            PronosticoHoraItem(pronostico = pronostico)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (uiState.pronosticoHelada != null) {
                    PronosticoView(pronostico = uiState.pronosticoHelada!!)
                } else if (uiState.error != null) {
                    Text(
                        text = uiState.error!!,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f, fill = false)) // No ocupa espacio si no es necesario

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 16.dp, end = 16.dp, top = 32.dp),
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
                        Text("Historial")
                    }
                    ElevatedButton(
                        onClick = { navController.navigate(Screen.Configuracion.route) },
                        colors = buttonColors
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
fun DatoClimatico(nombre: String, valor: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = nombre, fontSize = 14.sp, color = Color.Black.copy(alpha = 0.8f))
        Text(text = valor, fontSize = 20.sp, color = Color.Black)
    }
}

@Composable
private fun PronosticoHoraItem(pronostico: PronosticoHora) {
    val formatter = remember { DateTimeFormatter.ofPattern("h a") }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "${pronostico.temperatura.toInt()}°", color = Color.Black, fontSize = 20.sp)
        Text(text = pronostico.hora.format(formatter), color = Color.Black.copy(alpha = 0.8f), fontSize = 14.sp)
    }
}

@Composable
private fun PronosticoView(pronostico: PronosticoHelada) {
    val formatter = remember { DateTimeFormatter.ofPattern("h:mm a", Locale("es", "ES")) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Próxima baja temperatura:",
            fontSize = 14.sp,
            color = Color.Black
        )
        Text(
            text = "${pronostico.temperaturaMinima.toInt()}°C",
            fontSize = 32.sp,
            color = Color.Black
        )
        Text(
            text = "aprox. a las ${pronostico.hora.format(formatter)}",
            fontSize = 12.sp,
            color = Color.Black.copy(alpha = 0.9f)
        )
    }
}
