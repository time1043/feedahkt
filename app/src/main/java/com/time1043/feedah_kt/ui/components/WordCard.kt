package com.time1043.feedah_kt.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.types.Word

/** The word card, first pass: a single centered column (source 3a852dd). */
@Composable
fun WordCard(word: Word, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = word.position.toString(),
            style = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BasicText(
            text = word.word,
            style = TextStyle(
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 24.sp,
                maxFontSize = 48.sp,
                stepSize = 1.sp,
            ),
            modifier = Modifier.padding(top = 16.dp),
        )
        if (word.ipa.isNotEmpty()) {
            Text(
                text = word.ipa,
                style = TextStyle(fontSize = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (word.meaning.isNotEmpty()) {
            Text(
                text = word.meaning,
                style = TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 24.dp),
            )
        }
        if (word.forms.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().padding(top = 32.dp)) {
                word.forms.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { form ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                            ) {
                                Text(
                                    text = form,
                                    style = TextStyle(fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
