package dev.malangkey.app.game

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import dev.malangkey.R
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.app.LocalNavController
import dev.malangkey.app.apptheme.JuaFontFamily
import dev.malangkey.app.apptheme.MalangButton
import dev.malangkey.app.apptheme.MalangCardSubLight
import dev.malangkey.app.apptheme.MalangCardSubMuted
import dev.malangkey.app.apptheme.MalangCardText
import dev.malangkey.app.apptheme.MalangCocoa
import dev.malangkey.app.apptheme.MalangSettingsBorder
import dev.malangkey.app.apptheme.MalangSettingsCard
import dev.malangkey.app.apptheme.MalangSettingsScreen
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSettingsSummary
import dev.malangkey.app.apptheme.MalangSettingsTitle
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random

private val WrongColor = Color(0xFFD9534F)
private val GoodColor = Color(0xFF4E9A5B)
private val GhostColor = Color(0xFFB9A595)

/** 한 판의 결과: 점수와 줄별 기록. */
private class AnthemRun(val result: TypingScore.Result, val lines: List<LineRecord>)

/** 애국가 빨리치기: 한 줄씩 따라 치고, 끝나면 점수 카드를 보여준다. 최고 기록은 고스트가 되어 다음 판과 겨룬다. */
@Composable
fun AnthemGameScreen(full: Boolean) {
    val prefs by FlorisPreferenceStore
    val targets = remember(full) { AnthemLyrics.lines(full) }
    val bestPref = if (full) prefs.malang.anthemBestScoreFull else prefs.malang.anthemBestScore
    val ghostPref = if (full) prefs.malang.anthemGhostFull else prefs.malang.anthemGhost
    val scope = rememberCoroutineScope()
    var round by remember { mutableIntStateOf(0) }
    var run by remember { mutableStateOf<AnthemRun?>(null) }
    // 판을 시작할 때의 최고 기록. 결과 화면에서 이번 판과 비교한다.
    var previousBest by remember { mutableIntStateOf(bestPref.get()) }
    val modeLabel = if (full) "전체 (1~4절)" else "1절"

    MalangSettingsScreen(
        title = "애국가 빨리치기",
        subtitle = modeLabel,
        showTestInput = false,
    ) {
        val finished = run
        if (finished == null) {
            // round가 바뀌면 판을 처음부터 다시 만든다.
            androidx.compose.runtime.key(round) {
                val ghost = remember { AnthemGhost.decode(ghostPref.get()) }
                AnthemPlay(targets, ghost) { result, lines ->
                    previousBest = bestPref.get()
                    if (result.score > previousBest) {
                        scope.launch {
                            bestPref.set(result.score)
                            ghostPref.set(AnthemGhost.encode(lines.map { it.endMillis }))
                        }
                    }
                    run = AnthemRun(result, lines)
                }
            }
        } else {
            AnthemResult(finished, targets, modeLabel, previousBest, onRetry = { run = null; round++ })
        }
    }
}

