package com.example.photofriend.camera

import android.content.Context
import android.graphics.Bitmap
import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class CameraManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private val mainExecutor = ContextCompat.getMainExecutor(context)

    // Stored so they can be re-applied once the camera finishes binding.
    private var pendingNrMode: Int = NR_FAST
    private var pendingEdgeMode: Int = EDGE_FAST

    fun bindToLifecycle(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture!!
                )
                applyPendingHardwareSettings()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, mainExecutor)
    }

    /**
     * Applies Noise Reduction and Sharpness (edge mode) at the sensor/ISP level via
     * Camera2 interop. Safe to call before the camera is bound — values are cached and
     * applied once binding completes.
     *
     * @param noiseReduction setting value string, e.g. "-4", "0", "+3"
     * @param sharpness setting value string, e.g. "-2", "0", "+4"
     */
    fun applyHardwareSettings(noiseReduction: String, sharpness: String) {
        val nr = noiseReduction.removePrefix("+").trim().toIntOrNull() ?: 0
        val sh = sharpness.removePrefix("+").trim().toIntOrNull() ?: 0

        pendingNrMode = when {
            nr <= -3 -> NR_OFF
            nr <= -1 -> NR_MINIMAL
            nr >= 2  -> NR_HIGH_QUALITY
            else     -> NR_FAST
        }
        pendingEdgeMode = when {
            sh <= -2 -> EDGE_OFF
            sh >= 2  -> EDGE_HIGH_QUALITY
            else     -> EDGE_FAST
        }
        applyPendingHardwareSettings()
    }

    @OptIn(ExperimentalCamera2Interop::class)
    private fun applyPendingHardwareSettings() {
        val cam = camera ?: return
        Camera2CameraControl.from(cam.cameraControl)
            .setCaptureRequestOptions(
                CaptureRequestOptions.Builder()
                    .setCaptureRequestOption(CaptureRequest.NOISE_REDUCTION_MODE, pendingNrMode)
                    .setCaptureRequestOption(CaptureRequest.EDGE_MODE, pendingEdgeMode)
                    .build()
            )
    }

    companion object {
        private const val NR_OFF          = 0  // NOISE_REDUCTION_MODE_OFF
        private const val NR_FAST         = 1  // NOISE_REDUCTION_MODE_FAST
        private const val NR_HIGH_QUALITY = 2  // NOISE_REDUCTION_MODE_HIGH_QUALITY
        private const val NR_MINIMAL      = 3  // NOISE_REDUCTION_MODE_MINIMAL
        private const val EDGE_OFF          = 0  // EDGE_MODE_OFF
        private const val EDGE_FAST         = 1  // EDGE_MODE_FAST
        private const val EDGE_HIGH_QUALITY = 2  // EDGE_MODE_HIGH_QUALITY
    }

    suspend fun captureFrame(): Bitmap = suspendCancellableCoroutine { cont ->
        val capture = imageCapture
        if (capture == null) {
            cont.resumeWithException(IllegalStateException("Camera not ready. Please wait for the preview to start."))
            return@suspendCancellableCoroutine
        }
        capture.takePicture(
            mainExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toBitmap()
                    image.close()
                    if (cont.isActive) cont.resume(bitmap)
                }

                override fun onError(exception: ImageCaptureException) {
                    if (cont.isActive) cont.resumeWithException(exception)
                }
            }
        )
    }
}
