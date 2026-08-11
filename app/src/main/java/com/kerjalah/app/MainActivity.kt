package com.kerjalah.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.rememberNavController
import com.kerjalah.app.navigation.NavGraph
import com.kerjalah.app.ui.theme.KerjaLahTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Wrap everything in our brand theme (colors from Color.kt).
            KerjaLahTheme {
                // rememberNavController = the "driver" that handles navigation.
                val navController = rememberNavController()
                // Hand the driver to the subway map (NavGraph).
                NavGraph(navController = navController)
            }
        }
    }
}