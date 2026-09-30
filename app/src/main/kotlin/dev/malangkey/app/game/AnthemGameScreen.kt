package dev.malangkey.app.game

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
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

private val WrongColor = Color(0xFFD9534F)

/** 애국가 빨리치기: 한 줄씩 따라 치고, 끝나면 점수 카드를 보여준다. */
@Composable
fun AnthemGameScreen(full: Boolean) {
    val targets = remember(full) { AnthemLyrics.lines(full) }
    var round by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<TypingScore.Result?>(null) }
    val modeLabel = if (full) "전체 (1~4절)" else "1절"

    MalangSettingsScreen(
        title = "애국가 빨리치기",
        subtitle = modeLabel,
        showTestInput = false,
    ) {
        val finished = result
        if (finished == null) {
            // round가 바뀌면 판을 처음부터 다시 만든다.
            androidx.compose.runtime.key(round) {
                AnthemPlay(targets) { result = it }
            }
        } else {
            AnthemResult(finished, modeLabel, onRetry = { result = null; round++ })
        }
    }
}

@Composable
private fun AnthemPlay(targets: List<String>, onFinished: (TypingScore.Result) -> Unit) {
    val typedLines = remember { mutableStateListOf<String>() }
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var startedAt by remember { mutableLongStateOf(0L) }
    var elapsed by remember { mutableLongStateOf(0L) }
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
            delay(100)
        }
    }

    fun submitLine() {
        if (startedAt == 0L) return
        typedLines.add(input.text)
        input = TextFieldValue("")
        if (typedLines.size >= targets.size) {
            onFinished(TypingScore.result(typedLines, targets, System.currentTimeMillis() - startedAt))
        }
    }

    val live = if (startedAt > 0L && elapsed > 0L) {
        TypingScore.result(typedLines + input.text, targets.take(lineIndex + 1), elapsed)
    } else {
        null
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatChip("시간", formatTime(elapsed), Modifier.weight(1f))
        StatChip("타수", "${live?.strokesPerMinute ?: 0}", Modifier.weight(1f))
        StatChip("정확도", "${((live?.accuracy ?: 1f) * 100).toInt()}%", Modifier.weight(1f))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MalangSettingsCard)
            .border(1.dp, MalangSettingsBorder, RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("${lineIndex + 1} / ${targets.size} 줄", color = MalangSettingsSummary, fontSize = 13.sp)
        // 친 글자는 맞으면 진하게, 틀리면 빨갛게, 아직 안 친 글자는 흐리게 보여준다.
        Text(
            buildAnnotatedString {
                target.forEachIndexed { i, c ->
                    val typed = input.text.getOrNull(i)
                    val color = when {
                        typed == null -> MalangSettingsSummary.copy(alpha = 0.6f)
                        typed == c -> MalangSettingsTitle
                        else -> WrongColor
                    }
                    withStyle(SpanStyle(color = color)) { append(c) }
                }
            },
            fontSize = 24.sp,
            lineHeight = 34.sp,
            fontFamily = JuaFontFamily,
        )
        targets.getOrNull(lineIndex + 1)?.let { next ->
            Text("다음: $next", color = MalangSettingsSummary.copy(alpha = 0.7f), fontSize = 14.sp)
        }
    }

    BasicTextField(
        value = input,
        onValueChange = { value ->
            // 엔터는 줄바꿈 글자로 들어오기도 한다. 그러면 줄바꿈을 지우고 다음 줄로 넘긴다.
            if (value.text.contains('\n')) {
                input = TextFieldValue(value.text.replace("\n", ""))
                submitLine()
                return@BasicTextField
            }
            if (startedAt == 0L && value.text.isNotEmpty()) startedAt = System.currentTimeMillis()
            input = value
            // 줄을 정확히 다 치면 엔터 없이 다음 줄로 넘어간다.
            if (value.text == target && value.composition == null) submitLine()
        },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            // 말랑키는 이런 입력칸에서 엔터를 '다음' 동작이 아니라 엔터 키로 보내므로 키를 직접 받는다.
            .onPreviewKeyEvent { event ->
                val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter
                if (isEnter && event.type == KeyEventType.KeyUp) submitLine()
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
        keyboardActions = KeyboardActions(onNext = { submitLine() }, onDone = { submitLine() }),
    )
    Text(
        if (startedAt == 0L) "위 가사를 따라 치면 시간이 시작돼요. 한 줄을 다 치면 엔터를 누르세요."
        else "틀려도 엔터를 누르면 다음 줄로 넘어가요. 오타는 점수에서 깎여요.",
        color = MalangSettingsSummary,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    )
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

@Composable
private fun AnthemResult(result: TypingScore.Result, modeLabel: String, onRetry: () -> Unit) {
    val context = LocalContext.current
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()
    val prefs by FlorisPreferenceStore
    val best by prefs.malang.anthemBestScore.collectAsState()
    val isNewBest = result.score > best
    val playedAt = remember { SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA).format(Date()) }
    val cardLayer = rememberGraphicsLayer()

    LaunchedEffect(result) {
        if (result.score > prefs.malang.anthemBestScore.get()) prefs.malang.anthemBestScore.set(result.score)
    }

    // 사진으로 저장할 카드. 그리는 내용을 그대로 이미지로 뽑을 수 있게 레이어에 기록한다.
    Box(
        modifier = Modifier.drawWithContent {
            cardLayer.record { this@drawWithContent.drawContent() }
            drawLayer(cardLayer)
        }
    ) {
        ResultCard(result, modeLabel, playedAt, isNewBest)
    }

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
private fun ResultCard(result: TypingScore.Result, modeLabel: String, playedAt: String, isNewBest: Boolean) {
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
        if (isNewBest) {
            Text(
                "최고 기록!",
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MalangCardText)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                color = MalangCocoa,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Text("${result.score}", color = MalangCardText, fontSize = 72.sp, lineHeight = 76.sp, fontFamily = JuaFontFamily)
        Text("점", color = MalangCardSubLight, fontSize = 16.sp, fontFamily = JuaFontFamily)
        Text(
            "${result.grade}등급 · ${result.gradeTitle}",
            color = MalangCardText,
            fontSize = 20.sp,
            fontFamily = JuaFontFamily,
            textAlign = TextAlign.Center,
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

@Composable
private fun ResultStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = MalangCardText, fontSize = 18.sp, fontFamily = JuaFontFamily)
        Text(label, color = MalangCardSubMuted, fontSize = 12.sp)
    }
}

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
