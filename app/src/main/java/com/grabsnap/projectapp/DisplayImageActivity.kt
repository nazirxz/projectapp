package com.grabsnap.projectapp

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.grabsnap.projectapp.databinding.ActivityImageBinding
import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.*
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import java.text.SimpleDateFormat
import java.util.*
import com.itextpdf.text.Document
import com.itextpdf.text.Image
import com.itextpdf.text.PageSize
import com.itextpdf.text.pdf.PdfWriter
import com.yalantis.ucrop.UCrop
import java.io.*

class DisplayImageActivity : AppCompatActivity() {

    private val REQUEST_CAMERA_PERMISSION = 1
    private val CAMERA_REQUEST_CODE = 2
    private lateinit var binding: ActivityImageBinding
    private val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 3
    private lateinit var photoBitmap: Bitmap
    private val UCROP_REQUEST_CODE = 123
    private var photoFile: File? = null
    private var currentPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.printButton.setOnClickListener {
            if (::photoBitmap.isInitialized) {
                val trueColorBitmap = convertToTrueColor(photoBitmap)
                if (trueColorBitmap != null) {
                    createPdfAndPrint(trueColorBitmap)
                }
            } else {
                showToast("No photo available to print")
            }
        }

        binding.displayImageView.setOnClickListener {
            if (hasCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }
        binding.backIcon.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.saveImageButton.setOnClickListener {
            if (::photoBitmap.isInitialized) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    saveImageToExternalStorage(photoBitmap)
                } else {
                    // Request WRITE_EXTERNAL_STORAGE permission
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_EXTERNAL_STORAGE_REQUEST_CODE)
                }
            } else {
                // Lakukan sesuatu jika photoBitmap belum diinisialisasi
                // Contoh: Tampilkan pesan kesalahan
                showToast("Photo has not been initialized")
            }
        }

        binding.backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun createPdfAndPrint(image: Bitmap) {
        val trueColorBitmap = convertToTrueColor(image)
        if (trueColorBitmap != null) {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "IMG_$timeStamp.pdf"
            val pdfFile = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

            try {
                val document = Document()
                val outputStream = FileOutputStream(pdfFile)
                val writer = PdfWriter.getInstance(document, outputStream)
                document.open()

                val imageInstance = Image.getInstance(getByteFromBitmap(trueColorBitmap))

                // Calculate new image dimensions
                val pageSize = document.pageSize
                val pageWidth = pageSize.width
                val pageHeight = pageSize.height
                val imageWidth = imageInstance.width
                val imageHeight = imageInstance.height
                val maxImageWidth = pageWidth * 0.8f // Adjust the percentage as needed
                val maxImageHeight = pageHeight * 0.8f // Adjust the percentage as needed

                // Calculate the scaling factor
                val widthScale = maxImageWidth / imageWidth
                val heightScale = maxImageHeight / imageHeight
                val scaleFactor = if (widthScale < heightScale) widthScale else heightScale

                // Scale the image
                imageInstance.scalePercent((scaleFactor * 100).toFloat())

                // Center the image on the page
                val x = (pageWidth - imageInstance.scaledWidth) / 2
                val y = (pageHeight - imageInstance.scaledHeight) / 2

                // Add the image to the document
                imageInstance.setAbsolutePosition(x, y)
                document.add(imageInstance)

                document.close()
                printPdf(pdfFile)
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Failed to create and print PDF")
            }
        }
    }
    private fun getByteFromBitmap(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
    private fun printPdf(pdfFile: File) {
        val printManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
        val jobName = getString(R.string.app_name) + " Document"
        val printAdapter = object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?,
                newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?,
                callback: LayoutResultCallback?,
                extras: Bundle?
            ) {
                if (cancellationSignal?.isCanceled == true) {
                    callback?.onLayoutCancelled()
                    return
                }

                val info = PrintDocumentInfo.Builder("print_output.pdf")
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()

                callback?.onLayoutFinished(info, newAttributes != oldAttributes)
            }

            override fun onWrite(
                pages: Array<out PageRange>?,
                destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?,
                callback: WriteResultCallback?
            ) {
                val input = FileInputStream(pdfFile)

                val output = FileOutputStream(destination?.fileDescriptor)
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } >= 0) {
                    output.write(buffer, 0, bytesRead)
                }

                input.close()
                output.close()

                callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            }
        }

        printManager.print(
            jobName,
            printAdapter,
            PrintAttributes.Builder().build()
        )
    }

    private fun displayImage(bitmap: Bitmap) {
        photoBitmap = bitmap
        // Tampilkan foto yang sudah diambil ke ImageView atau lakukan operasi lain yang diperlukan
        binding.displayImageView.setImageBitmap(photoBitmap)
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, save the image
                val photo: Bitmap? = intent.getParcelableExtra("photo")
                if (photo != null) {
                    saveImageToExternalStorage(photo)
                }
            } else {
                // Permission denied, show message to the user
                showToast("Storage permission denied")
            }
        }
    }
    private fun hasCameraPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }

    private fun openCamera() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                photoFile = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.grabsnap.projectapp.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, CAMERA_REQUEST_CODE)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    private fun openUCropActivity(imageUri: Uri) {
        val destinationUri = Uri.fromFile(createTempFile())
        val options = UCrop.Options()
        options.withAspectRatio(1f, 1f) // Sesuaikan rasio jika diperlukan
        options.setCompressionQuality(80)
        UCrop.of(imageUri, destinationUri)
            .withOptions(options)
            .start(this, UCROP_REQUEST_CODE)
    }

    private fun saveImageToExternalStorage(image: Bitmap) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "IMG_$timeStamp.png"

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }

        val resolver = contentResolver
        var imageUri: Uri? = null
        try {
            val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val uri = resolver.insert(contentUri, contentValues)
            if (uri != null) {
                val imageOutputStream = resolver.openOutputStream(uri)
                image.compress(Bitmap.CompressFormat.PNG, 100, imageOutputStream)
                imageOutputStream?.close()
                showToast("Image saved successfully in external storage as $fileName")
            } else {
                showToast("Failed to save image")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            showToast("Failed to save image")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    private fun convertToTrueColor(negativeImage: Bitmap?): Bitmap? {
        if (negativeImage == null) {
            return null
        }
        val width = negativeImage.width
        val height = negativeImage.height
        val trueColorImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val matrix = ColorMatrix().apply {
            set(floatArrayOf(
                -1f, 0f, 0f, 0f, 225f,
                0f, -1f, 0f, 0f, 200f,
                0f, 0f, -1f, 0f, 200f,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        val filter = ColorMatrixColorFilter(matrix)
        val paint = Paint().apply {
            colorFilter = filter
        }

        val canvas = Canvas(trueColorImage)
        canvas.drawBitmap(negativeImage, 0f, 0f, paint)

        return trueColorImage
    }
    private fun createTempFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_$timeStamp"
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageFile = photoFile
            if (imageFile != null) {
                val imageUri = Uri.fromFile(imageFile)
                openUCropActivity(imageUri)
            }
        } else if (requestCode == UCROP_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(data!!)
            if (resultUri != null) {
                // Dekode hasil UCrop
                val imageBitmap = BitmapFactory.decodeFile(resultUri.path)

                // Kompresi gambar sebelum konversi ke warna sejati
                val compressedBitmap = compressBitmap(imageBitmap, 80) // Atur kualitas kompresi di sini

                val trueColorBitmap = convertToTrueColor(compressedBitmap)
                if (trueColorBitmap != null) {
                    displayImage(trueColorBitmap) // Menampilkan gambar ke aplikasi
                }
            }
        }
    }
    private fun compressBitmap(bitmap: Bitmap, quality: Int): Bitmap {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, quality, stream)
        return BitmapFactory.decodeStream(ByteArrayInputStream(stream.toByteArray()))
    }
}


