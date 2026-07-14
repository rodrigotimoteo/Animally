package com.github.rodrigotimoteo.animally

import androidx.compose.ui.window.ComposeUIViewController
import com.github.rodrigotimoteo.animally.di.initKoin
import com.github.rodrigotimoteo.animally.presentation.AnimallyApp

fun MainViewController() = ComposeUIViewController {
    initKoin()
    AnimallyApp()
}