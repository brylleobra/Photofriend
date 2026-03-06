package com.example.photofriend.ui.component

import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.photofriend.camera.CameraManager
import com.example.photofriend.camera.ViewfinderEffectParams
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun CameraPreview(
    cameraManager: CameraManager,
    effectParams: ViewfinderEffectParams = ViewfinderEffectParams.NONE,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusState = remember { mutableStateOf<Offset?>(null) }

    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    cameraManager.bindToLifecycle(lifecycleOwner, this)
                    setOnTouchListener { view, event ->
                        if (event.action == MotionEvent.ACTION_UP) {
                            val factory = (view as PreviewView).meteringPointFactory
                            cameraManager.focusAt(factory, event.x, event.y)
                            focusState.value = Offset(event.x, event.y)
                            view.performClick()
                        }
                        true
                    }
                }
            },
            update = { view ->
                val paint = Paint().apply {
                    colorFilter = ColorMatrixColorFilter(
                        android.graphics.ColorMatrix(effectParams.colorMatrixValues)
                    )
                }
                view.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
            },
            modifier = Modifier.fillMaxSize()
        )

        // Focus ring — auto-hides after 1.5 s
        focusState.value?.let { center ->
            FocusRing(
                center = center,
                onDismiss = { focusState.value = null }
            )
        }

        if (effectParams.grainAmount > 0f) {
            GrainOverlay(
                amount = effectParams.grainAmount,
                sizePx = effectParams.grainSizePx,
                modifier = Modifier.fillMaxSize()
            )
        }

        // Vignette overlay — driven by effectParams
        if (effectParams.vignetteStrength > 0f) {
            VignetteOverlay(
                strength = effectParams.vignetteStrength,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ── Focus ring ────────────────────────────────────────────────────────────────

@Composable
private fun FocusRing(center: Offset, onDismiss: () -> Unit) {
    val scale = remember { Animatable(1.4f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(center) {
        // Pop in then settle
        scale.animateTo(1f, animationSpec = tween(200))
        delay(900L)
        // Fade out
        alpha.animateTo(0f, animationSpec = tween(300))
        onDismiss()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val boxSize = 72.dp.toPx() * scale.value
        drawRect(
            color = Color.Yellow.copy(alpha = alpha.value),
            topLeft = Offset(center.x - boxSize / 2f, center.y - boxSize / 2f),
            size = Size(boxSize, boxSize),
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

// ── Vignette overlay ──────────────────────────────────────────────────────────

@Composable
private fun VignetteOverlay(strength: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = maxOf(size.width, size.height) * 0.75f
        drawCircle(
            brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.Black.copy(alpha = strength.coerceIn(0f, 0.85f))
                ),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = radius
            ),
            radius = radius,
            center = Offset(size.width / 2f, size.height / 2f)
        )
    }
}

// ── Grain overlay ─────────────────────────────────────────────────────────────

@Composable
private fun GrainOverlay(amount: Float, sizePx: Float, modifier: Modifier = Modifier) {
    var seed by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(83L)
            seed++
        }
    }

    Canvas(modifier = modifier) {
        drawGrain(seed = seed, amount = amount, sizePx = sizePx)
    }
}

private fun DrawScope.drawGrain(seed: Int, amount: Float, sizePx: Float) {
    val rng = Random(seed)
    val count = (size.width * size.height * amount * 0.003f).toInt().coerceAtMost(6_000)

    val brightOffsets = Array(count) {
        Offset(rng.nextFloat() * size.width, rng.nextFloat() * size.height)
    }
    drawPoints(
        points = brightOffsets.toList(),
        pointMode = androidx.compose.ui.graphics.PointMode.Points,
        color = Color.White,
        strokeWidth = sizePx,
        alpha = amount * 0.45f,
        blendMode = BlendMode.Overlay
    )

    val darkOffsets = Array(count / 2) {
        Offset(rng.nextFloat() * size.width, rng.nextFloat() * size.height)
    }
    drawPoints(
        points = darkOffsets.toList(),
        pointMode = androidx.compose.ui.graphics.PointMode.Points,
        color = Color.Black,
        strokeWidth = sizePx,
        alpha = amount * 0.30f,
        blendMode = BlendMode.Overlay
    )
}
