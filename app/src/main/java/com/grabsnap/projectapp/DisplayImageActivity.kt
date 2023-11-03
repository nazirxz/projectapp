package com.grabsnap.projectapp

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.grabsnap.projectapp.databinding.ActivityImageBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.Manifest
import android.widget.Toast

class DisplayImageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageBinding
    private val WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val photo: Bitmap? = intent.getParcelableExtra("photo")
        binding.displayImageView.setImageBitmap(photo)

        binding.saveImageButton.setOnClickListener {
            if (photo != null) {
                saveImage(photo)
            }
        }


        binding.backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun saveImage(image: Bitmap) {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val imageFile = File(storageDir, "true_color_image.jpg")

        try {
            val imageOutputStream = FileOutputStream(imageFile)
            image.compress(Bitmap.CompressFormat.JPEG, 100, imageOutputStream)
            imageOutputStream.close()
            // Gambar berhasil disimpan
            showToast("Gambar berhasil disimpan!")
        } catch (e: IOException) {
            e.printStackTrace()
            // Terjadi kesalahan saat menyimpan gambar
            showToast("Gagal menyimpan gambar")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}