package dev.malangkey.ime.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import dev.malangkey.app.FlorisPreferenceStore
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/** 키보드 배경 사진. 고른 사진을 앱 안에 줄여서 복사해 두고, 키보드 뒤에 깐다. */
object KeyboardBackgroundPhoto {
    private const val DIR = "keyboard_background"
    private const val MAX_SIDE = 1600

    /** 사진을 줄여 저장하고 저장한 파일 경로를 준다. 읽을 수 없는 사진이면 null. */
    suspend fun save(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = decode(context, uri)
            val dir = File(context.filesDir, DIR).apply { mkdirs() }
            val file = File(dir, "bg_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            bitmap.recycle()
            // 예전 사진은 지운다.
            dir.listFiles()?.filter { it != file }?.forEach { it.delete() }
            file.absolutePath
        }.getOrNull()
    }

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        File(context.filesDir, DIR).listFiles()?.forEach { it.delete() }
    }

    private fun decode(context: Context, uri: Uri): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val side = max(info.size.width, info.size.height)
                if (side > MAX_SIDE) {
                    val scale = MAX_SIDE.toFloat() / side
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_SIDE) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
            ?: error("Could not decode $uri")
    }
}

/** 키보드 창 뒤에 까는 배경 사진과 어둡게 하는 막. 사진이 없으면 아무것도 그리지 않는다. */
@Composable
fun KeyboardBackgroundPhotoLayer(modifier: Modifier) {
    val prefs by FlorisPreferenceStore
    val path by prefs.malang.bgImageUri.collectAsState()
    val dimPercent by prefs.malang.bgDimPercent.collectAsState()
    val image by produceState<ImageBitmap?>(null, path) {
        value = if (path.isEmpty()) null else withContext(Dispatchers.IO) {
            runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()
        }
    }
    val bitmap = image ?: return
    Box(modifier) {
        Image(bitmap, contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Crop)
        if (dimPercent > 0) {
            Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = dimPercent / 100f)))
        }
    }
}
