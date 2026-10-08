package dev.malangkey.ime.theme

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dev.malangkey.app.FlorisPreferenceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import java.util.UUID

/**
 * 커스텀 테마(색 + 배경 사진)를 여러 개 저장해 두고 골라 쓰는 "내 테마".
 *
 * 목록은 설정값에 JSON으로 두고, 사진은 `custom_theme_saved/<id>/` 폴더에 따로 복사한다.
 * 지금 쓰는 사진 폴더([CustomThemeImage])와 나눠 두어서, 사진을 바꾸거나 지워도 저장한 테마는 남는다.
 */
object CustomThemeLibrary {
    private const val DirName = "custom_theme_saved"
    const val MaxThemes = 20

    /** 공유 파일 확장자. FileProvider의 `theme-share/` 캐시 폴더에 만든다. */
    const val FileExtension = "malangtheme"
    private const val ShareDirName = "theme-share"
    private const val SharedJsonName = "theme.json"
    private const val SharedImageName = "bg.jpg"
    private const val SharedSourceName = "src.jpg"
    private val SharedEntryNames = setOf(SharedJsonName, SharedImageName, SharedSourceName)
    private const val MaxSharedEntryBytes = 10 * 1024 * 1024

    // 기본값도 모두 적어 둬야, 나중에 기본값이 바뀌어도 공유한 테마가 그대로 보인다.
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** 색은 ARGB 정수, 고르지 않은 색은 null. 사진 파일 이름은 그 테마 폴더 안의 이름이다. */
    @Serializable
    data class SavedTheme(
        val id: String,
        val name: String,
        val keyboardBg: Int? = null,
        val keyBg: Int? = null,
        val keyText: Int? = null,
        val specialBg: Int? = null,
        val specialText: Int? = null,
        val enterBg: Int? = null,
        val enterText: Int? = null,
        val bgImage: String? = null,
        val bgSource: String? = null,
        val crop: String = "",
        val dim: Int = 15,
        val keyOpacity: Int = 55,
        val blur: Int = 0,
    )

    fun decode(value: String): List<SavedTheme> =
        runCatching { json.decodeFromString<List<SavedTheme>>(value) }.getOrDefault(emptyList())

    private fun encode(themes: List<SavedTheme>) = json.encodeToString(themes)

    private fun dir(context: Context, id: String) = File(File(context.filesDir, DirName), id)

    /** 저장한 테마의 사진 파일. 없으면 null. */
    fun imageFile(context: Context, theme: SavedTheme): File? =
        theme.bgImage?.let { File(dir(context, theme.id), it) }?.takeIf { it.isFile }

    /** 지금 커스텀 테마를 [name]으로 저장한다. 사진도 함께 복사한다. */
    suspend fun saveCurrent(context: Context, prefs: FlorisPreferenceModel, name: String): Result<SavedTheme> =
        withContext(Dispatchers.IO) {
            runCatching {
                val themes = decode(prefs.malang.customThemeLibrary.get())
                check(themes.size < MaxThemes) { "Too many saved themes" }
                val id = UUID.randomUUID().toString()
                val folder = dir(context, id).apply { mkdirs() }
                fun copy(fileName: String): String? {
                    val source = CustomThemeImage.file(context, fileName) ?: return null
                    source.copyTo(File(folder, fileName), overwrite = true)
                    return fileName
                }
                val m = prefs.malang
                val theme = SavedTheme(
                    id = id,
                    name = name,
                    keyboardBg = m.customKeyboardBgColor.get().argbOrNull(),
                    keyBg = m.customKeyBgColor.get().argbOrNull(),
                    keyText = m.customKeyTextColor.get().argbOrNull(),
                    specialBg = m.customEnterKeyBgColor.get().argbOrNull(),
                    specialText = m.customEnterKeyTextColor.get().argbOrNull(),
                    enterBg = m.customRealEnterKeyBgColor.get().argbOrNull(),
                    enterText = m.customRealEnterKeyTextColor.get().argbOrNull(),
                    bgImage = copy(m.customBgImageFile.get()),
                    bgSource = copy(m.customBgImageSource.get()),
                    crop = m.customBgImageCrop.get(),
                    dim = m.customBgImageDim.get(),
                    keyOpacity = m.customBgImageKeyOpacity.get(),
                    blur = m.customBgImageBlur.get(),
                )
                prefs.malang.customThemeLibrary.set(encode(themes + theme))
                theme
            }
        }

