package com.madlauncher.ui

import android.graphics.RectF
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.math.roundToInt
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import com.madlauncher.R
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import com.madlauncher.data.EdgeSide
import com.madlauncher.data.HomeItem
import com.madlauncher.data.HomeLayout
import com.madlauncher.data.HomePageKind
import com.madlauncher.data.LaunchableApp
import com.madlauncher.data.LauncherViewModel
import com.madlauncher.data.PageEffectId
import com.madlauncher.data.UnreadApp
import com.madlauncher.ui.theme.IconShapes
import com.madlauncher.ui.theme.LocalMadTheme
import com.madlauncher.ui.theme.MadTheme
import java.io.File

private const val INFINITE_PAGE_LOOP_COUNT = 512

private fun realHomePageIndex(virtualPage: Int, realCount: Int): Int {
    if (realCount <= 0) return 0
    return ((virtualPage % realCount) + realCount) % realCount
}

private fun virtualHomePageIndex(realIndex: Int, realCount: Int, loopAnchor: Int): Int {
    if (realCount <= 0) return 0
    val safe = realIndex.coerceIn(0, realCount - 1)
    return loopAnchor + safe
}

/**
 * Infinite paging composes the same real page at multiple virtual indexes
 * (beyondViewport). AppWidgetHost keeps one HostView per widget id — if two
 * pages both call createView, updates (e.g. YT Music track changes) land on
 * the off-screen copy. Only the canonical virtual page for each real page
 * should mount live app widgets.
 */
