package com.kiraya.diary.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kiraya.diary.R

/** Scrollable form body with a save button at the end. Shared by edit screens. */
@Composable
fun FormScaffold(
    title: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
    saveLabel: String = stringResource(R.string.action_save),
    content: @Composable () -> Unit,
) {
    Scaffold(
        modifier = modifier,
        topBar = { KirayaTopBar(title = title, onBack = onBack) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            content()
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text(saveLabel)
            }
        }
    }
}
