package com.kerjalah.app.ui.theme   // [A] owned by Member A (theme)

import androidx.compose.ui.graphics.Color

// This file = the app's single color palette.
// Change a color HERE -> every screen that uses the theme updates automatically.

// ---- Brand (KerjaLah identity) ----
val BrandOrange = Color(0xFFE8590C)       // primary: buttons, pay text, highlights
val BrandOrangeDeep = Color(0xFFB34300)   // darker orange for pressed / emphasis
val Peach = Color(0xFFFFDCC3)             // light orange surface / container
val Sdg8Red = Color(0xFFA21942)           // SDG 8 badge (official goal color)

// ---- Neutrals (warm, not cold gray) ----
val WarmPaper = Color(0xFFFFFBF7)         // app background (soft warm white)
val WarmCard = Color(0xFFFFF1E6)          // card / surface tint
// ^ 8 hex digits = AARRGGBB. This was 0xFFFF1E6 (seven digits), which Kotlin
// read as alpha 0x0F - a 6%-opaque card that never showed up.
val Ink = Color(0xFF26190F)               // main text (warm near-black)
val InkSoft = Color(0xFF7A6A5F)           // secondary text

// ---- Application status colors (Module 3) ----
val AcceptedGreen = Color(0xFF2E7D32)     // green = got the job
val PendingOrange = Color(0xFFEF6C00)     // orange = still waiting
val RejectedGray = Color(0xFF8A8A8A)      // gray = closed

// Soft backgrounds for the StatusChip, one per status above.
val AcceptedGreenSoft = Color(0xFFD7EBD8)
val PendingOrangeSoft = Color(0xFFFFE3C7)
val RejectedGraySoft = Color(0xFFE8E3DE)