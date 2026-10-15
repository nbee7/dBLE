package id.project.df.dble.core.domain

import id.project.df.dble.core.data.scan.DeviceScanRepository
import id.project.df.dble.core.model.TrackedDevice
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveDevicesUseCase @Inject constructor(
    private val scan: DeviceScanRepository,
) {
    operator fun invoke(): Flow<List<TrackedDevice>> = scan.devices
}
