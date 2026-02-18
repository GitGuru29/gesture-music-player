package com.example.musicplayer.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Frosted glass surface — translucent panel with gradient background, 
 * visible border, and inner top-edge highlight for realistic glass depth.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = GlassSurface,
    borderColor: Color = GlassBorder,
    borderWidth: Dp = 1.dp,
    enableTopHighlight: Boolean = true,
    glowColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .then(
                if (glowColor != null) {
                    Modifier.graphicsLayer {
                        shadowElevation = 32f
                        this.shape = shape
                        clip = false
                        ambientShadowColor = glowColor
                        spotShadowColor = glowColor
                    }
                } else Modifier
            )
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        backgroundColor,
                        backgroundColor.copy(alpha = backgroundColor.alpha * 0.5f)
                    )
                )
            )
            .then(
                if (enableTopHighlight) {
                    Modifier.drawBehind {
                        // Top-edge highlight shine
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    GlassHighlightTop,
                                    Color.Transparent
                                ),
                                startY = 0f,
                                endY = size.height * 0.3f
                            )
                        )
                    }
                } else Modifier
            )
            .border(borderWidth, borderColor, shape),
        content = content
    )
}

/**
 * Glass card — a styled glass surface for list items and content cards.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    onClick: (() -> Unit)? = null,
    glowColor: Color? = null,
    content: @Composable BoxScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        ),
        shape = shape,
        glowColor = glowColor,
        content = content
    )
}

/**
 * Glass icon button — circular frosted button with visible fill and border.
 */
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    activeColor: Color = AccentPrimary,
    size: Dp = 44.dp,
    content: @Composable () -> Unit
) {
    val bgBrush = if (isActive) {
        Brush.radialGradient(
            colors = listOf(
                activeColor.copy(alpha = 0.4f),
                activeColor.copy(alpha = 0.15f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                GlassSurfaceStrong,
                GlassSurface
            )
        )
    }
    val border = if (isActive) activeColor.copy(alpha = 0.6f) else GlassBorder

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (isActive) {
                    Modifier.graphicsLayer {
                        shadowElevation = 20f
                        shape = CircleShape
                        clip = false
                        ambientShadowColor = activeColor
                        spotShadowColor = activeColor
                    }
                } else Modifier
            )
            .clip(CircleShape)
            .background(bgBrush)
            .border(1.dp, border, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Glass pill/chip — for tabs and tags with gradient fill and glow.
 */
@Composable
fun GlassPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = AccentPrimary,
    badge: String? = null
) {
    val bgBrush = if (selected) {
        Brush.horizontalGradient(
            colors = listOf(
                activeColor.copy(alpha = 0.35f),
                activeColor.copy(alpha = 0.15f)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                GlassSurfaceStrong,
                GlassSurface
            )
        )
    }
    val border = if (selected) activeColor.copy(alpha = 0.6f) else GlassBorder
    val textColor = if (selected) Color.White else TextSecondary

    Box(
        modifier = modifier
            .height(38.dp)
            .then(
                if (selected) {
                    Modifier.graphicsLayer {
                        shadowElevation = 16f
                        shape = RoundedCornerShape(20.dp)
                        clip = false
                        ambientShadowColor = activeColor
                        spotShadowColor = activeColor
                    }
                } else Modifier
            )
            .clip(RoundedCornerShape(20.dp))
            .background(bgBrush)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = textColor
            )
            if (badge != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) Color.White.copy(alpha = 0.25f)
                            else activeColor.copy(alpha = 0.3f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) Color.White else activeColor
                    )
                }
            }
        }
    }
}

/**
 * Glass search bar — frosted input with prominent borders and icon.
 */
@Composable
fun GlassSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search...",
    onSearch: (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    GlassSurface(
        modifier = modifier.height(52.dp),
        shape = RoundedCornerShape(16.dp),
        backgroundColor = GlassSurfaceStrong,
        borderColor = GlassBorder
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                leadingIcon()
            } else {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = TextPrimary
                ),
                cursorBrush = SolidColor(AccentSecondary),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearch?.invoke() }
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (value.isEmpty()) {
                            Text(
                                placeholder,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    }
                }
            )
            if (trailingIcon != null) {
                trailingIcon()
            }
        }
    }
}

/**
 * Animated glass glow ring — pulsing luminous border.
 */
@Composable
fun GlassGlowRing(
    modifier: Modifier = Modifier,
    color: Color = AccentPrimary,
    shape: Shape = CircleShape
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                shadowElevation = 24f
                this.shape = shape
                clip = false
                ambientShadowColor = color.copy(alpha = glowAlpha)
                spotShadowColor = color.copy(alpha = glowAlpha)
            }
            .border(2.dp, color.copy(alpha = glowAlpha), shape)
    )
}
