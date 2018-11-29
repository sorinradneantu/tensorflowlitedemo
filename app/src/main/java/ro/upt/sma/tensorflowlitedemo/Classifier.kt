package ro.upt.sma.tensorflowlitedemo

import android.graphics.Bitmap

interface Classifier {
    fun recognize(bitmap : Bitmap) : List<Recognition>
}