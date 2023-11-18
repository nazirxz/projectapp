package com.grabsnap.projectapp

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.grabsnap.projectapp.databinding.ActivityImageBinding
import android.Manifest
import android.content.Context
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.*
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*
import com.itextpdf.text.Document
import com.itextpdf.text.Image
import com.itextpdf.text.pdf.PdfWriter
import java.io.*

class DisplayImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageBinding
    private val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val photo: Bitmap? = intent.getParcelableExtra("photo")
        binding.displayImageView.setImageBitmap(photo)

        binding.printButton.setOnClickListener {
            if (photo != null) {
                createPdfAndPrint(photo)
            }
        }

        binding.backIcon.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.saveImageButton.setOnClickListener {
            if (photo != null) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    saveImageToExternalStorage(photo)
                } else {
                    // Request WRITE_EXTERNAL_STORAGE permission
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_EXTERNAL_STORAGE_REQUEST_CODE)
                }
            }
        }

        binding.backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
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

    private fun saveImageToExternalStorage(image: Bitmap) {
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val timeStamp = System.currentTimeMillis()
        val fileName = "IMG_$timeStamp.png"

        try {
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }

            val imageFile = File(storageDir, fileName)
            val imageOutputStream = FileOutputStream(imageFile)
            image.compress(Bitmap.CompressFormat.PNG, 100, imageOutputStream)
            imageOutputStream.close()
            showToast("Image saved successfully in external storage as $fileName")
        } catch (e: IOException) {
            e.printStackTrace()
            showToast("Failed to save image")
        }
    }
    private fun createPdfAndPrint(image: Bitmap) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "IMG_$timeStamp.pdf"
        val pdfFile = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            val document = Document()
            val pdfWriter = PdfWriter.getInstance(document, FileOutputStream(pdfFile))
            document.open()

            val imageInstance = Image.getInstance(BitmapConverter.getByteFromBitmap(image))
            document.add(imageInstance)

            document.close()

            printPdf(pdfFile)
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Failed to create and print PDF")
        }
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
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

object BitmapConverter {
    fun getByteFromBitmap(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}