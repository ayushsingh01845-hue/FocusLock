package com.focuslock.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.focuslock.app.ui.FocusViewModel
import com.focuslock.app.ui.components.Button
import com.focuslock.app.ui.components.HairCard
import com.focuslock.app.ui.theme.InkDim
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun JournalScreen(navController: NavHostController, viewModel: FocusViewModel) {
    val entries by viewModel.journal.collectAsState()
    val accent = MaterialTheme.colorScheme.primary
    var text by remember { mutableStateOf("") }
    val fmt = remember { SimpleDateFormat("d MMM, h:mm a", Locale.getDefault()) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(start = 4.dp, end = 20.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
            }
            Text("STUDY JOURNAL", style = MaterialTheme.typography.titleLarge)
        }
        LazyColumn(
            Modifier.weight(1f).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text("Write one line about what you studied today.", style = MaterialTheme.typography.bodyMedium, color = InkDim)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(300) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Today I studied...") },
                    shape = RoundedCornerShape(18.dp),
                    minLines = 2
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.addJournal(text); text = "" },
                    enabled = text.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("SAVE ENTRY") }
            }
            items(entries.reversed(), key = { it.id }) { e ->
                HairCard(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(fmt.format(Date(e.dateMillis)).uppercase(), style = MaterialTheme.typography.labelSmall, color = accent)
                            Spacer(Modifier.height(4.dp))
                            Text(e.text, style = MaterialTheme.typography.bodyLarge)
                        }
                        IconButton(onClick = { viewModel.deleteJournal(e.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete entry")
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}
