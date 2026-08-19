package com.example.sleepcycle.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ==========================================
// 睡眠科技调色板 (Sleep Tech Color Palette)
// ==========================================

// 深色模式：深邃夜空、星河紫、极光青
private val DarkPrimary = Color(0xFF818CF8) // 柔和星空靛蓝
private val DarkOnPrimary = Color(0xFF0F172A)
private val DarkPrimaryContainer = Color(0xFF312E81)
private val DarkOnPrimaryContainer = Color(0xFFE0E7FF)

private val DarkSecondary = Color(0xFF2DD4BF) // 极光青绿 (健康、充能)
private val DarkOnSecondary = Color(0xFF042F2E)
private val DarkSecondaryContainer = Color(0xFF115E59)
private val DarkOnSecondaryContainer = Color(0xFFCCFBF1)

private val DarkTertiary = Color(0xFFF472B6) // 暮光暖粉 (提示、辅助)
private val DarkOnTertiary = Color(0xFF4C0519)
private val DarkTertiaryContainer = Color(0xFF831843)
private val DarkOnTertiaryContainer = Color(0xFFFFE4E6)

private val DarkBackground = Color(0xFF0B0F19) // 极深邃星夜蓝底色
private val DarkOnBackground = Color(0xFFF1F5F9)
private val DarkSurface = Color(0xFF131B2E) // 悬浮卡片深色表面
private val DarkOnSurface = Color(0xFFF8FAFC)
private val DarkSurfaceVariant = Color(0xFF1E293B) // 次级容器色
private val DarkOnSurfaceVariant = Color(0xFF94A3B8)
private val DarkOutline = Color(0xFF334155)
private val DarkOutlineVariant = Color(0xFF1E293B)

// 浅色模式：晨曦破晓、柔和白昼、清晨微光
private val LightPrimary = Color(0xFF4F46E5) // 经典深靛蓝
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFEEF2FF)
private val LightOnPrimaryContainer = Color(0xFF1E1B4B)

private val LightSecondary = Color(0xFF0D9488) // 晨雾薄荷绿
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFCCFBF1)
private val LightOnSecondaryContainer = Color(0xFF115E59)

private val LightTertiary = Color(0xFFDB2777)
private val LightOnTertiary = Color(0xFFFFFFFF)
private val LightTertiaryContainer = Color(0xFFFFE4E6)
private val LightOnTertiaryContainer = Color(0xFF831843)

private val LightBackground = Color(0xFFF8FAFC) // 柔和晨曦白底
private val LightOnBackground = Color(0xFF0F172A)
private val LightSurface = Color(0xFFFFFFFF)
private val LightOnSurface = Color(0xFF0F172A)
private val LightSurfaceVariant = Color(0xFFF1F5F9)
private val LightOnSurfaceVariant = Color(0xFF64748B)
private val LightOutline = Color(0xFFCBD5E1)
private val LightOutlineVariant = Color(0xFFE2E8F0)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant
)

// ==========================================
// 渐变与微光设计规范 (Custom Gradients)
// ==========================================
@Immutable
data class SleepGradientColors(
    val backgroundBrush: Brush,
    val cardBackgroundBrush: Brush,
    val recommendedCardBrush: Brush,
    val glowBorderBrush: Brush,
    val primaryGradientBrush: Brush,
    val qualityExcellentBrush: Brush,
    val qualityOptimalBrush: Brush
)

val LocalSleepGradients = staticCompositionLocalOf {
    SleepGradientColors(
        backgroundBrush = Brush.verticalGradient(listOf(DarkBackground, DarkBackground)),
        cardBackgroundBrush = Brush.verticalGradient(listOf(DarkSurface, DarkSurface)),
        recommendedCardBrush = Brush.linearGradient(listOf(DarkPrimaryContainer, DarkSurface)),
        glowBorderBrush = Brush.linearGradient(listOf(DarkPrimary, DarkSecondary)),
        primaryGradientBrush = Brush.horizontalGradient(listOf(DarkPrimary, DarkSecondary)),
        qualityExcellentBrush = Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF059669))),
        qualityOptimalBrush = Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF2563EB)))
    )
}

val SleepTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 44.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

@Composable
fun SleepCycleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val sleepGradients = if (darkTheme) {
        SleepGradientColors(
            backgroundBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF0B0F19),
                    Color(0xFF111827),
                    Color(0xFF0B0F19)
                )
            ),
            cardBackgroundBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF162032),
                    Color(0xFF111827)
                )
            ),
            recommendedCardBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1E1B4B).copy(alpha = 0.9f),
                    Color(0xFF0F172A).copy(alpha = 0.95f)
                )
            ),
            glowBorderBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF818CF8),
                    Color(0xFF2DD4BF),
                    Color(0xFF818CF8)
                )
            ),
            primaryGradientBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF6366F1),
                    Color(0xFF818CF8)
                )
            ),
            qualityExcellentBrush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF059669), Color(0xFF10B981))
            ),
            qualityOptimalBrush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF0284C7), Color(0xFF38BDF8))
            )
        )
    } else {
        SleepGradientColors(
            backgroundBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF8FAFC),
                    Color(0xFFF1F5F9),
                    Color(0xFFEDE9FE).copy(alpha = 0.3f)
                )
            ),
            cardBackgroundBrush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),
                    Color(0xFFF8FAFC)
                )
            ),
            recommendedCardBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFEEF2FF),
                    Color(0xFFFFFFFF)
                )
            ),
            glowBorderBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF6366F1),
                    Color(0xFF0D9488),
                    Color(0xFF818CF8)
                )
            ),
            primaryGradientBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF4F46E5),
                    Color(0xFF6366F1)
                )
            ),
            qualityExcellentBrush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF059669), Color(0xFF10B981))
            ),
            qualityOptimalBrush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF0284C7), Color(0xFF0EA5E9))
            )
        )
    }

    CompositionLocalProvider(LocalSleepGradients provides sleepGradients) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SleepTypography,
            content = content
        )
    }
}
