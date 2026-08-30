package dev.malangkey.ime.text.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.malangkey.editorInstance
import dev.malangkey.ime.nlp.StyleAndEmoticonProvider
import dev.malangkey.keyboardManager

@Composable
fun EmoticonSearchPanel(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val editorInstance by context.editorInstance()
    
    val activeContent by editorInstance.activeContentFlow.collectAsState()
    
    val currentWord = remember(activeContent) {
        val textBefore = activeContent.textBeforeSelection.toString()
        textBefore.takeLast(20).takeLastWhile { !it.isWhitespace() }
    }
    
    val candidates = remember(currentWord) {
        StyleAndEmoticonProvider.generateCandidates(currentWord)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .background(Color(0xFFFCF5D6)) // Malang Key default bg
    ) {
        // Panel Header with Title and Close Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (currentWord.isEmpty()) "이모티콘 & 인스타체 추천" else "'$currentWord' 변환 결과",
                color = Color(0xFF887766),
                fontSize = 14.sp
            )
            IconButton(
                onClick = { keyboardManager.isEmoticonSearchVisible.value = false },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "닫기",
                    tint = Color(0xFF311D18)
                )
            }
        }

        // Candidate List (Fills the entire Emoticon Window)
        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            items(candidates) { candidate ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (currentWord.isNotEmpty()) {
                                editorInstance.setSelectionSurrounding(
                                    currentWord.length,
                                    dev.malangkey.ime.editor.OperationUnit.CHARACTERS,
                                    dev.malangkey.ime.editor.OperationScope.BEFORE_CURSOR
                                )
                            }
                            editorInstance.commitText(candidate.text.toString())
                            keyboardManager.isEmoticonSearchVisible.value = false
                        }
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = candidate.text.toString(),
                        fontSize = 18.sp,
                        color = Color(0xFF311D18),
                        modifier = Modifier.weight(1f)
                    )
                    if (candidate.secondaryText != null) {
                        Text(
                            text = candidate.secondaryText.toString(),
                            fontSize = 12.sp,
                            color = Color(0xFF887766),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}
