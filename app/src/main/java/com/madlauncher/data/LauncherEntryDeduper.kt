package com.madlauncher.data

/**
 * Microsoft Copilot ships two LAUNCHER aliases (Default + Alt icon). HyperOS
 * [android.content.pm.LauncherApps] still lists the disabled one. Microsoft 365
 * (`officehubrow`) and Bing also advertise the drawer name "Copilot". Keep the
 * dedicated Copilot package and drop the stand-ins.
 */
object LauncherEntryDeduper {
    const val COPILOT_PACKAGE = "com.microsoft.copilot"
    const val BING_PACKAGE = "com.microsoft.bing"
    const val OFFICE_HUB_PACKAGE = "com.microsoft.office.officehubrow"

    data class Entry(
        val label: String,
        val packageName: String,
        val activityName: String,
    )

    fun collapse(
        entries: List<Entry>,
        preferredActivity: (packageName: String) -> String? = { null },
    ): List<Entry> {
        val byPackage = entries.groupBy { it.packageName }
        val collapsed = byPackage.values.flatMap { group ->
            group.groupBy { normalizeLabel(it.label) }.values.map { sameLabel ->
                pickPreferred(sameLabel, preferredActivity(sameLabel.first().packageName))
            }
        }
        val copilotNamed = collapsed.filter { normalizeLabel(it.label).contains("copilot") }
        if (copilotNamed.size <= 1) return collapsed
        val keepPackage = copilotNamed.firstOrNull { it.packageName == COPILOT_PACKAGE }?.packageName
            ?: copilotNamed.first().packageName
        return collapsed.filterNot { entry ->
            normalizeLabel(entry.label).contains("copilot") && entry.packageName != keepPackage
        }
    }

    internal fun normalizeLabel(label: String): String =
        label.lowercase().replace("microsoft", "").replace(Regex("[^a-z0-9]"), "")

    private fun pickPreferred(sameLabel: List<Entry>, preferredClass: String?): Entry {
        if (sameLabel.size == 1) return sameLabel.first()
        preferredClass?.let { want ->
            sameLabel.firstOrNull { it.activityName == want }?.let { return it }
        }
        sameLabel.firstOrNull { it.activityName.endsWith("MainActivityDefault") }?.let { return it }
        return sameLabel.first()
    }
}
