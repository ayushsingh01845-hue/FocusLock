package com.focuslock.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.focuslock.app.ui.FocusViewModel
import com.focuslock.app.ui.components.Button
import com.focuslock.app.ui.components.HairCard
import com.focuslock.app.ui.components.ProfileAvatarImage
import com.focuslock.app.ui.theme.GlassRim
import com.focuslock.app.ui.theme.InkDim
import com.focuslock.app.ui.theme.Red
import com.focuslock.app.util.profilePhotoFile
import com.focuslock.app.util.removeProfilePhoto
import com.focuslock.app.util.saveProfilePhoto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
@Suppress("UNUSED_PARAMETER")
fun ProfileScreen(navController: NavHostController, viewModel: FocusViewModel) {
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember(settings.profileName) { mutableStateOf(settings.profileName) }
    var message by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val ok = withContext(Dispatchers.IO) { saveProfilePhoto(context, uri) }
                if (ok) {
                    viewModel.updateSettings { it.copy(profilePhotoVersion = System.currentTimeMillis()) }
                    message = "Photo updated"
                } else {
                    message = "Could not use that picture, try another one"
                }
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                Modifier
                    .size(140.dp)
                    .border(3.dp, Brush.sweepGradient(listOf(Red, Color.White, Red)), CircleShape)
                    .padding(6.dp)
                    .clip(CircleShape)
                    .clickable { picker.launch("image/*") }
            ) {
                ProfileAvatarImage(photoVersion = settings.profilePhotoVersion, modifier = Modifier.fillMaxSize())
            }
            Box(
                Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Red)
                    .border(1.dp, GlassRim, CircleShape)
                    .clickable { picker.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.CameraAlt, contentDescription = "Change photo", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            settings.profileName.trim().ifBlank { "Guest" }.uppercase(),
            style = MaterialTheme.typography.headlineMedium
        )
        Text("Your photo and name stay on this phone only.", style = MaterialTheme.typography.bodySmall, color = InkDim)

        Spacer(Modifier.height(24.dp))
        HairCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("YOUR NAME", style = MaterialTheme.typography.labelLarge, color = Red)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 24) name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Type your name") },
                    shape = RoundedCornerShape(18.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = {
                        viewModel.updateSettings { it.copy(profileName = name.trim()) }
                        message = "Saved"
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("SAVE NAME") }
            }
        }

        Spacer(Modifier.height(14.dp))
        HairCard(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp)) {
                Text("PHOTO", style = MaterialTheme.typography.labelLarge, color = Red)
                Spacer(Modifier.height(10.dp))
                Button(onClick = { picker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text("CHOOSE FROM GALLERY")
                }
                if (profilePhotoFile(context).exists() || settings.profilePhotoVersion != 0L) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            removeProfilePhoto(context)
                            viewModel.updateSettings { it.copy(profilePhotoVersion = System.currentTimeMillis()) }
                            message = "Photo removed"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Remove photo") }
                }
            }
        }

        message?.let {
            Spacer(Modifier.height(14.dp))
            Text(it, style = MaterialTheme.typography.labelMedium, color = Red)
        }
        Spacer(Modifier.height(96.dp))
    }
}
