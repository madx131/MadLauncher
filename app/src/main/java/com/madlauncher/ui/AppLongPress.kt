package com.madlauncher.ui

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.sqrt
import kotlinx.coroutines.withTimeoutOrNull

private enum class PressOutcome { Tap, LongPress, Cancel }

/** Pickup delay — short enough to drag, long enough not to steal taps. Menu still opens on release. */
private const val IconPickupTimeoutMs = 180L

/**
 * Long-press / tap / drag that cooperates with scrollables (LazyColumn, HorizontalPager).
 * Moving past touch slop before the long-press timeout cancels without consuming, so the
 * parent list/pager can scroll — including from empty gaps and icon padding.
 */
fun Modifier.appLongPress(
    key: Any,
    coordinates: LayoutCoordinates?,
    onPositioned: (LayoutCoordinates) -> Unit,
    onTap: () -> Unit,
    onHoldStart: () -> Unit,
    onMove: (x: Float, y: Float, moved: Boolean) -> Unit,
    onHoldEnd: () -> Unit,
): Modifier = composed {
    // Keep a live coords handle inside pointerInput — the [coordinates] arg alone goes stale.
    var liveCoords by remember { mutableStateOf(coordinates) }
    Modifier
        .onGloballyPositioned {
            liveCoords = it
            onPositioned(it)
        }
        .pointerInput(key) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                val slop = viewConfiguration.touchSlop
                val pickupMs = minOf(viewConfiguration.longPressTimeoutMillis, IconPickupTimeoutMs)
                val outcome = withTimeoutOrNull(pickupMs) {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: return@withTimeoutOrNull PressOutcome.Cancel
                        if (change.isConsumed) return@withTimeoutOrNull PressOutcome.Cancel
                        if (!change.pressed) return@withTimeoutOrNull PressOutcome.Tap
                        val dist = hypot(
                            (change.position.x - down.position.x).toDouble(),
                            (change.position.y - down.position.y).toDouble(),
                        )
                        // Yield to scroll / page swipe — do not consume.
                        if (dist > slop) return@withTimeoutOrNull PressOutcome.Cancel
                    }
                    @Suppress("UNREACHABLE_CODE")
                    PressOutcome.Cancel
                } ?: PressOutcome.LongPress

                when (outcome) {
                    PressOutcome.Tap -> onTap()
                    PressOutcome.Cancel -> Unit
                    PressOutcome.LongPress -> {
                        var holdStarted = false
                        try {
                            currentEvent.changes.forEach { if (it.id == down.id) it.consume() }
                            onHoldStart()
                            holdStarted = true
                            val origin = down.position
                            var moved = false
                            fun windowPos(local: Offset): Offset {
                                val coords = liveCoords
                                return if (coords != null && coords.isAttached) {
                                    coords.localToWindow(local)
                                } else {
                                    local
                                }
                            }
                            onMove(windowPos(origin).x, windowPos(origin).y, false)
                            drag(down.id) { change ->
                                val win = windowPos(change.position)
                                if (!moved &&
                                    hypot(
                                        (change.position.x - origin.x).toDouble(),
                                        (change.position.y - origin.y).toDouble(),
                                    ) > slop
                                ) {
                                    moved = true
                                }
                                onMove(win.x, win.y, moved)
                                change.consume()
                            }
                        } finally {
                            if (holdStarted) onHoldEnd()
                        }
                    }
                }
            }
        }
}

/**
 * Wallpaper / empty-space long-press that opens the home context menu.
 * This is not a remappable gesture: it stays on even when landscape home
 * gestures are off, and it yields to icons/widgets via [shouldIgnore].
 */
