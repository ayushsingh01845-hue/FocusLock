package com.focuslock.app.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focuslock.app.ui.FocusViewModel
import com.focuslock.app.ui.components.Button
import com.focuslock.app.ui.components.ProfileAvatarImage
import com.focuslock.app.ui.theme.Bg
import com.focuslock.app.ui.theme.GlassFill
import com.focuslock.app.ui.theme.GlassRim
import com.focuslock.app.ui.theme.InkDim
import com.focuslock.app.util.saveProfilePhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Three short glass screens shown only the first time the app opens: welcome, your name and photo, permissions. */
@Composable
fun OnboardingScreen(viewModel: FocusViewModel) {
    val settings by viewModel.settings.collectAsState()
    val accent = MaterialTheme.colorScheme.primary
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var page by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { saveProfilePhoto(context, uri) }
                if (ok) viewModel.updateSettings { it.copy(profilePhotoVersion = System.currentTimeMillis()) }
            }
        }
    }

    fun finish() {
        viewModel.updateSettings { it.copy(profileName = name.trim().ifBlank { it.profileName }, onboardingDone = true) }
    }

    val pulse = rememberInfiniteTransition(label = "pulse")
    val glow by pulse.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "glow"
    )
    val cardShape = RoundedCornerShape(36.dp)

    Box(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(accent.copy(alpha = glow), Color.Transparent),
                        center = Offset(size.width / 2f, size.height * 0.30f),
                        radius = size.width * 1.1f
                    ),
                    radius = size.width * 1.1f,
                    center = Offset(size.width / 2f, size.height * 0.30f)
                )
            }
    ) {
        TextButton(onClick = { finish() }, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
            Text("SKIP", color = InkDim)
        }

        Column(
            Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .clip(cardShape)
                .background(GlassFill)
                .border(1.dp, GlassRim, cardShape)
                .padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Crossfade(targetState = page, animationSpec = tween(350), label = "page") { p ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    when (p) {
                        0 -> {
                            Box(
                                Modifier
                                    .size(96.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.18f))
                                    .border(1.dp, GlassRim, CircleShape),
                                contentAlignment = Alignment.Center
                            ) { Icon(Icons.Filled.Lock, contentDescription = null, tint = accent, modifier = Modifier.size(46.dp)) }
                            Spacer(Modifier.height(22.dp))
                            Text("WELCOME TO AYUSH FOCUS", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Lock the apps that steal your time, start a focus session, and get back to what matters.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkDim,
                                textAlign = TextAlign.Center
                            )
                        }
                        1 -> {
                            Box(
                                Modifier
                                    .size(120.dp)
                                    .border(3.dp, Brush.sweepGradient(listOf(accent, Color.White, accent)), CircleShape)
                                    .padding(6.dp)
                                    .clip(CircleShape)
                                    .clickable { picker.launch("image/*") }
                            ) {
                                ProfileAvatarImage(photoVersion = settings.profilePhotoVersion, modifier = Modifier.fillMaxSize())
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(
                                Modifier.clip(RoundedCornerShape(50)).clickable { picker.launch("image/*") }.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("ADD PHOTO", style = MaterialTheme.typography.labelMedium, color = accent)
                            }
                            Spacer(Modifier.height(14.dp))
                            Text("WHAT SHOULD WE CALL YOU?", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = name,
                                onValueChange = { if (it.length <= 24) name = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text("Your name") },
                                shape = RoundedCornerShape(18.dp),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                            )
                            Spacer(Modifier.height(6.dp))
                            Text("Stays on this phone only.", style = MaterialTheme.typography.bodySmall, color = InkDim)
                        }
                        else -> {
                            Text("TWO QUICK PERMISSIONS", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "These let the app see which app is open and block it. Nothing leaves your phone.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkDim,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(18.dp))
                            Button(
                                onClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("1. USAGE ACCESS") }
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("2. ACCESSIBILITY") }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Find Ayush Focus in each list and switch it on. You can also do this later in Permissions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = InkDim,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i ->
                    Box(
                        Modifier
                            .size(if (i == page) 22.dp else 8.dp, 8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (i == page) accent else Color.White.copy(alpha = 0.25f))
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { if (page < 2) page += 1 else finish() },
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) { Text(if (page < 2) "NEXT" else "LET'S GO") }
        }
    }
}
