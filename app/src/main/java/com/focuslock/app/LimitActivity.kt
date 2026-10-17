package com.focuslock.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focuslock.app.ui.FocusViewModel
import com.focuslock.app.ui.components.Button
import com.focuslock.app.ui.theme.Bg
import com.focuslock.app.ui.theme.FocusLockTheme
import com.focuslock.app.ui.theme.GlassFill
import com.focuslock.app.ui.theme.GlassRim
import com.focuslock.app.ui.theme.InkDim

/** Shown when a daily app time limit has been used up. Back and the button both go Home. */
class LimitActivity : ComponentActivity() {

    private val viewModel: FocusViewModel by viewModels()

    companion object {
        const val EXTRA_PACKAGE = "limit_package"
        const val EXTRA_LIMIT = "limit_minutes"
        const val EXTRA_USED_MIN = "used_minutes"
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_PACKAGE) ?: ""
        val limit = intent.getIntExtra(EXTRA_LIMIT, 0)
        val used = intent.getIntExtra(EXTRA_USED_MIN, 0)
        val label = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault("This app")

        setContent {
            val settings by viewModel.settings.collectAsState()
            FocusLockTheme(accentTheme = settings.accentTheme) {
                val accent = MaterialTheme.colorScheme.primary
                val shape = RoundedCornerShape(32.dp)
                Box(Modifier.fillMaxSize().background(Bg).padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(shape)
                            .background(GlassFill)
                            .border(1.dp, GlassRim, shape)
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("DAILY LIMIT REACHED", style = MaterialTheme.typography.labelLarge, color = accent)
                        Spacer(Modifier.height(12.dp))
                        Text(label.uppercase(), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "You used $used of $limit minutes today. Come back tomorrow.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkDim,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(22.dp))
                        Button(onClick = { goHome() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            Text("GO HOME")
                        }
                    }
                }
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() = goHome()
}
