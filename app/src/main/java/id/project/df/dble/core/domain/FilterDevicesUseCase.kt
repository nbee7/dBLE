package id.project.df.dble.core.domain

import id.project.df.dble.core.model.TrackedDevice
import javax.inject.Inject

enum class FilterField { NAME, MAC }

class FilterDevicesUseCase @Inject constructor() {

    operator fun invoke(
        devices: List<TrackedDevice>,
        query: String,
        field: FilterField,
        minRssiEnabled: Boolean,
        minRssi: Int,
    ): List<TrackedDevice> {
        val q = query.trim()
        val filtered = devices.filter { device ->
            val matchQuery = q.isEmpty() || when (field) {
                FilterField.NAME -> device.name?.contains(q, ignoreCase = true) == true
                FilterField.MAC -> device.mac.contains(q, ignoreCase = true)
            }
            val aboveThreshold = !minRssiEnabled || device.rawRssi >= minRssi
            matchQuery && aboveThreshold
        }
        return filtered.sortedByDescending { it.smoothedRssi }
    }
}
