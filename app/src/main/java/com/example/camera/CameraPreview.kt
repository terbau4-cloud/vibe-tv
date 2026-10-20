package com.example.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.example.network.WebRtcManager
import org.webrtc.SurfaceViewRenderer

/**
 * Bottom half: displays own live camera feed via WebRTC camera pipeline.
 * Feeds both local preview and streams real-time video to the remote peer.
 * If camera is off or unavailable, displays pure solid black.
 */
@Composable
fun CameraPreview(
    isCameraOn: Boolean,
    isFrontCamera: Boolean,
    onCameraActiveChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isLocalStreaming by WebRtcManager.isLocalCameraStreaming.collectAsState()

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

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            WebRtcManager.startLocalCamera(context, isFrontCamera)
        } else {
            onCameraActiveChanged(false)
        }
    }

    LaunchedEffect(hasCameraPermission, hasCameraHardware) {
        if (!hasCameraHardware) {
            onCameraActiveChanged(false)
        } else if (hasCameraPermission) {
            WebRtcManager.startLocalCamera(context, isFrontCamera)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(isCameraOn) {
        WebRtcManager.setCameraEnabled(isCameraOn)
    }

    LaunchedEffect(isLocalStreaming, isCameraOn) {
        onCameraActiveChanged(isLocalStreaming && isCameraOn)
    }

    // IF CAMERA IS OFF OR HARDWARE MISSING: SHOW PURE SOLID BLACK
    if (!isCameraOn || !hasCameraHardware || !hasCameraPermission) {
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
                SurfaceViewRenderer(ctx).apply {
                    WebRtcManager.attachLocalRenderer(this)
                }
            }
        )
    }
}
