package com.ohadsam.specialevents.ui.help

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.domain.TextSearch

/** The whole user guide: every help topic as an expandable card, searchable. Opens with [initialTopic] expanded. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(initialTopic: HelpTopic?, onBack: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var expanded by rememberSaveable { mutableStateOf(initialTopic?.key ?: HelpTopic.START.key) }
    val texts = HelpTopic.entries.associateWith { stringResource(it.title) + " " + stringResource(it.body) }
    val q = TextSearch.normalize(query)
    val shown = remember(q, texts) { HelpTopic.entries.filter { q.isEmpty() || q.split(' ').all { w -> TextSearch.normalize(texts.getValue(it)).contains(w) } } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.guide_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) } },
            )
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                OutlinedTextField(query, { query = it }, label = { Text(stringResource(R.string.guide_search)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            if (shown.isEmpty()) item { Text(stringResource(R.string.guide_none), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(shown, key = { it.key }) { topic ->
                val open = q.isNotEmpty() || expanded == topic.key
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable { expanded = if (expanded == topic.key) "" else topic.key }.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(topic.title), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                            Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
                        }
                        AnimatedVisibility(open) {
                            Text(stringResource(topic.body), Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
