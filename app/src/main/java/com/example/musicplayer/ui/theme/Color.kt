package com.example.musicplayer.ui.theme

import androidx.compose.ui.graphics.Color

// Primary accent — vibrant coral-red
val AccentPrimary = Color(0xFFFF6B6B)
val AccentPrimaryDark = Color(0xFFE55656)
val AccentSecondary = Color(0xFF4ECDC4)

// Background palette — deep space darks
val DarkBackground = Color(0xFF070710)
val DarkSurface = Color(0xFF0C0C1A)
val DarkSurfaceVariant = Color(0xFF161630)
val DarkCard = Color(0xFF121228)

// ─── Glass System ─────────────────────────────────────────────
// Frosted glass surfaces — highly visible translucency
val GlassSurface = Color(0x30FFFFFF)          // 19% white — visible frost
val GlassSurfaceStrong = Color(0x40FFFFFF)    // 25% white — prominent glass
val GlassSurfaceDim = Color(0x18FFFFFF)       // 9% white — subtle glass

// Glass borders — bright enough to define edges
val GlassBorder = Color(0x40FFFFFF)           // 25% white — visible border
val GlassBorderBright = Color(0x66FFFFFF)     // 40% white — prominent edge
val GlassBorderDim = Color(0x20FFFFFF)        // 12% white — faint edge

// Glass highlights — top-edge shine
val GlassHighlightTop = Color(0x33FFFFFF)     // 20% white — inner edge top shine
val GlassHighlightCenter = Color(0x0DFFFFFF)  // 5% white — center subtle

// Accent glows — colored halos for active/playing states
val GlowPrimary = Color(0x66FF6B6B)           // 40% red glow
val GlowPrimaryStrong = Color(0x99FF6B6B)     // 60% red glow
val GlowSecondary = Color(0x664ECDC4)         // 40% teal glow
val GlowSoft = Color(0x33FF6B6B)              // 20% soft glow

// ─── Light Theme ──────────────────────────────────────────────
val LightBackground = Color(0xFFFAFAFA)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F0F0)

// Text colors
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFA0A0B8)
val TextTertiary = Color(0xFF606078)

// Gradient colors for player background
val GradientStart = Color(0xFF1A1A2E)
val GradientMid = Color(0xFF16213E)
val GradientEnd = Color(0xFF070710)

// Playing indicator
val NowPlayingGlow = Color(0xFF4ECDC4)

// Legacy compatibility
val Purple80 = AccentPrimary
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = AccentPrimaryDark
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)