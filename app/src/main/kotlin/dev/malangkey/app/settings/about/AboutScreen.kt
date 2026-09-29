/*
 * Copyright (C) 2021-2025 The FlorisBoard Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.malangkey.app.settings.about

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.malangkey.BuildConfig
import dev.malangkey.R
import dev.malangkey.app.LocalNavController
import dev.malangkey.app.Routes
import dev.malangkey.app.apptheme.JuaFontFamily
import dev.malangkey.app.apptheme.MalangDarkCard
import dev.malangkey.app.apptheme.MalangNavRow
import dev.malangkey.app.apptheme.MalangSettingsScreen
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSettingsSummary
import dev.malangkey.clipboardManager
import dev.malangkey.lib.util.launchUrl
import org.florisboard.lib.compose.FlorisCanvasIcon

@Composable
fun AboutScreen() = MalangSettingsScreen(title = "앱 정보", subtitle = "About", showTestInput = false) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val clipboardManager by context.clipboardManager()

    val appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FlorisCanvasIcon(
            modifier = Modifier.requiredSize(72.dp),
            iconId = R.mipmap.malang_app_icon,
            contentDescription = "말랑키 아이콘",
        )
        Text(
            text = "말랑키",
            modifier = Modifier.padding(top = 12.dp),
            color = MalangDarkCard,
            fontSize = 28.sp,
            fontFamily = JuaFontFamily,
        )
        Text(text = "버전 $appVersion", color = MalangSettingsSummary, fontSize = 13.sp)
    }

    MalangSettingsSection(
        title = "앱",
        items = listOf(
            {
                MalangNavRow(title = "버전", summary = "$appVersion · 눌러서 복사") {
                    try {
                        clipboardManager.addNewPlaintext(appVersion)
                        Toast.makeText(context, "버전을 복사했어요.", Toast.LENGTH_SHORT).show()
                    } catch (e: Throwable) {
                        Toast.makeText(context, "복사하지 못했어요: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            {
                MalangNavRow(title = "개인정보처리방침") {
                    context.launchUrl(R.string.florisboard__privacy_policy_url)
                }
            },
        ),
    )

    MalangSettingsSection(
        title = "오픈소스",
        items = listOf(
            {
                MalangNavRow(
                    title = "FlorisBoard 기반",
                    summary = "말랑키는 FlorisBoard(Apache 2.0)를 수정해 만든 앱입니다.",
                ) {
                    context.launchUrl("https://github.com/florisboard/florisboard")
                }
            },
            {
                MalangNavRow(title = "말랑키 라이선스", summary = "Apache 2.0") {
                    navController.navigate(Routes.Settings.ProjectLicense)
                }
            },
            {
                MalangNavRow(title = "Mozc", summary = "일본어 입력 엔진 (BSD 3-Clause)") {
                    context.launchUrl(R.string.mozc__project_url)
                }
            },
            {
                MalangNavRow(title = "오픈소스 라이선스", summary = "앱에 쓰인 라이브러리와 글꼴") {
                    navController.navigate(Routes.Settings.ThirdPartyLicenses)
                }
            },
        ),
    )
}
