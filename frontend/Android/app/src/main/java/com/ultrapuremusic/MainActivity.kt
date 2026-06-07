package com.ultrapuremusic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ultrapuremusic.core.datastore.UserPreferences
import com.ultrapuremusic.core.network.NetworkMonitor
import com.ultrapuremusic.core.ui.components.BottomNavBar
import com.ultrapuremusic.core.ui.navigation.NavGraph
import com.ultrapuremusic.core.ui.navigation.Screen
import com.ultrapuremusic.core.ui.theme.Background
import com.ultrapuremusic.core.ui.theme.UltrapureMusicTheme
import com.ultrapuremusic.core.ui.theme.WarningColor
import com.ultrapuremusic.feature.player.MiniPlayer
import com.ultrapuremusic.feature.player.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Routes where the bottom navigation bar is visible */
private val MAIN_ROUTES = setOf(
    Screen.Home.route,
    Screen.Search.route,
    Screen.Library.route,
    Screen.Profile.route,
)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var userPreferences: UserPreferences
    @Inject lateinit var networkMonitor: NetworkMonitor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val theme        by userPreferences.theme.collectAsState(initial = "dark")
            val dynamicColor by userPreferences.dynamicColor.collectAsState(initial = false)
            val systemDark   = isSystemInDarkTheme()
            val isDark = when (theme) {
                "light"  -> false
                "system" -> systemDark
                else     -> true   // "dark" and default
            }
            val isOnline by networkMonitor.isOnline.collectAsState(initial = true)
            UltrapureMusicTheme(darkTheme = isDark, dynamicColor = dynamicColor) {
                Box(modifier = Modifier.fillMaxSize()) {
                    MainApp()
                    // Offline banner — slides in from the top when connectivity is lost.
                    AnimatedVisibility(
                        visible = !isOnline,
                        enter   = slideInVertically { -it } + fadeIn(),
                        exit    = slideOutVertically { -it } + fadeOut(),
                        modifier = Modifier.align(Alignment.TopCenter),
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(WarningColor)
                                .statusBarsPadding()
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text      = "Không có mạng · Chỉ nghe được nhạc đã tải",
                                color     = Color.White,
                                fontSize  = 12.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainApp() {
    val navController: NavHostController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    // Activity-scoped PlayerViewModel — shared across MiniPlayer, PlayerScreen, and all
    // screens so playback state stays in sync regardless of navigation.
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val playerState by playerViewModel.playerState.collectAsState()

    val showBottomNav  = currentRoute in MAIN_ROUTES
    val showMiniPlayer = showBottomNav && playerState.currentSong != null

    Scaffold(
        containerColor = Background,
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomNav,
                enter   = slideInVertically { it } + fadeIn(),
                exit    = slideOutVertically { it } + fadeOut(),
            ) {
                Column {
                    // Mini-player sits directly above the nav bar
                    AnimatedVisibility(
                        visible = showMiniPlayer,
                        enter   = slideInVertically { it } + fadeIn(),
                        exit    = slideOutVertically { it } + fadeOut(),
                    ) {
                        MiniPlayer(
                            onExpand  = { navController.navigate(Screen.Player.route) },
                            viewModel = playerViewModel,
                        )
                    }

                    BottomNavBar(
                        currentRoute = currentRoute,
                        onNavigate   = { route ->
                            navController.navigate(route) {
                                popUpTo(Screen.Home.route) { saveState = true }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier        = Modifier.fillMaxSize().padding(innerPadding),
            contentAlignment = Alignment.TopStart,
        ) {
            NavGraph(
                navController   = navController,
                playerViewModel = playerViewModel,
            )
        }
    }
}
