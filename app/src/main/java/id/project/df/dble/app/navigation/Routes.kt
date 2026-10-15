package id.project.df.dble.app.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object ScannerRoute : NavKey

@Serializable
data object HistoryRoute : NavKey

@Serializable
data class TrackingRoute(val mac: String) : NavKey
