package com.weatherapp

import ForecastMonitor
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.util.Consumer
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.weatherapp.api.WeatherService
import com.weatherapp.db.fb.FBDatabase
import com.weatherapp.db.local.LocalDatabase
import com.weatherapp.model.MainViewModel
import com.weatherapp.model.MainViewModel.MainViewModelFactory
import com.weatherapp.repo.Repository
import com.weatherapp.ui.components.BottomNavBar
import com.weatherapp.ui.components.CityDialog
import com.weatherapp.ui.nav.BottomNavItem
import com.weatherapp.ui.nav.MainNavHost
import com.weatherapp.ui.nav.Route
import com.weatherapp.ui.pages.HomePage
import com.weatherapp.ui.theme.WeatherAppTheme


class MainActivity : ComponentActivity() {
    @SuppressLint("RestrictedApi", "RememberReturnType")
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {

            val fbDB = remember { FBDatabase() }
            val user = Firebase.auth.currentUser
            val dbName = if (user != null) {
                "local_database_${user.uid}"
            } else {
                "local_database_anonimo"
            }
            val localDB = remember { LocalDatabase(this, dbName) }
            val repository = remember { Repository(
                fbDB = fbDB,
                localDB = localDB
            ) }
            val monitor = ForecastMonitor(this)
            val weatherService = remember { WeatherService() }
            val viewModel: MainViewModel = viewModel(
                factory = MainViewModelFactory(repository = repository, weatherService, monitor)
            )
            DisposableEffect(Unit) {
                val listener = Consumer<Intent> { intent ->
                    val name = intent.getStringExtra("city")
                    val city = viewModel.cities.find { it.name == name }
                    viewModel.city = city
                    viewModel.page = Route.Home
                }
                addOnNewIntentListener(listener)
                onDispose { removeOnNewIntentListener(listener) }
            }
            val navController = rememberNavController()
            var showDialog by remember { mutableStateOf(false) }
            val currentRoute = navController.currentBackStackEntryAsState()
            val showButton = currentRoute.value?.destination?.hasRoute(Route.List::class) == true
            val launcher = rememberLauncherForActivityResult(
                contract =
                    ActivityResultContracts.RequestPermission(), onResult = {})


            val systemDarkTheme = isSystemInDarkTheme()
            var isDarkTheme by remember { mutableStateOf(systemDarkTheme) }

            WeatherAppTheme(darkTheme = isDarkTheme) {
                if (showDialog) CityDialog(
                    onDismiss = { showDialog = false },
                    onConfirm = { city ->
                        if (city.isNotBlank()) viewModel.add(city)
                        showDialog = false
                    })

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = {
                                val name = viewModel.user?.name ?: "[não logado]"
                                Text(("Bem-vindo/a! $name"))
                            },

                            actions = {
                                IconButton(onClick = { isDarkTheme = !isDarkTheme }) {
                                    val isCurrentlyDark = isDarkTheme
                                    Icon(
                                        imageVector = if (isCurrentlyDark) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                        contentDescription = if (isCurrentlyDark) "Ativar modo claro" else "Ativar modo escuro"
                                    )
                                }
                                IconButton(onClick = {
                                    Firebase.auth.signOut()
                                }) {
                                    Icon(
                                        imageVector =
                                            Icons.AutoMirrored.Filled.ExitToApp,
                                        contentDescription = "Localized description"
                                    )
                                }
                            }
                        )
                    },

                    bottomBar = {
                        val items = listOf(
                            BottomNavItem.HomeButton,
                            BottomNavItem.ListButton,
                            BottomNavItem.MapButton,

                            )

                        BottomNavBar(viewModel, items)

                    },

                    floatingActionButton = {
                        if (showButton) {
                            FloatingActionButton(onClick = { showDialog = true }) {
                                Icon(Icons.Default.Add, contentDescription = "Adicionar")
                            }
                        }
                    }

                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        MainNavHost(navController = navController, viewModel = viewModel)
                    }
                    LaunchedEffect(viewModel.page) {
                        navController.navigate(viewModel.page) {
                            // Volta pilha de navegação até HomePage (startDest).
                            navController.graph.startDestinationRoute?.let {
                                popUpTo(it) {
                                    saveState = true
                                }
                                restoreState = true
                            }
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    }
}

private fun NavDestination?.hasRoute(string: String) {}

@Preview(showBackground = true)
@Composable
fun HomePagePreview() {
    WeatherAppTheme {
        HomePage(Modifier = Modifier, viewModel = viewModel())
    }
}
