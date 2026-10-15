package id.project.df.dble.core.data.scan

import id.project.df.dble.core.model.ScanConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeScanConfigStore(initial: ScanConfig = ScanConfig()) : ScanConfigStore {
    private val state = MutableStateFlow(initial)
    override val config: StateFlow<ScanConfig> = state
    val saved = mutableListOf<ScanConfig>()

    override fun save(config: ScanConfig) {
        saved += config
        state.value = config
    }
}
