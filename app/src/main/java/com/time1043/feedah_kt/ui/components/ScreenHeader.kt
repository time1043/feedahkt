package com.time1043.feedah_kt.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.time1043.feedah_kt.R

/**
 * Icon button for screen headers (mirror of HeaderIconButton in
 * src/components/header.tsx): one size (22dp in a 36dp hit box) and one
 * muted color everywhere, so icons never jitter between pages.
 */
@Composable
fun HeaderIconButton(
    iconRes: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clickable(onClick = onClick)
            .padding(4.dp), // hitSlop 4
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
}

/**
 * The header row of the full-screen mode pages (mirror of ScreenHeader in
 * src/components/header.tsx): back on the left, the title centered (passed
 * in by the screen), search on the right.
 */
@Composable
fun ScreenHeader(
    /** The centered title; omit it for an empty center (the row keeps its
     * height either way). */
    title: String?,
    onBack: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderIconButton(
            iconRes = R.drawable.ic_chevron_back,
            contentDescription = "Back",
            onClick = onBack,
        )
        if (title != null) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        } else {
            Box(Modifier.weight(1f))
        }
        HeaderIconButton(
            iconRes = R.drawable.ic_search,
            contentDescription = "Search words",
            onClick = onSearch,
        )
    }
}

/**
 * The header row of the list pages (word list, round list; mirror of
 * ListHeader in src/components/header.tsx): back on the left, the title
 * centered, and a free right slot — the caller decides what lives there
 * (the word list's eye toggle, …). Kept separate from ScreenHeader so
 * neither accumulates the other's judgments.
 */
@Composable
fun ListHeader(
    /** The centered title; omit it for an empty center (the row keeps its
     * height either way). */
    title: String?,
    onBack: () -> Unit,
    /** The free right slot, rendered in the same 36dp box as an icon
     * button. */
    right: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderIconButton(
            iconRes = R.drawable.ic_chevron_back,
            contentDescription = "Back",
            onClick = onBack,
        )
        if (title != null) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        } else {
            Box(Modifier.weight(1f))
        }
        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center,
        ) {
            right?.invoke()
        }
    }
}

/** Header row of the four tabs (mirror of TabHeader in
 * src/components/header.tsx): 22sp bold title left, optional right slot.
 * The title is optional — an untitled tab renders a spacer so its right
 * slot still lands on the right edge (the home header carries only its
 * search icon). */
@Composable
fun TabHeader(
    title: String?,
    right: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp), // px-4 pb-2 pt-3
        horizontalArrangement = Arrangement.SpaceBetween, // justify-between
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (title != null) {
            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        } else {
            Box(Modifier.weight(1f))
        }
        right?.invoke()
    }
}
