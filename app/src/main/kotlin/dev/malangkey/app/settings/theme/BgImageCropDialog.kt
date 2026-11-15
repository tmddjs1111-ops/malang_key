package dev.malangkey.app.settings.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.malangkey.app.FlorisPreferenceStore
import dev.malangkey.app.apptheme.JuaFontFamily
import dev.malangkey.app.apptheme.MalangButton
import dev.malangkey.app.apptheme.MalangSlider
import dev.malangkey.app.apptheme.MalangSettingsCard
import dev.malangkey.app.apptheme.MalangSettingsSummary
import dev.malangkey.app.apptheme.MalangSettingsTitle
import dev.malangkey.app.setup.KeyboardPreview
import dev.malangkey.app.setup.rememberCustomPreviewPalette
import dev.malangkey.ime.theme.CustomThemeImage
import dev.malangkey.subtypeManager
import dev.patrickgold.jetpref.datastore.model.collectAsState
import kotlin.math.max
import kotlin.math.roundToInt

private const val MaxZoom = 5f

/** 키보드 창에서 스마트바 아래 자판이 차지하는 대략의 비율. 자판 안내선을 그 자리에 그린다. */
private const val KeyboardAreaRatio = 0.84f

/**
 * 배경 사진을 키보드 모양 틀에 맞추는 창. 끌어서 옮기고 두 손가락으로 확대한다.
 * 틀 위에 실제 자판을 반투명하게 겹쳐서 키가 어디에 올지 보여 준다.
 */
@Composable
fun BgImageCropDialog(
    sourceName: String,
    initialCrop: CustomThemeImage.Crop?,
    onDismiss: () -> Unit,
    onConfirm: (CustomThemeImage.Crop) -> Unit,
) {
    val context = LocalContext.current
    val prefs by FlorisPreferenceStore
    val aspect by prefs.malang.keyboardWindowAspect.collectAsState()
    val image by produceState<ImageBitmap?>(initialValue = null, sourceName) {
        value = CustomThemeImage.loadSource(context, sourceName)?.asImageBitmap()
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MalangSettingsCard)
                .padding(16.dp),
        ) {
            Text("사진 위치 맞추기", color = MalangSettingsTitle, fontFamily = JuaFontFamily, fontSize = 22.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("끌어서 옮기고, 아래 막대나 두 손가락으로 확대해요. 확대하면 좌우로도 옮길 수 있어요.", color = MalangSettingsSummary, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(12.dp))

            val cropHolder = remember { CropHolder() }
            val loaded = image
            if (loaded == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(aspect),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                CropFrame(loaded, aspect, initialCrop, cropHolder)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MalangButton("취소", modifier = Modifier.weight(1f), primary = false, onClick = onDismiss)
                MalangButton(
                    "적용",
                    modifier = Modifier.weight(1f),
                    enabled = loaded != null,
                    onClick = { cropHolder.current?.let { onConfirm(it()) } },
                )
            }
        }
    }
}

/** 틀 안의 지금 위치를 잘라낼 영역으로 바꾸는 함수를 담는다. 화면을 다시 그릴 필요는 없어서 상태로 두지 않는다. */
private class CropHolder {
    var current: (() -> CustomThemeImage.Crop)? = null
}

@Composable
private fun CropFrame(
    image: ImageBitmap,
    aspect: Float,
    initialCrop: CustomThemeImage.Crop?,
    cropHolder: CropHolder,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val subtypeManager by context.subtypeManager()
    val activeSubtype by subtypeManager.activeSubtypeFlow.collectAsState()
    val prefs by FlorisPreferenceStore
    val keyOpacity by prefs.malang.customBgImageKeyOpacity.collectAsState()
    val customPalette = rememberCustomPreviewPalette()
    // 안내용 자판은 판 배경 없이, 설정한 키 불투명도로만 그린다.
    val guidePalette = remember(customPalette, keyOpacity) {
        val alpha = keyOpacity / 100f
        customPalette.copy(
            background = Color.Transparent,
            backgroundImage = null,
            keyBackground = customPalette.keyBackground.copy(alpha = alpha),
            byCode = customPalette.byCode.mapValues { (_, colors) -> colors.first.copy(alpha = alpha) to colors.second },
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val frameW = constraints.maxWidth.toFloat()
        val frameH = frameW / aspect
        val baseScale = max(frameW / image.width, frameH / image.height)

        var zoom by remember(image, frameW, frameH) {
            mutableFloatStateOf(
                initialCrop?.let { (1f / (it.right - it.left)) * frameW / (image.width * baseScale) }?.coerceIn(1f, MaxZoom) ?: 1f
            )
        }
        fun drawnW() = image.width * baseScale * zoom
        fun drawnH() = image.height * baseScale * zoom
        fun clamp(offset: Offset) = Offset(
            offset.x.coerceIn(frameW - drawnW(), 0f),
            offset.y.coerceIn(frameH - drawnH(), 0f),
        )
        var offset by remember(image, frameW, frameH) {
            mutableStateOf(
                clamp(
                    if (initialCrop != null) {
                        Offset(-initialCrop.left * drawnW(), -initialCrop.top * drawnH())
                    } else {
                        Offset((frameW - drawnW()) / 2f, (frameH - drawnH()) / 2f)
                    }
                )
            )
        }

        cropHolder.current = {
            val left = (-offset.x / drawnW()).coerceIn(0f, 1f)
            val top = (-offset.y / drawnH()).coerceIn(0f, 1f)
            CustomThemeImage.Crop(
                left = left,
                top = top,
                right = (left + frameW / drawnW()).coerceIn(left, 1f),
                bottom = (top + frameH / drawnH()).coerceIn(top, 1f),
            )
        }

        /** 틀 가운데를 기준으로 [newZoom]배로 바꾼다. */
        fun zoomTo(newZoom: Float, center: Offset = Offset(frameW / 2f, frameH / 2f)) {
            val target = newZoom.coerceIn(1f, MaxZoom)
            val applied = target / zoom
            zoom = target
            offset = clamp((offset - center) * applied + center)
        }

        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspect)
                    .clip(RoundedCornerShape(12.dp))
                    .pointerInput(image, frameW, frameH) {
                        detectTransformGestures { centroid, pan, gestureZoom, _ ->
                            zoomTo(zoom * gestureZoom, centroid)
                            offset = clamp(offset + pan)
                        }
                    },
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawImage(
                        image = image,
                        dstOffset = IntOffset(offset.x.toInt(), offset.y.toInt()),
                        dstSize = IntSize(drawnW().toInt() + 1, drawnH().toInt() + 1),
                        filterQuality = FilterQuality.Medium,
                    )
                }
                KeyboardPreview(
                    subtype = activeSubtype,
                    palette = guidePalette,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    height = with(density) { (frameH * KeyboardAreaRatio).toDp() },
                )
            }
            // 두 손가락 확대를 모르는 사람도 쓸 수 있게 확대 슬라이더를 둔다. 확대해야 좌우로도 옮길 수 있다.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("확대", color = MalangSettingsTitle, fontSize = 14.sp)
                Box(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    MalangSlider(
                        value = zoom,
                        onValueChange = { zoomTo(it) },
                        onValueChangeFinished = {},
                        range = 1f..MaxZoom,
                    )
                }
                Text("${(zoom * 100).roundToInt()}%", color = MalangSettingsSummary, fontSize = 14.sp)
            }
        }
    }
}
