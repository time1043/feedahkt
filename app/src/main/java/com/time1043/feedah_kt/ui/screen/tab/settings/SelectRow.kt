package com.time1043.feedah_kt.ui.screen.tab.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.time1043.feedah_kt.R

data class SelectOption(
    val value: String,
    val label: String,
)

/**
 * One labeled select row in a settings group (mirror of SelectRow in
 * src/components/settings/select-row.tsx): the label on the left, the
 * current value behind a chevron on the right, and the options in a modal
 * dropdown. The modal pattern keeps the settings page free of expanding
 * layout shifts; the backdrop tap closes it.
 */
@Composable
fun SelectRow(
    label: String,
    /** The currently selected option's value. */
    value: String,
    options: List<SelectOption>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val current = options.find { it.value == value }?.label ?: value

    Column(modifier = modifier) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button) { open = true }
                .padding(vertical = 8.dp) // py-2
                .semantics { contentDescription = "$label, $current" },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = label, color = MaterialTheme.colorScheme.onSurface)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp), // gap-1.5
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = current, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(
                    painter = painterResource(R.drawable.ic_chevron_down),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        if (open) {
            Dialog(onDismissRequest = { open = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp), // rounded-2xl
                    color = MaterialTheme.colorScheme.surface, // bg-white / bg-gray-900
                ) {
                    Column(Modifier.padding(8.dp)) { // p-2
                        for (option in options) {
                            val isSelected = option.value == value
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.RadioButton) {
                                        onValueChange(option.value)
                                        open = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp) // px-3 py-3
                                    .semantics { selected = isSelected },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = option.label,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else null,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.primary // text-blue-500
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                )
                                if (isSelected) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_check),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary, // ACCENT
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
