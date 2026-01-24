package com.screenshotvault.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.screenshotvault.ui.screens.about.AboutScreen
import com.screenshotvault.ui.screens.detail.DetailScreen
import com.screenshotvault.ui.screens.feed.FeedScreen
import com.screenshotvault.ui.screens.insights.InsightsScreen
import com.screenshotvault.ui.screens.search.SearchScreen
import com.screenshotvault.ui.screens.settings.SettingsScreen
import com.screenshotvault.ui.screens.topics.TopicsScreen

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    data object Feed : Screen("feed", "Feed", Icons.Default.Home)
    data object Insights : Screen("insights", "Insights", Icons.Default.Dashboard)
    data object Topics : Screen("topics", "Topics", Icons.Default.Tag)
    data object Search : Screen("search", "Search", Icons.Default.Search)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    data object Detail : Screen("detail/{screenshotId}", "Detail", Icons.Default.Home) {
        fun createRoute(screenshotId: String) = "detail/$screenshotId"
    }
    data object TopicFilter : Screen("topic-filter/{topicName}", "Topic", Icons.Default.Tag) {
        fun createRoute(topicName: String) = "topic-filter/${java.net.URLEncoder.encode(topicName, "UTF-8")}"
    }
    data object About : Screen("about", "About", Icons.Default.Home)
}

private val bottomNavScreens = listOf(
    Screen.Feed,
    Screen.Insights,
    Screen.Topics,
    Screen.Search,
    Screen.Settings,
)

@Composable
fun ScreenshotVaultNavHost() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination

            // Hide bottom bar on detail screen
            val showBottomBar = bottomNavScreens.any { screen ->
                currentDestination?.hierarchy?.any { it.route == screen.route } == true
            }

            if (showBottomBar) {
                NavigationBar {
                    bottomNavScreens.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Feed.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Feed.route) {
                FeedScreen(
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onItemClick = { screenshotId ->
                        navController.navigate(Screen.Detail.createRoute(screenshotId))
                    },
                    onTopicClick = { topic ->
                        navController.navigate(Screen.TopicFilter.createRoute(topic))
                    },
                )
            }
            composable(Screen.Topics.route) {
                TopicsScreen(
                    onItemClick = { screenshotId ->
                        navController.navigate(Screen.Detail.createRoute(screenshotId))
                    },
                    onTopicClick = { topic ->
                        navController.navigate(Screen.TopicFilter.createRoute(topic))
                    },
                )
            }
            composable(
                route = Screen.TopicFilter.route,
                arguments = listOf(
                    navArgument("topicName") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val topicName = backStackEntry.arguments?.getString("topicName")
                    ?.let { java.net.URLDecoder.decode(it, "UTF-8") }
                    ?: return@composable
                TopicsScreen(
                    initialTopic = topicName,
                    onItemClick = { screenshotId ->
                        navController.navigate(Screen.Detail.createRoute(screenshotId))
                    },
                    onTopicClick = { topic ->
                        navController.navigate(Screen.TopicFilter.createRoute(topic))
                    },
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(Screen.Insights.route) {
                InsightsScreen(
                    onItemClick = { screenshotId ->
                        navController.navigate(Screen.Detail.createRoute(screenshotId))
                    },
                    onTopicClick = { topic ->
                        navController.navigate(Screen.TopicFilter.createRoute(topic))
                    },
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    onItemClick = { screenshotId ->
                        navController.navigate(Screen.Detail.createRoute(screenshotId))
                    },
                    onTopicClick = { topic ->
                        navController.navigate(Screen.TopicFilter.createRoute(topic))
                    },
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateToAbout = {
                        navController.navigate(Screen.About.route)
                    },
                )
            }
            composable(Screen.About.route) {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Screen.Detail.route,
                arguments = listOf(
                    navArgument("screenshotId") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val screenshotId = backStackEntry.arguments?.getString("screenshotId") ?: return@composable
                DetailScreen(
                    screenshotId = screenshotId,
                    onNavigateBack = { navController.popBackStack() },
                    onTopicClick = { topic ->
                        navController.navigate(Screen.TopicFilter.createRoute(topic))
                    },
                )
            }
        }
    }
}
