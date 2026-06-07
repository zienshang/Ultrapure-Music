package com.ultrapuremusic.core.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Home : Screen("home")
    data object Search : Screen("search")
    data object Library : Screen("library")
    data object Profile : Screen("profile")
    data object Player : Screen("player")
    data object Login : Screen("login")
    data object Settings : Screen("settings")
    data object Downloads : Screen("downloads")
    data object PlaylistDetail : Screen("playlist/{playlistId}") {
        fun createRoute(playlistId: String) = "playlist/$playlistId"
    }
    data object Favorites : Screen("favorites")
    data object ArtistDetail : Screen("artist/{artistName}") {
        fun createRoute(artistName: String) =
            "artist/${android.net.Uri.encode(artistName)}"
    }
    data object AlbumDetail : Screen("album/{albumName}") {
        fun createRoute(albumName: String) =
            "album/${android.net.Uri.encode(albumName)}"
    }
}
