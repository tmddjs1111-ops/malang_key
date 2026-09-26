package dev.malangkey.app.settings.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.malangkey.app.apptheme.MalangCream
import dev.malangkey.app.apptheme.MalangDarkCard
import dev.malangkey.app.apptheme.MalangDarkCardTitle
import dev.malangkey.app.apptheme.MalangText
import dev.malangkey.app.settings.MalangJuaFont
import dev.malangkey.lib.compose.FlorisScreen

private data class TestMessage(val text: String, val mine: Boolean)

/**
 * 채팅창 모양의 키보드 테스트 화면. 들어오면 입력창에 포커스를 주어
 * 현재 설정된 말랑키 키보드가 바로 올라온다.
 */
@Composable
fun KeyboardTestScreen() = FlorisScreen {
    title = "키보드 테스트"
    previewFieldVisible = false
    scrollable = false

    content {
        val messages = remember {
            mutableStateListOf(
                TestMessage("안녕하세요! 말랑키 테스트 채팅방이에요 😊", mine = false),
                TestMessage("아래 입력창에 자유롭게 입력해 보세요.", mine = false),
            )
        }
        var input by remember { mutableStateOf("") }
        val listState = rememberLazyListState()
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
        LaunchedEffect(messages.size) {
            listState.animateScrollToItem(messages.lastIndex)
        }

        fun send() {
            if (input.isBlank()) return
            messages.add(TestMessage(input.trim(), mine = true))
            input = ""
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MalangCream)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(messages) { message -> ChatBubble(message) }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    if (input.isEmpty()) {
                        Text("메시지 입력", color = MalangText.copy(alpha = 0.4f), fontSize = 16.sp)
                    }
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        textStyle = TextStyle(color = MalangText, fontSize = 16.sp),
                        cursorBrush = SolidColor(MalangDarkCard),
                        maxLines = 4,
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MalangDarkCard)
                        .clickable { send() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "보내기",
                        tint = MalangCream,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: TestMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.mine) Arrangement.End else Arrangement.Start,
    ) {
        Text(
            text = message.text,
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (message.mine) MalangDarkCard else Color.White)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            color = if (message.mine) MalangDarkCardTitle else MalangText,
            fontSize = 15.sp,
            fontFamily = MalangJuaFont,
        )
    }
}
