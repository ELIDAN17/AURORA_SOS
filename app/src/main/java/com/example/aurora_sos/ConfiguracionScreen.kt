// Archivo: ConfiguracionScreen.kt

package com.example.aurora_sos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracionScreen(navController: NavController) {
    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val scope = rememberCoroutineScope()

    // --- LÓGICA DE PERMISOS DE NOTIFICACIÓN ---
    // 1. Comprobamos si ya tenemos el permiso
    var hasNotificationPermission by remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            )
        } else {
            mutableStateOf(true)
        }
    }

    // 2. Preparamos el lanzador de la solicitud de permiso
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
            if (!isGranted) {
                // Si el usuario deniega el permiso, nos aseguramos de guardar el estado del switch como "apagado"
                scope.launch {
                    val currentConfig = dataStoreManager.preferencesFlow.first()
                    dataStoreManager.saveConfiguracion(currentConfig.first, false)
                }
            }
        }
    )

    val configData by dataStoreManager.preferencesFlow.collectAsState(
        initial = Pair(2.0, true)
    )

    var umbralText by remember { mutableStateOf("") }

    LaunchedEffect(configData) {
        umbralText = configData.first.toString()
    }

    // El estado del switch ahora depende de si el permiso está concedido Y de lo que guardó el usuario
    val notificacionesActivadas = hasNotificationPermission && configData.second


    Scaffold(
        topBar = { TopAppBar(title = { Text("Configuración") }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF87CEEB))) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF87CEEB))
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(32.dp))
            Text("Umbral de Alerta de Helada", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = umbralText,
                onValueChange = { umbralText = it.filter { char -> char.isDigit() || char == '.' } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text("Temperatura crítica (°C)") },
                modifier = Modifier.width(200.dp)
            )
            Spacer(modifier = Modifier.height(32.dp))

            // --- Control: Activar Notificaciones (CON LÓGICA DE PERMISO) ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal=40.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Activar Notificaciones", style = MaterialTheme.typography.titleLarge)
                Switch(
                    checked = notificacionesActivadas,
                    onCheckedChange = { isChecked ->
                        scope.launch {
                            val currentConfig = dataStoreManager.preferencesFlow.first()
                            if (isChecked) {
                                // 3. Si el usuario quiere activar, PRIMERO pedimos el permiso
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                dataStoreManager.saveConfiguracion(currentConfig.first, true)
                            } else {
                                // Si quiere desactivar, simplemente guardamos el estado
                                dataStoreManager.saveConfiguracion(currentConfig.first, false)
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            ElevatedButton(
                onClick = {
                    val umbralNuevo = umbralText.toDoubleOrNull() ?: configData.first
                    scope.launch {
                        dataStoreManager.saveConfiguracion(umbralNuevo, notificacionesActivadas)
                    }
                    navController.popBackStack()
                },
                modifier = Modifier.width(200.dp).height(50.dp)
            ) {
                Text("Guardar")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}