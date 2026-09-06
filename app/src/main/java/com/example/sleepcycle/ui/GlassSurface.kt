package com.example.sleepcycle.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.example.sleepcycle.ui.theme.LocalSleepGlass
import com.example.sleepcycle.ui.theme.LocalSleepGradients
import com.example.sleepcycle.ui.theme.SleepGlassColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

/** 由 SleepScreen 提供的全局 Haze 源，供所有玻璃表面模糊背景使用。 */
val LocalHazeState = staticCompositionLocalOf<HazeState?> { null }

fun glassHazeStyle(glass: SleepGlassColors): HazeStyle = HazeStyle(
    backgroundColor = glass.tint.copy(alpha = 1f),
    tints = listOf(HazeTint(glass.tint)),
    blurRadius = 24.dp
)

/** 挂在任意容器上的玻璃模糊效果；无可用 Haze 源时退化为半透明底色。 */
@Composable
fun Modifier.glassEffect(glass: SleepGlassColors = LocalSleepGlass.current): Modifier {
    val state = LocalHazeState.current ?: return this.background(glass.tint)
    return this.hazeChild(state, glassHazeStyle(glass))
}

/**
 * 统一的液态玻璃表面：真模糊 + 半透明底色 + 高光描边，可选品牌渐变叠层。
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    glass: SleepGlassColors = LocalSleepGlass.current,
    overlay: Brush? = null,
    overlayColor: Color? = null,
    overlayAlpha: Float = 1f,
    borderBrush: Brush? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .glassEffect(glass)
            .border(1.dp, borderBrush ?: glass.highlightBorder, shape)
    ) {
        if (overlayColor != null) {
            Box(Modifier.matchParentSize().alpha(overlayAlpha).background(overlayColor, shape))
        } else if (overlay != null) {
            Box(Modifier.matchParentSize().alpha(overlayAlpha).background(overlay, shape))
        }
        content()
    }
}

/**
 * 全局背景：主题渐变 + 静态极光光斑（玻璃模糊的“光源”），并登记为 Haze 源。
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val gradients = LocalSleepGradients.current
    val glass = LocalSleepGlass.current
    val hazeState = LocalHazeState.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Box(modifier.fillMaxSize()) {
        // 模糊源单独成层：Haze 要求玻璃面不能是源节点的子孙
        Box(
            Modifier
                .matchParentSize()
                .background(gradients.backgroundBrush)
                .then(if (hazeState != null) Modifier.haze(hazeState) else Modifier)
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    val a = glass.auroraAlpha
                    drawCircle(
                        brush = Brush.radialGradient(listOf(primary.copy(alpha = a), Color.Transparent), center = Offset(w * 0.15f, h * 0.10f), radius = w * 0.75f),
                        radius = w * 0.75f, center = Offset(w * 0.15f, h * 0.10f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(listOf(secondary.copy(alpha = a * 0.8f), Color.Transparent), center = Offset(w * 0.95f, h * 0.40f), radius = w * 0.65f),
                        radius = w * 0.65f, center = Offset(w * 0.95f, h * 0.40f)
                    )
                    drawCircle(
                        brush = Brush.radialGradient(listOf(tertiary.copy(alpha = a * 0.7f), Color.Transparent), center = Offset(w * 0.30f, h * 0.95f), radius = w * 0.80f),
                        radius = w * 0.80f, center = Offset(w * 0.30f, h * 0.95f)
                    )
                }
        )
        content()
    }
}
