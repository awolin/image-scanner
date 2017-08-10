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
                LoaderCallbackInterface.SUCCESS ->
                        Log.i(MainActivity::javaClass.name, "OpenCV loaded.")
                else ->
                        super.onManagerConnected(status)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        capture_image_btn.setOnClickListener {
            takePhoto(it)
        }
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
//        val storageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//
//        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
//        val imageFileName = "JPEG_${timeStamp}_"
//
//        return File("${storageDirectory.path}${File.pathSeparator}$imageFileName.jpg")
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
//            recepit_imageview.setImageURI(file)
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
        Imgproc.GaussianBlur(edges, edges, Size(5.0, 5.0), 0.0)
        Imgproc.threshold(edges, edges, 128.0, 255.0, Imgproc.THRESH_BINARY)
//        Imgproc.Canny(edges, edges, 80.0, 100.0)

        val result = Bitmap.createBitmap(edges.cols(), edges.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(edges, result)
        BitmapHelper.showBitmap(this, result, recepit_imageview)
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
//
//    fun getPage() {
//        ProcessImageTask().execute()
//    }
//
//    fun processImageForKMeans(rows: Int, cols: Int): Mat {
//        val samples = Mat(rows * cols, 3, CvType.CV_32F)
//
//        for (y in 0..rows) {
//            for (x in 0..cols) {
//                for (z in 0..3) {
//                    samples.put(x + y * cols, z, src.get(y, x)[z])
//                }
//            }
//        }
//
//        return samples
//    }
//
//    fun applyKMeansAlgorithm(samples: Mat): Bitmap? {
//        val clusterCount = 2
//        val attempts = 5
//        val labels = Mat()
//        val centers = Mat()
//
//        // Apply kmeans algorithm to retrieve cluster count.
//        Core.kmeans(samples, clusterCount, labels,
//                TermCriteria(TermCriteria.MAX_ITER or TermCriteria.EPS, 10000, 0.0001), attempts,
//                Core.KMEANS_PP_CENTERS, centers)
//
//        // Detect paper cluster and background clusters.
//        val dstCenter0 = calcWhiteDistance(centers.get(0, 0)[0], centers.get(0, 1)[0], centers.get(0, 2)[0])
//        val dstCenter1 = calcWhiteDistance(centers.get(1, 0)[0], centers.get(1, 1)[0], centers.get(1, 2)[0])
//        val paperCluster = if (dstCenter0 < dstCenter1) 0 else 1
//
//        // Display all foreground pixels as white and background pixels as black
//        val srcRes = Mat(src.size(), src.type())
//        val srcGray = Mat()
//        for (y in 0..src.rows()) {
//            for (x in 0..src.cols()) {
//                val clusterIdx = labels.get(x + y * src.cols(), 0)[0].toInt()
//                if (clusterIdx != paperCluster) {
//                    srcRes.put(y, x, 0.0, 0.0, 0.0, 255.0)
//                } else {
//                    srcRes.put(y, x, 255.0, 255.0, 255.0, 255.0)
//                }
//            }
//        }
//
//        // Apply Canny edge detection
//        Imgproc.cvtColor(src, srcGray, Imgproc.COLOR_BGR2GRAY)
//        Imgproc.Canny(srcGray, srcGray, 50.0, 150.0)
//        val contours = ArrayList<MatOfPoint>()
//        val hierarchy = Mat()
//
//        Imgproc.findContours(srcGray, contours, hierarchy,
//                Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE)
//
//        // Find the biggest contours
//        var index = 0
//        var maxim = Imgproc.contourArea(contours[0])
//        for (contourIdx in 1..contours.size) {
//            var temp = Imgproc.contourArea(contours.get(contourIdx))
//            if (maxim < temp) {
//                maxim = temp
//                index = contourIdx
//            }
//        }
//
//        val drawing = Mat.zeros(srcRes.size(), CvType.CV_8UC1)
//        Imgproc.drawContours(drawing, contours, index, Scalar(255.0))
//
//        // Detect the lines that contain the biggest contours, use intersection points to determine corners
//        val lines = Mat()
//        Imgproc.HoughLinesP(drawing, lines, 1.0, Math.PI / 180, 70, 30.0, 10.0)
//
//        val corners = ArrayList<Point>()
//        for (i in 0..lines.cols()) {
//            var j = i + 1
//            for (j in j..lines.cols()) {
//                val line1 = lines.get(0, i)
//                val line2 = lines.get(0, j)
//
//                val pt = findIntersection(line1, line2)
//                // Remove redundant points.
//                if (pt.x >= 0 && pt.y >= 0 && pt.x <=
//                        drawing.cols() && pt.y <= drawing.rows()) {
//                    if (!exists(corners, pt)) {
//                        corners.add(pt)
//                    }
//                }
//            }
//        }
//
//        // Detect the 4 corners, if not return null;
//        if (corners.size != 4) {
//            errorMsg = "Cannot detect perfect corners"
//            return null
//        }
//
//        sortCorners(corners)
//
//        // Determine the size of the resulting image.
//        val top = Math.sqrt(Math.pow(corners[0].x - corners[1].x, 2.0) + Math.pow(corners[0].y - corners[1].y, 2.0))
//
//        val right = Math.sqrt(Math.pow(corners[1].x - corners[2].x, 2.0) + Math.pow(corners[1].y - corners[2].y, 2.0))
//
//        val bottom = Math.sqrt(Math.pow(corners[2].x - corners[3].x, 2.0) + Math.pow(corners[2].y - corners[3].y, 2.0))
//
//        val left = Math.sqrt(Math.pow(corners[3].x - corners[1].x, 2.0) + Math.pow(corners[3].y - corners[1].y, 2.0))
//        val quad = Mat.zeros(Size(Math.max(top, bottom), Math.max(left, right)), CvType.CV_8UC3)
//
//        // Add perspective transformation for image warp to occupy the entire space.
//        // Order of the points is key to apply a proper transformation.
//        val result_pts = ArrayList<Point>()
//        result_pts.add(Point(0.0, 0.0))
//        result_pts.add(Point(quad.cols().toDouble(), 0.0))
//        result_pts.add(Point(quad.cols().toDouble(), quad.rows().toDouble()))
//        result_pts.add(Point(0.0, quad.rows().toDouble()))
//
//        val cornerPts = Converters.vector_Point2f_to_Mat(corners)
//        val resultPts = Converters.vector_Point2f_to_Mat(result_pts)
//
//        val transformation = Imgproc.getPerspectiveTransform(cornerPts,
//                resultPts)
//        Imgproc.warpPerspective(srcOrig, quad, transformation,
//                quad.size())
//        Imgproc.cvtColor(quad, quad, Imgproc.COLOR_BGR2RGBA)
//
//        val bitmap = Bitmap.createBitmap(quad.cols(), quad.rows(),
//                Bitmap.Config.ARGB_8888)
//        Utils.matToBitmap(quad, bitmap)
//
//        return bitmap
//    }
//
//    fun sortCorners(corners: ArrayList<Point>) {
//        val top = ArrayList<Point>()
//        val bottom = ArrayList<Point>()
//
//        val center = Point()
//
//        for (i in 0..corners.size) {
//            center.x += corners[i].x / corners.size
//            center.y += corners[i].y / corners.size
//        }
//
//        for (i in 0..corners.size) {
//            if (corners[i].y < center.y)
//                top.add(corners[i])
//            else
//                bottom.add(corners[i])
//        }
//
//        corners.clear()
//
//        if (top.size == 2 && bottom.size == 2) {
//            val top_left = if (top[0].x > top[1].x) top[1] else top[0]
//            val top_right = if (top[0].x > top[1].x) top[0] else top[1]
//            val bottom_left = if (bottom[0].x > bottom[1].x) bottom[1] else bottom[0]
//            val bottom_right = if (bottom[0].x > bottom[1].x) bottom[0] else bottom[1]
//
//            top_left.x *= scaleFactor;
//            top_left.y *= scaleFactor;
//
//            top_right.x *= scaleFactor;
//            top_right.y *= scaleFactor;
//
//            bottom_left.x *= scaleFactor;
//            bottom_left.y *= scaleFactor;
//
//            bottom_right.x *= scaleFactor;
//            bottom_right.y *= scaleFactor;
//
//            corners.add(top_left)
//            corners.add(top_right)
//            corners.add(bottom_right)
//            corners.add(bottom_left)
//        }
//    }
//
//    fun findIntersection(line1: DoubleArray, line2: DoubleArray): Point {
//        val start_x1 = line1[0]
//        val start_y1 = line1[1]
//        val end_x1 = line1[2]
//        val end_y1 = line1[3]
//        val start_x2 = line2[0]
//        val start_y2 = line2[1]
//        val end_x2 = line2[2]
//        val end_y2 = line2[3]
//
//        val denominator = (start_x1 - end_x1) * (start_y2 - end_y2) - (start_y1 - end_y1) * (start_x2 - end_x2)
//
//        if (denominator != 0.0) {
//            val pt = Point()
//            pt.x = ((start_x1 * end_y1 - start_y1 * end_x1) *
//                    (start_x2 - end_x2) - (start_x1 - end_x1) *
//                    (start_x2 * end_y2 - start_y2 * end_x2)) /
//                    denominator
//
//            pt.y = ((start_x1 * end_y1 - start_y1 * end_x1) *
//                    (start_y2 - end_y2) - (start_y1 - end_y1) *
//                    (start_x2 * end_y2 - start_y2 * end_x2)) /
//                    denominator
//
//            return pt
//        } else {
//            return Point(-1.0, -1.0)
//        }
//    }
//
//    fun exists(corners: ArrayList<Point>, pt: Point): Boolean {
//        return (0..corners.size).any { Math.sqrt(Math.pow(corners[it].x - pt.x, 2.0) + Math.pow(corners[it].y - pt.y, 2.0)) < 10 }
//    }
//
//    fun calcWhiteDistance(r: Double, g: Double, b: Double): Double {
//        return Math.sqrt(Math.pow(255 - r, 2.0) + Math.pow(255 - g, 2.0) + Math.pow(255 - b, 2.0))
//    }
//
//    inner class ProcessImageTask : AsyncTask<Void, Void, Bitmap?>() {
//
//        override fun onPreExecute() {
//            super.onPreExecute()
//        }
//
//        override fun doInBackground(vararg params: Void?): Bitmap? {
//            val samples = processImageForKMeans(src.rows(), src.cols())
//
//            return applyKMeansAlgorithm(samples)
//        }
//
//        override fun onPostExecute(result: Bitmap?) {
//            super.onPostExecute(result)
//
//            if (result != null) {
//                recepit_imageview.setImageBitmap(result)
//            } else if (errorMsg != null) {
//                Toast.makeText(applicationContext, errorMsg, Toast.LENGTH_SHORT).show()
//            }
//        }

//    }
}
