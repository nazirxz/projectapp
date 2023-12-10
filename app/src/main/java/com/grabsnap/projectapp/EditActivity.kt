package com.grabsnap.projectapp

import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.grabsnap.projectapp.databinding.ActivityEditBinding
import com.grabsnap.projectapp.databinding.ActivityImageBinding

class EditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditBinding
    private lateinit var sharedViewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedViewModel = ViewModelProvider(this).get(SharedViewModel::class.java)

        // Access photoBitmap from the sharedViewModel
        val photoBitmap = sharedViewModel.photoBitmap

        // Inisialisasi SeekBars
        binding.contrastSeekBar.setOnSeekBarChangeListener(createSeekBarChangeListener())
        binding.saturationSeekBar.setOnSeekBarChangeListener(createSeekBarChangeListener())
        binding.brightnessSeekBar.setOnSeekBarChangeListener(createSeekBarChangeListener())
    }

    private fun createSeekBarChangeListener(): SeekBar.OnSeekBarChangeListener {
        return object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                applyChangesToImage()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
    }

    private fun applyChangesToImage() {
        val contrast = binding.contrastSeekBar.progress / 100f
        val saturation = binding.saturationSeekBar.progress / 100f
        val brightness = binding.brightnessSeekBar.progress - 100

        // Access photoBitmap from the sharedViewModel
        val photoBitmap = sharedViewModel.photoBitmap

        if (photoBitmap != null) {
            val adjustedBitmap = adjustBitmap(photoBitmap, contrast, saturation, brightness)
            binding.editImageView.setImageBitmap(adjustedBitmap)
        }
    }

    private fun adjustBitmap(bitmap: Bitmap, contrast: Float, saturation: Float, brightness: Int): Bitmap {
        val cm = ColorMatrix().apply {
            setScale(contrast, contrast, contrast, 1f)
            setSaturation(saturation)
            set(floatArrayOf(
                1f, 0f, 0f, 0f, brightness.toFloat(),
                0f, 1f, 0f, 0f, brightness.toFloat(),
                0f, 0f, 1f, 0f, brightness.toFloat(),
                0f, 0f, 0f, 1f, 0f
            ))
        }

        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }

        val adjustedBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
        val canvas = Canvas(adjustedBitmap)
        val matrix = Matrix()
        canvas.drawBitmap(bitmap, matrix, paint)
        return adjustedBitmap
    }
}