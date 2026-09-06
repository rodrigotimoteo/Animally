package com.github.rodrigotimoteo.animally

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.github.rodrigotimoteo.animally.domain.notification.AndroidNotificationPermissionBridge
import com.github.rodrigotimoteo.animally.presentation.AnimallyApp

class MainActivity : ComponentActivity() {
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            AndroidNotificationPermissionBridge.complete(granted)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidNotificationPermissionBridge.register {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                AndroidNotificationPermissionBridge.complete(true)
            }
        }

        setContent {
            AnimallyApp()
        }
    }

    override fun onDestroy() {
        AndroidNotificationPermissionBridge.unregister()
        super.onDestroy()
    }
}

@Preview
@Composable
@Suppress("ktlint:standard:function-naming")
fun AnimallyAppAndroidPreview() {
    AnimallyApp()
}