fun Modifier.homePageContextMenu(
    enabled: Boolean,
    shouldIgnore: (windowX: Float, windowY: Float) -> Boolean = { _, _ -> false },
    onTap: () -> Unit = {},
    onLongPress: () -> Unit,
): Modifier = composed {
    val view = LocalView.current
    val enabledState = rememberUpdatedState(enabled)
    val ignoreState = rememberUpdatedState(shouldIgnore)
    val onTapState = rememberUpdatedState(onTap)
    val onLongPressState = rememberUpdatedState(onLongPress)
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    Modifier
        .onGloballyPositioned { coords = it }
        .pointerInput(Unit) {
            awaitEachGesture {
                if (!enabledState.value) {
                    awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                    while (true) {
                        val idle = awaitPointerEvent(PointerEventPass.Final)
                        if (idle.changes.none { it.pressed }) break
                    }
                    return@awaitEachGesture
                }
                val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                val win = coords?.takeIf { it.isAttached }?.localToWindow(down.position) ?: down.position
                if (ignoreState.value(win.x, win.y)) return@awaitEachGesture
                val slop = viewConfiguration.touchSlop
                val outcome = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id }
                            ?: return@withTimeoutOrNull PressOutcome.Cancel
                        if (change.isConsumed) return@withTimeoutOrNull PressOutcome.Cancel
                        if (!change.pressed) return@withTimeoutOrNull PressOutcome.Tap
                        val dist = hypot(
                            (change.position.x - down.position.x).toDouble(),
                            (change.position.y - down.position.y).toDouble(),
                        )
                        if (dist > slop) return@withTimeoutOrNull PressOutcome.Cancel
                    }
                    @Suppress("UNREACHABLE_CODE")
                    PressOutcome.Cancel
                } ?: PressOutcome.LongPress
                when (outcome) {
                    PressOutcome.Tap -> onTapState.value()
                    PressOutcome.Cancel -> Unit
                    PressOutcome.LongPress -> {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        onLongPressState.value()
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { change ->
                                if (change.pressed) change.consume()
                            }
                            if (event.changes.none { it.pressed }) break
                        }
                    }
                }
            }
        }
}

/**
 * Home-screen gestures: single-finger vertical swipes, two-finger swipes, and pinch.
 * Yields single-finger horizontal moves so [androidx.compose.foundation.pager.HorizontalPager] can page.
 */
fun Modifier.launcherHomeGestures(
    enabled: Boolean,
    allowVertical: Boolean = true,
    /** When true, vertical swipes are home gestures even if a child (Most-used list) would scroll. */
    stealVertical: Boolean = false,
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onTwoFingerUp: () -> Unit,
    onTwoFingerDown: () -> Unit,
    onPinchIn: () -> Unit,
    onPinchOut: () -> Unit,
): Modifier = composed {
    val enabledState = rememberUpdatedState(enabled)
    val allowVerticalState = rememberUpdatedState(allowVertical)
    val stealVerticalState = rememberUpdatedState(stealVertical)
    val onSwipeUpState = rememberUpdatedState(onSwipeUp)
    val onSwipeDownState = rememberUpdatedState(onSwipeDown)
    val onTwoFingerUpState = rememberUpdatedState(onTwoFingerUp)
    val onTwoFingerDownState = rememberUpdatedState(onTwoFingerDown)
    val onPinchInState = rememberUpdatedState(onPinchIn)
    val onPinchOutState = rememberUpdatedState(onPinchOut)
    Modifier.pointerInput(enabled, allowVertical, stealVertical) {
        val slop = viewConfiguration.touchSlop
        val swipeThresh = slop * 5f
        val pinchThresh = slop * 6f
        awaitEachGesture {
            if (!enabledState.value) {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                while (true) {
                    val idle = awaitPointerEvent(PointerEventPass.Final)
                    if (idle.changes.none { it.pressed }) break
                }
                return@awaitEachGesture
            }
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var totalX = 0f
            var totalY = 0f
            var mode = GestureMode.UNDECIDED
            var startSpan = 0f
            var lastSpan = 0f
            var twoFingerDy = 0f

            fun spanOf(changes: List<androidx.compose.ui.input.pointer.PointerInputChange>): Float {
                if (changes.size < 2) return 0f
                val a = changes[0].position
                val b = changes[1].position
                val dx = a.x - b.x
                val dy = a.y - b.y
                return sqrt(dx * dx + dy * dy)
            }

            while (true) {
                val initial = awaitPointerEvent(PointerEventPass.Initial)
                val pressed = initial.changes.filter { it.pressed }
                if (pressed.isEmpty()) break

                if (pressed.size >= 2) {
                    if (mode != GestureMode.MULTI) {
                        mode = GestureMode.MULTI
                        startSpan = spanOf(pressed)
                        lastSpan = startSpan
                        twoFingerDy = 0f
                    }
                    pressed.fastForEach { change ->
                        if (change.positionChanged()) change.consume()
                    }
                    val span = spanOf(pressed)
                    val avgDy = pressed.map { it.position.y - it.previousPosition.y }.average().toFloat()
                    twoFingerDy += avgDy
                    lastSpan = span
                    continue
                }

                if (mode == GestureMode.MULTI) break

                if (!allowVerticalState.value) {
                    // News feed owns one-finger vertical; keep watching for a second finger.
                    continue
                }

                val change = initial.changes.firstOrNull { it.pressed } ?: break
                val dx = change.position.x - change.previousPosition.x
                val dy = change.position.y - change.previousPosition.y
                when (mode) {
                    GestureMode.UNDECIDED -> {
                        totalX += dx
                        totalY += dy
                        if (hypot(totalX.toDouble(), totalY.toDouble()) > slop) {
                            if (abs(totalY) > abs(totalX) * 1.15f) {
                                mode = GestureMode.SINGLE_VERTICAL
                                change.consume()
                            } else {
                                // Horizontal page swipe — do not consume.
                                return@awaitEachGesture
                            }
                        }
                    }
                    GestureMode.SINGLE_VERTICAL -> {
                        totalY += dy
                        change.consume()
                    }
                    GestureMode.MULTI -> break
                }
                // Frequent list: after we lock vertical, keep stealing so the list
                // does not eat swipe-up / swipe-down. Horizontal already yielded above.
                if (stealVerticalState.value && mode == GestureMode.SINGLE_VERTICAL &&
                    change.positionChanged()
                ) {
                    change.consume()
                }
            }

            when (mode) {
                GestureMode.SINGLE_VERTICAL -> when {
                    totalY < -swipeThresh -> onSwipeUpState.value()
                    totalY > swipeThresh -> onSwipeDownState.value()
                }
                GestureMode.MULTI -> {
                    val spanDelta = lastSpan - startSpan
                    when {
                        spanDelta < -pinchThresh -> onPinchInState.value()
                        spanDelta > pinchThresh -> onPinchOutState.value()
                        twoFingerDy < -swipeThresh -> onTwoFingerUpState.value()
                        twoFingerDy > swipeThresh -> onTwoFingerDownState.value()
                    }
                }
                GestureMode.UNDECIDED -> Unit
            }
        }
    }
}

