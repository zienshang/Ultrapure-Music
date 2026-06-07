package com.ultrapuremusic.feature.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ultrapuremusic.core.ui.theme.AccentPrimary
import com.ultrapuremusic.core.ui.theme.AccentSubtle
import com.ultrapuremusic.core.ui.theme.Background
import com.ultrapuremusic.core.ui.theme.TextPrimary
import com.ultrapuremusic.core.ui.theme.TextSecondary

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    var targetValue by remember { mutableFloatStateOf(0f) }
    val alpha by animateFloatAsState(
        targetValue   = targetValue,
        animationSpec = tween(durationMillis = 700),
        label         = "splash_alpha",
    )
    val scale by animateFloatAsState(
        targetValue   = if (targetValue > 0f) 1f else 0.7f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label         = "splash_scale",
    )

    LaunchedEffect(Unit) {
        targetValue = 1f
        viewModel.destination.collect { dest ->
            when (dest) {
                "home" -> onNavigateToHome()
                else   -> onNavigateToLogin()
            }
        }
    }

    Box(
        modifier         = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier
                .alpha(alpha)
                .scale(scale),
        ) {
            // Icon with circle background
            Box(
                modifier         = Modifier
                    .size(96.dp)
                    .background(AccentSubtle, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Default.MusicNote,
                    contentDescription = "Ultrapure Music",
                    tint               = AccentPrimary,
                    modifier           = Modifier.size(52.dp),
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text  = "Ultrapure Music",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text  = "Premium · Calm · Modern",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
            )
        }
    }
}
