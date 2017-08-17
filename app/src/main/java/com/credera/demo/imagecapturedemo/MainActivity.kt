package com.credera.demo.imagecapturedemo

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.SeekBar
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*


const val REQUEST_IMAGE_CAPTURE = 1

class MainActivity : AppCompatActivity() {

    lateinit var currentPhotoPath: String
    lateinit var file: Uri
    lateinit var srcOrig: Mat
    lateinit var src: Mat
    lateinit var errorMsg: String

    var scaleFactor: Int = 0

    private val callback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> {
                    Log.i(MainActivity::javaClass.name, "OpenCV loaded.")
                    val beforeImage = BitmapFactory.decodeResource(resources, R.drawable.receipt_image)

                    setContrast(beforeImage)
                }
                else ->
                    super.onManagerConnected(status)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Load the original image
        Glide.with(this).load(R.drawable.receipt_image).into(receipt_before_imageview)

        // Progress bar for changing between original and processed
        seekBar.progress = 100;

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    receipt_after_imageview.alpha = (progress * 0.01).toFloat()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
            }
        })


//        capture_image_btn.setOnClickListener {
//            takePhoto(it)
//        }
    }


    override fun onResume() {
        super.onResume()

        // Process the original image
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0, this, callback)
    }

    fun takePhoto(view: View) {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        file = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, getOutputMediaFile())

        intent.putExtra(MediaStore.EXTRA_OUTPUT, file)

        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    fun getOutputMediaFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val image = File.createTempFile(
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
//        mCurrentPhotoPath = image.absolutePath
        return image
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            try {
                val imageStream: InputStream = contentResolver.openInputStream(file)
                val selectedImage: Bitmap = BitmapFactory.decodeStream(imageStream)
                srcOrig = Mat(selectedImage.height, selectedImage.width, CvType.CV_8UC4)
                src = Mat()

                Utils.bitmapToMat(selectedImage, srcOrig)

                scaleFactor = calcScaleFactor(srcOrig.rows(), srcOrig.cols())

                Imgproc.resize(srcOrig, src,
                        Size((srcOrig.rows() / scaleFactor).toDouble(), (srcOrig.cols() / scaleFactor).toDouble()))

                setContrast(selectedImage)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        }
    }

    fun setContrast(bitmap: Bitmap) {

        // Read the bitmap
        val rgba = Mat()
        Utils.bitmapToMat(bitmap, rgba)

        // Convert to grayscale
        val edges = rgba//Mat(rgba.size(), CvType.CV_8UC1)
        Imgproc.cvtColor(rgba, edges, Imgproc.COLOR_BGR2GRAY, 4)

        // Blurs
        Imgproc.GaussianBlur(edges, edges, Size(3.0, 3.0), 0.0)
//        Imgproc.medianBlur(edges, edges, 3)

        // Filters
//        standardThreshold(edges)
//        thresholdToZero(edges)
//        thresholdToZeroInverted(edges)
//        adaptiveGaussianThreshold(edges)
//        adaptiveGaussianWithOtsu(edges)
//        adaptiveMean(edges)
//        adaptiveGaussianThreshold(edges)
//        adaptiveMean(edges)

        adaptiveMean(edges)

        // Convert the Mat to a bitmap
        val result = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(edges, result)

        BitmapHelper.showBitmap(this, result, receipt_after_imageview)
    }

    fun standardThreshold(image: Mat) {
        //        Imgproc.GaussianBlur(edges, edges, Size(5.0, 5.0), 0.0)
//        Imgproc.GaussianBlur(image, image, Size(1.0, 1.0), 0.0, 0.0)
//        Imgproc.threshold(edges, edges, 128.0, 255.0, Imgproc.THRESH_BINARY)
        Imgproc.threshold(image, image, 120.0, 255.0, Imgproc.THRESH_BINARY)
//        Imgproc.Canny(edges, edges, 80.0, 100.0)
    }

    fun adaptiveGaussianThreshold(image: Mat) {
        Imgproc.adaptiveThreshold(image, image, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 11, 2.0)
    }

    fun adaptiveGaussianWithOtsu(image: Mat) {
//        val blur = Mat()
//        Imgproc.GaussianBlur(image, blur, Size(5.0, 5.0), 0.0)
        Imgproc.threshold(image, image, 0.0, 255.0, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU)
    }

    fun thresholdToZero(image: Mat) {
        val threshold = 128.0 // TODO: Calculate from the image?
        Imgproc.threshold(image, image, threshold, 255.0, Imgproc.THRESH_TOZERO)
    }

    fun thresholdToZeroInverted(image: Mat) {
        val threshold = 228.0 // TODO: Calculate from the image?
        Imgproc.threshold(image, image, threshold, 255.0, Imgproc.THRESH_TOZERO_INV)
    }

    fun adaptiveMean(image: Mat) {
        Imgproc.adaptiveThreshold(image, image, 255.0, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 27, 12.0)
    }

    fun calcScaleFactor(rows: Int, cols: Int): Int {
        var idealRow = 0
        var idealCol = 0

        if (rows < cols) {
            idealRow = 240
            idealCol = 320
        } else {
            idealCol = 240
            idealRow = 320
        }

        val value = Math.min(rows / idealRow, cols / idealCol)

        if (value <= 0) {
            return 1
        } else {
            return value
        }
    }

}
