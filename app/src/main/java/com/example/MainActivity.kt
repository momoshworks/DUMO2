package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.dashboard.DashboardScreen
import com.example.ui.screens.media.MediaHubScreen
import com.example.ui.screens.schedule.ScheduleImportExportDialog
import com.example.ui.screens.schedule.ScheduleScreen
import com.example.ui.screens.tools.AudioRecorderDialog
import com.example.ui.screens.tools.ImageToPdfScreen
import com.example.ui.screens.tools.PomodoroScreen
import com.example.ui.screens.tools.ToolsAndProfileScreen
import com.example.ui.theme.DumuTheme
import com.example.viewmodel.DumuViewModel

enum class DumuNavTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    DASHBOARD("Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "tab_dashboard"),
    SCHEDULE("Schedule", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, "tab_schedule"),
    MEDIA("Hub", Icons.Filled.Folder, Icons.Outlined.Folder, "tab_media"),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person, "tab_profile")
}

enum class SubScreen {
    NONE,
    IMAGE_TO_PDF,
    POMODORO
}

class MainActivity : ComponentActivity() {
    private val viewModel: DumuViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DumuTheme {
                var currentTab by remember { mutableStateOf(DumuNavTab.DASHBOARD) }
                var currentSubScreen by remember { mutableStateOf(SubScreen.NONE) }
                var showAudioRecorderDialog by remember { mutableStateOf(false) }
                var showScheduleImportExportDialog by remember { mutableStateOf(false) }

                // System Back button handling
                if (currentSubScreen != SubScreen.NONE) {
                    BackHandler {
                        currentSubScreen = SubScreen.NONE
                    }
                } else if (currentTab != DumuNavTab.DASHBOARD) {
                    BackHandler {
                        currentTab = DumuNavTab.DASHBOARD
                    }
                }

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        if (currentSubScreen == SubScreen.NONE) {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 8.dp,
                                modifier = Modifier.testTag("bottom_nav_bar")
                            ) {
                                DumuNavTab.values().forEach { tab ->
                                    val isSelected = currentTab == tab
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { currentTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.title,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = tab.title,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 11.sp
                                            )
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        ),
                                        modifier = Modifier.testTag(tab.tag)
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (currentSubScreen == SubScreen.IMAGE_TO_PDF) {
                            ImageToPdfScreen(
                                viewModel = viewModel,
                                onBack = { currentSubScreen = SubScreen.NONE }
                            )
                        } else if (currentSubScreen == SubScreen.POMODORO) {
                            PomodoroScreen(
                                viewModel = viewModel,
                                onBack = { currentSubScreen = SubScreen.NONE }
                            )
                        } else {
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "ScreenTransition"
                            ) { targetTab ->
                                when (targetTab) {
                                    DumuNavTab.DASHBOARD -> DashboardScreen(
                                        viewModel = viewModel,
                                        onNavigateToSchedule = { currentTab = DumuNavTab.SCHEDULE },
                                        onNavigateToMedia = { currentTab = DumuNavTab.MEDIA },
                                        onNavigateToPdfConverter = { currentSubScreen = SubScreen.IMAGE_TO_PDF },
                                        onOpenAudioRecorder = { showAudioRecorderDialog = true },
                                        onOpenScheduleImport = { showScheduleImportExportDialog = true }
                                    )

                                    DumuNavTab.SCHEDULE -> ScheduleScreen(
                                        viewModel = viewModel,
                                        onOpenImportExport = { showScheduleImportExportDialog = true }
                                    )

                                    DumuNavTab.MEDIA -> MediaHubScreen(
                                        viewModel = viewModel,
                                        onNavigateToPdfConverter = { currentSubScreen = SubScreen.IMAGE_TO_PDF },
                                        onNavigateToPomodoro = { currentSubScreen = SubScreen.POMODORO },
                                        onOpenAudioRecorder = { showAudioRecorderDialog = true }
                                    )

                                    DumuNavTab.PROFILE -> ToolsAndProfileScreen(
                                        viewModel = viewModel
                                    )
                                }
                            }
                        }

                        // Dialogs
                        if (showAudioRecorderDialog) {
                            AudioRecorderDialog(
                                viewModel = viewModel,
                                onDismiss = { showAudioRecorderDialog = false }
                            )
                        }

                        if (showScheduleImportExportDialog) {
                            ScheduleImportExportDialog(
                                viewModel = viewModel,
                                onDismiss = { showScheduleImportExportDialog = false }
                            )
                        }
                    }
                }
            }
        }
    }
}
