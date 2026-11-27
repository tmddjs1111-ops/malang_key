/*
 * 설정 앱 홈 화면 하단 배너 광고.
 *
 * 원칙: 키보드로 입력한 내용은 절대 기기 밖으로 나가지 않는다. 광고는 설정 앱 화면에만 보이고,
 * 키보드(IME) 화면에는 절대 넣지 않는다. 다른 앱 위에 뜨는 키보드에 광고를 띄우면
 * Google Play 정책(앱 밖 광고·방해 광고) 위반이다.
 */

package dev.malangkey.app.settings

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import dev.malangkey.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/** 광고 SDK 는 설정 홈 화면이 처음 보일 때 한 번만 초기화한다. 키보드만 쓸 때는 초기화하지 않는다. */
object MalangAds {
    private val started = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    fun init(context: Context) {
        if (!started.compareAndSet(false, true)) return
        val app = context.applicationContext
        // Google 권장대로 메인 스레드가 아닌 곳에서 초기화한다.
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(app) { _ready.value = true }
        }
    }
}

/**
 * 화면 너비에 맞춘 고정형 적응 배너. 광고가 늦게 와도 카드가 흔들리지 않게
 * 배너 높이만큼 자리를 먼저 잡아 둔다.
 */
@Composable
fun HomeBannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { MalangAds.init(context) }
    val ready by MalangAds.ready.collectAsState()

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt().coerceAtLeast(1)
        val adSize = remember(widthDp) {
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(adSize.height.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (ready) {
                // AdView 의 크기는 한 번만 정할 수 있으므로 너비가 바뀌면 새로 만든다.
                key(widthDp) {
                    BannerAdView(adSize)
                }
            }
        }
    }
}

@Composable
private fun BannerAdView(adSize: AdSize) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val adViewHolder = remember { mutableStateOf<AdView?>(null) }

    AndroidView(
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(adSize)
                adUnitId = ctx.getString(R.string.admob_banner_home_id)
                loadAd(AdRequest.Builder().build())
            }.also { adViewHolder.value = it }
        },
        onRelease = { it.destroy() },
    )

    // 화면을 벗어나면 광고 새로고침을 멈춘다.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> adViewHolder.value?.pause()
                Lifecycle.Event.ON_RESUME -> adViewHolder.value?.resume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
}
