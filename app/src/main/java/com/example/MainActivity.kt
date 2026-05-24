package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AgriRepository
import com.example.ui.AgriViewModel
import com.example.ui.AgriViewModelFactory
import com.example.ui.screens.AgriStoreAppContent
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Edge-to-edge support is enabled to let layouts flow beautifully around camera notches
        enableEdgeToEdge()

        val repository = AgriRepository(applicationContext)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            MyApplicationTheme {
                val viewModel: AgriViewModel = viewModel(
                    factory = AgriViewModelFactory(repository)
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AgriStoreAppContent(viewModel = viewModel, windowSizeClass = windowSizeClass)
                }
            }
        }
    }
}
