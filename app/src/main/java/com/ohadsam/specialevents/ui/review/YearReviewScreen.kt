package com.ohadsam.specialevents.ui.review

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ohadsam.specialevents.R
import com.ohadsam.specialevents.domain.review.YearReview
import com.ohadsam.specialevents.greetings.SendChannel
import com.ohadsam.specialevents.ui.common.labelRes
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun YearReviewScreen(viewModel: YearReviewViewModel, onBack: () -> Unit) {
    val review by viewModel.review.collectAsStateWithLifecycle()
    val year by viewModel.selectedYear.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.review_title)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.action_back)) } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            YearSwitcher(year, onPrevious = viewModel::previous, onNext = viewModel::next)
            val r = review
            when {
                r == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                r.isEmpty -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.review_empty), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                }
                else -> ReviewBody(r)
            }
        }
    }
}

@Composable
private fun YearSwitcher(year: Int, onPrevious: () -> Unit, onNext: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        IconButton(onClick = onPrevious) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = stringResource(R.string.review_previous_year)) }
        Text(year.toString(), style = MaterialTheme.typography.headlineSmall)
        IconButton(onClick = onNext) { Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = stringResource(R.string.review_next_year)) }
    }
}

@Composable
private fun ReviewBody(r: YearReview) {
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(r.occurrences.toString(), stringResource(R.string.review_occurrences, r.occurrences))
        r.busiestMonth?.let { m ->
            StatCard(
                Month.of(m).getDisplayName(TextStyle.FULL_STANDALONE, Locale.getDefault()),
                stringResource(R.string.review_busiest_month, r.busiestMonthCount),
            )
        }
        if (r.byType.isNotEmpty()) {
            ListCard(stringResource(R.string.review_by_type), r.byType.map { (type, n) -> stringResource(type.labelRes()) to n.toString() })
        }
        StatCard(r.greetingsSent.toString(), stringResource(R.string.review_greetings, r.greetingsSent))
        r.coveragePercent?.let { StatCard("$it%", stringResource(R.string.review_coverage, r.greeted, r.passed)) }
        if (r.byChannel.isNotEmpty()) {
            ListCard(stringResource(R.string.review_by_channel), r.byChannel.map { (c, n) -> stringResource(channelRes(c)) to n.toString() })
        }
        if (r.topPeople.isNotEmpty()) {
            ListCard(stringResource(R.string.review_top_people), r.topPeople.map { it.name to it.count.toString() })
        }
        if (r.gifts.count > 0) {
            ListCard(
                stringResource(R.string.review_gifts),
                listOf(
                    stringResource(R.string.review_gifts_spent) to stringResource(R.string.review_amount, r.gifts.spent),
                    stringResource(R.string.review_gifts_planned) to stringResource(R.string.review_amount, r.gifts.budget),
                ),
            )
        }
    }
}

private fun channelRes(c: SendChannel): Int = when (c) {
    SendChannel.WHATSAPP -> R.string.greeting_send_whatsapp_short
    SendChannel.EMAIL -> R.string.greeting_send_email
    SendChannel.SMS -> R.string.greeting_send_sms
    SendChannel.SHARE -> R.string.greeting_send_other
}

@Composable
private fun StatCard(big: String, caption: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(big, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Text(caption, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ListCard(title: String, rows: List<Pair<String, String>>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                    Text(value, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
