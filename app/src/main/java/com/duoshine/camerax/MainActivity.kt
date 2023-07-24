package com.duoshine.camerax

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.ViewTreeObserver
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_main.*

import android.graphics.*
import android.os.*
import androidx.camera.core.ImageProxy
import java.lang.Math.*
import kotlin.math.pow

class MainActivity : AppCompatActivity() {
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private var TAG = "MainActivity_"
    private var avcEncoder: AvcEncoder? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var isPreviewing = true
    private lateinit var imageView: ImageView
    private lateinit var horizontalSeekBar: SeekBar
    private lateinit var verticalSeekBar: SeekBar
    private var scaleFactor = 1f
    private val REQUEST_PERMISSIONS = 100
    /**
     * 默认分辨率。他最好是一个比较适配主流机型的值，否则CameraX将会自动降低他,
     * 注意别写死了导致编码失败 demo中[customRecording]方法处理了size不支持的情况
     *
     */
    private var size: Size = Size(1920, 1080)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        requestPermission()
        initCamera()
        initView()
        val color_edit: EditText = findViewById(R.id.PH)
        color_edit.setCursorVisible(false)
        val PhText: EditText = findViewById(R.id.PH)
        PhText.setCursorVisible(false)
        val UaText: EditText = findViewById(R.id.UA)
        UaText.setCursorVisible(false)
//        val red_edit: EditText = findViewById(R.id.Red)
//        red_edit.setCursorVisible(false)
//        val green_edit: EditText = findViewById(R.id.Green)
//        green_edit.setCursorVisible(false)
//        val blue_edit: EditText = findViewById(R.id.Blue)
//        blue_edit.setCursorVisible(false)



        imageView = findViewById(R.id.rotate_img)
        horizontalSeekBar = findViewById<SeekBar>(R.id.horzontalSeekBar)
        verticalSeekBar = findViewById<SeekBar>(R.id.verticalSeekBar)
        // Set the max value of the seek bar to the width of the image
        imageView.post {
            val width = imageView.width
            horizontalSeekBar.max = width * 2
            verticalSeekBar.max = imageView.height * 2

        }

        setupSeekBarListeners(horzontalSeekBar, verticalSeekBar, imageView)

    }

    private fun setupSeekBarListeners(horizontalSeekBar: SeekBar, verticalSeekBar: SeekBar, imageView: ImageView) {
    // Set the progress of the seek bar to the initial position of the image
    imageView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
        override fun onGlobalLayout() {
            imageView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            val initialPosition = imageView.left
            horizontalSeekBar.progress = horizontalSeekBar.max / 2
            verticalSeekBar.progress = verticalSeekBar.max / 2
        }
    })

    // Add a listener to the seek bar to move the image horizontally
    horizontalSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(horizontalSeekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            imageView.translationX = progress.toFloat()
        }

        override fun onStartTrackingTouch(horizontalSeekBar: SeekBar?) {}

        override fun onStopTrackingTouch(horizontalSeekBar: SeekBar?) {}
    })

    verticalSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(verticalSeekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            imageView.translationY = progress.toFloat()
        }

        override fun onStartTrackingTouch(verticalSeekBar: SeekBar?) {}

        override fun onStopTrackingTouch(verticalSeekBar: SeekBar?) {}
    })
}

    
    private fun initView() {
//        h264Record.setOnClickListener {
//            customRecording()
//        }
//
//        disableH264Record.setOnClickListener {
//            customDisableStopRecording()
//        }
    }

    /**
     * 结束录制
     */
    private fun customDisableStopRecording() {
        //取消分析器
        imageAnalysis!!.clearAnalyzer()
        //停止h264编码
        avcEncoder?.stop()
        avcEncoder = null
    }

    /**
     * 开始录制
     */
