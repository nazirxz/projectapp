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
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import android.Manifest
import android.content.Context
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*
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
                    // Meminta izin WRITE_EXTERNAL_STORAGE
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
                // Izin diberikan, simpan gambar
                val photo: Bitmap? = intent.getParcelableExtra("photo")
                if (photo != null) {
                    saveImageToExternalStorage(photo)
                }
            } else {
                // Izin ditolak, tampilkan pesan kepada pengguna
                showToast("Izin penyimpanan ditolak")
            }
        }
    }

    private fun saveImageToExternalStorage(image: Bitmap) {
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val timeStamp = System.currentTimeMillis()
        val fileName = "IMG_$timeStamp.png" // Menggunakan format PNG untuk gambar berkualitas tinggi

        try {
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }

            val imageFile = File(storageDir, fileName)
            val imageOutputStream = FileOutputStream(imageFile)
            image.compress(Bitmap.CompressFormat.PNG, 100, imageOutputStream) // Menggunakan format PNG
            imageOutputStream.close()
            // Gambar berhasil disimpan
            showToast("Gambar berhasil disimpan di penyimpanan eksternal sebagai $fileName")
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