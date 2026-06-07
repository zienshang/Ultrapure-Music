package com.ultrapuremusic.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import com.ultrapuremusic.core.ui.theme.AccentPrimary
import com.ultrapuremusic.core.ui.theme.AccentSubtle
import com.ultrapuremusic.core.ui.theme.Background
import com.ultrapuremusic.core.ui.theme.CardBackground
import com.ultrapuremusic.core.ui.theme.GlassBorder
import com.ultrapuremusic.core.ui.theme.TextPrimary
import com.ultrapuremusic.core.ui.theme.TextSecondary

// Logout brand red — semantic destructive action color
private val LogoutRed = Color(0xFFE53935)

@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state        by viewModel.uiState.collectAsState()
    val snackbarHost  = remember { SnackbarHostState() }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Show errors in Snackbar
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHost.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    // After logout completes → navigate back to login
    LaunchedEffect(Unit) {
        viewModel.logoutEvent.collect { onLoggedOut() }
    }

    if (showLogoutDialog) {
        LogoutConfirmDialog(
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout()
            },
            onDismiss = { showLogoutDialog = false },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .verticalScroll(rememberScrollState()),
        ) {
            // ── Page header ──────────────────────────────────────────────────
            Column(
                modifier = Modifier.padding(
                    start  = 20.dp,
                    end    = 20.dp,
                    top    = 24.dp,
                    bottom = 8.dp,
                ),
            ) {
                Text(
                    text  = "Hồ sơ",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                )
            }

            // ── Avatar + account info ─────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                AccentPrimary.copy(alpha = 0.25f),
                                CardBackground,
                            ),
                        ),
                    )
                    .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ProfileAvatar(
                    avatarUrl = state.avatarUrl,
                    size      = 64.dp,
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text  = state.userName.ifBlank { "Người dùng" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = state.userEmail.ifBlank { "Chưa đăng nhập" },
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── App settings ──────────────────────────────────────────────────
            SectionLabel("Cài đặt")

            MenuGroup {
                MenuItem(
                    icon    = Icons.Default.Settings,
                    label   = "Cài đặt ứng dụng",
                    onClick = onNavigateToSettings,
                )
                MenuDivider()
                MenuItem(
                    icon    = Icons.Default.Notifications,
                    label   = "Thông báo",
                    onClick = { },
                )
            }

            Spacer(Modifier.height(16.dp))

            // ── Info ──────────────────────────────────────────────────────────
            SectionLabel("Thông tin")

            MenuGroup {
                MenuItem(
                    icon         = Icons.Default.Info,
                    label        = "Phiên bản",
                    trailingText = "1.0.0",
                    onClick      = { },
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Logout ────────────────────────────────────────────────────────
            LogoutButton(
                isLoading = state.isLoggingOut,
                onClick   = { showLogoutDialog = true },
            )

            Spacer(Modifier.height(32.dp))
        }

        SnackbarHost(
            hostState = snackbarHost,
            modifier  = Modifier.align(Alignment.BottomCenter),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Avatar
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Round avatar that loads the Google / YouTube account photo via Coil.
 * Falls back to the AccountCircle icon while loading or if the URL is missing
 * or the request fails (offline, photo removed, etc.).
 */
@Composable
private fun ProfileAvatar(
    avatarUrl: String?,
    size: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier         = Modifier
            .size(size)
            .clip(CircleShape)
            .background(AccentSubtle),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarUrl.isNullOrBlank()) {
            FallbackAvatarIcon(size = size)
        } else {
            val painter = rememberAsyncImagePainter(model = avatarUrl)
            val painterState by painter.state.collectAsState()
            AsyncImage(
                model              = avatarUrl,
                contentDescription = "Ảnh đại diện",
                contentScale       = ContentScale.Crop,
                modifier           = Modifier.matchParentSize(),
            )
            // Show the fallback while loading / on error so the circle never looks empty
            if (painterState is AsyncImagePainter.State.Loading ||
                painterState is AsyncImagePainter.State.Error
            ) {
                FallbackAvatarIcon(size = size)
            }
        }
    }
}

@Composable
private fun FallbackAvatarIcon(size: androidx.compose.ui.unit.Dp) {
    Icon(
        imageVector        = Icons.Default.AccountCircle,
        contentDescription = null,
        tint               = AccentPrimary,
        modifier           = Modifier.size(size * 0.75f),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Generic menu components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color    = TextSecondary,
        modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun MenuGroup(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp)),
    ) {
        content()
    }
}

@Composable
private fun MenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    trailingText: String? = null,
    iconTint: Color = AccentPrimary,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier         = Modifier
                .size(34.dp)
                .background(iconTint.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = label,
                tint               = iconTint,
                modifier           = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text     = label,
            style    = MaterialTheme.typography.bodyMedium,
            color    = TextPrimary,
            modifier = Modifier.weight(1f),
        )
        if (trailingText != null) {
            Text(
                text  = trailingText,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
            )
            Spacer(Modifier.width(4.dp))
        }
        Icon(
            imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint               = TextSecondary,
            modifier           = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        color    = GlassBorder,
        modifier = Modifier.padding(start = 64.dp),
        thickness = 0.5.dp,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Logout
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LogoutButton(
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(LogoutRed.copy(alpha = 0.12f))
            .border(1.dp, LogoutRed.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .clickable(enabled = !isLoading) { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier         = Modifier
                .size(34.dp)
                .background(LogoutRed.copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.AutoMirrored.Filled.Logout,
                contentDescription = null,
                tint               = LogoutRed,
                modifier           = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text     = if (isLoading) "Đang đăng xuất..." else "Đăng xuất",
            style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color    = LogoutRed,
            modifier = Modifier.weight(1f),
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier    = Modifier.size(20.dp),
                color       = LogoutRed,
                strokeWidth = 2.dp,
            )
        }
    }
}

@Composable
private fun LogoutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text  = "Đăng xuất?",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
            )
        },
        text = {
            Text(
                text  = "Bạn sẽ cần đăng nhập lại để sử dụng ứng dụng. Các playlist đã đồng bộ vẫn được giữ lại trên thiết bị.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = "Đăng xuất", color = LogoutRed, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Huỷ", color = TextSecondary)
            }
        },
        containerColor = CardBackground,
    )
}