//    @SuppressLint("RestrictedApi")
//    private fun customRecording() {
//        //保存为h264
//        val file = File(
//            getExternalFilesDir(Environment.DIRECTORY_MOVIES)!!.path,
//            "${System.currentTimeMillis()}.h264"
//        )
//        if (!file.exists()) {
//            file.createNewFile()
//        }
//
//        imageAnalysis?.let {
//            it.setAnalyzer(mainExecutor, ImageAnalysis.Analyzer { image ->
//                if (avcEncoder == null) {
//                    avcEncoder = AvcEncoder()
//                    //如果设置的默认分辨率不支持，则使用自动选择的分辨率，否则编码器将会报错
//                    if (image.width != size.width) {
//                        Log.d(TAG, "customRecording: 不支持默认分辨率，新的分辨率为：${image.width }")
//                        size = Size(image.width,image.height)
//                    }
//                    avcEncoder?.config(size, file)
//                    avcEncoder?.start()
//                }
//                Log.d(TAG, "width: ${image.width}")
//                Log.d(TAG, "height: ${image.height}")
//                //编码为h624
//                avcEncoder?.addPlanes(ImageUtil.yuv_420_888toNv21(image))
//                image.close()
//            })
//        }
//    }

    private fun initCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        //请求 CameraProvider 后，请验证它能否在视图创建后成功初始化。以下代码展示了如何执行此操作：
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    @SuppressLint("RestrictedApi", "UnsafeOptInUsageError")
    fun bindPreview(cameraProvider: ProcessCameraProvider) {
        //预览
        var preview: Preview = Preview.Builder()
            .build()

//        选择相机
        var cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK) //后置
            .build()

//        提供previewView预览控件
        preview.setSurfaceProvider(previewView.surfaceProvider)
        val catchButton: Button = findViewById(R.id.start)
        catchButton.setOnClickListener {
            if (isPreviewing) {
                imageAnalysis?.clearAnalyzer()
                cameraProvider.unbind(preview)
                isPreviewing = false
            } else {
                var camera = cameraProvider.bindToLifecycle(this as LifecycleOwner,
                    cameraSelector,
                    imageAnalysis,
                    preview)
                isPreviewing = true

                setImageAnalysis(imageAnalysis!!)
            }
        }
