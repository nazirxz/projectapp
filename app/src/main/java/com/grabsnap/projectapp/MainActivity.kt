package com.grabsnap.projectapp

import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.grabsnap.projectapp.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
class MainActivity : AppCompatActivity() {
    private lateinit var binding:ActivityMainBinding
    private val REQUEST_CAMERA_PERMISSION = 1
    private val CAMERA_REQUEST_CODE = 2
    private val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 3

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
    }

    private fun hasCameraPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }

    private fun openCamera() {
        val cameraIntent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            val photo = data?.extras?.get("data") as Bitmap
            val trueColorPhoto = convertToTrueColor(photo)
            binding.imageView.setImageBitmap(trueColorPhoto)
            saveImage(trueColorPhoto)
        }
    }

    private fun convertToTrueColor(negativeImage: Bitmap): Bitmap {
        val width = negativeImage.width
        val height = negativeImage.height
        val trueColorImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixelColor = negativeImage.getPixel(x, y)
                val red = 255 - Color.red(pixelColor)
                val green = 255 - Color.green(pixelColor)
                val blue = 255 - Color.blue(pixelColor)
                trueColorImage.setPixel(x, y, Color.rgb(red, green, blue))
            }
        }

        return trueColorImage
    }

    private fun saveImage(image: Bitmap) {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
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
            // Jika izin tidak diberikan, minta izin
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_EXTERNAL_STORAGE_REQUEST_CODE)
        }
    }
}