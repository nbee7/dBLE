package id.project.df.dble.core.data.scan

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import id.project.df.dble.core.model.ScanConfig
import id.project.df.dble.core.model.scanConfigFrom
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

interface ScanConfigStore {
    val config: StateFlow<ScanConfig>
    fun save(config: ScanConfig)
}

@Singleton
class SharedPrefsScanConfigStore @Inject constructor(
    @ApplicationContext context: Context,
) : ScanConfigStore {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(
        scanConfigFrom(prefs.getString(KEY_MODE, null), prefs.getBoolean(KEY_EXTENDED, false)),
    )

    override val config: StateFlow<ScanConfig> = state

    override fun save(config: ScanConfig) {
        prefs.edit()
            .putString(KEY_MODE, config.mode.name)
            .putBoolean(KEY_EXTENDED, config.extended)
            .apply()
        state.value = config
    }

    private companion object {
        const val PREFS_NAME = "scan_config"
        const val KEY_MODE = "mode"
        const val KEY_EXTENDED = "extended"
    }
}
