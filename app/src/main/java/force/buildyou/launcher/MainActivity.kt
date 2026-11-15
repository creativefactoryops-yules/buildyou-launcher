package force.buildyou.launcher

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private const val TAG = "BuildYou"

/**
 * Single-activity launcher.
 *
 * - Manifest: launchMode="singleTask" + clearTaskOnLaunch → pressing Home returns to
 *   this deck instead of stacking a new activity instance.
 * - onNewIntent: HOME re-dispatches while the instance is alive land here.
 * - savedInstanceState: Compose state survives config changes via remember/state;
 *   the hidden-package set and deck order live in SharedPreferences, so nothing
 *   extra needs manual restore.
 *
 * Debug signing: assembleDebug signs with AGP's auto-generated debug keystore —
 * no signingConfig is declared in build.gradle.kts.
 */
class MainActivity : ComponentActivity() {

    private lateinit var inventory: AppInventory
    private lateinit var prefs: LauncherPrefs

    private val hiddenState = mutableStateOf(setOf<String>())
    private val appsState = mutableStateOf<List<AppEntry>>(emptyList())
    private lateinit var terminal: TerminalEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        inventory = AppInventory(this)
        prefs = LauncherPrefs(this)
        hiddenState.value = prefs.loadHidden()
        appsState.value = decorate(inventory.load(), hiddenState.value, prefs.loadOrder())

        // Terminal engine wired to REAL actions on this activity. The lambdas read
        // live state (not snapshot copies) so the terminal always sees current truth.
        terminal = TerminalEngine(
            inventory = inventory,
            reloadInventory = ::reloadInventory,
            launchApp = ::launchApp,
            hiddenPackages = { hiddenState.value },
            unhidePackage = ::unhideApp,
        )

        setContent {
            LauncherRoot(
                apps = appsState.value,
                engine = terminal,
                onLaunch = ::launchApp,
                onAppInfo = ::openAppInfo,
                onUninstall = ::requestUninstall,
                onHide = ::hideApp,
            )
        }

        if (savedInstanceState == null) {
            Log.d(TAG, "cold start — inventory=${appsState.value.size} hidden=${hiddenState.value.size}")
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // A HOME intent re-dispatched while we are alive: rescan in case apps were
        // installed or uninstalled while another activity was frontmost.
        Log.d(TAG, "onNewIntent action=${intent.action}")
        reloadInventory()
    }

    override fun onResume() {
        super.onResume()
        // Returning to the deck: cheap rescan so new installs appear without --force.
        reloadInventory()
    }

    private fun reloadInventory() {
        appsState.value = decorate(inventory.load(), hiddenState.value, prefs.loadOrder())
    }

    /** Apply the hidden set and the persisted order to the raw inventory. */
    private fun decorate(
        raw: List<AppEntry>,
        hidden: Set<String>,
        order: List<String>,
    ): List<AppEntry> {
        val visible = raw.filter { it.packageName !in hidden }
        if (order.isEmpty()) return visible // already label-sorted by AppInventory
        // Persisted order first (still-installed apps only), unseen apps after in label order.
        val rank = order.withIndex().associate { (i, pkg) -> pkg to i }
        return visible.sortedBy { rank[it.packageName] ?: Int.MAX_VALUE }
    }

    /** Launch the real app; null launch intent falls back to App Info. */
    private fun launchApp(pkg: String) {
        val intent = inventory.launchIntentFor(pkg)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { startActivity(intent) }
                .onFailure { Log.w(TAG, "launch failed for $pkg: ${it.message}") }
        } else {
            Log.w(TAG, "no launch intent for $pkg — opening App Info")
            openAppInfo(pkg)
        }
    }

    private fun openAppInfo(pkg: String) {
        runCatching { startActivity(inventory.appDetailsIntent(pkg)) }
            .onFailure { Log.w(TAG, "app info failed for $pkg: ${it.message}") }
    }

    private fun requestUninstall(pkg: String) {
        runCatching { startActivity(inventory.uninstallIntent(pkg)) }
            .onFailure { Log.w(TAG, "uninstall request failed for $pkg: ${it.message}") }
    }

    private fun hideApp(pkg: String) {
        val next = hiddenState.value + pkg
        hiddenState.value = next
        prefs.saveHidden(next)
        appsState.value = decorate(inventory.load(), next, prefs.loadOrder())
    }

    private fun unhideApp(pkg: String): Boolean {
        if (pkg !in hiddenState.value) return false
        val next = hiddenState.value - pkg
        hiddenState.value = next
        prefs.saveHidden(next)
        appsState.value = decorate(inventory.load(), next, prefs.loadOrder())
        return true
    }
}

/**
 * Root layout: brand header on top, app deck in the middle (weight 1), terminal pane
 * pinned at the bottom. The usable width is measured from the real layout so the
 * wordmark auto-shrink and deck column count size against actual available dp.
 */
@Composable
private fun LauncherRoot(
    apps: List<AppEntry>,
    engine: TerminalEngine,
    onLaunch: (String) -> Unit,
    onAppInfo: (String) -> Unit,
    onUninstall: (String) -> Unit,
    onHide: (String) -> Unit,
) {
    var usableWidth by remember { mutableStateOf(360f) }
    val density = LocalDensity.current

    LaunchedEffect(Unit) { engine.printBanner() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                val dp = size.width / density.density
                if (dp > 0f) usableWidth = dp
            },
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            BrandHeader(
                maxWidth = usableWidth.dp,
                modifier = Modifier.padding(
                    start = 16.dp, end = 16.dp, top = 18.dp, bottom = 10.dp,
                ),
            )
            AppDeck(
                apps = apps,
                usableWidthDp = usableWidth,
                onLaunch = { onLaunch(it.packageName) },
                onAppInfo = { onAppInfo(it.packageName) },
                onUninstall = { onUninstall(it.packageName) },
                onHide = { onHide(it.packageName) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
            TerminalPane(engine = engine)
        }
    }
}
