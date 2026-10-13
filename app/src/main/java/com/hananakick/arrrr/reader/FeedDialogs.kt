package com.hananakick.arrrr.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun AddFeedDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, url: String, topic: Topic) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf(Topic.TECH) }
    val canSubmit = name.isNotBlank() &&
        (url.trim().startsWith("https://", ignoreCase = true) ||
            url.trim().startsWith("http://", ignoreCase = true))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("새 피드 추가") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("관심 있는 사이트의 RSS 주소를 등록하세요.")
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("이름") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("RSS 주소") },
                    placeholder = { Text("https://example.org/feed.xml") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Text("카테고리")
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Topic.TECH, Topic.DESIGN).forEach { item ->
                            FilterChip(
                                selected = topic == item,
                                onClick = { topic = item },
                                label = { Text(item.label) },
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(Topic.PRODUCT, Topic.LIFE).forEach { item ->
                            FilterChip(
                                selected = topic == item,
                                onClick = { topic = item },
                                label = { Text(item.label) },
                            )
                        }
                    }
                }
                Text("미리보기에서는 주소만 등록되며 글을 가져오지 않습니다.")
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), url.trim(), topic) },
                enabled = canSubmit,
            ) { Text("추가") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소") }
        },
    )
}
