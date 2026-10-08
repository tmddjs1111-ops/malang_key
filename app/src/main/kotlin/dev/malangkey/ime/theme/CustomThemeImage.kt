package dev.malangkey.ime.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import dev.malangkey.app.FlorisPreferenceModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URI
import kotlin.math.roundToInt

/**
 * 커스텀 테마의 배경 사진. 사용자가 고른 사진은 줄여서 원본(`src_`)으로 앱 안쪽 폴더에 두고,
 * 사용자가 맞춘 위치대로 잘라낸 배경(`bg_`)을 따로 저장한다. 키보드 스타일시트에서는
 * `malang:/custom-bg/<파일 이름>` 주소로 배경을 가리킨다.
 *
 * 저장할 때마다 파일 이름이 달라져서 이미지 캐시에 옛 사진이 남지 않는다.
 */
object CustomThemeImage {
    private const val UriScheme = "malang"
    private const val UriDir = "/custom-bg/"
    private const val DirName = "custom_theme"
    private const val SourceMaxSidePx = 1600
    private const val BackgroundMaxWidthPx = 1440
    private const val JpegQuality = 90

    /** 원본 사진에서 잘라 쓸 영역. 각 값은 원본 가로·세로에 대한 비율(0~1)이다. */
    data class Crop(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        fun serialize() = "$left,$top,$right,$bottom"

        companion object {
            fun deserialize(value: String): Crop? {
                val parts = value.split(',').mapNotNull { it.toFloatOrNull() }
                if (parts.size != 4) return null
                val crop = Crop(parts[0], parts[1], parts[2], parts[3])
                val valid = crop.left in 0f..1f && crop.top in 0f..1f && crop.right in 0f..1f &&
                    crop.bottom in 0f..1f && crop.left < crop.right && crop.top < crop.bottom
                return crop.takeIf { valid }
            }
        }
    }

    private fun dir(context: Context) = File(context.filesDir, DirName)

    private val _revision = MutableStateFlow(0)

    /**
     * 사진 폴더가 바뀔 때마다 오르는 번호. 설정값은 그대로인데 파일만 바뀌는 경우(백업 복원 등)에도
     * 키보드가 파일이 있는지 다시 확인하게 한다.
     */
    val revision: StateFlow<Int> = _revision

    private fun bumpRevision() = _revision.update { it + 1 }

    fun uriFor(fileName: String) = "$UriScheme:$UriDir$fileName"

    fun file(context: Context, fileName: String): File? {
        if (fileName.isEmpty() || fileName.contains('/') || fileName.contains("..")) return null
        return File(dir(context), fileName).takeIf { it.isFile }
    }

    /** [uri]가 배경 사진 주소면 그 파일의 절대 경로, 아니면 null. */
    fun resolve(context: Context, uri: String): Result<String>? {
        val parsed = runCatching { URI.create(uri) }.getOrNull() ?: return null
        if (parsed.scheme != UriScheme) return null
        return runCatching {
            val path = checkNotNull(parsed.path) { "No path in '$uri'" }
            require(path.startsWith(UriDir)) { "Unknown path '$path'" }
            val file = checkNotNull(file(context, path.removePrefix(UriDir))) { "Missing file for '$uri'" }
            file.absolutePath
        }
    }