private fun isCanonicalHomeVirtualPage(virtualPage: Int, realCount: Int, focusPage: Int): Boolean {
    if (realCount <= 1) return true
    val focusReal = realHomePageIndex(focusPage, realCount)
    val base = focusPage - focusReal
    val real = realHomePageIndex(virtualPage, realCount)
    return virtualPage == base + real
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeDesktop(
    vm: LauncherViewModel,
    modifier: Modifier = Modifier,
) {
    val realCount = vm.homePages.size.coerceAtLeast(1)
    val editing = vm.homeEditMode
    val infiniteEnabled = vm.displayAppearance.infiniteHomePages && realCount > 1 && !editing
    val virtualCount = if (infiniteEnabled) realCount * INFINITE_PAGE_LOOP_COUNT else realCount
    val loopAnchor = if (infiniteEnabled) realCount * (INFINITE_PAGE_LOOP_COUNT / 2) else 0
    val pagerState = rememberPagerState(
        initialPage = if (infiniteEnabled) {
            virtualHomePageIndex(vm.homePageIndex, realCount, loopAnchor)
        } else {
            vm.homePageIndex.coerceIn(0, realCount - 1)
        },
        pageCount = { virtualCount },
    )
    val pageIdsKey = vm.homePages.joinToString(separator = ",") { it.id }
    // Keep pager on the VM page when dots / home / add-page change the index.
    LaunchedEffect(vm.homePageIndex, pageIdsKey, infiniteEnabled, loopAnchor, realCount) {
        val targetReal = vm.homePageIndex.coerceIn(0, (realCount - 1).coerceAtLeast(0))
        if (infiniteEnabled) {
            val currentReal = realHomePageIndex(pagerState.settledPage, realCount)
            if (currentReal != targetReal && !pagerState.isScrollInProgress) {
                val base = pagerState.settledPage - currentReal
                pagerState.scrollToPage(base + targetReal)
            }
        } else {
            if (pagerState.settledPage != targetReal && !pagerState.isScrollInProgress) {
                pagerState.scrollToPage(targetReal)
            }
        }
    }
    // Re-anchor when infinite wrap turns on/off or edit mode ends (page count shrinks).
    LaunchedEffect(infiniteEnabled, loopAnchor, realCount) {
        val targetReal = vm.homePageIndex.coerceIn(0, (realCount - 1).coerceAtLeast(0))
        if (infiniteEnabled) {
            val settled = pagerState.settledPage
            val inRange = settled in loopAnchor until loopAnchor + realCount
            if (!inRange) {
                pagerState.scrollToPage(loopAnchor + targetReal)
            }
        } else if (pagerState.settledPage != targetReal) {
            pagerState.scrollToPage(targetReal)
        }
    }
    // Authoritative one-page-per-screen index once the snap settles.
    LaunchedEffect(pagerState, infiniteEnabled, realCount) {
        snapshotFlow { pagerState.settledPage }.collect { virtualPage ->
            val safe = if (infiniteEnabled) {
                realHomePageIndex(virtualPage, realCount)
            } else {
                virtualPage.coerceIn(0, (realCount - 1).coerceAtLeast(0))
            }
            if (vm.homePageIndex != safe) {
                vm.homePageIndex = safe
                vm.persistLastHomePageIndex()
            }
        }
    }
    LaunchedEffect(vm.homePageIndex, vm.homePages.getOrNull(vm.homePageIndex)?.kind) {
        when (vm.homePages.getOrNull(vm.homePageIndex)?.kind) {
            HomePageKind.FREQUENT -> vm.refreshFrequentApps()
            HomePageKind.NEWS -> vm.refreshNews()
            else -> Unit
        }
    }
    val pageEffect = vm.displayAppearance.pageEffect
    Column(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            val sidePeek = (maxWidth * 0.10f).coerceIn(24.dp, 48.dp)
            val pageGutter = 8.dp
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = if (editing) {
                    PaddingValues(horizontal = sidePeek, vertical = 10.dp)
                } else {
                    PaddingValues(0.dp)
                },
                pageSpacing = if (editing) pageGutter else 0.dp,
                beyondViewportPageCount = 2,
                userScrollEnabled = !vm.homePagerLocked && vm.editingWidgetId == null,
                key = { virtualPage ->
                    val real = realHomePageIndex(virtualPage, realCount)
                    "${vm.homePages.getOrNull(real)?.id ?: real}-$virtualPage"
                },
            ) { virtualPage ->
                val realPage = realHomePageIndex(virtualPage, realCount)
                HomePagerPageSlot(
                    vm = vm,
                    pageIndex = realPage,
                    virtualPageIndex = virtualPage,
                    pagerState = pagerState,
                    pageEffect = pageEffect,
                    editing = editing,
                    pageGutter = pageGutter,
                )
            }
        }
        if (vm.homePages.size > 1 || editing) {
            val virtualIndicator = if (pagerState.isScrollInProgress) {
                pagerState.targetPage
            } else {
                pagerState.settledPage
            }
            val indicatorPage = if (infiniteEnabled) {
                realHomePageIndex(virtualIndicator, realCount)
            } else {
                virtualIndicator.coerceIn(0, (vm.homePages.size - 1).coerceAtLeast(0))
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (editing) 2.dp else 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                vm.homePages.forEachIndexed { i, page ->
                    val selected = i == indicatorPage
                    val color = when (page.kind) {
                        HomePageKind.FREQUENT -> if (selected) LocalMadTheme.current.colors.green else LocalMadTheme.current.colors.border
                        HomePageKind.NEWS -> if (selected) LocalMadTheme.current.colors.orange else LocalMadTheme.current.colors.border
                        HomePageKind.EMPTY -> if (selected) LocalMadTheme.current.colors.primary else LocalMadTheme.current.colors.border
                    }
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (selected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable {
                                vm.homePageIndex = i
                                vm.persistLastHomePageIndex()
                            },
                    )
                }
            }
        }
        if (editing && vm.homePages.size > 1) {
            Text(
                stringResource(R.string.hold_page_to_drag),
                color = LocalMadTheme.current.colors.muted,
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomePagerPageSlot(
    vm: LauncherViewModel,
    pageIndex: Int,
    virtualPageIndex: Int,
    pagerState: androidx.compose.foundation.pager.PagerState,
    pageEffect: PageEffectId,
    editing: Boolean,
    pageGutter: Dp,
) {
    val realCount = vm.homePages.size.coerceAtLeast(1)
    val infiniteEnabled = vm.displayAppearance.infiniteHomePages && realCount > 1 && !editing
    val hostLiveWidgets = !infiniteEnabled ||
        isCanonicalHomeVirtualPage(virtualPageIndex, realCount, pagerState.settledPage) ||
        isCanonicalHomeVirtualPage(virtualPageIndex, realCount, pagerState.currentPage)
    // Drop cells share a map keyed by real page — only the current virtual copy may write.
    val acceptDropRects = !infiniteEnabled ||
        isCanonicalHomeVirtualPage(virtualPageIndex, realCount, pagerState.currentPage)
    val pageContent: @Composable () -> Unit = {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalHostLiveAppWidgets provides hostLiveWidgets,
        ) {
            OverviewPage(
                vm = vm,
                pageIndex = pageIndex,
                editing = editing,
                pageGutter = pageGutter,
                acceptDropRects = acceptDropRects,
            )
        }
    }
    when {
        editing -> Box(Modifier.fillMaxSize()) { pageContent() }
        pageEffect == PageEffectId.BOOK -> {
            BookPagerPage(
                pageIndex = virtualPageIndex,
                pagerState = pagerState,
                modifier = Modifier.fillMaxSize(),
                content = pageContent,
            )
        }
        pageEffect == PageEffectId.STACK -> {
            StackPagerPage(
                pageIndex = virtualPageIndex,
                pagerState = pagerState,
                modifier = Modifier.fillMaxSize(),
                content = pageContent,
            )
        }
        pageEffect.toOverlapEffect() != null -> {
            OverlapPagerPage(
                effect = pageEffect.toOverlapEffect()!!,
                pageIndex = virtualPageIndex,
                pagerState = pagerState,
                modifier = Modifier.fillMaxSize(),
                content = pageContent,
            )
        }
        else -> {
            // Slide — default HorizontalPager motion only.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds(),
            ) {
                pageContent()
            }
        }
    }
}

@Composable
private fun OverviewPage(
    vm: LauncherViewModel,
    pageIndex: Int,
    editing: Boolean,
    pageGutter: Dp,
    acceptDropRects: Boolean,
) {
    val view = LocalView.current
    val density = LocalDensity.current
    var lifting by remember { mutableStateOf(false) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var edgeHint by remember { mutableStateOf(0) } // -1 left, 0 none, 1 right
    val scale by animateFloatAsState(
        targetValue = when {
            !editing -> 1f
            lifting -> 0.96f
            else -> 1f
        },
        label = "homePageScale",
    )
    val cardShape = RoundedCornerShape(28.dp)
    val theme = LocalMadTheme.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = dragX
                shadowElevation = when {
                    !editing -> 0f
                    lifting -> 28.dp.toPx()
                    else -> 16.dp.toPx()
                }
                clip = editing
                shape = cardShape
            }
            .then(
                if (editing) {
                    Modifier
                        .clip(cardShape)
                        .background(LocalMadTheme.current.colors.bg.copy(alpha = 0.42f))
                        .border(
                            width = if (edgeHint != 0) 2.dp else 1.dp,
                            color = when (edgeHint) {
                                -1, 1 -> theme.colors.primary
                                else -> LocalMadTheme.current.colors.border
                            },
                            shape = cardShape,
                        )
                } else Modifier,
            )
    ) {
        HomePagePane(
            vm = vm,
            pageIndex = pageIndex,
            editing = editing,
            acceptDropRects = acceptDropRects,
        )
        if (editing && edgeHint != 0) {
            Box(
                modifier = Modifier
                    .align(if (edgeHint > 0) Alignment.CenterEnd else Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(theme.colors.primary.copy(alpha = 0.55f)),
            )
        }
        if (editing) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .zIndex(12f)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(LocalMadTheme.current.colors.red.copy(alpha = if (vm.homePages.size > 1) 1f else 0.35f))
                    .clickable(enabled = vm.homePages.size > 1) {
                        vm.removeHomePageAt(pageIndex)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = stringResource(R.string.remove_page),
                    tint = LocalMadTheme.current.colors.fg,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        if (editing && vm.homePages.size > 1) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 44.dp)
                    .pointerInput(pageGutter) {
                        val gutterPx = with(density) { pageGutter.toPx() }
                        val minThresh = with(density) { 96.dp.toPx() }
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                            val longPress = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: return@withTimeoutOrNull false
                                    if (!change.pressed) return@withTimeoutOrNull false
                                    if ((change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                        return@withTimeoutOrNull false
                                    }
                                }
                                @Suppress("UNREACHABLE_CODE")
                                true
                            } == null
                            if (!longPress) return@awaitEachGesture
                            lifting = true
                            vm.lockHomePager()
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            var reverseArmed = 0
                            try {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                    if (!change.pressed) break
                                    val dx = change.positionChange().x
                                    change.consume()
                                    val pagePitch = size.width + gutterPx
                                    val upperThresh = (size.width * 0.55f).coerceAtLeast(minThresh)
                                    val swapThreshold = (pagePitch * 0.45f).coerceIn(minThresh, upperThresh)
                                    val dead = swapThreshold * 0.25f
                                    dragX = (dragX + dx).coerceIn(-pagePitch, pagePitch)
                                    edgeHint = when {
                                        dragX > swapThreshold * 0.7f && vm.canMoveHomePage(1) -> 1
                                        dragX < -swapThreshold * 0.7f && vm.canMoveHomePage(-1) -> -1
                                        else -> 0
                                    }
                                    when {
                                        reverseArmed > 0 && dragX < dead -> reverseArmed = 0
                                        reverseArmed < 0 && dragX > -dead -> reverseArmed = 0
                                        reverseArmed == 0 && dragX > swapThreshold && vm.canMoveHomePage(1) -> {
                                            vm.moveHomePage(1)
                                            dragX -= swapThreshold
                                            reverseArmed = 1
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        }
                                        reverseArmed == 0 && dragX < -swapThreshold && vm.canMoveHomePage(-1) -> {
                                            vm.moveHomePage(-1)
                                            dragX += swapThreshold
                                            reverseArmed = -1
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        }
                                    }
                                }
                            } finally {
                                lifting = false
                                dragX = 0f
                                edgeHint = 0
                                vm.unlockHomePagerIfIdle()
                            }
                        }
                    },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomePagePane(
    vm: LauncherViewModel,
    pageIndex: Int,
    editing: Boolean,
    acceptDropRects: Boolean,
) {
    val page = vm.homePages.getOrNull(pageIndex) ?: return
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        when (page.kind) {
            HomePageKind.FREQUENT -> FrequentAppsPage(vm, page, Modifier.fillMaxSize())
            HomePageKind.NEWS -> NewsPage(vm, page, Modifier.fillMaxSize())
            HomePageKind.EMPTY -> {
                DisposableEffect(pageIndex, acceptDropRects) {
                    onDispose {
                        if (acceptDropRects) vm.clearHomeDropRectsForPage(pageIndex)
                    }
                }
                Spacer(Modifier.height(if (pageIndex == 0) 28.dp else 36.dp))
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) {
                    val grid = vm.gridSizeFor(pageIndex).coerceIn(HomeLayout.MIN_GRID, HomeLayout.MAX_GRID)
                    val cellW = maxWidth / grid
                    val cellH = maxHeight / grid
                    val iconSize = emptyPageIconSize(vm.displayAppearance.homeIconSizeDp, cellW, cellH)
                    val gridH = cellH * grid
                    val gridScroll = rememberScrollState()
                    LaunchedEffect(pageIndex, grid) {
                        vm.pruneHomeDropRects(pageIndex, grid)
                    }
                    val movingId = vm.widgetMovePreview?.first
                    val moveDest = vm.widgetMovePreview?.second
                    val swapPartnerId = vm.widgetSwapPartnerId
                    val draggedItem = movingId?.let { id -> page.items.find { it.id == id } }
                    val aimingTiles = vm.appHold != null && vm.dragMoved && vm.widgetMovePreview == null
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .homePageContextMenu(
                                enabled = !editing && vm.editingWidgetId == null && vm.appHold == null,
                                shouldIgnore = { x, y -> vm.touchHitsHomeItem(x, y, pageIndex) },
                                onTap = {
                                    if (vm.editingWidgetId != null) {
                                        vm.exitWidgetEdit()
                                        vm.collapseEdge()
                                    }
                                },
                                onLongPress = { vm.enterHomeEdit() },
                            )
                            .verticalScroll(
                                state = gridScroll,
                                enabled = gridH > maxHeight + 0.5.dp &&
                                    vm.editingWidgetId == null &&
                                    vm.widgetResizePreview == null,
                            ),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(gridH),
                        ) {
                            for (row in 0 until grid) {
                                for (col in 0 until grid) {
                                    val key = "$pageIndex:$col,$row"
                                    val occupied = page.items.any { it.occupies(col, row) }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(start = cellW * col, top = cellH * row)
                                            .size(cellW, cellH)
                                            .then(
                                                if (acceptDropRects) {
                                                    Modifier.onGloballyPositioned { coords ->
                                                        val b = coords.boundsInWindow()
                                                        vm.setHomeDropRect(
                                                            pageIndex,
                                                            col,
                                                            row,
                                                            grid,
                                                            RectF(b.left, b.top, b.right, b.bottom),
                                                        )
                                                    }
                                                } else {
                                                    Modifier
                                                },
                                            )
                                            .then(
                                                if (!occupied && !editing && vm.editingWidgetId != null) {
                                                    Modifier.clickable {
                                                        vm.exitWidgetEdit()
                                                        vm.collapseEdge()
                                                    }
                                                } else {
                                                    Modifier
                                                },
                                            )
                                            .then(
                                                if (aimingTiles) {
                                                    val hovered = vm.dragDropKey == key
                                                    Modifier
                                                        .padding(4.dp)
                                                        .dropAimTile(true, hovered, LocalMadTheme.current.colors.primary)
                                                } else {
                                                    Modifier
                                                },
                                            ),
                                    )
                                }
                            }
                            if (moveDest != null && draggedItem != null &&
                                (moveDest.first != draggedItem.col || moveDest.second != draggedItem.row)
                            ) {
                                WidgetMoveFootprint(
                                    cellW = cellW,
                                    cellH = cellH,
                                    col = moveDest.first,
                                    row = moveDest.second,
                                    spanX = draggedItem.spanX,
                                    spanY = draggedItem.spanY,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .zIndex(2f)
                                        .padding(start = cellW * moveDest.first, top = cellH * moveDest.second)
                                        .size(cellW * draggedItem.spanX, cellH * draggedItem.spanY),
                                )
                            }
                            swapPartnerId?.let { partnerId ->
                                val partner = page.items.find { it.id == partnerId } ?: return@let
                                val origin = draggedItem ?: return@let
                                WidgetMoveFootprint(
                                    cellW = cellW,
                                    cellH = cellH,
                                    col = origin.col,
                                    row = origin.row,
                                    spanX = partner.spanX,
                                    spanY = partner.spanY,
                                    accent = LocalMadTheme.current.colors.muted,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .zIndex(2f)
                                        .padding(start = cellW * origin.col, top = cellH * origin.row)
                                        .size(cellW * partner.spanX, cellH * partner.spanY),
                                )
                            }
                                page.items.forEach { item ->
                                    val preview = vm.widgetResizePreview?.takeIf { it.first == item.id }?.second
                                    val move = vm.widgetMovePreview?.takeIf { it.first == item.id }?.second
                                    val spanX = preview?.first ?: item.spanX
                                    val spanY = preview?.second ?: item.spanY
                                    val col = item.col
                                    val row = item.row
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .zIndex(if (vm.editingWidgetId == item.id || move != null) 6f else 0f)
                                            .padding(start = cellW * col, top = cellH * row)
                                            .size(cellW * spanX, cellH * spanY)
                                            .padding(
                                                if (item.isAppWidget || item.widget in setOf("glance", "clock", "date", "weather", "notes")) {
                                                    0.dp
                                                } else {
                                                    4.dp
                                                },
                                            )
                                            .onGloballyPositioned { coords ->
                                                val b = coords.boundsInWindow()
                                                vm.setHomeItemHitRect(
                                                    pageIndex,
                                                    item.id,
                                                    android.graphics.RectF(b.left, b.top, b.right, b.bottom),
                                                )
                                            },
                                    ) {
                                        if (item.isApp) {
                                            val app = vm.resolveApp(item.packageName, item.activityName)
                                            if (app != null) {
                                                HomeAppCell(vm, app, item, iconSize, pageIndex)
                                            }
                                        } else if (item.isShortcut) {
                                            HomeShortcutCell(vm, item, iconSize, pageIndex)
                                        } else if (item.isFolder) {
                                            HomeFolderCell(vm, item, iconSize, pageIndex)
                                        } else if (item.isAppWidget) {
                                            WidgetCell(vm, item, cellW, cellH, grid)
                                        } else {
                                            WidgetCell(vm, item, cellW, cellH, grid)
                                        }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Settings → Icons slider, as drawn on home / Most used / drawer. */
fun homeIconSize(sliderDp: Float): Dp = sliderDp.dp.coerceIn(40.dp, 88.dp)

/** Minimum column width so [iconSize] fits without horizontal scrolling. */
fun iconGridMinCell(iconSize: Dp): Dp = iconSize + 8.dp

/** How many icons of [iconSize] fit in [availableWidth]; extras wrap to the next row. */
fun columnsForIconSize(availableWidth: Dp, iconSize: Dp): Int {
    val cell = iconGridMinCell(iconSize)
    if (cell.value <= 0f || availableWidth.value <= 0f) return 1
    return (availableWidth.value / cell.value).toInt().coerceAtLeast(1)
}

/** Same icon size, column count, and cell size as Most-used / the app drawer. */
data class HomeIconGridSpec(
    val iconSize: Dp,
    val columns: Int,
    val cellW: Dp,
    val cellH: Dp,
)

fun homeIconGridSpec(availableWidth: Dp, iconSizeDp: Float): HomeIconGridSpec {
    val iconSize = homeIconSize(iconSizeDp)
    val columns = columnsForIconSize(availableWidth, iconSize)
    return HomeIconGridSpec(
        iconSize = iconSize,
        columns = columns,
        cellW = availableWidth / columns,
        cellH = iconSize + 22.dp,
    )
}

fun emptyPageIconSize(sliderDp: Float, cellW: Dp, cellH: Dp): Dp {
    val wanted = homeIconSize(sliderDp)
    val maxW = (cellW - 8.dp).coerceAtLeast(1.dp)
    val maxH = (cellH - 28.dp).coerceAtLeast(1.dp)
    return minOf(wanted, maxW, maxH)
}

/**
 * Icon size inside a grid cell. [sliderDp] is Settings → Icons (homeIconSizeDp).
 * Uses the slider as-is; only shrinks if the cell is narrower than the icon
 * (height must not shrink landscape icons — grow the row instead).
 */
fun iconLabelSize(sp: Float): androidx.compose.ui.unit.TextUnit = sp.coerceIn(9f, 16f).sp

/** Largest dock icon that still leaves the label visible under it. */
fun maxDockIconDp(dockHeightDp: Float, labelSp: Float): Float {
    val text = labelSp.coerceIn(9f, 16f) * 1.35f
    return (dockHeightDp - text - 6f).coerceIn(33f, 72f)
}

fun iconSizeForCell(sliderDp: Float, cellW: Dp, @Suppress("UNUSED_PARAMETER") cellH: Dp): Dp {
    val wanted = homeIconSize(sliderDp)
    val maxW = (cellW - 4.dp).coerceAtLeast(1.dp)
    return minOf(wanted, maxW)
}

@Composable
private fun HomeAppCell(vm: LauncherViewModel, app: LaunchableApp, item: HomeItem, iconSize: Dp, pageIndex: Int) {
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val held = vm.appHold?.itemId == item.id
    Column(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = if (held) 0f else 1f }
            .appLongPress(
                key = item.id,
                coordinates = coords,
                onPositioned = { coords = it },
                onTap = { if (!vm.homeEditMode) vm.launchApp(app) },
                onHoldStart = {
                    if (app.packageName in HomeLayout.protectedDock) {
                        vm.openEditDocksFromControl()
                    } else {
                        vm.beginAppHold(app, "home", itemId = item.id, fromPageIndex = pageIndex)
                    }
                },
                onMove = { x, y, moved ->
                    if (app.packageName !in HomeLayout.protectedDock) {
                        vm.updateHoldPointer(x, y, moved)
                    }
                },
                onHoldEnd = {
                    if (app.packageName !in HomeLayout.protectedDock) vm.endAppHold()
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .keepUpright(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            DesktopIcon(vm, app, iconSize, rotateUpright = false)
            Text(
                app.label,
                color = LocalMadTheme.current.colors.fg,
                fontSize = iconLabelSize(vm.displayAppearance.iconLabelSizeSp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun HomeShortcutCell(vm: LauncherViewModel, item: HomeItem, iconSize: Dp, pageIndex: Int) {
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val dummy = LaunchableApp(item.title.ifBlank { "Shortcut" }, item.packageName, item.id, null)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .appLongPress(
                key = item.id,
                coordinates = coords,
                onPositioned = { coords = it },
                onTap = { if (!vm.homeEditMode) vm.launchShortcut(item) },
                onHoldStart = { vm.beginAppHold(dummy, "home", itemId = item.id, fromPageIndex = pageIndex) },
                onMove = { x, y, moved -> vm.updateHoldPointer(x, y, moved) },
                onHoldEnd = { vm.endAppHold() },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier.keepUpright(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val file = item.iconPath.takeIf { it.isNotBlank() }?.let { java.io.File(it) }
            Box(modifier = Modifier.size(iconSize)) {
                if (file != null && file.exists()) {
                    val bmp = remember(item.iconPath) { android.graphics.BitmapFactory.decodeFile(item.iconPath) }
                    if (bmp != null) {
                        androidx.compose.foundation.Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = item.title,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    } else {
                        AppIcon(null, item.title, size = iconSize)
                    }
                } else {
                    AppIcon(null, item.title, size = iconSize)
                }
            }
            Text(
                item.title.ifBlank { "Shortcut" },
                color = LocalMadTheme.current.colors.fg,
                fontSize = iconLabelSize(vm.displayAppearance.iconLabelSizeSp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HomeFolderCell(vm: LauncherViewModel, item: HomeItem, iconSize: Dp, pageIndex: Int) {
    var coords by remember { mutableStateOf<LayoutCoordinates?>(null) }
    val dummy = LaunchableApp(item.title.ifBlank { "Folder" }, "folder", item.id, null)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .appLongPress(
                key = item.id,
                coordinates = coords,
                onPositioned = { coords = it },
                onTap = { if (!vm.homeEditMode) vm.openFolder(item.id) },
                onHoldStart = { vm.beginAppHold(dummy, "home", itemId = item.id, fromPageIndex = pageIndex) },
                onMove = { x, y, moved -> vm.updateHoldPointer(x, y, moved) },
                onHoldEnd = { vm.endAppHold() },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier.keepUpright(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FolderPreview(vm, item, iconSize, rotateUpright = false)
            Text(
                item.title.ifBlank { "Folder" },
                color = LocalMadTheme.current.colors.fg,
                fontSize = iconLabelSize(vm.displayAppearance.iconLabelSizeSp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun FolderPreview(vm: LauncherViewModel, item: HomeItem, size: Dp, rotateUpright: Boolean = true) {
    val theme = LocalMadTheme.current
    val view = vm.displayAppearance.folderView
    val apps = item.folderApps.take(4).mapNotNull { vm.resolveApp(it.packageName, it.activityName) }
    val folderUnread = item.folderApps.sumOf { vm.unreadBadgeCount(it.packageName) }
    Box(modifier = Modifier.size(size).then(if (rotateUpright) Modifier.keepUpright() else Modifier)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(theme.colors.card.copy(alpha = 0.9f))
                .border(1.dp, theme.colors.border, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            when (view) {
                com.madlauncher.data.FolderViewId.GRID -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            apps.getOrNull(0)?.let { DesktopIcon(vm, it, size * 0.38f, showBadge = false, rotateUpright = false) }
                            apps.getOrNull(1)?.let { DesktopIcon(vm, it, size * 0.38f, showBadge = false, rotateUpright = false) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            apps.getOrNull(2)?.let { DesktopIcon(vm, it, size * 0.38f, showBadge = false, rotateUpright = false) }
                            apps.getOrNull(3)?.let { DesktopIcon(vm, it, size * 0.38f, showBadge = false, rotateUpright = false) }
                        }
                    }
                }
                com.madlauncher.data.FolderViewId.STACK -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        apps.asReversed().forEachIndexed { i, app ->
                            Box(modifier = Modifier.offset(x = (i * 3).dp, y = (i * 2).dp)) {
                                DesktopIcon(vm, app, size * 0.62f, showBadge = false, rotateUpright = false)
                            }
                        }
                    }
                }
                com.madlauncher.data.FolderViewId.BADGE -> {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Icon(
                            Icons.Outlined.Folder,
                            contentDescription = null,
                            tint = theme.colors.primary,
                            modifier = Modifier.size(size * 0.55f),
                        )
                        Text(
                            "${item.folderApps.size}",
                            color = theme.colors.bg,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(theme.colors.primary)
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
            }
        }
        NotificationIconBadge(
            count = folderUnread,
            style = vm.displayAppearance.iconBadgeStyle,
            modifier = Modifier.align(Alignment.TopEnd),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OpenFolderOverlay(vm: LauncherViewModel, modifier: Modifier = Modifier) {
    val item = vm.folderItem() ?: return
    val theme = LocalMadTheme.current
    var editingTitle by remember(item.id) { mutableStateOf(false) }
    var titleDraft by remember(item.id) { mutableStateOf(item.title.ifBlank { "Folder" }) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable { vm.closeFolder() },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .clip(RoundedCornerShape(22.dp))
                .background(theme.colors.card.copy(alpha = 0.97f))
                .clickable(enabled = false) {}
                .padding(16.dp),
        ) {
            if (editingTitle) {
                androidx.compose.foundation.text.BasicTextField(
                    value = titleDraft,
                    onValueChange = { titleDraft = it },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = theme.colors.fg,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(theme.colors.bg)
                                .padding(10.dp),
                        ) { inner() }
                    },
                )
                Text(
                    "Done",
                    color = theme.colors.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.End)
                        .clickable {
                            vm.renameFolder(item.id, titleDraft)
                            editingTitle = false
                        }
                        .padding(bottom = 8.dp),
                )
            } else {
                Text(
                    item.title.ifBlank { "Folder" },
                    color = theme.colors.fg,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clickable { editingTitle = true }
                        .padding(bottom = 12.dp),
                )
            }
            val folderIconSize = homeIconSize(vm.displayAppearance.homeIconSizeDp)
            androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(
                    minSize = iconGridMinCell(folderIconSize),
                ),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
            ) {
                items(item.folderApps.size) { idx ->
                    val member = item.folderApps[idx]
                    val app = vm.resolveApp(member.packageName, member.activityName) ?: return@items
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val iconSize = iconSizeForCell(vm.displayAppearance.homeIconSizeDp, maxWidth, maxWidth)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .keepUpright()
                                .clip(RoundedCornerShape(12.dp))
                                .combinedClickable(
                                    onClick = { vm.launchFolderApp(member) },
                                    onLongClick = { vm.removeAppFromFolder(item.id, member) },
                                )
                                .padding(4.dp),
                        ) {
                            DesktopIcon(vm, app, iconSize, rotateUpright = false)
                            Text(
                                app.label,
                                color = theme.colors.fg,
                                fontSize = iconLabelSize(vm.displayAppearance.iconLabelSizeSp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
            Text(
                "Long-press an app to place it on the page · Tap title to rename",
                color = theme.colors.muted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

@Composable
private fun WidgetMoveFootprint(
    cellW: Dp,
    cellH: Dp,
    col: Int,
    row: Int,
    spanX: Float,
    spanY: Float,
    modifier: Modifier = Modifier,
    accent: Color = LocalMadTheme.current.colors.primary,
) {
    val tileCols = kotlin.math.ceil(spanX.toDouble()).toInt().coerceAtLeast(1)
    val tileRows = kotlin.math.ceil(spanY.toDouble()).toInt().coerceAtLeast(1)
    Box(
        modifier = modifier
            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
            .border(2.dp, accent.copy(alpha = 0.65f), RoundedCornerShape(8.dp)),
    ) {
        for (r in 0 until tileRows) {
            for (c in 0 until tileCols) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = cellW * c, top = cellH * r)
                        .size(cellW, cellH)
                        .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(4.dp)),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WidgetCell(vm: LauncherViewModel, item: HomeItem, cellW: Dp, cellH: Dp, grid: Int) {
    val theme = LocalMadTheme.current
    var previewSpanX by remember(item.id) { mutableFloatStateOf(item.spanX) }
    var previewSpanY by remember(item.id) { mutableFloatStateOf(item.spanY) }
    var moveOffsetX by remember(item.id) { mutableFloatStateOf(0f) }
    var moveOffsetY by remember(item.id) { mutableFloatStateOf(0f) }
    androidx.compose.runtime.LaunchedEffect(item.spanX, item.spanY) {
        previewSpanX = item.spanX
        previewSpanY = item.spanY
    }
    androidx.compose.runtime.LaunchedEffect(item.col, item.row, vm.widgetMovePreview?.first) {
        if (vm.widgetMovePreview?.first != item.id) {
            moveOffsetX = 0f
            moveOffsetY = 0f
        }
    }
    val glance = item.widget == "glance" || item.widget in setOf("clock", "date", "weather")
    val multi = item.widget == "multi"
    val madWidget = glance || multi
    val label = when {
        glance -> vm.clock.ifBlank { "MAD" }
        item.widget == "notes" -> vm.floatingNote?.let { it.title.ifBlank { it.text } } ?: "Notes"
        else -> item.widget.ifBlank { "Widget" }
    }
    val editing = vm.editingWidgetId == item.id
    val moving = vm.widgetMovePreview?.first == item.id
    val view = LocalView.current
    val density = LocalDensity.current
    val maxSpanX = (grid - item.col).toFloat()
    val maxSpanY = (grid - item.row).toFloat()
    fun applyPreview(nextX: Float, nextY: Float) {
        val x = nextX.coerceIn(1f, maxSpanX.coerceAtLeast(1f))
        val y = nextY.coerceIn(1f, maxSpanY.coerceAtLeast(1f))
        previewSpanX = x
        previewSpanY = y
        vm.widgetResizePreview = item.id to (x to y)
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = moveOffsetX
                translationY = moveOffsetY
                scaleX = if (moving) 1.03f else 1f
                scaleY = if (moving) 1.03f else 1f
                shadowElevation = if (moving) 22.dp.toPx() else 0f
            }
            .then(
                // Glance handles its own taps; a parent pointerInput would steal clock/date/weather hits.
                if (editing || item.isAppWidget || madWidget) Modifier else Modifier.pointerInput(item.id) {
                    detectTapGestures(
                        onLongPress = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            vm.enterWidgetEdit(item.id)
                        },
                        onTap = {
                            if (vm.editingWidgetId != null) {
                                vm.enterWidgetEdit(item.id)
                            }
                        },
                    )
                },
            )
            // App-widget edge margin → home customize (not widget edit). Glance is one movable unit.
            .then(
                if (!editing && item.isAppWidget) {
                    Modifier.pointerInput(item.id) {
                        awaitEachGesture {
                            val down = awaitFirstDown(
                                requireUnconsumed = false,
                                pass = PointerEventPass.Initial,
                            )
                            val edge = minOf(size.width, size.height) * 0.14f
                            val inEdge = down.position.x < edge ||
                                down.position.x > size.width - edge ||
                                down.position.y < edge ||
                                down.position.y > size.height - edge
                            if (!inEdge) return@awaitEachGesture
                            val slop = viewConfiguration.touchSlop
                            val longPress = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == down.id }
                                        ?: return@withTimeoutOrNull false
                                    if (!change.pressed) return@withTimeoutOrNull false
                                    if ((change.position - down.position).getDistance() > slop) {
                                        return@withTimeoutOrNull false
                                    }
                                    change.consume()
                                }
                                @Suppress("UNREACHABLE_CODE")
                                true
                            } == null
                            if (longPress && vm.editingWidgetId == null) {
                                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                vm.enterHomeEdit()
                            }
                        }
                    }
                } else {
                    Modifier
                },
            )
            .then(
                if (editing) Modifier.border(2.dp, LocalMadTheme.current.colors.primary, RoundedCornerShape(16.dp))
                else if (item.isAppWidget) Modifier // flush to widget content — no card chrome
                else Modifier.clip(RoundedCornerShape(16.dp)),
            )
            .then(
                if (item.isAppWidget) Modifier
                else if (multi) {
                    Modifier.background(multiPanelColor(vm.multiBoardFor(item.id), theme.colors.card))
                } else if (glance) {
                    Modifier.background(glancePanelColor(vm.glanceSettings, theme.colors.card))
                } else {
                    Modifier.background(theme.colors.card.copy(alpha = theme.widgetAlpha))
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        KeepUprightInBounds(
            modifier = Modifier
                .fillMaxSize()
                .then(if (item.isAppWidget || madWidget) Modifier else Modifier.padding(10.dp)),
            enabled = item.isAppWidget && appWidgetRotatesInPlace(previewSpanX, previewSpanY),
        ) {
            when {
                item.isAppWidget -> {
                    HostedAppWidget(
                        appWidgetId = item.appWidgetId,
                        onLongPress = { vm.enterWidgetEdit(item.id) },
                        interceptGestures = !editing,
                        blockTouches = editing,
                        widthDp = cellW * previewSpanX,
                        heightDp = cellH * previewSpanY,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                multi -> {
                    MadMultiWidget(
                        vm = vm,
                        itemId = item.id,
                        clicksEnabled = !editing && !vm.homeEditMode && vm.appHold == null,
                        onLongPress = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            vm.enterWidgetEdit(item.id)
                        },
                        modifier = Modifier.fillMaxSize().keepUprightFill(),
                    )
                }
                glance -> {
                    MadGlanceWidget(
                        vm = vm,
                        clicksEnabled = !editing && !vm.homeEditMode && vm.appHold == null,
                        onLongPress = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            vm.enterWidgetEdit(item.id)
                        },
                        modifier = Modifier.fillMaxSize().keepUprightFill(),
                    )
                }
                else -> {
                    Text(
                        label,
                        color = LocalMadTheme.current.colors.fg,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.keepUpright(),
                    )
                }
            }
        }
        if (editing) {
            // Full-body drag to reposition; edge/corner handles sit above for resize.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 22.dp, start = 8.dp, end = 22.dp, bottom = 30.dp)
                    .zIndex(1f)
                    .pointerInput(item.id, item.col, item.row, cellW, cellH) {
                        val cellWPx = with(density) { cellW.toPx() }.coerceAtLeast(1f)
                        val cellHPx = with(density) { cellH.toPx() }.coerceAtLeast(1f)
                        detectDragGestures(
                            onDragStart = {
                                moveOffsetX = 0f
                                moveOffsetY = 0f
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                vm.lockHomePager()
                                vm.previewWidgetMove(item.id, item.col, item.row)
                            },
                            onDragEnd = {
                                vm.commitWidgetMove(item.id)
                                moveOffsetX = 0f
                                moveOffsetY = 0f
                            },
                            onDragCancel = {
                                vm.cancelWidgetMove()
                                moveOffsetX = 0f
                                moveOffsetY = 0f
                            },
                            onDrag = { change, drag ->
                                change.consume()
                                moveOffsetX += drag.x
                                moveOffsetY += drag.y
                                val destCol = kotlin.math.round(item.col + moveOffsetX / cellWPx).toInt()
                                val destRow = kotlin.math.round(item.row + moveOffsetY / cellHPx).toInt()
                                vm.previewWidgetMove(item.id, destCol, destRow)
                            },
                        )
                    },
            )
            if (madWidget) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = LocalMadTheme.current.colors.fg,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .zIndex(5f)
                        .padding(4.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(LocalMadTheme.current.colors.primary.copy(alpha = 0.85f))
                        .clickable {
                            val route = if (multi) {
                                com.madlauncher.ui.settings.SettingsRoute.MULTI
                            } else {
                                com.madlauncher.ui.settings.SettingsRoute.GLANCE
                            }
                            vm.exitWidgetEdit()
                            vm.openHomeSettings()
                            vm.openSettingsRoute(route)
                        }
                        .padding(8.dp),
                )
            }
            Text(
                "×",
                color = LocalMadTheme.current.colors.fg,
                fontSize = 13.sp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .zIndex(4f)
                    .clip(CircleShape)
                    .background(LocalMadTheme.current.colors.red)
                    .clickable { vm.removeDesktopItem(item.id) }
                    .padding(horizontal = 6.dp, vertical = 1.dp),
            )
            WidgetResizeHandle(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(20.dp)
                    .background(LocalMadTheme.current.colors.primary.copy(alpha = 0.35f))
                    .zIndex(3f),
                itemId = item.id,
                cellW = cellW,
                cellH = cellH,
                horizontal = true,
                vertical = false,
                currentX = { previewSpanX },
                currentY = { previewSpanY },
                onPreview = ::applyPreview,
                onDragStart = { vm.lockHomePager() },
                onCommit = { x, y ->
                    vm.widgetResizePreview = null
                    vm.resizeWidget(item.id, x, y)
                },
            )
            WidgetResizeHandle(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(28.dp)
                    .background(LocalMadTheme.current.colors.primary.copy(alpha = 0.35f))
                    .zIndex(3f),
                itemId = item.id,
                cellW = cellW,
                cellH = cellH,
                horizontal = false,
                vertical = true,
                currentX = { previewSpanX },
                currentY = { previewSpanY },
                onPreview = ::applyPreview,
                onDragStart = { vm.lockHomePager() },
                onCommit = { x, y ->
                    vm.widgetResizePreview = null
                    vm.resizeWidget(item.id, x, y)
                },
            )
            WidgetResizeHandle(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .clip(RoundedCornerShape(topStart = 8.dp, bottomEnd = 12.dp))
                    .background(LocalMadTheme.current.colors.primary)
                    .zIndex(4f),
                itemId = item.id,
                cellW = cellW,
                cellH = cellH,
                horizontal = true,
                vertical = true,
                currentX = { previewSpanX },
                currentY = { previewSpanY },
                onPreview = ::applyPreview,
                onDragStart = { vm.lockHomePager() },
                onCommit = { x, y ->
                    vm.widgetResizePreview = null
                    vm.resizeWidget(item.id, x, y)
                },
            )
            Text(
                "${kotlin.math.round(previewSpanX).toInt()}×${kotlin.math.round(previewSpanY).toInt()}",
                color = LocalMadTheme.current.colors.primary,
                fontSize = 9.sp,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 4.dp, bottom = 4.dp)
                    .zIndex(2f),
            )
        }
    }
}

@Composable
private fun WidgetResizeHandle(
    modifier: Modifier,
    itemId: String,
    cellW: Dp,
    cellH: Dp,
    horizontal: Boolean,
    vertical: Boolean,
    currentX: () -> Float,
    currentY: () -> Float,
    onPreview: (Float, Float) -> Unit,
    onCommit: (Float, Float) -> Unit,
    onDragStart: () -> Unit = {},
) {
    val density = LocalDensity.current
    Box(
        modifier = modifier.pointerInput(itemId, cellW, cellH, horizontal, vertical) {
            val cellWPx = with(density) { cellW.toPx() }.coerceAtLeast(1f)
            val cellHPx = with(density) { cellH.toPx() }.coerceAtLeast(1f)
            awaitEachGesture {
                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial,
                )
                down.consume()
                onDragStart()
                val startX = currentX()
                val startY = currentY()
                var accX = 0f
                var accY = 0f
                try {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        val delta = change.positionChange()
                        change.consume()
                        if (!change.pressed) break
                        accX += delta.x
                        accY += delta.y
                        val nextX = if (horizontal) spanFromDrag(startX, accX, cellWPx) else startX
                        val nextY = if (vertical) spanFromDrag(startY, accY, cellHPx) else startY
                        onPreview(nextX, nextY)
                    }
                } finally {
                    onCommit(currentX(), currentY())
                }
            }
        },
    )
}

/** Follow the finger in cell units. Whole tiles are applied when the drag ends. */
internal fun spanFromDrag(start: Float, accPx: Float, cellPx: Float): Float {
    return start + accPx / cellPx.coerceAtLeast(1f)
}

@Composable
fun DesktopIcon(
    vm: LauncherViewModel,
    app: LaunchableApp,
    size: androidx.compose.ui.unit.Dp,
    showBadge: Boolean = true,
    rotateUpright: Boolean = true,
) {
    val theme = LocalMadTheme.current
    val shapeId = theme.iconShape
    val shape = MadTheme.iconClip(shapeId, theme.iconCornerDp)
    val density = LocalDensity.current
    val custom = vm.customIconPath(app.packageName)
    val file = custom?.let { File(it) }
    Box(modifier = Modifier.size(size).then(if (rotateUpright) Modifier.keepUpright() else Modifier)) {
        if (file != null && file.exists()) {
            val bgAlpha = theme.iconBgAlpha
            val fillOnly = IconShapes.shapesFillOnly(shapeId)
            val silhouette = IconShapes.usesAdaptiveInset(shapeId)
            val bmp = remember(custom, shapeId, theme.iconCornerDp, bgAlpha) {
                val raw = android.graphics.BitmapFactory.decodeFile(custom) ?: return@remember null
                if (fillOnly) maskIconFill(raw, shape, density, bgAlpha, IconShapes.logoSlot(shapeId)) else raw
            }
            if (bmp != null) {
                androidx.compose.foundation.Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = app.label,
                    contentScale = if (silhouette) {
                        androidx.compose.ui.layout.ContentScale.Fit
                    } else {
                        androidx.compose.ui.layout.ContentScale.Crop
                    },
                    modifier = Modifier.fillMaxSize().then(if (silhouette) Modifier else Modifier.clip(shape)),
                )
            } else {
                AppIcon(null, app.label, size = size)
            }
        } else {
            val packIcon = remember(app.id, vm.displayAppearance.iconPackPackage, shapeId) {
                vm.resolvePackIcon(app.packageName, app.activityName)
            }
            AppIcon(
                packIcon ?: app.icon,
                app.label,
                size = size,
                packageName = if (packIcon == null) app.packageName else null,
                activityName = if (packIcon == null) app.activityName else null,
            )
        }
        if (showBadge) {
            val count = vm.unreadBadgeCount(app.packageName)
            NotificationIconBadge(
                count = count,
                style = vm.displayAppearance.iconBadgeStyle,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

@Composable
private fun UnreadIconRow(
    unread: List<UnreadApp>,
    clicksEnabled: Boolean,
    onClick: () -> Unit,
    iconSize: Dp = 28.dp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.then(if (clicksEnabled) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        unread.take(6).forEach { u ->
            Box(
                modifier = Modifier
                    .size(iconSize)
                    .clip(CircleShape)
                    .background(LocalMadTheme.current.colors.primary.copy(alpha = 0.18f))
                    .border(1.dp, LocalMadTheme.current.colors.primary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(u.appName.take(1), color = LocalMadTheme.current.colors.primary, fontSize = (iconSize.value * 0.42f).sp, fontWeight = FontWeight.Bold)
                if (u.count > 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size((iconSize.value * 0.43f).dp)
                            .clip(CircleShape)
                            .background(LocalMadTheme.current.colors.red),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            if (u.count > 9) "9+" else u.count.toString(),
                            color = LocalMadTheme.current.colors.fg,
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeBlock(
    greeting: String,
    clock: String,
    date: String,
    weather: String,
    unread: List<UnreadApp>,
    onUnreadClick: () -> Unit,
    onClockClick: () -> Unit,
    onDateClick: () -> Unit,
    onWeatherClick: () -> Unit,
    clicksEnabled: Boolean = true,
    onLongEmpty: () -> Unit = {},
    blockModifier: Modifier = Modifier,
) {
    Column(
        modifier = blockModifier.combinedClickable(
            onClick = {},
            onLongClick = onLongEmpty,
        ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            greeting.uppercase(),
            color = LocalMadTheme.current.colors.muted,
            fontSize = 16.sp,
            letterSpacing = 1.4.sp,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = clock.ifBlank { "--:--" },
            color = LocalMadTheme.current.colors.fg,
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 68.sp,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .then(if (clicksEnabled) Modifier.clickable(onClick = onClockClick) else Modifier)
                .focusProperties { canFocus = false }
                .padding(horizontal = 8.dp),
        )
        Text(
            date,
            color = LocalMadTheme.current.colors.primary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .then(if (clicksEnabled) Modifier.clickable(onClick = onDateClick) else Modifier)
                .focusProperties { canFocus = false }
                .padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Text(
            weather,
            color = LocalMadTheme.current.colors.muted,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .then(if (clicksEnabled) Modifier.clickable(onClick = onWeatherClick) else Modifier)
                .focusProperties { canFocus = false }
                .padding(horizontal = 6.dp, vertical = 4.dp),
        )
        if (unread.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            UnreadIconRow(
                unread = unread,
                clicksEnabled = clicksEnabled,
                onClick = onUnreadClick,
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
fun HomeCustomizeSheet(
    vm: LauncherViewModel,
    onPickWallpaper: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var addPageMenuOpen by remember { mutableStateOf(false) }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(LocalMadTheme.current.colors.card.copy(alpha = 0.97f))
            .border(1.dp, LocalMadTheme.current.colors.border, RoundedCornerShape(22.dp))
            .padding(top = 8.dp, bottom = 4.dp, start = 8.dp, end = 8.dp)
            .width(280.dp)
            .heightIn(max = 280.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (addPageMenuOpen) {
                SheetRow(stringResource(R.string.back_arrow)) { addPageMenuOpen = false }
                Text(
                    stringResource(R.string.add_new_page),
                    color = LocalMadTheme.current.colors.muted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
                SheetRow(HomePageKind.EMPTY.label) { vm.addHomePage(HomePageKind.EMPTY) }
                SheetRow(HomePageKind.FREQUENT.label) { vm.addHomePage(HomePageKind.FREQUENT) }
                SheetRow(HomePageKind.NEWS.label) { vm.addHomePage(HomePageKind.NEWS) }
            } else {
                SheetRow(stringResource(R.string.wallpaper), onPickWallpaper)
                SheetRow(stringResource(R.string.live_wallpaper)) { vm.openLiveWallpapers() }
                SheetRow(stringResource(R.string.widgets)) { vm.openWidgetPicker() }
                SheetRow(stringResource(R.string.add_sticky_note)) { vm.createStickyAndEdit() }
                SheetRow(stringResource(R.string.add_new_page_arrow)) { addPageMenuOpen = true }
                SheetRow(stringResource(R.string.remove_page)) { vm.removeHomePage() }
                val current = vm.homePages.getOrNull(vm.homePageIndex)
                if (current?.kind == HomePageKind.EMPTY) {
                    val gridSizes = HomeLayout.PAGE_GRID_SIZES
                    val activeGrid = vm.gridSizeFor(current)
                    Text(
                        stringResource(R.string.settings_section_page_grid),
                        color = LocalMadTheme.current.colors.muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        gridSizes.forEach { size ->
                            val selected = activeGrid == size
                            Text(
                                "${size}×$size",
                                color = if (selected) LocalMadTheme.current.colors.fg else LocalMadTheme.current.colors.muted,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) LocalMadTheme.current.colors.primary.copy(alpha = 0.28f) else LocalMadTheme.current.colors.card)
                                    .clickable { vm.setCurrentPageGridSize(size) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
                if (current?.kind == HomePageKind.FREQUENT) {
                    SheetRow(if (current.frequentSortTop) stringResource(R.string.most_used_top) else stringResource(R.string.most_used_bottom)) {
                        vm.toggleFrequentSort()
                    }
                }
                if (current?.kind == HomePageKind.NEWS) {
                    SheetRow(stringResource(R.string.news_sources_label)) { vm.openNewsPicker() }
                }
                SheetRow(if (vm.hasEdgePanel(EdgeSide.LEFT)) stringResource(R.string.remove_left_dock) else stringResource(R.string.add_left_dock)) {
                    vm.toggleEdgePanel(EdgeSide.LEFT)
                }
                SheetRow(if (vm.hasEdgePanel(EdgeSide.RIGHT)) stringResource(R.string.remove_right_dock) else stringResource(R.string.add_right_dock)) {
                    vm.toggleEdgePanel(EdgeSide.RIGHT)
                }
                SheetRow(if (vm.hasEdgePanel(EdgeSide.TOP)) stringResource(R.string.remove_top_dock) else stringResource(R.string.add_top_dock)) {
                    vm.toggleEdgePanel(EdgeSide.TOP)
                }
                val pageId = current?.id
                if (pageId != null) {
                    SheetRow(if (pageId == vm.storeHomePageId()) "Home page (current)" else "Set as home page") {
                        vm.setAsHomePage(pageId)
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.help_system_settings_label),
                color = LocalMadTheme.current.colors.fg,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LocalMadTheme.current.colors.secondary)
                    .clickable { vm.openSystemSettings() }
                    .padding(horizontal = 8.dp, vertical = 14.dp),
            )
            Text(
                stringResource(R.string.launcher_settings),
                color = LocalMadTheme.current.colors.bg,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(LocalMadTheme.current.colors.primary)
                    .clickable { vm.openHomeSettings() }
                    .padding(horizontal = 8.dp, vertical = 14.dp),
            )
        }
    }
}

@Composable
private fun SheetRow(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = LocalMadTheme.current.colors.fg,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    )
}

@Composable
fun AppContextMenu(
    vm: LauncherViewModel,
    onEditIcon: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val menu = vm.appMenu ?: return
    val theme = LocalMadTheme.current
    val c = theme.colors
    val special = menu.app.packageName in HomeLayout.specialDock
    val likeDrawer = menu.from == "drawer" || menu.from == "actionbox"
    val canRemove = menu.from != "frequent" &&
        !(menu.from == "dock" && menu.dockIndex != null &&
        vm.dockSlotAt(menu.dockIndex) in HomeLayout.protectedDock)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(c.card.copy(alpha = 0.97f))
            .border(1.dp, c.border, RoundedCornerShape(18.dp))
            .padding(10.dp)
            .width(240.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(menu.app.label, color = c.fg, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        if (menu.app.packageName == "actions") {
            MenuRow(stringResource(R.string.menu_disable)) { vm.disableActionBoxOnDock() }
            MenuRow(stringResource(R.string.cancel)) { vm.appMenu = null }
            return
        }
        if (menu.app.packageName == "folder" && menu.itemId != null) {
            MenuRow(stringResource(R.string.menu_open)) { vm.openFolder(menu.itemId); vm.appMenu = null }
            MenuRow(stringResource(R.string.remove)) { vm.removeFromMenu() }
            MenuRow(stringResource(R.string.cancel)) { vm.appMenu = null }
            return
        }
        if (canRemove) {
            MenuRow(if (likeDrawer) stringResource(R.string.hide_from_drawer) else stringResource(R.string.remove)) { vm.removeFromMenu() }
        }
        if (!special) {
            if (likeDrawer) {
                MenuRow(stringResource(R.string.menu_add_to_dock)) { vm.requestAddToDock(menu.app) }
            }
            MenuRow(stringResource(R.string.menu_notifications)) {
                vm.openAppNotifications(menu.app.packageName)
            }
            MenuRow(stringResource(R.string.edit_icon), onClick = onEditIcon)
            MenuRow(stringResource(R.string.app_settings)) { vm.openMenuAppSettings() }
            MenuRow(stringResource(R.string.uninstall), isDestructive = true) { vm.uninstallFromMenu() }
        }
        MenuRow(stringResource(R.string.cancel)) { vm.appMenu = null }
    }
}

@Composable
private fun MenuRow(label: String, isDestructive: Boolean = false, onClick: () -> Unit) {
    val c = LocalMadTheme.current.colors
    Text(
        label,
        color = if (isDestructive) c.red else c.fg,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    )
}

@Composable
fun DragGhost(vm: LauncherViewModel, root: LayoutCoordinates?, modifier: Modifier = Modifier) {
    val hold = vm.appHold ?: return
    val density = LocalDensity.current
    val iconSize = vm.displayAppearance.homeIconSizeDp.dp.coerceIn(36.dp, 88.dp)
    val halfPx = with(density) { (iconSize / 2).toPx() }
    val local = root?.takeIf { it.isAttached }?.windowToLocal(Offset(vm.dragX, vm.dragY))
        ?: Offset(vm.dragX, vm.dragY)
    Box(
        modifier = modifier
            .zIndex(80f)
            .offset { IntOffset((local.x - halfPx).roundToInt(), (local.y - halfPx).roundToInt()) }
            .graphicsLayer {
                scaleX = if (vm.dragMoved) 1.08f else 1.04f
                scaleY = if (vm.dragMoved) 1.08f else 1.04f
                shadowElevation = 18.dp.toPx()
                alpha = 0.95f
            },
    ) {
        val source = hold.itemId?.let { id ->
            vm.homePages.flatMap { it.items }.firstOrNull { it.id == id }
        }
        val shortcutIcon = source?.takeIf { it.isShortcut }?.iconPath
            ?.takeIf { it.isNotBlank() }
            ?.let { path ->
                remember(path) {
                    runCatching { android.graphics.BitmapFactory.decodeFile(path) }.getOrNull()
                }
            }
        if (shortcutIcon != null) {
            androidx.compose.foundation.Image(
                bitmap = shortcutIcon.asImageBitmap(),
                contentDescription = source?.title,
                modifier = Modifier
                    .size(iconSize)
                    .clip(RoundedCornerShape(14.dp)),
            )
        } else {
            DesktopIcon(vm, hold.app, iconSize)
        }
    }
}
