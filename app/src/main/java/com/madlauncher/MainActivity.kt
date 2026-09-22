package com.madlauncher

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.LauncherApps
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.madlauncher.AppearanceBridge
import com.madlauncher.data.AppearanceSettings
import com.madlauncher.data.LocalStore
import com.madlauncher.notif.SystemShadeController
import com.madlauncher.pin.ConfirmPinActivity
import com.madlauncher.pin.PinShortcutHelper
import com.madlauncher.ui.LauncherScreen
import com.madlauncher.ui.LanguageOverlayContextWrapper

class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MadPin"
    }
    private val appearanceListener: (AppearanceSettings) -> Unit = { appearance ->
        applySystemBars(appearance)
        applyKeepPortraitOrientation(appearance)
        SystemShadeController.syncWithAppearance(this, appearance)
    }

    private var overlayResources: Resources? = null
    private var overlayResourcesConfigHash: Int = 0

    override fun getResources(): Resources {
        val base = super.getResources()
        val hash = base.configuration.hashCode()
        if (overlayResources == null || overlayResourcesConfigHash != hash) {
            overlayResources = LanguageOverlayContextWrapper.OverlayResources(base)
            overlayResourcesConfigHash = hash
        }
        return overlayResources!!
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(MadApp.englishContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppearanceBridge.addListener(appearanceListener)
        applySystemBars(LocalStore(this).appearanceSettings)
        applyKeepPortraitOrientation(LocalStore(this).appearanceSettings)
        SystemShadeController.syncWithAppearance(this, LocalStore(this).appearanceSettings)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER)
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    (application as? MadApp)?.backHandler?.invoke()
                }
            },
        )
        handleOpen(intent)
        handleNavShortcut(intent)
        handlePinExtras(intent, source = "onCreate")
        setContent {
            LauncherScreen(rootModifier = Modifier.fillMaxSize())
        }
    }

    override fun startActivity(intent: Intent, options: android.os.Bundle?) {
        LauncherWindowDismiss.noteIfKeepWindows(intent)
        super.startActivity(intent, options)
    }

    @Deprecated("Framework still funnels Activity Result launches through this.")
    override fun startActivityForResult(intent: Intent, requestCode: Int, options: android.os.Bundle?) {
        LauncherWindowDismiss.noteIfKeepWindows(intent)
        @Suppress("DEPRECATION")
        super.startActivityForResult(intent, requestCode, options)
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Recents / Home from the nav bar — close MAD panels so they are not still
        // open in the Overview thumbnail or when returning. Skip grant/picker leaves.
        if (LauncherWindowDismiss.shouldPreserve()) return
        (application as? MadApp)?.windowDismisser?.invoke()
    }

    override fun onDestroy() {
        if ((application as? MadApp)?.launcherActivity === this) {
            (application as? MadApp)?.launcherActivity = null
        }
        AppearanceBridge.removeListener(appearanceListener)
        super.onDestroy()
    }

    override fun onStart() {
        super.onStart()
        runCatching { (application as MadApp).widgetHost.ensureListening() }
    }

    override fun onResume() {
        super.onResume()
        (application as? MadApp)?.launcherActivity = this
        runCatching { (application as MadApp).widgetHost.ensureListening() }
        val appearance = LocalStore(this).appearanceSettings
        applySystemBars(appearance)
        SystemShadeController.syncWithAppearance(this, appearance)
        com.madlauncher.data.WallpaperApplier.bindToScreen(this)
        // Recents "Clear all" orphans a11y overlays; re-attach when home is visible again.
        NavShortcutBridge.requestLayout()
        runCatching { com.madlauncher.systemui.MadNavBarOverlay.sync(this) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        Log.i(
            TAG,
            "onNewIntent action=${intent.action} cats=${intent.categories} extras=${intent.extras?.keySet()}",
        )
        handleOpen(intent)
        handleNavShortcut(intent)
        if (handlePinExtras(intent, source = "onNewIntent")) {
            return
        }
        // Don't treat panel-open intents as Home — that closes the window we just requested.
        if (!intent.getStringExtra("open").isNullOrBlank()) return
        if (LauncherCommands.isHomeSuppressed()) return
        (application as? MadApp)?.notifyHomePressed()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_HOME && event.action == KeyEvent.ACTION_DOWN) {
            (application as? MadApp)?.notifyHomePressed()
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        com.madlauncher.data.WallpaperApplier.bindToScreen(this)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applySystemBars(LocalStore(this).appearanceSettings)
            com.madlauncher.data.WallpaperApplier.bindToScreen(this)
            NavShortcutBridge.requestLayout()
        }
    }

    private fun handleOpen(intent: Intent?) {
        intent?.getStringExtra("open")?.let { (application as? MadApp)?.openLauncherPanel(it) }
    }

    private fun handleNavShortcut(intent: Intent?) {
        if (intent?.action != com.madlauncher.platform.MadOsRuntime.ACTION_NAV_SHORTCUT) return
        if (!NavShortcutBridge.enabled) return
        val kind = com.madlauncher.platform.MadOsRuntime.navKindFromExtra(
            intent.getStringExtra(com.madlauncher.platform.MadOsRuntime.EXTRA_NAV_KIND),
        ) ?: return
        (application as? MadApp)?.openLauncherPanel(
            com.madlauncher.platform.MadOsRuntime.panelForKind(kind),
        )
    }

    /**
     * Some devices deliver [LauncherApps.EXTRA_PIN_ITEM_REQUEST] to the home activity
     * instead of starting [ConfirmPinActivity]. Accept and place immediately.
     */
    private fun handlePinExtras(intent: Intent?, source: String): Boolean {
        if (intent == null || Build.VERSION.SDK_INT < 26) return false
        val launcherApps = getSystemService(LauncherApps::class.java) ?: return false
        val request = launcherApps.getPinItemRequest(intent)
            ?: run {
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT >= 33) {
                    intent.getParcelableExtra(
                        LauncherApps.EXTRA_PIN_ITEM_REQUEST,
                        LauncherApps.PinItemRequest::class.java,
                    )
                } else {
                    intent.getParcelableExtra(LauncherApps.EXTRA_PIN_ITEM_REQUEST)
                }
            }
        if (request == null) return false
        Log.i(TAG, "pin extras via $source type=${request.requestType} valid=${request.isValid}")
        if (!request.isValid) return false
        if (request.requestType != LauncherApps.PinItemRequest.REQUEST_TYPE_SHORTCUT) {
            val forward = Intent(this, ConfirmPinActivity::class.java).apply {
                action = LauncherApps.ACTION_CONFIRM_PIN_APPWIDGET
                putExtras(intent)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(forward)
            return true
        }
        val info = request.shortcutInfo ?: return false
        if (!request.accept()) {
            Log.w(TAG, "pin accept() failed via $source")
            Toast.makeText(this, "Couldn't add that shortcut", Toast.LENGTH_SHORT).show()
            return true
        }
        val density = resources.displayMetrics.densityDpi
        val drawable = runCatching { launcherApps.getShortcutIconDrawable(info, density) }.getOrNull()
        val bmp = drawable?.let { d ->
            if (d is android.graphics.drawable.BitmapDrawable && d.bitmap != null) {
                d.bitmap
            } else {
                val w = d.intrinsicWidth.takeIf { it > 0 } ?: 192
                val h = d.intrinsicHeight.takeIf { it > 0 } ?: 192
                val b = android.graphics.Bitmap.createBitmap(w, h, android.graphics.Bitmap.Config.ARGB_8888)
                val c = android.graphics.Canvas(b)
                d.setBounds(0, 0, w, h)
                d.draw(c)
                b
            }
        }
        val id = "sc${System.currentTimeMillis()}"
        val iconPath = bmp?.let { PinShortcutHelper.saveIcon(this, id, it) }.orEmpty()
        val ok = PinShortcutHelper.placeShortcut(
            context = this,
            id = id,
            title = info.shortLabel?.toString()?.ifBlank { info.longLabel?.toString() } ?: "Shortcut",
            packageName = info.`package`,
            shortcutId = info.id,
            intentUri = "",
            iconPath = iconPath,
        )
        Toast.makeText(this, if (ok) "Added to home" else "Couldn't place on home", Toast.LENGTH_SHORT).show()
        return true
    }

    private fun applySystemBars(appearance: AppearanceSettings) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        if (Build.VERSION.SDK_INT >= 29) {
            window.isNavigationBarContrastEnforced = true
        }
        val dark = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView).apply {
            show(WindowInsetsCompat.Type.statusBars())
            show(WindowInsetsCompat.Type.navigationBars())
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }

    private fun applyKeepPortraitOrientation(appearance: AppearanceSettings) {
        requestedOrientation = if (appearance.landscapeRotateContents) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

}
