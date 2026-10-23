package com.focuslock.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File

/** The profile picture lives only on this phone, as a small square JPEG in the app's private folder. */
fun profilePhotoFile(context: Context): File = File(context.filesDir, "profile_photo.jpg")

/** Copies the chosen picture into the app (cropped to a centered square, max 512px). Returns true on success. */
fun saveProfilePhoto(context: Context, uri: Uri): Boolean = runCatching {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= 512 && bounds.outHeight / (sample * 2) >= 512) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        ?: return@runCatching false
    val side = minOf(decoded.width, decoded.height)
    val square = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side)
    val scaled = if (side > 512) Bitmap.createScaledBitmap(square, 512, 512, true) else square
    profilePhotoFile(context).outputStream().use { scaled.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    true
}.getOrDefault(false)

fun removeProfilePhoto(context: Context) {
    runCatching { profilePhotoFile(context).delete() }
}

fun loadProfileBitmap(context: Context): Bitmap? = runCatching {
    val f = profilePhotoFile(context)
    if (f.exists()) BitmapFactory.decodeFile(f.absolutePath) else null
}.getOrNull()