    /** 갤러리의 [source] 사진을 줄이고 바로 세워 원본으로 저장한다. 새 원본 파일 이름을 돌려준다. */
    suspend fun importSource(context: Context, source: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = checkNotNull(decodeScaled(context, source)) { "Could not decode $source" }
            val rotated = rotateByExif(context, source, bitmap)
            val fileName = writeJpeg(context, "src", rotated)
            rotated.recycle()
            fileName
        }
    }

    suspend fun loadSource(context: Context, fileName: String): Bitmap? = withContext(Dispatchers.IO) {
        file(context, fileName)?.let { BitmapFactory.decodeFile(it.absolutePath) }
    }

    /**
     * 원본 [sourceName]에서 [crop] 영역을 잘라 [blur] 단계(0~10)만큼 흐리게 한 뒤 배경으로 저장하고
     * 새 배경 파일 이름을 돌려준다. 원본과 새 배경을 뺀 나머지 파일은 지운다.
     */
    suspend fun saveCropped(context: Context, sourceName: String, crop: Crop, blur: Int): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val source = checkNotNull(loadSource(context, sourceName)) { "Missing source $sourceName" }
            val x = (crop.left * source.width).roundToInt().coerceIn(0, source.width - 1)
            val y = (crop.top * source.height).roundToInt().coerceIn(0, source.height - 1)
            val w = ((crop.right - crop.left) * source.width).roundToInt().coerceIn(1, source.width - x)
            val h = ((crop.bottom - crop.top) * source.height).roundToInt().coerceIn(1, source.height - y)
            val scale = (BackgroundMaxWidthPx.toFloat() / w).coerceAtMost(1f)
            val matrix = Matrix().apply { postScale(scale, scale) }
            val cropped = Bitmap.createBitmap(source, x, y, w, h, matrix, true)
            val blurred = blurred(cropped, blur)
            val fileName = writeJpeg(context, "bg", blurred)
            if (blurred !== cropped) blurred.recycle()
            if (cropped !== source) cropped.recycle()
            source.recycle()
            deleteAllExcept(context, setOf(sourceName, fileName))
            fileName
        }
    }

    /** 다른 곳에 있는 사진 [source]를 사진 폴더에 [fileName]으로 복사한다. */
    fun importFile(context: Context, source: File, fileName: String) {
        val dir = dir(context).apply { mkdirs() }
        source.copyTo(File(dir, fileName), overwrite = true)
        bumpRevision()
    }

    /** [keep]에 없는 파일을 모두 지운다. */
    fun deleteAllExcept(context: Context, keep: Set<String>) {
        dir(context).listFiles()?.filter { it.name !in keep }?.forEach { it.delete() }
        bumpRevision()
    }

    fun clear(context: Context) = deleteAllExcept(context, emptySet())

    /** 백업에 함께 넣는 폴더. 설정값이 이 폴더 안의 파일 이름을 가리킨다. */
    private val BackupDirNames = listOf(DirName, "custom_theme_saved")

    /** 지금 사진과 내 테마 사진을 백업 작업 폴더 [filesDir]에 복사한다. */
    fun backupTo(context: Context, filesDir: File) {
        for (name in BackupDirNames) {
            val dir = File(context.filesDir, name)
            if (dir.isDirectory) dir.copyRecursively(File(filesDir, name), overwrite = true)
        }
    }

    /** 백업 작업 폴더 [filesDir]에 있는 사진을 되살린다. 지금 사진은 백업 것으로 바꾸고, 내 테마는 합친다. */
    fun restoreFrom(context: Context, filesDir: File) {
        for (name in BackupDirNames) {
            val source = File(filesDir, name)
            if (!source.isDirectory) continue
            val target = File(context.filesDir, name)
            if (name == DirName) target.deleteRecursively()
            source.copyRecursively(target, overwrite = true)
        }
        bumpRevision()
    }

    /** 배경 사진과 사진 설정을 모두 처음 상태로 되돌린다. */
    suspend fun reset(context: Context, prefs: FlorisPreferenceModel) {
        prefs.malang.customBgImageFile.set("")
        prefs.malang.customBgImageSource.set("")
        prefs.malang.customBgImageCrop.set("")
        prefs.malang.customBgImageDim.set(prefs.malang.customBgImageDim.default)
        prefs.malang.customBgImageKeyOpacity.set(prefs.malang.customBgImageKeyOpacity.default)
        prefs.malang.customBgImageBlur.set(prefs.malang.customBgImageBlur.default)
        withContext(Dispatchers.IO) { clear(context) }
    }

    /** 작게 줄였다가 다시 키워서 흐리게 만든다. 단계가 클수록 더 작게 줄인다. */
    private fun blurred(bitmap: Bitmap, level: Int): Bitmap {
        if (level <= 0) return bitmap
        val factor = 1f + level * 2f
        val smallW = (bitmap.width / factor).roundToInt().coerceAtLeast(1)
        val smallH = (bitmap.height / factor).roundToInt().coerceAtLeast(1)
        // 한 번에 줄이면 계단이 지므로 반씩 나눠 줄인다.
        var current = bitmap
        while (current.width / 2 >= smallW && current.height / 2 >= smallH) {
            val next = Bitmap.createScaledBitmap(current, current.width / 2, current.height / 2, true)
            if (current !== bitmap) current.recycle()
            current = next
        }
        val small = Bitmap.createScaledBitmap(current, smallW, smallH, true)
        if (current !== bitmap && current !== small) current.recycle()
        val result = Bitmap.createScaledBitmap(small, bitmap.width, bitmap.height, true)
        if (small !== result) small.recycle()
        return result
    }

    /** 미리보기용으로 [maxSidePx] 안팎까지 줄여 읽는다. */
    fun loadThumbnail(context: Context, fileName: String, maxSidePx: Int): Bitmap? {
        val file = file(context, fileName) ?: return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxSidePx)
        }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun writeJpeg(context: Context, prefix: String, bitmap: Bitmap): String {
        val dir = dir(context).apply { mkdirs() }
        val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
        File(dir, fileName).outputStream().use { out ->
            check(bitmap.compress(Bitmap.CompressFormat.JPEG, JpegQuality, out)) { "Could not save image" }
        }
        return fileName
    }

    private fun decodeScaled(context: Context, source: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, SourceMaxSidePx)
        }
        val decoded = resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return null
        val longest = maxOf(decoded.width, decoded.height)
        if (longest <= SourceMaxSidePx) return decoded
        val scale = SourceMaxSidePx.toFloat() / longest
        val scaled = Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true,
        )
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    private fun rotateByExif(context: Context, source: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(source)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            else -> return bitmap
        }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun sampleSizeFor(width: Int, height: Int, maxSidePx: Int): Int {
        var sample = 1
        while (maxOf(width, height) / (sample * 2) >= maxSidePx) sample *= 2
        return sample
    }
}
