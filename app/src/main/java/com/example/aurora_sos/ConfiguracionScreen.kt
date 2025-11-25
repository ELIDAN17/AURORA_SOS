package com.example.aurora_sos

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionScreen(
    navController: NavController,
    viewModel: ConfiguracionViewModel = viewModel(factory = ViewModelFactory(LocalContext.current.applicationContext as Application))
) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val uiState by viewModel.uiState.collectAsState()

    // --- State and Logic for Permissions ---
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else { true }
        )
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
            if (!isGranted) {
                scope.launch { dataStoreManager.saveConfiguracion(dataStoreManager.preferencesFlow.first().copy(notificacionesActivas = false)) }
            }
        }
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
                val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                        .addOnSuccessListener { location ->
                            if (location != null) {
                                viewModel.buscarCiudadPorCoordenadas(location.latitude, location.longitude)
                            } else {
                                scope.launch { snackbarHostState.showSnackbar("No se pudo obtener la ubicación. Intenta de nuevo.") }
                            }
                        }
                        .addOnFailureListener { 
                            scope.launch { snackbarHostState.showSnackbar("Error al obtener la ubicación: ${it.message}") }
                        }
                }
            }
        }
    )

    // --- State for TextFields ---
    val configData by dataStoreManager.preferencesFlow.collectAsState(
        initial = UserPreferences(2.0, true, -15.84, -70.02, "Puno", "PE")
    )

    var umbralText by remember { mutableStateOf("") }
    var ciudadText by remember { mutableStateOf("") }
    var paisText by remember { mutableStateOf("") }

    LaunchedEffect(configData) {
        umbralText = configData.umbralHelada.toString()
        ciudadText = configData.nombreCiudad
        paisText = configData.codigoPais
    }

    val notificacionesSwitchState = hasNotificationPermission && configData.notificacionesActivas

    // --- Side Effects ---
    LaunchedEffect(uiState.error, uiState.guardadoConExito) {
        if (uiState.error != null) {
            snackbarHostState.showSnackbar(uiState.error!!, duration = SnackbarDuration.Long)
            viewModel.resetState()
        }
        if (uiState.guardadoConExito) {
            snackbarHostState.showSnackbar("¡Configuración guardada con éxito!")
            navController.previousBackStackEntry?.savedStateHandle?.set("config_updated", true)
            navController.popBackStack()
        }
    }

    LaunchedEffect(uiState.ciudadEncontrada, uiState.paisEncontrado) {
        uiState.ciudadEncontrada?.let { ciudadText = it }
        uiState.paisEncontrado?.let { paisText = it }
    }

    val textFieldColors = TextFieldDefaults.outlinedTextFieldColors(
        focusedBorderColor = Color.Black,
        unfocusedBorderColor = Color.Black.copy(alpha = 0.7f),
        focusedLabelColor = Color.Black,
        unfocusedLabelColor = Color.Black.copy(alpha = 0.8f),
        cursorColor = Color.Black
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { 
            TopAppBar(
                title = { Text("Configuración", color = Color.Black) }, 
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF87CEEB))
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFF87CEEB)).padding(paddingValues).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text("Umbral de Alerta", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black)
            OutlinedTextField(
                value = umbralText,
                onValueChange = { umbralText = it.filter { char -> char.isDigit() || char == '.' || char == '-' } },
                label = { Text("Temperatura crítica (°C)") },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("Ubicación", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(
                text = "Si tu ciudad no es encontrada, intenta con una localidad cercana más grande.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Button(onClick = { 
                locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }) {
                Icon(Icons.Default.LocationOn, contentDescription = "Usar ubicación actual")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Usar mi ubicación actual")
            }
            
            OutlinedTextField(
                value = ciudadText,
                onValueChange = { ciudadText = it },
                label = { Text("Ciudad") },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = paisText,
                onValueChange = { paisText = it },
                label = { Text("Código de País (ej: PE, US, ES)") },
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Activar Notificaciones", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = Color.Black)
                Switch(
                    checked = notificacionesSwitchState,
                    onCheckedChange = { isChecked ->
                        scope.launch {
                            if (isChecked) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    dataStoreManager.saveConfiguracion(configData.copy(notificacionesActivas = true))
                                }
                            } else {
                                dataStoreManager.saveConfiguracion(configData.copy(notificacionesActivas = false))
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            val buttonColors = ButtonDefaults.elevatedButtonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF0D47A1)
            )

            ElevatedButton(
                onClick = { viewModel.guardarConfiguracion(umbralText, ciudadText, paisText, notificacionesSwitchState) },
                colors = buttonColors,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Guardar y Actualizar")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}