package dev.malangkey.ime.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import dev.malangkey.app.FlorisPreferenceModel
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.ime.smartbar.quickaction.QuickAction
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.min

/**
 * 커스텀 테마에서 스마트바 버튼 아이콘 대신 쓰는 사진.
 *
 * 사진은 정사각형으로 잘라 `custom_theme_icons/` 폴더에 PNG로 두고, 어떤 버튼에 어떤 사진을 쓰는지는
 * 설정값 [dev.malangkey.app.FlorisPreferenceModel.Malang.customSmartbarIcons]에 `버튼 키 → 파일 이름` JSON으로 둔다.
 * 배경 사진 폴더와 따로 두어서, 배경 사진을 바꿀 때 지우는 파일에 섞이지 않는다.
 */
object CustomSmartbarIcons {
    internal const val DirName = "custom_theme_icons"
    private const val IconSidePx = 192
    /** 공유 파일 안 항목 이름이자 이 폴더의 파일 이름 모양. */
    private val FileNameRegex = Regex("""icon_\d{1,20}(_\d{1,4})?\.png""")
    private const val MaxKeyLength = 200
    const val MaxIcons = 20

    private val json = Json { ignoreUnknownKeys = true }

    private val _revision = MutableStateFlow(0)

    /** 아이콘 폴더가 바뀔 때마다 오르는 번호. 파일 이름이 같아도 다시 읽게 한다. */
    val revision: StateFlow<Int> = _revision

    internal fun bumpRevision() {
        cache.evictAll()
        _revision.update { it + 1 }
    }

    private val cache = LruCache<String, ImageBitmap>(MaxIcons * 2)

    /** 버튼을 가리키는 키. 같은 기능이면 스마트바 어느 자리에 있어도 같은 사진을 쓴다. */
    fun actionKey(action: QuickAction): String = when (action) {
        is QuickAction.InsertKey -> "key:${action.data.code}"
        is QuickAction.InsertText -> "text:${action.data}"
        is QuickAction.QuickPhrase -> "phrase:${action.text}"
    }.take(MaxKeyLength)

    fun isValidFileName(name: String) = FileNameRegex.matches(name)

    private fun isValidKey(key: String) =
        key.length <= MaxKeyLength && (key.startsWith("key:") || key.startsWith("text:") || key.startsWith("phrase:"))

    /** 설정값을 읽는다. 모양이 틀린 항목은 버린다. */
    fun decode(value: String): Map<String, String> {
        if (value.isBlank()) return emptyMap()
        val map = runCatching { json.decodeFromString<Map<String, String>>(value) }.getOrDefault(emptyMap())
        return sanitize(map)
    }

    fun encode(map: Map<String, String>): String = if (map.isEmpty()) "" else json.encodeToString(map)

    /** 키와 파일 이름 모양을 확인하고 개수를 [MaxIcons]개로 줄인다. 받은 테마 파일을 읽을 때도 쓴다. */
    fun sanitize(map: Map<String, String>): Map<String, String> =
        map.filter { (key, name) -> isValidKey(key) && isValidFileName(name) }.entries.take(MaxIcons)
            .associate { it.key to it.value }

    private fun dir(context: Context) = File(context.filesDir, DirName)

    fun file(context: Context, fileName: String): File? {
        if (!isValidFileName(fileName)) return null
        return File(dir(context), fileName).takeIf { it.isFile }
    }

    /** 갤러리의 [source] 사진을 가운데 정사각형으로 잘라 아이콘으로 저장하고 새 파일 이름을 돌려준다. */
    suspend fun import(context: Context, source: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val decoded = checkNotNull(CustomThemeImage.decodeScaled(context, source, IconSidePx * 2)) {
                "Could not decode $source"
            }
            val rotated = CustomThemeImage.rotateByExif(context, source, decoded)
            val side = min(rotated.width, rotated.height)
            val square = Bitmap.createBitmap(rotated, (rotated.width - side) / 2, (rotated.height - side) / 2, side, side)
            val icon = Bitmap.createScaledBitmap(square, IconSidePx, IconSidePx, true)
            val fileName = newFileName()
            val target = File(dir(context).apply { mkdirs() }, fileName)
            target.outputStream().use { out ->
                check(icon.compress(Bitmap.CompressFormat.PNG, 100, out)) { "Could not save icon" }
            }
            if (icon !== square) icon.recycle()
            if (square !== rotated) square.recycle()
            rotated.recycle()
            bumpRevision()
            fileName
        }
    }

    private var lastStamp = 0L

    @Synchronized
    private fun newFileName(): String {
        // 한 번에 여러 개를 복사할 때 이름이 겹치지 않게 한다.
        val stamp = maxOf(System.currentTimeMillis(), lastStamp + 1)
        lastStamp = stamp
        return "icon_$stamp.png"
    }

    /** 다른 곳에 있는 아이콘 [source]를 새 이름으로 이 폴더에 복사하고 그 이름을 돌려준다. */
    fun importFile(context: Context, source: File): String {
        val fileName = newFileName()
        source.copyTo(File(dir(context).apply { mkdirs() }, fileName), overwrite = true)
        return fileName
    }

    /** [keep]에 없는 파일을 모두 지운다. */
    fun deleteAllExcept(context: Context, keep: Collection<String>) {
        dir(context).listFiles()?.filter { it.name !in keep }?.forEach { it.delete() }
        bumpRevision()
    }

    /** 아이콘 사진과 스마트바 색을 모두 처음 상태로 되돌린다. */
    suspend fun reset(context: Context, prefs: FlorisPreferenceModel) {
        prefs.malang.customSmartbarIcons.set("")
        withContext(Dispatchers.IO) { deleteAllExcept(context, emptySet()) }
    }

    fun load(context: Context, fileName: String): ImageBitmap? {
        cache.get(fileName)?.let { return it }
        val file = file(context, fileName) ?: return null
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap() ?: return null
        cache.put(fileName, bitmap)
        return bitmap
    }
}

/** 커스텀 테마가 켜져 있을 때 [action] 버튼에 쓸 사진. 없으면 null. */
@Composable
fun rememberCustomSmartbarIcon(action: QuickAction): ImageBitmap? {
    val prefs by FlorisPreferenceStore
    val iconsValue by prefs.malang.customSmartbarIcons.collectAsState()
    val revision by CustomSmartbarIcons.revision.collectAsState()
    val isCustom = rememberIsCustomThemeActive()
    val fileName = remember(iconsValue, action, isCustom) {
        if (!isCustom) null else CustomSmartbarIcons.decode(iconsValue)[CustomSmartbarIcons.actionKey(action)]
    }
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(initialValue = null, fileName, revision) {
        value = fileName?.let { withContext(Dispatchers.IO) { CustomSmartbarIcons.load(context, it) } }
    }
    return icon
}
