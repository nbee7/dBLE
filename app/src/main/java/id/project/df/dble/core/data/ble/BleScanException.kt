package id.project.df.dble.core.data.ble

class BleScanException(val code: Int) : Exception("BLE scan failed, code=$code")
