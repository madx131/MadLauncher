package com.madlauncher.data

data class QsTileSpec(
    val id: String,
    val label: String,
)

object QsCatalog {
    /** All catalog tiles; device filtering is done via [QsStateReader.availableOnDevice]. */
    val defaultIds: List<String> get() = all.map { it.id }

    val all = listOf(
        QsTileSpec("wifi", "Wi-Fi"),
        QsTileSpec("bt", "Bluetooth"),
        QsTileSpec("cell", "Mobile data"),
        QsTileSpec("share", "Quick Share"),
        QsTileSpec("dnd", "Do Not Disturb"),
        QsTileSpec("air", "Airplane"),
        QsTileSpec("torch", "Flashlight"),
        QsTileSpec("rotate", "Auto-rotate"),
        QsTileSpec("hotspot", "Hotspot"),
        QsTileSpec("location", "Location"),
        QsTileSpec("battery", "Battery Saver"),
        QsTileSpec("dark", "Dark theme"),
        QsTileSpec("night", "Night Light"),
        QsTileSpec("volume", "Volume"),
        QsTileSpec("brightness", "Brightness"),
        QsTileSpec("sound", "Sound"),
        QsTileSpec("display", "Display"),
        QsTileSpec("nfc", "NFC"),
        QsTileSpec("cast", "Cast"),
        QsTileSpec("vpn", "VPN"),
        QsTileSpec("data_saver", "Data Saver"),
        QsTileSpec("invert", "Invert colors"),
        QsTileSpec("bedtime", "Bedtime"),
        QsTileSpec("wallet", "Wallet"),
        QsTileSpec("settings", "Settings"),
    )

    fun spec(id: String): QsTileSpec? = all.firstOrNull { it.id == id }

    fun label(id: String): String = spec(id)?.label ?: id
}