@Composable
private fun AnthemPlay(
    targets: List<String>,
    ghost: List<Long>,
    onFinished: (TypingScore.Result, List<LineRecord>) -> Unit,
) {
    val typedLines = remember { mutableStateListOf<String>() }
    val lineRecords = remember { mutableStateListOf<LineRecord>() }
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var elapsed by remember { mutableLongStateOf(0L) }
    // 틀리지 않고 이어서 맞힌 글자 수.
    var combo by remember { mutableIntStateOf(0) }
    var bestCombo by remember { mutableIntStateOf(0) }
    // 방금 끝낸 줄에 대한 한마디(완벽!, 고스트와의 차이). 바뀔 때마다 잠깐 떴다 사라진다.
    var toast by remember { mutableStateOf<LineToast?>(null) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val lineIndex = typedLines.size
    val target = targets.getOrNull(lineIndex) ?: ""

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboard?.show()
    }
    LaunchedEffect(startedAt) {
        while (startedAt > 0L) {
            elapsed = System.currentTimeMillis() - startedAt
            delay(50)
        }
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(1400)
            toast = null
        }
    }

    fun submitLine() {
        if (startedAt == 0L) return
        val now = System.currentTimeMillis() - startedAt
        val errors = TypingScore.errors(input.text, target)
        typedLines.add(input.text)
        lineRecords.add(LineRecord(now, errors))
        val delta = AnthemGhost.deltaAt(ghost, lineRecords.size - 1, now)
        toast = LineToast(perfect = errors == 0, ghostDeltaMillis = delta, id = lineRecords.size)
        input = TextFieldValue("")
        if (typedLines.size >= targets.size) {
            onFinished(TypingScore.result(typedLines, targets, now), lineRecords.toList())
        }
    }

    // 엔터는 줄의 마지막 글자까지 쳤을 때만 받는다. 중간에 잘못 눌러 줄이 넘어가지 않게 한다.
    // 마지막 글자가 아직 조합 중이어도 글자 수는 찼으므로 넘어간다.
    fun onEnter(text: String = input.text) {
        if (text.length >= target.length) submitLine()
    }

    // 확정된 글자(조합 중인 마지막 글자는 빼고)가 하나 늘 때마다 맞았는지 보고 콤보와 흔들림을 정한다.
    fun onCommittedGrew(old: TextFieldValue, new: TextFieldValue) {
        val oldDone = old.text.length - if (old.composition != null) 1 else 0
        val newDone = new.text.length - if (new.composition != null) 1 else 0
        for (i in oldDone.coerceAtLeast(0) until newDone) {
            if (new.text.getOrNull(i) == target.getOrNull(i)) {
                combo++
                if (combo > bestCombo) bestCombo = combo
            } else {
                combo = 0
                scope.launch {
                    shake.snapTo(0f)
                    shake.animateTo(0f, keyframes {
                        durationMillis = 280
                        -10f at 40
                        9f at 90
                        -6f at 150
                        4f at 210
                    })
                }
            }
        }
    }

    val live = if (startedAt > 0L && elapsed > 0L) {
        TypingScore.result(typedLines + input.text, targets.take(lineIndex + 1), elapsed)
    } else {
        null
    }
    val myProgress = lineIndex + (input.text.length.toFloat() / target.length.coerceAtLeast(1)).coerceAtMost(1f)
    val ghostProgress = if (startedAt > 0L) AnthemGhost.progressAt(ghost, targets.size, elapsed) else if (ghost.isNotEmpty()) 0f else null

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatChip("시간", formatTime(elapsed), Modifier.weight(1f))
        StatChip("타수", live?.strokesPerMinute ?: 0, Modifier.weight(1f))
        StatChip("정확도", ((live?.accuracy ?: 1f) * 100).toInt(), Modifier.weight(1f), suffix = "%")
    }

    RaceBar(
        progress = myProgress / targets.size,
        ghostProgress = ghostProgress?.let { it / targets.size },
    )

    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(shake.value.dp.roundToPx(), 0) }
                .clip(RoundedCornerShape(24.dp))
                .background(MalangSettingsCard)
                .border(1.dp, MalangSettingsBorder, RoundedCornerShape(24.dp))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${lineIndex + 1} / ${targets.size} 줄", color = MalangSettingsSummary, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                ComboBadge(combo)
            }
            AnimatedContent(
                targetState = lineIndex,
                transitionSpec = {
                    (slideInVertically(tween(260)) { it / 2 } + fadeIn(tween(260))) togetherWith
                        (slideOutVertically(tween(220)) { -it / 2 } + fadeOut(tween(180)))
                },
                label = "line",
            ) { index ->
                val shownTarget = targets.getOrNull(index) ?: ""
                LyricLine(shownTarget, if (index == lineIndex) input.text else "")
            }
            targets.getOrNull(lineIndex + 1)?.let { next ->
                Text("다음  $next", color = MalangSettingsSummary.copy(alpha = 0.7f), fontSize = 14.sp)
            }
        }
        // 줄을 끝낼 때마다 카드 위에 잠깐 뜨는 한마디.
        AnimatedVisibility(
            visible = toast != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-14).dp),
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = fadeOut(tween(300)),
        ) {
            toast?.let { LineToastBubble(it) }
        }
    }

    BasicTextField(
        value = input,
        onValueChange = { value ->
            // 엔터는 줄바꿈 글자로 들어오기도 한다. 그러면 줄바꿈을 지우고 다음 줄로 넘긴다.
            if (value.text.contains('\n')) {
                val text = value.text.replace("\n", "")
                input = TextFieldValue(text, TextRange(text.length))
                onEnter(text)
                return@BasicTextField
            }
            if (startedAt == 0L && value.text.isNotEmpty()) startedAt = System.currentTimeMillis()
            val old = input
            input = value
            onCommittedGrew(old, value)
            // 줄을 정확히 다 치면 엔터 없이 다음 줄로 넘어간다.
            if (value.text == target && value.composition == null) submitLine()
        },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            // 말랑키는 이런 입력칸에서 엔터를 '다음' 동작이 아니라 엔터 키로 보내므로 키를 직접 받는다.
            .onPreviewKeyEvent { event ->
                val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter
                if (isEnter && event.type == KeyEventType.KeyUp) onEnter()
                isEnter
            }
            .clip(RoundedCornerShape(18.dp))
            .background(MalangSettingsCard)
            .border(2.dp, MalangSettingsSection, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        textStyle = TextStyle(color = MalangSettingsTitle, fontSize = 20.sp),
        cursorBrush = SolidColor(MalangSettingsSection),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        keyboardActions = KeyboardActions(onNext = { onEnter() }, onDone = { onEnter() }),
    )
    if (startedAt == 0L) {
        val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Reverse),
            label = "pulse",
        )
        Text(
            if (ghost.isNotEmpty()) "첫 글자를 치면 시작! 내 최고 기록 고스트와 겨뤄요." else "첫 글자를 치면 시작! 한 줄을 다 치면 엔터.",
            modifier = Modifier.graphicsLayer { alpha = pulse },
            color = MalangSettingsSection,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    } else {
        Text(
            "줄 끝까지 치고 엔터를 누르면 틀려도 다음 줄로 넘어가요. 오타는 점수에서 깎여요.",
            color = MalangSettingsSummary,
            fontSize = 13.sp,
            lineHeight = 19.sp,
        )
    }
}

