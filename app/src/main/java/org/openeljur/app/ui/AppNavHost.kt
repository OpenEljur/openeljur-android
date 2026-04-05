package org.openeljur.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.Message
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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import org.openeljur.app.R
import org.openeljur.app.data.NetworkMonitor
import org.openeljur.app.data.PrefsStore
import org.openeljur.app.ui.screens.*
import org.openeljur.app.viewmodel.AuthViewModel
import org.openeljur.app.viewmodel.MessagesViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Diary : Screen("diary")
    object Marks : Screen("marks")
    object Messages : Screen("messages")
    object MessageDetail : Screen("message/{id}") { fun go(id: String) = "message/$id" }
    object MessageCompose : Screen("compose?replyTo={replyTo}") {
        fun go(replyTo: String? = null) = if (replyTo != null) "compose?replyTo=$replyTo" else "compose"
    }
    object Settings : Screen("settings")
}

@Composable
fun AppNavHost() {
    val authVm: AuthViewModel = viewModel()
    val messagesVm: MessagesViewModel = viewModel() // единый экземпляр на весь граф
    val context = LocalContext.current
    val prefs = remember { PrefsStore(context) }

    // Ждём первое значение токена чтобы не мигать
    val tokenState by authVm.token.collectAsStateWithLifecycle()
    val isReady by authVm.isReady.collectAsStateWithLifecycle()
    val isAuthenticated by authVm.isAuthenticated.collectAsStateWithLifecycle()

    val network = remember { NetworkMonitor(context) }
    val isOnline by network.isOnline.collectAsStateWithLifecycle(initialValue = true)
    val navController = rememberNavController()

    // Навигация только после того как состояние готово
    LaunchedEffect(isReady, isAuthenticated) {
        if (!isReady) return@LaunchedEffect
        if (!isAuthenticated) {
            navController.navigate(Screen.Login.route) { popUpTo(0) { inclusive = true } }
        }
    }

    data class NavItem(val screen: Screen, val icon: androidx.compose.ui.graphics.vector.ImageVector, val labelRes: Int)
    val navItems = listOf(
        NavItem(Screen.Diary, Icons.AutoMirrored.Filled.MenuBook, R.string.nav_diary),
        NavItem(Screen.Marks, Icons.Default.BarChart, R.string.nav_marks),
        NavItem(Screen.Messages, Icons.AutoMirrored.Filled.Message, R.string.nav_messages),
        NavItem(Screen.Settings, Icons.Default.Settings, R.string.nav_settings),
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = isReady && isAuthenticated && currentRoute != Screen.Login.route

    if (!isReady) {
        // Пока DataStore не загрузился — пустой экран (splash уже показан)
        Box(Modifier.fillMaxSize())
        return
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    navItems.forEach { item ->
                        val selected = navBackStackEntry?.destination?.hierarchy
                            ?.any { it.route == item.screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true; restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                            label = { Text(stringResource(item.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (!isOnline && showBottomBar) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                ) {
                    Text(
                        stringResource(R.string.common_no_network),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            NavHost(
                navController = navController,
                startDestination = if (isAuthenticated) Screen.Diary.route else Screen.Login.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Screen.Login.route) { LoginScreen(navController) }
                composable(Screen.Diary.route) { DiaryScreen() }
                composable(Screen.Marks.route) { MarksScreen() }
                composable(Screen.Messages.route) { MessagesScreen(navController, messagesVm) }
                composable(Screen.MessageDetail.route) { entry ->
                    MessageDetailScreen(entry.arguments?.getString("id") ?: "", navController, messagesVm)
                }
                composable(Screen.MessageCompose.route) { entry ->
                    MessageComposeScreen(entry.arguments?.getString("replyTo"), navController)
                }
                composable(Screen.Settings.route) { SettingsScreen(authVm) }
            }
        }
    }
}
