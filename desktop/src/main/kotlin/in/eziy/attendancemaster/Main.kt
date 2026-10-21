package `in`.eziy.attendancemaster

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import `in`.eziy.attendancemaster.data.local.SecurePreferencesManager
import `in`.eziy.attendancemaster.data.repository.AttendanceRepositoryImpl
import `in`.eziy.attendancemaster.sip.EziyLinphoneManager
import `in`.eziy.attendancemaster.ui.history.HistoryScreen
import `in`.eziy.attendancemaster.ui.history.HistoryViewModel
import `in`.eziy.attendancemaster.ui.intercom.SipIntercomCallOverlay
import `in`.eziy.attendancemaster.ui.intercom.SipIntercomScreen
import `in`.eziy.attendancemaster.ui.intercom.SipIntercomViewModel
import `in`.eziy.attendancemaster.ui.settings.SettingsScreen
import `in`.eziy.attendancemaster.ui.settings.SettingsViewModel
import `in`.eziy.attendancemaster.ui.theme.EziyCyanLight
import `in`.eziy.attendancemaster.ui.theme.EziyNavy
import `in`.eziy.attendancemaster.ui.theme.EziyTheme

val EziyNavyColor = Color(0xFF0F172A)
val EziyCyanLightColor = Color(0xFFE0F2FE)

@Composable
@Preview
fun DesktopAppContent() {
    var selectedTab by remember { mutableStateOf("intercom") }

    EziyTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Desktop Navigation Rail Sidebar
                NavigationRail(
                    containerColor = EziyNavyColor,
                    contentColor = Color.White,
                    modifier = Modifier.width(96.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 20.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "VSPL",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )

                            NavigationRailItem(
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                selected = selectedTab == "home",
                                onClick = { selectedTab = "home" },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = EziyNavyColor,
                                    selectedTextColor = Color.White,
                                    indicatorColor = EziyCyanLightColor,
                                    unselectedIconColor = Color.White.copy(alpha = 0.7f),
                                    unselectedTextColor = Color.White.copy(alpha = 0.7f)
                                )
                            )

                            NavigationRailItem(
                                icon = { Icon(Icons.Default.History, contentDescription = "History") },
                                label = { Text("History", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                selected = selectedTab == "history",
                                onClick = { selectedTab = "history" },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = EziyNavyColor,
                                    selectedTextColor = Color.White,
                                    indicatorColor = EziyCyanLightColor,
                                    unselectedIconColor = Color.White.copy(alpha = 0.7f),
                                    unselectedTextColor = Color.White.copy(alpha = 0.7f)
                                )
                            )

                            NavigationRailItem(
                                icon = { Icon(Icons.Default.PhoneInTalk, contentDescription = "Intercom") },
                                label = { Text("Intercom", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                selected = selectedTab == "intercom",
                                onClick = { selectedTab = "intercom" },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = EziyNavyColor,
                                    selectedTextColor = Color.White,
                                    indicatorColor = EziyCyanLightColor,
                                    unselectedIconColor = Color.White.copy(alpha = 0.7f),
                                    unselectedTextColor = Color.White.copy(alpha = 0.7f)
                                )
                            )

                            NavigationRailItem(
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                selected = selectedTab == "settings",
                                onClick = { selectedTab = "settings" },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = EziyNavyColor,
                                    selectedTextColor = Color.White,
                                    indicatorColor = EziyCyanLightColor,
                                    unselectedIconColor = Color.White.copy(alpha = 0.7f),
                                    unselectedTextColor = Color.White.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                }

                // Desktop Workspace Content Area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    when (selectedTab) {
                        "history" -> {
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text("🕒 Attendance History Logs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text("Check-in and Check-out Historical Records", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        "intercom" -> {
                            // Intercom Dashboard Screen
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = MaterialTheme.colorScheme.background
                            ) {
                                Column(modifier = Modifier.fillMaxSize()) {
                                    Text("📞 VSPL Intercom Station", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("3x3 Speed Dial Station & Office Directory Active", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        "settings" -> {
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text("⚙️ System Configuration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text("Configure Server URL, Odoo Company Name & SIP Extension", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        else -> {
                            Card(
                                modifier = Modifier.fillMaxSize(),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text("🏢 Employee Attendance Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text("Odoo ERP Connected • Employee Face Check-in Ready", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "VSPL Attendance Master — Windows Desktop Station",
        state = WindowState(size = DpSize(1280.dp, 800.dp))
    ) {
        DesktopAppContent()
    }
}
