package com.focuslock.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.focuslock.app.R
import com.focuslock.app.util.loadProfileBitmap

/**
 * Shows the person's own photo if they picked one, otherwise the default app avatar.
 * [photoVersion] changes every time the photo changes, so the picture refreshes right away.
 */
@Composable
fun ProfileAvatarImage(photoVersion: Long, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(photoVersion) { loadProfileBitmap(context) }
    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Profile photo",
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}