//        图片分析
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetResolution(size) //设置分辨率
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)  //阻塞模式，setAnalyzer过于耗时会导致预览卡顿
            .setTargetRotation(Surface.ROTATION_90)
            .build()

        setImageAnalysis(imageAnalysis!!)

        //        在绑定之前 你应该先解绑
                cameraProvider.unbindAll()
                var camera = cameraProvider.bindToLifecycle(
                    this as LifecycleOwner,
                    cameraSelector,
                    imageAnalysis,
                    preview
                )
    }

    fun setImageAnalysis(imageAnalysis: ImageAnalysis) {
        imageAnalysis?.setAnalyzer(mainExecutor, object : ImageAnalysis.Analyzer {
            override fun analyze(image: ImageProxy) {
                val buffer = image.planes[0].buffer
                val rowStride = image.planes[0].rowStride
                val pixelStride = image.planes[0].pixelStride
                val centerX = image.width / 2 // 获取图像中心点横坐标
                val centerY = image.height / 2 // 获取图像中心点纵坐标
                val buttonLeft = centerX - centerButton.width / 2 // 计算Button的左边界
                val buttonRight = centerX + centerButton.width / 2 // 计算Button的右边界
                val buttonTop = centerY - centerButton.height / 2 // 计算Button的上边界
                val buttonBottom = centerY + centerButton.height / 2 // 计算Button的下边界

                //将centerButton 分成左右两个区域，分别计算中心点RGB
                val leftPixel =
                    (buttonTop + centerButton.height / 2) * rowStride + (buttonLeft + centerButton.width / 4) * pixelStride
                val leftRedAvg = (buffer.get(leftPixel).toInt() and 0xFF)
                val leftGreenAvg = (buffer.get(leftPixel + 1).toInt() and 0xFF)
                val leftBlueAvg = (buffer.get(leftPixel + 2).toInt() and 0xFF)
                val rightPixel =
                    (buttonTop + centerButton.height / 2) * rowStride + (buttonRight - centerButton.width / 4) * pixelStride
                val rightRedAvg = (buffer.get(rightPixel).toInt() and 0xFF)
                val rightGreenAvg = (buffer.get(rightPixel + 1).toInt() and 0xFF)
                val rightBlueAvg = (buffer.get(rightPixel + 2).toInt() and 0xFF)
                Log.d("RGB", "Left: R=$leftRedAvg G=$leftGreenAvg B=$leftBlueAvg")
                Log.d("RGB", "Right: R=$rightRedAvg G=$rightGreenAvg B=$rightBlueAvg")

                // PH RGB显示
                //将获取到的RGB像素值，分别写入textview的Red，Green，Blue内
//                val L_red_edit: EditText = findViewById(R.id.Red)
//                L_red_edit.setText(leftRedAvg.toString())
//                val L_green_edit: EditText = findViewById(R.id.Green)
//                L_green_edit.setText(leftGreenAvg.toString())
//                val L_blue_edit: EditText = findViewById(R.id.Blue)
//                L_blue_edit.setText(leftBlueAvg.toString())

                val phColor = mapOf(
//                    intArrayOf(0, 4, 206) to 1.0f,
//                    // intArrayOf(4, 41, 253) to 2.0f,
//                    intArrayOf(7, 64, 250) to 3.0f,
//                    intArrayOf(79, 83, 255) to 4.0f,
                    intArrayOf(223,210,182) to 5.0f,   // *
                    intArrayOf(233,216,173) to 5.5f, // *
                    intArrayOf(237,225,160 ) to 6.0f,  // *
                    intArrayOf(230,228,143) to 6.5f,   // *
                    intArrayOf(227,225,132) to 7.0f, // *
                     intArrayOf(225, 228, 112) to 7.5f,
                    intArrayOf(221, 227, 95) to 8.0f,
                    intArrayOf(210, 231, 90) to 8.5f,
                    intArrayOf(208, 233, 89) to 9.0f,
                    // intArrayOf(163, 0, 1) to 10.0f,
                    // intArrayOf(110, 3, 0) to 11.0f,
//                    intArrayOf(78, 25, 28) to 12.0f
                )
                // BGR顺序传入
                val color = intArrayOf(leftGreenAvg, leftBlueAvg, leftRedAvg)
                val PhValue = getPhValueFloat(color, phColor)
                val PhText: EditText = findViewById(R.id.PH)
//                PhText.setText("PH:" + String.format("%.1f", PhValue))

                val uaColor = mapOf(
//                    intArrayOf(205,152,58) to 0.0f,   // *
//                    intArrayOf(89,111,50) to 10.0f, // *
//                    intArrayOf(65,107,50 ) to 50.0f,  // *
//                    intArrayOf(54,104,52) to 100.0f,   // *
//                    intArrayOf(37,95,50) to 250.0f, // *
                    intArrayOf(205,120,58) to 200.0f,
                    intArrayOf(185,118,56) to 205.0f,
                    intArrayOf(162,115,55) to 212.0f,
                    intArrayOf(140,113,54) to 221.0f,
                    intArrayOf(120,112,52) to 233.0f,
                    intArrayOf(90,111,51) to 250.0f,
                    intArrayOf(70,109,50) to 270.0f,
                    intArrayOf(65,108,51) to 300.0f,
                    intArrayOf(60,106,52) to 323.0f,
                    intArrayOf(58,105,51) to 353.2f,
//                    intArrayOf(56,100,50) to 373.0f,
//                    intArrayOf(52,96,49) to 400.0f,


                )
                // UA RGB显示
//                val R_red_edit: EditText = findViewById(R.id.Red2)
//                R_red_edit.setText(rightRedAvg.toString())
//                val R_green_edit: EditText = findViewById(R.id.Green2)
//                R_green_edit.setText(rightGreenAvg.toString())
//                val R_blue_edit: EditText = findViewById(R.id.Blue2)
//                R_blue_edit.setText(rightBlueAvg.toString())

                val color2 = intArrayOf(rightGreenAvg, rightBlueAvg, rightRedAvg)
                val UaValue = getPhValueFloat(color2, uaColor)
                val UaText: EditText = findViewById(R.id.UA)
//                UaText.setText("UA:" + String.format("%.1f", UaValue))
                image.close()
            }
        })
    }

    /**
     * 获取颜色值对应的PH值
     * @param color 颜色值，BGR顺序
     * @param phColor PH值对应的颜色值，BGR顺序
     * @return Float PH值
     */
    fun getPhValueFloat(color: IntArray, phColor: List<IntArray>): Float{
        // 计算颜色值与每个PH值对应颜色值的欧氏距离
        val dists = mutableListOf<Float>()
        for (i in phColor.indices) {
            val dist = sqrt(
                ((color[0] - phColor[i][0]).toDouble().pow(2) +
                        (color[1] - phColor[i][1]).toDouble().pow(2) +
                        (color[2] - phColor[i][2]).toDouble().pow(2))
            ).toFloat()
            dists.add(dist)
        }
        // 找到距离最小的两个PH值
        val minIndex1 = dists.indexOf(dists.minOrNull())
        val dist1 = dists[minIndex1]
        dists[minIndex1] = 999999f
        val minIndex2 = dists.indexOf(dists.minOrNull())
        val dist2 = dists[minIndex2]
        // 计算最终的PH值
        val finalPh: Float = if (minIndex1 <= minIndex2) {
            minIndex1 + abs(minIndex1 - minIndex2) * (dist1 / (dist1 + dist2))
        } else {
            minIndex1 - abs(minIndex1 - minIndex2) * (dist1 / (dist1 + dist2))
        }
        // 返回PH值
        return finalPh + 1
    }


    /**
     * 获取颜色值对应的PH值
     * @param color 颜色值，BGR顺序
     * @param phColor PH值对应的颜色值和PH值的映射
     * @return Float PH值
     */
    fun getPhValueFloat(color: IntArray, phColor: Map<IntArray, Float>): Float{
        // 计算颜色值与每个PH值对应颜色值的欧氏距离
        val dists = mutableListOf<Float>()
        for (entry in phColor.entries) {
            val key = entry.key // 获取颜色值
            val value = entry.value // 获取对应的PH值
            val dist = sqrt(
                ((color[0] - key[0]).toDouble().pow(2) + // 计算欧氏距离
                        (color[1] - key[1]).toDouble().pow(2) +
                        (color[2] - key[2]).toDouble().pow(2))
            ).toFloat()
            dists.add(dist)
        }
        // 找到距离最小的两个PH值
        val minIndex1 = dists.indexOf(dists.minOrNull())
        val dist1 = dists[minIndex1]
        dists[minIndex1] = 999999f
        val minIndex2 = dists.indexOf(dists.minOrNull())
        val dist2 = dists[minIndex2]
        // 计算最终的PH值
        val finalPh: Float = if (minIndex1 <= minIndex2) {
            val key1 = phColor.keys.toList()[minIndex1]
            val key2 = phColor.keys.toList()[minIndex2]
            val ph1 = phColor[key1]!!
            val ph2 = phColor[key2]!!
            ph1 + abs(ph1 - ph2) * (dist1 / (dist1 + dist2))
        } else {
            val key1 = phColor.keys.toList()[minIndex1]
            val key2 = phColor.keys.toList()[minIndex2]
            val ph1 = phColor[key1]!!
            val ph2 = phColor[key2]!!
            ph1 - abs(ph1 - ph2) * (dist1 / (dist1 + dist2))
        }
        // 返回PH值
        return finalPh
    }



    /*
      请求授权
       */
    private fun requestPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            val permissions = arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO

            )
            val permissionsList = ArrayList<String>()
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        permission
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    permissionsList.add(permission)
                }
            }
            if (permissionsList.size != 0) {
                val permissionsArray = permissionsList.toTypedArray()
                ActivityCompat.requestPermissions(
                    this, permissionsArray,
                    22
                )
            }
        }
    }
}