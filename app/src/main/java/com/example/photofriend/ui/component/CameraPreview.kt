package com.example.photofriend.ui.component

import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.photofriend.camera.CameraManager

@Composable
fun CameraPreview(
    cameraManager: CameraManager,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        factory = { context ->
            PreviewView(context).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                cameraManager.bindToLifecycle(lifecycleOwner, this)
            }
        },
        modifier = modifier
    )
}
