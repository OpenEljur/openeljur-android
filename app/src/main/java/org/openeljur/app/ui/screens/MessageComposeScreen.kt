package org.openeljur.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.openeljur.app.R
import org.openeljur.app.data.*
import org.openeljur.app.data.apiPost
import org.openeljur.app.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageComposeScreen(replyToId: String?, navController: NavController) {
    val authVm: AuthViewModel = viewModel()
    val token by authVm.token.collectAsStateWithLifecycle()
    val schoolId by authVm.schoolId.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var sent by remember { mutableStateOf(false) }

    // Получатели
    var selectedRecipients by remember { mutableStateOf<List<ReceiverUser>>(emptyList()) }
    var showRecipientPicker by remember { mutableStateOf(false) }

    // Файлы
    var attachedFiles by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> attachedFiles = attachedFiles + uris }

    LaunchedEffect(sent) { if (sent) navController.popBackStack() }

    // Формируем строку получателей (search через ~)
    val usersToStr = selectedRecipients.joinToString(",") { it.search ?: it.name ?: "" }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.messages_compose)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.common_cancel))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isSending = true; error = null
                                val result = apiPost<MessageSendRequest, Envelope<MessageSendData>>(
                                    "/v1/messages/send",
                                    MessageSendRequest(token, schoolId.ifBlank { null }, subject, body, usersToStr)
                                )
                                isSending = false
                                result.fold(
                                    onSuccess = { if (it.ok) sent = true else error = it.error?.message },
                                    onFailure = { error = it.message }
                                )
                            }
                        },
                        enabled = !isSending && selectedRecipients.isNotEmpty() && subject.isNotBlank() && body.isNotBlank()
                    ) {
                        if (isSending) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.AutoMirrored.Filled.Send, stringResource(R.string.messages_send))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Получатели
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showRecipientPicker = true }
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(
                        stringResource(R.string.messages_recipients),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (selectedRecipients.isEmpty()) {
                        Text(
                            stringResource(R.string.messages_add_recipient),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        selectedRecipients.forEach { user ->
                            val name = formatRecipientName(user)
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(name, style = MaterialTheme.typography.bodyMedium)
                                IconButton(
                                    onClick = { selectedRecipients = selectedRecipients - user },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        TextButton(
                            onClick = { showRecipientPicker = true },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.messages_add_recipient),
                                style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            OutlinedTextField(
                value = subject, onValueChange = { subject = it },
                label = { Text(stringResource(R.string.messages_subject)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = body, onValueChange = { body = it },
                label = { Text(stringResource(R.string.messages_body)) },
                modifier = Modifier.fillMaxWidth().weight(1f),
                minLines = 4
            )

            // Прикреплённые файлы
            if (attachedFiles.isNotEmpty()) {
                attachedFiles.forEach { uri ->
                    val name = uri.lastPathSegment ?: uri.toString()
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AttachFile, null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(4.dp))
                        Text(name, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { attachedFiles = attachedFiles - uri },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Кнопка прикрепить файл
            TextButton(
                onClick = { filePicker.launch("*/*") },
                contentPadding = PaddingValues(0.dp)
            ) {
                Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.messages_add_attachment))
            }

            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showRecipientPicker) {
        RecipientPickerSheet(
            token = token,
            schoolId = schoolId,
            alreadySelected = selectedRecipients,
            onSelect = { user ->
                if (selectedRecipients.none { it.search == user.search }) {
                    selectedRecipients = selectedRecipients + user
                }
                showRecipientPicker = false
            },
            onDismiss = { showRecipientPicker = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipientPickerSheet(
    token: String,
    schoolId: String,
    alreadySelected: List<ReceiverUser>,
    onSelect: (ReceiverUser) -> Unit,
    onDismiss: () -> Unit
) {
    var groups by remember { mutableStateOf<List<ReceiverGroup>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val result = apiPost<ReceiversRequest, Envelope<ReceiversData>>(
            "/v1/messages/receivers",
            ReceiversRequest(token, schoolId.ifBlank { null })
        )
        groups = result.getOrNull()?.data?.groups ?: emptyList()
        isLoading = false
    }

    // Плоский список всех пользователей
    val allUsers = remember(groups) {
        fun flatten(g: ReceiverGroup): List<Pair<String, ReceiverUser>> {
            val users = (g.users ?: emptyList()).map { (g.name ?: "") to it }
            val sub = (g.subgroups ?: emptyList()).flatMap { flatten(it) }
            return users + sub
        }
        groups.flatMap { flatten(it) }
    }

    val filtered = remember(allUsers, query) {
        if (query.isBlank()) allUsers
        else allUsers.filter { (_, u) ->
            val name = formatRecipientName(u)
            name.contains(query, ignoreCase = true)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                stringResource(R.string.messages_recipients),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                label = { Text(stringResource(R.string.common_search)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (query.isNotBlank()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Default.Clear, null)
                    }
                }
            )
            Spacer(Modifier.height(8.dp))

            if (isLoading) {
                Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Группируем по категории
                    val grouped = filtered.groupBy { it.first }
                    grouped.forEach { (groupName, users) ->
                        if (groupName.isNotBlank()) {
                            item {
                                Text(
                                    groupName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 6.dp)
                                )
                            }
                        }
                        items(users) { (_, user) ->
                            val name = formatRecipientName(user)
                            val isSelected = alreadySelected.any { it.search == user.search }
                            ListItem(
                                headlineContent = { Text(name) },
                                supportingContent = user.info?.takeIf { it.isNotBlank() }?.let {
                                    { Text(it, style = MaterialTheme.typography.bodySmall) }
                                },
                                trailingContent = if (isSelected) ({
                                    Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                }) else null,
                                modifier = Modifier.clickable { onSelect(user) }
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

fun formatRecipientName(user: ReceiverUser): String {
    val search = user.search
    if (!search.isNullOrBlank()) {
        val parts = search.split("~").map { it.trim() }.filter { it.isNotBlank() }
        if (parts.size >= 2) return "${parts[0]} ${parts[1]}"
        if (parts.isNotEmpty()) return parts[0]
    }
    return user.name ?: "-"
}
