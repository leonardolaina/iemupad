package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepSlate
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosMint
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    var selectedDevice by remember { mutableStateOf("iPhone 4 (Retina A4)") }
    var selectedIosVersion by remember { mutableStateOf("iOS 4.2.1") }
    var jitCompilerEnabled by remember { mutableStateOf(true) }
    var metalVulkanLayer by remember { mutableStateOf(true) }
    var hapticFeedbackEnabled by remember { mutableStateOf(true) }
    var tiltSensitivity by remember { mutableFloatStateOf(1.0f) }
    var lowLatencyAudio by remember { mutableStateOf(true) }
    var cacheClearedMessage by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B16)),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Ajustes do Emulador iOS",
                color = TextPrimary,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Section: Emulated Hardware & Firmware
        item {
            SettingsSectionCard(
                title = "Hardware & Firmware Emulado",
                icon = Icons.Default.Smartphone
            ) {
                SettingRowSelector(
                    label = "Aparelho Emulado",
                    value = selectedDevice,
                    options = listOf("iPhone 4 (Retina A4)", "iPhone 3GS", "iPhone 5 (A6)", "iPod Touch 4G"),
                    onSelect = { selectedDevice = it }
                )
                HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 10.dp))
                SettingRowSelector(
                    label = "Versão do iOS",
                    value = selectedIosVersion,
                    options = listOf("iOS 4.2.1", "iOS 5.1.1", "iOS 6.1.6", "iOS 3.1.3"),
                    onSelect = { selectedIosVersion = it }
                )
            }
        }

        // Section: Graphics & Performance
        item {
            SettingsSectionCard(
                title = "Gráficos & Renderização",
                icon = Icons.Default.Palette
            ) {
                SettingToggleRow(
                    title = "Dynarmic ARMv7 JIT Compiler",
                    subtitle = "Compilação de código de máquina em tempo real (60 FPS)",
                    checked = jitCompilerEnabled,
                    onCheckedChange = { jitCompilerEnabled = it }
                )
                HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 10.dp))
                SettingToggleRow(
                    title = "Tradução Metal / OpenGL ES para Vulkan",
                    subtitle = "Converte chamadas gráficas da Apple diretamente para Vulkan no Android",
                    checked = metalVulkanLayer,
                    onCheckedChange = { metalVulkanLayer = it }
                )
            }
        }

        // Section: Sensors & Controls
        item {
            SettingsSectionCard(
                title = "Entrada, Sensores & Giroscópio",
                icon = Icons.Default.Sensors
            ) {
                SettingToggleRow(
                    title = "Resposta Háptica e Vibrações",
                    subtitle = "Feedback tátil ao tocar botões, pular e fatiar",
                    checked = hapticFeedbackEnabled,
                    onCheckedChange = { hapticFeedbackEnabled = it }
                )
                HorizontalDivider(color = SurfaceBorder, modifier = Modifier.padding(vertical = 10.dp))
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sensibilidade da Inclinação (Tilt)", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("%.1fx".format(tiltSensitivity), color = IosBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = tiltSensitivity,
                        onValueChange = { tiltSensitivity = it },
                        valueRange = 0.5f..2.5f,
                        colors = SliderDefaults.colors(
                            thumbColor = IosBlue,
                            activeTrackColor = IosBlue,
                            inactiveTrackColor = Color(0xFF334155)
                        ),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        // Section: Audio
        item {
            SettingsSectionCard(
                title = "Áudio & CoreAudio",
                icon = Icons.Default.VolumeUp
            ) {
                SettingToggleRow(
                    title = "Buffer de Baixa Latência (CoreAudio)",
                    subtitle = "64 amostras para resposta de som instantânea em jogos de ritmo",
                    checked = lowLatencyAudio,
                    onCheckedChange = { lowLatencyAudio = it }
                )
            }
        }

        // Section: Storage & Maintenance
        item {
            SettingsSectionCard(
                title = "Armazenamento & Cache",
                icon = Icons.Default.CleaningServices
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Cache de Shaders Vulkan", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text("Tamanho: 14.2 MB", color = TextSecondary, fontSize = 12.sp)
                    }
                    Button(
                        onClick = { cacheClearedMessage = "Cache de Shaders limpo com sucesso!" },
                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceBorder),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Limpar Cache", color = TextPrimary, fontSize = 12.sp)
                    }
                }

                if (cacheClearedMessage != null) {
                    Text(
                        text = cacheClearedMessage!!,
                        color = IosMint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // About iEmu
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Sobre o iEmu Games Android", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        text = "Baseado na tecnologia de emulação de alto nível (HLE) touchHLE v0.2.3 para execução de binários Mach-O ARMv7 de 32 bits no Android com suporte completo a gráficos acelerados por hardware Vulkan.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    Text(
                        text = "Build: v1.0.0 (ARMv7-touchHLE-JIT)",
                        color = Color(0xFF64D2FF),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(icon, contentDescription = null, tint = IosBlue, modifier = Modifier.size(20.dp))
                Text(title, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            content()
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = IosBlue,
                uncheckedThumbColor = Color(0xFFA1A1AA),
                uncheckedTrackColor = Color(0xFF334155)
            )
        )
    }
}

@Composable
private fun SettingRowSelector(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    Column {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.take(2).forEach { opt ->
                val isSelected = (opt == value)
                Surface(
                    color = if (isSelected) IosBlue else Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.Transparent)
                ) {
                    Button(
                        onClick = { onSelect(opt) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) IosBlue else Color(0xFF1E293B)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(opt, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}
