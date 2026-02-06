package com.example.musicplayer

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.musicplayer.ui.LibraryScreen
import com.example.musicplayer.ui.MusicPlayerScreen
import com.example.musicplayer.ui.MusicViewModel

sealed class Screen(val route: String) {
    object Library : Screen("library")
    object Player : Screen("player")
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
    }
}
