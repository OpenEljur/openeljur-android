package org.openeljur.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.openeljur.app.R
import org.openeljur.app.ui.Screen
import org.openeljur.app.viewmodel.MessagesViewModel

private val URL_REGEX = Regex("""https?://[^\s<>"]+""")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageDetailScreen(messageId: String, navController: NavController, vm: MessagesViewModel = viewModel()) {
    val selectedMessage by vm.selectedMessage.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val message = selectedMessage?.takeIf { it.id == messageId }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.messages_message)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_cancel))
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.MessageCompose.go(messageId)) }) {
                        Icon(Icons.AutoMirrored.Filled.Reply, stringResource(R.string.messages_reply))
                    }
                }
            )
        }
    ) { padding ->
        if (message == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                SelectionContainer {
                    Text(message.subject ?: "...", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            item {
                Card {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        message.user_from?.let { user ->
                            LabelValue(stringResource(R.string.messages_from),
                                parseUserLabel(user.search, user.firstname, user.lastname))
                        }
                        message.date?.let { LabelValue(stringResource(R.string.messages_date), it.take(16)) }
                    }
                }
            }

            val files = (message.files ?: emptyList()) + (message.resources ?: emptyList())
            if (files.isNotEmpty()) {
                item { Text(stringResource(R.string.messages_files), style = MaterialTheme.typography.labelLarge) }
                items(files) { file ->
                    OutlinedCard(onClick = {
                        file.link?.let { link -> downloadFile(context, link, file.filename) }
                    }) {
                        Row(Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(file.filename ?: file.link ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f))
                            Icon(Icons.Default.Download, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item {
                val bodyText = stripHtml(message.text ?: message.short_text ?: "")
                LinkifyText(text = bodyText, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun LinkifyText(text: String, style: TextStyle = TextStyle.Default) {
    val uriHandler = LocalUriHandler.current
    val linkColor = MaterialTheme.colorScheme.primary

    val annotated = remember(text) {
        buildAnnotatedString {
            append(text)
            URL_REGEX.findAll(text).forEach { match ->
                addStyle(
                    style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
                    start = match.range.first,
                    end = match.range.last + 1
                )
                addStringAnnotation(
                    tag = "URL",
                    annotation = match.value,
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }
        }
    }

    SelectionContainer {
        ClickableText(
            text = annotated,
            style = style.copy(color = MaterialTheme.colorScheme.onSurface),
            onClick = { offset ->
                annotated.getStringAnnotations("URL", offset, offset)
                    .firstOrNull()?.let { uriHandler.openUri(it.item) }
            }
        )
    }
}

fun parseUserLabel(search: String?, first: String?, last: String?): String {
    if (!search.isNullOrBlank()) {
        val parts = search.split("~").map { it.trim() }.filter { it.isNotBlank() }
        if (parts.size >= 2) return "${parts[0]} ${parts[1]}"
        if (parts.isNotEmpty()) return parts[0]
    }
    return listOfNotNull(last, first).filter { it.isNotBlank() }.joinToString(" ").ifBlank { "-" }
}

fun stripHtml(html: String): String =
    html.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n\n")
        .replace(Regex("<[^>]+>"), "")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
