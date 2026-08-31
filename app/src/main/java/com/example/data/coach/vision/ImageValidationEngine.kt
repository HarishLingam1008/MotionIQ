package com.example.data.coach.vision

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.sqrt

data class ImageValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null,
    val isScreenshot: Boolean = false,
    val isUnclearOrBlurry: Boolean = false,
    val isTooDarkOrBright: Boolean = false,
    val isSubjectTooSmall: Boolean = false,
    val averageBrightness: Float = 0.5f,
    val blurScore: Float = 0.5f
)

object ImageValidationEngine {

    /**
     * Inspects image quality, resolution, blur, brightness, contrast, screenshot artifacts,
     * and subject size before passing to Computer Vision / OCR.
     */
    fun validateImage(bitmap: Bitmap): ImageValidationResult {
        val width = bitmap.width
        val height = bitmap.height

        // 1. Resolution & Dimension check
        if (width < 32 || height < 32) {
            return ImageValidationResult(
                isValid = false,
                errorMessage = "I can't analyze this image because the resolution is too small. Please upload a higher-resolution photo.",
                isSubjectTooSmall = true
            )
        }

        // Downsample to 64x64 for fast, comprehensive visual metric extraction
        val sample = Bitmap.createScaledBitmap(bitmap, 64, 64, true)
        val pixels = IntArray(64 * 64)
        sample.getPixels(pixels, 0, 64, 0, 0, 64, 64)

        var totalBrightness = 0.0
        var totalSquaredBrightness = 0.0
        val totalPixels = 64 * 64

        val brightnessArray = FloatArray(totalPixels)
        for (i in 0 until totalPixels) {
            val p = pixels[i]
            val r = Color.red(p)
            val g = Color.green(p)
            val b = Color.blue(p)
            val bVal = (r * 0.299f + g * 0.587f + b * 0.114f) / 255.0f
            brightnessArray[i] = bVal
            totalBrightness += bVal
            totalSquaredBrightness += (bVal * bVal)
        }

        val avgBrightness = (totalBrightness / totalPixels).toFloat()
        val variance = ((totalSquaredBrightness / totalPixels) - (avgBrightness * avgBrightness)).coerceAtLeast(0.0)
        val stdDev = sqrt(variance).toFloat()

        // 2. Extreme Lighting Check (Dark or Washed Out)
        if (avgBrightness < 0.06f) {
            return ImageValidationResult(
                isValid = false,
                errorMessage = "The image is too dark to clearly identify the subject. Please upload a photo taken in better lighting.",
                isTooDarkOrBright = true,
                averageBrightness = avgBrightness
            )
        }

        if (avgBrightness > 0.96f && stdDev < 0.05f) {
            return ImageValidationResult(
                isValid = false,
                errorMessage = "The image is overexposed and washed out. Please take a clearer photo with balanced lighting.",
                isTooDarkOrBright = true,
                averageBrightness = avgBrightness
            )
        }

        // 3. Blur & Edge Contrast Check using Laplacian Gradient Variance
        var gradientSum = 0.0
        for (y in 0 until 63) {
            for (x in 0 until 63) {
                val b1 = brightnessArray[y * 64 + x]
                val bRight = brightnessArray[y * 64 + (x + 1)]
                val bDown = brightnessArray[(y + 1) * 64 + x]
                val diff = abs(b1 - bRight) + abs(b1 - bDown)
                gradientSum += diff
            }
        }
        val edgeScore = (gradientSum / (63 * 63)).toFloat()

        if (edgeScore < 0.008f && stdDev < 0.03f) {
            return ImageValidationResult(
                isValid = false,
                errorMessage = "I can't reliably analyze the image because it is too blurry or featureless. Please upload a clearer, sharper photo.",
                isUnclearOrBlurry = true,
                blurScore = edgeScore
            )
        }

        // 4. Screenshot Detection Heuristic
        val isScreenshot = detectScreenshot(bitmap, width, height, pixels)
        val subjectTooSmall = isScreenshot && checkSubjectTooSmall(brightnessArray)

        if (subjectTooSmall) {
            return ImageValidationResult(
                isValid = true, // allow processing but warn clearly
                errorMessage = "The image appears to be a screenshot with a small or unclear subject. Please upload the original photo or a closer crop for more accurate analysis.",
                isScreenshot = true,
                isSubjectTooSmall = true,
                averageBrightness = avgBrightness,
                blurScore = edgeScore
            )
        }

        return ImageValidationResult(
            isValid = true,
            isScreenshot = isScreenshot,
            averageBrightness = avgBrightness,
            blurScore = edgeScore
        )
    }

    private fun detectScreenshot(original: Bitmap, width: Int, height: Int, samplePixels: IntArray): Boolean {
        // Typical mobile screenshot aspect ratios (e.g. 9:16, 9:19.5, 9:20, 9:21)
        val aspectRatio = height.toFloat() / width.toFloat()
        val isPhoneRatio = aspectRatio in 1.7f..2.4f

        if (!isPhoneRatio) return false

        // Check if top row or bottom row has solid/system bar patterns
        var topRowUniform = true
        val topP0 = samplePixels[0]
        for (x in 1 until 64) {
            if (abs(Color.red(samplePixels[x]) - Color.red(topP0)) > 20 ||
                abs(Color.green(samplePixels[x]) - Color.green(topP0)) > 20 ||
                abs(Color.blue(samplePixels[x]) - Color.blue(topP0)) > 20) {
                topRowUniform = false
                break
            }
        }

        var bottomRowUniform = true
        val botP0 = samplePixels[63 * 64]
        for (x in 1 until 64) {
            if (abs(Color.red(samplePixels[63 * 64 + x]) - Color.red(botP0)) > 20 ||
                abs(Color.green(samplePixels[63 * 64 + x]) - Color.green(botP0)) > 20 ||
                abs(Color.blue(samplePixels[63 * 64 + x]) - Color.blue(botP0)) > 20) {
                bottomRowUniform = false
                break
            }
        }

        return topRowUniform || bottomRowUniform
    }

    private fun checkSubjectTooSmall(brightnessArray: FloatArray): Boolean {
        // Check if outer border area (> 75% of image) is uniform background and center contains tiny object
        var centerVariance = 0.0
        var centerCount = 0
        var centerMean = 0.0

        for (y in 20 until 44) {
            for (x in 20 until 44) {
                val b = brightnessArray[y * 64 + x]
                centerMean += b
                centerCount++
            }
        }
        centerMean /= centerCount.coerceAtLeast(1)

        for (y in 20 until 44) {
            for (x in 20 until 44) {
                val b = brightnessArray[y * 64 + x]
                centerVariance += (b - centerMean) * (b - centerMean)
            }
        }
        val centerStdDev = sqrt(centerVariance / centerCount.coerceAtLeast(1))

        // If center is very small and low contrast, subject is too small
        return centerStdDev < 0.04
    }
}
