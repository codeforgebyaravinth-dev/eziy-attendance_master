package `in`.eziy.attendancemaster.ui.components

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.io.File
import java.util.concurrent.Executors

@OptIn(ExperimentalGetImage::class)
@Composable
fun EziyFaceCameraDialog(
    onDismiss: () -> Unit,
    onPhotoCaptured: (File) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isFaceValidInOval by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Position face straight ahead with eyes open") }
    var isCapturing by remember { mutableStateOf(false) }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }

    var validFaceFrameCount by remember { mutableIntStateOf(0) }

    // ML Kit Face Detector with Landmarks & Eye Openness Classification
    val faceDetector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.35f)
            .build()
        FaceDetection.getClient(options)
    }

    fun isFullFaceValid(face: Face, imgWidth: Int, imgHeight: Int): Boolean {
        // 1. Center & Size Check
        val bounds = face.boundingBox
        val centerX = bounds.centerX().toFloat() / imgWidth.toFloat()
        val centerY = bounds.centerY().toFloat() / imgHeight.toFloat()
        val widthRatio = bounds.width().toFloat() / imgWidth.toFloat()

        val isCentered = centerX in 0.30f..0.70f && centerY in 0.20f..0.80f
        val isSizedWell = widthRatio >= 0.30f
        if (!isCentered || !isSizedWell) return false

        // 2. Head Pose Check (must face straight ahead)
        val rotY = face.headEulerAngleY // Turn left/right
        val rotZ = face.headEulerAngleZ // Tilt
        if (rotY !in -20f..20f || rotZ !in -18f..18f) return false

        // 3. Eye Openness Classification
        val leftEyeOpen = face.leftEyeOpenProbability
        val rightEyeOpen = face.rightEyeOpenProbability
        if (leftEyeOpen != null && leftEyeOpen < 0.40f) return false
        if (rightEyeOpen != null && rightEyeOpen < 0.40f) return false

        // 4. Facial Landmarks Verification (Nose + Eyes + Mouth must be detected)
        val hasLeftEye = face.getLandmark(FaceLandmark.LEFT_EYE) != null
        val hasRightEye = face.getLandmark(FaceLandmark.RIGHT_EYE) != null
        val hasNose = face.getLandmark(FaceLandmark.NOSE_BASE) != null
        val hasMouth = face.getLandmark(FaceLandmark.MOUTH_BOTTOM) != null ||
                       face.getLandmark(FaceLandmark.MOUTH_LEFT) != null ||
                       face.getLandmark(FaceLandmark.MOUTH_RIGHT) != null

        return hasLeftEye && hasRightEye && hasNose && hasMouth
    }

    fun takePicture() {
        if (isCapturing) return
        isCapturing = true
        statusText = "Full Face Verified! Capturing..."

        val file = File(context.cacheDir, "face_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onPhotoCaptured(file)
                    onDismiss()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("FaceCamera", "Capture failed: ${exception.message}", exception)
                    isCapturing = false
                    statusText = "Capture failed. Use manual button."
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // CameraX Live Preview
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        imageCapture = ImageCapture.Builder()
                            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                            .build()

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null && !isCapturing) {
                                val rotation = imageProxy.imageInfo.rotationDegrees
                                val image = InputImage.fromMediaImage(mediaImage, rotation)

                                // Calculate dimensions accounting for rotation
                                val imgW = if (rotation == 90 || rotation == 270) mediaImage.height else mediaImage.width
                                val imgH = if (rotation == 90 || rotation == 270) mediaImage.width else mediaImage.height

                                faceDetector.process(image)
                                    .addOnSuccessListener { faces ->
                                        val validFace = faces.firstOrNull { face ->
                                            isFullFaceValid(face, imgW, imgH)
                                        }

                                        if (validFace != null) {
                                            validFaceFrameCount++
                                            isFaceValidInOval = true
                                            statusText = "Full Face Verified! Hold still..."

                                            // Require 5 consecutive fully-valid face frames before triggering auto-capture
                                            if (validFaceFrameCount >= 5 && !isCapturing) {
                                                takePicture()
                                            }
                                        } else {
                                            validFaceFrameCount = 0
                                            isFaceValidInOval = false
                                            statusText = if (faces.isNotEmpty()) "Face camera directly with eyes open" else "Position face straight ahead inside oval"
                                        }
                                    }
                                    .addOnFailureListener {
                                        validFaceFrameCount = 0
                                        isFaceValidInOval = false
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageCapture,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            Log.e("FaceCamera", "Use case binding failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Face Oval Cutout Overlay
            Canvas(modifier = Modifier.fillMaxSize()) {
                val ovalWidth = size.width * 0.72f
                val ovalHeight = size.height * 0.48f
                val left = (size.width - ovalWidth) / 2f
                val top = (size.height - ovalHeight) / 2.3f

                val ovalPath = Path().apply {
                    addOval(androidx.compose.ui.geometry.Rect(left, top, left + ovalWidth, top + ovalHeight))
                }

                // Dark background cutout
                val outerPath = Path().apply {
                    addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
                    op(this, ovalPath, androidx.compose.ui.graphics.PathOperation.Difference)
                }

                drawPath(outerPath, color = Color.Black.copy(alpha = 0.65f))

                // Oval Ring Stroke (Green when valid face centered, Orange when not)
                val ringColor = if (isFaceValidInOval) Color(0xFF22C55E) else Color(0xFFF97316)
                drawOval(
                    color = ringColor,
                    topLeft = androidx.compose.ui.geometry.Offset(left, top),
                    size = androidx.compose.ui.geometry.Size(ovalWidth, ovalHeight),
                    style = Stroke(width = 6.dp.toPx())
                )
            }

            // Top Dismiss Button & Title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                Text(
                    text = "Live Face Detection",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                Spacer(modifier = Modifier.size(48.dp))
            }

            // Bottom Status Card & Manual Fallback Button
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isFaceValidInOval) Color(0xFF166534) else Color(0xFF1E293B),
                    contentColor = Color.White
                ) {
                    Text(
                        text = statusText,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }

                Button(
                    onClick = { takePicture() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.height(50.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Manual Capture", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Capture Manually", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
