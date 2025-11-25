package com.example.aurora_sos

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Principal : Screen("principal")
    data object Configuracion : Screen("configuracion")
    data object Historial : Screen("historial")
    data object SensorHistorial : Screen("sensor_historial")
}
