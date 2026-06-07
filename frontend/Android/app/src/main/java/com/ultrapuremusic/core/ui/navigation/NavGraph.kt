package com.ultrapuremusic.core.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ultrapuremusic.feature.album.AlbumDetailScreen
import com.ultrapuremusic.feature.artist.ArtistDetailScreen
import com.ultrapuremusic.feature.auth.LoginScreen
import com.ultrapuremusic.feature.downloads.DownloadsScreen
import com.ultrapuremusic.feature.favorites.FavoritesScreen
import com.ultrapuremusic.feature.home.HomeScreen
import com.ultrapuremusic.feature.library.LibraryScreen
import com.ultrapuremusic.feature.player.PlayerScreen
import com.ultrapuremusic.feature.player.PlayerViewModel
import com.ultrapuremusic.feature.playlist.PlaylistDetailScreen
import com.ultrapuremusic.feature.profile.ProfileScreen
import com.ultrapuremusic.feature.search.SearchScreen
import com.ultrapuremusic.feature.settings.SettingsScreen
import com.ultrapuremusic.feature.splash.SplashScreen

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
    startDestination: String = Screen.Splash.route,
) {
    NavHost(
        navController    = navController,
        startDestination = startDestination,
        modifier         = modifier,
    ) {

        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToHome  = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToSearch   = { navController.navigate(Screen.Search.route) },
                onNavigateToPlayer   = { navController.navigate(Screen.Player.route) },
                onNavigateToPlaylist = { id ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(id))
                },
                onNavigateToArtist   = { name ->
                    navController.navigate(Screen.ArtistDetail.createRoute(name))
                },
            )
        }

        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateToPlayer = { navController.navigate(Screen.Player.route) },
                onNavigateToArtist = { name ->
                    navController.navigate(Screen.ArtistDetail.createRoute(name))
                },
            )
        }

        composable(Screen.Library.route) {
            LibraryScreen(
                onNavigateToPlayer    = { navController.navigate(Screen.Player.route) },
                onNavigateToPlaylist  = { id ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(id))
                },
                onNavigateToFavorites = { navController.navigate(Screen.Favorites.route) },
                onNavigateToDownloads = { navController.navigate(Screen.Downloads.route) },
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                onLoggedOut = {
                    // Clear entire back stack and land on Login
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Player.route) {
            PlayerScreen(
                onDismiss = { navController.popBackStack() },
                viewModel = playerViewModel,
            )
        }

        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onNavigateToPlayer = { navController.navigate(Screen.Player.route) },
            )
        }

        composable(Screen.Downloads.route) {
            DownloadsScreen(
                onNavigateToPlayer   = { navController.navigate(Screen.Player.route) },
                onNavigateToPlaylist = { id ->
                    navController.navigate(Screen.PlaylistDetail.createRoute(id))
                },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route     = Screen.ArtistDetail.route,
            arguments = listOf(navArgument("artistName") { type = NavType.StringType }),
        ) {
            ArtistDetailScreen(
                onNavigateBack     = { navController.popBackStack() },
                onNavigateToPlayer = { navController.navigate(Screen.Player.route) },
                onNavigateToArtist = { name ->
                    navController.navigate(Screen.ArtistDetail.createRoute(name))
                },
                onNavigateToAlbum  = { album ->
                    navController.navigate(Screen.AlbumDetail.createRoute(album))
                },
            )
        }

        composable(
            route     = Screen.AlbumDetail.route,
            arguments = listOf(navArgument("albumName") { type = NavType.StringType }),
        ) {
            AlbumDetailScreen(
                onNavigateBack     = { navController.popBackStack() },
                onNavigateToPlayer = { navController.navigate(Screen.Player.route) },
                onNavigateToArtist = { name ->
                    navController.navigate(Screen.ArtistDetail.createRoute(name))
                },
            )
        }

        composable(
            route     = Screen.PlaylistDetail.route,
            arguments = listOf(navArgument("playlistId") { type = NavType.StringType }),
        ) { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId").orEmpty()
            PlaylistDetailScreen(
                playlistId         = playlistId,
                onNavigateBack     = { navController.popBackStack() },
                onNavigateToPlayer = { navController.navigate(Screen.Player.route) },
            )
        }
    }
}
