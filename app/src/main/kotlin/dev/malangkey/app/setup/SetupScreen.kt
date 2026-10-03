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

package dev.malangkey.app.setup

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.malangkey.R
import dev.malangkey.app.FlorisAppActivity
import dev.malangkey.app.LocalNavController
import dev.malangkey.app.Routes
import dev.malangkey.app.apptheme.JuaFontFamily
import dev.malangkey.app.apptheme.MalangButton
import dev.malangkey.app.apptheme.MalangDarkCard
import dev.malangkey.app.apptheme.MalangSettingsBg
import dev.malangkey.app.apptheme.MalangSettingsBorder
import dev.malangkey.app.apptheme.MalangSettingsCard
import dev.malangkey.app.apptheme.MalangSettingsSection
import dev.malangkey.app.apptheme.MalangSettingsSummary
import dev.malangkey.app.apptheme.MalangSettingsTitle
import dev.malangkey.lib.compose.FlorisScreen
import dev.malangkey.lib.util.InputMethodUtils
import dev.malangkey.lib.util.launchActivity
import dev.malangkey.lib.util.launchUrl
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class StepState { DONE, CURRENT, WAITING }

/**
 * 처음 실행 화면: 말랑키를 켜고 고르는 두 단계를 보여주고, 끝나면 간단 설정으로 이어진다.
 */
@Composable
fun SetupScreen() = FlorisScreen {
    title = ""
    topBarVisible = false
    navigationIconVisible = false
    previewFieldVisible = false
    scrollable = false

    content {
        val navController = LocalNavController.current
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        val isEnabled by InputMethodUtils.observeIsFlorisboardEnabled(foregroundOnly = true)
        val isSelected by InputMethodUtils.observeIsFlorisboardSelected(foregroundOnly = true)
        val isReady = isEnabled && isSelected

        // 시스템 설정에서 말랑키를 켜는 순간 이 화면으로 돌아오게 한다.
        LaunchedEffect(Unit) {
            while (true) {
                delay(200L)
                if (!isEnabled && InputMethodUtils.isFlorisboardEnabled(context)) {
                    context.launchActivity(FlorisAppActivity::class) {
                        it.flags = (Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                            or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                }
            }
        }

        val finish: () -> Unit = {
            scope.launch { prefs.internal.isImeSetUp.set(true) }
            navController.navigate(Routes.Settings.Home) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MalangSettingsBg)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Image(
                    painter = painterResource(R.drawable.mk_logo_mark),
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                )
                Text(
                    "말랑키에 오신 걸 환영해요",
                    color = MalangDarkCard,
                    fontSize = 28.sp,
                    lineHeight = 34.sp,
                    fontFamily = JuaFontFamily,
                    textAlign = TextAlign.Center,
                )
                Text(
                    if (isReady) "준비가 끝났어요!" else "두 단계만 거치면 바로 쓸 수 있어요.",
                    color = MalangSettingsSummary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))

                SetupStepCard(
                    number = 1,
                    title = "말랑키 켜기",
                    description = "휴대폰 설정의 키보드 목록에서 '말랑키'를 켜 주세요. 켜면 자동으로 이 화면으로 돌아와요.",
                    state = if (isEnabled) StepState.DONE else StepState.CURRENT,
                    buttonLabel = "설정 열기",
                    onClick = { InputMethodUtils.showImeEnablerActivity(context) },
                )
                SetupStepCard(
                    number = 2,
                    title = "말랑키 고르기",
                    description = "키보드 선택 창에서 '말랑키'를 골라 주세요.",
                    state = when {
                        isSelected -> StepState.DONE
                        isEnabled -> StepState.CURRENT
                        else -> StepState.WAITING
                    },
                    buttonLabel = "키보드 고르기",
                    onClick = { InputMethodUtils.showImePicker(context) },
                )

                if (isReady) {
                    Text(
                        "자판, 소리, 스마트 바, 테마를 1분 안에 골라볼까요?\n귀찮으면 건너뛰고 기본값으로 바로 써도 괜찮아요.",
                        modifier = Modifier.padding(top = 8.dp),
                        color = MalangSettingsSummary,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isReady) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MalangButton("건너뛰기", modifier = Modifier.weight(1f), primary = false, onClick = finish)
                    MalangButton("간단 설정 시작", modifier = Modifier.weight(2f)) {
                        navController.navigate(Routes.Setup.Quick)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FooterLink("개인정보처리방침") { context.launchUrl(R.string.florisboard__privacy_policy_url) }
                Text("·", color = MalangSettingsSummary, fontSize = 13.sp)
                FooterLink("앱 정보") { navController.navigate(Routes.Settings.About) }
            }
        }
    }
}

@Composable
private fun SetupStepCard(
    number: Int,
    title: String,
    description: String,
    state: StepState,
    buttonLabel: String,
    onClick: () -> Unit,
) {
    val current = state == StepState.CURRENT
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (state == StepState.WAITING) 0.5f else 1f)
            .clip(RoundedCornerShape(24.dp))
            .background(MalangSettingsCard)
            .border(
                width = if (current) 2.dp else 1.dp,
                color = if (current) MalangSettingsSection else MalangSettingsBorder,
                shape = RoundedCornerShape(24.dp),
            )
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (state == StepState.WAITING) MalangSettingsBorder else MalangSettingsSection),
                contentAlignment = Alignment.Center,
            ) {
                if (state == StepState.DONE) {
                    Icon(Icons.Default.Check, contentDescription = "완료", tint = MalangSettingsCard, modifier = Modifier.size(20.dp))
                } else {
                    Text("$number", color = MalangSettingsCard, fontSize = 17.sp, fontFamily = JuaFontFamily)
                }
            }
            Text(
                title,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                color = MalangSettingsTitle,
                fontSize = 20.sp,
                fontFamily = JuaFontFamily,
            )
            if (state == StepState.DONE) {
                Text("완료", color = MalangSettingsSection, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (state != StepState.DONE) {
            Text(description, color = MalangSettingsSummary, fontSize = 14.sp, lineHeight = 21.sp)
        }
        if (current) {
            MalangButton(buttonLabel, modifier = Modifier.fillMaxWidth(), onClick = onClick)
        }
    }
}

@Composable
private fun FooterLink(label: String, onClick: () -> Unit) {
    Text(
        label,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        color = MalangSettingsSummary,
        fontSize = 13.sp,
    )
}
