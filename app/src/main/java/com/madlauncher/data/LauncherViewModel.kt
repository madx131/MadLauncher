package com.madlauncher.data

import android.app.Application
import android.app.Notification
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.madlauncher.R
import com.madlauncher.AppearanceBridge
import com.madlauncher.AppsReloadBus
import com.madlauncher.LauncherCommands
import com.madlauncher.MadApp
import com.madlauncher.SetupWindowPolicy
import com.madlauncher.data.LanguageOverlay
import com.madlauncher.NavShortcutBridge
import com.madlauncher.action.InstantAnswer
import com.madlauncher.action.InstantAnswerClient
import com.madlauncher.action.InstantModules
import com.madlauncher.action.OnDeviceTranslateUi
import com.madlauncher.action.OnDeviceTranslator
import com.madlauncher.action.AppFuzzy
import com.madlauncher.action.ActionAppKind
import com.madlauncher.action.ActionApps
import com.madlauncher.action.AliasTarget
import com.madlauncher.action.CallResult
import com.madlauncher.action.PostResult
import com.madlauncher.action.ActionCommand
import com.madlauncher.action.CommandAliases
import com.madlauncher.action.UserShortcut
import com.madlauncher.action.UserShortcutCombo
import com.madlauncher.action.UserShortcutContact
import com.madlauncher.action.UserShortcutKind
import com.madlauncher.action.UserShortcuts
import com.madlauncher.action.RecipientPicker
import com.madlauncher.action.ParsedTranslate
import com.madlauncher.action.TranslateCommandParser
import com.madlauncher.action.TranslateLangSlot
import com.madlauncher.action.TranslatorLanguage
import com.madlauncher.action.TranslatorLanguages
import com.madlauncher.action.CommandDetector
import com.madlauncher.action.CalcParser
import com.madlauncher.action.ContactInsertParser
import com.madlauncher.action.DefaultCommandApps
import com.madlauncher.action.InstalledActionApp
import com.madlauncher.action.ResolvedSearchApp
import com.madlauncher.action.SearchInAppResult
import com.madlauncher.action.SearchableApps
import com.madlauncher.action.SendMessageResult
import com.madlauncher.notif.LiveNotif
import com.madlauncher.notif.MessagingAppRegistry
import com.madlauncher.notif.NotifClassifier
import com.madlauncher.notif.SmsInboxMonitor
import com.madlauncher.notif.NotificationHub
import com.madlauncher.notif.SystemBadgeHub
import com.madlauncher.notif.SystemShadeController
import com.madlauncher.platform.FeatureStatus
import com.madlauncher.platform.OemFamily
import com.madlauncher.platform.CompatibilityReport
import com.madlauncher.platform.DeviceOsInfo
import com.madlauncher.platform.FeedbackSubmitter
import com.madlauncher.platform.PlatformBridge
import com.madlauncher.platform.PlatformDetector
import com.madlauncher.platform.PlatformFeature
import com.madlauncher.platform.PlatformProber
import com.madlauncher.platform.PlatformProfile
import com.madlauncher.ui.settings.CommandEditId
import com.madlauncher.ui.settings.SettingsBackStack
import com.madlauncher.ui.settings.SettingsRoute
import com.madlauncher.ui.WindowGeom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ToggleItem(
    val id: String,
    val label: String,
    val enabled: Boolean,
    /** Raw `sysui_qs_tiles` token used for status-bar click/sync. */
    val systemSpec: String = id,
)

data class UnreadApp(
    val packageName: String,
    val appName: String,
    val count: Int,
)

data class ThreadLine(
    val text: String,
    val outgoing: Boolean,
)

enum class NotifPane { Messages, System }

