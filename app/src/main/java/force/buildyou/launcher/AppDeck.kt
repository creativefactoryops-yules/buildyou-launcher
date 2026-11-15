package force.buildyou.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DeckBackground = Color(0xFF050505)
private val LabelInk = Color(0xFFB9B9B4)
private val InkFull = Color(0xFFF2F2F0)
private val InkDim = Color(0xFF6E6E68)
private val SheetSurface = Color(0xFF0C0C0B)
private val AccentGreen = Color(0xFF7CFF9B)
private val AccentAmber = Color(0xFFFFC24B)
private val AccentRed = Color(0xFFFF6B5E)

/** Deck column choice by usable width: 3 columns narrow, 4 columns wide (~388dp break). */
private fun columnsFor(usableWidthDp: Float): Int = if (usableWidthDp >= 388f) 4 else 3

/**
 * The app deck: a stable grid of the real installed apps in persisted order.
 * Columns adapt to available width (3 narrow / 4 wide, ~388dp usable break).
 *
 * Drag-to-reorder is a NICE-TO-HAVE in the spec; v1 ships the stable grid with a
 * persisted order (alphabetical first run, then whatever order prefs record).
 * TODO(v2): drag-to-reorder gesture writing through to LauncherPrefs.saveOrder.
 *
 * Explicitly OUT OF SCOPE for v1 per spec — do not stub silently:
 * TODO(v2): home-screen widgets (AppWidgetHost).
 * TODO(v2): notification badges (NotificationListener).
 * TODO(v2): the snake game.
 * TODO(v2): the creature mascot.
 */
@Composable
fun AppDeck(
    apps: List<AppEntry>,
    usableWidthDp: Float,
    onLaunch: (AppEntry) -> Unit,
    onAppInfo: (AppEntry) -> Unit,
    onUninstall: (AppEntry) -> Unit,
    onHide: (AppEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    var sheetApp by remember { mutableStateOf<AppEntry?>(null) }

    Box(modifier = modifier.fillMaxSize().background(DeckBackground)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnsFor(usableWidthDp)),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(apps, key = { it.packageName }) { app ->
                AppIcon(
                    app = app,
                    onClick = { onLaunch(app) },
                    onLongClick = { sheetApp = app },
                )
            }
        }

        sheetApp?.let { app ->
            AppSheet(
                app = app,
                onDismiss = { sheetApp = null },
                onAppInfo = { onAppInfo(app); sheetApp = null },
                onUninstall = { onUninstall(app); sheetApp = null },
                onHide = { onHide(app); sheetApp = null },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppIcon(
    app: AppEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val bitmap = app.imageBitmap
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF141414)),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = app.label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(6.dp),
                )
            } else {
                // Honest fallback when PackageManager returned no icon.
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2A2A26)),
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = app.label,
            color = LabelInk,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontFamily = FontFamily.Monospace,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

/**
 * Long-press bottom sheet: App info / Uninstall / Hide — each bound to a real
 * intent or persisted state via the callbacks MainActivity supplies.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppSheet(
    app: AppEntry,
    onDismiss: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    onHide: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SheetSurface,
        contentColor = InkFull,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            val bitmap = app.imageBitmap
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                )
                Spacer(Modifier.width(12.dp))
            }
            Column {
                Text(
                    text = app.label,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    color = InkFull,
                )
                Text(
                    text = app.packageName,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = InkDim,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        SheetAction(label = "App info", ink = AccentGreen) { onAppInfo() }
        SheetAction(label = "Uninstall", ink = AccentAmber) { onUninstall() }
        SheetAction(label = "Hide from deck", ink = AccentRed) { onHide() }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SheetAction(label: String, ink: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(ink),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            color = InkFull,
        )
    }
}
