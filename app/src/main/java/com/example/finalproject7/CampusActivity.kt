package com.example.finalproject7

import android.content.Intent
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.finalproject7.databinding.ActivityCampusBinding

//This displays and responds whenever the student clicks on the screen

class CampusActivity : ComponentActivity() {
    // View binding instance
    private lateinit var binding: ActivityCampusBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view binding
        binding = ActivityCampusBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up home button to navigate back to MainActivity
        setupHomeButton()

        // Set up touch listener for campus map interaction
        setupMapTouchListener()
    }

    // Configure the home button to return to MainActivity
    private fun setupHomeButton() {
        val buttonHome = findViewById<Button>(R.id.homeButton)
        buttonHome.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    // Set up touch listener to handle taps on the campus map
    private fun setupMapTouchListener() {
        binding.campusMapImageView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                // Get raw tap coordinates
                val x = event.x
                val y = event.y

                // Adjust coordinates based on ImageView scaling
                val adjustedCoords = adjustCoordinates(x, y, binding.campusMapImageView)
                val adjustedX = adjustedCoords[0]
                val adjustedY = adjustedCoords[1]

                // Log adjusted coordinates for debugging
                android.util.Log.d("CampusActivity", "Tapped at: ($adjustedX, $adjustedY)")

                // Determine and display the tapped area
                checkTappedArea(adjustedX, adjustedY)
            }
            true // Event handled
        }
    }

    // Adjust tap coordinates to account for ImageView scaling and aspect ratio
    private fun adjustCoordinates(x: Float, y: Float, imageView: ImageView): FloatArray {
        val adjusted = FloatArray(2)

        // Get ImageView dimensions
        val imageViewWidth = imageView.width.toFloat()
        val imageViewHeight = imageView.height.toFloat()

        // Get original image dimensions, default to 1f if null
        val imageWidth = imageView.drawable?.intrinsicWidth?.toFloat() ?: 1f
        val imageHeight = imageView.drawable?.intrinsicHeight?.toFloat() ?: 1f

        // Calculate scaling factor to maintain aspect ratio
        val scale = minOf(imageViewWidth / imageWidth, imageViewHeight / imageHeight)

        // Calculate scaled image dimensions
        val scaledImageWidth = imageWidth * scale
        val scaledImageHeight = imageHeight * scale

        // Calculate offsets due to centering
        val offsetX = (imageViewWidth - scaledImageWidth) / 2
        val offsetY = (imageViewHeight - scaledImageHeight) / 2

        // Adjust coordinates to match the original image's coordinate system
        val adjustedX = (x - offsetX) / scale
        val adjustedY = (y - offsetY) / scale

        // Constrain coordinates within image bounds
        adjusted[0] = adjustedX.coerceIn(0f, imageWidth)
        adjusted[1] = adjustedY.coerceIn(0f, imageHeight)

        return adjusted
    }

    // Identify the tapped area and display a corresponding message
    private fun checkTappedArea(x: Float, y: Float) {
        when {
            // OA Building (Academic Building)
            x in 5f..280f && y in 180f..370f ->
                Toast.makeText(this, "Academic Building", Toast.LENGTH_SHORT).show()

            // OC Building (Cafeteria & Bookstore)
            x in 110f..300f && y in 420f..545f ->
                Toast.makeText(this, "Cafeteria & Bookstore", Toast.LENGTH_SHORT).show()

            // OR Building (Residence)
            x in 428f..528f && y in 290f..439f ->
                Toast.makeText(this, "Residence", Toast.LENGTH_SHORT).show()

            // OR Building (Residence)
            x in 307f..430f && y in 210f..460f ->
                Toast.makeText(this, "Residence", Toast.LENGTH_SHORT).show()

            // Default case for untapped or undefined areas
            else ->
                Toast.makeText(this, "Blank area ($x, $y)", Toast.LENGTH_SHORT).show()
        }
    }
}