package com.focuslock.app

import com.focuslock.app.ui.components.Button
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.animateContentSize
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import android.content.Intent
import com.focuslock.app.ui.screens.ToolsActivity
import com.focuslock.app.ui.components.ProfileAvatarImage
import com.focuslock.app.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.focuslock.app.data.AppSettings
import com.focuslock.app.data.AppTheme
import com.focuslock.app.ui.FocusViewModel
import com.focuslock.app.ui.navigation.FocusLockNavGraph
import com.focuslock.app.ui.navigation.Screen
import com.focuslock.app.ui.theme.FocusLockTheme
import com.focuslock.app.ui.theme.focusLockBackgroundBrush
import java.io.File

class MainActivity : ComponentActivity() {

    private val viewModel: FocusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val crashFile = File(filesDir, "last_crash.txt")
        if (crashFile.exists()) {
            val crashText = runCatching { crashFile.readText() }.getOrDefault("(could not read crash log)")
            setContent {
                FocusLockTheme {
                    CrashLogScreen(crashText = crashText, onDismiss = {
                        crashFile.delete()
                        recreate()
                    })
                }
            }
            return
        }

        setContent {
            val settings by viewModel.settings.collectAsState()
            FocusLockTheme(accentTheme = settings.accentTheme) {
                FocusLockApp(viewModel, settings.accentTheme)
            }
        }
    }
}

@Composable
private fun CrashLogScreen(crashText: String, onDismiss: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Text("Ayush Focus crashed last time", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Here's the error - screenshot this and send it over.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Card(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Text(
                crashText,
                modifier = Modifier
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState()),
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Clear and continue")
        }
    }
}

private data class NavEntry(val label: String, val route: String?, val icon: ImageVector)

private val MenuEntries = listOf(
    NavEntry("Home", Screen.Home.route, Icons.Filled.Home),
    NavEntry("Profile", Screen.Profile.route, Icons.Filled.Person),
    NavEntry("Focus", Screen.Focus.route, Icons.Filled.Timer),
    NavEntry("Stats", Screen.Stats.route, Icons.Filled.BarChart),
    NavEntry("Screen Time", Screen.ScreenTime.route, Icons.Filled.PhoneAndroid),
    NavEntry("AI Assistant", Screen.AiChat.route, Icons.Filled.SmartToy),
    NavEntry("Tools", null, Icons.Filled.Build),
    NavEntry("History", Screen.History.route, Icons.Filled.History),
    NavEntry("Settings", Screen.Settings.route, Icons.Filled.Settings),
    NavEntry("Permissions", Screen.Permissions.route, Icons.Filled.Security)
)

private val DockEntries = listOf(
    NavEntry("Home", Screen.Home.route, Icons.Filled.Home),
    NavEntry("Focus", Screen.Focus.route, Icons.Filled.Timer),
    NavEntry("Stats", Screen.Stats.route, Icons.Filled.BarChart),
    NavEntry("Settings", Screen.Settings.route, Icons.Filled.Settings)
)

