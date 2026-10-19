package com.example.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun CameraPreview(
    isCameraOn: Boolean,
    isFrontCamera: Boolean,
    onCameraActiveChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Check if the device / PC actually has camera hardware
    val hasCameraHardware = remember {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isCameraStreaming by remember { mutableStateOf(false) }
    var cameraBindingFailed by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            isCameraStreaming = false
            onCameraActiveChanged(false)
        }
    }

    LaunchedEffect(isCameraOn, hasCameraHardware) {
        if (!isCameraOn || !hasCameraHardware) {
            isCameraStreaming = false
            onCameraActiveChanged(false)
        } else if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // IF CAMERA IS NOT OPEN OR IF ON PC WITHOUT CAMERA: SHOW PURE BLACK!
    if (!isCameraOn || !hasCameraHardware || !hasCameraPermission || cameraBindingFailed) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .testTag("camera_solid_black_screen")
        )
        return
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .testTag("camera_preview_view"),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val lensFacing = if (isFrontCamera) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }

                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        // Verify camera hardware actually exists on PC/device
                        if (!cameraProvider.hasCamera(cameraSelector)) {
                            // Try opposite lens if default lens missing
                            val fallbackLens = if (isFrontCamera) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
                            val fallbackSelector = CameraSelector.Builder().requireLensFacing(fallbackLens).build()
                            if (!cameraProvider.hasCamera(fallbackSelector)) {
                                cameraBindingFailed = true
                                isCameraStreaming = false
                                onCameraActiveChanged(false)
                                return@addListener
                            }
                        }

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview
                        )
                        isCameraStreaming = true
                        onCameraActiveChanged(true)
                    } catch (exc: Exception) {
                        Log.e("CameraPreview", "Camera failed on this device/PC: ${exc.message}")
                        cameraBindingFailed = true
                        isCameraStreaming = false
                        onCameraActiveChanged(false)
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            },
            update = { previewView ->
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        val lensFacing = if (isFrontCamera) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(lensFacing)
                            .build()

                        if (cameraProvider.hasCamera(cameraSelector)) {
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview
                            )
                            isCameraStreaming = true
                            onCameraActiveChanged(true)
                        } else {
                            cameraBindingFailed = true
                            isCameraStreaming = false
                            onCameraActiveChanged(false)
                        }
                    } catch (exc: Exception) {
                        Log.e("CameraPreview", "Rebind failed: ${exc.message}")
                        cameraBindingFailed = true
                        isCameraStreaming = false
                        onCameraActiveChanged(false)
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            onCameraActiveChanged(false)
        }
    }
}
