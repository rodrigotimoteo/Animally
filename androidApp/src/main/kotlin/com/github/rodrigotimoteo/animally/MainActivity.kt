package com.github.rodrigotimoteo.animally

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.github.rodrigotimoteo.animally.presentation.AnimallyApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AnimallyApp()
        }
    }
}

@Preview
@Composable
@Suppress("ktlint:standard:function-naming")
fun AnimallyAppAndroidPreview() {
    AnimallyApp()
}
