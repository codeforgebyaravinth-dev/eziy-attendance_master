package `in`.eziy.attendancemaster

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import `in`.eziy.attendancemaster.data.local.SecurePreferencesManager
import `in`.eziy.attendancemaster.data.repository.AttendanceRepositoryImpl
import `in`.eziy.attendancemaster.location.EziyLocationClient
import `in`.eziy.attendancemaster.service.EziyMqttService
import `in`.eziy.attendancemaster.ui.history.HistoryScreen
import `in`.eziy.attendancemaster.ui.history.HistoryViewModel
import `in`.eziy.attendancemaster.ui.home.HomeScreen
import `in`.eziy.attendancemaster.ui.home.HomeViewModel
import `in`.eziy.attendancemaster.ui.navigation.EziyBottomNavItem
import `in`.eziy.attendancemaster.ui.settings.SettingsScreen
import `in`.eziy.attendancemaster.ui.settings.SettingsViewModel
import `in`.eziy.attendancemaster.ui.setup.SetupScreen
import `in`.eziy.attendancemaster.ui.setup.SetupViewModel
import `in`.eziy.attendancemaster.ui.theme.EziyCyanLight
import `in`.eziy.attendancemaster.ui.theme.EziyNavy
import `in`.eziy.attendancemaster.ui.theme.EziyTheme

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force Light Mode globally at Android OS level
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        val prefs = SecurePreferencesManager(applicationContext)
        val repository = AttendanceRepositoryImpl(prefs)
        val locationClient = EziyLocationClient(applicationContext)

        requestNeededPermissions()

        if (prefs.isLoggedIn()) {
            startMqttService()
        }

        setContent {
            EziyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    EziyNavApp(
                        prefs = prefs,
                        repository = repository,
                        locationClient = locationClient,
                        onRequestPermissions = { requestNeededPermissions() },
                        onStartMqtt = { startMqttService() },
                        onStopMqtt = { stopMqttService() }
                    )
                }
            }
        }
    }

    private fun requestNeededPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun startMqttService() {
        try {
            val intent = Intent(this, EziyMqttService::class.java)
            ContextCompat.startForegroundService(this, intent)
        } catch (_: Exception) {}
    }

    private fun stopMqttService() {
        try {
            stopService(Intent(this, EziyMqttService::class.java))
        } catch (_: Exception) {}
    }
}

@Composable
fun EziyNavApp(
    prefs: SecurePreferencesManager,
    repository: AttendanceRepositoryImpl,
    locationClient: EziyLocationClient,
    onRequestPermissions: () -> Unit,
    onStartMqtt: () -> Unit,
    onStopMqtt: () -> Unit
) {
    val navController = rememberNavController()
    val startDestination = if (prefs.isLoggedIn()) "home" else "setup"

    val setupViewModel = remember { SetupViewModel(repository) }
    val homeViewModel = remember { HomeViewModel(repository, prefs, locationClient) }
    val historyViewModel = remember { HistoryViewModel(repository) }
    val settingsViewModel = remember { SettingsViewModel(prefs) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in listOf("home", "history", "settings")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    color = Color.White,
                    tonalElevation = 0.dp
                ) {
                    Column {
                        HorizontalDivider(
                            color = Color(0xFFEEEEEE),
                            thickness = 1.dp
                        )
                        NavigationBar(
                            containerColor = Color.White,
                            tonalElevation = 0.dp,
                            modifier = Modifier.height(68.dp)
                        ) {
                            EziyBottomNavItem.items.forEach { item ->
                                val selected = currentRoute == item.route
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.title,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = item.title,
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    selected = selected,
                                    onClick = {
                                        if (currentRoute != item.route) {
                                            if (item.route == "history") historyViewModel.loadHistory()
                                            if (item.route == "home") {
                                                homeViewModel.loadUserInfo()
                                                homeViewModel.refreshStatus()
                                            }
                                            if (item.route == "settings") {
                                                settingsViewModel.loadSettings()
                                            }
                                            navController.navigate(item.route) {
                                                popUpTo(navController.graph.findStartDestination().id) {
                                                    saveState = true
                                                }
                                                launchSingleTop = true
                                                restoreState = true
                                            }
                                        }
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = EziyNavy,
                                        selectedTextColor = EziyNavy,
                                        indicatorColor = EziyCyanLight,
                                        unselectedIconColor = Color(0xFF6B7280),
                                        unselectedTextColor = Color(0xFF6B7280)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("setup") {
                SetupScreen(
                    viewModel = setupViewModel,
                    onSetupSuccess = {
                        onStartMqtt()
                        setupViewModel.resetSuccess()
                        homeViewModel.loadUserInfo()
                        homeViewModel.refreshStatus()
                        settingsViewModel.loadSettings()
                        navController.navigate("home") {
                            popUpTo("setup") { inclusive = true }
                        }
                    }
                )
            }

            composable("home") {
                HomeScreen(
                    viewModel = homeViewModel,
                    onRequestPermissions = onRequestPermissions
                )
            }

            composable("history") {
                HistoryScreen(
                    viewModel = historyViewModel
                )
            }

            composable("settings") {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onLogout = {
                        onStopMqtt()
                        navController.navigate("setup") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
