package com.credera.demo.imagecapturedemo

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.Toast
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.utils.Converters
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


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

        Glide.with(this).load(R.drawable.receipt_image).into(receipt_before_imageview)

        // Testing .gitignore.

//        capture_image_btn.setOnClickListener {
//            takePhoto(it)
//        }
    }

    override fun onResume() {
        super.onResume()
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
        val edges = Mat(rgba.size(), CvType.CV_8UC1)
        Imgproc.cvtColor(rgba, edges, Imgproc.COLOR_BGR2GRAY, 4)
//        Imgproc.GaussianBlur(edges, edges, Size(5.0, 5.0), 0.0)
        Imgproc.GaussianBlur(edges, edges, Size(1.0, 1.0), 0.0, 0.0)
//        Imgproc.threshold(edges, edges, 128.0, 255.0, Imgproc.THRESH_BINARY)
        Imgproc.threshold(edges, edges, 120.0, 255.0, Imgproc.THRESH_BINARY)
//        Imgproc.Canny(edges, edges, 80.0, 100.0)

        val result = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(edges, result)
        BitmapHelper.showBitmap(this, result, receipt_after_imageview)
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
