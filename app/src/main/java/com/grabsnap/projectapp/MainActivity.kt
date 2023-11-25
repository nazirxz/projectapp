package com.grabsnap.projectapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.grabsnap.projectapp.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
class MainActivity : AppCompatActivity() {

    private val REQUEST_CAMERA_PERMISSION = 1
    private val CAMERA_REQUEST_CODE = 2
    private val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 3
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnCamera.setOnClickListener {
            if (hasCameraPermission()) {
                openCamera()
            } else {
                requestCameraPermission()
            }
        }
        binding.btnAbout.setOnClickListener {
            val intent = Intent(this, AboutActivity::class.java)
            startActivity(intent)
            finish()
        }
        binding.btnSignOut.setOnClickListener {
            finish()
        }
    }

    private fun hasCameraPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }

    private fun openCamera() {
        val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            val photo: Bitmap? = data?.extras?.get("data") as Bitmap?
            val trueColorPhoto = convertToTrueColor(photo)  // Ubah gambar menjadi true color
            val intent = Intent(this, DisplayImageActivity::class.java)
            intent.putExtra("photo", trueColorPhoto)  // Kirim gambar true color sebagai data ekstra
            startActivity(intent)
        }
    }

    private fun saveImage(image: Bitmap) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val imageFile = File(storageDir, "true_color_image.jpg")

            try {
                val imageOutputStream = FileOutputStream(imageFile)
                image.compress(Bitmap.CompressFormat.JPEG, 100, imageOutputStream)
                imageOutputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_EXTERNAL_STORAGE_REQUEST_CODE)
        }
    }

    private fun convertToTrueColor(negativeImage: Bitmap?): Bitmap? {
        if (negativeImage == null) {
            return null
        }

        val width = negativeImage.width
        val height = negativeImage.height
        val trueColorImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixelColor = negativeImage.getPixel(x, y)
                val red = 250 - android.graphics.Color.red(pixelColor) - 25
                val green = 250 - android.graphics.Color.green(pixelColor) - 50
                val blue = 250 - android.graphics.Color.blue(pixelColor) - 50

                // Tambahkan pencetakan nilai-nilai warna
                Log.d("Warna", "Red: $red, Green: $green, Blue: $blue")

                trueColorImage.setPixel(x, y, android.graphics.Color.rgb(red, green, blue))
            }
        }

        return trueColorImage
    }

}