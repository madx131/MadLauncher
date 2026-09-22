package com.madlauncher.data

import android.content.Context
import kotlinx.coroutines.delay

/**
 * Toggles mirrored QS tiles. HyperOS often reports click-tile success without a readable
 * state change — so a successful status-bar click counts as success.
 */
object QsToggleEngine {
    suspend fun toggle(
        ctx: Context,
        store: LocalStore,
        id: String,
        spec: String,
        beforeEnabled: Boolean,
        isCustom: Boolean,
        onCustomOptimisticFlip: () -> Unit,
        localFirst: Boolean = false,
    ): Boolean {
        if (SystemQsMirror.isActionTile(spec)) {
            return SystemQsMirror.clickTile(ctx, spec)
        }

        if (isCustom) {
            if (!SystemQsMirror.clickTile(ctx, spec)) return false
            onCustomOptimisticFlip()
            delay(300)
            return true
        }

        if (localFirst && QsTileActions.hasLocalToggle(id, spec)) {
            if (QsTileActions.toggle(ctx, id, spec)) {
                delay(320)
                store.qsToggleStrategy(spec, "local")
                return true
            }
        }

        if (SystemQsMirror.clickTile(ctx, spec)) {
            delay(380)
            store.qsToggleStrategy(spec, "click")
            return true
        }

        if (!localFirst && QsTileActions.hasLocalToggle(id, spec) && QsTileActions.toggle(ctx, id, spec)) {
            delay(320)
            store.qsToggleStrategy(spec, "local")
            return true
        }

        return false
    }
}
