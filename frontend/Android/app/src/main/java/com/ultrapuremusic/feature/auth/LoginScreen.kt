package com.ultrapuremusic.feature.auth

import android.app.Activity.RESULT_OK
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ultrapuremusic.core.ui.components.MusicButton
import com.ultrapuremusic.core.ui.theme.AccentPrimary
import com.ultrapuremusic.core.ui.theme.AccentSubtle
import com.ultrapuremusic.core.ui.theme.Background
import com.ultrapuremusic.core.ui.theme.PlayerGradientStart
import com.ultrapuremusic.core.ui.theme.TextPrimary
import com.ultrapuremusic.core.ui.theme.TextSecondary

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState      by viewModel.uiState.collectAsState()
    val snackbarState = remember { SnackbarHostState() }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            viewModel.handleSignInResult(result.data)
        }
    }

    // Fallback: if a sign-in cache from an older app version did not include the
    // YouTube scope, the OS will return a UserRecoverableAuthException with an
    // Intent. We launch it here, then tell the ViewModel to resume the flow.
    val youtubePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { _ ->
        viewModel.resumeAfterPermissionGrant()
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { destination ->
            if (destination == "home") onLoginSuccess()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.youtubePermissionIntent.collect { intent ->
            youtubePermissionLauncher.launch(intent)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Error) {
            snackbarState.showSnackbar((uiState as AuthUiState.Error).message)
        }
    }

    val contentAlpha by animateFloatAsState(
        targetValue   = if (uiState is AuthUiState.Loading) 0.5f else 1f,
        animationSpec = tween(300),
        label         = "content_alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to PlayerGradientStart.copy(alpha = 0.8f),
                        0.5f to PlayerGradientStart.copy(alpha = 0.2f),
                        1.0f to Background,
                    ),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // App icon
            Box(
                modifier         = Modifier
                    .size(100.dp)
                    .background(AccentSubtle, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint               = AccentPrimary,
                    modifier           = Modifier.size(56.dp),
                )
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text      = "Ultrapure Music",
                style     = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color     = TextPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text      = "Khám phá âm nhạc không giới hạn",
                style     = MaterialTheme.typography.bodyMedium,
                color     = TextSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(72.dp))

            if (uiState is AuthUiState.Loading) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentPrimary)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text      = (uiState as AuthUiState.Loading).message,
                        style     = MaterialTheme.typography.bodyMedium,
                        color     = TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                MusicButton(
                    text     = "Đăng nhập với Google",
                    onClick  = { signInLauncher.launch(viewModel.getSignInIntent()) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text      = "Đăng nhập sẽ tự đồng bộ playlist từ YouTube của bạn",
                    style     = MaterialTheme.typography.labelSmall,
                    color     = TextSecondary.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text  = "Tiếp tục là bạn đồng ý với Điều khoản dịch vụ\nvà Chính sách quyền riêng tư của chúng tôi",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
            )
        }

        SnackbarHost(
            hostState = snackbarState,
            modifier  = Modifier.align(Alignment.BottomCenter),
        )
    }
}