data class CommandAppEditorRow(
    val app: InstalledActionApp,
    val systemDefault: Boolean,
    val preferred: Boolean,
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MadApp
    private val appsRepo get() = app.appsRepo
    private val contactsRepo get() = app.contacts
    private val store get() = app.store
    private val actionApps get() = app.actionApps
    private val intents get() = app.intents
    private val widgetHost get() = app.widgetHost
    private val widgetManager get() = AppWidgetManager.getInstance(app)
    private val usageTracker = AppUsageTracker(application, store)

    var apps by mutableStateOf<List<LaunchableApp>>(emptyList())
        private set
    /** Apps that support in-app search (`:yt`, etc.); refreshed with [reloadLaunchableApps]. */
    var searchableApps by mutableStateOf<List<ResolvedSearchApp>>(emptyList())
        private set
    var loadingApps by mutableStateOf(true)
        private set
    val recents = mutableStateListOf<LaunchableApp>()
    var drawerRecent by mutableStateOf<List<AppUsageTracker.DrawerRecentApp>>(emptyList())
        private set
    private var recentsRefreshJob: Job? = null
    private var recentsQueryJob: Job? = null
    private val recentsIngestListener: (String) -> Unit = { pkg ->
        usageTracker.touchUsed(pkg)
        recentsRefreshJob?.cancel()
        recentsRefreshJob = viewModelScope.launch {
            bumpRecent(pkg)
            delay(250)
            refreshSystemRecents()
        }
    }

    var clock by mutableStateOf("")
        private set
    var date by mutableStateOf("")
        private set
    var greeting by mutableStateOf("Good morning")
        private set
    var weatherLabel by mutableStateOf("Weather")
        private set
    var grants by mutableStateOf(GrantState())
        private set
    var platformProfile by mutableStateOf(loadCachedPlatformProfile())
        private set
    var platformProbing by mutableStateOf(false)
        private set
    var deviceOsInfo by mutableStateOf(DeviceOsInfo.capture(getApplication()))
        private set
    var compatibilityReportSubmitted by mutableStateOf(store.compatibilityReportSubmitted)
        private set
    var persistentApps by mutableStateOf<List<PersistentApp>>(emptyList())
        private set
    var setupOpen by mutableStateOf(false)
    private var setupOpenedByUser = false

    var notificationsOpen by mutableStateOf(false)
        private set
    /** Internal split of the single notifications window (full shade copy). */
    var notifPane by mutableStateOf(NotifPane.Messages)
        private set
    var systemNotifDropdownOpen by mutableStateOf(false)
    var quickSettingsOpen by mutableStateOf(false)
        private set
    var appDrawerOpen by mutableStateOf(false)
    var actionBoxOpen by mutableStateOf(false)
        private set
    var eatActionEnter by mutableStateOf(false)
    var recentsOpen by mutableStateOf(false)
        private set
    var helpOpen by mutableStateOf(false)
    var commandPickerOpen by mutableStateOf(false)
    var barrelRollGen by mutableStateOf(0)
    var settingsRoute by mutableStateOf<SettingsRoute?>(null)
    var commandEditId by mutableStateOf<CommandEditId?>(null)
        private set
    var appearanceSettings by mutableStateOf(store.appearanceSettings)
        private set
    var appearanceDraft by mutableStateOf(store.appearanceSettings)
    var appearanceEpoch by mutableStateOf(0)
    var iconPacks by mutableStateOf<List<com.madlauncher.data.IconPackResolver.PackInfo>>(emptyList())
        private set
    var wallpaperPalette by mutableStateOf<WallpaperColorExtractor.WallpaperPalette?>(null)
        private set

    val settingsOpen: Boolean get() = settingsRoute != null

    val displayAppearance: AppearanceSettings
        get() = appearanceSettings

    val useMadShadeWindows: Boolean
        get() = appearanceSettings.experimentalMadWindows

    val useHelpWindow: Boolean
        get() = appearanceSettings.experimentalHelpWindow

    val useNavShortcuts: Boolean
        get() = appearanceSettings.experimentalNavShortcuts

    val useMadRecents: Boolean
        get() = appearanceSettings.experimentalMadRecents

    val useWidgetBadges: Boolean
        get() = appearanceSettings.experimentalWidgetBadges

    var toast by mutableStateOf<String?>(null)
        private set
    private var toastJob: Job? = null

    var liveNotifs by mutableStateOf<List<LiveNotif>>(emptyList())
        private set
    var listenerConnected by mutableStateOf(false)
        private set
    var binderFilter by mutableStateOf<String?>(null)
    val readIds = mutableStateListOf<String>()
    val dismissedIds = mutableStateListOf<String>()
    val floatingConvs = mutableStateListOf<FloatingConv>()
    private val outgoingByConversation = mutableStateMapOf<String, List<String>>()

    private fun syncAtPackages() {
        runCatching { MessagingAppRegistry.refresh(getApplication(), actionApps) }
    }
    private fun persistDismissedNotifs() {
        // Shade is source of truth — do not persist dismiss ghosts across process death.
        store.dismissedNotifKeys = emptySet()
        store.dismissedConversations = emptySet()
    }
    var actionQuery by mutableStateOf("")
    var composeApp by mutableStateOf<InstalledActionApp?>(null)
        private set
    var composeKind by mutableStateOf<ActionAppKind?>(null)
        private set
    var composeContact by mutableStateOf<ContactEntry?>(null)
        private set
    var searchScope by mutableStateOf<ResolvedSearchApp?>(null)
        private set
    var contacts by mutableStateOf<List<ContactEntry>>(emptyList())
        private set
    var contactsSyncing by mutableStateOf(false)
        private set
    var contactsAllowed by mutableStateOf(false)
        private set
    var callContactQuery by mutableStateOf("")
        private set
    /** Name/number filter for @ messaging — kept after app pick clears [actionQuery]. */
    var recipientQuery by mutableStateOf("")
        private set
    /** `&` contact-first flow active (survives clearing the `&` prefix from the field). */
    var contactLookupActive by mutableStateOf(false)
        private set
    var translateFromCode by mutableStateOf<String?>(null)
        private set
    var translateToCode by mutableStateOf<String?>(null)
        private set
    var instantAnswer by mutableStateOf<InstantAnswer?>(null)
        private set
    var onDeviceTranslate by mutableStateOf<OnDeviceTranslateUi>(OnDeviceTranslateUi.Idle)
        private set
    private var instantAnswerJob: Job? = null
    private var onDeviceTranslateJob: Job? = null

    var notes by mutableStateOf(store.notes())
        private set
    var reminders by mutableStateOf(store.reminders())
        private set
    var floatingNote by mutableStateOf(store.notes().lastOrNull { it.floating })
        private set
    /** When set, [FloatingNote] should focus its text field for editing. */
    var editingStickyId by mutableStateOf<String?>(null)

    var scheduleDate by mutableStateOf("")
    var scheduleTime by mutableStateOf("")

    var preferredMessaging by mutableStateOf(store.preferredMessaging)
    var preferredEmail by mutableStateOf(store.preferredEmail)
    var preferredSocial by mutableStateOf(store.preferredSocial)
    var preferredNav by mutableStateOf(store.preferredNav)
    var actionBoxCommandsRev by mutableStateOf(0)
    var independentActionBox by mutableStateOf(false)
        private set
    var settingsFromActionBox by mutableStateOf(false)
        private set
    var userAliases by mutableStateOf(store.userCommandAliases)
        private set
    var userShortcuts by mutableStateOf(store.userShortcuts)
        private set
    var shortcutContactHits by mutableStateOf<List<ContactEntry>>(emptyList())
        private set
    var pendingUserShortcutPkg by mutableStateOf<String?>(null)
        private set
    var disabledBuiltInAliases by mutableStateOf(store.disabledBuiltInCommandAliases)
        private set
    var overlayStickyPending by mutableStateOf(false)
    var overlayGrantInFlight by mutableStateOf(false)

    var notifGeom by mutableStateOf(loadWindow("notif", 24f, 140f, 340f, 460f))
    var qsGeom by mutableStateOf(loadWindow("qs", 40f, 160f, 320f, 380f))
    var systemGeom by mutableStateOf(loadWindow("system", 28f, 100f, 340f, 440f))
    var actionGeom by mutableStateOf(loadWindow("action", 24f, 220f, 340f, 72f))
    var helpGeom by mutableStateOf(loadWindow("help", 32f, 100f, 340f, 480f))
    var settingsGeom by mutableStateOf(loadWindow("settings", 36f, 120f, 320f, 420f))
    var setupGeom by mutableStateOf(loadWindow("setup", 20f, 72f, 348f, 540f))
    var floatNotifW by mutableStateOf(store.floatNotifW)
        private set
    var floatNotifH by mutableStateOf(store.floatNotifH)
        private set

    private fun loadWindow(key: String, x: Float, y: Float, w: Float, h: Float): WindowGeom {
        val a = store.loadGeom(key, x, y, w, h)
        return WindowGeom(a[0], a[1], a[2], a[3])
    }

    fun saveWindow(kind: String, geom: WindowGeom) {
        when (kind) {
            "notif" -> notifGeom = geom
            "qs" -> qsGeom = geom
            "system" -> systemGeom = geom
            "action" -> actionGeom = geom
            "help" -> helpGeom = geom
            "settings" -> settingsGeom = geom
            "setup" -> setupGeom = geom
        }
        store.saveGeom(kind, geom.x, geom.y, geom.w, geom.h)
    }

    val hasOutsideTargets: Boolean
        get() = notificationsOpen || quickSettingsOpen || systemNotifDropdownOpen || helpOpen || settingsOpen || setupOpen ||
            openFolderId != null || floatingConvs.any { !it.minimized }

    fun dismissOutsideFloats() {
        notificationsOpen = false
        quickSettingsOpen = false
        systemNotifDropdownOpen = false
        helpOpen = false
        commandPickerOpen = false
        settingsRoute = null
        syncAppearanceDraft()
        if (!SetupWindowPolicy.keepOpen(
                setupOpen,
                store.setupDismissed,
                grants.requiredDone,
                independentActionBox,
            ) && !com.madlauncher.LauncherWindowDismiss.shouldPreserve()
        ) {
            setupOpen = false
        }
        openFolderId = null
        for (i in floatingConvs.indices) {
            if (!floatingConvs[i].minimized) {
                floatingConvs[i] = floatingConvs[i].copy(minimized = true)
            }
        }
    }

    /** Last-known on state for 3rd-party TileServices (no public state API). */
    private val customTileEnabled = mutableMapOf<String, Boolean>()
    private var qsTilesObserver: android.database.ContentObserver? = null

    var qsEditing by mutableStateOf(false)
    var qsReplaceId by mutableStateOf<String?>(null)
    var toggles by mutableStateOf(loadQsTiles())
        private set

    val availableQsTiles: List<QsTileSpec>
        get() {
            val have = toggles.map { it.id }.toSet()
            val fromSystem = SystemQsMirror.discoverExtraTiles(getApplication(), have)
                .map { QsTileSpec(it.id, it.label) }
            val fromCatalog = QsCatalog.all.filter { it.id !in have }
            return (fromSystem + fromCatalog).distinctBy { it.id }
        }

    private fun loadQsTiles(): List<ToggleItem> {
        val ctx = getApplication<Application>()
        val useMirror = isFeatureEnabled(PlatformFeature.QS_MIRROR)
        val mirrored = if (useMirror) SystemQsMirror.readTiles(ctx) else emptyList()
        if (mirrored.isNotEmpty()) {
            return mirrored.map { tile ->
                ToggleItem(
                    id = tile.id,
                    label = QsStateReader.liveLabel(ctx, tile.id, tile.label, tile.spec),
                    enabled = readTileEnabled(ctx, tile.id, tile.spec),
                    systemSpec = tile.spec,
                )
            }.also { list ->
                store.qsTileIds = list.map { it.id }
            }
        }
        // Fallback when Secure setting is empty / inaccessible.
        val device = QsStateReader.availableOnDevice(ctx)
        val saved = store.qsTileIds.filter { id -> device.any { it.id == id } || id.startsWith("custom:") }
        val ordered = LinkedHashSet<String>()
        saved.forEach { ordered.add(it) }
        device.forEach { ordered.add(it.id) }
        if (ordered.isEmpty()) QsCatalog.defaultIds.forEach { ordered.add(it) }
        return ordered.map { id ->
            val spec = SystemQsMirror.specForId(id) ?: id
            val fallback = QsCatalog.label(id)
            ToggleItem(
                id,
                QsStateReader.liveLabel(ctx, id, fallback, spec),
                readTileEnabled(ctx, id, spec),
                spec,
            )
        }
    }

    private fun readTileEnabled(ctx: Context, id: String, spec: String): Boolean {
        if (QsStateReader.canReadState(id, spec)) {
            return QsStateReader.isEnabled(ctx, id, spec)
        }
        if (id.startsWith("custom:")) {
            return customTileEnabled[id] ?: false
        }
        return QsStateReader.isEnabled(ctx, id, spec)
    }

    private fun persistQsTiles() {
        store.qsTileIds = toggles.map { it.id }
        // Keep the real status-bar row in the same order (extension / mirror).
        val specs = toggles.mapNotNull { tile ->
            tile.systemSpec.takeIf { it.isNotBlank() }
                ?: SystemQsMirror.specForId(tile.id)
        }
        SystemQsMirror.writeSpecs(getApplication(), specs)
    }

    var wallpaperPath by mutableStateOf(store.wallpaperPath)
        private set
    var dockSlots by mutableStateOf(store.dockSlots)
        private set
    var dockRow2Slots by mutableStateOf(store.dockRow2Slots)
        private set
    var dockSecondRowExpanded by mutableStateOf(false)
    var edgePanels by mutableStateOf(store.edgePanels)
        private set
    var expandedEdge by mutableStateOf<EdgeSide?>(null)
    var homePages by mutableStateOf(store.homePages)
        private set
    var homePageIndex by mutableStateOf(initialHomeIndex())
    var dockPickApp by mutableStateOf<LaunchableApp?>(null)
    var dockRevealed by mutableStateOf(false)
    var homeEditMode by mutableStateOf(false)
    var editingWidgetId by mutableStateOf<String?>(null)
    var homePagerLocked by mutableStateOf(false)

    fun lockHomePager() {
        homePagerLocked = true
    }

    fun unlockHomePagerIfIdle() {
        if (editingWidgetId == null && appHold == null) homePagerLocked = false
    }

    var widgetPickerOpen by mutableStateOf(false)
    var newsPickerOpen by mutableStateOf(false)
    var openFolderId by mutableStateOf<String?>(null)
    var newsProviders by mutableStateOf<List<NewsProvider>>(emptyList())
        private set
    var newsStories by mutableStateOf<List<NewsStory>>(emptyList())
        private set
    var newsLoading by mutableStateOf(false)
        private set
    var newsPlace by mutableStateOf<DevicePlace?>(null)
        private set
    var frequentApps by mutableStateOf<List<LaunchableApp>>(emptyList())
        private set
    var hasUsageAccess by mutableStateOf(false)
        private set
    var widgetCatalog by mutableStateOf<List<WidgetCatalogEntry>>(emptyList())
        private set
    var widgetCatalogLoading by mutableStateOf(false)
        private set
    var widgetBindIntent by mutableStateOf<Intent?>(null)
    var widgetConfigIntent by mutableStateOf<Intent?>(null)
    private var pendingAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pendingAppWidgetSpan = 2 to 2
    private var pendingPlaceCol = -1
    private var pendingPlaceRow = -1
    var appHold by mutableStateOf<AppHold?>(null)
        private set
    var dragMoved by mutableStateOf(false)
        private set
    private var lastHomeFlipMs = 0L
    var dragX by mutableStateOf(0f)
    var dragY by mutableStateOf(0f)
    var dragDropKey by mutableStateOf<String?>(null)
    var widgetResizePreview by mutableStateOf<Pair<String, Pair<Float, Float>>?>(null)
    var widgetMovePreview by mutableStateOf<Pair<String, Pair<Int, Int>>?>(null)
    /** When dragging a widget over another, the partner item id to swap with. */
    var widgetSwapPartnerId by mutableStateOf<String?>(null)
    var appMenu by mutableStateOf<AppHold?>(null)
    private var appMenuGuardUntil = 0L
    var iconEditTarget by mutableStateOf<String?>(null)
    var hiddenApps by mutableStateOf(store.hiddenApps)
        private set
    val dockDropRects = MutableList(HomeLayout.DOCK_ROW_SLOTS * 2) { RectF() }
    val homeDropRects = mutableMapOf<String, RectF>()
    fun clearHomeDropRectsForPage(pageIndex: Int) {
        val prefix = HomeDropHit.pagePrefix(pageIndex)
        homeDropRects.keys.filter { it.startsWith(prefix) }.forEach { homeDropRects.remove(it) }
    }
    /** Window-space bounds of laid-out home items (after cell padding), keyed by "pageIndex:itemId". */
    val homeItemHitRects = mutableMapOf<String, RectF>()
    val edgeDropRects = mutableMapOf<String, RectF>()

    fun setHomeItemHitRect(pageIndex: Int, itemId: String, rect: RectF?) {
        val key = "$pageIndex:$itemId"
        if (rect == null || rect.isEmpty) homeItemHitRects.remove(key)
        else homeItemHitRects[key] = RectF(rect)
    }

    fun gridSizeFor(page: HomePage): Int = HomeLayout.gridSizeFor(page, displayAppearance.homeGridSize)

    fun gridSizeFor(pageIndex: Int): Int =
        homePages.getOrNull(pageIndex)?.let { gridSizeFor(it) } ?: displayAppearance.homeGridSize

    fun setDefaultHomeGridSize(size: Int) {
        val clamped = size.coerceIn(HomeLayout.MIN_GRID, HomeLayout.MAX_GRID)
        patchAppearanceDraft { it.copy(homeGridSize = clamped) }
    }

    fun setCurrentPageGridSize(size: Int) {
        val idx = homePageIndex
        val page = homePages.getOrNull(idx) ?: return
        if (page.kind != HomePageKind.EMPTY) return
        val clamped = size.coerceIn(HomeLayout.MIN_GRID, HomeLayout.MAX_GRID)
        replacePage(idx, clampPageItemsToGrid(page, clamped).copy(gridSize = clamped))
    }

    /**
     * True when a window-space point sits on a home desktop item (app / folder / widget).
     * Uses real item bounds (not full grid cells) and insets widget edges so near-border
     * presses count as free wallpaper for the home long-press menu.
     */
    fun touchHitsHomeItem(windowX: Float, windowY: Float, pageIndex: Int = homePageIndex): Boolean {
        val page = homePages.getOrNull(pageIndex) ?: return false
        for (item in page.items) {
            val rect = homeItemHitRects["$pageIndex:${item.id}"] ?: continue
            val inset = homeItemEdgeInset(item, rect)
            if (rect.width() <= inset * 2f || rect.height() <= inset * 2f) {
                // Tiny item — use full bounds.
                if (rect.contains(windowX, windowY)) return true
                continue
            }
            if (windowX >= rect.left + inset && windowX < rect.right - inset &&
                windowY >= rect.top + inset && windowY < rect.bottom - inset
            ) {
                return true
            }
        }
        return false
    }

    private fun homeItemEdgeInset(item: HomeItem, rect: RectF): Float {
        val widgetish = item.isAppWidget ||
            item.widget.isNotBlank() ||
            item.kind.equals("glance", ignoreCase = true)
        if (!widgetish) return 6f
        // Shrink widget hit box from the edges so free-space long-press works in the margin.
        val minSide = minOf(rect.width(), rect.height())
        return (minSide * 0.14f).coerceIn(18f, 56f)
    }

    val drawerApps: List<LaunchableApp>
        get() = apps.filter { it.packageName !in hiddenApps }

    fun sortDrawerApps(source: List<LaunchableApp>): List<LaunchableApp> {
        return when (displayAppearance.drawerSort) {
            DrawerSortId.NAME_AZ -> source.sortedBy { it.label.lowercase() }
            DrawerSortId.NAME_ZA -> source.sortedByDescending { it.label.lowercase() }
            DrawerSortId.MOST_USED -> {
                val stats = store.appLaunchStats
                source.sortedWith(
                    compareByDescending<LaunchableApp> { stats[it.packageName]?.count ?: 0 }
                        .thenByDescending { stats[it.packageName]?.lastUsed ?: 0L }
                        .thenBy { it.label.lowercase() },
                )
            }
        }
    }

    fun setDrawerSort(sort: DrawerSortId) {
        applyAppearance(appearanceSettings.copy(drawerSort = sort))
    }

    fun customIconPath(packageName: String): String? = store.customIconPath(packageName)

    val command: ActionCommand
        get() {
            when (composeKind) {
                ActionAppKind.Translator -> return ActionCommand.Translate(actionQuery)
                ActionAppKind.Notes -> return ActionCommand.NotePick(actionQuery)
                ActionAppKind.Ai -> return ActionCommand.AiAsk(actionQuery)
                ActionAppKind.Nav -> return ActionCommand.Navigate(actionQuery.trim())
                ActionAppKind.Contacts -> return ActionCommand.AddContact(actionQuery)
                else -> Unit
            }
            pendingUserShortcutPkg?.let { pkg ->
                return ActionCommand.OpenApp(pkg, actionQuery.trim())
            }
            if (contactLookupActive) {
                return ActionCommand.ContactLookup(
                    if (composeContact != null) "" else actionQuery.trim(),
                )
            }
            return CommandDetector.detect(actionQuery)
        }

    val mockNotifs = listOf(
        LiveNotif("m1", "com.google.android.apps.messaging", "Messages", "New message from Alice", "Are you free this evening?", "2m ago", 5, "sms|alice", listOf("Are you free this evening?"), canInlineReply = true, category = Notification.CATEGORY_MESSAGE),
        LiveNotif("m2", "com.google.android.calendar", "Calendar", "Meeting reminder", "Standup in 15 minutes — Daily sync", "8m ago", 4, "cal|standup", listOf("Standup in 15 minutes — Daily sync"), category = Notification.CATEGORY_EVENT),
        LiveNotif("m3a", "org.telegram.messenger", "Telegram", "Bob", "Check out this MAD launcher!", "12m ago", 3, "telegram|bob", listOf("Hey", "Check out this MAD launcher!"), canInlineReply = true, category = Notification.CATEGORY_MESSAGE),
        LiveNotif("m3b", "org.telegram.messenger", "Telegram", "Bob", "It has floating windows now", "11m ago", 3, "telegram|bob", listOf("Hey", "Check out this MAD launcher!", "It has floating windows now"), canInlineReply = true, category = Notification.CATEGORY_MESSAGE),
        LiveNotif("m4", "com.github.android", "GitHub", "CI passed", "mad-launcher #142", "1h ago", 2, "github|ci", listOf("mad-launcher #142"), category = Notification.CATEGORY_STATUS),
        LiveNotif("m5", "com.google.android.gm", "Gmail", "Deploy ready", "Release notes attached", "2h ago", 1, "gmail|deploy", listOf("Release notes attached"), category = Notification.CATEGORY_EMAIL),
        LiveNotif("m6", "com.android.systemui", "System UI", "Virtual SD card", "For storing photos and media", "Now", 6, "sys|sdcard", listOf("Virtual SD card"), ongoing = true, category = Notification.CATEGORY_SYSTEM),
    )

    private val notifSource: List<LiveNotif>
        get() = when {
            // Always prefer live data once the listener is connected — even if empty.
            // Falling back to mocks hid real notifications and made the panel look stuck.
            listenerConnected -> liveNotifs
            grants.notificationListener -> liveNotifs
            else -> mockNotifs
        }

    private val installedMessagingPkgs: Set<String>
        get() = NotificationHub.atPackageNames.ifEmpty {
            buildSet {
                addAll(actionApps.installed(ActionAppKind.Messaging).map { it.packageName })
                addAll(actionApps.installed(ActionAppKind.Email).map { it.packageName })
            }
        }

    private fun filteredSource(): List<LiveNotif> {
        val floating = floatingConvs.map { it.conversationId }.toSet()
        // Active shade is truth — do not hide rows with local dismiss ghosts.
        return notifSource.filter { it.conversationId !in floating }
    }

    val messagingNotifs: List<LiveNotif>
        get() {
            val filter = binderFilter
            // Long-press "Notifications": all alerts from that app (chat + other).
            if (filter != null) {
                return filteredSource()
                    .filter { it.packageName == filter }
                    .distinctBy { it.conversationId }
                    .sortedByDescending { it.postedAt }
            }
            val pkgs = installedMessagingPkgs
            return filteredSource()
                .filter { NotifClassifier.isMessagingWindow(it, pkgs) }
                .distinctBy { it.conversationId }
        }

    /** App label for the per-app notifications floating window title. */
    val binderFilterAppName: String?
        get() {
            val pkg = binderFilter ?: return null
            return notifSource.firstOrNull { it.packageName == pkg }?.appName
                ?: apps.firstOrNull { it.packageName == pkg }?.label
                ?: pkg
        }

    val systemNotifs: List<LiveNotif>
        get() {
            val pkgs = installedMessagingPkgs
            val runningPkgs = persistentApps.map { it.packageName }.toSet()
            return filteredSource()
                .filterNot { NotifClassifier.isMessagingWindow(it, pkgs) }
                // Media / ongoing players already listed under "Now playing / running"
                .filterNot { it.packageName in runningPkgs && (it.hasMediaSession || it.ongoing) }
                .distinctBy { it.key }
                .sortedByDescending { it.postedAt }
        }

    val displayedNotifs: List<LiveNotif>
        get() = messagingNotifs

    val unreadApps: List<UnreadApp>
        get() {
            val read = readIds.toSet()
            val dismissed = dismissedIds.toSet()
            val pkgs = installedMessagingPkgs
            return notifSource
                .filter {
                    it.key !in read &&
                        it.key !in dismissed &&
                        !NotificationHub.isDismissed(it.key) &&
                        !NotificationHub.isConversationDismissed(it.conversationId)
                }
                .filter { NotifClassifier.isMessagingWindow(it, pkgs) }
                .groupBy { it.packageName }
                .map { (pkg, items) -> UnreadApp(pkg, items.first().appName, items.size) }
        }

    /** System shade badge counts (OEM canShowBadge rules) — not MAD dismiss filters. */
    var systemBadgeCounts by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    /** Unread count for icon badges — any device with notification listener access. */
    fun unreadBadgeCount(packageName: String): Int {
        if (packageName.isBlank()) return 0
        if (!grants.notificationListener && !listenerConnected) return 0
        return systemBadgeCounts[packageName] ?: 0
    }

    val systemUnreadCount: Int
        get() {
            val read = readIds.toSet()
            val dismissed = dismissedIds.toSet()
            val pkgs = installedMessagingPkgs
            return notifSource.count {
                it.key !in read && it.key !in dismissed &&
                    !NotificationHub.isDismissed(it.key) &&
                    !NotificationHub.isConversationDismissed(it.conversationId) &&
                    !NotifClassifier.isMessagingWindow(it, pkgs)
            }
        }

    fun conversationThread(conversationId: String): List<LiveNotif> {
        return notifSource.filter { it.conversationId == conversationId }.sortedBy { it.postedAt }
    }

    fun conversationLines(conversationId: String): List<ThreadLine> {
        val incoming = conversationThread(conversationId)
            .flatMap { it.messages.ifEmpty { listOf(it.body) } }
            .filter { it.isNotBlank() }
            .distinct()
            .map { ThreadLine(it, outgoing = false) }
        val outgoing = outgoingByConversation[conversationId].orEmpty().map { ThreadLine(it, outgoing = true) }
        return (incoming + outgoing).takeLast(12)
    }

    fun latestInConversation(conversationId: String): LiveNotif? {
        return notifSource.filter { it.conversationId == conversationId }.maxByOrNull { it.postedAt }
    }

    private val persistentTracker = PersistentAppsTracker(application)

    init {
        if (com.madlauncher.BuildConfig.ACTION_BOX_APP) {
            independentActionBox = true
            setupOpen = false
        }
        CommandAliases.replaceExtras(userAliases)
        CommandAliases.replaceDisabledBuiltIns(disabledBuiltInAliases)
        UserShortcuts.replace(userShortcuts)
        refreshGrants()
        if (!com.madlauncher.BuildConfig.ACTION_BOX_APP) {
            migrateBuiltInWidgets()
            ensureDefaultGlanceWidget()
            applyAppearance(appearanceSettings, persist = false)
            AppearanceBridge.update(appearanceSettings)
            refreshWallpaperPalette()
            refreshClock()
            syncAtPackages()
            refreshPlatformProfile()
            viewModelScope.launch {
                while (isActive) {
                    delay(15_000)
                    refreshClock()
                }
            }
            viewModelScope.launch {
                delay(800)
                refreshNews()
                while (isActive) {
                    delay(15 * 60_000)
                    refreshNews()
                }
            }
            viewModelScope.launch {
                while (isActive) {
                    persistentApps = persistentTracker.snapshot(liveNotifs)
                    delay(2_500)
                }
            }
        }
        contactsAllowed = contactsRepo.hasPermission()
        viewModelScope.launch {
            reloadLaunchableApps()
        }
        viewModelScope.launch {
            AppsReloadBus.tick.debounce(300).collect {
                reloadLaunchableApps()
                // Package install/update/uninstall — re-probe Action Box + launcher capabilities.
                refreshPlatformProfile()
            }
        }
        val shortcutListener = {
            viewModelScope.launch(Dispatchers.Main) {
                homePages = store.homePages
                showToast("Home updated")
            }
            Unit
        }
        com.madlauncher.pin.ShortcutBus.addListener(shortcutListener)
        viewModelScope.launch {
            var lastMediaFingerprint = ""
            NotificationHub.items.collect { list ->
                liveNotifs = list
                RecipientHistory.mergeFromNotifs(store, list)
                // Keys present in the active shade are live again — clear local dismiss/read.
                val liveKeys = list.map { it.key }.toSet()
                dismissedIds.removeAll { it in liveKeys }
                readIds.removeAll { it in liveKeys }
                if (composeApp != null && composeContact == null &&
                    (composeKind == ActionAppKind.Messaging || composeKind == ActionAppKind.Email ||
                        composeKind == ActionAppKind.VoiceCall || composeKind == ActionAppKind.VideoCall)
                ) {
                    refreshContacts()
                }
                // Media widgets (YT Music, etc.) sometimes miss RemoteViews while a duplicate
                // pager host held the slot — nudge providers when the playing track changes.
                val mediaFp = list.filter { it.hasMediaSession }
                    .joinToString { "${it.packageName}|${it.title}|${it.body}" }
                if (mediaFp != lastMediaFingerprint) {
                    lastMediaFingerprint = mediaFp
                    list.filter { it.hasMediaSession }
                        .map { it.packageName }
                        .distinct()
                        .forEach { nudgeAppWidgetsForPackage(it) }
                }
            }
        }
        viewModelScope.launch {
            SystemBadgeHub.counts.collect { systemBadgeCounts = it }
        }
        viewModelScope.launch {
            NotificationHub.connected.collect { connected ->
                listenerConnected = connected
                if (connected) refreshPlatformProfile()
            }
        }
        viewModelScope.launch {
            NotificationHub.clearedKeys.collect { key ->
                // App or system cleared the status-bar notif (opened/read in-app, etc.).
                if (key !in readIds) readIds.add(key)
                if (key !in dismissedIds) dismissedIds.add(key)
                val conv = liveNotifs.firstOrNull { it.key == key }?.conversationId
                liveNotifs = liveNotifs.filterNot {
                    it.key == key || (conv != null && it.conversationId == conv)
                }
                // Drop floating chips for conversations that no longer have any live notifs.
                val goneConvs = floatingConvs.filter { fc ->
                    liveNotifs.none { it.conversationId == fc.conversationId }
                }.map { it.conversationId }
                if (goneConvs.isNotEmpty()) {
                    floatingConvs.removeAll { it.conversationId in goneConvs }
                }
                persistDismissedNotifs()
            }
        }
        viewModelScope.launch {
            NotificationHub.clearedConversations.collect { conversationId ->
                val keys = liveNotifs.filter { it.conversationId == conversationId }.map { it.key }
                keys.forEach { key ->
                    if (key !in readIds) readIds.add(key)
                    if (key !in dismissedIds) dismissedIds.add(key)
                }
                liveNotifs = liveNotifs.filterNot { it.conversationId == conversationId }
                floatingConvs.removeAll { it.conversationId == conversationId }
                persistDismissedNotifs()
            }
        }
        if (!com.madlauncher.BuildConfig.ACTION_BOX_APP) {
            viewModelScope.launch(Dispatchers.IO) {
                val path = store.wallpaperPath ?: return@launch
                val file = File(path)
                if (!file.exists()) return@launch
                if (WallpaperApplier.applyFile(getApplication(), file)) {
                    store.wallpaperPath = null
                    withContext(Dispatchers.Main) { wallpaperPath = null }
                }
            }
            registerQsMirrorObservers()
            RecentsIngest.addListener(recentsIngestListener)
        }
    }

    private fun registerQsMirrorObservers() {
        val ctx = getApplication<Application>()
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        qsTilesObserver?.let { ctx.contentResolver.unregisterContentObserver(it) }
        qsTilesObserver = object : android.database.ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                reloadQsTilesFromSystem()
            }
        }.also { observer ->
            ctx.contentResolver.registerContentObserver(
                android.provider.Settings.Secure.getUriFor("sysui_qs_tiles"),
                false,
                observer,
            )
        }
        viewModelScope.launch {
            while (isActive) {
                if (quickSettingsOpen) refreshQsStates()
                delay(1200)
            }
        }
    }

    private fun refreshClock() {
        val now = Date()
        clock = DateFormat.getTimeInstance(DateFormat.SHORT).format(now).uppercase()
        date = DateFormat.getDateInstance(DateFormat.FULL).format(now)
        greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            else -> "Good evening"
        }
    }

    fun refreshGrants() {
        val ctx = getApplication<Application>()
        grants = PermissionChecker.snapshot(ctx)
        contactsAllowed = grants.contacts
        if (!com.madlauncher.BuildConfig.ACTION_BOX_APP) {
            SmsInboxMonitor.start(ctx)
        }
        if (independentActionBox) {
            setupOpen = false
        } else if (com.madlauncher.LauncherWindowDismiss.shouldPreserve() && setupOpen) {
            // Stay on screen while the user is in a grant / battery dialog.
        } else if (!store.setupDismissed && !grants.requiredDone && !com.madlauncher.platform.MadOsRuntime.isMadOs(ctx)) {
            setupOpen = true
        } else if ((grants.requiredDone && !setupOpenedByUser) ||
            com.madlauncher.platform.MadOsRuntime.isMadOs(ctx)
        ) {
            setupOpen = false
        }
        if (!com.madlauncher.BuildConfig.ACTION_BOX_APP && grants.location) {
            refreshWeather()
            refreshNewsProviders()
        }
        reloadLaunchableApps()
        refreshPlatformProfile()
    }

    fun featureStatus(feature: PlatformFeature): FeatureStatus = platformProfile.status(feature)

    fun isFeatureEnabled(feature: PlatformFeature): Boolean = platformProfile.isEnabled(feature)

    fun isXiaomiPlatform(): Boolean {
        if (com.madlauncher.platform.MadOsRuntime.isMadOs(getApplication())) return false
        val f = platformProfile.oemFamily
        return f == OemFamily.XIAOMI_HYPEROS || f == OemFamily.MIUI
    }

    fun refreshPlatformProfile(recheckDelayMs: Long = 0L, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            platformProbing = true
            try {
                val ctx = getApplication<Application>()
                val family = PlatformDetector.detect(ctx)
                val profile = PlatformProber.probe(ctx, family, recheckDelayMs)
                platformProfile = profile
                PlatformBridge.update(profile)
                deviceOsInfo = DeviceOsInfo.capture(ctx)
                store.savePlatformCapabilities(
                    family = family.name,
                    caps = profile.capabilities.mapKeys { it.key.name }.mapValues { it.value.name },
                    probedAt = profile.probedAtMillis,
                )
                store.actionBoxCatalog = profile.actionBoxCatalog
            } finally {
                platformProbing = false
                onComplete()
            }
        }
    }

    /** Re-probe only. Upload is a separate confirm dialog. */
    fun recheckCompatibility(onComplete: () -> Unit = {}) {
        refreshPlatformProfile(recheckDelayMs = 400L, onComplete = onComplete)
    }

    /**
     * Re-check then upload. Prefer [recheckCompatibility] + confirm, then this.
     */
    fun recheckAndUploadCompatibilityReport(
        userName: String = "",
        userEmail: String = "",
        userNotes: String = "",
        onComplete: () -> Unit = {},
    ) {
        uploadCompatibilityReport(userName, userEmail, userNotes, onComplete)
    }

    fun uploadCompatibilityReport(
        userName: String = "",
        userEmail: String = "",
        userNotes: String = "",
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch {
            try {
                val ctx = getApplication<Application>()
                val report = buildCompatibilityReport(userName, userEmail, userNotes)
                val result = FeedbackSubmitter.submitCompatibilityReport(ctx, report)
                store.compatibilityReportSubmitted = true
                compatibilityReportSubmitted = true
                showToast(result.message)
            } finally {
                onComplete()
            }
        }
    }

    fun openSystemShade() {
        if (!isFeatureEnabled(PlatformFeature.SYSTEM_SHADE_EXPAND)) {
            showToast("System shade unavailable on this device")
            return
        }
        val ok = SystemShadeController.expandNotifications(getApplication())
        if (!ok) showToast("Could not open system shade")
    }

    private fun loadCachedPlatformProfile(): PlatformProfile {
        val familyName = store.platformOemFamily
        val family = runCatching { OemFamily.valueOf(familyName.orEmpty()) }.getOrDefault(OemFamily.UNKNOWN)
        val cached = store.loadPlatformCapabilities()
        val caps = if (cached.isEmpty()) {
            PlatformProfile.baseline(family)
        } else {
            cached.mapNotNull { (k, v) ->
                val feature = runCatching { PlatformFeature.valueOf(k) }.getOrNull() ?: return@mapNotNull null
                val status = runCatching { FeatureStatus.valueOf(v) }.getOrNull() ?: return@mapNotNull null
                feature to status
            }.toMap()
        }
        return PlatformProfile.build(
            family,
            caps,
            store.platformProbeAt,
            actionBoxCatalog = store.actionBoxCatalog,
        )
    }

    private fun suppressQsToggleError(): Boolean {
        val s = featureStatus(PlatformFeature.QS_TOGGLE_SYNC)
        return s == FeatureStatus.DEGRADED || s == FeatureStatus.UNKNOWN
    }

    private fun madVersionName(): String {
        return runCatching {
            val ctx = getApplication<Application>()
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
        }.getOrNull() ?: "?"
    }

    fun buildCompatibilityReport(
        userName: String = "",
        userEmail: String = "",
        userNotes: String = "",
    ): CompatibilityReport {
        return CompatibilityReport(
            device = deviceOsInfo,
            madVersion = madVersionName(),
            probedAtMillis = platformProfile.probedAtMillis,
            features = platformProfile.capabilities,
            grants = grants,
            userNotes = userNotes,
            userName = userName,
            userEmail = userEmail,
            actionBoxCatalog = platformProfile.actionBoxCatalog.ifBlank { store.actionBoxCatalog },
        )
    }

    fun submitCompatibilityReport(
        userName: String,
        userEmail: String,
        userNotes: String,
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val report = buildCompatibilityReport(userName, userEmail, userNotes)
            val result = FeedbackSubmitter.submitCompatibilityReport(ctx, report)
            store.compatibilityReportSubmitted = true
            compatibilityReportSubmitted = true
            showToast(result.message)
            onComplete()
        }
    }

    fun skipCompatibilityReport() {
        store.compatibilityReportSubmitted = true
        compatibilityReportSubmitted = true
        showToast("You can send a report anytime from Not working or Setup")
    }

    fun reloadLaunchableApps() {
        viewModelScope.launch {
            loadingApps = apps.isEmpty()
            try {
                actionApps.invalidate()
                apps = appsRepo.loadLaunchableApps()
                searchableApps = withContext(Dispatchers.IO) {
                    runCatching { SearchableApps.resolveAll(getApplication(), apps) }
                        .getOrDefault(emptyList())
                }
                restoreRecentsFromStore()
                refreshSystemRecents()
            } finally {
                loadingApps = false
            }
            syncAtPackages()
            migrateDockDefaults()
            refreshFrequentApps()
            refreshNewsProviders()
        }
    }

    private fun restoreRecentsFromStore() {
        if (recents.isNotEmpty()) return
        val byId = apps.associateBy { it.id }
        store.recentAppIds.mapNotNull { byId[it] }.forEach { recents.add(it) }
    }

    fun refreshSystemRecents() {
        hasUsageAccess = usageTracker.hasUsageAccess()
        val recentsPool = apps.toList().ifEmpty { drawerApps }
        val drawerPool = drawerApps
        if (recentsPool.isEmpty() && drawerPool.isEmpty()) return
        recentsQueryJob?.cancel()
        recentsQueryJob = viewModelScope.launch(Dispatchers.IO) {
            val next = if (recentsPool.isEmpty()) emptyList() else usageTracker.recents(recentsPool)
            val drawer = usageTracker.drawerRecentRow(next, drawerPool.ifEmpty { recentsPool })
            withContext(Dispatchers.Main) {
                recents.clear()
                recents.addAll(next)
                store.recentAppIds = recents.map { it.id }
                drawerRecent = drawer
            }
        }
    }

    override fun onCleared() {
        RecentsIngest.removeListener(recentsIngestListener)
        super.onCleared()
    }

    private fun initialHomeIndex(): Int {
        val lastId = store.lastHomePageId
        if (lastId.isNotBlank()) {
            val lastIdx = homePages.indexOfFirst { it.id == lastId }
            if (lastIdx >= 0) return lastIdx
        }
        val id = store.homePageId
        val idx = homePages.indexOfFirst { it.id == id }
        return if (idx >= 0) idx else 0
    }

    fun persistLastHomePageIndex() {
        homePages.getOrNull(homePageIndex)?.id?.takeIf { it.isNotBlank() }?.let {
            store.lastHomePageId = it
        }
    }

    fun setAsHomePage(pageId: String) {
        store.homePageId = pageId
        val idx = homePages.indexOfFirst { it.id == pageId }
        if (idx >= 0) homePageIndex = idx
        showToast("Home page set")
    }

    fun designatedHomeIndex(): Int {
        val id = store.homePageId
        val idx = homePages.indexOfFirst { it.id == id }
        return if (idx >= 0) idx else 0
    }

    fun storeHomePageId(): String = store.homePageId

    fun hostIndependentActionBox() {
        independentActionBox = true
        setupOpen = false
        CommandAliases.replaceExtras(userAliases)
        CommandAliases.replaceDisabledBuiltIns(disabledBuiltInAliases)
        UserShortcuts.replace(userShortcuts)
    }

    fun openActionBoxSettings() {
        settingsFromActionBox = true
        settingsRoute = SettingsRoute.ACTION_BOX_COMMANDS
    }

    fun addCommandAlias(target: AliasTarget, phrase: String) {
        val word = phrase.trim()
        if (word.isEmpty()) return
        val stock = CommandAliases.stockPhrases(target).any { it.equals(word, ignoreCase = true) }
        if (stock) {
            disabledBuiltInAliases = disabledBuiltInAliases - word.lowercase()
            store.disabledBuiltInCommandAliases = disabledBuiltInAliases
            CommandAliases.replaceDisabledBuiltIns(disabledBuiltInAliases)
        } else {
            userAliases = userAliases.withAdded(target, word)
            store.userCommandAliases = userAliases
            CommandAliases.replaceExtras(userAliases)
        }
        actionBoxCommandsRev++
    }

    fun removeCommandAlias(target: AliasTarget, phrase: String) {
        val stock = CommandAliases.stockPhrases(target).any { it.equals(phrase, ignoreCase = true) }
        if (stock) {
            disabledBuiltInAliases = disabledBuiltInAliases + phrase.lowercase()
            store.disabledBuiltInCommandAliases = disabledBuiltInAliases
            CommandAliases.replaceDisabledBuiltIns(disabledBuiltInAliases)
        } else {
            userAliases = userAliases.withRemoved(target, phrase)
            store.userCommandAliases = userAliases
            CommandAliases.replaceExtras(userAliases)
        }
        actionBoxCommandsRev++
    }

    fun renameCommandAlias(target: AliasTarget, oldPhrase: String, newPhrase: String) {
        val next = newPhrase.trim()
        if (next.isEmpty()) {
            removeCommandAlias(target, oldPhrase)
            return
        }
        if (next.equals(oldPhrase, ignoreCase = true)) return
        removeCommandAlias(target, oldPhrase)
        addCommandAlias(target, next)
    }

    fun addUserShortcut(
        phrases: List<String>,
        packageName: String,
        kind: UserShortcutKind = UserShortcutKind.OPEN_APP,
        combo: UserShortcutCombo? = null,
        contact: UserShortcutContact? = null,
    ): String? {
        val words = phrases.mapNotNull { UserShortcuts.normalizePhrase(it) }.distinctBy { it.lowercase() }
        if (words.isEmpty()) return overlayString(R.string.action_box_shortcut_need_words)
        if (packageName.isBlank()) {
            return overlayString(
                if (kind == UserShortcutKind.COMBO) R.string.action_box_shortcut_pick_combo_app
                else R.string.action_box_shortcut_pick_app,
            )
        }
        if (kind == UserShortcutKind.COMBO && combo == null) {
            return overlayString(R.string.action_box_shortcut_pick_combo)
        }
        words.forEach { word ->
            reservedShortcutReason(word)?.let { return it }
        }
        val item = UserShortcut(
            id = System.currentTimeMillis().toString(),
            phrases = words,
            packageName = packageName.trim(),
            kind = kind,
            combo = if (kind == UserShortcutKind.COMBO) combo else null,
            contact = if (kind == UserShortcutKind.COMBO && combo?.usesContact == true) contact else null,
        )
        persistUserShortcuts(userShortcuts + item)
        return null
    }

    fun removeUserShortcut(id: String) {
        persistUserShortcuts(userShortcuts.filterNot { it.id == id })
    }

    fun addUserShortcutPhrase(id: String, phrase: String) {
        val word = UserShortcuts.normalizePhrase(phrase) ?: return
        reservedShortcutReason(word, exceptId = id)?.let {
            showToast(it)
            return
        }
        persistUserShortcuts(
            userShortcuts.map { if (it.id == id) it.withPhrase(word) else it },
        )
    }

    fun removeUserShortcutPhrase(id: String, phrase: String) {
        val next = userShortcuts.map { if (it.id == id) it.withoutPhrase(phrase) else it }
            .filter { it.phrases.isNotEmpty() }
        persistUserShortcuts(next)
    }

    fun renameUserShortcutPhrase(id: String, oldPhrase: String, newPhrase: String) {
        val word = UserShortcuts.normalizePhrase(newPhrase)
        if (word == null) {
            removeUserShortcutPhrase(id, oldPhrase)
            return
        }
        if (word.equals(oldPhrase, ignoreCase = true)) return
        reservedShortcutReason(word, exceptId = id)?.let {
            showToast(it)
            return
        }
        persistUserShortcuts(
            userShortcuts.map { if (it.id == id) it.renamed(oldPhrase, word) else it },
        )
    }

    fun setUserShortcutApp(id: String, packageName: String) {
        if (packageName.isBlank()) return
        persistUserShortcuts(
            userShortcuts.map { if (it.id == id) it.copy(packageName = packageName, contact = null) else it },
        )
    }

    fun setUserShortcutKind(id: String, kind: UserShortcutKind, combo: UserShortcutCombo? = null) {
        persistUserShortcuts(
            userShortcuts.map { shortcut ->
                if (shortcut.id != id) shortcut
                else shortcut.copy(
                    kind = kind,
                    combo = if (kind == UserShortcutKind.COMBO) combo ?: shortcut.combo else null,
                    contact = if (kind == UserShortcutKind.COMBO && (combo ?: shortcut.combo)?.usesContact == true) {
                        shortcut.contact
                    } else {
                        null
                    },
                )
            },
        )
    }

    fun setUserShortcutContact(id: String, contact: UserShortcutContact?) {
        persistUserShortcuts(
            userShortcuts.map { if (it.id == id) it.copy(contact = contact) else it },
        )
    }

    fun comboAppsFor(combo: UserShortcutCombo): List<InstalledActionApp> {
        val seen = linkedSetOf<String>()
        return buildList {
            combo.appKinds.forEach { kind ->
                installedFor(kind, "").forEach { app ->
                    if (seen.add(app.packageName)) add(app)
                }
            }
        }
    }

    fun searchShortcutContacts(packageName: String, query: String, emails: Boolean) {
        viewModelScope.launch {
            shortcutContactHits = RecipientPicker.load(
                contactsRepo = contactsRepo,
                store = store,
                notifs = notifSource,
                packageName = packageName,
                query = query,
                emails = emails,
            )
        }
    }

    fun runUserShortcut(packageName: String, payload: String) {
        val q = payload.trim()
        if (q.isNotEmpty()) {
            val searchable = searchableApps.firstOrNull { it.app.packageName == packageName }
            val result = runCatching {
                intents.searchInApp(
                    packageName = packageName,
                    query = q,
                    urls = searchable?.def?.urlsFor(q).orEmpty(),
                    assisted = searchable?.def?.assistedSearch ?: true,
                )
            }.getOrDefault(SearchInAppResult.Failed)
            when (result) {
                is SearchInAppResult.Failed -> {
                    val app = apps.firstOrNull { it.packageName == packageName }
                    if (app != null) launchApp(app) else runCatching { intents.launchPackage(packageName) }
                        .onFailure { showToast(overlayString(R.string.action_box_open_failed)) }
                }
                else -> Unit
            }
            closeActionBox()
            return
        }
        val app = apps.firstOrNull { it.packageName == packageName }
        if (app != null) {
            launchApp(app)
        } else {
            runCatching { intents.launchPackage(packageName) }
                .onFailure { showToast(overlayString(R.string.action_box_open_failed)) }
        }
        closeActionBox()
    }

    fun runQuickCombo(cmd: ActionCommand.QuickCombo) {
        val app = actionApps.resolvePackage(cmd.packageName)
        if (app == null) {
            showToast(overlayString(R.string.action_box_open_failed))
            return
        }
        when (cmd.combo) {
            UserShortcutCombo.SOCIAL -> openSocialComposer(app)
            UserShortcutCombo.CHAT -> {
                val contact = cmd.toContact()
                if (contact != null) {
                    openConversationNow(app, contact)
                } else {
                    pickAtApp(app)
                    if (cmd.payload.isNotBlank()) {
                        recipientQuery = cmd.payload
                        refreshContacts()
                    }
                }
            }
            UserShortcutCombo.CALL -> {
                pickApp(ActionAppKind.VoiceCall, app)
                val contact = cmd.toContact()
                if (contact != null) callContact(contact, video = false)
            }
            UserShortcutCombo.VIDEO -> {
                pickApp(ActionAppKind.VideoCall, app)
                val contact = cmd.toContact()
                if (contact != null) callContact(contact, video = true)
            }
        }
    }

    private fun persistUserShortcuts(next: List<UserShortcut>) {
        userShortcuts = next
        store.userShortcuts = next
        UserShortcuts.replace(next)
        actionBoxCommandsRev++
    }

    private fun reservedShortcutReason(phrase: String, exceptId: String? = null): String? {
        val word = UserShortcuts.normalizePhrase(phrase) ?: return overlayString(R.string.action_box_shortcut_need_word)
        if (word.lowercase() in reservedSlashCommands) {
            return overlayString(R.string.action_box_shortcut_builtin, word)
        }
        val builtin = AliasTarget.entries.any { target ->
            CommandAliases.allPhrases(target).any { it.equals(word, ignoreCase = true) }
        }
        if (builtin) return overlayString(R.string.action_box_shortcut_builtin, word)
        if (UserShortcuts.phraseTaken(word, exceptId)) return overlayString(R.string.action_box_shortcut_taken, word)
        return null
    }

    private val reservedSlashCommands = setOf("/ss", "/ls", "/abs", "/s", "/n", "/t", "/g", "/c", "/r", "/h", "/a")

    fun openSetup() {
        setupOpenedByUser = true
        setupOpen = true
    }

    fun closeSetup() {
        setupOpen = false
    }

    fun skipSetup() {
        store.setupDismissed = true
        setupOpen = false
        setupOpenedByUser = false
    }

    fun openFromOverlay(panel: String) {
        if (panel == "home") {
            homePressed()
            return
        }
        if (!appearanceSettings.experimentalMadWindows) {
            openStockShade(panel)
            return
        }
        recentsOpen = false
        appDrawerOpen = false
        when (panel) {
            "notifications" -> {
                quickSettingsOpen = false
                systemNotifDropdownOpen = false
                binderFilter = null
                notifPane = NotifPane.Messages
                notificationsOpen = true
            }
            "quicksettings" -> {
                notificationsOpen = false
                systemNotifDropdownOpen = false
                quickSettingsOpen = true
                refreshQsStates()
            }
            "systemnotifs", "systemapps" -> openSystemNotifShade()
            "setup" -> openSetup()
            "home" -> homePressed()
        }
    }

    private fun openStockShade(panel: String) {
        val ok = SystemShadeController.openStockPanel(getApplication(), panel)
        if (!ok) showToast("Could not open system shade")
    }

    fun toggleNotifications() {
        if (!appearanceSettings.experimentalMadWindows) {
            openStockShade("notifications")
            return
        }
        recentsOpen = false
        appDrawerOpen = false
        systemNotifDropdownOpen = false
        if (notificationsOpen && notifPane == NotifPane.Messages && binderFilter == null) {
            notificationsOpen = false
        } else {
            binderFilter = null
            notifPane = NotifPane.Messages
            notificationsOpen = true
        }
    }

    /** Open the notifications floating window filtered to one app. */
    fun openAppNotifications(packageName: String) {
        if (packageName.isBlank() || packageName in HomeLayout.specialDock) return
        if (!appearanceSettings.experimentalMadWindows) {
            openStockShade("notifications")
            return
        }
        appMenu = null
        recentsOpen = false
        appDrawerOpen = false
        systemNotifDropdownOpen = false
        quickSettingsOpen = false
        binderFilter = packageName
        notifPane = NotifPane.Messages
        notificationsOpen = true
    }

    fun selectNotifPane(pane: NotifPane) {
        notifPane = pane
        if (pane == NotifPane.System) binderFilter = null
    }

    fun closeNotifications() {
        notificationsOpen = false
        binderFilter = null
        systemNotifDropdownOpen = false
    }

    fun openSystemNotifShade() {
        if (!appearanceSettings.experimentalMadWindows) {
            openStockShade("notifications")
            return
        }
        recentsOpen = false
        appDrawerOpen = false
        quickSettingsOpen = false
        binderFilter = null
        notifPane = NotifPane.System
        notificationsOpen = true
        systemNotifDropdownOpen = false
    }

    fun closeSystemNotifShade() {
        if (notificationsOpen && notifPane == NotifPane.System) {
            notificationsOpen = false
        }
        systemNotifDropdownOpen = false
    }

    fun toggleSystemNotifDropdown() {
        if (notificationsOpen && notifPane == NotifPane.System && binderFilter == null) {
            closeNotifications()
        } else {
            openSystemNotifShade()
        }
    }

    fun toggleQuickSettings() {
        if (!appearanceSettings.experimentalMadWindows) {
            openStockShade("quicksettings")
            return
        }
        recentsOpen = false
        appDrawerOpen = false
        quickSettingsOpen = !quickSettingsOpen
        if (quickSettingsOpen) {
            reloadQsTilesFromSystem()
        } else {
            qsEditing = false
            qsReplaceId = null
            dndDurationMenuOpen = false
        }
    }

    var dndDurationMenuOpen by mutableStateOf(false)

    fun refreshQsStates() {
        val ctx = getApplication<Application>()
        val mirrored = SystemQsMirror.readTiles(ctx)
        if (mirrored.isNotEmpty()) {
            toggles = mirrored.map { tile ->
                ToggleItem(
                    id = tile.id,
                    label = QsStateReader.liveLabel(ctx, tile.id, tile.label, tile.spec),
                    enabled = readTileEnabled(ctx, tile.id, tile.spec),
                    systemSpec = tile.spec,
                )
            }
            store.qsTileIds = toggles.map { it.id }
            return
        }
        toggles = toggles.map { tile ->
            tile.copy(
                enabled = readTileEnabled(ctx, tile.id, tile.systemSpec),
                label = QsStateReader.liveLabel(ctx, tile.id, tile.label, tile.systemSpec),
            )
        }
    }

    fun reloadQsTilesFromSystem() {
        toggles = loadQsTiles()
    }

    fun toggle(id: String) {
        if (qsEditing) {
            qsReplaceId = if (qsReplaceId == id) null else id
            return
        }
        val ctx = getApplication<Application>()
        val tile = toggles.firstOrNull { it.id == id } ?: return
        val spec = tile.systemSpec.takeIf { it.isNotBlank() }
            ?: SystemQsMirror.specForId(id, SystemQsMirror.readTiles(ctx))
            ?: id
        val before = readTileEnabled(ctx, id, spec)
        val isCustom = SystemQsMirror.isCustomSpec(spec)
        val mirrored = tile.systemSpec.isNotBlank()

        if (id == "dnd" || spec.equals("quietmode", ignoreCase = true) || spec == "dnd") {
            viewModelScope.launch {
                if (SystemQsMirror.clickTile(ctx, spec)) {
                    delay(400)
                    refreshQsStates()
                } else {
                    toggleDnd()
                }
            }
            return
        }

        viewModelScope.launch {
            val localFirst = featureStatus(PlatformFeature.QS_TOGGLE_SYNC) == FeatureStatus.DEGRADED
            val ok = QsToggleEngine.toggle(
                ctx = ctx,
                store = store,
                id = id,
                spec = spec,
                beforeEnabled = before,
                isCustom = isCustom,
                onCustomOptimisticFlip = { customTileEnabled[id] = !before },
                localFirst = localFirst,
            )
            refreshQsStates()
            when {
                ok -> Unit
                mirrored && !suppressQsToggleError() -> showToast("Couldn't toggle ${tile.label}")
                mirrored -> Unit
                else -> runCatching { intents.openQsTile(id) }
            }
        }
    }

    fun openDndDurationMenu() {
        if (qsEditing) return
        dndDurationMenuOpen = true
    }

    fun closeDndDurationMenu() {
        dndDurationMenuOpen = false
    }

    fun toggleDnd() {
        val app = getApplication<Application>()
        if (!DndController.hasPolicyAccess(app)) {
            requestDndPolicyAccess()
            return
        }
        val on = DndController.toggle(app)
        toggles = toggles.map { if (it.id == "dnd") it.copy(enabled = on) else it }
        showToast(if (on) "Do Not Disturb on" else "Do Not Disturb off")
    }

    fun setDndDuration(minutes: Int) {
        dndDurationMenuOpen = false
        val app = getApplication<Application>()
        if (!DndController.hasPolicyAccess(app)) {
            requestDndPolicyAccess()
            return
        }
        val ok = DndController.turnOn(app, minutes)
        if (!ok) {
            showToast("Couldn't enable Do Not Disturb")
            return
        }
        toggles = toggles.map { if (it.id == "dnd") it.copy(enabled = true) else it }
        val label = DndController.durationOptions.firstOrNull { it.minutes == minutes }?.label
            ?: "$minutes min"
        showToast("Do Not Disturb · $label")
    }

    private fun requestDndPolicyAccess() {
        showToast("Allow notification policy access for Do Not Disturb")
        runCatching {
            getApplication<Application>().startActivity(DndController.policyAccessIntent())
        }.onFailure {
            runCatching { intents.openQsTile("dnd") }
        }
    }

    fun openClockWidget() {
        runCatching { intents.openClock() }
            .onFailure { showToast("No clock app found") }
    }

    fun openCalendarWidget() {
        runCatching { intents.openCalendar() }
            .onFailure { showToast("No calendar app found") }
    }

    fun openWeatherWidget() {
        runCatching { intents.openWeather() }
            .onFailure { showToast("Couldn't open weather") }
    }

    fun launchPersistent(app: PersistentApp) {
        if (app.packageName in installedMessagingPkgs) {
            clearPackageNotifications(app.packageName)
        }
        runCatching { intents.launchPackage(app.packageName) }
            .onFailure { showToast("Couldn't open ${app.appName}") }
    }

    fun mediaPlayPause(app: PersistentApp) {
        if (!persistentTracker.playPause(app.packageName)) {
            showToast("Couldn't control ${app.appName}")
        } else {
            viewModelScope.launch {
                delay(200)
                persistentApps = persistentTracker.snapshot(liveNotifs)
            }
        }
    }

    fun mediaSkipNext(app: PersistentApp) {
        if (!persistentTracker.skipNext(app.packageName)) {
            showToast("No next track")
        } else {
            viewModelScope.launch {
                delay(250)
                persistentApps = persistentTracker.snapshot(liveNotifs)
            }
        }
    }

    fun mediaSkipPrevious(app: PersistentApp) {
        if (!persistentTracker.skipPrevious(app.packageName)) {
            showToast("No previous track")
        } else {
            viewModelScope.launch {
                delay(250)
                persistentApps = persistentTracker.snapshot(liveNotifs)
            }
        }
    }

    private fun refreshWeather() {
        viewModelScope.launch {
            val label = withContext(Dispatchers.IO) {
                WeatherFetcher.label(getApplication())
            }
            weatherLabel = label
        }
    }

    fun openAppDrawer() {
        if (appDrawerOpen) {
            appDrawerOpen = false
            return
        }
        cancelHold()
        recentsOpen = false
        actionBoxOpen = false
        systemNotifDropdownOpen = false
        openFolderId = null
        refreshSystemRecents()
        appDrawerOpen = true
    }

    fun openActionBox(showCommandMenu: Boolean = false) {
        recentsOpen = false
        appDrawerOpen = false
        actionBoxOpen = true
        eatActionEnter = true
        commandPickerOpen = showCommandMenu && useHelpWindow
        if (commandPickerOpen) {
            actionQuery = ""
            clearCompose()
        }
    }

    fun applyHelpCommand(symbol: String) {
        commandPickerOpen = false
        when (symbol) {
            "/ls" -> openHomeSettings()
            "/ss" -> openSystemSettings()
            "/abs" -> openActionBoxSettings()
            "/h" -> { }
            "+" -> onQueryChange("+ ")
            else -> {
                val inject = if (symbol.startsWith("/") || symbol.any { it.isLetter() }) {
                    "$symbol "
                } else {
                    symbol
                }
                onQueryChange(inject)
            }
        }
    }

    fun closeActionBox() {
        actionBoxOpen = false
        eatActionEnter = true
        actionQuery = ""
        searchScope = null
        instantAnswer = null
        instantAnswerJob?.cancel()
        onDeviceTranslate = OnDeviceTranslateUi.Idle
        onDeviceTranslateJob?.cancel()
        clearCompose()
        helpOpen = false
        commandPickerOpen = false
        if (settingsFromActionBox) {
            settingsRoute = null
            settingsFromActionBox = false
        }
    }

    fun playBarrelRoll() {
        barrelRollGen++
        viewModelScope.launch {
            delay(1_600)
            closeActionBox()
        }
    }

    fun openRecents() {
        appDrawerOpen = false
        actionBoxOpen = false
        if (useMadRecents) {
            refreshSystemRecents()
            recentsOpen = true
            return
        }
        recentsOpen = false
        val ok = SystemShadeController.expandRecents(getApplication())
        if (!ok) showToast("Could not open Recents")
    }

    fun closeRecents() {
        recentsOpen = false
    }

    fun dismissLauncherWindows() {
        appDrawerOpen = false
        actionBoxOpen = false
        recentsOpen = false
        helpOpen = false
        settingsRoute = null
        syncAppearanceDraft()
        if (!SetupWindowPolicy.keepOpen(
                setupOpen,
                store.setupDismissed,
                grants.requiredDone,
                independentActionBox,
            ) && !com.madlauncher.LauncherWindowDismiss.shouldPreserve()
        ) {
            setupOpen = false
        }
        searchScope = null
        actionQuery = ""
        clearCompose()
        commandPickerOpen = false
        appMenu = null
        widgetPickerOpen = false
        newsPickerOpen = false
        systemNotifDropdownOpen = false
        notificationsOpen = false
        quickSettingsOpen = false
        binderFilter = null
        qsEditing = false
        qsReplaceId = null
        dndDurationMenuOpen = false
        openFolderId = null
        cancelHold()
        if (homeEditMode) exitHomeEdit()
        if (editingWidgetId != null) exitWidgetEdit()
        collapseEdge()
        for (i in floatingConvs.indices) {
            if (!floatingConvs[i].minimized) {
                floatingConvs[i] = floatingConvs[i].copy(minimized = true)
            }
        }
    }

    fun homePressed() {
        dismissLauncherWindows()
        homePageIndex = designatedHomeIndex()
        persistLastHomePageIndex()
        if (!dockSecondRowExpanded) dockRevealed = false
    }

    fun handleBack(): Boolean {
        when {
            appMenu != null -> appMenu = null
            appHold != null -> cancelHold()
            widgetBindIntent != null || widgetConfigIntent != null -> cancelPendingAppWidget()
            widgetPickerOpen -> {
                widgetPickerOpen = false
                cancelPendingAppWidget()
            }
            newsPickerOpen -> newsPickerOpen = false
            openFolderId != null -> openFolderId = null
            homeEditMode -> exitHomeEdit()
            editingWidgetId != null -> exitWidgetEdit()
            expandedEdge != null -> collapseEdge()
            setupOpen -> closeSetup()
            helpOpen -> helpOpen = false
            settingsRoute != null -> settingsBack()
            actionBoxOpen -> closeActionBox()
            appDrawerOpen -> appDrawerOpen = false
            dockSecondRowExpanded -> dockSecondRowExpanded = false
            recentsOpen -> closeRecents()
            notificationsOpen -> notificationsOpen = false
            systemNotifDropdownOpen -> closeSystemNotifShade()
            quickSettingsOpen -> quickSettingsOpen = false
            floatingConvs.any { !it.minimized } -> {
                for (i in floatingConvs.indices) {
                    if (!floatingConvs[i].minimized) {
                        floatingConvs[i] = floatingConvs[i].copy(minimized = true)
                    }
                }
            }
            else -> return false
        }
        return true
    }

    fun setBinder(packageName: String?) {
        binderFilter = if (binderFilter == packageName) null else packageName
    }

    fun dismissNotification(id: String) {
        val conv = notifSource.firstOrNull { it.key == id }?.conversationId
        val keys = if (conv != null) {
            notifSource.filter { it.conversationId == conv }.map { it.key }
        } else {
            listOf(id)
        }.distinct()
        keys.forEach { key ->
            if (key !in dismissedIds) dismissedIds.add(key)
            if (key !in readIds) readIds.add(key)
            NotificationHub.markDismissed(key)
        }
        liveNotifs = liveNotifs.filterNot { it.key in keys.toSet() }
        persistDismissedNotifs()
    }

    /** Dismiss every notification currently listed in the Notifications window (respects binder filter / pane). */
    fun dismissAllMessagingNotifs() {
        if (binderFilter == null && notifPane == NotifPane.System) {
            systemNotifs.forEach { dismissNotification(it.key) }
            return
        }
        val keys = displayedNotifs.flatMap { row ->
            notifSource.filter { it.conversationId == row.conversationId }.map { it.key }
        }.distinct()
        if (keys.isEmpty()) {
            // Fallback: clear everything currently shown in the filtered source for this window.
            val pkgs = installedMessagingPkgs
            val filter = binderFilter
            val fallback = filteredSource().filter { n ->
                if (filter != null) n.packageName == filter
                else NotifClassifier.isMessagingWindow(n, pkgs)
            }.map { it.key }
            fallback.forEach { dismissNotification(it) }
            return
        }
        keys.forEach { dismissNotification(it) }
    }

    fun markAsRead(id: String) {
        val conv = notifSource.firstOrNull { it.key == id }?.conversationId
        val keys = if (conv != null) {
            notifSource.filter { it.conversationId == conv }.map { it.key }
        } else {
            listOf(id)
        }.distinct()
        if (conv != null) {
            NotificationHub.markConversationDismissed(conv)
        }
        keys.forEach { key ->
            if (key !in readIds) readIds.add(key)
            if (key !in dismissedIds) dismissedIds.add(key)
            NotificationHub.requestMarkRead(key)
        }
        // Also clear from the MAD notifications window + icon badge.
        liveNotifs = liveNotifs.filterNot { it.key in keys.toSet() || (conv != null && it.conversationId == conv) }
        persistDismissedNotifs()
    }

    fun floatNotification(item: LiveNotif) {
        // Keep notification payloads for the floating thread; only clear icon badges.
        val keys = notifSource.filter { it.conversationId == item.conversationId }.map { it.key }
            .ifEmpty { listOf(item.key) }
        keys.forEach { key -> if (key !in readIds) readIds.add(key) }
        if (floatingConvs.none { it.conversationId == item.conversationId }) {
            val n = floatingConvs.size
            floatingConvs.add(
                FloatingConv(
                    conversationId = item.conversationId,
                    minimized = true,
                    x = 36f + n * 18f,
                    y = 160f + n * 28f,
                    w = floatNotifW,
                    h = floatNotifH,
                ),
            )
        }
    }

    fun resizeFloating(conversationId: String, w: Float, h: Float) {
        val idx = floatingConvs.indexOfFirst { it.conversationId == conversationId }
        if (idx >= 0) floatingConvs[idx] = floatingConvs[idx].copy(w = w, h = h)
        floatNotifW = w
        floatNotifH = h
        store.floatNotifW = w
        store.floatNotifH = h
    }

    fun moveFloating(conversationId: String, x: Float, y: Float) {
        val idx = floatingConvs.indexOfFirst { it.conversationId == conversationId }
        if (idx >= 0) floatingConvs[idx] = floatingConvs[idx].copy(x = x, y = y)
    }

    fun minimizeFloating(conversationId: String, minimized: Boolean) {
        val idx = floatingConvs.indexOfFirst { it.conversationId == conversationId }
        if (idx >= 0) floatingConvs[idx] = floatingConvs[idx].copy(minimized = minimized)
    }

    fun closeFloating(conversationId: String) {
        floatingConvs.removeAll { it.conversationId == conversationId }
        notifSource.filter { it.conversationId == conversationId }.forEach { markAsRead(it.key) }
    }

    fun sendReply(item: LiveNotif, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val sent = NotificationHub.reply(getApplication(), item.key, item.conversationId, trimmed)
        if (sent) {
            val prev = outgoingByConversation[item.conversationId].orEmpty()
            outgoingByConversation[item.conversationId] = prev + "You: $trimmed"
            // Reply counts as read — drop from Notifications / per-app windows immediately.
            // Many apps also cancel the status-bar notif; listener sync covers the rest.
            val keys = notifSource.filter { it.conversationId == item.conversationId }.map { it.key }
                .ifEmpty { listOf(item.key) }
                .distinct()
            keys.forEach { key ->
                if (key !in readIds) readIds.add(key)
                if (key !in dismissedIds) dismissedIds.add(key)
                NotificationHub.markDismissed(key)
            }
            liveNotifs = liveNotifs.filterNot { it.key in keys.toSet() }
            floatingConvs.removeAll { it.conversationId == item.conversationId }
            persistDismissedNotifs()
            showToast("Sent")
            return
        }
        val opened = NotificationHub.open?.let { runCatching { it(item.key); true }.getOrDefault(false) } ?: false
        if (!opened) {
            runCatching { intents.launchPackage(item.packageName) }
        }
        showToast("Inline reply unavailable — opened ${item.appName}")
    }

    fun toggleQsEditing() {
        qsEditing = !qsEditing
        if (!qsEditing) qsReplaceId = null
    }

    fun moveQsTile(id: String, delta: Int) {
        val list = toggles.toMutableList()
        val from = list.indexOfFirst { it.id == id }
        if (from < 0) return
        val to = (from + delta).coerceIn(0, list.lastIndex)
        if (from == to) return
        val item = list.removeAt(from)
        list.add(to, item)
        toggles = list
        persistQsTiles()
    }

    fun removeQsTile(id: String) {
        if (toggles.size <= 1) return
        toggles = toggles.filterNot { it.id == id }
        if (qsReplaceId == id) qsReplaceId = null
        persistQsTiles()
    }

    fun addQsTile(id: String) {
        val replace = qsReplaceId
        val ctx = getApplication<Application>()
        val discovered = SystemQsMirror.discoverExtraTiles(ctx, emptySet())
            .firstOrNull { it.id == id }
        val catalog = QsCatalog.spec(id)
        if (discovered == null && catalog == null && !id.startsWith("custom:")) return
        val label = discovered?.label ?: catalog?.label ?: id
        val systemSpec = discovered?.spec
            ?: SystemQsMirror.specForId(id)
            ?: id
        val newTile = ToggleItem(id, label, readTileEnabled(ctx, id, systemSpec), systemSpec)
        if (replace != null && toggles.any { it.id == replace }) {
            toggles = toggles.map { item ->
                if (item.id == replace) newTile else item
            }
            qsReplaceId = null
        } else if (toggles.none { it.id == id }) {
            toggles = toggles + newTile
        }
        persistQsTiles()
    }

    fun launchApp(app: LaunchableApp) {
        appDrawerOpen = false
        recentsOpen = false
        // Automatic side docks tuck away after launching an app from them.
        if (edgePanels.any { it.autoHide == DockAutoHide.AUTOMATIC }) {
            collapseEdge()
        }
        dockRevealed = false
        dockSecondRowExpanded = false
        if (LaunchableApp.isMadSettings(app.packageName, app.activityName, getApplication<Application>().packageName)) {
            openHomeSettings()
            return
        }
        recents.removeAll { it.id == app.id }
        recents.add(0, app)
        while (recents.size > 12) recents.removeAt(recents.lastIndex)
        store.recentAppIds = recents.map { it.id }
        usageTracker.recordLaunch(app.packageName)
        publishDrawerRecentFromLauncherRecents(app.packageName)
        refreshSystemRecents()
        refreshFrequentApps()
        if (app.packageName in installedMessagingPkgs) {
            clearPackageNotifications(app.packageName)
        }
        runCatching { appsRepo.launch(app) }
            .onFailure { showToast("Couldn't open ${app.label}") }
    }

    /** Opening a messaging/email app clears shade + MAD unread for that package (native-like). */
    private fun clearPackageNotifications(packageName: String) {
        val keys = notifSource.filter { it.packageName == packageName }.map { it.key }
        keys.forEach { key ->
            if (key !in readIds) readIds.add(key)
            if (key !in dismissedIds) dismissedIds.add(key)
        }
        NotificationHub.requestClearPackage(packageName)
        liveNotifs = liveNotifs.filterNot { it.packageName == packageName }
        persistDismissedNotifs()
    }

    fun launchShortcut(item: HomeItem) {
        if (!item.isShortcut) return
        appDrawerOpen = false
        recentsOpen = false
        var launched = false
        if (item.intentUri.isNotBlank()) {
            val intent = runCatching {
                Intent.parseUri(item.intentUri, Intent.URI_INTENT_SCHEME)
            }.getOrNull()
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                rememberShortcutLaunch(item, intent)
                runCatching { app.startActivity(intent) }
                    .onFailure { showToast("Couldn't open ${item.title.ifBlank { "shortcut" }}") }
                launched = true
            }
        }
        if (!launched && item.packageName.isNotBlank() && item.activityName.isNotBlank() && Build.VERSION.SDK_INT >= 25) {
            val launcherApps = app.getSystemService(android.content.pm.LauncherApps::class.java)
            rememberShortcutLaunch(item, null)
            runCatching {
                launcherApps?.startShortcut(
                    item.packageName,
                    item.activityName,
                    null,
                    null,
                    android.os.Process.myUserHandle(),
                )
            }.onFailure { showToast("Couldn't open ${item.title.ifBlank { "shortcut" }}") }
            launched = true
        }
        if (!launched) {
            showToast("Shortcut is incomplete")
            return
        }
        if (RecentAppsMerge.isTrampolinePackage(item.packageName)) {
            viewModelScope.launch {
                delay(800)
                refreshSystemRecents()
            }
        }
    }

    private fun rememberShortcutLaunch(item: HomeItem, intent: Intent?) {
        val resolved = intent?.let {
            runCatching {
                app.packageManager.resolveActivity(it, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                    ?.activityInfo?.packageName
            }.getOrNull()
        }
        val pkg = RecentAppsMerge.packageFromShortcut(
            itemPackage = item.packageName,
            intentPackage = intent?.`package`,
            componentPackage = intent?.component?.packageName,
            resolvedPackage = resolved,
        ) ?: return
        usageTracker.recordLaunch(pkg)
        bumpRecent(pkg)
        refreshSystemRecents()
    }

    private fun bumpRecent(packageName: String) {
        val found = apps.firstOrNull { it.packageName == packageName }
            ?: usageTracker.resolveLaunchable(packageName)
            ?: return
        recents.removeAll { it.packageName == packageName }
        recents.add(0, found)
        while (recents.size > RecentAppsMerge.RECENTS_LIMIT) recents.removeAt(recents.lastIndex)
        store.recentAppIds = recents.map { it.id }
        publishDrawerRecentFromLauncherRecents(packageName)
    }

    private fun publishDrawerRecentFromLauncherRecents(openedPackage: String? = null) {
        val recentsPkgs = recents.map { it.packageName }
        val stillNew = drawerRecent
            .filter { it.newlyInstalled && it.app.packageName != openedPackage }
            .map { it.app }
        val byPkg = (recents + stillNew).associateBy { it.packageName }
        drawerRecent = RecentAppsMerge.drawerRow(
            recentsNewestFirst = recentsPkgs,
            neverOpenedInstallsNewestFirst = stillNew.map { it.packageName },
        ).mapNotNull { slot ->
            val app = byPkg[slot.packageName] ?: return@mapNotNull null
            AppUsageTracker.DrawerRecentApp(app, slot.newlyInstalled)
        }
    }

    fun dockPhone() {
        runCatching { appsRepo.openDialer() }
            .onFailure { showToast("No dialer available") }
    }

    fun dockBrowser() {
        runCatching { appsRepo.openBrowser() }
            .onFailure { showToast("No browser available") }
    }

    fun dockSettings() {
        runCatching { appsRepo.openSettings() }
            .onFailure { showToast("Couldn't open settings") }
    }

    fun activateDockSlot(index: Int) {
        if (homeEditMode) exitHomeEdit()
        when (val slot = dockSlotAt(index)) {
            "phone" -> dockPhone()
            "browser" -> dockBrowser()
            "actions" -> openActionBox()
            "apps" -> openAppDrawer()
            "settings", "launcher_settings" -> openHomeSettings()
            else -> {
                val parsed = HomeLayout.parseAppSlot(slot) ?: return
                resolveApp(parsed.first, parsed.second)?.let { launchApp(it) }
            }
        }
    }

    private fun migrateDockDefaults() {
        val next = dockSlots.toMutableList()
        var changed = false
        next.indices.forEach { i ->
            when (next[i]) {
                "phone" -> appsRepo.defaultDialer()?.let {
                    next[i] = HomeLayout.appSlot(it)
                    changed = true
                }
                "browser" -> appsRepo.defaultBrowser()?.let {
                    next[i] = HomeLayout.appSlot(it)
                    changed = true
                }
                "settings" -> {
                    next[i] = "launcher_settings"
                    changed = true
                }
            }
        }
        if (!displayAppearance.showDockActionBox) {
            next.indices.forEach { i -> if (next[i] == "actions") { next[i] = ""; changed = true } }
        } else if (next.none { it == "actions" } && displayAppearance.showDockActionBox) {
            val empty = next.indexOfFirst { it.isBlank() }
            if (empty >= 0) {
                next[empty] = "actions"
                changed = true
            }
        }
        if (!displayAppearance.showDockApps) {
            next.indices.forEach { i -> if (next[i] == "apps") { next[i] = ""; changed = true } }
        } else if (next.none { it == "apps" } && displayAppearance.showDockApps) {
            val empty = next.indexOfFirst { it.isBlank() }
            if (empty >= 0) {
                next[empty] = "apps"
                changed = true
            }
        }
        if (changed) {
            dockSlots = next
            store.dockSlots = next
        }
    }

    fun activeDockTargets(): List<String> {
        val targets = mutableListOf("bottom")
        if (hasEdgePanel(EdgeSide.LEFT)) targets += "left"
        if (hasEdgePanel(EdgeSide.RIGHT)) targets += "right"
        if (hasEdgePanel(EdgeSide.TOP)) targets += "top"
        return targets
    }

    fun requestAddToDock(app: LaunchableApp) {
        val targets = activeDockTargets()
        if (targets.size <= 1) {
            addAppToDock(app, "bottom")
            appMenu = null
        } else {
            dockPickApp = app
            appMenu = null
        }
    }

    fun addAppToDock(app: LaunchableApp, target: String) {
        dockPickApp = null
        when (target) {
            "left" -> addToEdge(app, EdgeSide.LEFT)
            "right" -> addToEdge(app, EdgeSide.RIGHT)
            "top" -> addToEdge(app, EdgeSide.TOP)
            else -> {
                val limit = if (displayAppearance.dockSecondRow) HomeLayout.DOCK_ROW_SLOTS * 2 else HomeLayout.DOCK_ROW_SLOTS
                val slots = dockSlotsCombined()
                val idx = slots.take(limit).indexOfFirst { it.isBlank() }.takeIf { it >= 0 }
                    ?: slots.take(limit).indexOfFirst { it !in HomeLayout.protectedDock && it != "launcher_settings" }.takeIf { it >= 0 }
                if (idx == null) {
                    showToast("No space on the dock")
                    return
                }
                placeOnDock(AppHold(app, "drawer"), idx)
            }
        }
    }

    private fun addToEdge(app: LaunchableApp, side: EdgeSide) {
        if (!hasEdgePanel(side)) addEdgePanel(side)
        val panel = edgePanels.firstOrNull { it.side == side } ?: return
        val empty = panel.slots.indexOfFirst { it.isBlank() }
        if (empty < 0) {
            showToast("No space on this dock")
            return
        }
        placeOnEdge(AppHold(app, "drawer"), side, empty)
    }

    fun disableActionBoxOnDock() {
        appMenu = null
        val next = dockSlots.toMutableList()
        next.indices.forEach { i -> if (next[i] == "actions") next[i] = "" }
        dockSlots = next
        store.dockSlots = next
        patchAppearance { it.copy(showDockActionBox = false) }
        showToast(overlayString(R.string.toast_action_box_disabled))
    }

    fun applyDockToggles() {
        migrateDockDefaults()
    }

    fun resolveApp(packageName: String, activityName: String): LaunchableApp? {
        apps.firstOrNull { it.packageName == packageName && it.activityName == activityName }?.let { return it }
        val self = getApplication<Application>().packageName
        if (LaunchableApp.isMadSettings(packageName, activityName, self)) {
            return apps.firstOrNull {
                it.packageName.equals(self, ignoreCase = true) && it.activityName == LaunchableApp.SETTINGS_ACTIVITY
            }
        }
        return apps.firstOrNull { it.packageName == packageName }
    }

    fun resolveDockApp(index: Int): LaunchableApp? {
        val parsed = HomeLayout.parseAppSlot(dockSlotAt(index)) ?: return null
        return resolveApp(parsed.first, parsed.second)
    }

    /** True when this swipe already revealed the extra dock row (don't also open the drawer). */
    fun onDockPullUp(): Boolean {
        if (displayAppearance.dockSecondRow && !dockSecondRowExpanded) {
            expandDockSecondRow()
            return true
        }
        return false
    }

    fun onDockSwipeUp() {
        if (onDockPullUp()) return
        if (displayAppearance.dockSecondRow) {
            com.madlauncher.LauncherCommands.suppressHomeBriefly(1_500L)
        }
        runGestureAction(GestureId.SWIPE_UP)
    }

    private fun expandDockSecondRow() {
        dockRevealed = true
        dockSecondRowExpanded = true
        com.madlauncher.LauncherCommands.suppressHomeBriefly(1_500L)
    }

    fun onDockSwipeDown() {
        if (displayAppearance.dockSecondRow && dockSecondRowExpanded) {
            dockSecondRowExpanded = false
        } else {
            runGestureAction(GestureId.SWIPE_DOWN)
        }
    }

    fun dockSlotAt(index: Int): String {
        val row = HomeLayout.DOCK_ROW_SLOTS
        return if (index < row) dockSlots.getOrNull(index).orEmpty()
        else dockRow2Slots.getOrNull(index - row).orEmpty()
    }

    private fun padDockRow(list: List<String>): MutableList<String> {
        val row = HomeLayout.DOCK_ROW_SLOTS
        val next = list.take(row).toMutableList()
        while (next.size < row) next.add("")
        return next
    }

    private fun dockSlotsCombined(): MutableList<String> =
        (padDockRow(dockSlots) + padDockRow(dockRow2Slots)).toMutableList()

    private fun commitDockSlots(combined: List<String>) {
        val row = HomeLayout.DOCK_ROW_SLOTS
        val padded = combined.toMutableList()
        while (padded.size < row * 2) padded.add("")
        dockSlots = padded.take(row)
        store.dockSlots = dockSlots
        dockRow2Slots = padded.drop(row).take(row)
        store.dockRow2Slots = dockRow2Slots
    }

    fun enterHomeEdit() {
        homeEditMode = true
        editingWidgetId = null
        appMenu = null
        cancelHold()
        actionBoxOpen = false
        appDrawerOpen = false
        widgetPickerOpen = false
        newsPickerOpen = false
    }

    fun exitHomeEdit() {
        homeEditMode = false
        widgetResizePreview = null
        widgetMovePreview = null
        widgetSwapPartnerId = null
    }

    fun enterWidgetEdit(id: String) {
        editingWidgetId = id
        homePagerLocked = true
        homeEditMode = false
        appMenu = null
        cancelHold()
        actionBoxOpen = false
        appDrawerOpen = false
        widgetPickerOpen = false
    }

    fun exitWidgetEdit() {
        editingWidgetId = null
        widgetResizePreview = null
        widgetMovePreview = null
        widgetSwapPartnerId = null
        homePagerLocked = false
    }

    fun hasEdgePanel(side: EdgeSide): Boolean = edgePanels.any { it.side == side }

    fun toggleEdgePanel(side: EdgeSide) {
        if (hasEdgePanel(side)) removeEdgePanel(side) else addEdgePanel(side)
    }

    fun addEdgePanel(side: EdgeSide) {
        if (hasEdgePanel(side)) return
        edgePanels = edgePanels + EdgePanel(side)
        store.edgePanels = edgePanels
        expandedEdge = side
        exitHomeEdit()
        showToast("Drag apps from the drawer onto the MAD Dock")
        com.madlauncher.overlay.EdgeDockOverlay.sync(getApplication())
    }

    fun patchEdgePanel(side: EdgeSide, transform: (EdgePanel) -> EdgePanel) {
        if (!hasEdgePanel(side)) return
        val previous = edgePanels.firstOrNull { it.side == side }
        edgePanels = edgePanels.map { if (it.side == side) transform(it) else it }
        store.edgePanels = edgePanels
        val next = edgePanels.firstOrNull { it.side == side } ?: return
        when (next.autoHide.forUi()) {
            DockAutoHide.REGULAR -> {
                if (expandedEdge == side) expandedEdge = null
            }
            DockAutoHide.AUTOMATIC -> {
                if (previous?.autoHide?.forUi() != DockAutoHide.AUTOMATIC) {
                    expandedEdge = null
                    clearEdgeSlotRects(side)
                }
            }
            else -> Unit
        }
        com.madlauncher.overlay.EdgeDockOverlay.sync(getApplication())
    }

    fun edgePanel(side: EdgeSide): EdgePanel? = edgePanels.firstOrNull { it.side == side }

    fun edgeShouldShowExpanded(panel: EdgePanel): Boolean {
        // While editing docks, always show so width/icon/hide previews are visible.
        if (settingsRoute == com.madlauncher.ui.settings.SettingsRoute.EDIT_DOCKS) return true
        return when (panel.autoHide.forUi()) {
            DockAutoHide.REGULAR -> true
            DockAutoHide.AUTOMATIC -> expandedEdge == panel.side
            else -> true
        }
    }

    fun removeEdgePanel(side: EdgeSide) {
        edgePanels = edgePanels.filterNot { it.side == side }
        store.edgePanels = edgePanels
        edgeDropRects.keys.filter { it.startsWith("${side.key}:") }.forEach { edgeDropRects.remove(it) }
        if (expandedEdge == side) expandedEdge = null
        exitHomeEdit()
        showToast("MAD Dock removed")
        com.madlauncher.overlay.EdgeDockOverlay.sync(getApplication())
    }

    fun toggleEdgeExpanded(side: EdgeSide) {
        expandedEdge = if (expandedEdge == side) {
            clearEdgeSlotRects(side)
            null
        } else side
    }

    fun collapseEdge() {
        expandedEdge = null
        edgePanels.forEach { clearEdgeSlotRects(it.side) }
    }

    private fun clearEdgeSlotRects(side: EdgeSide) {
        edgeDropRects.keys.filter { it.startsWith("${side.key}:") && !it.endsWith("handle") }
            .toList()
            .forEach { edgeDropRects.remove(it) }
    }

    fun resolveEdgeApp(side: EdgeSide, index: Int): LaunchableApp? {
        val slot = edgePanels.firstOrNull { it.side == side }?.slots?.getOrNull(index).orEmpty()
        val parsed = HomeLayout.parseAppSlot(slot) ?: return null
        return resolveApp(parsed.first, parsed.second)
    }

    fun persistEdgePanels() {
        store.edgePanels = edgePanels
        com.madlauncher.overlay.EdgeDockOverlay.sync(getApplication())
    }

    fun openHomeSettings() {
        syncAppearanceDraft()
        exitHomeEdit()
        settingsFromActionBox = independentActionBox
        settingsRoute = SettingsRoute.HUB
    }

    fun openSystemSettings() {
        exitHomeEdit()
        dockSettings()
    }

    fun syncAppearanceDraft() {
        appearanceDraft = appearanceSettings
    }

    fun patchAppearanceDraft(transform: (AppearanceSettings) -> AppearanceSettings) {
        applyAppearance(transform(appearanceDraft), persist = true)
    }

    fun openSettingsRoute(route: SettingsRoute) {
        if (route != SettingsRoute.ACTION_BOX_COMMAND_EDIT) commandEditId = null
        settingsRoute = route
        refreshPlatformProfile()
    }

    fun openCommandEdit(id: CommandEditId) {
        commandEditId = id
        settingsRoute = SettingsRoute.ACTION_BOX_COMMAND_EDIT
    }

    fun openCommandEditFromActionBox(id: CommandEditId) {
        settingsFromActionBox = true
        openCommandEdit(id)
    }

    fun closeSettings() {
        settingsRoute = null
        commandEditId = null
        settingsFromActionBox = false
        syncAppearanceDraft()
    }

    fun settingsBack() {
        if (settingsRoute == SettingsRoute.ACTION_BOX_COMMAND_EDIT) {
            commandEditId = null
            settingsRoute = SettingsRoute.ACTION_BOX_COMMANDS
            return
        }
        val next = SettingsBackStack.parent(
            settingsRoute,
            settingsFromActionBox,
            independentActionBox,
        )
        if (next == null) {
            settingsRoute = null
            commandEditId = null
            settingsFromActionBox = false
            syncAppearanceDraft()
            return
        }
        settingsRoute = next
    }

    fun patchAppearance(transform: (AppearanceSettings) -> AppearanceSettings) {
        applyAppearance(transform(appearanceSettings))
    }

    fun applyAppearance(next: AppearanceSettings, persist: Boolean = true) {
        val sanitized = MadShadeWindows.sanitize(next).let { s ->
            s.copy(
                themeMode = ThemeMode.SYSTEM,
                palette = AppearanceSettings.normalizePalette(s.palette),
            )
        }
        val overlay = LanguageCodes.migrateFileName(sanitized.languageOverlay)
        val applied = if (overlay == sanitized.languageOverlay) sanitized else sanitized.copy(languageOverlay = overlay)
        appearanceSettings = applied
        appearanceDraft = applied
        if (!applied.dockSecondRow) dockSecondRowExpanded = false
        if (persist) store.appearanceSettings = applied
        if (!com.madlauncher.BuildConfig.ACTION_BOX_APP) {
            NavShortcutBridge.update(applied)
            AppearanceBridge.update(applied)
            applyNavShortcutSystemPolicy()
            migrateDockDefaults()
        }
        if (com.madlauncher.BuildConfig.ACTION_BOX_APP) {
            LanguageOverlay.applySystemLanguage(app)
        } else if (LanguageOverlay.selectedFile != applied.languageOverlay) {
            LanguageOverlay.load(app, applied.languageOverlay)
        }
    }

    var remoteLanguageCatalog by mutableStateOf<List<LanguagePackEntry>>(emptyList())
        private set
    var remoteLanguageLoading by mutableStateOf(false)
        private set
    var languageCatalogError by mutableStateOf<String?>(null)
        private set
    var languageDownloadingFile by mutableStateOf<String?>(null)
        private set
    var languageDownloadError by mutableStateOf<String?>(null)
        private set
    var installedLanguageLabels by mutableStateOf<Map<String, String>>(emptyMap())
        private set
    var installedLanguageRevision by mutableStateOf(0)
        private set
    var installedLanguagePacks by mutableStateOf<List<LanguagePackEntry>>(emptyList())
        private set

    fun refreshInstalledLanguagePacks() {
        val labels = remoteLanguageCatalog.associate { it.fileName to it.label } + installedLanguageLabels
        installedLanguagePacks = LanguageCatalog.installedEntries(getApplication(), labels)
    }

    fun setLanguageOverlay(fileName: String) {
        if (fileName.isNotBlank()) {
            val destName = LanguageCodes.migrateFileName(fileName)
            val dest = java.io.File(LanguageOverlay.deviceDir(getApplication()), destName)
            if (!dest.exists() || LanguageOverlay.parse(dest).isEmpty()) {
                LanguageOverlay.installBundledPack(getApplication(), fileName)
            }
        }
        applyAppearance(appearanceSettings.copy(languageOverlay = fileName), persist = true)
        appearanceEpoch++
        installedLanguageRevision++
        refreshInstalledLanguagePacks()
    }

    fun openEditDocksFromControl() {
        cancelHold()
        appMenu = null
        appDrawerOpen = false
        recentsOpen = false
        closeActionBox()
        syncAppearanceDraft()
        if (homeEditMode) exitHomeEdit()
        settingsFromActionBox = independentActionBox
        openSettingsRoute(SettingsRoute.EDIT_DOCKS)
    }

    fun refreshRemoteLanguageCatalog() {
        viewModelScope.launch {
            remoteLanguageLoading = true
            languageCatalogError = null
            LanguageCatalog.fetchAvailable(getApplication())
                .onSuccess { list ->
                    remoteLanguageCatalog = list
                    if (list.isEmpty()) {
                        languageCatalogError = overlayString(R.string.toast_language_list_empty)
                    } else {
                        syncLanguagePackIfRemoteChanged(list)
                    }
                }
                .onFailure { languageCatalogError = it.message ?: overlayString(R.string.extras_download_failed) }
            remoteLanguageLoading = false
        }
    }

    /** Download the active overlay from GitHub only when the remote blob SHA differs. */
    private suspend fun syncLanguagePackIfRemoteChanged(catalog: List<LanguagePackEntry>) {
        val selected = appearanceSettings.languageOverlay
        if (selected.isBlank()) return
        val remote = catalog.firstOrNull { it.fileName.equals(selected, ignoreCase = true) } ?: return
        val remoteSha = remote.sha ?: return
        val ctx = getApplication<Application>()
        val dest = java.io.File(LanguageOverlay.deviceDir(ctx), remote.fileName)
        val localSha = if (dest.exists()) LanguageCatalog.gitBlobSha(dest.readBytes()) else null
        if (localSha == remoteSha) return
        LanguageCatalog.downloadPack(ctx, remote).onSuccess {
            installedLanguageRevision++
            refreshInstalledLanguagePacks()
            LanguageOverlay.load(ctx, remote.fileName)
            appearanceEpoch++
        }
    }

    fun downloadAndApplyLanguage(entry: LanguagePackEntry, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            languageDownloadingFile = entry.fileName
            languageDownloadError = null
            val ctx = getApplication<Application>()
            val dest = java.io.File(LanguageOverlay.deviceDir(ctx), entry.fileName)
            val localSha = dest.takeIf { it.exists() }?.let { LanguageCatalog.gitBlobSha(it.readBytes()) }
            val remoteSha = entry.sha
            if (remoteSha != null && localSha == remoteSha) {
                installedLanguageLabels = installedLanguageLabels + (entry.fileName to entry.label)
                installedLanguageRevision++
                refreshInstalledLanguagePacks()
                setLanguageOverlay(entry.fileName)
                showToast(overlayString(R.string.toast_language_installed, entry.label))
                onComplete(true)
                languageDownloadingFile = null
                return@launch
            }
            val bundledSha = LanguageCatalog.bundledBlobSha(ctx, entry.fileName)
            val remoteMatchesBundled = remoteSha == null || (bundledSha != null && remoteSha == bundledSha)
            if (remoteMatchesBundled && LanguageOverlay.installBundledPack(ctx, entry.fileName)) {
                installedLanguageLabels = installedLanguageLabels + (entry.fileName to entry.label)
                installedLanguageRevision++
                refreshInstalledLanguagePacks()
                setLanguageOverlay(entry.fileName)
                showToast(overlayString(R.string.toast_language_installed, entry.label))
                onComplete(true)
                languageDownloadingFile = null
                return@launch
            }
            showToast(overlayString(R.string.toast_language_downloading, entry.label))
            LanguageCatalog.downloadPack(ctx, entry)
                .onSuccess {
                    installedLanguageLabels = installedLanguageLabels + (entry.fileName to entry.label)
                    installedLanguageRevision++
                    refreshInstalledLanguagePacks()
                    setLanguageOverlay(entry.fileName)
                    showToast(overlayString(R.string.toast_language_installed, entry.label))
                    onComplete(true)
                }
                .onFailure { err ->
                    val msg = err.message ?: overlayString(R.string.extras_download_failed)
                    languageDownloadError = msg
                    showToast(msg)
                    onComplete(false)
                }
            languageDownloadingFile = null
        }
    }

    private fun applyNavShortcutSystemPolicy() {
        com.madlauncher.notif.SystemShadeController.syncWithAppearance(app, appearanceSettings)
    }

    fun setExperimentalMadWindows(enabled: Boolean) {
        val next = MadShadeWindows.sanitize(
            appearanceSettings.copy(experimentalMadWindows = enabled),
        )
        applyAppearance(next)
        if (!enabled) {
            notificationsOpen = false
            quickSettingsOpen = false
            systemNotifDropdownOpen = false
        }
        NavShortcutBridge.requestRebuild()
    }

    fun setExperimentalHelpWindow(enabled: Boolean) {
        val next = appearanceSettings.copy(experimentalHelpWindow = enabled)
        applyAppearance(next)
        if (!enabled) {
            helpOpen = false
            commandPickerOpen = false
        }
    }

    fun setExperimentalWidgetBadges(enabled: Boolean) {
        applyAppearance(appearanceSettings.copy(experimentalWidgetBadges = enabled))
    }

    fun setExperimentalNavShortcuts(enabled: Boolean) {
        val next = appearanceSettings.copy(experimentalNavShortcuts = enabled)
        applyAppearance(next)
        if (!enabled && settingsRoute == SettingsRoute.NAV_SHORTCUTS) {
            settingsRoute = SettingsRoute.EXPERIMENTAL
        }
        NavShortcutBridge.requestRebuild()
    }

    fun setExperimentalMadRecents(enabled: Boolean) {
        applyAppearance(appearanceSettings.copy(experimentalMadRecents = enabled))
        if (!enabled) recentsOpen = false
    }

    fun setWebSearchEngine(engine: WebSearchEngine) {
        appearanceSettings = appearanceSettings.copy(webSearchEngine = engine)
        store.appearanceSettings = appearanceSettings
        appearanceDraft = appearanceDraft.copy(webSearchEngine = engine)
    }

    fun setInstantAnswersEnabled(enabled: Boolean) {
        appearanceSettings = appearanceSettings.copy(instantAnswersEnabled = enabled)
        store.appearanceSettings = appearanceSettings
        appearanceDraft = appearanceDraft.copy(instantAnswersEnabled = enabled)
        if (!enabled) {
            instantAnswerJob?.cancel()
            instantAnswer = instantAnswer?.takeIf { !it.fromDuckDuckGo }
        }
    }

    fun refreshWallpaperPalette() {
        wallpaperPalette = WallpaperColorExtractor.extract(app)
    }

    fun refreshIconPacks() {
        iconPacks = IconPackResolver.listInstalled(app)
    }

    fun setPageEffect(effect: PageEffectId) {
        val nextSettings = appearanceSettings.copy(pageEffect = effect)
        val nextDraft = if (settingsOpen) appearanceDraft.copy(pageEffect = effect) else nextSettings
        appearanceSettings = nextSettings
        appearanceDraft = nextDraft
        store.appearanceSettings = nextSettings
        AppearanceBridge.update(nextSettings)
        appearanceEpoch++
    }

    fun setInfiniteHomePages(enabled: Boolean) {
        val nextSettings = appearanceSettings.copy(infiniteHomePages = enabled)
        val nextDraft = if (settingsOpen) appearanceDraft.copy(infiniteHomePages = enabled) else nextSettings
        appearanceSettings = nextSettings
        appearanceDraft = nextDraft
        store.appearanceSettings = nextSettings
        AppearanceBridge.update(nextSettings)
        appearanceEpoch++
    }

    fun setIconShape(shape: IconShapeId) {
        // Apply immediately so home/dock/drawer update without a separate Apply tap.
        val nextSettings = appearanceSettings.copy(iconShape = shape)
        val nextDraft = if (settingsOpen) appearanceDraft.copy(iconShape = shape) else nextSettings
        appearanceSettings = nextSettings
        appearanceDraft = nextDraft
        store.appearanceSettings = nextSettings
        AppearanceBridge.update(nextSettings)
        appearanceEpoch++
    }

    fun setIconPack(packageName: String) {
        IconPackResolver.clearCache()
        applyAppearance(appearanceSettings.copy(iconPackPackage = packageName), persist = true)
        appearanceEpoch++
    }

    fun setCustomFont(path: String) {
        applyAppearance(appearanceSettings.copy(fontPath = path), persist = true)
        appearanceEpoch++
    }

    fun runGestureAction(gesture: GestureId) {
        runGestureAction(appearanceSettings.gestures[gesture] ?: GestureActionId.NONE)
    }

    fun runGestureAction(action: GestureActionId) {
        when (action) {
            GestureActionId.NONE -> Unit
            GestureActionId.APP_DRAWER -> openAppDrawer()
            GestureActionId.SEARCH -> openActionBox()
            GestureActionId.NOTIFICATIONS -> toggleNotifications()
            GestureActionId.SYSTEM_APPS -> toggleSystemNotifDropdown()
            GestureActionId.QUICK_SETTINGS -> toggleQuickSettings()
            GestureActionId.RECENTS -> openRecents()
            GestureActionId.CUSTOMIZE -> enterHomeEdit()
            GestureActionId.LOCK_SCREEN -> lockScreen()
            GestureActionId.HIDDEN_APPS -> showToast("Hidden apps filter coming soon")
        }
    }

    fun lockScreen() {
        val svc = com.madlauncher.a11y.MadAccessibilityService.instance
        if (svc != null) {
            runCatching {
                svc.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            }
            return
        }
        if (!com.madlauncher.platform.MadOsRuntime.isMadOs(getApplication())) {
            showToast("Enable accessibility for screen lock")
        }
    }

    fun resolvePackIcon(packageName: String, activityName: String): android.graphics.drawable.Drawable? {
        val pack = displayAppearance.iconPackPackage
        if (pack.isBlank()) return null
        return IconPackResolver.loadIcon(
            app,
            pack,
            android.content.ComponentName(packageName, activityName),
        )
    }

    fun addHomePage() {
        addHomePage(HomePageKind.EMPTY)
    }

    fun addHomePage(kind: HomePageKind) {
        if (kind == HomePageKind.NEWS) {
            viewModelScope.launch {
                val place = withContext(Dispatchers.IO) {
                    DevicePlaceFinder.current(app, grants.location)
                }
                newsPlace = place
                newsProviders = NewsCatalog.discover(app.packageManager, apps, place, grants.location)
                val providers = NewsCatalog.defaultSelection(
                    newsProviders,
                    apps.map { it.packageName }.toSet(),
                    place,
                    grants.location,
                )
                homePages = homePages + HomePage(
                    id = "p${System.currentTimeMillis()}",
                    items = emptyList(),
                    kind = kind,
                    newsProviders = providers,
                )
                store.homePages = homePages
                homePageIndex = homePages.lastIndex
                exitHomeEdit()
                refreshNews()
                newsPickerOpen = true
                showToast("News page added")
            }
            return
        }
        homePages = homePages + HomePage(
            id = "p${System.currentTimeMillis()}",
            items = emptyList(),
            kind = kind,
            newsProviders = emptyList(),
        )
        store.homePages = homePages
        homePageIndex = homePages.lastIndex
        exitHomeEdit()
        when (kind) {
            HomePageKind.EMPTY -> showToast("Empty page added")
            HomePageKind.FREQUENT -> {
                refreshFrequentApps()
                showToast("Most-used page added")
            }
            HomePageKind.NEWS -> Unit
        }
    }

    fun toggleFrequentSort() {
        val page = homePages.getOrNull(homePageIndex) ?: return
        if (page.kind != HomePageKind.FREQUENT) return
        replacePage(homePageIndex, page.copy(frequentSortTop = !page.frequentSortTop))
        refreshFrequentApps()
    }

    fun openNewsPicker() {
        refreshNewsProviders()
        exitHomeEdit()
        newsPickerOpen = true
    }

    fun toggleNewsProvider(id: String) {
        val page = homePages.getOrNull(homePageIndex) ?: return
        if (page.kind != HomePageKind.NEWS) return
        val next = if (id in page.newsProviders) page.newsProviders - id else page.newsProviders + id
        replacePage(homePageIndex, page.copy(newsProviders = next))
        refreshNews()
    }

    fun refreshNewsProviders() {
        // Geocoder can block for seconds — never call on main (was ANR on resume).
        viewModelScope.launch {
            val place = withContext(Dispatchers.IO) {
                DevicePlaceFinder.current(app, grants.location)
            }
            newsPlace = place
            newsProviders = NewsCatalog.discover(app.packageManager, apps, place, grants.location)
        }
    }

    fun refreshFrequentApps() {
        hasUsageAccess = usageTracker.hasUsageAccess()
        val pinned = pinnedPackageNames()
        val pool = drawerApps.filter { it.packageName !in pinned }
        // Always most-used-first; FrequentAppsPage lays out from the bottom when sorted that way.
        val ranked = usageTracker.ranked(pool, mostUsedOnTop = true)
        frequentApps = if (ranked.isNotEmpty()) ranked
        else recents.distinctBy { it.packageName }.filter { it.packageName !in pinned }.take(12)
    }

    /** Packages already on the dock, home pages, or edge panels — excluded from Most used. */
    private fun pinnedPackageNames(): Set<String> {
        val out = linkedSetOf<String>()
        (dockSlots + dockRow2Slots).forEach { slot ->
            when (slot) {
                "phone" -> appsRepo.defaultDialer()?.let { out += it.packageName }
                "browser" -> appsRepo.defaultBrowser()?.let { out += it.packageName }
                else -> HomeLayout.parseAppSlot(slot)?.let { (pkg, _) -> out += pkg }
            }
        }
        homePages.forEach { page ->
            page.items.forEach { item ->
                when (item.kind) {
                    "app" -> if (item.packageName.isNotBlank()) out += item.packageName
                    "folder" -> item.folderApps.forEach { out += it.packageName }
                    else -> Unit
                }
            }
        }
        edgePanels.forEach { panel ->
            panel.slots.forEach { slot ->
                HomeLayout.parseAppSlot(slot)?.let { (pkg, _) -> out += pkg }
            }
        }
        return out
    }

    fun openUsageAccessSettings() {
        runCatching { app.startActivity(usageTracker.usageAccessIntent()) }
            .onFailure { showToast("Couldn't open usage access") }
    }

    fun refreshNews() {
        val page = homePages.filter { it.kind == HomePageKind.NEWS }.let { list ->
            list.firstOrNull { homePages.indexOf(it) == homePageIndex } ?: list.firstOrNull()
        } ?: run {
            newsStories = emptyList()
            return
        }
        val selected = newsProviders.filter { it.id in page.newsProviders }
        if (selected.isEmpty()) {
            newsStories = emptyList()
            return
        }
        newsLoading = true
        viewModelScope.launch {
            val rss = withContext(Dispatchers.IO) { NewsFetcher.fetch(selected) }
            val fromNotifs = liveNotifs.filter { n ->
                selected.any { it.packageName.isNotBlank() && it.packageName == n.packageName }
            }.map { n ->
                NewsStory(
                    id = "notif:${n.key}",
                    title = n.title.ifBlank { n.body },
                    url = "",
                    sourceId = n.packageName,
                    sourceName = n.appName,
                    publishedAt = n.postedAt,
                    snippet = n.body,
                    packageName = n.packageName,
                )
            }
            newsStories = (rss + fromNotifs)
                .distinctBy { it.title.lowercase() }
                .sortedByDescending { it.publishedAt }
            newsLoading = false
        }
    }

    fun openNewsStory(story: NewsStory) {
        if (story.url.isNotBlank()) {
            val view = Intent(Intent.ACTION_VIEW, Uri.parse(story.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (story.packageName.isNotBlank()) {
                view.`package` = story.packageName
                if (view.resolveActivity(app.packageManager) == null) view.`package` = null
            }
            runCatching { app.startActivity(view) }
                .onFailure {
                    resolveApp(story.packageName, "")?.let { launchApp(it) }
                        ?: showToast("Couldn't open story")
                }
            return
        }
        if (story.packageName.isNotBlank()) {
            resolveApp(story.packageName, "")?.let { launchApp(it) }
                ?: showToast("Install ${story.sourceName} to open stories")
        }
    }

    fun launchNewsProvider(provider: NewsProvider) {
        if (provider.packageName.isNotBlank()) {
            resolveApp(provider.packageName, "")?.let {
                launchApp(it)
                return
            }
        }
        if (provider.rssUrl.isNotBlank()) {
            openNewsStory(
                NewsStory(
                    id = provider.id,
                    title = provider.name,
                    url = provider.rssUrl.substringBefore("rss").ifBlank { "https://news.google.com" },
                    sourceId = provider.id,
                    sourceName = provider.name,
                    publishedAt = 0L,
                    packageName = provider.packageName,
                ),
            )
        }
    }

    fun removeHomePage() = removeHomePageAt(homePageIndex)

    fun removeHomePageAt(index: Int) {
        if (homePages.size <= 1) {
            showToast("Keep at least one page")
            return
        }
        if (index !in homePages.indices) return
        homePages.getOrNull(index)?.items?.forEach { deleteAppWidgetIfNeeded(it) }
        homePages = homePages.filterIndexed { i, _ -> i != index }
        store.homePages = homePages
        homePageIndex = when {
            homePageIndex > index -> homePageIndex - 1
            homePageIndex >= homePages.size -> homePages.lastIndex
            else -> homePageIndex
        }.coerceIn(0, homePages.lastIndex.coerceAtLeast(0))
        showToast("Page removed")
    }

    fun moveHomePage(delta: Int) {
        val from = homePageIndex.coerceIn(0, homePages.lastIndex)
        val to = from + delta
        if (to !in homePages.indices || from == to) return
        val pages = homePages.toMutableList()
        val page = pages.removeAt(from)
        pages.add(to, page)
        homePages = pages
        store.homePages = pages
        homePageIndex = to
    }

    fun canMoveHomePage(delta: Int): Boolean {
        val to = homePageIndex + delta
        return homePages.size > 1 && to in homePages.indices
    }

    fun openWidgetPicker() {
        val page = homePages.getOrNull(homePageIndex)
        if (page != null && page.kind != HomePageKind.EMPTY) {
            showToast("Widgets go on an empty page")
            return
        }
        homeEditMode = false
        widgetPickerOpen = true
        widgetCatalogLoading = true
        viewModelScope.launch {
            try {
                widgetCatalog = withContext(Dispatchers.IO) { buildWidgetCatalog() }
                android.util.Log.i("MadWidget", "widget catalog size=${widgetCatalog.size}")
            } catch (t: Throwable) {
                android.util.Log.e("MadWidget", "widget catalog failed", t)
                showToast("Couldn't load widgets")
                widgetCatalog = emptyList()
            } finally {
                widgetCatalogLoading = false
            }
        }
    }

    private fun buildWidgetCatalog(): List<WidgetCatalogEntry> {
        val pm = app.packageManager
        val builtIn = listOf(
            WidgetCatalogEntry(
                providerKey = "builtin:glance",
                appLabel = "MAD",
                widgetLabel = "MAD Glance",
                packageName = app.packageName,
                spanX = 4,
                spanY = 2,
                builtInId = "glance",
            ),
            WidgetCatalogEntry(
                providerKey = "builtin:notes",
                appLabel = "MAD",
                widgetLabel = "Notes",
                packageName = app.packageName,
                spanX = 2,
                spanY = 2,
                builtInId = "notes",
            ),
        )
        // Fast path: no previews here (lazy in UI). Discover every provider we can see.
        val thirdParty = discoverAppWidgetProviders()
            .map { info ->
                val key = info.provider.flattenToString()
                val widgetLabel = runCatching { info.loadLabel(pm)?.toString() }.getOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: info.provider.className.substringAfterLast('.')
                val appLabelRaw = runCatching {
                    pm.getApplicationLabel(pm.getApplicationInfo(info.provider.packageName, 0)).toString()
                }.getOrDefault(info.provider.packageName)
                val appLabel = disambiguateWidgetAppLabel(info.provider.packageName, appLabelRaw)
                val span = spansFor(info)
                WidgetCatalogEntry(
                    providerKey = key,
                    appLabel = appLabel,
                    widgetLabel = widgetLabel,
                    packageName = info.provider.packageName,
                    spanX = span.first,
                    spanY = span.second,
                    info = info,
                )
            }
            .sortedWith(
                compareBy<WidgetCatalogEntry> { it.appLabel.lowercase() }
                    .thenBy { it.widgetLabel.lowercase() }
                    .thenBy { it.spanX * 10 + it.spanY },
            )
        return builtIn + thirdParty
    }

    /** Keep patched ports (RVX, etc.) findable when their label matches the stock app. */
    private fun disambiguateWidgetAppLabel(packageName: String, label: String): String {
        val pkg = packageName.lowercase()
        return when {
            pkg.startsWith("app.rvx.") || pkg.contains(".rvx.") -> {
                if (label.contains("rvx", ignoreCase = true) || label.contains("revanced", ignoreCase = true)) label
                else "RVX · $label"
            }
            pkg.contains("revanced") && !label.contains("revanced", ignoreCase = true) -> "ReVanced · $label"
            else -> label
        }
    }

    /**
     * Collect AppWidget providers the way a home launcher should: current profile,
     * all user profiles, plus any home/search/keyguard categories the public API exposes.
     */
    private fun discoverAppWidgetProviders(): List<AppWidgetProviderInfo> {
        val byKey = LinkedHashMap<String, AppWidgetProviderInfo>()
        fun addAll(list: List<AppWidgetProviderInfo>?) {
            list?.forEach { info ->
                val key = info.provider.flattenToString()
                if (key.isNotBlank()) byKey.putIfAbsent(key, info)
            }
        }
        addAll(runCatching { widgetManager.installedProviders }.getOrNull())
        runCatching {
            val um = app.getSystemService(android.content.Context.USER_SERVICE) as android.os.UserManager
            um.userProfiles.forEach { profile ->
                addAll(widgetManager.getInstalledProvidersForProfile(profile))
            }
        }
        // Hidden overload present on many OEMs: getInstalledProviders(categoryFilter).
        runCatching {
            val method = AppWidgetManager::class.java.getMethod(
                "getInstalledProviders",
                Int::class.javaPrimitiveType,
            )
            val cats = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN or
                AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD or
                AppWidgetProviderInfo.WIDGET_CATEGORY_SEARCHBOX
            @Suppress("UNCHECKED_CAST")
            addAll(method.invoke(widgetManager, cats) as? List<AppWidgetProviderInfo>)
        }
        android.util.Log.i("MadWidget", "discovered providers=${byKey.size}")
        return byKey.values.toList()
    }

    fun loadWidgetPreviewBitmap(info: AppWidgetProviderInfo): android.graphics.Bitmap? {
        return loadWidgetPreview(info, app.resources.displayMetrics.densityDpi)
    }

    private fun loadWidgetPreview(info: AppWidgetProviderInfo, densityDpi: Int): android.graphics.Bitmap? {
        val drawable = runCatching { info.loadPreviewImage(app, densityDpi) }.getOrNull()
            ?: runCatching {
                if (info.previewImage != 0) {
                    app.packageManager.getDrawable(info.provider.packageName, info.previewImage, null)
                } else null
            }.getOrNull()
            ?: runCatching { app.packageManager.getApplicationIcon(info.provider.packageName) }.getOrNull()
            ?: return null
        return runCatching {
            val max = 256
            val w = (drawable.intrinsicWidth.takeIf { it > 0 } ?: 160).coerceAtMost(max)
            val h = (drawable.intrinsicHeight.takeIf { it > 0 } ?: 160).coerceAtMost(max)
            val bmp = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bmp)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(canvas)
            bmp
        }.getOrNull()
    }

    fun removeDesktopItem(id: String) {
        removeHomeItem(id)
        if (editingWidgetId == id) exitWidgetEdit()
    }

    fun addWidget(widget: String) {
        val page = homePages.getOrNull(homePageIndex) ?: return
        if (page.kind != HomePageKind.EMPTY) {
            showToast("Widgets go on an empty page")
            return
        }
        val tries = if (widget == "glance") {
            listOf(4 to 2, 4 to 1, 2 to 2, 2 to 1, 1 to 1)
        } else {
            listOf(2 to 2, 1 to 1)
        }
        val placed = tries.firstNotNullOfOrNull { (spanX, spanY) ->
            firstEmptyCell(page, spanX, spanY)?.let { cell -> Triple(cell.first, cell.second, spanX to spanY) }
        }
        if (placed == null) {
            showToast("No space on this page")
            return
        }
        val item = HomeItem(
            id = "w${System.currentTimeMillis()}",
            kind = "widget",
            widget = widget,
            col = placed.first,
            row = placed.second,
            spanX = placed.third.first.toFloat(),
            spanY = placed.third.second.toFloat(),
        )
        replacePage(homePageIndex, page.copy(items = page.items + item))
        widgetPickerOpen = false
        enterWidgetEdit(item.id)
        showToast("Widget added — drag to move, handles to resize")
    }

    fun pickWidget(entry: WidgetCatalogEntry) {
        val builtIn = entry.builtInId
        if (builtIn != null) {
            addWidget(builtIn)
            return
        }
        val info = entry.info ?: return
        addInstalledWidget(info)
    }

    fun addInstalledWidget(entry: WidgetCatalogEntry) {
        val info = entry.info ?: return
        addInstalledWidget(info)
    }

    fun addInstalledWidget(info: AppWidgetProviderInfo) {
        val page = homePages.getOrNull(homePageIndex) ?: return
        if (page.kind != HomePageKind.EMPTY) {
            showToast("Widgets go on an empty page")
            return
        }
        val span = spansFor(info)
        val cell = firstEmptyCell(page, span.first, span.second)
            ?: firstEmptyCell(page, 1, 1)
        if (cell == null) {
            showToast("No space on this page")
            return
        }
        pendingPlaceCol = cell.first
        pendingPlaceRow = cell.second
        val id = widgetHost.allocateAppWidgetId()
        pendingAppWidgetId = id
        pendingAppWidgetSpan = span
        val bound = if (Build.VERSION.SDK_INT >= 17) {
            widgetManager.bindAppWidgetIdIfAllowed(id, info.provider)
        } else {
            widgetManager.bindAppWidgetIdIfAllowed(id, info.provider)
        }
        if (bound) {
            continueAppWidgetSetup(info.configure)
        } else {
            widgetBindIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
            }
        }
    }

    fun onAppWidgetBindResult(ok: Boolean) {
        widgetBindIntent = null
        val id = pendingAppWidgetId
        if (!ok || id == AppWidgetManager.INVALID_APPWIDGET_ID) {
            cancelPendingAppWidget()
            return
        }
        val info = widgetManager.getAppWidgetInfo(id)
        if (info == null) {
            cancelPendingAppWidget()
            showToast("Couldn't bind that widget")
            return
        }
        pendingAppWidgetSpan = spansFor(info)
        continueAppWidgetSetup(info.configure)
    }

    fun onAppWidgetConfigResult(ok: Boolean) {
        widgetConfigIntent = null
        if (!ok) {
            cancelPendingAppWidget()
            return
        }
        placePendingAppWidget()
    }

    private fun continueAppWidgetSetup(configure: ComponentName?) {
        if (configure != null) {
            widgetConfigIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, pendingAppWidgetId)
            }
        } else {
            placePendingAppWidget()
        }
    }

    private fun placePendingAppWidget() {
        val id = pendingAppWidgetId
        if (id == AppWidgetManager.INVALID_APPWIDGET_ID) return
        val page = homePages.getOrNull(homePageIndex) ?: run {
            cancelPendingAppWidget()
            return
        }
        var spanX = pendingAppWidgetSpan.first.coerceIn(1, gridSizeFor(page))
        var spanY = pendingAppWidgetSpan.second.coerceIn(1, gridSizeFor(page))
        val preferred = if (pendingPlaceCol >= 0 && pendingPlaceRow >= 0) {
            pendingPlaceCol to pendingPlaceRow
        } else null
        pendingPlaceCol = -1
        pendingPlaceRow = -1
        val grid = gridSizeFor(page)
        var cell = preferred?.takeIf { (col, row) ->
            col + spanX <= grid &&
                row + spanY <= grid &&
                !cellTaken(page, col, row, spanX = spanX, spanY = spanY)
        }
        if (cell == null) cell = firstEmptyCell(page, spanX, spanY)
        if (cell == null) {
            spanX = 1
            spanY = 1
            cell = preferred?.takeIf { (col, row) -> !cellTaken(page, col, row) }
                ?: firstEmptyCell(page, 1, 1)
        }
        if (cell == null) {
            cancelPendingAppWidget()
            showToast("No space on this page")
            return
        }
        val info = widgetManager.getAppWidgetInfo(id)
        val item = HomeItem(
            id = "aw$id",
            kind = "appwidget",
            widget = info?.loadLabel(app.packageManager)?.toString().orEmpty(),
            packageName = info?.provider?.packageName.orEmpty(),
            activityName = info?.provider?.className.orEmpty(),
            appWidgetId = id,
            col = cell.first,
            row = cell.second,
            spanX = spanX.toFloat(),
            spanY = spanY.toFloat(),
        )
        replacePage(homePageIndex, page.copy(items = page.items + item))
        pendingAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        widgetPickerOpen = false
        enterWidgetEdit(item.id)
        showToast("Widget added — drag to move, handles to resize")
    }

    private fun cancelPendingAppWidget() {
        if (pendingAppWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            runCatching { widgetHost.deleteAppWidgetId(pendingAppWidgetId) }
        }
        pendingAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
        pendingPlaceCol = -1
        pendingPlaceRow = -1
        widgetBindIntent = null
        widgetConfigIntent = null
    }

    /** Ask a package to republish RemoteViews for any of its widgets on the home grid. */
    private fun nudgeAppWidgetsForPackage(packageName: String) {
        if (packageName.isBlank()) return
        val ids = homePages.flatMap { page ->
            page.items.filter { it.isAppWidget && it.appWidgetId != 0 }
                .mapNotNull { item ->
                    val info = widgetManager.getAppWidgetInfo(item.appWidgetId) ?: return@mapNotNull null
                    if (info.provider.packageName != packageName) return@mapNotNull null
                    item.appWidgetId to info.provider
                }
        }
        if (ids.isEmpty()) return
        ids.groupBy({ it.second }, { it.first }).forEach { (provider, widgetIds) ->
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE).apply {
                component = provider
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds.toIntArray())
            }
            runCatching { app.sendBroadcast(intent) }
        }
        widgetHost.ensureListening()
    }

    private fun spansFor(info: AppWidgetProviderInfo): Pair<Int, Int> {
        val grid = gridSizeFor(homePageIndex)
        if (Build.VERSION.SDK_INT >= 31 && info.targetCellWidth > 0 && info.targetCellHeight > 0) {
            return info.targetCellWidth.coerceIn(1, grid) to
                info.targetCellHeight.coerceIn(1, grid)
        }
        val spanX = ((info.minWidth + 30) / 70).coerceIn(1, grid)
        val spanY = ((info.minHeight + 30) / 70).coerceIn(1, grid)
        return spanX to spanY
    }

    private fun deleteAppWidgetIfNeeded(item: HomeItem) {
        if (item.appWidgetId != 0) {
            runCatching { widgetHost.discardHostView(item.appWidgetId) }
            runCatching { widgetHost.deleteAppWidgetId(item.appWidgetId) }
        }
    }

    fun resizeWidget(itemId: String, spanX: Float, spanY: Float) {
        val snapped = snapWidgetSpan(itemId, spanX, spanY) ?: run {
            showToast("Not enough space")
            return
        }
        val page = homePages.getOrNull(homePageIndex) ?: return
        val item = page.items.firstOrNull { it.id == itemId } ?: return
        val updated = item.copy(spanX = snapped.first, spanY = snapped.second)
        replacePage(homePageIndex, page.copy(items = page.items.map { if (it.id == itemId) updated else it }))
    }

    /** Snap a resize preview to whole grid tiles; null when the size would overlap another item. */
    fun snapWidgetSpan(itemId: String, spanX: Float, spanY: Float): Pair<Float, Float>? {
        val page = homePages.getOrNull(homePageIndex) ?: return null
        val item = page.items.firstOrNull { it.id == itemId } ?: return null
        val grid = gridSizeFor(page)
        val maxSpanXInt = (grid - item.col).coerceAtLeast(1)
        val maxSpanYInt = (grid - item.row).coerceAtLeast(1)
        val sx = kotlin.math.round(spanX).toInt().coerceIn(1, maxSpanXInt).toFloat()
        val sy = kotlin.math.round(spanY).toInt().coerceIn(1, maxSpanYInt).toFloat()
        if (!widgetFits(page, item.col, item.row, sx, sy, itemId, grid)) return null
        return sx to sy
    }

    fun previewWidgetMove(itemId: String, col: Int, row: Int) {
        val page = homePages.getOrNull(homePageIndex) ?: return
        val item = page.items.firstOrNull { it.id == itemId } ?: return
        val grid = gridSizeFor(page)
        val destCol = col.coerceIn(0, (grid - item.spanX).toInt().coerceAtLeast(0))
        val destRow = row.coerceIn(0, (grid - item.spanY).toInt().coerceAtLeast(0))
        if (destCol == item.col && destRow == item.row) {
            widgetMovePreview = null
            widgetSwapPartnerId = null
            dragDropKey = null
            return
        }
        val partner = findSwapPartner(page, item, destCol, destRow)
        if (partner != null &&
            widgetFits(page, item.col, item.row, partner.spanX, partner.spanY, partner.id, grid, setOf(itemId)) &&
            widgetFits(page, destCol, destRow, item.spanX, item.spanY, itemId, grid, setOf(partner.id))
        ) {
            widgetMovePreview = itemId to (destCol to destRow)
            widgetSwapPartnerId = partner.id
            dragDropKey = null
            return
        }
        widgetSwapPartnerId = null
        if (!widgetFits(page, destCol, destRow, item.spanX, item.spanY, itemId, grid)) return
        widgetMovePreview = itemId to (destCol to destRow)
        dragDropKey = "$homePageIndex:$destCol,$destRow"
    }

    fun commitWidgetMove(itemId: String) {
        val dest = widgetMovePreview?.takeIf { it.first == itemId }?.second
        val partnerId = widgetSwapPartnerId
        widgetMovePreview = null
        widgetSwapPartnerId = null
        dragDropKey = null
        if (dest == null) return
        val page = homePages.getOrNull(homePageIndex) ?: return
        val item = page.items.firstOrNull { it.id == itemId } ?: return
        if (item.col == dest.first && item.row == dest.second) return
        val grid = gridSizeFor(page)
        if (partnerId != null) {
            val partner = page.items.firstOrNull { it.id == partnerId } ?: return
            if (!widgetFits(page, item.col, item.row, partner.spanX, partner.spanY, partner.id, grid, setOf(itemId)) ||
                !widgetFits(page, dest.first, dest.second, item.spanX, item.spanY, itemId, grid, setOf(partner.id))
            ) {
                return
            }
            replacePage(
                homePageIndex,
                page.copy(
                    items = page.items.map {
                        when (it.id) {
                            itemId -> it.copy(col = dest.first, row = dest.second)
                            partnerId -> it.copy(col = item.col, row = item.row)
                            else -> it
                        }
                    },
                ),
            )
            return
        }
        if (!widgetFits(page, dest.first, dest.second, item.spanX, item.spanY, itemId, grid)) return
        replacePage(
            homePageIndex,
            page.copy(items = page.items.map { if (it.id == itemId) it.copy(col = dest.first, row = dest.second) else it }),
        )
    }

    fun cancelWidgetMove() {
        widgetMovePreview = null
        widgetSwapPartnerId = null
        dragDropKey = null
    }

    fun beginAppHold(
        app: LaunchableApp,
        from: String,
        itemId: String? = null,
        dockIndex: Int? = null,
        edgeSide: EdgeSide? = null,
        fromPageIndex: Int? = null,
    ) {
        appHold = AppHold(
            app,
            from,
            itemId,
            dockIndex,
            edgeSide,
            fromPageIndex = fromPageIndex ?: if (from == "home") homePageIndex else null,
        )
        dragMoved = false
        appMenu = null
        // Don't lock the pager yet — flipping userScrollEnabled cancels the pointer and pops the menu.
        if (from == "drawer") {
            homeEditMode = false
            editingWidgetId = null
            recentsOpen = false
            systemNotifDropdownOpen = false
        }
    }

    fun updateHoldPointer(x: Float, y: Float, moved: Boolean) {
        dragX = x
        dragY = y
        val edgeHit = edgeDropRects.entries.firstOrNull { (_, rect) -> rect.contains(x, y) }?.key
        dragDropKey = edgeHit
            ?: dockDropRects.indices.firstOrNull { dockDropRects[it].contains(x, y) }?.let { "dock:$it" }
            ?: HomeDropHit.pick(homeDropRects, x, y, homePageIndex)
        if (edgeHit != null) {
            EdgeSide.fromKey(edgeHit.substringBefore(":"))?.let { expandedEdge = it }
        } else {
            edgePanels.forEach { panel ->
                val near = edgeDropRects.filterKeys { it.startsWith("${panel.side.key}:") }.values.any { rect ->
                    val pad = 80f
                    x >= rect.left - pad && x <= rect.right + pad && y >= rect.top - pad && y <= rect.bottom + pad
                }
                if (near) expandedEdge = panel.side
            }
        }
        if (moved && !dragMoved) {
            dragMoved = true
            homePagerLocked = true
            recentsOpen = false
            systemNotifDropdownOpen = false
            if (appHold?.from == "drawer") ensureEmptyHomeForPin()
        }
        if (dragMoved && (appHold?.from == "drawer" || appHold?.from == "home")) {
            maybeFlipHomePage(x)
        }
    }

    private fun ensureEmptyHomeForPin() {
        if (homePages.getOrNull(homePageIndex)?.kind == HomePageKind.EMPTY) return
        val existing = homePages.indexOfFirst { it.kind == HomePageKind.EMPTY }
        if (existing >= 0) {
            homePageIndex = existing
            persistLastHomePageIndex()
            return
        }
        homePages = homePages + HomePage(
            id = "p${System.currentTimeMillis()}",
            items = emptyList(),
            kind = HomePageKind.EMPTY,
        )
        store.homePages = homePages
        homePageIndex = homePages.lastIndex
        persistLastHomePageIndex()
    }

    private fun maybeFlipHomePage(x: Float) {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - lastHomeFlipMs < 550L) return
        val prefix = HomeDropHit.pagePrefix(homePageIndex)
        val rects = homeDropRects.filterKeys { it.startsWith(prefix) }.values
        if (rects.isEmpty()) return
        val left = rects.minOf { it.left }
        val right = rects.maxOf { it.right }
        val edge = ((right - left) * 0.12f).coerceIn(48f, 96f)
        val target = when {
            x < left + edge -> nearestEmptyPage(homePageIndex, -1)
            x > right - edge -> nearestEmptyPage(homePageIndex, +1)
            else -> null
        } ?: return
        if (target == homePageIndex) return
        homePageIndex = target
        persistLastHomePageIndex()
        lastHomeFlipMs = now
    }

    private fun nearestEmptyPage(from: Int, direction: Int): Int? {
        var i = from + direction
        while (i in homePages.indices) {
            if (homePages[i].kind == HomePageKind.EMPTY) return i
            i += direction
        }
        return null
    }

    fun endAppHold() {
        val hold = appHold ?: return
        if (hold.from == "actionbox") {
            appMenu = hold
            appMenuGuardUntil = SystemClock.uptimeMillis() + 400
            cancelHold()
            return
        }
        if (dragMoved && !releasedOnOrigin(hold)) {
            val placed = dropHold(hold)
            if (hold.from == "drawer" && placed) appDrawerOpen = false
            appMenu = null
            cancelHold()
        } else {
            appMenu = hold
            appMenuGuardUntil = SystemClock.uptimeMillis() + 400
            cancelHold()
        }
    }

    fun dismissAppMenu() {
        if (SystemClock.uptimeMillis() < appMenuGuardUntil) return
        appMenu = null
    }

    private fun releasedOnOrigin(hold: AppHold): Boolean {
        val x = dragX
        val y = dragY
        return when (hold.from) {
            "home" -> {
                val pageIdx = hold.fromPageIndex ?: homePageIndex
                val item = homePages.getOrNull(pageIdx)?.items?.firstOrNull { it.id == hold.itemId }
                    ?: return true
                homeDropRects["$pageIdx:${item.col},${item.row}"]?.contains(x, y) == true
            }
            "dock" -> hold.dockIndex?.let { idx ->
                idx in dockDropRects.indices && dockDropRects[idx].contains(x, y)
            } == true
            "edge" -> {
                val key = "${hold.edgeSide?.key}:${hold.dockIndex}"
                edgeDropRects[key]?.contains(x, y) == true
            }
            else -> false
        }
    }

    fun cancelHold() {
        appHold = null
        dragMoved = false
        dragDropKey = null
        unlockHomePagerIfIdle()
    }

    private fun dropHold(hold: AppHold): Boolean {
        val x = dragX
        val y = dragY
        val edgeHit = edgeDropRects.entries.firstOrNull { (_, rect) -> rect.contains(x, y) }?.key
        if (edgeHit != null) {
            val side = EdgeSide.fromKey(edgeHit.substringBefore(":"))
            val index = edgeHit.substringAfter(":").toIntOrNull()
            if (side != null && index != null) {
                placeOnEdge(hold, side, index)
                dragDropKey = null
                return true
            }
        }
        dockDropRects.forEachIndexed { index, rect ->
            if (rect.contains(x, y)) {
                placeOnDock(hold, index)
                dragDropKey = null
                return true
            }
        }
        val hitKey = HomeDropHit.pick(homeDropRects, x, y, homePageIndex)
        if (hitKey != null) {
            val pageIdx = hitKey.substringBefore(":").toIntOrNull() ?: homePageIndex
            val parts = hitKey.substringAfter(":").split(",")
            val col = parts.getOrNull(0)?.toIntOrNull()
            val row = parts.getOrNull(1)?.toIntOrNull()
            if (col != null && row != null) {
                placeOnHome(hold, col, row, pageIdx)
                dragDropKey = null
                return true
            }
        }
        if (hold.from != "drawer") showToast("Not placed")
        dragDropKey = null
        return false
    }

    private fun placeOnDock(hold: AppHold, index: Int) {
        val current = dockSlotsCombined()
        if (index !in current.indices) return
        val incoming = HomeLayout.appSlot(hold.app)
        if (current.getOrNull(index) in HomeLayout.protectedDock && incoming != current[index]) {
            val empty = current.indexOfFirst { it.isBlank() }
            if (empty >= 0) {
                current[empty] = incoming
            } else {
                showToast("That dock slot stays for launcher controls")
                return
            }
        } else {
            val previous = current[index]
            current[index] = incoming
            if (hold.dockIndex != null && hold.dockIndex != index && hold.dockIndex in current.indices) {
                current[hold.dockIndex] = previous
            }
        }
        if (hold.from == "home" && hold.itemId != null) {
            removeHomeItem(hold.itemId)
        }
        clearEdgeSlot(hold)
        commitDockSlots(current)
        showToast("Added to dock")
    }

    private fun placeOnEdge(hold: AppHold, side: EdgeSide, index: Int) {
        if (hold.app.packageName in setOf("phone", "browser", "actions", "apps", "settings")) {
            showToast("Launcher buttons stay on the bottom dock")
            return
        }
        val panel = edgePanels.firstOrNull { it.side == side } ?: return
        val slots = panel.slots.toMutableList()
        if (index !in slots.indices) return
        val incoming = HomeLayout.appSlot(hold.app)
        val previous = slots[index]
        slots[index] = incoming
        if (hold.from == "edge" && hold.edgeSide != null && hold.dockIndex != null) {
            if (hold.edgeSide == side) {
                if (hold.dockIndex != index) slots[hold.dockIndex] = previous
            } else {
                clearEdgeSlot(hold, slotsFor = side)
                val other = edgePanels.firstOrNull { it.side == hold.edgeSide } ?: return
                val otherSlots = other.slots.toMutableList()
                if (hold.dockIndex in otherSlots.indices) otherSlots[hold.dockIndex] = previous
                edgePanels = edgePanels.map {
                    when (it.side) {
                        side -> it.copy(slots = slots)
                        hold.edgeSide -> it.copy(slots = otherSlots)
                        else -> it
                    }
                }
                store.edgePanels = edgePanels
                if (hold.from == "home" && hold.itemId != null) removeHomeItem(hold.itemId)
                showToast("Added to MAD Dock")
                return
            }
        }
        edgePanels = edgePanels.map { if (it.side == side) it.copy(slots = slots) else it }
        store.edgePanels = edgePanels
        if (hold.from == "home" && hold.itemId != null) removeHomeItem(hold.itemId)
        if (hold.from == "dock" && hold.dockIndex != null) {
            val dock = dockSlotsCombined()
            if (hold.dockIndex in dock.indices) dock[hold.dockIndex] = ""
            commitDockSlots(dock)
        }
        showToast("Added to MAD Dock")
    }

    private fun clearEdgeSlot(hold: AppHold, slotsFor: EdgeSide? = null) {
        if (hold.from != "edge" || hold.edgeSide == null || hold.dockIndex == null) return
        if (slotsFor != null && hold.edgeSide == slotsFor) return
        edgePanels = edgePanels.map { panel ->
            if (panel.side != hold.edgeSide) panel
            else {
                val slots = panel.slots.toMutableList()
                if (hold.dockIndex in slots.indices) slots[hold.dockIndex] = ""
                panel.copy(slots = slots)
            }
        }
        store.edgePanels = edgePanels
    }

    private fun placeOnHome(hold: AppHold, col: Int, row: Int, pageIndex: Int = homePageIndex) {
        if (hold.app.packageName in setOf("phone", "browser", "actions", "apps", "settings", "launcher_settings")) {
            showToast("Launcher buttons stay on the dock")
            return
        }
        val page = homePages.getOrNull(pageIndex) ?: return
        if (page.kind != HomePageKind.EMPTY) {
            showToast("Drop apps on an empty page")
            return
        }
        val sourceItem = hold.itemId?.let { id ->
            homePages.flatMap { it.items }.firstOrNull { it.id == id }
        }
        // Moving an existing home tile must keep kind/iconPath/intentUri (shortcuts, folders, …).
        if (sourceItem != null && (sourceItem.isShortcut || sourceItem.isAppWidget || sourceItem.isFolder)) {
            val occupant = page.items.firstOrNull { it.id != hold.itemId && it.occupies(col, row) }
            if (occupant != null) {
                showToast("That cell is taken")
                return
            }
            homePages = homePages.mapIndexed { i, p ->
                val without = p.items.filterNot { it.id == sourceItem.id }
                if (i == pageIndex) p.copy(items = without + sourceItem.copy(col = col, row = row))
                else p.copy(items = without)
            }
            store.homePages = homePages
            clearHoldSource(hold)
            if (pageIndex != homePageIndex) homePageIndex = pageIndex
            showToast("Moved")
            return
        }
        val incoming = membersFromHold(hold)
        if (incoming.isEmpty()) return
        val occupant = page.items.firstOrNull { it.id != hold.itemId && it.occupies(col, row) }
        when {
            occupant == null -> {
                if (sourceItem != null) {
                    homePages = homePages.mapIndexed { i, p ->
                        val without = p.items.filterNot { it.id == sourceItem.id }
                        if (i == pageIndex) p.copy(items = without + sourceItem.copy(col = col, row = row))
                        else p.copy(items = without)
                    }
                    store.homePages = homePages
                } else {
                    val item = HomeItem(
                        id = hold.itemId ?: "a${System.currentTimeMillis()}",
                        kind = "app",
                        packageName = hold.app.packageName,
                        activityName = hold.app.activityName,
                        col = col,
                        row = row,
                    )
                    replacePage(pageIndex, page.copy(items = page.items.filterNot { it.id == item.id } + item))
                }
                clearHoldSource(hold)
                if (pageIndex != homePageIndex) homePageIndex = pageIndex
                showToast("Added to home")
            }
            occupant.isApp || occupant.isFolder -> {
                val existing = if (occupant.isFolder) {
                    occupant.folderApps
                } else {
                    listOf(FolderApp(occupant.packageName, occupant.activityName))
                }
                val merged = (existing + incoming)
                    .distinctBy { it.id }
                    .take(HomeLayout.FOLDER_MAX)
                if (merged.size <= 1) {
                    showToast("Couldn't create folder")
                    return
                }
                if (merged.size == existing.size && hold.itemId == null) {
                    showToast("Already in folder")
                    return
                }
                if (merged.size == existing.size && existing.size >= HomeLayout.FOLDER_MAX) {
                    showToast("Folder is full")
                    return
                }
                val folder = HomeItem(
                    id = if (occupant.isFolder) occupant.id else "f${System.currentTimeMillis()}",
                    kind = "folder",
                    title = if (occupant.isFolder) occupant.title.ifBlank { "Folder" } else "Folder",
                    col = col,
                    row = row,
                    folderApps = merged,
                )
                homePages = homePages.mapIndexed { i, p ->
                    val without = p.items.filterNot { it.id == occupant.id || it.id == hold.itemId }
                    if (i == pageIndex) p.copy(items = without.filterNot { it.id == folder.id } + folder)
                    else p.copy(items = without)
                }
                store.homePages = homePages
                clearHoldSource(hold)
                if (pageIndex != homePageIndex) homePageIndex = pageIndex
                showToast(if (occupant.isFolder) "Added to folder" else "Folder created")
            }
            else -> showToast("That cell is taken")
        }
    }

    private fun membersFromHold(hold: AppHold): List<FolderApp> {
        val source = hold.itemId?.let { id ->
            homePages.flatMap { it.items }.firstOrNull { it.id == id }
        }
        if (source?.isFolder == true) return source.folderApps
        if (hold.app.packageName == "folder") return emptyList()
        return listOf(FolderApp.from(hold.app))
    }

    private fun clearHoldSource(hold: AppHold) {
        if (hold.dockIndex != null && hold.from == "dock") {
            val slots = dockSlotsCombined()
            if (hold.dockIndex in slots.indices) slots[hold.dockIndex] = ""
            commitDockSlots(slots)
        }
        clearEdgeSlot(hold)
    }

    fun openFolder(itemId: String) {
        openFolderId = itemId
        appMenu = null
    }

    fun closeFolder() {
        openFolderId = null
    }

    fun folderItem(): HomeItem? {
        val id = openFolderId ?: return null
        return homePages.flatMap { it.items }.firstOrNull { it.id == id && it.isFolder }
    }

    fun renameFolder(itemId: String, title: String) {
        val name = title.trim().ifBlank { "Folder" }
        homePages = homePages.map { page ->
            page.copy(items = page.items.map { if (it.id == itemId && it.isFolder) it.copy(title = name) else it })
        }
        store.homePages = homePages
    }

    fun removeAppFromFolder(folderId: String, app: FolderApp) {
        homePages = homePages.map { page ->
            val items = page.items.mapNotNull { item ->
                if (item.id != folderId || !item.isFolder) return@mapNotNull item
                val next = item.folderApps.filterNot { it.id == app.id }
                when {
                    next.isEmpty() -> null
                    next.size == 1 -> HomeItem(
                        id = "a${System.currentTimeMillis()}",
                        kind = "app",
                        packageName = next[0].packageName,
                        activityName = next[0].activityName,
                        col = item.col,
                        row = item.row,
                    )
                    else -> item.copy(folderApps = next)
                }
            }
            page.copy(items = items)
        }
        store.homePages = homePages
        if (folderItem() == null) openFolderId = null
    }

    fun launchFolderApp(app: FolderApp) {
        resolveApp(app.packageName, app.activityName)?.let {
            closeFolder()
            launchApp(it)
        }
    }

    fun uninstallFromMenu() {
        val menu = appMenu ?: return
        val pkg = menu.app.packageName
        if (pkg.isBlank() || pkg in HomeLayout.specialDock) {
            showToast("Can't uninstall that")
            appMenu = null
            return
        }
        appMenu = null
        // Close overlays so the system uninstall confirmation is visible.
        appDrawerOpen = false
        recentsOpen = false
        runCatching { intents.uninstallApp(pkg) }
            .onFailure { showToast("Couldn't uninstall ${menu.app.label}") }
    }

    fun removeFromMenu() {
        val menu = appMenu ?: return
        when (menu.from) {
            "dock" -> {
                val idx = menu.dockIndex ?: return
                if (dockSlotAt(idx) in HomeLayout.protectedDock) {
                    showToast("That dock button stays")
                } else {
                    val slots = dockSlotsCombined()
                    if (idx in slots.indices) slots[idx] = ""
                    commitDockSlots(slots)
                }
            }
            "home" -> menu.itemId?.let { removeHomeItem(it) }
            "edge" -> {
                val side = menu.edgeSide ?: return
                val idx = menu.dockIndex ?: return
                edgePanels = edgePanels.map { panel ->
                    if (panel.side != side) panel
                    else {
                        val slots = panel.slots.toMutableList()
                        if (idx in slots.indices) slots[idx] = ""
                        panel.copy(slots = slots)
                    }
                }
                store.edgePanels = edgePanels
            }
            else -> {
                hiddenApps = hiddenApps + menu.app.packageName
                store.hiddenApps = hiddenApps
                showToast("Hidden from the app drawer")
            }
        }
        appMenu = null
    }

    fun openMenuAppSettings() {
        val pkg = appMenu?.app?.packageName ?: return
        appMenu = null
        runCatching { intents.openAppSettings(pkg) }
    }

    fun setWallpaper(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = WallpaperApplier.applyImage(getApplication(), uri)
            withContext(Dispatchers.Main) {
                if (ok) {
                    wallpaperPath = null
                    store.wallpaperPath = null
                    refreshWallpaperPalette()
                    showToast("Wallpaper updated")
                } else {
                    showToast("Couldn't set wallpaper")
                }
            }
        }
    }

    /**
     * Prefer system crop-and-set; fall back to MAD center-crop apply.
     * @return crop Intent to launch, or null when already applied / failed.
     */
    fun prepareWallpaperUri(uri: Uri): Intent? {
        runCatching {
            app.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val crop = WallpaperApplier.cropAndSetIntent(app, uri)
        if (crop != null) return crop
        setWallpaper(uri)
        return null
    }

    fun onSystemWallpaperCropFinished(ok: Boolean) {
        if (ok) {
            wallpaperPath = null
            store.wallpaperPath = null
            refreshWallpaperPalette()
            showToast("Wallpaper updated")
        } else {
            showToast("Wallpaper crop cancelled")
        }
    }

    fun openLiveWallpapers() {
        runCatching { intents.openLiveWallpaperChooser() }
            .onFailure { showToast("No live wallpaper picker") }
    }

    fun setCustomIcon(uri: Uri) {
        val pkg = iconEditTarget ?: appMenu?.app?.packageName ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val out = File(getApplication<MadApp>().filesDir, "icon_${pkg.hashCode()}.png")
            runCatching {
                getApplication<MadApp>().contentResolver.openInputStream(uri)?.use { input ->
                    out.outputStream().use { input.copyTo(it) }
                }
            }
            withContext(Dispatchers.Main) {
                store.setCustomIconPath(pkg, out.absolutePath)
                iconEditTarget = null
                appMenu = null
                homePages = homePages.toList()
                dockSlots = dockSlots.toList()
                dockRow2Slots = dockRow2Slots.toList()
                showToast("Icon updated")
            }
        }
    }

    fun requestIconEdit() {
        iconEditTarget = appMenu?.app?.packageName
    }

    private fun removeHomeItem(id: String) {
        val idx = homePageIndex.coerceIn(0, homePages.lastIndex)
        val page = homePages[idx]
        page.items.firstOrNull { it.id == id }?.let { deleteAppWidgetIfNeeded(it) }
        replacePage(idx, page.copy(items = page.items.filterNot { it.id == id }))
    }

    private fun replacePage(index: Int, page: HomePage) {
        homePages = homePages.mapIndexed { i, old -> if (i == index) page else old }
        store.homePages = homePages
    }

    private fun cellTaken(page: HomePage, col: Int, row: Int, ignoreId: String? = null, spanX: Int = 1, spanY: Int = 1): Boolean {
        for (x in col until col + spanX) {
            for (y in row until row + spanY) {
                if (page.items.any { it.id != ignoreId && it.occupies(x, y) }) return true
            }
        }
        return false
    }

    private fun firstEmptyCell(page: HomePage, spanX: Int, spanY: Int): Pair<Int, Int>? {
        val grid = gridSizeFor(page)
        for (row in 0..grid - spanY) {
            for (col in 0..grid - spanX) {
                if (!cellTaken(page, col, row, spanX = spanX, spanY = spanY)) return col to row
            }
        }
        return null
    }

    private fun clampPageItemsToGrid(page: HomePage, gridSize: Int): HomePage {
        val items = page.items.mapNotNull { item ->
            if (item.col >= gridSize || item.row >= gridSize) return@mapNotNull null
            val maxSpanX = gridSize - item.col.toFloat()
            val maxSpanY = gridSize - item.row.toFloat()
            if (maxSpanX < HomeLayout.MIN_SPAN || maxSpanY < HomeLayout.MIN_SPAN) return@mapNotNull null
            item.copy(
                spanX = item.spanX.coerceAtMost(maxSpanX),
                spanY = item.spanY.coerceAtMost(maxSpanY),
            )
        }
        return page.copy(items = items)
    }

    private fun rectsOverlap(
        colA: Int,
        rowA: Int,
        spanXA: Float,
        spanYA: Float,
        colB: Int,
        rowB: Int,
        spanXB: Float,
        spanYB: Float,
    ): Boolean {
        return colA < colB + spanXB && colA + spanXA > colB &&
            rowA < rowB + spanYB && rowA + spanYA > rowB
    }

    private fun findSwapPartner(page: HomePage, item: HomeItem, destCol: Int, destRow: Int): HomeItem? {
        val overlapping = page.items.filter { other ->
            other.id != item.id &&
                rectsOverlap(
                    destCol,
                    destRow,
                    item.spanX,
                    item.spanY,
                    other.col,
                    other.row,
                    other.spanX,
                    other.spanY,
                )
        }
        return overlapping.singleOrNull() ?: overlapping.firstOrNull()
    }

    private fun widgetFits(
        page: HomePage,
        col: Int,
        row: Int,
        spanX: Float,
        spanY: Float,
        ignoreId: String,
        grid: Int = gridSizeFor(page),
        alsoIgnore: Set<String> = emptySet(),
    ): Boolean {
        if (col < 0 || row < 0) return false
        if (col + spanX > grid + 0.001f) return false
        if (row + spanY > grid + 0.001f) return false
        for (x in 0 until grid) {
            for (y in 0 until grid) {
                val here = x + 1 > col && x < col + spanX && y + 1 > row && y < row + spanY
                if (here && page.items.any { it.id != ignoreId && it.id !in alsoIgnore && it.occupies(x, y) }) {
                    return false
                }
            }
        }
        return true
    }

    private fun migrateBuiltInWidgets() {
        val old = setOf("clock", "date", "weather")
        var changed = false
        val migrated = homePages.map { page ->
            val firstOld = page.items.firstOrNull { it.kind == "widget" && it.widget in old }
            val items = page.items.mapNotNull { item ->
                if (item.kind != "widget" || item.widget !in old) item
                else {
                    changed = true
                    if (item.id == firstOld?.id) item.copy(widget = "glance") else null
                }
            }
            if (changed) page.copy(items = items) else page
        }
        if (!changed) return
        homePages = migrated
        store.homePages = migrated
    }

    /** Page 0 ships with a single MAD Glance widget instead of a fixed header block. */
    private fun ensureDefaultGlanceWidget() {
        val page = homePages.getOrNull(0) ?: return
        if (page.kind != HomePageKind.EMPTY || page.items.isNotEmpty()) return
        val grid = gridSizeFor(page)
        val glance = HomeItem(
            id = "glance-default",
            kind = "widget",
            widget = "glance",
            col = 0,
            row = 0,
            spanX = minOf(4f, grid.toFloat()),
            spanY = minOf(2f, grid.toFloat()),
        )
        val pages = homePages.toMutableList()
        pages[0] = page.copy(items = listOf(glance))
        homePages = pages
        store.homePages = pages
    }

    fun installedFor(kind: ActionAppKind, filter: String): List<InstalledActionApp> {
        @Suppress("UNUSED_VARIABLE")
        val observeApps = apps
        val observeCommands = actionBoxCommandsRev
        val list = actionApps.installed(kind, "").toMutableList()
        store.commandExtra(kind.name).forEach { pkg ->
            if (kind == ActionAppKind.Contacts && !ActionApps.isReliableContactInsert(pkg)) return@forEach
            if (kind == ActionAppKind.Messaging && ActionApps.isCallerIdPackage(pkg)) return@forEach
            if (list.none { it.packageName == pkg }) {
                actionApps.resolvePackage(pkg)?.let { list.add(it) }
            }
        }
        if (kind == ActionAppKind.Contacts) {
            list.removeAll { !ActionApps.isReliableContactInsert(it.packageName) }
        } else if (kind == ActionAppKind.Messaging) {
            list.removeAll { ActionApps.isCallerIdPackage(it.packageName) }
        }
        val defaultPkg = DefaultCommandApps.packageFor(getApplication(), kind)
            ?.takeIf { kind != ActionAppKind.Contacts || ActionApps.isReliableContactInsert(it) }
        if (defaultPkg != null && list.none { it.packageName == defaultPkg }) {
            actionApps.resolvePackage(defaultPkg)?.let { list.add(0, it) }
        }
        val preferred = store.commandPreferred(kind.name)
        val hidden = store.commandHidden(kind.name)
        val order = store.commandOrder(kind.name)
        val ranked = DefaultCommandApps.arrange(list, defaultPkg, preferred, hidden, order)
        @Suppress("UNUSED_VARIABLE")
        val observed = observeCommands
        if (filter.isBlank()) return ranked
        val f = filter.trim()
        return AppFuzzy.rank(f, ranked, limit = 12, cutoff = 54) {
            "${it.name} ${it.packageName}"
        }
    }

    fun matchingSearchable(query: String): List<ResolvedSearchApp> {
        return SearchableApps.matching(query, searchableApps)
    }

    fun matchingLaunchApps(query: String): List<LaunchableApp> {
        val q = query.trim()
        if (q.length < 2 || q.startsWith(":")) return emptyList()
        return AppFuzzy.rank(q, apps, limit = 8, cutoff = 56) { app ->
            "${app.label} ${app.packageName}"
        }
    }

    fun autocompleteFor(query: String): String? {
        TranslateCommandParser.autocomplete(query)?.let { return it }
        CommandAliases.autocomplete(query)?.let { return it }
        return SearchableApps.autocomplete(query, searchableApps, apps)
    }

    fun parsedTranslate(query: String): ParsedTranslate {
        if (composeKind == ActionAppKind.Translator) {
            val typed = TranslateCommandParser.parse(query)
            val text = if (typed.fromCode != null || typed.toCode != null) typed.text else query.trim()
            val from = translateFromCode ?: typed.fromCode
            val to = translateToCode ?: typed.toCode
            val slot = when {
                typed.activeSlot != null -> typed.activeSlot
                to == null -> TranslateLangSlot.To
                else -> null
            }
            return ParsedTranslate(
                fromCode = from,
                toCode = to,
                text = text,
                activeSlot = slot ?: TranslateLangSlot.To,
                partialLang = typed.partialLang,
            )
        }
        return TranslateCommandParser.parse(CommandAliases.translateTail(query))
    }

    fun translateLanguageSuggestions(query: String): List<TranslatorLanguage> {
        val parsed = parsedTranslate(query)
        return TranslatorLanguages.suggest(parsed.partialLang)
    }

    fun applyTranslateLanguage(lang: TranslatorLanguage) {
        val parsed = parsedTranslate(actionQuery)
        val slot = parsed.activeSlot ?: TranslateLangSlot.To
        if (composeKind == ActionAppKind.Translator || command is ActionCommand.Translate) {
            composeKind = ActionAppKind.Translator
            if (slot == TranslateLangSlot.From) {
                translateFromCode = lang.code
            } else {
                translateToCode = lang.code
            }
            actionQuery = parsed.text
            return
        }
        actionQuery = TranslateCommandParser.applyLanguage(actionQuery, lang)
    }

    fun pickTranslatorApp(app: InstalledActionApp, tail: String) {
        val source = tail.ifBlank { actionQuery }
        val parsed = parsedTranslate(source)
        val text = parsed.text.ifBlank { source.trim() }
        if (text.isBlank()) {
            showToast("Type text to translate first")
            return
        }
        runCatching {
            intents.openTranslator(app, text, parsed.fromCode, parsed.toCode)
        }.onFailure { showToast("Couldn't open ${app.name}") }
        showToast("Translate in ${app.name}")
        closeActionBox()
    }

    fun pickSearchScope(app: ResolvedSearchApp) {
        searchScope = app
        composeApp = null
        composeKind = null
        composeContact = null
        actionQuery = ""
    }

    fun clearSearchScope() {
        searchScope = null
    }

    fun searchInScope() {
        val scope = searchScope ?: return
        val q = actionQuery.trim()
        if (q.isEmpty()) return
        val result = runCatching {
            intents.searchInApp(
                packageName = scope.app.packageName,
                query = q,
                urls = scope.def.urlsFor(q),
                assisted = scope.def.assistedSearch,
                searchComponent = scope.searchComponent,
            )
        }.getOrDefault(SearchInAppResult.Failed)
        when (result) {
            is SearchInAppResult.AssistedClipboard ->
                showToast("Copied \"$q\" — paste into ${scope.def.displayName} search")
            is SearchInAppResult.Failed ->
                showToast("Couldn't search in ${scope.def.displayName}")
            else -> Unit
        }
        closeActionBox()
    }

    /** One-shot `:yt cats` / `:ig cats` from ActionBox. */
    private fun runInlineAppSearch(app: ResolvedSearchApp, query: String) {
        val result = runCatching {
            intents.searchInApp(
                packageName = app.app.packageName,
                query = query,
                urls = app.def.urlsFor(query),
                assisted = app.def.assistedSearch,
                searchComponent = app.searchComponent,
            )
        }.getOrDefault(SearchInAppResult.Failed)
        when (result) {
            is SearchInAppResult.AssistedClipboard ->
                showToast("Copied \"$query\" — paste into ${app.def.displayName} search")
            is SearchInAppResult.Failed ->
                showToast("Couldn't search in ${app.def.displayName}")
            else -> Unit
        }
        closeActionBox()
    }

    fun pickNotesApp(app: InstalledActionApp, text: String) {
        val body = text.ifBlank { actionQuery }.trim()
        if (body.isBlank()) {
            showToast("Type a note first")
            return
        }
        runCatching { intents.createNoteInApp(app, body) }
            .onFailure { showToast("Couldn't open ${app.name}") }
        showToast("Note in ${app.name}")
        closeActionBox()
    }

    fun pickAddContactApp(app: InstalledActionApp, details: String) {
        val raw = details.ifBlank { actionQuery }.trim()
        val (name, phone) = ContactInsertParser.parse(raw)
        if (!ActionApps.isReliableContactInsert(app.packageName)) {
            showToast("${app.name} can't add contacts — use the Contacts app")
            return
        }
        runCatching { intents.insertContact(app, name, phone) }
            .onFailure { showToast("Couldn't open ${app.name}") }
        closeActionBox()
    }

    fun pickCallApp(kind: ActionAppKind, app: InstalledActionApp, query: String) {
        searchScope = null
        composeKind = kind
        composeApp = app
        composeContact = null
        callContactQuery = query.trim()
        recipientQuery = ""
        actionQuery = ""
        RecipientHistory.mergeFromNotifs(store, notifSource)
        refreshContacts()
    }

    fun callContact(contact: ContactEntry, video: Boolean) {
        val app = composeApp ?: return
        val result = runCatching {
            if (video) intents.videoCall(app, contact) else intents.voiceCall(app, contact)
        }.getOrElse {
            showToast("Couldn't call via ${app.name}")
            closeActionBox()
            return
        }
        when (result) {
            CallResult.Started -> showToast("Calling ${contact.name} via ${app.name}")
            CallResult.Automating -> showToast("Starting call in ${app.name}…")
            CallResult.OpenedThread -> showToast("Opened ${app.name} — tap call there")
            CallResult.Failed -> showToast("Calls via ${app.name} aren't supported — see Settings → Not working")
        }
        if (result != CallResult.Failed) closeActionBox()
    }

    fun pickApp(kind: ActionAppKind, app: InstalledActionApp) {
        if (kind == ActionAppKind.Social) {
            openSocialComposer(app)
            return
        }
        searchScope = null
        composeKind = kind
        composeApp = app
        composeContact = null
        val cmd = command
        when (cmd) {
            is ActionCommand.ChatPick -> recipientQuery = cmd.filter.trim()
            is ActionCommand.Call -> callContactQuery = cmd.query.trim()
            is ActionCommand.VideoCall -> callContactQuery = cmd.query.trim()
            else -> Unit
        }
        actionQuery = ""
        RecipientHistory.mergeFromNotifs(store, notifSource)
        when (kind) {
            ActionAppKind.Messaging, ActionAppKind.Email, ActionAppKind.Social,
            ActionAppKind.VoiceCall, ActionAppKind.VideoCall,
            -> {
                if (kind == ActionAppKind.Messaging || kind == ActionAppKind.Email) refreshContacts()
            }
            ActionAppKind.Nav, ActionAppKind.Ai, ActionAppKind.Notes, ActionAppKind.Translator,
            ActionAppKind.Contacts,
            -> Unit
        }
    }

    fun openSocialComposer(app: InstalledActionApp) {
        val result = runCatching { intents.openComposer(app) }.getOrElse { PostResult.Failed }
        when (result) {
            PostResult.Started -> showToast("Opening ${app.name}")
            PostResult.Automating -> {
                showToast("Opening ${app.name} composer…")
                LauncherCommands.suppressHomeBriefly()
                MadApp.instance.deferLauncherToBack()
            }
            PostResult.PasteManual -> showToast("Opened ${app.name} — tap the composer")
            PostResult.Failed -> showToast("Couldn't open ${app.name}")
        }
        if (result != PostResult.Failed) closeActionBox()
    }

    fun pickAtApp(app: InstalledActionApp) {
        val kind = if (actionApps.find(ActionAppKind.Email, app.packageName) != null) {
            ActionAppKind.Email
        } else {
            ActionAppKind.Messaging
        }
        pickApp(kind, app)
    }

    fun installedForAt(filter: String): List<InstalledActionApp> {
        val seen = linkedSetOf<String>()
        return buildList {
            installedFor(ActionAppKind.Messaging, filter).forEach { app ->
                if (seen.add(app.packageName)) add(app)
            }
            installedFor(ActionAppKind.Email, filter).forEach { app ->
                if (seen.add(app.packageName)) add(app)
            }
        }
    }

    fun pickContact(contact: ContactEntry) {
        val app = composeApp
        val kind = composeKind
        if (app != null && (kind == ActionAppKind.Messaging || kind == ActionAppKind.Email)) {
            openConversationNow(app, contact)
            return
        }
        composeContact = contact
        actionQuery = ""
        recipientQuery = ""
        callContactQuery = ""
    }

    private fun openConversationNow(app: InstalledActionApp, contact: ContactEntry) {
        val ok = runCatching { intents.openConversation(app, contact) }.getOrDefault(false)
        if (ok) {
            showToast("Opening ${contact.name} in ${app.name}")
            LauncherCommands.suppressHomeBriefly()
            MadApp.instance.deferLauncherToBack()
            closeActionBox()
        } else {
            showToast("Couldn't open ${contact.name} in ${app.name}")
        }
    }

    /** `&` flow: contact picked → next pick message/call app. */
    fun pickLookupContact(contact: ContactEntry) {
        contactLookupActive = true
        composeApp = null
        composeKind = null
        composeContact = contact
        actionQuery = ""
        recipientQuery = ""
        callContactQuery = ""
    }

    /**
     * `&` flow after contact: messaging/email → clear field for message;
     * call app → place voice call immediately.
     */
    fun pickLookupActionApp(app: InstalledActionApp, call: Boolean) {
        val contact = composeContact ?: return
        if (call) {
            composeKind = ActionAppKind.VoiceCall
            composeApp = app
            val result = runCatching { intents.voiceCall(app, contact) }.getOrElse { CallResult.Failed }
            when (result) {
                CallResult.Started -> showToast("Calling ${contact.name} via ${app.name}")
                CallResult.Automating -> showToast("Starting call in ${app.name}…")
                CallResult.OpenedThread -> showToast("Opened ${app.name} — tap call there")
                CallResult.Failed -> showToast("Couldn't call ${contact.name} via ${app.name}")
            }
            if (result != CallResult.Failed) closeActionBox()
            return
        }
        val kind = if (actionApps.find(ActionAppKind.Email, app.packageName) != null) {
            ActionAppKind.Email
        } else {
            ActionAppKind.Messaging
        }
        composeKind = kind
        composeApp = app
        actionQuery = ""
        recipientQuery = ""
        contactLookupActive = false
        openConversationNow(app, contact)
    }

    fun clearCompose() {
        composeApp = null
        composeKind = null
        composeContact = null
        contacts = emptyList()
        callContactQuery = ""
        recipientQuery = ""
        contactLookupActive = false
        pendingUserShortcutPkg = null
        translateFromCode = null
        translateToCode = null
    }

    fun dispatchMessage(app: InstalledActionApp, contact: ContactEntry, body: String) {
        val result = runCatching { intents.sendMessage(app, contact, body) }
            .getOrElse { SendMessageResult.Failed(it.message ?: "Send failed") }
        when (result) {
            SendMessageResult.OpenedComposer -> {
                showToast("Opened ${app.name} — tap Send there")
                closeActionBox()
            }
            SendMessageResult.Automating -> {
                showToast("Pasting in ${app.name}…")
                LauncherCommands.suppressHomeBriefly()
                closeActionBox()
            }
            is SendMessageResult.Failed -> showToast(result.reason)
        }
    }

    fun onContactsPermission(granted: Boolean) {
        contactsAllowed = granted
        if (granted) refreshContacts()
    }

    fun refreshContacts() {
        val app = composeApp
        val kind = composeKind
        val cmd = command
        val typed = actionQuery.trim()
        val filter = when {
            composeContact != null -> ""
            composeKind == ActionAppKind.VoiceCall || composeKind == ActionAppKind.VideoCall -> {
                callContactQuery.ifBlank { typed }.ifBlank {
                    when (cmd) {
                        is ActionCommand.Call -> cmd.query
                        is ActionCommand.VideoCall -> cmd.query
                        else -> ""
                    }
                }
            }
            else -> recipientQuery.ifBlank { typed }.ifBlank {
                when (cmd) {
                    is ActionCommand.ChatPick -> cmd.filter
                    is ActionCommand.ContactLookup -> cmd.filter
                    else -> ""
                }
            }
        }
        val emails = kind == ActionAppKind.Email
        contactsSyncing = true
        viewModelScope.launch {
            MessagingAppRegistry.refresh(getApplication(), actionApps)
            if (SmsInboxMonitor.hasReadSms(getApplication())) {
                SmsInboxMonitor.refresh(getApplication())
            }
            val merged = RecipientPicker.load(
                contactsRepo = contactsRepo,
                store = store,
                notifs = notifSource,
                packageName = app?.packageName,
                query = filter,
                emails = emails,
            )
            contacts = merged
            contactsAllowed = contactsRepo.hasPermission()
            contactsSyncing = false
        }
    }

    fun onQueryChange(value: String) {
        if (value.isNotBlank()) commandPickerOpen = false
        val continued = CalcParser.continueFromEquals(value) ?: run {
            if (value.trim() == "=") {
                (command as? ActionCommand.Calc)?.let { CalcParser.continueValue(it.result) }
            } else {
                null
            }
        }
        if (continued != null) {
            actionQuery = continued
            scheduleInstantAnswer(continued)
            return
        }
        // Commit tokens with trailing space → clear field for the payload body.
        if (composeApp == null && composeContact == null) {
            when {
                composeKind != ActionAppKind.Translator && CommandAliases.isTranslateCommit(value) -> {
                    composeKind = ActionAppKind.Translator
                    translateFromCode = null
                    translateToCode = null
                    actionQuery = ""
                    return
                }
                composeKind != ActionAppKind.Notes && CommandAliases.isNotesCommit(value) -> {
                    composeKind = ActionAppKind.Notes
                    actionQuery = ""
                    return
                }
                composeKind != ActionAppKind.Ai && CommandAliases.isAiCommit(value) -> {
                    composeKind = ActionAppKind.Ai
                    contactLookupActive = false
                    actionQuery = ""
                    return
                }
                composeKind != ActionAppKind.Nav && CommandAliases.isNavCommit(value) -> {
                    composeKind = ActionAppKind.Nav
                    actionQuery = ""
                    return
                }
                composeKind != ActionAppKind.Contacts && CommandAliases.isAddContactCommit(value) -> {
                    composeKind = ActionAppKind.Contacts
                    pendingUserShortcutPkg = null
                    actionQuery = ""
                    return
                }
                pendingUserShortcutPkg == null && CommandAliases.isUserShortcutCommit(value) -> {
                    pendingUserShortcutPkg =
                        (UserShortcuts.matchDetailed(value.trimEnd())?.second as? ActionCommand.OpenApp)
                            ?.packageName
                    composeKind = null
                    actionQuery = ""
                    return
                }
                !contactLookupActive && CommandAliases.isContactLookupCommit(value) -> {
                    contactLookupActive = true
                    composeKind = null
                    actionQuery = ""
                    recipientQuery = ""
                    refreshContacts()
                    return
                }
            }
        }
        actionQuery = value
        if (composeApp != null && composeContact == null) {
            when (composeKind) {
                ActionAppKind.Messaging, ActionAppKind.Email -> {
                    recipientQuery = value.trim()
                    refreshContacts()
                }
                ActionAppKind.VoiceCall, ActionAppKind.VideoCall -> {
                    callContactQuery = value.trim()
                    refreshContacts()
                }
                else -> Unit
            }
            instantAnswer = null
            return
        }
        if (composeApp == null && composeContact == null &&
            (contactLookupActive || CommandDetector.detect(value) is ActionCommand.ContactLookup)
        ) {
            recipientQuery = value.removePrefix("&").trimStart()
            refreshContacts()
            instantAnswer = null
            return
        }
        if (composeApp == null) {
            val cmd = CommandDetector.detect(value)
            when (cmd) {
                is ActionCommand.Call -> callContactQuery = cmd.query
                is ActionCommand.VideoCall -> callContactQuery = cmd.query
                is ActionCommand.ContactLookup -> {
                    contactLookupActive = true
                    recipientQuery = cmd.filter
                    refreshContacts()
                }
                is ActionCommand.QuickCombo -> {
                    if (!cmd.hasContact && cmd.combo != UserShortcutCombo.SOCIAL) {
                        val app = actionApps.resolvePackage(cmd.packageName)
                        if (app != null) {
                            when (cmd.combo) {
                                UserShortcutCombo.CHAT -> pickAtApp(app)
                                UserShortcutCombo.CALL -> pickApp(ActionAppKind.VoiceCall, app)
                                UserShortcutCombo.VIDEO -> pickApp(ActionAppKind.VideoCall, app)
                                UserShortcutCombo.SOCIAL -> Unit
                            }
                            if (cmd.payload.isNotBlank()) {
                                when (cmd.combo) {
                                    UserShortcutCombo.CHAT -> {
                                        recipientQuery = cmd.payload
                                        refreshContacts()
                                    }
                                    UserShortcutCombo.CALL, UserShortcutCombo.VIDEO -> {
                                        callContactQuery = cmd.payload
                                        refreshContacts()
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
        scheduleInstantAnswer(value)
    }

    private fun scheduleInstantAnswer(query: String) {
        instantAnswerJob?.cancel()
        if (!InstantAnswerClient.shouldLookup(query)) {
            instantAnswer = null
            return
        }
        if (!appearanceSettings.instantAnswersEnabled) {
            instantAnswer = InstantModules.localAnswer(query)
            return
        }
        instantAnswerJob = viewModelScope.launch {
            delay(380)
            if (actionQuery.trim() != query.trim()) return@launch
            val hit = withContext(Dispatchers.IO) { InstantAnswerClient.fetch(query) }
            if (actionQuery.trim() == query.trim()) instantAnswer = hit
        }
    }

    fun openInstantAnswer() {
        val url = instantAnswer?.url?.trim().orEmpty()
        if (url.isNotBlank()) {
            runCatching {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                getApplication<Application>().startActivity(intent)
            }
            closeActionBox()
            return
        }
        runPrimaryAction()
    }

    fun requestOnDeviceTranslate(text: String, fromCode: String?, toCode: String?) {
        onDeviceTranslateJob?.cancel()
        if (!InstantModules.isEnabled(InstantModules.TRANSLATE) || text.isBlank() || toCode.isNullOrBlank()) {
            onDeviceTranslate = OnDeviceTranslateUi.Idle
            return
        }
        onDeviceTranslate = OnDeviceTranslateUi.Working("Translating…")
        onDeviceTranslateJob = viewModelScope.launch {
            delay(280)
            onDeviceTranslate = OnDeviceTranslateUi.Working("Downloading language if needed…")
            val result = OnDeviceTranslator.translate(text, fromCode, toCode)
            onDeviceTranslate = result
        }
    }

    private fun isRecipientPickStep(): Boolean {
        if (composeApp == null || composeContact != null) return false
        return composeKind == ActionAppKind.Messaging ||
            composeKind == ActionAppKind.Email ||
            composeKind == ActionAppKind.VoiceCall ||
            composeKind == ActionAppKind.VideoCall
    }

    private fun tryPickRecipientFromQuery(): Boolean {
        if (!isRecipientPickStep()) return false
        val q = actionQuery.trim()
        if (q.isBlank()) {
            showToast("Pick a recent conversation")
            return true
        }
        val app = composeApp ?: return true
        val kind = composeKind ?: return true
        val filter = when (kind) {
            ActionAppKind.VoiceCall, ActionAppKind.VideoCall -> callContactQuery.ifBlank { q }
            else -> recipientQuery.ifBlank { q }
        }
        val matches = runBlocking {
            RecipientPicker.load(
                contactsRepo = contactsRepo,
                store = store,
                notifs = notifSource,
                packageName = app.packageName,
                query = filter,
                emails = kind == ActionAppKind.Email,
            )
        }
        contacts = matches
        val match = matches.firstOrNull()
        if (match == null) {
            showToast(if (contactsRepo.hasPermission()) "No matches in ${app.name} or phone contacts" else "No recent conversations in ${app.name}")
            return true
        }
        when (kind) {
            ActionAppKind.VoiceCall -> callContact(match, video = false)
            ActionAppKind.VideoCall -> callContact(match, video = true)
            else -> pickContact(match)
        }
        return true
    }

    fun runPrimaryAction() {
        eatActionEnter = true
        val cmd = command
        val app = composeApp
        val kind = composeKind
        val contact = composeContact
        val inlineSearch = SearchableApps.parseInlineSearch(actionQuery, searchableApps)
        when {
            inlineSearch != null -> {
                val (searchApp, q) = inlineSearch
                runInlineAppSearch(searchApp, q)
            }
            searchScope != null && actionQuery.isNotBlank() -> searchInScope()
            app != null && kind == ActionAppKind.Social && actionQuery.isNotBlank() -> {
                val result = runCatching { intents.sharePost(app, actionQuery) }.getOrElse { PostResult.Failed }
                when (result) {
                    PostResult.Started -> showToast("Post in ${app.name}")
                    PostResult.Automating -> {
                        showToast("Opening ${app.name} composer…")
                        LauncherCommands.suppressHomeBriefly()
                    }
                    PostResult.PasteManual -> showToast("Copied — paste in ${app.name} (Meta blocks filling from MAD)")
                    PostResult.Failed -> showToast("Couldn't post to ${app.name} — see Not working")
                }
                if (result != PostResult.Failed) closeActionBox()
            }
            app != null && contact != null && actionQuery.isNotBlank() && kind == ActionAppKind.Email -> {
                runCatching { intents.sendEmail(app, contact, actionQuery) }
                showToast("Email ${contact.name}")
                closeActionBox()
            }
            app != null && contact != null && actionQuery.isNotBlank() -> {
                dispatchMessage(app, contact, actionQuery)
            }
            cmd is ActionCommand.Call && CommandDetector.isPhoneLike(cmd.query) -> {
                val app = composeApp
                if (app != null && composeKind == ActionAppKind.VoiceCall) {
                    runCatching {
                        intents.voiceCall(
                            app,
                            ContactEntry(id = "dial", name = cmd.query.trim(), phone = cmd.query.trim()),
                        )
                    }
                } else {
                    runCatching { intents.dial(cmd.query) }
                }
                closeActionBox()
            }
            cmd is ActionCommand.VideoCall && CommandDetector.isPhoneLike(cmd.query) -> {
                val app = composeApp
                if (app != null && composeKind == ActionAppKind.VideoCall) {
                    runCatching {
                        intents.videoCall(
                            app,
                            ContactEntry(id = "dial", name = cmd.query.trim(), phone = cmd.query.trim()),
                        )
                    }
                } else {
                    runCatching { intents.videoDial(cmd.query) }
                }
                closeActionBox()
            }
            cmd is ActionCommand.ContactLookup -> {
                if (composeContact == null) {
                    if (actionQuery.isBlank() && !contactLookupActive) {
                        contactLookupActive = true
                        refreshContacts()
                    } else {
                        // Try resolve typed name as contact.
                        val matches = contacts.filter {
                            it.name.contains(actionQuery.trim(), ignoreCase = true)
                        }
                        matches.firstOrNull()?.let { pickLookupContact(it) }
                            ?: showToast("Pick a contact")
                    }
                } else if (composeApp == null) {
                    showToast("Pick a message or call app")
                }
            }
            cmd is ActionCommand.PostPick -> {
                val apps = installedFor(ActionAppKind.Social, cmd.filter.trim())
                when {
                    apps.size == 1 -> openSocialComposer(apps.first())
                    apps.isEmpty() -> showToast("No social apps found")
                    else -> showToast("Pick a social app")
                }
            }
            cmd is ActionCommand.QuickCombo -> runQuickCombo(cmd)
            cmd is ActionCommand.OpenApp -> runUserShortcut(cmd.packageName, cmd.payload)
            cmd is ActionCommand.Help -> {
                // Command list is already showing in Action Box.
            }
            cmd is ActionCommand.AddUserCommand -> {
                // Wizard stays in the Action Box pill.
            }
            cmd is ActionCommand.UpdateApps -> {
                val ok = runCatching { intents.openAppUpdates() }.getOrDefault(false)
                if (!ok) showToast(overlayString(R.string.toast_updates_unavailable))
                closeActionBox()
            }
            cmd is ActionCommand.BarrelRoll -> playBarrelRoll()
            cmd is ActionCommand.SystemSettings -> {
                runCatching { intents.openSystemSettings() }
                closeActionBox()
            }
            cmd is ActionCommand.LauncherSettings -> {
                if (independentActionBox) openHomeSettings() else {
                    closeActionBox()
                    openHomeSettings()
                }
            }
            cmd is ActionCommand.ActionBoxSettings -> {
                if (independentActionBox) openActionBoxSettings() else {
                    closeActionBox()
                    openActionBoxSettings()
                }
            }
            cmd is ActionCommand.Reminder -> saveReminder(cmd.text)
            cmd is ActionCommand.Note -> saveNote(cmd.text, cmd.floating)
            cmd is ActionCommand.NotePick -> {
                val apps = installedFor(ActionAppKind.Notes, "")
                when {
                    apps.isEmpty() -> showToast("No notes apps installed")
                    apps.size == 1 -> pickNotesApp(apps.first(), cmd.text)
                    else -> showToast("Pick a notes app")
                }
            }
            cmd is ActionCommand.AddContact -> {
                val apps = installedFor(ActionAppKind.Contacts, "")
                val target = DefaultCommandApps.pickAddContactTarget(
                    apps,
                    systemDefaultPackage(ActionAppKind.Contacts),
                )
                when {
                    target != null -> pickAddContactApp(target, cmd.details.ifBlank { actionQuery })
                    apps.none { ActionApps.isReliableContactInsert(it.packageName) } ->
                        showToast("No contacts apps installed")
                    else -> showToast("Pick a contacts app")
                }
            }
            cmd is ActionCommand.Translate -> {
                val apps = installedFor(ActionAppKind.Translator, "")
                when {
                    apps.isEmpty() -> showToast("No translator apps installed")
                    apps.size == 1 -> pickTranslatorApp(apps.first(), cmd.tail)
                    else -> showToast("Pick a translator app")
                }
            }
            cmd is ActionCommand.AiAsk -> {
                val apps = installedFor(ActionAppKind.Ai, "")
                when {
                    apps.isEmpty() -> showToast("No AI assistants installed")
                    apps.size == 1 -> askAi(apps.first(), cmd.prompt)
                    cmd.prompt.isBlank() -> showToast("Pick an AI assistant")
                    else -> showToast("Pick an AI assistant")
                }
            }
            cmd is ActionCommand.Calc -> {
                val copied = instantAnswer?.takeIf { it.fromDuckDuckGo }?.text ?: cmd.result.answer
                copyText(copied)
                showToast("Copied $copied")
            }
            isRecipientPickStep() -> tryPickRecipientFromQuery()
            cmd is ActionCommand.None && actionQuery.isNotBlank() -> {
                runCatching { intents.webSearch(actionQuery, appearanceSettings.webSearchEngine) }
                showToast("Search: $actionQuery")
                closeActionBox()
            }
        }
    }

    private fun copyText(text: String) {
        val cm = getApplication<Application>().getSystemService(android.content.Context.CLIPBOARD_SERVICE)
            as? android.content.ClipboardManager ?: return
        cm.setPrimaryClip(android.content.ClipData.newPlainText("MAD calc", text))
    }

    fun confirmEmergency() {
        runCatching { intents.dial("911") }
        closeActionBox()
    }

    fun saveReminder(text: String) {
        runCatching { intents.setReminder(text) }
            .onSuccess { closeActionBox() }
            .onFailure { showToast(it.message ?: overlayString(R.string.toast_reminder_no_clock)) }
    }

    fun saveNote(text: String, floating: Boolean) {
        val body = text.trim().ifBlank { if (floating) "" else "Note" }
        val collapsed = floating && body.isNotBlank()
        val note = store.addNote(body, floating, collapsed = collapsed)
        notes = store.notes()
        floatingNote = if (floating) note else notes.lastOrNull { it.floating }
        if (floating) editingStickyId = if (collapsed) null else note.id
        if (floating && independentActionBox) {
            showToast("Use /n to note in an app")
            closeActionBox()
            return
        }
        showToast(if (floating) "Sticky note added" else "Note saved")
        closeActionBox()
    }

    fun createStickyAndEdit() {
        homeEditMode = false
        saveNote("", floating = true)
    }

    fun updateFloatingNoteText(text: String) {
        val id = floatingNote?.id ?: return
        store.updateNoteText(id, text)
        refreshFloatingNote(id)
    }

    fun updateFloatingNoteTitle(title: String) {
        val id = floatingNote?.id ?: return
        store.updateNote(id) { it.copy(title = title) }
        refreshFloatingNote(id)
    }

    fun setFloatingNoteCollapsed(collapsed: Boolean) {
        val id = floatingNote?.id ?: return
        if (collapsed) editingStickyId = null
        store.updateNote(id) { it.copy(collapsed = collapsed) }
        refreshFloatingNote(id)
    }

    fun setFloatingNoteLayout(x: Int, y: Int, edge: String?) {
        val id = floatingNote?.id ?: return
        store.updateNote(id) { it.copy(x = x, y = y, pinnedEdge = edge) }
        refreshFloatingNote(id)
    }

    fun addStickyImages(uris: List<android.net.Uri>) {
        val id = floatingNote?.id ?: return
        val ctx = getApplication<Application>()
        val paths = uris.mapNotNull { com.madlauncher.overlay.StickyNoteImages.copyFromUri(ctx, id, it) }
        if (paths.isEmpty()) return
        store.updateNote(id) { it.copy(imagePaths = it.imagePaths + paths) }
        refreshFloatingNote(id)
    }

    fun removeStickyImage(path: String) {
        val id = floatingNote?.id ?: return
        com.madlauncher.overlay.StickyNoteImages.delete(path)
        store.updateNote(id) { it.copy(imagePaths = it.imagePaths.filterNot { p -> p == path }) }
        refreshFloatingNote(id)
    }

    private fun refreshFloatingNote(id: String) {
        notes = store.notes()
        floatingNote = notes.find { it.id == id }
    }

    fun clearEditingSticky() {
        editingStickyId = null
    }

    fun dismissFloatingNote() {
        floatingNote?.let { store.dismissFloating(it.id) }
        floatingNote = null
        editingStickyId = null
        notes = store.notes()
    }

    fun saveEvent(title: String, date: String, time: String) {
        val whenLabel = listOf(date, time).filter { it.isNotBlank() }.joinToString(" ")
        store.addEvent(title, whenLabel)
        val begin = parseWhen(date, time)
        runCatching { intents.insertCalendar(title, begin, begin?.let { it + 3_600_000 }) }
        showToast("Event: $title")
        closeActionBox()
    }

    fun navigateWith(app: InstalledActionApp, destination: String) {
        if (destination.isBlank()) {
            showToast("Type a destination")
            return
        }
        runCatching { intents.navigate(app, destination) }
        closeActionBox()
    }

    fun askAi(app: InstalledActionApp, prompt: String) {
        val body = prompt.ifBlank { actionQuery }.trim()
        runCatching { intents.askAi(app, body) }
            .onSuccess {
                showToast(
                    if (body.isBlank()) "Opening ${app.name}"
                    else "Asking ${app.name}",
                )
                closeActionBox()
            }
            .onFailure { showToast("Couldn't open ${app.name}") }
    }

    fun systemDefaultPackage(kind: ActionAppKind): String? =
        DefaultCommandApps.packageFor(getApplication(), kind)

    fun commandAppEditorRows(kind: ActionAppKind): List<CommandAppEditorRow> {
        @Suppress("UNUSED_VARIABLE")
        val observe = actionBoxCommandsRev to apps
        val discovered = actionApps.installed(kind, "").toMutableList()
        store.commandExtra(kind.name).forEach { pkg ->
            if (kind == ActionAppKind.Contacts && !ActionApps.isReliableContactInsert(pkg)) return@forEach
            if (kind == ActionAppKind.Messaging && ActionApps.isCallerIdPackage(pkg)) return@forEach
            if (discovered.none { it.packageName == pkg }) {
                actionApps.resolvePackage(pkg)?.let { discovered.add(it) }
            }
        }
        if (kind == ActionAppKind.Contacts) {
            discovered.removeAll { !ActionApps.isReliableContactInsert(it.packageName) }
        } else if (kind == ActionAppKind.Messaging) {
            discovered.removeAll { ActionApps.isCallerIdPackage(it.packageName) }
        }
        val defaultPkg = systemDefaultPackage(kind)
            ?.takeIf { kind != ActionAppKind.Contacts || ActionApps.isReliableContactInsert(it) }
        if (defaultPkg != null && discovered.none { it.packageName == defaultPkg }) {
            actionApps.resolvePackage(defaultPkg)?.let { discovered.add(0, it) }
        }
        val preferred = store.commandPreferred(kind.name)
        val hidden = store.commandHidden(kind.name)
        val order = store.commandOrder(kind.name)
        val ranked = DefaultCommandApps.arrange(
            apps = discovered,
            defaultPackage = defaultPkg,
            preferredPackage = preferred,
            hidden = hidden,
            order = order,
            pinDefault = false,
        )
        return ranked.map { app ->
            CommandAppEditorRow(
                app = app,
                systemDefault = app.packageName == defaultPkg,
                preferred = app.packageName == preferred,
            )
        }
    }

    fun addCommandApp(kind: ActionAppKind, packageName: String) {
        if (kind == ActionAppKind.Contacts && !ActionApps.isReliableContactInsert(packageName)) return
        if (kind == ActionAppKind.Messaging && ActionApps.isCallerIdPackage(packageName)) return
        patchCommandMembership(kind, packageName, include = true)
        actionBoxCommandsRev++
    }

    fun removeCommandApp(kind: ActionAppKind, packageName: String) {
        patchCommandMembership(kind, packageName, include = false)
        actionBoxCommandsRev++
    }

    fun dropCommandApp(from: ActionAppKind, packageName: String, to: ActionAppKind, toIndex: Int) {
        if (from == to) {
            val order = commandAppEditorRows(from).map { it.app.packageName }.toMutableList()
            if (!order.remove(packageName)) return
            order.add(toIndex.coerceIn(0, order.size), packageName)
            store.setCommandOrder(from.name, order)
            actionBoxCommandsRev++
            return
        }
        patchCommandMembership(from, packageName, include = false)
        patchCommandMembership(to, packageName, include = true)
        val dest = commandAppEditorRows(to).map { it.app.packageName }.toMutableList()
        dest.remove(packageName)
        dest.add(toIndex.coerceIn(0, dest.size), packageName)
        store.setCommandOrder(to.name, dest)
        actionBoxCommandsRev++
    }

    private fun patchCommandMembership(kind: ActionAppKind, packageName: String, include: Boolean) {
        val extra = store.commandExtra(kind.name).toMutableList()
        val hidden = store.commandHidden(kind.name).toMutableSet()
        val order = store.commandOrder(kind.name).toMutableList()
        if (include) {
            if (extra.none { it == packageName }) extra.add(packageName)
            hidden.remove(packageName)
            if (order.none { it == packageName }) order.add(packageName)
        } else {
            extra.removeAll { it == packageName }
            hidden.add(packageName)
            order.removeAll { it == packageName }
            if (store.commandPreferred(kind.name) == packageName) {
                store.setCommandPreferred(kind.name, null)
            }
        }
        store.setCommandExtra(kind.name, extra)
        store.setCommandHidden(kind.name, hidden)
        store.setCommandOrder(kind.name, order)
    }

    fun setPreferred(kind: ActionAppKind, packageName: String, announce: Boolean = true) {
        store.setCommandPreferred(kind.name, packageName)
        when (kind) {
            ActionAppKind.Messaging -> preferredMessaging = packageName
            ActionAppKind.Email -> preferredEmail = packageName
            ActionAppKind.Social -> preferredSocial = packageName
            ActionAppKind.Nav -> preferredNav = packageName
            else -> Unit
        }
        actionBoxCommandsRev++
        if (announce) showToast("Preferred app saved")
    }

    fun openListenerSettings() {
        runCatching { intents.openNotificationListenerSettings() }
    }

    fun listenerEnabled(): Boolean = grants.notificationListener || listenerConnected

    fun submitHelpImprove(name: String, email: String, report: String) {
        if (report.isBlank()) {
            showToast("Write an idea or bug report first")
            return
        }
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val result = FeedbackSubmitter.submitHelpImprove(ctx, madVersionName(), name, email, report)
            showToast(result.message)
        }
    }

    private fun overlayString(id: Int, vararg args: Any): String {
        val ctx = getApplication<Application>()
        val template = LanguageOverlay.wrapGetString(ctx.resources, id) { ctx.getString(id) }
        return if (args.isEmpty()) template else String.format(template, *args)
    }

    fun showToast(message: String) {
        toast = message
        toastJob?.cancel()
        toastJob = viewModelScope.launch {
            delay(2200)
            if (toast == message) toast = null
        }
    }

    private fun parseWhen(date: String, time: String): Long? {
        if (date.isBlank() && time.isBlank()) return null
        val cal = Calendar.getInstance()
        val dateHit = Regex("(\\d{4})-(\\d{1,2})-(\\d{1,2})").find(date)
            ?: Regex("(\\d{1,2})/(\\d{1,2})").find(date)
        if (dateHit != null) {
            val g = dateHit.groupValues
            if (g.size >= 4) {
                cal.set(g[1].toInt(), g[2].toInt() - 1, g[3].toInt())
            } else {
                cal.set(Calendar.MONTH, g[1].toInt() - 1)
                cal.set(Calendar.DAY_OF_MONTH, g[2].toInt())
            }
        }
        val timeHit = Regex("(\\d{1,2}):(\\d{2})\\s*(am|pm)?", RegexOption.IGNORE_CASE).find(time)
        if (timeHit != null) {
            var hour = timeHit.groupValues[1].toInt()
            val minute = timeHit.groupValues[2].toInt()
            val ampm = timeHit.groupValues.getOrNull(3)?.lowercase(Locale.getDefault())
            if (ampm == "pm" && hour < 12) hour += 12
            if (ampm == "am" && hour == 12) hour = 0
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
        }
        return cal.timeInMillis
    }
}
