package com.hananakick.arrrr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.hananakick.arrrr.ui.ReaderApp
import com.hananakick.arrrr.ui.ReaderViewModel
import com.hananakick.arrrr.ui.theme.ArrrrTheme
import com.hananakick.arrrr.ui.theme.isReaderDark

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: ReaderViewModel = viewModel()
            val settings by model.settings.collectAsStateWithLifecycle()
            val dark=isReaderDark(settings)
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars=!dark
                    isAppearanceLightNavigationBars=!dark
                }
            }
            LaunchedEffect(model) {
                lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { model.whileVisible() }
            }
            ArrrrTheme(settings) { ReaderApp(model, settings) }
        }
    }
}
