package com.gamer.community

import android.graphics.Bitmap

internal fun Bitmap.firstSpritesheetFrame(): Bitmap {
    if (width < 2 || height < 2) {
        return this
    }

    return Bitmap.createBitmap(this, 0, 0, width / 2, height / 2)
}
