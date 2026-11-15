package force.buildyou.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import force.buildyou.launcher.TerminalEngine.Line

private val TerminalBg = Color(0xFF080807)
private val PromptInk = Color(0xFFC8FF3D)
private val OutInk = Color(0xFFB9B9B4)
private val OkInk = Color(0xFF7CFF9B)
private val WarnInk = Color(0xFFFFC24B)
private val ErrorInk = Color(0xFFFF6B5E)
private val InOutDim = Color(0xFF6E6E68)
private val CursorInk = Color(0xFFF2F2F0)

/**
 * Terminal pane: real command line at the bottom of the deck, bound to the
 * TerminalEngine. Autofocus is off (it is a home screen, not a REPL) — tap to focus.
 */
@Composable
fun TerminalPane(
    engine: TerminalEngine,
    modifier: Modifier = Modifier,
) {
    val listState = rememberScrollState()
    var input by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    // Keep the newest line visible.
    LaunchedEffect(engine.output.size) {
        if (engine.output.isNotEmpty()) listState.animateScrollTo(listState.maxValue)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(TerminalBg)
            // Edge-to-edge: keep the input row clear of the gesture/3-button nav bar.
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // TERM row: label + COPY ALL — the owner could not copy help output in v1
        // (Compose Text is not selectable by default), so the whole buffer is one
        // tap onto the clipboard AND long-press selection works line by line.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 6.dp),
        ) {
            Text(
                text = "TERM",
                color = InOutDim,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
            )
            Box(Modifier.weight(1f))
            Text(
                text = "COPY ALL",
                color = PromptInk,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .clickable {
                        val all = engine.output.joinToString("\n") { it.text }
                        val cm = ctx.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                            as android.content.ClipboardManager
                        cm.setPrimaryClip(android.content.ClipData.newPlainText("BUILD YOU", all))
                    }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            )
        }
        SelectionContainer(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .height(180.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(listState)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
            ) {
                engine.output.forEach { line ->
                    Text(
                        text = line.toAnnotated(),
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = "\$",
                color = PromptInk,
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
            )
            Box(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(24.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (input.isEmpty() && !focused) {
                    Text(
                        text = "help · apps · open · reload · find apps in the search bar",
                        color = InOutDim,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    textStyle = TextStyle(
                        color = CursorInk,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            engine.execute(input)
                            input = ""
                        },
                    ),
                    // Real focus tracking: the hint hides only while the field is
                    // actually focused, so it never sits under the user's cursor.
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focused = it.isFocused },
                )
            }
        }
    }
}

private fun Line.toAnnotated(): AnnotatedString = buildAnnotatedString {
    when (kind) {
        Line.Kind.INPUT -> withStyle(SpanStyle(color = PromptInk)) { append(text) }
        Line.Kind.OK -> withStyle(SpanStyle(color = OkInk)) { append(text) }
        Line.Kind.WARN -> withStyle(SpanStyle(color = WarnInk)) { append(text) }
        Line.Kind.ERROR -> withStyle(SpanStyle(color = ErrorInk)) { append(text) }
        Line.Kind.OUTPUT -> withStyle(SpanStyle(color = OutInk)) { append(text) }
    }
}
