package com.example.cabglance2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import com.example.cabglance2.ui.theme.CabGlance2Theme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.draw.rotate

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            // Handled
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val permsList = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permsList.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            permsList.add(Manifest.permission.RECEIVE_SMS)
        }
        if (permsList.isNotEmpty()) {
            permissionLauncher.launch(permsList.toTypedArray())
        }

        ScheduleManager.setupReminders(this)
        
        setContent {
            CabGlance2Theme {
                Scaffold(
                    bottomBar = {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp, top = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("lit by ", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "@", 
                                style = MaterialTheme.typography.bodySmall, 
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "invert", 
                                style = MaterialTheme.typography.bodySmall, 
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.rotate(180f)
                            )
                            Text(
                                "ignite", 
                                style = MaterialTheme.typography.bodySmall, 
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                " ; )", 
                                style = MaterialTheme.typography.bodySmall, 
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) { innerPadding ->
                    var currentMode by remember { mutableStateOf(SettingsManager.getSourcingMode(this)) }
                    var isStickyEnabled by remember { mutableStateOf(SettingsManager.isStickyNotificationEnabled(this)) }
                    var history by remember { mutableStateOf(RideDataStore.getHistory(this)) }

                    DashboardAppUI(
                        modifier = Modifier.padding(innerPadding).fillMaxSize(),
                        currentMode = currentMode,
                        isStickyEnabled = isStickyEnabled,
                        history = history,
                        onModeSelected = { selectedMode ->
                            SettingsManager.setSourcingMode(this, selectedMode)
                            currentMode = selectedMode
                        },
                        onStickyCheckChanged = { active ->
                            SettingsManager.setStickyNotificationEnabled(this, active)
                            isStickyEnabled = active
                            if (!active) {
                                // Clear immediately 
                                androidx.core.app.NotificationManagerCompat.from(this).cancel(1001)
                            }
                        },
                        onRequestNotificationPermission = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        onSimulate = { rideInfo ->
                            WidgetUpdateHelper.updateWidgetAndNotification(this, rideInfo)
                            history = RideDataStore.getHistory(this)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardAppUI(
    modifier: Modifier = Modifier,
    currentMode: SourcingMode,
    isStickyEnabled: Boolean,
    history: List<String>,
    onModeSelected: (SourcingMode) -> Unit,
    onStickyCheckChanged: (Boolean) -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onSimulate: (RideInfo) -> Unit
) {
    var devTapCount by remember { mutableStateOf(0) }

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "CabGlance", 
                style = MaterialTheme.typography.headlineLarge, 
                fontWeight = FontWeight.Bold, 
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    devTapCount++
                }
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        item {
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = currentMode == SourcingMode.APP_NOTIFICATION, onClick = { onModeSelected(SourcingMode.APP_NOTIFICATION) })
                        Text("App Notifications Mode")
                    }
                    if (currentMode == SourcingMode.APP_NOTIFICATION) {
                        Button(onClick = onRequestNotificationPermission, modifier = Modifier.padding(start=40.dp)) {
                            Text("Grant Listener Access")
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = currentMode == SourcingMode.SMS, onClick = { onModeSelected(SourcingMode.SMS) })
                        Text("SMS Listening Mode (Recommended)")
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Sticky Notification", fontWeight = FontWeight.Medium)
                            Text("Persistent active tab", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = isStickyEnabled, onCheckedChange = onStickyCheckChanged)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    val context = androidx.compose.ui.platform.LocalContext.current
                    var morningTime by remember { mutableStateOf(ScheduleManager.getMorningTime(context)) }
                    var eveningTime by remember { mutableStateOf(ScheduleManager.getEveningTime(context)) }

                    Text("Custom Reminders", fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        OutlinedButton(onClick = {
                            android.app.TimePickerDialog(context, { _, h, m ->
                                ScheduleManager.setMorningTime(context, h, m)
                                morningTime = Pair(h, m)
                            }, morningTime.first, morningTime.second, true).show()
                        }) { 
                            val timeStr = String.format("%02d:%02d", morningTime.first, morningTime.second)
                            Text("Morning ($timeStr)") 
                        }
                        
                        OutlinedButton(onClick = {
                            android.app.TimePickerDialog(context, { _, h, m ->
                                ScheduleManager.setEveningTime(context, h, m)
                                eveningTime = Pair(h, m)
                            }, eveningTime.first, eveningTime.second, true).show()
                        }) { 
                            val timeStr = String.format("%02d:%02d", eveningTime.first, eveningTime.second)
                            Text("Evening ($timeStr)") 
                        }
                    }
                }
            }
        }

        if (devTapCount >= 7) {
            item {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Developer Simulation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(onClick = {
                            onSimulate(RideInfo(NotificationType.LOGIN, "8192", "4096", "08:30", "KA-01-AB-1234", "R-15", false, "Test Morning"))
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Simulate Morning Login")
                        }
                        Button(onClick = {
                            onSimulate(RideInfo(NotificationType.APPROACHING, "8192", "4096", null, "KA-01-AB-1234", null, true, "Test Approach"))
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Simulate 1.5km Away")
                        }
                        Button(onClick = {
                            onSimulate(RideInfo(NotificationType.LOGOUT, "1024", "2048", null, null, "R-18", false, "Test Evening"))
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Simulate Evening Logout")
                        }
                    }
                }
            }
        }

        item {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                Text("App Logs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        if (history.isEmpty()) {
            item {
                Text("No recent activity.", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(history) { logStr ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), 
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(logStr, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        
        item { 
            Spacer(modifier = Modifier.height(32.dp)) 
        }
    }
}
