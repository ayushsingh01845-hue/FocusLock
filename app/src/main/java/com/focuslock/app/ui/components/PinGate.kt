package com.focuslock.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.focuslock.app.data.AppSettings
import com.focuslock.app.util.AdminPin

/** Asks for the admin PIN. Calls [onSuccess] when it is right. */
@Composable
fun PinDialog(
    settings: AppSettings,
    title: String = "Enter admin PIN",
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = { v -> pin = v.filter { it.isDigit() }.take(6); wrong = false },
                singleLine = true,
                isError = wrong,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                supportingText = { if (wrong) Text("Wrong PIN") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (AdminPin.verify(pin, settings.adminSalt, settings.adminPinHash)) onSuccess() else { wrong = true; pin = "" }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCEL") } }
    )
}

/**
 * Wraps a screen. When [required] and a PIN exists, the content stays hidden until the right PIN
 * is typed. Cancelling calls [onBack].
 */
@Composable
fun PinGate(
    required: Boolean,
    settings: AppSettings,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    val needed = required && settings.adminPinHash.isNotEmpty()
    var unlocked by remember { mutableStateOf(false) }
    if (!needed || unlocked) {
        content()
    } else {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("LOCKED BY ADMIN", style = MaterialTheme.typography.titleLarge)
        }
        PinDialog(settings = settings, onSuccess = { unlocked = true }, onDismiss = onBack)
    }
}
