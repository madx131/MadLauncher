package com.madlauncher.data

enum class NavShortcutPlacement {
    NAV_LEFT,
    NAV_RIGHT,
    SHADE,
    OFF,
    ;

    val isNavBar: Boolean get() = this == NAV_LEFT || this == NAV_RIGHT

    val isActive: Boolean get() = this != OFF
}

/**
 * "Game of chairs" — up to two shortcuts on the nav bar; overflow sits in the status shade.
 * Moving into an occupied seat swaps occupants.
 */
object NavShortcutPlacements {
    private val chairOrder = listOf(
        NavShortcutPlacement.NAV_LEFT,
        NavShortcutPlacement.NAV_RIGHT,
        NavShortcutPlacement.SHADE,
    )

    fun toMap(slots: List<NavShortcutSlot>): Map<NavShortcutKind, NavShortcutPlacement> {
        val map = NavShortcutKind.entries.associateWith { NavShortcutPlacement.OFF }.toMutableMap()
        slots.forEach { map[it.kind] = it.placement }
        return map
    }

    fun fromMap(map: Map<NavShortcutKind, NavShortcutPlacement>): List<NavShortcutSlot> =
        map.entries
            .filter { it.value.isActive }
            .map { (kind, placement) -> NavShortcutSlot(kind, placement) }

    fun placementOf(slots: List<NavShortcutSlot>, kind: NavShortcutKind): NavShortcutPlacement =
        slots.firstOrNull { it.kind == kind }?.placement ?: NavShortcutPlacement.OFF

    fun navSlots(slots: List<NavShortcutSlot>): List<NavShortcutSlot> =
        slots.filter { it.placement.isNavBar }

    fun shadeSlots(slots: List<NavShortcutSlot>): List<NavShortcutSlot> =
        slots.filter { it.placement == NavShortcutPlacement.SHADE }

    fun setEnabled(slots: List<NavShortcutSlot>, kind: NavShortcutKind, enabled: Boolean): List<NavShortcutSlot> {
        if (!enabled) {
            val map = toMap(slots).toMutableMap()
            map[kind] = NavShortcutPlacement.OFF
            return normalize(fromMap(map))
        }
        if (placementOf(slots, kind).isActive) return normalize(slots)
        val map = toMap(slots).toMutableMap()
        val seat = chairOrder.firstOrNull { chair ->
            map.none { (k, p) -> k != kind && p == chair }
        } ?: NavShortcutPlacement.SHADE
        return normalize(occupyChair(map, kind, seat))
    }

    /** Cycle: Nav left → Nav right → Status shade → Off. Swaps when the next seat is taken. */
    fun cycleChair(slots: List<NavShortcutSlot>, kind: NavShortcutKind): List<NavShortcutSlot> {
        val map = toMap(slots).toMutableMap()
        val current = map[kind] ?: NavShortcutPlacement.OFF
        if (!current.isActive) return normalize(setEnabled(slots, kind, true))
        val idx = chairOrder.indexOf(current)
        if (idx < 0 || idx >= chairOrder.lastIndex) {
            map[kind] = NavShortcutPlacement.OFF
            return normalize(fromMap(map))
        }
        return normalize(occupyChair(map, kind, chairOrder[idx + 1]))
    }

    fun normalize(slots: List<NavShortcutSlot>): List<NavShortcutSlot> {
        val map = toMap(slots).toMutableMap()
        chairOrder.forEach { chair ->
            val occupants = map.filter { it.value == chair }.keys.toList()
            if (occupants.size <= 1) return@forEach
            occupants.drop(1).forEach { bumped ->
                val fallback = chairOrder.firstOrNull { seat ->
                    seat != chair && map.none { (k, p) -> k != bumped && p == seat }
                } ?: NavShortcutPlacement.OFF
                map[bumped] = fallback
            }
        }
        val navCount = map.count { it.value.isNavBar }
        if (navCount > 2) {
            map.filter { it.value.isNavBar }
                .keys
                .drop(2)
                .forEach { kind ->
                    val fallback = chairOrder.firstOrNull { seat ->
                        map.none { (k, p) -> k != kind && p == seat }
                    } ?: NavShortcutPlacement.OFF
                    map[kind] = fallback
                }
        }
        return fromMap(map)
    }

    private fun occupyChair(
        map: MutableMap<NavShortcutKind, NavShortcutPlacement>,
        kind: NavShortcutKind,
        seat: NavShortcutPlacement,
    ): List<NavShortcutSlot> {
        if (!seat.isActive) {
            map[kind] = NavShortcutPlacement.OFF
            return fromMap(map)
        }
        val previous = map[kind] ?: NavShortcutPlacement.OFF
        val occupant = map.entries.firstOrNull { it.key != kind && it.value == seat }?.key
        map[kind] = seat
        if (occupant != null) {
            map[occupant] = when {
                previous.isActive -> previous
                else -> {
                    chairOrder.firstOrNull { chair ->
                        map.none { (k, p) -> k != occupant && p == chair }
                    } ?: NavShortcutPlacement.OFF
                }
            }
        }
        return fromMap(map)
    }
}
