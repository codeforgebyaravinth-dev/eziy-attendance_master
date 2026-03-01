package `in`.eziy.attendancemaster.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class EziyBottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : EziyBottomNavItem("home", "Home", Icons.Default.Home)
    object History : EziyBottomNavItem("history", "History", Icons.Default.DateRange)
    object Settings : EziyBottomNavItem("settings", "Settings", Icons.Default.Settings)

    companion object {
        val items = listOf(Home, History, Settings)
    }
}
