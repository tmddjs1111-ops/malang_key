package dev.patrickgold.florisboard.app.settings.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.patrickgold.florisboard.app.FlorisPreferenceStore
import dev.patrickgold.florisboard.lib.compose.FlorisScreen
import dev.patrickgold.jetpref.datastore.model.collectAsState
import dev.patrickgold.jetpref.datastore.ui.DialogSliderPreference
import dev.patrickgold.jetpref.datastore.ui.ExperimentalJetPrefDatastoreUi
import dev.patrickgold.jetpref.datastore.ui.SwitchPreference

@OptIn(ExperimentalJetPrefDatastoreUi::class)
@Composable
fun AdvancedThemeScreen() = FlorisScreen {
    title = "고급 테마 설정"
    previewFieldVisible = true

    val prefs by FlorisPreferenceStore

    content {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(32.dp))
                    .clip(RoundedCornerShape(32.dp))
                    .background(Color.White)
                    .padding(vertical = 16.dp)
            ) {
                Box(modifier = Modifier.padding(vertical = 4.dp)) {
                    SwitchPreference(
                        prefs.malang.isGlassmorphismEnabled,
                        title = "글래스모피즘 (투명 키보드)",
                        summary = "배경이 은은하게 비치는 투명한 디자인을 적용합니다."
                    )
                }
                
                if (prefs.malang.isGlassmorphismEnabled.collectAsState().value) {
                    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                        DialogSliderPreference(
                            prefs.malang.glassmorphismTransparency,
                            title = "투명도 조절",
                            valueLabel = { "${(it * 100).toInt()}%" },
                            min = 0.1f,
                            max = 1.0f,
                            stepIncrement = 0.05f,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().padding(horizontal = 24.dp).background(Color(0xFFF0EBE1)))

                Box(modifier = Modifier.padding(vertical = 4.dp)) {
                    SwitchPreference(
                        prefs.malang.isNeumorphismEnabled,
                        title = "뉴모피즘 (입체감)",
                        summary = "볼록하게 튀어나온 폭신한 키 모양을 적용합니다."
                    )
                }

                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().padding(horizontal = 24.dp).background(Color(0xFFF0EBE1)))

                Box(modifier = Modifier.padding(vertical = 4.dp)) {
                    SwitchPreference(
                        prefs.malang.squircleShapeEnabled,
                        title = "스쿼클 쉐이프 (애플 스타일 둥근 키)",
                        summary = "일반적인 둥근 사각형보다 훨씬 부드러운 곡률을 적용합니다."
                    )
                }

                Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().padding(horizontal = 24.dp).background(Color(0xFFF0EBE1)))

                Box(modifier = Modifier.padding(vertical = 4.dp)) {
                    SwitchPreference(
                        prefs.malang.malangSoundEnabled,
                        title = "감각적인 말랑 사운드",
                        summary = "기본 타건음 대신 말랑 키보드만의 통통 튀는 타건음을 적용합니다."
                    )
                }
            }
        }
    }
}
