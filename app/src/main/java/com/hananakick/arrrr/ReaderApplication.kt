package com.hananakick.arrrr

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.svg.SvgDecoder
import com.hananakick.arrrr.data.*
import com.hananakick.arrrr.network.FeedClient
import com.hananakick.arrrr.sync.SyncScheduler
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

class ReaderApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val settings by lazy { SettingsStore(this) }
    val repository by lazy { ReaderRepository(this, ReaderDatabase(this), FeedClient()) }

    override fun onCreate() {
        super.onCreate()
        SingletonImageLoader.setSafe { context ->
            ImageLoader.Builder(context).components { add(SvgDecoder.Factory()) }.build()
        }
        applicationScope.launch { repository.load() }
        applicationScope.launch {
            settings.flow.map { Triple(it.background, it.pollMinutes, it.wifiOnly) }.distinctUntilChanged().collect { (on, minutes, wifi) ->
                SyncScheduler.schedule(this@ReaderApplication, ReaderSettings(background=on, pollMinutes=minutes, wifiOnly=wifi))
            }
        }
    }
}
