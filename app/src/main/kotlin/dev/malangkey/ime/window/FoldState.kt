package dev.malangkey.ime.window

import android.inputmethodservice.InputMethodService
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * 폴더블 폰이 펼쳐져 있는지 알려준다.
 *
 * 시스템이 접힘 상태가 바뀔 때만 알려주므로(이벤트 방식), 키를 누를 때마다 확인하는 일은 없다.
 * 펼친 화면에만 접는 선(FoldingFeature)이 보고되므로, 그것이 있으면 펼친 상태로 본다.
 */
object FoldState {
    private val _isUnfolded = MutableStateFlow(false)
    val isUnfolded: StateFlow<Boolean> = _isUnfolded.asStateFlow()

    fun observe(service: InputMethodService, scope: CoroutineScope) {
        scope.launch {
            runCatching {
                WindowInfoTracker.getOrCreate(service)
                    .windowLayoutInfo(service)
                    // 접힘 정보를 줄 수 없는 기기나 버전에서는 펼치지 않은 것으로 둔다.
                    .catch { _isUnfolded.value = false }
                    .collect { info ->
                        _isUnfolded.value = info.displayFeatures.any { it is FoldingFeature }
                    }
            }
        }
    }
}
