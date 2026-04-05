package org.openeljur.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.openeljur.app.R
import org.openeljur.app.data.*
import org.openeljur.app.viewmodel.MarksViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarksScreen(vm: MarksViewModel = viewModel()) {
    val lessons by vm.lessons.collectAsStateWithLifecycle()
    val studentName by vm.studentName.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val quarter by vm.quarter.collectAsStateWithLifecycle()
    var selectedMark by remember { mutableStateOf<Mark?>(null) }

    val quarters = listOf(
        1 to stringResource(R.string.marks_q1),
        2 to stringResource(R.string.marks_q2),
        3 to stringResource(R.string.marks_q3),
        4 to stringResource(R.string.marks_q4)
    )

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text(stringResource(R.string.marks_title))
                    if (studentName.isNotBlank())
                        Text(studentName, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            })
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ScrollableTabRow(selectedTabIndex = quarter - 1, edgePadding = 12.dp) {
                quarters.forEach { (q, label) ->
                    Tab(selected = quarter == q, onClick = { vm.setQuarter(q) }, text = { Text(label) })
                }
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    isLoading && lessons.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    error != null && lessons.isEmpty() -> Column(
                        Modifier.align(Alignment.Center).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { vm.load() }) { Text(stringResource(R.string.common_retry)) }
                    }
                    else -> LazyColumn(
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(lessons, key = { it.name ?: "" }) { lesson ->
                            SubjectCard(lesson, onMarkClick = { selectedMark = it })
                        }
                    }
                }
            }
        }
    }

    selectedMark?.let { mark ->
        MarkCommentDialog(mark = mark, onDismiss = { selectedMark = null })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectCard(lesson: LessonMarks, onMarkClick: (Mark) -> Unit) {
    val avg = lesson.average?.let {
        try { it.toString().trim('"').toDouble() } catch (e: Exception) { null }
    } ?: lesson.averageConvert?.toDouble()
    // Не показываем средний балл если 0 или null
    val showAvg = avg != null && avg > 0.0
    val avgLabel = stringResource(R.string.marks_average)
    val noMarksLabel = stringResource(R.string.marks_no_marks)

    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(lesson.name ?: "", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            avg?.takeIf { showAvg }?.let { v ->
                Surface(shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer) {
                    Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("$avgLabel ", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                        Text(String.format("%.1f", v), style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold, color = markColor(v.toString()))
                    }
                }
            }
        }
        HorizontalDivider()

        val marks = lesson.marks ?: emptyList()
        if (marks.isEmpty()) {
            Text(noMarksLabel, Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            FlowRow(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                marks.forEach { mark ->
                    val hasComment = !mark.comment.isNullOrBlank() || !mark.lesson_comment.isNullOrBlank()
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (hasComment) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(width = 52.dp, height = 52.dp)
                            .clickable(enabled = hasComment) { onMarkClick(mark) }
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(formatMarkDate(mark.date ?: "").take(5),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(mark.value ?: "-", style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold, color = markColor(mark.value))
                        }
                    }
                }
            }
        }
    }
}
