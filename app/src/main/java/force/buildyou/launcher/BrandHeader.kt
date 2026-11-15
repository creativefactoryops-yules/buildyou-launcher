package force.buildyou.launcher

import android.util.Log
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Brand ink ladder (dimmest → brightest) for the dark-only deck.
private val InkDim = Color(0xFF6E6E68)      // "--force" — dimmest ink
private val InkBright = Color(0xFFB9B9B4)   // "./BUILD YOU" — one step brighter
private val InkMid = Color(0xFF9A9A93)      // date, brackets
private val InkFull = Color(0xFFF2F2F0)     // wordmark — full brightness
private val StampBorder = Color(0xFF8A8A82)

private const val TAG = "BuildYou"
private const val WORDMARK = "BUILD YOU"

/**
 * Header stack, top-down (locked brand spec):
 *  row 1 — "SUN 15 NOV" (live, from LocalDate) left · "[ --force ./BUILD YOU ]" right,
 *          both on the SAME baseline;
 *  row 2 — "BUILD YOU" wordmark alone on its own line, full brightness,
 *          auto-shrinking 18–40sp (measured with TextMeasurer) so it never overflows.
 *
 * The stamp is monospace at ~0.30x the wordmark size inside a 1dp border that never
 * scales with the type, and its text follows the icon ladder when width is tight.
 */
@Composable
fun BrandHeader(maxWidth: Dp, modifier: Modifier = Modifier) {
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

    // Live date: day-of-week, day-of-month, month — e.g. "SUN 15 NOV".
    val dateText = remember {
        LocalDate.now()
            .format(DateTimeFormatter.ofPattern("EEE dd MMM", Locale.ROOT))
            .uppercase(Locale.ROOT)
    }

    // --- Wordmark: largest sp in [18, 40] whose measured width fits the available width. ---
    fun wordmarkStyle(sp: Int) = TextStyle(
        fontSize = sp.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.5).sp,
    )

    val wordmarkSp = remember(maxWidth, density.density, density.fontScale) {
        val widthPx = with(density) { maxWidth.toPx() }
        var chosen = 18 // guaranteed floor — never overflow
        for (sp in 40 downTo 18) {
            val measured = textMeasurer.measure(
                AnnotatedString(WORDMARK),
                wordmarkStyle(sp),
            )
            if (measured.size.width <= widthPx) {
                chosen = sp
                break
            }
        }
        chosen
    }

    // --- Stamp size: monospace at ~0.30x the wordmark (floored for legibility). ---
    val stampSp = (wordmarkSp * 0.30f).coerceAtLeast(7f)

    // --- Icon ladder: first stamp string that fits the width not taken by the date wins.
    //     The chosen rung is logged. Rung 4 is "nothing" — the stamp disappears. ---
    val stampCandidates = listOf("[ --force ./BUILD YOU ]", "[ -f ]", "[ · ]")
    val stampText = remember(maxWidth, density.density, density.fontScale, wordmarkSp) {
        val widthPx = with(density) { maxWidth.toPx() }
        val dateWidth = textMeasurer.measure(
            AnnotatedString(dateText),
            TextStyle(fontSize = stampSp.sp, fontFamily = FontFamily.Monospace),
        ).size.width
        val available = widthPx - dateWidth - with(density) { 16.dp.toPx() }
        val stampStyle = TextStyle(fontSize = stampSp.sp, fontFamily = FontFamily.Monospace)
        val rung = stampCandidates.firstOrNull { candidate ->
            textMeasurer.measure(AnnotatedString(candidate), stampStyle).size.width <= available
        }
        val chosen = rung ?: "" // ladder rung 4: no stamp at all
        Log.d(TAG, "stamp ladder → ${if (chosen.isEmpty()) "(none)" else chosen}")
        chosen
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Row 1 — date left, stamp right, shared baseline.
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = dateText,
                color = InkMid,
                fontSize = stampSp.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.alignByBaseline(),
            )
            Spacer(Modifier.weight(1f))
            StampBox(
                text = stampText,
                stampSp = stampSp,
                // Reuse the ladder string for the ink split only when the full stamp won.
                full = stampText == stampCandidates[0],
                modifier = Modifier.alignByBaseline(),
            )
        }
        // Row 2 — wordmark alone on its own line, full brightness.
        Text(
            text = WORDMARK,
            color = InkFull,
            style = wordmarkStyle(wordmarkSp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
        )
    }
}

/**
 * The bracketed stamp as a bordered Text — a Text exposes a REAL baseline, so
 * Modifier.alignByBaseline() in the row above puts the stamp's type on the same
 * baseline as the date (a Box has no baseline, which silently top-aligned it).
 *
 * With the full ladder string, "--force" renders in the dimmest ink and
 * "./BUILD YOU" one step brighter (brackets mid ink). The border is a fixed 1dp
 * at every text size — a layout dp that never scales with the type.
 */
@Composable
private fun StampBox(text: String, stampSp: Float, full: Boolean, modifier: Modifier = Modifier) {
    if (text.isEmpty()) return // ladder rung 4: nothing
    val ink: AnnotatedString = if (full) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = InkMid)) { append("[ ") }
            withStyle(SpanStyle(color = InkDim)) { append("--force") }
            append(" ")
            withStyle(SpanStyle(color = InkBright)) { append("./BUILD YOU") }
            withStyle(SpanStyle(color = InkMid)) { append(" ]") }
        }
    } else {
        buildAnnotatedString {
            withStyle(SpanStyle(color = InkDim)) { append(text.removeSurrounding("[ ", " ]")) }
        }
    }
    Text(
        text = ink,
        fontSize = stampSp.sp,
        fontFamily = FontFamily.Monospace,
        modifier = modifier
            // Border drawn at a fixed 1dp around the text; padding offsets the
            // baseline uniformly so date and stamp type stay baseline-aligned.
            .border(width = 1.dp, color = StampBorder)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
