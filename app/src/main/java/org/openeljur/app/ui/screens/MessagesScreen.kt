package org.openeljur.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import org.openeljur.app.R
import org.openeljur.app.ui.Screen
import org.openeljur.app.viewmodel.MessagesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(navController: NavController, vm: MessagesViewModel = viewModel()) {
    val messages by vm.messages.collectAsStateWithLifecycle()
    val isLoading by vm.isLoading.collectAsStateWithLifecycle()
    val isLoadingMore by vm.isLoadingMore.collectAsStateWithLifecycle()
    val hasMore by vm.hasMore.collectAsStateWithLifecycle()
    val error by vm.error.collectAsStateWithLifecycle()
    val folder by vm.folder.collectAsStateWithLifecycle()
    val unreadOnly by vm.unreadOnly.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.messages_title)) },
                actions = {
                    FilterChip(
                        selected = unreadOnly,
                        onClick = { vm.setUnreadOnly(!unreadOnly) },
                        label = { Text(stringResource(R.string.messages_unread_only)) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate(Screen.MessageCompose.go()) }) {
                Icon(Icons.Default.Edit, stringResource(R.string.messages_compose))
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = if (folder == "inbox") 0 else 1) {
                Tab(selected = folder == "inbox", onClick = { vm.setFolder("inbox") },
                    text = { Text(stringResource(R.string.messages_inbox)) })
                Tab(selected = folder == "sent", onClick = { vm.setFolder("sent") },
                    text = { Text(stringResource(R.string.messages_sent)) })
            }

            Box(Modifier.fillMaxSize()) {
                when {
                    isLoading && messages.isEmpty() -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    error != null && messages.isEmpty() -> Column(
                        Modifier.align(Alignment.Center).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { vm.load() }) { Text(stringResource(R.string.common_retry)) }
                    }
                    messages.isEmpty() -> Text(stringResource(R.string.messages_no_messages),
                        Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> LazyColumn {
                        items(messages, key = { it.id ?: "" }) { msg ->
                            ListItem(
                                headlineContent = {
                                    Text(msg.subject ?: "...",
                                        fontWeight = if (msg.unread == true) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 2)
                                },
                                supportingContent = {
                                    Text(msg.short_text ?: msg.text ?: "",
                                        maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                },
                                trailingContent = {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(msg.date?.take(10) ?: "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (msg.unread == true) {
                                            Spacer(Modifier.height(4.dp))
                                            Badge {}
                                        }
                                    }
                                },
                                modifier = Modifier.clickable {
                                    vm.loadMessage(msg.id ?: "")
                                    navController.navigate(Screen.MessageDetail.go(msg.id ?: ""))
                                }
                            )
                            HorizontalDivider()
                        }
                        if (hasMore) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                    if (isLoadingMore) CircularProgressIndicator(Modifier.size(24.dp))
                                    else TextButton(onClick = { vm.loadMore() }) {
                                        Text(stringResource(R.string.messages_load_more))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
