package force.buildyou.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/** A launchable app resolved from PackageManager. */
data class AppEntry(
    val label: String,
    val packageName: String,
    val icon: Drawable?,
) {
    /**
     * Compose bitmap, converted lazily and cached: BitmapDrawables hand over their
     * bitmap directly; adaptive/vector drawables are rasterized into one.
     */
    val imageBitmap: ImageBitmap? by lazy {
        icon?.let { drawable -> drawable.toImageBitmap() }
    }
}

private fun Drawable.toImageBitmap(): ImageBitmap {
    if (this is BitmapDrawable && bitmap != null) return bitmap.asImageBitmap()
    val w = intrinsicWidth.coerceAtLeast(1)
    val h = intrinsicHeight.coerceAtLeast(1)
    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bmp.asImageBitmap()
}

/**
 * Real app inventory from PackageManager: queryIntentActivities for MAIN/LAUNCHER,
 * excluding this launcher's own package, sorted by label.
 */
class AppInventory(context: Context) {

    private val appContext = context.applicationContext
    private val pm: PackageManager = appContext.packageManager
    private val selfPackage: String = appContext.packageName

    fun load(): List<AppEntry> {
        val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val infos = pm.queryIntentActivities(mainIntent, 0)
        val seen = HashSet<String>()
        val entries = ArrayList<AppEntry>(infos.size)
        for (info in infos) {
            val ri = info.activityInfo ?: continue
            val pkg = ri.packageName ?: continue
            if (pkg == selfPackage) continue // never list ourselves on the deck
            if (!seen.add(pkg)) continue // one entry per package
            val label = runCatching { pm.getApplicationLabel(ri.applicationInfo).toString() }
                .getOrElse { runCatching { ri.loadLabel(pm).toString() }.getOrDefault(pkg) }
            entries.add(
                AppEntry(
                    label = label.ifBlank { pkg },
                    packageName = pkg,
                    icon = runCatching { ri.loadIcon(pm) }.getOrNull(),
                ),
            )
        }
        return entries.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }

    /** Resolve the real launch intent for a package (null when it cannot be launched). */
    fun launchIntentFor(pkg: String): Intent? = pm.getLaunchIntentForPackage(pkg)

    /** Open the system App Info page for a package. */
    fun appDetailsIntent(pkg: String): Intent =
        Intent(
            android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            android.net.Uri.parse("package:$pkg"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Uninstall request intent; the system shows its own confirmation sheet. */
    fun uninstallIntent(pkg: String): Intent =
        Intent(Intent.ACTION_UNINSTALL_PACKAGE, android.net.Uri.parse("package:$pkg"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** WallpaperManager reachable on this device. */
    fun wallpaperSupported(): Boolean = runCatching {
        android.app.WallpaperManager.getInstance(appContext) != null
    }.getOrDefault(false)

    /**
     * Apply a solid colour wallpaper via WallpaperManager.setBitmap.
     * Returns null on success, or an error message.
     */
    fun setWallpaperColor(color: Int): String? = runCatching {
        val wm = android.app.WallpaperManager.getInstance(appContext)
        val bmp = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(color)
        wm.setBitmap(bmp)
        null
        // TODO(v2): `wall <path>` — decode an image file with BitmapFactory and pass it to
        // the same setBitmap call; v1 supports solid colours only and says so honestly.
    }.getOrElse { e -> e.message ?: e.javaClass.simpleName }
}
