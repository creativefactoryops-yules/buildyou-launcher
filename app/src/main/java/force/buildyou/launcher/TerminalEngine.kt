package force.buildyou.launcher

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Terminal pane: a real command line bound to actual actions.
 * Every command either performs a real action or honestly reports that it is
 * not implemented — there are no fake successes.
 */
class TerminalEngine(
    private val inventory: AppInventory,
    private val reloadInventory: () -> Unit,
    private val launchApp: (String) -> Unit,
    private val hiddenPackages: () -> Set<String>,
    private val unhidePackage: (String) -> Boolean,
) {

    /** One line of terminal output. */
    data class Line(val text: String, val kind: Kind) {
        enum class Kind { INPUT, OUTPUT, OK, WARN, ERROR }
    }

    private val buffer = mutableListOf<Line>()

    var output by mutableStateOf<List<Line>>(emptyList())
        private set

    /** Command history for up/down recall. */
    val history = mutableListOf<String>()
    private var historyIndex: Int = -1

    private fun emit(text: String, kind: Line.Kind = Line.Kind.OUTPUT) {
        buffer += Line(text, kind)
        output = buffer.toList()
    }

    fun printBanner() {
        emit("--force ./BUILD YOU — launcher terminal v2.0.0", Line.Kind.OK)
        emit("type `help` for commands", Line.Kind.WARN)
    }

    fun execute(raw: String) {
        val input = raw.trim()
        emit("\$ $input", Line.Kind.INPUT)
        historyIndex = -1
        if (input.isEmpty()) return
        history += input
        val parts = input.split(Regex("\\s+"))
        val cmd = parts.first()
        val arg = parts.drop(1).joinToString(" ")
        when (cmd) {
            "help" -> help()
            "apps" -> apps()
            "open" -> open(arg)
            "wall" -> wall(arg)
            "brand" -> brand()
            "icon" -> icon()
            "reload" -> force()
            // v1 accepted "--force" as the reload command; it collided with the
            // brand stamp, so v2 renamed it. Kept as a silent alias so nothing
            // the owner already typed breaks.
            "--force" -> force()
            "hidden" -> hidden()
            "unhide" -> unhide(arg)
            "date" -> date()
            "clear" -> clear()
            else -> emit("unknown command: $cmd — try `help`", Line.Kind.ERROR)
        }
    }

    /** Previous history entry (up-arrow), or null. */
    fun recallBackward(): String? {
        if (history.isEmpty()) return null
        if (historyIndex == -1) historyIndex = history.size - 1
        else if (historyIndex > 0) historyIndex--
        return history[historyIndex]
    }

    /** Next history entry (down-arrow), or null when back at the live prompt. */
    fun recallForward(): String? {
        if (historyIndex == -1) return null
        if (historyIndex < history.size - 1) {
            historyIndex++
            return history[historyIndex]
        }
        historyIndex = -1
        return null
    }

    private fun help() {
        emit("commands:")
        emit("  help              this list")
        emit("  apps              list installed launcher apps")
        emit("  open <name>       launch an app by label or package (case-insensitive)")
        emit("  wall <color>      set solid-color wallpaper (#rrggbb or name)")
        emit("  brand             print the brand spec")
        emit("  icon              print the icon-ladder spec")
        emit("  reload            reload app inventory (v1 spelled it --force)")
        emit("  hidden            list hidden packages")
        emit("  unhide <pkg>      unhide a package")
        emit("  date              print today's date")
        emit("  clear             clear the terminal")
    }

    private fun visibleApps(): List<AppEntry> =
        runCatching { inventory.load() }.getOrDefault(emptyList())
            .filter { it.packageName !in hiddenPackages() }

    private fun apps() {
        val apps = visibleApps()
        if (apps.isEmpty()) {
            emit("no launcher apps visible — run `reload` to rescan", Line.Kind.WARN)
            return
        }
        apps.forEachIndexed { i, app ->
            val n = (i + 1).toString().padStart(3)
            emit("$n  ${app.label}  ·  ${app.packageName}")
        }
    }

    private fun open(arg: String) {
        if (arg.isEmpty()) {
            emit("usage: open <name>", Line.Kind.ERROR)
            return
        }
        val apps = runCatching { inventory.load() }.getOrDefault(emptyList())
        val needle = arg.lowercase()
        val hit = apps.firstOrNull { it.label.lowercase() == needle }
            ?: apps.firstOrNull { it.label.lowercase().contains(needle) }
            ?: apps.firstOrNull { it.packageName.lowercase().contains(needle) }
        if (hit == null) {
            emit("no app matches \"$arg\" — run `apps` to list", Line.Kind.ERROR)
        } else {
            emit("launching ${hit.label} …", Line.Kind.OK)
            launchApp(hit.packageName)
        }
    }

    private fun wall(arg: String) {
        if (arg.isEmpty()) {
            emit("usage: wall <color|#rrggbb> — image paths are not implemented in v1", Line.Kind.ERROR)
            return
        }
        if (arg.startsWith("/") || arg.startsWith("content://")) {
            emit("wall: wallpaper from image path is not implemented in v1", Line.Kind.ERROR)
            return
        }
        if (!inventory.wallpaperSupported()) {
            emit("wall: not supported on this device (WallpaperManager unavailable)", Line.Kind.ERROR)
            return
        }
        val color = parseColorArg(arg)
        if (color == null) {
            emit("wall: unrecognized color \"$arg\" — try #c8ff3d, lime, red, black …", Line.Kind.ERROR)
            return
        }
        val err = inventory.setWallpaperColor(color)
        if (err == null) {
            emit("wallpaper set → #${Integer.toHexString(color).padStart(6, '0')}", Line.Kind.OK)
        } else {
            emit("wallpaper failed: $err", Line.Kind.ERROR)
        }
    }

    private fun parseColorArg(arg: String): Int? {
        val s = arg.trim().lowercase()
        if (s.startsWith("#")) {
            return runCatching { android.graphics.Color.parseColor(s) }.getOrNull()
        }
        return when (s) {
            "black" -> 0xFF050505.toInt()
            "red" -> 0xFFFF4444.toInt()
            "green" -> 0xFF7CFF9B.toInt()
            "lime" -> 0xFFC8FF3D.toInt()
            "amber" -> 0xFFFFC24B.toInt()
            "blue" -> 0xFF4D9FFF.toInt()
            "white" -> 0xFFFFFFFF.toInt()
            "gray", "grey" -> 0xFF808080.toInt()
            else -> null
        }
    }

    private fun brand() {
        emit("brand spec:")
        emit("  row 1  : <DAY> <DD> <MON> (live date)  ·  [ --force ./BUILD YOU ] right-aligned")
        emit("  row 2  : BUILD YOU — wordmark, full brightness, auto-shrink 18–40sp")
        emit("  stamp  : monospace, ~0.30x wordmark size, 1dp border at every text size")
        emit("  inks   : --force = dimmest ink · ./BUILD YOU = one step brighter")
        emit("  deck   : 3–4 column icon grid, sorted by label")
        emit("  pane   : this terminal — real commands, no fake output")
    }

    private fun icon() {
        emit("icon ladder (first stamp that fits the available width wins):")
        emit("  1. \"--force\"   full stamp, widest slots")
        emit("  2. \"-f\"        narrow slots")
        emit("  3. \"·\"         very narrow slots")
        emit("  4. (none)      stamp hidden entirely")
        emit("  the chosen rung is written to logcat: stamp ladder → <rung>")
    }

    private fun force() {
        emit("reloading inventory …")
        reloadInventory()
        emit("reload: inventory reloaded", Line.Kind.OK)
    }

    private fun hidden() {
        val hidden = hiddenPackages()
        if (hidden.isEmpty()) {
            emit("no hidden packages", Line.Kind.WARN)
        } else {
            emit("${hidden.size} hidden package(s):")
            hidden.sorted().forEach { emit("  $it") }
            emit("unhide: `unhide <pkg>`", Line.Kind.WARN)
        }
    }

    private fun unhide(arg: String) {
        if (arg.isEmpty()) {
            emit("usage: unhide <package>", Line.Kind.ERROR)
            return
        }
        if (unhidePackage(arg.trim())) {
            emit("unhidden: ${arg.trim()}", Line.Kind.OK)
        } else {
            emit("not hidden (nothing to do): ${arg.trim()}", Line.Kind.WARN)
        }
    }

    private fun date() {
        // Locale.ENGLISH, not ROOT — ROOT renders "MMM" as "M11" on Android.
        emit(LocalDate.now().format(DateTimeFormatter.ofPattern("EEE d MMM uuuu", java.util.Locale.ENGLISH)))
    }

    private fun clear() {
        buffer.clear()
        output = emptyList()
    }

    private companion object {
        const val TAG = "BuildYouTerminal"
    }
}
