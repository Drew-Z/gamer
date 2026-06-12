package com.gamer.community

import android.graphics.Bitmap

internal fun Bitmap.firstSpritesheetFrame(): Bitmap {
    if (width < 2 || height < 2) {
        return this
    }

    return Bitmap.createBitmap(this, 0, 0, width / 2, height / 2)
}

internal fun Bitmap.horizontalSpritesheetFrames(frameCount: Int): List<Bitmap> {
    if (width < 2 || height < 2 || frameCount <= 0) {
        return emptyList()
    }

    val frameWidth = width / frameCount
    if (frameWidth <= 0) {
        return emptyList()
    }

    return (0 until frameCount).map { index ->
        Bitmap.createBitmap(this, index * frameWidth, 0, frameWidth, height)
    }
}
