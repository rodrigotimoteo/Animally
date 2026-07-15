package com.github.rodrigotimoteo.animally

import androidx.compose.ui.window.ComposeUIViewController
import com.github.rodrigotimoteo.animally.di.infra.initKoin
import com.github.rodrigotimoteo.animally.presentation.AnimallyApp

@Suppress("ktlint:standard:function-naming", "detekt:FunctionNaming")
fun MainViewController() =
    ComposeUIViewController {
        initKoin()
        AnimallyApp()
    }
