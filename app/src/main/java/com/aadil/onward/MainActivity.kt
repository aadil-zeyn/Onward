package com.aadil.onward

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aadil.onward.data.OnwardViewModel
import com.aadil.onward.model.ThemePref
import com.aadil.onward.ui.OnwardApp
import com.aadil.onward.ui.theme.OnwardTheme

class MainActivity : ComponentActivity() {
    private val vm: OnwardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val data by vm.data.collectAsStateWithLifecycle()
            val dark = when (data.theme) {
                ThemePref.SYSTEM -> isSystemInDarkTheme()
                ThemePref.LIGHT -> false
                ThemePref.DARK -> true
            }
            // Status and navigation bar icons follow the app's theme, not the system's.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            OnwardTheme(dark) {
                OnwardApp(vm, data)
            }
        }
    }
}
