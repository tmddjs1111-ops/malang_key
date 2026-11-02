package dev.malangkey.ime.editor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 입력칸 진단 기록. 보안 입력칸(PASS 등)에서 한글 조합이 왜 안 되는지 보려고, 키보드가 입력칸에서
 * 본 것(입력칸 종류, 앞 글자를 읽을 수 있었는지, 조합 범위)을 앱 안에만 남긴다. 설정에서 켰을 때만 기록한다.
 */
object InputDiagnostics {
    private const val MaxLines = 300
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    @Volatile
    var enabled: Boolean = false

    private val _lines = MutableStateFlow<List<String>>(emptyList())
    val lines: StateFlow<List<String>> = _lines.asStateFlow()

    fun log(message: () -> String) {
        if (!enabled) return
        val line = "${timeFormat.format(Date())} ${message()}"
        synchronized(this) {
            _lines.value = (_lines.value + line).takeLast(MaxLines)
        }
    }

    fun clear() {
        synchronized(this) { _lines.value = emptyList() }
    }
}
