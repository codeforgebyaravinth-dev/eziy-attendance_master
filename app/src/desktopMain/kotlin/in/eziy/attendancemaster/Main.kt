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
import `in`.eziy.attendancemaster.ui.components.EziyHeader
import `in`.eziy.attendancemaster.ui.theme.EziyCyanLight
import `in`.eziy.attendancemaster.ui.theme.EziyNavy
import `in`.eziy.attendancemaster.ui.theme.EziyTheme

@Composable
@Preview
fun DesktopAppContent() {
    var selectedTab by remember { mutableStateOf("home") }

    EziyTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Desktop Navigation Rail Sidebar
                NavigationRail(
                    containerColor = EziyNavy,
                    contentColor = Color.White,
                    modifier = Modifier.width(90.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(vertical = 20.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "EZIY",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            NavigationRailItem(
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("Home", fontSize = 11.sp) },
                                selected = selectedTab == "home",
                                onClick = { selectedTab = "home" },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = EziyNavy,
                                    selectedTextColor = Color.White,
                                    indicatorColor = EziyCyanLight,
                                    unselectedIconColor = Color.White.copy(alpha = 0.7f),
                                    unselectedTextColor = Color.White.copy(alpha = 0.7f)
                                )
                            )

                            NavigationRailItem(
                                icon = { Icon(Icons.Default.History, contentDescription = "History") },
                                label = { Text("History", fontSize = 11.sp) },
                                selected = selectedTab == "history",
                                onClick = { selectedTab = "history" },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = EziyNavy,
                                    selectedTextColor = Color.White,
                                    indicatorColor = EziyCyanLight,
                                    unselectedIconColor = Color.White.copy(alpha = 0.7f),
                                    unselectedTextColor = Color.White.copy(alpha = 0.7f)
                                )
                            )

                            NavigationRailItem(
                                icon = { Icon(Icons.Default.PhoneInTalk, contentDescription = "Intercom") },
                                label = { Text("Intercom", fontSize = 11.sp) },
                                selected = selectedTab == "intercom",
                                onClick = { selectedTab = "intercom" },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = EziyNavy,
                                    selectedTextColor = Color.White,
                                    indicatorColor = EziyCyanLight,
                                    unselectedIconColor = Color.White.copy(alpha = 0.7f),
                                    unselectedTextColor = Color.White.copy(alpha = 0.7f)
                                )
                            )

                            NavigationRailItem(
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Settings", fontSize = 11.sp) },
                                selected = selectedTab == "settings",
                                onClick = { selectedTab = "settings" },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = EziyNavy,
                                    selectedTextColor = Color.White,
                                    indicatorColor = EziyCyanLight,
                                    unselectedIconColor = Color.White.copy(alpha = 0.7f),
                                    unselectedTextColor = Color.White.copy(alpha = 0.7f)
                                )
                            )
                        }
                    }
                }

                // Desktop Main Workspace Content Area
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                ) {
                    EziyHeader(titleText = "Attendance Master — Enterprise Desktop")

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxSize(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            when (selectedTab) {
                                "home" -> {
                                    Text(text = "🏢 Attendance Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(text = "Odoo ERP Connected • Employee Face Check-in Ready", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                "history" -> {
                                    Text(text = "🕒 Attendance Logs History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(text = "Check-in and Check-out Historical Activity", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                "intercom" -> {
                                    Text(text = "📞 VoIP Intercom Dashboard", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(text = "Enterprise Linphone SIP Station Active", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                "settings" -> {
                                    Text(text = "⚙️ System Configuration", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(text = "Server URL, Company Credentials & Preferences", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        title = "Eziy Attendance Master — Enterprise Desktop",
        state = WindowState(size = DpSize(1280.dp, 800.dp))
    ) {
        DesktopAppContent()
    }
}
