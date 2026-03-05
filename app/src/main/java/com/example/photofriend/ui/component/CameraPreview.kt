package com.example.photofriend.ui.component

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
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
    val colorMatrix = remember(effectParams.colorMatrixValues) {
        ColorMatrix(effectParams.colorMatrixValues)
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { context ->
                PreviewView(context).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    cameraManager.bindToLifecycle(lifecycleOwner, this)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    colorFilter = ColorFilter.colorMatrix(colorMatrix)
                }
        )

        if (effectParams.grainAmount > 0f) {
            GrainOverlay(
                amount = effectParams.grainAmount,
                sizePx = effectParams.grainSizePx,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun GrainOverlay(amount: Float, sizePx: Float, modifier: Modifier = Modifier) {
    var seed by remember { mutableIntStateOf(0) }

    // Animate grain at ~12 fps for a film-like feel
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

    // Bright grain (lifts shadows like real film grain)
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

    // Dark grain (adds texture in highlights)
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
