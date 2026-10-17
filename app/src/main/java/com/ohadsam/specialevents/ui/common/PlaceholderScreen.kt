package com.ohadsam.specialevents.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.ui.theme.SpecialEventsTheme

/** Friendly empty state used by screens whose feature lands in a later batch. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceholderScreen(@StringRes title: Int, @StringRes headline: Int, @StringRes body: Int) {
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(title)) }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Outlined.Cake,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            Text(stringResource(headline), style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(
                stringResource(body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderLightPreview() {
    SpecialEventsTheme(darkTheme = false) {
        PlaceholderScreen(R.string.tab_upcoming, R.string.upcoming_empty_title, R.string.upcoming_empty_body)
    }
}

@Preview(showBackground = true)
@Composable
private fun PlaceholderDarkPreview() {
    SpecialEventsTheme(darkTheme = true) {
        PlaceholderScreen(R.string.tab_upcoming, R.string.upcoming_empty_title, R.string.upcoming_empty_body)
    }
}