@Composable
@Suppress("UNUSED_PARAMETER")
fun FocusLockApp(viewModel: FocusViewModel, accentTheme: com.focuslock.app.data.AccentTheme) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val activeSession by viewModel.activeSession.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val sessionOn = activeSession != null
    val profile by viewModel.settings.collectAsState()
    val displayName = profile.profileName.trim().ifBlank { "Guest" }

    fun goTo(target: String) {
        navController.navigate(target) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val sectionName = when (route) {
        Screen.CreateSession.route -> "New Session"
        Screen.AppSelection.route -> "Choose Apps"
        else -> MenuEntries.firstOrNull { it.route == route }?.label ?: ""
    }

    // Soft red light sources behind everything - this is what the glass "refracts".
    Box(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Red.copy(alpha = 0.32f), Color.Transparent),
                        center = Offset(size.width * 0.95f, size.height * 0.08f),
                        radius = size.width * 0.9f
                    ),
                    radius = size.width * 0.9f,
                    center = Offset(size.width * 0.95f, size.height * 0.08f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Red.copy(alpha = 0.20f), Color.Transparent),
                        center = Offset(size.width * 0.05f, size.height * 0.88f),
                        radius = size.width * 0.8f
                    ),
                    radius = size.width * 0.8f,
                    center = Offset(size.width * 0.05f, size.height * 0.88f)
                )
            }
    ) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = Color(0xFF080808),
                    drawerShape = RoundedCornerShape(topEnd = 32.dp, bottomEnd = 32.dp),
                    modifier = Modifier.width(300.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(Red.copy(alpha = 0.30f), Color.Transparent), endY = 900f))
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        val cardShape = RoundedCornerShape(26.dp)
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .background(GlassFill)
                                .border(1.dp, GlassRim, cardShape)
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AvatarRing(size = 58.dp, photoVersion = profile.profilePhotoVersion)
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(displayName.uppercase(), style = MaterialTheme.typography.titleLarge, color = Ink, maxLines = 1)
                                Text(
                                    "STREAK  " + streak + (if (streak == 1) " DAY" else " DAYS"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Red
                                )
                            }
                        }
                        Spacer(Modifier.height(18.dp))
                        Text("MENU", style = MaterialTheme.typography.labelSmall, color = InkDim, modifier = Modifier.padding(start = 6.dp, bottom = 8.dp))
                        MenuEntries.forEachIndexed { index, entry ->
                            val active = entry.route != null && entry.route == route
                            val itemShape = RoundedCornerShape(20.dp)
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(itemShape)
                                    .then(
                                        if (active) Modifier
                                            .background(Brush.horizontalGradient(listOf(Red.copy(alpha = 0.95f), Red.copy(alpha = 0.45f))))
                                            .border(1.dp, GlassRim, itemShape)
                                        else Modifier.background(Color.White.copy(alpha = 0.04f))
                                    )
                                    .clickable {
                                        scope.launch { drawerState.close() }
                                        if (entry.route == null) {
                                            context.startActivity(Intent(context, ToolsActivity::class.java))
                                        } else {
                                            goTo(entry.route)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(13.dp))
                                        .background(Color.White.copy(alpha = if (active) 0.22f else 0.08f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        entry.icon,
                                        contentDescription = null,
                                        tint = if (active) Color.White else Color.White.copy(alpha = 0.75f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    entry.label.uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (active) Ink else Color.White.copy(alpha = 0.82f),
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    "%02d".format(index + 1),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (active) Color.White.copy(alpha = 0.8f) else InkDim
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        Text(
                            "STAY LOCKED IN",
                            style = MaterialTheme.typography.labelSmall,
                            color = InkDim,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                contentColor = Ink,
                topBar = {
                    AppHeader(
                        sectionName = sectionName,
                        sessionOn = sessionOn,
                        photoVersion = profile.profilePhotoVersion,
                        onProfile = { goTo(Screen.Profile.route) },
                        onMenu = { scope.launch { drawerState.open() } }
                    )
                }
            ) { padding ->
                val showDock = route in DockEntries.map { it.route }
                Box(Modifier.padding(padding).fillMaxSize()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(bottom = if (showDock) 96.dp else 0.dp)
                    ) {
                        FocusLockNavGraph(navController = navController, viewModel = viewModel)
                    }
                    if (showDock) {
                        val dockShape = RoundedCornerShape(36.dp)
                        Row(
                            Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 18.dp)
                                .shadow(
                                    elevation = 22.dp,
                                    shape = dockShape,
                                    ambientColor = Red.copy(alpha = 0.45f),
                                    spotColor = Red.copy(alpha = 0.45f)
                                )
                                .clip(dockShape)
                                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.07f))))
                                .border(1.dp, GlassRim, dockShape)
                                .padding(6.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DockEntries.forEach { entry ->
                                val active = entry.route == route
                                val itemShape = RoundedCornerShape(30.dp)
                                Row(
                                    Modifier
                                        .animateContentSize()
                                        .height(50.dp)
                                        .clip(itemShape)
                                        .then(
                                            if (active) Modifier
                                                .background(Brush.verticalGradient(listOf(Red.copy(alpha = 0.95f), Red.copy(alpha = 0.60f))))
                                                .border(1.dp, GlassRim, itemShape)
                                            else Modifier
                                        )
                                        .clickable { entry.route?.let { goTo(it) } }
                                        .padding(horizontal = if (active) 18.dp else 15.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        entry.icon,
                                        contentDescription = entry.label,
                                        tint = if (active) Color.White else Color.White.copy(alpha = 0.65f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    if (active) {
                                        Spacer(Modifier.width(8.dp))
                                        Text(entry.label.uppercase(), style = MaterialTheme.typography.labelLarge, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AvatarRing(size: androidx.compose.ui.unit.Dp, photoVersion: Long, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .border(2.dp, Brush.sweepGradient(listOf(Red, Color.White, Red)), CircleShape)
            .padding(3.dp)
    ) {
        ProfileAvatarImage(photoVersion = photoVersion, modifier = Modifier.fillMaxSize().clip(CircleShape))
    }
}

/** Floating liquid-glass header: avatar ring + live status on the left, section chip and menu on the right. */
@Composable
private fun AppHeader(sectionName: String, sessionOn: Boolean, photoVersion: Long, onProfile: () -> Unit, onMenu: () -> Unit) {
    val shape = RoundedCornerShape(30.dp)
    val pulse = rememberInfiniteTransition(label = "pulse")
    val dotAlpha by pulse.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "dot"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .shadow(
                elevation = 16.dp,
                shape = shape,
                ambientColor = Red.copy(alpha = 0.30f),
                spotColor = Red.copy(alpha = 0.30f)
            )
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.20f), Color.White.copy(alpha = 0.06f))))
            .border(1.dp, GlassRim, shape)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarRing(size = 46.dp, photoVersion = photoVersion, modifier = Modifier.clip(CircleShape).clickable { onProfile() })
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("AYUSH FOCUS", style = MaterialTheme.typography.titleMedium, color = Ink, maxLines = 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (sessionOn) Red.copy(alpha = dotAlpha) else InkDim)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (sessionOn) "FOCUS ON" else "READY",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (sessionOn) Red else InkDim
                )
            }
        }
        if (sectionName.isNotEmpty()) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Red.copy(alpha = 0.22f))
                    .border(1.dp, Red.copy(alpha = 0.55f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    sectionName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        Box(
            Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.24f), Color.White.copy(alpha = 0.06f))))
                .border(1.dp, GlassRim, CircleShape)
                .clickable { onMenu() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Menu, contentDescription = "Menu", tint = Ink, modifier = Modifier.size(22.dp))
        }
    }
}