private class LineToast(val perfect: Boolean, val ghostDeltaMillis: Long?, val id: Int)

@Composable
private fun LineToastBubble(toast: LineToast) {
    val delta = toast.ghostDeltaMillis
    val text = buildString {
        if (toast.perfect) append("완벽!")
        if (delta != null) {
            if (isNotEmpty()) append("  ")
            val sec = "%.1f".format(abs(delta) / 1000f)
            append(if (delta <= 0) "고스트보다 ${sec}초 빠름" else "고스트보다 ${sec}초 느림")
        }
    }
    if (text.isEmpty()) return
    val color = when {
        delta != null && delta > 0 -> MalangSettingsSummary
        else -> GoodColor
    }
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        color = Color.White,
        fontSize = 14.sp,
        fontFamily = JuaFontFamily,
    )
}

/** 따라 칠 가사 한 줄. 맞은 글자는 진하게, 틀린 글자는 빨갛게, 다음에 칠 글자는 밑줄로 깜박인다. */
@Composable
private fun LyricLine(target: String, typed: String) {
    val blink by rememberInfiniteTransition(label = "caret").animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(520), RepeatMode.Reverse),
        label = "caret",
    )
    Text(
        buildAnnotatedString {
            target.forEachIndexed { i, c ->
                val t = typed.getOrNull(i)
                val style = when {
                    t == null && i == typed.length -> SpanStyle(
                        color = MalangSettingsSection.copy(alpha = blink),
                        textDecoration = TextDecoration.Underline,
                    )
                    t == null -> SpanStyle(color = MalangSettingsSummary.copy(alpha = 0.5f))
                    t == c -> SpanStyle(color = MalangSettingsTitle)
                    else -> SpanStyle(color = WrongColor, background = WrongColor.copy(alpha = 0.12f))
                }
                withStyle(style) { append(c) }
            }
        },
        fontSize = 26.sp,
        lineHeight = 36.sp,
        fontFamily = JuaFontFamily,
    )
}