    /** 저장한 테마를 지금 커스텀 테마로 불러온다. 사진은 지금 쓰는 사진 폴더로 새 이름을 붙여 복사한다. */
    suspend fun apply(context: Context, prefs: FlorisPreferenceModel, theme: SavedTheme): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val folder = dir(context, theme.id)
                val stamp = System.currentTimeMillis()
                fun copyIn(fileName: String?, prefix: String): String {
                    val source = fileName?.let { File(folder, it) }?.takeIf { it.isFile } ?: return ""
                    val target = "${prefix}_$stamp.jpg"
                    CustomThemeImage.importFile(context, source, target)
                    return target
                }
                val bg = copyIn(theme.bgImage, "bg")
                val src = if (bg.isEmpty()) "" else copyIn(theme.bgSource, "src")
                val m = prefs.malang
                m.customKeyboardBgColor.set(theme.keyboardBg.toColor())
                m.customKeyBgColor.set(theme.keyBg.toColor())
                m.customKeyTextColor.set(theme.keyText.toColor())
                m.customEnterKeyBgColor.set(theme.specialBg.toColor())
                m.customEnterKeyTextColor.set(theme.specialText.toColor())
                m.customRealEnterKeyBgColor.set(theme.enterBg.toColor())
                m.customRealEnterKeyTextColor.set(theme.enterText.toColor())
                m.customBgImageDim.set(theme.dim)
                m.customBgImageKeyOpacity.set(theme.keyOpacity)
                m.customBgImageBlur.set(theme.blur)
                m.customBgImageCrop.set(if (src.isEmpty()) "" else theme.crop)
                m.customBgImageSource.set(src)
                m.customBgImageFile.set(bg)
                CustomThemeImage.deleteAllExcept(context, setOf(bg, src))
            }
        }

    /**
     * [theme]을 다른 사람에게 보낼 파일 하나로 묶는다. 설정 JSON과 사진(배경·원본)을 zip으로 담고,
     * 확장자는 [FileExtension]이다. 파일은 캐시 폴더에 만들고 그 파일을 돌려준다.
     */
    suspend fun export(context: Context, theme: SavedTheme): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.cacheDir, ShareDirName).apply { deleteRecursively(); mkdirs() }
            val safeName = theme.name.replace(Regex("""[\\/:*?"<>|]"""), "_").ifBlank { "theme" }
            val out = File(dir, "$safeName.$FileExtension")
            val folder = dir(context, theme.id)
            val image = theme.bgImage?.let { File(folder, it) }?.takeIf { it.isFile }
            val source = theme.bgSource?.let { File(folder, it) }?.takeIf { it.isFile }
            val shared = theme.copy(
                id = "",
                bgImage = image?.let { SharedImageName },
                bgSource = source?.let { SharedSourceName },
            )
            ZipOutputStream(out.outputStream().buffered()).use { zip ->
                zip.putNextEntry(ZipEntry(SharedJsonName))
                zip.write(json.encodeToString(shared).toByteArray())
                zip.closeEntry()
                for ((file, entryName) in listOf(image to SharedImageName, source to SharedSourceName)) {
                    file ?: continue
                    zip.putNextEntry(ZipEntry(entryName))
                    file.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            out
        }
    }

    /** 다른 사람이 보낸 테마 파일 [uri]를 읽어 내 테마에 더한다. */
    suspend fun import(context: Context, prefs: FlorisPreferenceModel, uri: Uri): Result<SavedTheme> =
        withContext(Dispatchers.IO) {
            runCatching {
                val themes = decode(prefs.malang.customThemeLibrary.get())
                check(themes.size < MaxThemes) { "Too many saved themes" }
                // 정해진 이름의 항목만, 정해진 크기까지만 읽는다.
                val entries = mutableMapOf<String, ByteArray>()
                val stream = checkNotNull(context.contentResolver.openInputStream(uri)) { "Cannot open $uri" }
                ZipInputStream(stream.buffered()).use { zip ->
                    while (true) {
                        val entry = zip.nextEntry ?: break
                        if (entry.name in SharedEntryNames && entry.name !in entries) {
                            entries[entry.name] = zip.readLimited(MaxSharedEntryBytes)
                        }
                    }
                }
                val shared = json.decodeFromString<SavedTheme>(
                    checkNotNull(entries[SharedJsonName]) { "Not a theme file" }.decodeToString()
                )
                val id = UUID.randomUUID().toString()
                val folder = dir(context, id).apply { mkdirs() }
                fun saveImage(entryName: String): String? {
                    val bytes = entries[entryName] ?: return null
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
                    File(folder, entryName).writeBytes(bytes)
                    return entryName
                }
                val bgImage = saveImage(SharedImageName)
                val theme = shared.copy(
                    id = id,
                    name = shared.name.take(20).ifBlank { "받은 테마" },
                    bgImage = bgImage,
                    bgSource = if (bgImage != null) saveImage(SharedSourceName) else null,
                    dim = shared.dim.coerceIn(0, 80),
                    keyOpacity = shared.keyOpacity.coerceIn(0, 100),
                    blur = shared.blur.coerceIn(0, 10),
                )
                prefs.malang.customThemeLibrary.set(encode(themes + theme))
                theme
            }
        }

    private fun ZipInputStream.readLimited(limit: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            out.write(buffer, 0, read)
            check(out.size() <= limit) { "Entry too large" }
        }
        return out.toByteArray()
    }

    suspend fun rename(prefs: FlorisPreferenceModel, id: String, name: String) {
        val themes = decode(prefs.malang.customThemeLibrary.get())
        prefs.malang.customThemeLibrary.set(encode(themes.map { if (it.id == id) it.copy(name = name) else it }))
    }

    suspend fun delete(context: Context, prefs: FlorisPreferenceModel, id: String) {
        val themes = decode(prefs.malang.customThemeLibrary.get())
        prefs.malang.customThemeLibrary.set(encode(themes.filterNot { it.id == id }))
        withContext(Dispatchers.IO) { dir(context, id).deleteRecursively() }
    }

    private fun Color.argbOrNull(): Int? = if (this == Color.Unspecified) null else toArgb()

    private fun Int?.toColor(): Color = if (this == null) Color.Unspecified else Color(this)
}
