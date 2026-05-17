package com.weathersnap.app

import android.app.Application
import com.weathersnap.util.TempFileSweeper
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class WeatherSnapApp : Application() {

    @Inject lateinit var tempFileSweeper: TempFileSweeper

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        appScope.launch { tempFileSweeper.sweep() }
    }
}