@Composable
private fun ComboBadge(combo: Int) {
    AnimatedVisibility(
        visible = combo >= 10,
        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
        exit = scaleOut() + fadeOut(),
    ) {
        // 콤보가 오를 때마다 살짝 튄다.
        val bump = remember { Animatable(1f) }
        LaunchedEffect(combo) {
            bump.snapTo(1.25f)
            bump.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
        }
        Text(
            "콤보 $combo",
            modifier = Modifier
                .scale(bump.value)
                .clip(RoundedCornerShape(10.dp))
                .background(MalangSettingsSection)
                .padding(horizontal = 10.dp, vertical = 3.dp),
            color = MalangCardText,
            fontSize = 13.sp,
            fontFamily = JuaFontFamily,
        )
    }
}

/** 진행 막대. 내 위치와, 최고 기록(고스트)이 같은 시간에 있던 위치를 함께 보여준다. */
@Composable
private fun RaceBar(progress: Float, ghostProgress: Float?) {
    val mine by animateFloatAsState(progress.coerceIn(0f, 1f), tween(200), label = "mine")
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(22.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        val width = maxWidth
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(MalangSettingsBorder),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(mine)
                    .clip(CircleShape)
                    .background(MalangSettingsSection),
            )
        }
        if (ghostProgress != null) {
            val g = ghostProgress.coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .offset(x = width * g - 7.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(GhostColor)
                    .border(2.dp, MalangSettingsCard, CircleShape),
            )
        }
        // 내 위치 표시 (고스트 위에 그려 겹쳐도 내 쪽이 보이게).
        Box(
            modifier = Modifier
                .offset(x = width * mine - 9.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(MalangSettingsSection)
                .border(3.dp, MalangSettingsCard, CircleShape),
        )
    }
    if (ghostProgress != null) {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendDot(MalangSettingsSection, "나")
            LegendDot(GhostColor, "최고 기록")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(5.dp))
        Text(label, color = MalangSettingsSummary, fontSize = 12.sp)
    }
}

@Composable
private fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MalangSettingsCard)
            .border(1.dp, MalangSettingsBorder, RoundedCornerShape(16.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = MalangSettingsSummary, fontSize = 12.sp)
        Text(value, color = MalangSettingsTitle, fontSize = 20.sp, fontFamily = JuaFontFamily)
    }
}

/** 숫자가 바뀔 때 부드럽게 따라 올라가는 칸. */
@Composable
private fun StatChip(label: String, value: Int, modifier: Modifier = Modifier, suffix: String = "") {
    val shown by animateIntAsState(value, tween(250), label = label)
    StatChip(label, "$shown$suffix", modifier)
}

@Composable
private fun AnthemResult(
    run: AnthemRun,
    targets: List<String>,
    modeLabel: String,
    previousBest: Int,
    onRetry: () -> Unit,
) {
    val result = run.result
    val context = LocalContext.current
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    val isNewBest = result.score > previousBest
    val playedAt = remember { SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(Date()) }
    val cardLayer = rememberGraphicsLayer()

    // 점수가 0부터 올라가고, 다 오르면 등급 도장이 찍힌다.
    val countUp = remember { Animatable(0f) }
    val stamp = remember { Animatable(0f) }
    LaunchedEffect(result) {
        countUp.animateTo(1f, tween(1100, easing = FastOutSlowInEasing))
        stamp.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMediumLow))
    }

    Box {
        // 사진으로 저장할 카드. 그리는 내용을 그대로 이미지로 뽑을 수 있게 레이어에 기록한다.
        Box(
            modifier = Modifier.drawWithContent {
                cardLayer.record { this@drawWithContent.drawContent() }
                drawLayer(cardLayer)
            }
        ) {
            ResultCard(
                result = result,
                shownScore = (result.score * countUp.value).roundToInt(),
                stampProgress = stamp.value,
                modeLabel = modeLabel,
                playedAt = playedAt,
                isNewBest = isNewBest,
                previousBest = previousBest,
            )
        }
        if (isNewBest && stamp.value > 0.05f) Confetti(Modifier.matchParentSize())
    }

    LineBreakdown(run.lines, targets)

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MalangButton("다시 하기", modifier = Modifier.weight(1f), primary = false, onClick = onRetry)
        MalangButton("사진으로 저장", modifier = Modifier.weight(1.4f)) {
            scope.launch {
                val bitmap = cardLayer.toImageBitmap().asAndroidBitmap()
                saveResultImage(context, bitmap)
            }
        }
    }
    MalangButton("나가기", modifier = Modifier.fillMaxWidth(), primary = false) { navController.popBackStack() }
}

