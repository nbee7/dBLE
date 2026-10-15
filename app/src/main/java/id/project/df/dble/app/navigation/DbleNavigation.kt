package id.project.df.dble.app.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import id.project.df.dble.feature.history.HistoryScreen
import id.project.df.dble.feature.scanner.ScannerScreen
import id.project.df.dble.feature.tracking.TrackingScreen

private data class TopLevelTab(val route: NavKey, val label: String, val icon: ImageVector)

private val Tabs = listOf(
    TopLevelTab(ScannerRoute, "Pindai", Icons.Filled.Search),
    TopLevelTab(HistoryRoute, "Riwayat", Icons.Filled.Info),
)

@Composable
fun DbleNavHost() {
    // Back stack saveable (tahan rotasi/process death); Tracking selalu di atas satu tab.
    val backStack = rememberNavBackStack(ScannerRoute)
    val current = backStack.lastOrNull()
    val showBottomBar = current == ScannerRoute || current == HistoryRoute
    val activity = LocalActivity.current

    // Kedua tab tetap ada di back stack (hanya diurutkan ulang) agar ViewModel tab, mis. query/filter Scanner, tidak hancur.
    fun selectTab(route: NavKey) {
        backStack.removeAll { it == route }
        backStack.add(route)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = current == tab.route,
                            onClick = { if (current != tab.route) selectTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(innerPadding),
            onBack = {
                when (current) {
                    is TrackingRoute -> backStack.removeLastOrNull()
                    HistoryRoute -> selectTab(ScannerRoute)
                    else -> activity?.finish()
                }
            },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<ScannerRoute> {
                    ScannerScreen(onDeviceClick = { backStack.add(TrackingRoute(it)) })
                }
                entry<HistoryRoute> {
                    HistoryScreen(onEntryClick = { backStack.add(TrackingRoute(it)) })
                }
                entry<TrackingRoute> { key ->
                    TrackingScreen(route = key, onBack = { backStack.removeLastOrNull() })
                }
            },
        )
    }
}
