package com.kerjalah.app.ui.theme   // [A] owned by Member A (theme)

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// This file maps our palette onto Material 3 "roles".
// Screens should use MaterialTheme.colorScheme.primary etc., never raw colors.
private val KerjaLahColors = lightColorScheme(
    primary = BrandOrange,              // main action color
    onPrimary = Color.White,            // text / icon ON primary
    primaryContainer = Peach,           // soft orange block
    onPrimaryContainer = Ink,
    secondary = Sdg8Red,                // SDG 8 accent
    onSecondary = Color.White,
    background = WarmPaper,             // whole-app background
    onBackground = Ink,
    surface = WarmPaper,                // cards, sheets
    onSurface = Ink,
    surfaceVariant = WarmCard,          // tinted surfaces
    onSurfaceVariant = InkSoft,
    outline = InkSoft                   // borders, dividers
)

// Wrap the whole app in this. MainActivity will call KerjaLahTheme { ... }.
@Composable
fun KerjaLahTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KerjaLahColors,
        content = content
    )
}