@Composable
private fun ResultCard(
    result: TypingScore.Result,
    shownScore: Int,
    stampProgress: Float,
    modeLabel: String,
    playedAt: String,
    isNewBest: Boolean,
    previousBest: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(MalangCocoa)
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 로고가 어두운 갈색이라 밝은 원 위에 올려 카드 배경과 구분되게 한다.
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MalangCardText),
                contentAlignment = Alignment.Center,
            ) {
                Image(painterResource(R.drawable.ic_malang_logo), contentDescription = null, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.size(8.dp))
            Text("애국가 빨리치기 · $modeLabel", color = MalangCardSubLight, fontSize = 15.sp, fontFamily = JuaFontFamily)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("$shownScore", color = MalangCardText, fontSize = 72.sp, lineHeight = 76.sp, fontFamily = JuaFontFamily)
                Text("점", color = MalangCardSubLight, fontSize = 16.sp, fontFamily = JuaFontFamily)
            }
            // 등급 도장: 크게 떨어지며 찍히고 살짝 기운다.
            if (stampProgress > 0f) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 78.dp, y = (-6).dp)
                        .graphicsLayer {
                            val s = 2.4f - 1.4f * stampProgress
                            scaleX = s
                            scaleY = s
                            alpha = stampProgress.coerceIn(0f, 1f)
                        }
                        .rotate(-14f)
                        .size(56.dp)
                        .clip(CircleShape)
                        .border(3.dp, gradeColor(result.grade), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(result.grade, color = gradeColor(result.grade), fontSize = 30.sp, fontFamily = JuaFontFamily)
                }
            }
        }
        Text(
            result.gradeTitle,
            color = MalangCardText,
            fontSize = 20.sp,
            fontFamily = JuaFontFamily,
            textAlign = TextAlign.Center,
        )
        Text(
            when {
                isNewBest && previousBest > 0 -> "최고 기록!  +${result.score - previousBest}점"
                isNewBest -> "첫 기록!"
                else -> "내 최고 ${previousBest}점까지 ${previousBest - result.score}점"
            },
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (isNewBest) MalangCardText else MalangCardText.copy(alpha = 0.14f))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            color = if (isNewBest) MalangCocoa else MalangCardSubLight,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            ResultStat("타수", "${result.strokesPerMinute}타")
            ResultStat("정확도", "${(result.accuracy * 100).toInt()}%")
            ResultStat("오타", "${result.errorCount}개")
            ResultStat("시간", formatTime(result.elapsedMillis))
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text("$playedAt · 말랑키", color = MalangCardSubMuted, fontSize = 12.sp)
    }
}

private fun gradeColor(grade: String): Color = when (grade) {
    "S" -> Color(0xFFFFC94D)
    "A" -> Color(0xFFFF9A6B)
    "B" -> Color(0xFF9FD58F)
    "C" -> Color(0xFF8EC5E8)
    else -> Color(0xFFD9BF8C)
}

@Composable
private fun ResultStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = MalangCardText, fontSize = 18.sp, fontFamily = JuaFontFamily)
        Text(label, color = MalangCardSubMuted, fontSize = 12.sp)
    }
}

