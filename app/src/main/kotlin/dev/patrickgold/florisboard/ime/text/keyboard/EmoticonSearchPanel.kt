package dev.patrickgold.florisboard.ime.text.keyboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.patrickgold.florisboard.editorInstance
import dev.patrickgold.florisboard.ime.nlp.StyleAndEmoticonProvider
import dev.patrickgold.florisboard.keyboardManager

@Composable
fun EmoticonSearchPanel(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val keyboardManager by context.keyboardManager()
    val editorInstance by context.editorInstance()
    
    val activeContent by editorInstance.activeContentFlow.collectAsState()
    
    val currentWord = remember(activeContent) {
        val textBefore = activeContent.getTextBeforeCursor(20)
        textBefore.takeLastWhile { !it.isWhitespace() }.toString()
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
        // Fake Search Bar Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fake TextField
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(Color.White, RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFFE0D8B0), RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                if (currentWord.isEmpty()) {
                    Text("단어를 입력해보세요 (예: 안녕)", color = Color.Gray, fontSize = 16.sp)
                } else {
                    Text(currentWord, color = Color.Black, fontSize = 16.sp)
                }
            }
            
            IconButton(
                onClick = { keyboardManager.isEmoticonSearchVisible.value = false },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "닫기",
                    tint = Color(0xFF311D18)
                )
            }
        }
        
        // Candidate List
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
                                    dev.patrickgold.florisboard.ime.editor.OperationUnit.CHARACTERS,
                                    dev.patrickgold.florisboard.ime.editor.OperationScope.BEFORE_CURSOR
                                )
                            }
                            editorInstance.commitText(candidate.text.toString())
                            keyboardManager.isEmoticonSearchVisible.value = false
                        }
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = candidate.text.toString(),
                        fontSize = 18.sp,
                        color = Color(0xFF311D18)
                    )
                }
            }
        }
    }
}
