package org.openeljur.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.openeljur.app.R
import org.openeljur.app.data.*
import org.openeljur.app.viewmodel.DiaryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(vm: DiaryViewModel = viewModel()) {
    val days by vm.days.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val weekLabel by vm.weekLabel.collectAsStateWithLifecycle()
    var selectedMark by remember { mutableStateOf<Mark?>(null) }

    val pullState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.diary_title)) },
                actions = {
                    IconButton(onClick = { vm.prevWeek() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.diary_prev_week))
                    }
                    Text(weekLabel, style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { vm.nextWeek() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, stringResource(R.string.diary_next_week))
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { vm.load() },
            state = pullState,
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when {
                isLoading && days.isEmpty() -> Box(Modifier.fillMaxSize()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                }
                error != null && days.isEmpty() -> Box(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.align(Alignment.Center).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { vm.load() }) { Text(stringResource(R.string.common_retry)) }
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(days, key = { it.key }) { entry ->
                        DayCard(entry.key, entry.day, onMarkClick = { selectedMark = it })
                    }
                }
            }
        }
    }

    // Popover поверх всего — как в iOS
    selectedMark?.let { mark ->
        MarkCommentDialog(mark = mark, onDismiss = { selectedMark = null })
    }
}

@Composable
fun DayCard(dayKey: String, day: DiaryDay, onMarkClick: (Mark) -> Unit) {
    val isHoliday = (day.alert?.lowercase() in listOf("vacation", "holiday", "weekend"))
            || !day.holiday_name.isNullOrBlank()
    val holidayLabel = day.holiday_name?.takeIf { it.isNotBlank() }
        ?: when (day.alert?.lowercase()) {
            "vacation" -> stringResource(R.string.diary_vacation)
            "holiday", "weekend" -> stringResource(R.string.diary_weekend)
            else -> ""
        }
    val displayTitle = formatDayTitle(dayKey)
    val isToday = dayKey == SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
    val sortedLessons = (day.items ?: emptyMap()).entries.sortedBy { it.value.starttime ?: "" }

    // Карточка с тенью — как iOS .systemBackground + cornerRadius(12) + shadow
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            // Заголовок дня
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isHoliday) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    displayTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (isToday) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.common_today),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                if (holidayLabel.isNotBlank()) {
                    Text(
                        holidayLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Уроки
            if (isHoliday || sortedLessons.isEmpty()) {
                Text(
                    holidayLabel.ifBlank { stringResource(R.string.diary_no_lessons) },
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                sortedLessons.forEachIndexed { idx, (_, item) ->
                    LessonRow(item, onMarkClick)
                    if (idx < sortedLessons.size - 1) {
                        HorizontalDivider(Modifier.padding(start = 16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LessonRow(item: DiaryItem, onMarkClick: (Mark) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val homeworkText = item.homework?.values?.firstOrNull()?.value
    val isLongHomework = (homeworkText?.length ?: 0) > 80
    var expanded by remember { mutableStateOf(false) }
    val files = (item.files ?: emptyList()) + (item.resources ?: emptyList())
    val hwLabel = stringResource(R.string.diary_homework)
    val noHwLabel = stringResource(R.string.diary_no_homework)
    val showMoreLabel = stringResource(R.string.common_show_more)
    val showLessLabel = stringResource(R.string.common_show_less)

    // Время — две строки: начало и конец отдельно
    val startTime = item.starttime?.take(5)
    val endTime = item.endtime?.take(5)
    val hasTime = !startTime.isNullOrBlank() || !endTime.isNullOrBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Время — две строки как в iOS caption, фиксированная ширина 44dp
        Column(
            modifier = Modifier.width(44.dp).paddingFromBaseline(top = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            if (hasTime) {
                Text(
                    startTime ?: "--:--",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    endTime ?: "--:--",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "--:--",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Контент
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                item.name ?: "-",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )

            item.topic?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // ДЗ с expand/collapse
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "$hwLabel:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        homeworkText ?: noHwLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (expanded) Int.MAX_VALUE else 2
                    )
                }
                if (isLongHomework) {
                    Text(
                        if (expanded) showLessLabel else showMoreLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { expanded = !expanded }
                    )
                }
            }

            // Файлы — скачивание через DownloadManager
            files.forEach { file ->
                file.link?.let { link ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { downloadFile(context, link, file.filename) }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = fileIcon(file.filename),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            file.filename ?: link,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Оценки — как iOS: 34x30, цветной текст, dot если есть комментарий
        val assessments = item.assessments ?: emptyList()
        if (assessments.isNotEmpty()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.End
            ) {
                assessments.forEach { mark ->
                    val hasComment = !mark.comment.isNullOrBlank() || !mark.lesson_comment.isNullOrBlank()
                    Box(
                        modifier = Modifier
                            .size(width = 34.dp, height = 30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (hasComment) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .then(
                                if (hasComment) Modifier.clickable { onMarkClick(mark) }
                                else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            mark.value ?: "-",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = markColor(mark.value)
                        )
                        // Dot — как iOS: маленький кружок в правом верхнем углу
                        if (hasComment) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .align(Alignment.TopEnd)
                                    .offset(x = 2.dp, y = (-2).dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MarkCommentDialog(mark: Mark, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.marks_comment_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                mark.value?.takeIf { it.isNotBlank() }?.let {
                    LabelValue(stringResource(R.string.marks_mark_value), it)
                }
                mark.date?.takeIf { it.isNotBlank() }?.let {
                    LabelValue(stringResource(R.string.marks_mark_date), formatMarkDate(it))
                }
                mark.comment?.takeIf { it.isNotBlank() }?.let {
                    LabelValue(stringResource(R.string.marks_comment), it)
                }
                mark.lesson_comment?.takeIf { it.isNotBlank() }?.let {
                    LabelValue(stringResource(R.string.marks_lesson_comment), it)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) }
        }
    )
}

@Composable
fun LabelValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun markColor(value: String?): Color {
    val n = value?.replace(",", ".")?.toDoubleOrNull()
        ?: return MaterialTheme.colorScheme.onSurfaceVariant
    return when {
        n >= 4.5 -> Color(0xFF4CAF50)   // тёмно-зелёный
        n >= 3.5 -> Color(0xFF8CC83D)   // светло-зелёный
        n >= 3.0 -> Color(0xFFE68A24)   // жёлтый
        else     -> Color(0xFFFF5940)   // красный
    }
}

@Composable
fun fileIcon(filename: String?) = when {
    filename?.lowercase()?.let { it.endsWith(".jpg") || it.endsWith(".jpeg") || it.endsWith(".png") } == true ->
        Icons.Default.Photo
    filename?.lowercase()?.endsWith(".pdf") == true ->
        Icons.Default.PictureAsPdf
    else -> Icons.Default.AttachFile
}

fun formatDayTitle(key: String): String = try {
    val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).parse(key) ?: return key
    val fmt = SimpleDateFormat("EEEE, dd.MM", Locale.getDefault())
    fmt.format(date).replaceFirstChar { it.uppercase() }
} catch (e: Exception) { key }

fun formatMarkDate(raw: String): String {
    val parts = raw.split("-")
    return if (parts.size >= 3) "${parts[2]}.${parts[1]}.${parts[0]}" else raw
}

fun downloadFile(context: android.content.Context, url: String, filename: String?) {
    try {
        val name = filename?.takeIf { it.isNotBlank() } ?: url.substringAfterLast("/").ifBlank { "file" }
        val request = android.app.DownloadManager.Request(android.net.Uri.parse(url))
            .setTitle(name)
            .setDescription(name)
            .setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, name)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
        val dm = context.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
        dm.enqueue(request)
    } catch (e: Exception) {
        // Fallback — открыть в браузере если DownloadManager недоступен
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