/** 줄별 걸린 시간과 오타. 가장 오래 걸린 줄을 짚어 준다. */
@Composable
private fun LineBreakdown(lines: List<LineRecord>, targets: List<String>) {
    if (lines.isEmpty()) return
    val durations = lines.mapIndexed { i, r -> r.endMillis - (lines.getOrNull(i - 1)?.endMillis ?: 0L) }
    val slowest = durations.indices.maxBy { durations[it] }
    val longest = durations.max().coerceAtLeast(1L)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MalangSettingsCard)
            .border(1.dp, MalangSettingsBorder, RoundedCornerShape(24.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("줄별 기록", color = MalangSettingsTitle, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        lines.forEachIndexed { i, r ->
            val isSlowest = i == slowest && lines.size > 1
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        targets.getOrNull(i) ?: "",
                        modifier = Modifier.weight(1f),
                        color = if (isSlowest) WrongColor else MalangSettingsTitle,
                        fontSize = 13.sp,
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "%.1f초".format(durations[i] / 1000f) + if (r.errors > 0) " · 오타 ${r.errors}" else "",
                        color = MalangSettingsSummary,
                        fontSize = 12.sp,
                    )
                }
                val fraction by animateFloatAsState(
                    durations[i].toFloat() / longest,
                    tween(600, delayMillis = 80 * i),
                    label = "bar$i",
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(MalangSettingsBorder),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .clip(CircleShape)
                            .background(if (isSlowest) WrongColor else MalangSettingsSection),
                    )
                }
            }
        }
        if (lines.size > 1) {
            Text(
                "가장 오래 걸린 줄을 연습하면 점수가 쑥 올라요.",
                color = MalangSettingsSummary,
                fontSize = 12.sp,
            )
        }
    }
}

/** 최고 기록일 때 카드 위로 터지는 꽃가루. */
@Composable
private fun Confetti(modifier: Modifier) {
    val colors = listOf(Color(0xFFFFC94D), Color(0xFFFF9A6B), Color(0xFF9FD58F), Color(0xFF8EC5E8), Color(0xFFFFF1CF))
    val pieces = remember {
        List(70) {
            ConfettiPiece(
                x = 0.5f + (Random.nextFloat() - 0.5f) * 0.3f,
                vx = (Random.nextFloat() - 0.5f) * 1.6f,
                vy = -(0.9f + Random.nextFloat() * 0.9f),
                spin = (Random.nextFloat() - 0.5f) * 720f,
                color = colors[it % colors.size],
                w = 6f + Random.nextFloat() * 6f,
            )
        }
    }
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(1800, easing = LinearEasing)) }
    val density = LocalDensity.current
    Canvas(modifier) {
        val time = t.value
        val px = with(density) { 1.dp.toPx() }
        for (p in pieces) {
            // 위로 솟았다가 중력으로 떨어진다. 끝으로 갈수록 흐려진다.
            val x = (p.x + p.vx * time * 0.5f) * size.width
            val y = size.height * 0.35f + (p.vy * time + 1.6f * time * time) * size.height * 0.6f
            val alpha = (1f - time).coerceIn(0f, 1f)
            rotate(p.spin * time, Offset(x, y)) {
                drawRect(
                    color = p.color.copy(alpha = alpha),
                    topLeft = Offset(x - p.w * px / 2, y - p.w * px / 4),
                    size = Size(p.w * px, p.w * px / 2),
                )
            }
        }
    }
}

private class ConfettiPiece(
    val x: Float,
    val vx: Float,
    val vy: Float,
    val spin: Float,
    val color: Color,
    val w: Float,
)

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

/**
 * 결과 카드를 사진으로 남긴다. 안드로이드 10 이상은 갤러리의 Pictures/Malangkey에 바로 저장하고,
 * 그보다 낮은 버전은 저장 권한 없이 쓸 수 있도록 공유 창을 띄운다(갤러리·메신저로 보낼 수 있음).
 */
private suspend fun saveResultImage(context: Context, bitmap: Bitmap) {
    val fileName = "malangkey_anthem_${System.currentTimeMillis()}.png"
    val saved = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Malangkey")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val resolver = context.contentResolver
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
                resolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                null
            } else {
                val dir = File(context.cacheDir, "game").apply { mkdirs() }
                val file = File(dir, fileName)
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                FileProvider.getUriForFile(context, "${context.packageName}.provider.file", file)
            }
        }
    }
    saved.onSuccess { shareUri ->
        if (shareUri == null) {
            Toast.makeText(context, "갤러리에 사진을 저장했어요.", Toast.LENGTH_SHORT).show()
        } else {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, shareUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "사진 저장·공유").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }.onFailure {
        Toast.makeText(context, "사진을 저장하지 못했어요.", Toast.LENGTH_SHORT).show()
    }
}
