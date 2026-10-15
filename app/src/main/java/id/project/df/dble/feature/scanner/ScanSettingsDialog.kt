package id.project.df.dble.feature.scanner

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import id.project.df.dble.core.model.ScanConfig
import id.project.df.dble.core.model.ScanMode
import id.project.df.dble.ui.theme.DBLETheme

@Composable
fun ScanSettingsDialog(
    config: ScanConfig,
    onApply: (ScanConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    // Pilihan ditahan lokal sampai "Terapkan": scan berjalan baru di-restart saat disetujui.
    var draft by remember(config) { mutableStateOf(config) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Pengaturan pemindaian") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Mode pemindaian", style = MaterialTheme.typography.titleSmall)
                Column(modifier = Modifier.selectableGroup()) {
                    ScanMode.entries.forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = draft.mode == mode,
                                    onClick = { draft = draft.copy(mode = mode) },
                                    role = Role.RadioButton,
                                )
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = draft.mode == mode, onClick = null)
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    mode.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = draft.extended,
                            onValueChange = { draft = draft.copy(extended = it) },
                            role = Role.Switch,
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Iklan extended (Bluetooth 5)", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Ikut menangkap iklan BLE 5. Iklan biasa tetap terbaca.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = draft.extended, onCheckedChange = null)
                }
                Text(
                    "Berlaku juga untuk layar Pelacakan. Bila pemindaian sedang berjalan, akan dimulai ulang.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onApply(draft) }) { Text("Terapkan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Preview(name = "Light", showBackground = true)
@Composable
private fun ScanSettingsDialogLightPreview() {
    DBLETheme(darkTheme = false) {
        ScanSettingsDialog(config = ScanConfig(), onApply = {}, onDismiss = {})
    }
}

@Preview(name = "Dark", showBackground = true)
@Composable
private fun ScanSettingsDialogDarkPreview() {
    DBLETheme(darkTheme = true) {
        ScanSettingsDialog(config = ScanConfig(ScanMode.HEMAT, extended = true), onApply = {}, onDismiss = {})
    }
}
