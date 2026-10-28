package com.example

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.OndaFmApp
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.RadioTunerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            MyApplicationTheme(darkTheme = true) {
                val context = LocalContext.current
                val tunerViewModel: RadioTunerViewModel = viewModel(
                    factory = RadioTunerViewModel.provideFactory(context)
                )
                OndaFmApp(viewModel = tunerViewModel)
            }
        }
    }
}
