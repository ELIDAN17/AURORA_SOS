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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

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

    //val fondoClaro = Color(0xFF87CEEB)
    val colorLetraAzul = Color(0xFF0D47A1)

    val context = LocalContext.current
    val dataStoreManager = remember { DataStoreManager(context) }
    val notificationService = remember { NotificationService(context) }

    val configData by dataStoreManager.preferencesFlow.collectAsState(
        initial = Pair(2.0, true)
    )
    val umbralCritico = configData.first
    val notificacionesActivas = configData.second

    // --- CONEXIÓN A FIREBASE PARA DATOS REALES ---
    var temperaturaActual by remember { mutableStateOf(15.0) }

    DisposableEffect(Unit) {

        val database = Firebase.database("https://aurorasos-default-rtdb.firebaseio.com/")
        val tempRef = database.getReference("aurora/temperatura")

        // 3. Creamos el "oyente"
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

    val (colorObjetivo, mensajeAlerta) = getAlertaData(temperaturaActual, umbralCritico)
    val fondoColorAnimado by animateColorAsState(
        targetValue = colorObjetivo,
        label = "ColorDeFondoAnimado",
        animationSpec = tween(durationMillis = 1000) // 1 segundo de transición
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
                Spacer(modifier = Modifier.height(340.dp))
                AnimatedContent(targetState = mensajeAlerta, label = "AnimacionMensajeAlerta",
                    transitionSpec = {slideInVertically(animationSpec = tween(500))
                    {it} togetherWith slideOutVertically(animationSpec = tween(500))
                    {-it}}) { mensaje ->
                    Text(text = mensaje, fontSize = 24.sp,color=Color.Black)

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${temperaturaActual.toInt()}° C",
                        fontSize = 120.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = mensajeAlerta,
                        fontSize = 24.sp,
                        color = Color.Black
                    )

                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val buttonColors = ButtonDefaults.elevatedButtonColors(
                        containerColor = Color.White,
                        contentColor = colorLetraAzul
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

@Preview(showBackground = true)
@Composable
fun PrincipalScreenPreview() {
    PrincipalScreen(navController = rememberNavController())
}