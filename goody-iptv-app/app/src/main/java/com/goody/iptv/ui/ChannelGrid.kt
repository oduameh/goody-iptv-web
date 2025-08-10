package com.goody.iptv.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LiveTv
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.goody.iptv.model.Channel

@Composable
fun ChannelGrid(
    channels: List<Channel>,
    favorites: Set<String>,
    onChannelClick: (Channel) -> Unit,
    onFavoriteToggle: (Channel) -> Unit,
    isCompactView: Boolean = false
) {
    val cellSize = if (isCompactView) GridCells.Fixed(4) else GridCells.Fixed(2)
    
    LazyVerticalGrid(
        columns = cellSize,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(channels) { channel ->
            ChannelGridItem(
                channel = channel,
                isFavorite = favorites.contains(channel.tvgId.ifEmpty { channel.name }),
                onChannelClick = { onChannelClick(channel) },
                onFavoriteToggle = { onFavoriteToggle(channel) },
                isCompact = isCompactView
            )
        }
    }
}

@Composable
fun ChannelGridItem(
    channel: Channel,
    isFavorite: Boolean,
    onChannelClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    isCompact: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isCompact) 120.dp else 160.dp)
            .clickable { onChannelClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Channel logo/image
            if (channel.tvgLogo.isNotEmpty()) {
                AsyncImage(
                    model = channel.tvgLogo,
                    contentDescription = channel.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                    fallback = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.LiveTv,
                                contentDescription = null,
                                modifier = Modifier.size(if (isCompact) 24.dp else 32.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                )
            } else {
                // Default icon background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LiveTv,
                        contentDescription = null,
                        modifier = Modifier.size(if (isCompact) 24.dp else 32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Gradient overlay for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )
            
            // Channel info overlay
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Favorite button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onFavoriteToggle,
                        modifier = Modifier.size(if (isCompact) 24.dp else 32.dp)
                    ) {
                        Icon(
                            if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) Color.Yellow else Color.White,
                            modifier = Modifier.size(if (isCompact) 16.dp else 20.dp)
                        )
                    }
                }
                
                // Channel name and group
                Column {
                    Text(
                        text = channel.name,
                        style = if (isCompact) 
                            MaterialTheme.typography.bodySmall 
                        else 
                            MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = if (isCompact) 1 else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    
                    if (!isCompact && channel.group.isNotEmpty()) {
                        Text(
                            text = channel.group,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ViewModeToggle(
    isGridView: Boolean,
    isCompactGrid: Boolean,
    onViewModeChange: (isGrid: Boolean, isCompact: Boolean) -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = !isGridView,
            onClick = { onViewModeChange(false, false) },
            label = { Text("List") },
            leadingIcon = {
                Icon(
                    painter = androidx.compose.material.icons.Icons.Default.ViewList,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
        
        FilterChip(
            selected = isGridView && !isCompactGrid,
            onClick = { onViewModeChange(true, false) },
            label = { Text("Grid") },
            leadingIcon = {
                Icon(
                    painter = androidx.compose.material.icons.Icons.Default.GridView,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
        
        FilterChip(
            selected = isGridView && isCompactGrid,
            onClick = { onViewModeChange(true, true) },
            label = { Text("Compact") },
            leadingIcon = {
                Icon(
                    painter = androidx.compose.material.icons.Icons.Default.Apps,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
    }
} 