package com.example.aurora_sos

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    // Definimos el color de fondo
    val fondoClaro = Color(0xFF87CEEB)

    LaunchedEffect(key1 = true) {
        delay(4000L)
        navController.popBackStack()
        navController.navigate(Screen.Principal.route)
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = fondoClaro
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "AURORA SOS",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 9.dp),
                color = Color.Black
            )
            CircularProgressIndicator(
                modifier = Modifier.size(32.dp),
                color = Color.White
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    com.example.aurora_sos.ui.theme.AURORA_SOSTheme {
        SplashScreen(navController = rememberNavController())
    }
}