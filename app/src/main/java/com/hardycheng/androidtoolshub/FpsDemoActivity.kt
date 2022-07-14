package com.hardycheng.androidtoolshub

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CaptureRequest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.Range
import android.view.Surface.ROTATION_0
import android.view.View
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.hardycheng.androidtoolshub.databinding.ActivityFpsDemoBinding
import com.hardycheng.androidtoolshub.tool.FpsUtil
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class FpsDemoActivity : AppCompatActivity() {

    companion object {
        private val TAG = FpsDemoActivity::class.simpleName
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        private const val PERMISSION_REQUEST_CODE = 0x1001
    }

    lateinit var binding: ActivityFpsDemoBinding
    private var cameraInit = false
    private val fpsUtil = FpsUtil(debug = true)

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    recreate()
                } else {
                    finish()
                }
                return
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFpsDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        if(!checkPermission()){
            requestPermissions(listOf(Manifest.permission.CAMERA).toTypedArray(), PERMISSION_REQUEST_CODE)
        }
        else startCamera()
    }

    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startCamera() {
        cameraInit = false

        val cameraExecutor = ContextCompat.getMainExecutor(this)

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // Get screen metrics used to setup camera for full screen resolution
        val metrics = resources.displayMetrics
        Log.i(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

        val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
        Log.i(TAG, "Preview aspect ratio: $screenAspectRatio")

        val imageBuilder = ImageAnalysis.Builder()
//        Camera2Interop.Extender(imageBuilder)
//            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(60, 60))
//            .setCaptureRequestOption(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 1)
        val imageAnalyzer = imageBuilder
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//            .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
//            .setImageQueueDepth(60)
            .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_RGBA_8888)
//            .setOutputImageFormat(OUTPUT_IMAGE_FORMAT_YUV_420_888)
            .setTargetAspectRatio(screenAspectRatio)
            .setOutputImageRotationEnabled(true)
            .setTargetRotation(ROTATION_0)
            .build()
            .also{
                it.setAnalyzer(cameraExecutor) { imageProxy ->
                    val planes = imageProxy.planes
//                        if(imageProxy.format == OUTPUT_IMAGE_FORMAT_YUV_420_888) {
                    if(planes.size > 1) {
                        //OUTPUT_IMAGE_FORMAT_YUV_420_888
                        val yBuffer = planes[0].buffer // Y
                        val vuBuffer = planes[2].buffer // VU

                        val ySize = yBuffer.remaining()
                        val vuSize = vuBuffer.remaining()

                        val nv21 = ByteArray(ySize + vuSize)

                        yBuffer.get(nv21, 0, ySize)
                        vuBuffer.get(nv21, ySize, vuSize)

                        val yuvImage = YuvImage(
                            nv21, ImageFormat.NV21,
                            imageProxy.width, imageProxy.height, null
                        )
                        val out = ByteArrayOutputStream()
                        yuvImage.compressToJpeg(
                            Rect(0, 0, yuvImage.width, yuvImage.height), 100, out
                        )
                        val imageBytes = out.toByteArray()
                        draw(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size))
                    } else {
                        //OUTPUT_IMAGE_FORMAT_RGBA_8888
                        val buffer = planes[0].buffer
                        // use Bitmap.Config.ARGB_8888 instead of type is OK
                        val bitmap = Bitmap.createBitmap(imageProxy.width, imageProxy.height, Bitmap.Config.ARGB_8888)
                        bitmap.copyPixelsFromBuffer(buffer)
                        draw(bitmap)
                    }
                    imageProxy.close()
                    if (!cameraInit) {
                        cameraInit = true
                    }
                    fpsUtil.tick()
                }
            }

        cameraProviderFuture.addListener({
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(this
                        , cameraSelector, imageAnalyzer)
            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun draw(bitmap: Bitmap){
        try {
            val canvas = binding.canvas.lockCanvas()
            if (canvas != null) {
                canvas.drawColor(Color.BLACK)
                val srcWidth = bitmap.width
                val srcHeight = bitmap.height
                val ratio = 1.0 * srcWidth / srcHeight
                val viewWidth = binding.canvas.width
                val viewHeight = binding.canvas.height
                var destWidth = viewWidth
                var destHeight = (destWidth / ratio).toInt()
                if(destWidth > viewWidth){
                    destHeight = (1.0 * destHeight / destWidth * viewWidth).toInt()
                    destWidth = viewWidth
                }
                if(destHeight > viewHeight){
                    destWidth = (1.0 * destWidth / destHeight * viewHeight).toInt()
                    destHeight = viewHeight
                }
                val destLeft = (viewWidth - destWidth) / 2
                val destTop = (viewHeight - destHeight) / 2
                val srcRect = Rect(0, 0, srcWidth, srcHeight)
                val destRect = Rect(destLeft, destTop, destLeft+destWidth, destTop+destHeight)

                canvas.drawBitmap(bitmap, srcRect, destRect, null)

                val paint = Paint()
                paint.style = Paint.Style.FILL_AND_STROKE
                paint.color = Color.WHITE
                paint.textSize = 32F
                paint.textAlign = Paint.Align.LEFT

                canvas.drawText("FPS ${fpsUtil.fps}"
                    , 10f, 10f-paint.fontMetricsInt.ascent, paint)
                binding.canvas.unlockCanvasAndPost(canvas)

                Log.v(TAG, "draw: view $viewWidth x $viewHeight, src $srcWidth x $srcHeight"
                        + ", dest $destWidth x $destHeight ($destLeft, $destTop), ratio $ratio")
            }

        } catch (e: Exception){
            Log.e(TAG, "draw err: " + e.message)
        }
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            baseContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }
}