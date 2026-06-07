package com.ultrapuremusic.feature.settings

import android.os.Build
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HideSource
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ultrapuremusic.core.player.EqPreset
import com.ultrapuremusic.core.ui.theme.AccentPrimary
import com.ultrapuremusic.core.ui.theme.Background
import com.ultrapuremusic.core.ui.theme.CardBackground
import com.ultrapuremusic.core.ui.theme.GlassBorder
import com.ultrapuremusic.core.ui.theme.TextPrimary
import com.ultrapuremusic.core.ui.theme.TextSecondary

private val WarningRed = Color(0xFFE53935)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { destination ->
            if (destination == "login") onNavigateBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        TopAppBar(
            title = {
                Text(
                    text  = "Cài đặt",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint               = TextPrimary,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {

            // ── Giao diện ─────────────────────────────────────────────────────
            SettingsSectionLabel("Giao diện")

            SettingsGroup {
                // Theme
                SettingsItemWithChips(
                    icon   = Icons.Default.Brightness4,
                    label  = "Giao diện",
                    sublabel = "Chọn chế độ màu",
                    options = listOf("Tối" to "dark", "Sáng" to "light", "Hệ thống" to "system"),
                    selected = state.theme,
                    onSelect = viewModel::setTheme,
                )

                // Dynamic Color — only available on Android 12+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsDivider()
                    SettingsToggleRow(
                        icon    = Icons.Default.Palette,
                        label   = "Màu động (Material You)",
                        sublabel = "Lấy màu từ hình nền hệ thống (Android 12+)",
                        checked = state.useDynamicColor,
                        onCheckedChange = viewModel::setDynamicColor,
                    )
                }
            }

            // ── Mạng & tải xuống ──────────────────────────────────────────────
            SettingsSectionLabel("Mạng & Tải xuống")

            SettingsGroup {
                SettingsToggleRow(
                    icon    = Icons.Default.Wifi,
                    label   = "Chỉ tải qua Wi-Fi",
                    sublabel = "Tiết kiệm dữ liệu di động",
                    checked = state.downloadOverWifiOnly,
                    onCheckedChange = viewModel::setDownloadOverWifiOnly,
                )
            }

            // ── Phát nhạc ─────────────────────────────────────────────────────
            SettingsSectionLabel("Phát nhạc")

            SettingsGroup {
                SettingsItemWithChips(
                    icon     = Icons.Default.GraphicEq,
                    label    = "Chất lượng âm thanh",
                    sublabel = "Ảnh hưởng đến mức dùng dữ liệu",
                    options  = listOf(
                        "Tự động" to 0,
                        "Thấp"    to 1,
                        "Trung"   to 2,
                        "Cao"     to 3,
                    ),
                    selected = state.audioQuality,
                    onSelect = viewModel::setAudioQuality,
                )

                SettingsDivider()

                SettingsItemWithChips(
                    icon     = Icons.Default.MusicNote,
                    label    = "Crossfade",
                    sublabel = "Chuyển mượt giữa các bài",
                    options  = listOf(
                        "Tắt" to 0L,
                        "3s"  to 3_000L,
                        "5s"  to 5_000L,
                        "10s" to 10_000L,
                    ),
                    selected = state.crossfadeDurationMs,
                    onSelect = viewModel::setCrossfadeDuration,
                )

                SettingsDivider()

                SettingsToggleRow(
                    icon    = Icons.Default.HideSource,
                    label   = "Hiện nội dung 18+",
                    sublabel = "Bao gồm nội dung người lớn trong kết quả",
                    checked = state.showExplicit,
                    onCheckedChange = viewModel::setShowExplicit,
                )
            }

            // ── Âm thanh nâng cao ─────────────────────────────────────────────
            SettingsSectionLabel("Âm thanh nâng cao")

            SettingsGroup {
                // EQ Preset
                SettingsEqPresets(
                    selected = state.eqPreset,
                    onSelect = viewModel::setEqPreset,
                )

                SettingsDivider()

                // Bass Boost
                SettingsBassBoost(
                    strength    = state.bassBoostStrength,
                    onStrength  = viewModel::setBassBoostStrength,
                )

                SettingsDivider()

                // Loudness Normalization
                SettingsToggleRow(
                    icon    = Icons.Default.Tune,
                    label   = "Cân bằng âm lượng",
                    sublabel = "Tự động tăng âm bài nhỏ tiếng (+15 dB)",
                    checked = state.loudnessEnabled,
                    onCheckedChange = viewModel::setLoudnessEnabled,
                )
            }

            // ── Lưu trữ ───────────────────────────────────────────────────────
            SettingsSectionLabel("Lưu trữ")

            SettingsGroup {
                // Downloaded songs size
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier         = Modifier
                            .size(34.dp)
                            .background(AccentPrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector        = Icons.Default.Download,
                            contentDescription = null,
                            tint               = AccentPrimary,
                            modifier           = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text  = "Bài hát đã tải",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextPrimary,
                        )
                        Text(
                            text  = if (state.downloadsSizeMb > 0) "Đang dùng ${state.downloadsSizeMb} MB"
                                    else "Chưa tải bài nào",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }

                SettingsDivider()

                // Cache info + clear button
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !state.isClearingCache) { viewModel.clearCache() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier         = Modifier
                            .size(34.dp)
                            .background(AccentPrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.isClearingCache) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color       = AccentPrimary,
                            )
                        } else {
                            Icon(
                                imageVector        = Icons.Default.Cached,
                                contentDescription = null,
                                tint               = AccentPrimary,
                                modifier           = Modifier.size(18.dp),
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text  = "Xoá cache",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.isClearingCache) TextSecondary else TextPrimary,
                        )
                        Text(
                            text  = if (state.cacheSizeMb > 0) "Đang dùng ${state.cacheSizeMb} MB"
                                    else "Cache trống",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text     = text,
        style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
        color    = TextSecondary,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
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
private fun SettingsDivider() {
    HorizontalDivider(
        color    = GlassBorder,
        modifier = Modifier.padding(start = 64.dp),
        thickness = 0.5.dp,
    )
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    label: String,
    sublabel: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier         = Modifier
                .size(34.dp)
                .background(AccentPrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = AccentPrimary,
                modifier           = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(text = sublabel, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(checkedThumbColor = AccentPrimary),
        )
    }
}

@Composable
private fun SettingsEqPresets(selected: String, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier
                    .size(34.dp)
                    .background(AccentPrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Default.Equalizer,
                    contentDescription = null,
                    tint               = AccentPrimary,
                    modifier           = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Equalizer", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text("Chọn preset âm thanh", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        Spacer(Modifier.height(12.dp))
        // Two rows of 4 presets
        val presets = EqPreset.values()
        listOf(presets.take(4), presets.drop(4)).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                row.forEach { preset ->
                    val isSelected = preset.name == selected
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) AccentPrimary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onSelect(preset.name) }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    ) {
                        Text(
                            text  = preset.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) Color.White else TextSecondary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsBassBoost(strength: Int, onStrength: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier
                    .size(34.dp)
                    .background(AccentPrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint               = AccentPrimary,
                    modifier           = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Bass Boost", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text("${strength}%", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        Spacer(Modifier.height(8.dp))
        Slider(
            value         = strength.toFloat(),
            onValueChange = { onStrength(it.toInt()) },
            valueRange    = 0f..100f,
            steps         = 9,
            colors        = SliderDefaults.colors(
                thumbColor        = AccentPrimary,
                activeTrackColor  = AccentPrimary,
                inactiveTrackColor = AccentPrimary.copy(alpha = 0.2f),
            ),
        )
    }
}

@Composable
private fun <T> SettingsItemWithChips(
    icon: ImageVector,
    label: String,
    sublabel: String,
    options: List<Pair<String, T>>,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier         = Modifier
                    .size(34.dp)
                    .background(AccentPrimary.copy(alpha = 0.12f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = AccentPrimary,
                    modifier           = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                Text(text = sublabel, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEach { (chipLabel, value) ->
                val isSelected = value == selected
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) AccentPrimary else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { onSelect(value) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text(
                        text  = chipLabel,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) Color.White else TextSecondary,
                    )
                }
            }
        }
    }
}
