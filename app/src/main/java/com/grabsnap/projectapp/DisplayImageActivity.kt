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
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.*
import android.provider.MediaStore
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicConvolve3x3
import android.util.Log
import android.widget.SeekBar
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModelProvider
import java.text.SimpleDateFormat
import java.util.*
import com.itextpdf.text.Document
import com.itextpdf.text.Image
import com.itextpdf.text.PageSize
import com.itextpdf.text.pdf.PdfWriter
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*

class DisplayImageActivity : AppCompatActivity() {
    private val REQUEST_CAMERA_PERMISSION = 1
    private val CAMERA_REQUEST_CODE = 2
    private val GALLERY_REQUEST_CODE = 4
    private lateinit var binding: ActivityImageBinding
    private val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 3
    private lateinit var photoBitmap: Bitmap
    private val UCROP_REQUEST_CODE = 123
    private var photoFile: File? = null
    private var currentPhotoPath: String? = null
    private var isGalleryImageSelected = false
    private var contrastValue: Float = 1f
    private var saturationValue: Float = 1f
    private var brightnessValue: Float = 0f
    private var sharpnessValue: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Tambahkan kode untuk menampilkan opsi cetak gambar dari galeri atau displayImageView
        binding.printButton.setOnClickListener {
            showImagePickerDialog()
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
        binding.contrastSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                applyContrast(progress.toFloat() / 100)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.saturationSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                applySaturation(progress.toFloat() / 100)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.brightnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                applyBrightness(progress.toFloat())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        binding.sharpnessSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                applySharpness(progress.toFloat())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

    }
    private fun doSharpen(original: Bitmap, multiplier: Float): Bitmap {
        val sharp = floatArrayOf(0f, -multiplier, 0f, -multiplier, 5f * multiplier, -multiplier, 0f, -multiplier, 0f)
        val bitmap = Bitmap.createBitmap(original.width, original.height, Bitmap.Config.ARGB_8888)
        val rs = RenderScript.create(this)

        val allocIn = Allocation.createFromBitmap(rs, original)
        val allocOut = Allocation.createFromBitmap(rs, bitmap)

        val convolution = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs))
        convolution.setInput(allocIn)
        convolution.setCoefficients(sharp)
        convolution.forEach(allocOut)

        allocOut.copyTo(bitmap)
        rs.destroy()

        return bitmap
    }
    private fun applySharpness(sharpness: Float) {
        val sharpnessValue = sharpness / 100f
        if (::photoBitmap.isInitialized) {
            // Gunakan thread terpisah (misalnya, dengan menggunakan coroutine)
            CoroutineScope(Dispatchers.Default).launch {
                val sharpenedBitmap = doSharpen(photoBitmap, sharpnessValue)

                // Kembali ke thread utama untuk memperbarui UI
                withContext(Dispatchers.Main) {
                    binding.displayImageView.setImageBitmap(sharpenedBitmap)
                }
            }
        } else {
            showToast("Photo has not been initialized")
        }
    }
    private fun applySaturation(saturation: Float) {
        val matrix = ColorMatrix().apply {
            setSaturation(saturation)
        }

        applyImageEffectWithMatrixAsync(matrix)
    }

    private fun applyBrightness(brightness: Float) {
        val matrix = ColorMatrix().apply {
            set(floatArrayOf(
                1f, 0f, 0f, 0f, brightness,
                0f, 1f, 0f, 0f, brightness,
                0f, 0f, 1f, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            ))
        }

        applyImageEffectWithMatrixAsync(matrix)
    }

    private fun applyContrast(contrast: Float) {
        val matrix = ColorMatrix().apply {
            setScale(contrast, contrast, contrast, 1f) // Mengatur kontras
        }

        applyImageEffectWithMatrixAsync(matrix)
    }

    private fun applyImageEffectWithMatrixAsync(matrix: ColorMatrix) {
        val bmp = photoBitmap.copy(photoBitmap.config, true)
        val cmFilter = ColorMatrixColorFilter(matrix)
        val paint = Paint().apply { colorFilter = cmFilter }

        // Gunakan coroutines untuk melakukan operasi pemrosesan gambar di thread terpisah
        CoroutineScope(Dispatchers.Default).launch {
            val processedBitmap = withContext(Dispatchers.Default) {
                val canvas = Canvas(bmp)
                canvas.drawBitmap(bmp, 0f, 0f, paint)
                bmp
            }

            // Kembali ke thread utama untuk memperbarui UI
            withContext(Dispatchers.Main) {
                binding.displayImageView.setImageBitmap(processedBitmap)
            }
        }
    }
    private fun showImagePickerDialog() {
        val options = arrayOf("Cetak Gambar", "Import dari Galeri")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Pilih Sumber Gambar")
        builder.setItems(options) { dialog, which ->
            when (which) {
                0 ->  printPdfFromImageView()
                1 -> openGallery()
            }
        }
        builder.show()
    }


    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }


    private fun createPdfAndPrint(image: Bitmap?) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "IMG_$timeStamp.pdf"
        val pdfFile = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            val document = Document()
            val outputStream = FileOutputStream(pdfFile)
            val writer = PdfWriter.getInstance(document, outputStream)
            document.open()

            if (image != null) {
                val imageInstance = Image.getInstance(getByteFromBitmap(image))

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
                if (imageInstance.width > 0 && imageInstance.height > 0) {
                    imageInstance.setAbsolutePosition(x, y)
                    document.add(imageInstance)
                } else {
                    showToast("Failed to add image to the document")
                }
            } else {
                showToast("Image not found")
            }

            document.close()
            printPdf(pdfFile)
        } catch (e: Exception) {
            e.printStackTrace()
            showToast("Failed to create and print PDF")
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
    private fun printGalleryImage(image: Bitmap) {
        isGalleryImageSelected = true
        createPdfAndPrint(image)
    }
    private fun printPdfFromImageView() {
        val drawable = binding.displayImageView.drawable
        if (drawable is BitmapDrawable) {
            val image = drawable.bitmap
            createPdfAndPrint(image)
        } else {
            showToast("No image found in ImageView")
        }
    }
    private fun applyAllEffects(bitmap: Bitmap): Bitmap {
        var processedBitmap = bitmap.copy(bitmap.config, true)

        // Terapkan efek kontras
        val contrastMatrix = ColorMatrix().apply {
            setScale(contrastValue, contrastValue, contrastValue, 1f)
        }
        val contrastFilter = ColorMatrixColorFilter(contrastMatrix)
        val contrastPaint = Paint().apply {
            colorFilter = contrastFilter
        }
        val contrastCanvas = Canvas(processedBitmap)
        contrastCanvas.drawBitmap(processedBitmap, 0f, 0f, contrastPaint)

        // Terapkan efek saturasi
        val saturationMatrix = ColorMatrix().apply {
            setSaturation(saturationValue)
        }
        val saturationFilter = ColorMatrixColorFilter(saturationMatrix)
        val saturationPaint = Paint().apply {
            colorFilter = saturationFilter
        }
        val saturationBitmap = processedBitmap.copy(processedBitmap.config, true)
        val saturationCanvas = Canvas(saturationBitmap)
        saturationCanvas.drawBitmap(processedBitmap, 0f, 0f, saturationPaint)

        // Terapkan efek kecerahan
        val brightnessMatrix = ColorMatrix().apply {
            set(floatArrayOf(
                1f, 0f, 0f, 0f, brightnessValue,
                0f, 1f, 0f, 0f, brightnessValue,
                0f, 0f, 1f, 0f, brightnessValue,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        val brightnessFilter = ColorMatrixColorFilter(brightnessMatrix)
        val brightnessPaint = Paint().apply {
            colorFilter = brightnessFilter
        }
        val brightnessBitmap = saturationBitmap.copy(saturationBitmap.config, true)
        val brightnessCanvas = Canvas(brightnessBitmap)
        brightnessCanvas.drawBitmap(saturationBitmap, 0f, 0f, brightnessPaint)

        // Terapkan efek ketajaman
        processedBitmap = doSharpen(brightnessBitmap, sharpnessValue)

        return processedBitmap
    }

    private fun printDisplayImage() {
        if (::photoBitmap.isInitialized) {
            val trueColorBitmap = convertToTrueColor(photoBitmap)
            if (trueColorBitmap != null) {
                val bitmapWithEffects = applyAllEffects(trueColorBitmap)
                if (bitmapWithEffects != null) {
                    isGalleryImageSelected = false
                    createPdfAndPrint(bitmapWithEffects)
                } else {
                    showToast("Gagal menerapkan efek pada gambar")
                }
            } else {
                showToast("Gagal mengonversi gambar ke True Color")
            }
        } else {
            showToast("Tidak ada foto yang tersedia untuk dicetak")
        }
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
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
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
        val drawable = binding.displayImageView.drawable
        if (drawable is BitmapDrawable) {
            val image = drawable.bitmap
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "IMG_processed_$timeStamp.png"

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

                    // Compress dan simpan gambar yang sedang ditampilkan
                    image.compress(Bitmap.CompressFormat.PNG, 100, imageOutputStream)

                    imageOutputStream?.close()
                    showToast("Displayed image saved successfully as $fileName")
                } else {
                    showToast("Failed to save displayed image")
                }
            } catch (e: IOException) {
                e.printStackTrace()
                showToast("Failed to save displayed image")
            }
        } else {
            showToast("No image displayed")
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
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                GALLERY_REQUEST_CODE -> {
                    if (data != null) {
                        val selectedImage: Uri? = data.data
                        if (selectedImage != null) {
                            val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, selectedImage)
                            printGalleryImage(bitmap) // Cetak gambar dari galeri tanpa perubahan warna
                        }
                    }
                }
                CAMERA_REQUEST_CODE -> {
                    val imageFile = photoFile
                    if (imageFile != null) {
                        val imageUri = Uri.fromFile(imageFile)
                        openUCropActivity(imageUri)
                    }
                }
                UCROP_REQUEST_CODE -> {
                    val resultUri = UCrop.getOutput(data!!)
                    if (resultUri != null) {
                        // Dekode hasil UCrop
                        val imageBitmap = BitmapFactory.decodeFile(resultUri.path)

                        // Kompresi gambar sebelum konversi ke warna sejati
                        val compressedBitmap =
                            compressBitmap(imageBitmap, 80) // Atur kualitas kompresi di sini

                        val trueColorBitmap = convertToTrueColor(compressedBitmap)
                        if (trueColorBitmap != null) {
                            displayImage(trueColorBitmap) // Menampilkan gambar ke aplikasi
                        }
                    }
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


