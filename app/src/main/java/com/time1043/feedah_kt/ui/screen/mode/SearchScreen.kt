package com.time1043.feedah_kt.ui.screen.mode

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

/** The screen's event vocabulary; grows as the screen grows. */
sealed interface SearchEvent {
    data class OpenWord(val position: Int) : SearchEvent
}

@Composable
fun SearchScreen(
    onEvent: (SearchEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The search box takes the CARD NUMBER (the data position printed on
    // the card); default it to the bucket's first card so the first press
    // works (mirror of f18e173's entry default; a real search replaces
    // this stub).
    var position by rememberSaveable { mutableStateOf("1701") }
    val parsed = position.toIntOrNull()

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = "Search page")
        OutlinedTextField(
            value = position,
            onValueChange = { position = it.filter { c -> c.isDigit() } },
            label = { Text("Word position") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.testTag("search.position"),
        )
        Button(
            onClick = { parsed?.let { onEvent(SearchEvent.OpenWord(it)) } },
            enabled = parsed != null,
        ) {
            Text(text = parsed?.let { "Open word #$it" } ?: "Open word")
        }
    }
}