/**
 * Vertical swipe on the dock.
 * [onPullUp] runs as soon as the up-swipe crosses the threshold (so the extra row
 * can appear before finger-up). Return true if that swipe is fully handled.
 * Otherwise [onSwipeUp] / [onSwipeDown] run on release.
 */
fun Modifier.dockSwipeGestures(
    onPullUp: () -> Boolean = { false },
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
): Modifier = composed {
    val onPull = rememberUpdatedState(onPullUp)
    val onUp = rememberUpdatedState(onSwipeUp)
    val onDown = rememberUpdatedState(onSwipeDown)
    Modifier.pointerInput(Unit) {
        val slop = viewConfiguration.touchSlop
        val thresh = slop * 3.5f
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var totalX = 0f
            var totalY = 0f
            var vertical: Boolean? = null
            var pulled = false
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val pressed = event.changes.filter { it.pressed }
                if (pressed.isEmpty()) break
                val change = pressed.first()
                totalX += change.position.x - change.previousPosition.x
                totalY += change.position.y - change.previousPosition.y
                if (vertical == null && hypot(totalX.toDouble(), totalY.toDouble()) > slop) {
                    vertical = abs(totalY) > abs(totalX) * 1.15f
                    if (vertical == false) return@awaitEachGesture
                }
                if (vertical == true && change.positionChanged()) change.consume()
                if (vertical == true && !pulled && totalY < -thresh) {
                    if (onPull.value()) pulled = true
                }
            }
            if (vertical == true && !pulled) {
                when {
                    totalY < -thresh -> onUp.value()
                    totalY > thresh -> onDown.value()
                }
            }
        }
    }
}

private enum class GestureMode {
    UNDECIDED,
    SINGLE_VERTICAL,
    MULTI,
}

/** Visible drop tiles while dragging an icon onto the dock or a home page. */
fun Modifier.dropAimTile(
    aiming: Boolean,
    hovered: Boolean,
    color: Color,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp),
): Modifier {
    if (!aiming) return this
    return this
        .background(color.copy(alpha = if (hovered) 0.34f else 0.12f), shape)
        .border(
            width = if (hovered) 2.dp else 1.dp,
            color = color.copy(alpha = if (hovered) 0.95f else 0.48f),
            shape = shape,
        )
}
