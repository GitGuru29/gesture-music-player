package com.example.musicplayer

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.musicplayer.ui.DownloaderScreen
import com.example.musicplayer.ui.LibraryScreen
import com.example.musicplayer.ui.MusicPlayerScreen
import com.example.musicplayer.ui.MusicViewModel

sealed class Screen(val route: String) {
    object Library : Screen("library")
    object Player : Screen("player")
    object Downloader : Screen("downloader")
}

@Composable
fun AppNavigation(viewModel: MusicViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Library.route) {
        composable(Screen.Library.route) {
            LibraryScreen(
                viewModel = viewModel,
                onNavigateToPlayer = {
                    navController.navigate(Screen.Player.route)
                },
                onNavigateToDownloader = {
                    navController.navigate(Screen.Downloader.route)
                }
            )
        }
        composable(Screen.Player.route) {
            MusicPlayerScreen(
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Downloader.route) {
            DownloaderScreen(
                viewModel = viewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
