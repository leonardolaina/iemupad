package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GameEntity
import com.example.ui.theme.IosBlue
import com.example.ui.theme.IosMint
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GameCard(
    game: GameEntity,
    onGameClick: (GameEntity) -> Unit,
    onDownloadClick: (GameEntity) -> Unit,
    onPlayClick: (GameEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentColor = try {
        Color(android.graphics.Color.parseColor(game.accentColorHex))
    } catch (_: Exception) {
        IosBlue
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder.copy(alpha = 0.6f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("game_card_${game.id}")
            .clickable { onGameClick(game) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Squircle iOS app icon with gloss
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(accentColor, accentColor.copy(alpha = 0.7f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val iconVector: ImageVector = when (game.iconSymbol) {
                        "racing" -> Icons.Default.DirectionsCar
                        "runner" -> Icons.Default.DirectionsRun
                        "puzzle" -> Icons.Default.Extension
                        "plane" -> Icons.Default.Flight
                        "music" -> Icons.Default.MusicNote
                        "blade" -> Icons.Default.SportsEsports
                        "jump" -> Icons.Default.Star
                        "folder_zip" -> Icons.Default.FolderZip
                        "shield" -> Icons.Default.Shield
                        else -> Icons.Default.SportsEsports
                    }
                    Icon(
                        imageVector = iconVector,
                        contentDescription = game.title,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = game.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${game.developer} • ${game.category}",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    // iOS Version & Architecture Badges
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = game.iosVersion,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64D2FF),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            color = Color(0xFF1E293B),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            val sizeMb = game.fileSizeBytes / (1024 * 1024)
                            Text(
                                text = "${sizeMb} MB",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextMuted,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // Description preview
            Text(
                text = game.summary,
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 12.dp)
            )

            // Download Progress Bar if downloading
            if (game.isDownloading) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = game.downloadSpeed.ifEmpty { "Baixando IPA..." },
                            fontSize = 12.sp,
                            color = Color(0xFF64D2FF)
                        )
                        Text(
                            text = "${(game.downloadProgress * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { game.downloadProgress },
                        color = IosBlue,
                        trackColor = Color(0xFF334155),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .padding(top = 6.dp)
                            .clip(CircleShape)
                    )
                }
            }

            // Action Row: Compatibility rating + Play/Download button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Compatibilidade",
                        tint = IosMint,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = game.compatibilityRating,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = IosMint
                    )
                }

                if (game.isInstalled) {
                    Button(
                        onClick = { onPlayClick(game) },
                        colors = ButtonDefaults.buttonColors(containerColor = IosBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("play_button_${game.id}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Jogar", modifier = Modifier.size(18.dp))
                        Text("Emular", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                    }
                } else if (game.isDownloading) {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Instalando...", fontSize = 13.sp)
                    }
                } else {
                    Button(
                        onClick = { onDownloadClick(game) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("download_button_${game.id}")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Baixar", modifier = Modifier.size(18.dp), tint = Color.White)
                        Text("Baixar IPA", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
    }
}
