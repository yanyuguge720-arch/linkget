package com.linkget.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.linkget.LinkGetApp

@Composable
fun NetworkImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    cornerRadius: Int = 0,
    maxDimension: Int = 1440
) {
    val context = LocalContext.current
    val loader = (context.applicationContext as? LinkGetApp)?.container?.imageLoader
    var bitmap by remember(url, maxDimension) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(url, maxDimension) {
        bitmap = if (url != null && loader != null) loader.load(url, maxDimension) else null
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.Image,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            )
        }
    }
}
