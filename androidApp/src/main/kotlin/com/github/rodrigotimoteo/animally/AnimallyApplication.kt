package com.github.rodrigotimoteo.animally

import android.app.Application
import com.github.rodrigotimoteo.animally.di.initKoin

class AnimallyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(this)
    }
}
