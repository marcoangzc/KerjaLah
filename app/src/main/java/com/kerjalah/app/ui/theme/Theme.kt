package com.kerjalah.app.ui.theme   // [A] owned by Member A (theme)

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// This file maps our palette onto Material 3 "roles".
// Screens should use MaterialTheme.colorScheme.primary etc., never raw colors.
// Every role we leave out keeps Material 3's default baseline, which is
// purple - that is why cards and the navigation bar used to render lavender
// in an otherwise warm orange app. The container roles below are the ones
// Card, NavigationBar and StatusChip actually draw from, so they all have to
// be named explicitly.
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
    outline = InkSoft,                  // borders, dividers
    outlineVariant = Color(0xFFEADDD1),

    // Surface containers: Card and NavigationBar pick their fill from these.
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFF7F0),
    surfaceContainer = WarmCard,
    surfaceContainerHigh = Color(0xFFFCEADC),
    surfaceContainerHighest = Peach,

    // StatusChip reads these three pairs, one per application status.
    secondaryContainer = PendingOrangeSoft,   // PENDING
    onSecondaryContainer = Ink,
    tertiary = AcceptedGreen,
    onTertiary = Color.White,
    tertiaryContainer = AcceptedGreenSoft,    // ACCEPTED
    onTertiaryContainer = Color(0xFF1B5E20),
    errorContainer = RejectedGraySoft,        // REJECTED
    onErrorContainer = Color(0xFF4A423B),
)

// Wrap the whole app in this. MainActivity will call KerjaLahTheme { ... }.
@Composable
fun KerjaLahTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KerjaLahColors,
        content = content
    )
}