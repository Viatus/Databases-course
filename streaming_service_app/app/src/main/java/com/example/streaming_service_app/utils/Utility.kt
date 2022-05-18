package com.example.streaming_service_app.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import androidx.core.content.res.ResourcesCompat
import com.example.streaming_service_app.R

class Utility {
    fun generateCircleBitmap(
        context: Context,
        diameterDP: Float,
        text: String
    ): Bitmap {
        val textColor = 0xffffffff
        val circleColor = getMaterialColor(text)

        val metrics = Resources.getSystem().displayMetrics
        val diameterPixels = diameterDP * (metrics.densityDpi / 160f)
        val radiusPixels = diameterPixels / 2

        val output = Bitmap.createBitmap(
            diameterPixels.toInt(),
            diameterPixels.toInt(),
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(output)
        canvas.drawARGB(0, 0, 0, 0)

        val paintC = Paint()
        paintC.isAntiAlias = true
        paintC.color = circleColor.toInt()
        canvas.drawCircle(radiusPixels, radiusPixels, radiusPixels, paintC)

        if (text.isNotEmpty()) {
            val paintT = Paint()
            paintT.color = textColor.toInt()
            paintT.isAntiAlias = true
            paintT.textSize = radiusPixels * 2
            val typeFace = ResourcesCompat.getFont(context,
                R.font.roboto_thin
            )
            paintT.typeface = typeFace
            val textBounds = Rect()
            paintT.getTextBounds(text, 0, text.length, textBounds)
            canvas.drawText(
                text,
                radiusPixels - textBounds.exactCenterX(),
                radiusPixels - textBounds.exactCenterY(),
                paintT
            )
        }
        return output
    }

    private val materialColors = listOf(
        0xffe57373,
        0xfff06292,
        0xffba68c8,
        0xff9575cd,
        0xff7986cb,
        0xff64b5f6,
        0xff4fc3f7,
        0xff4dd0e1,
        0xff4db6ac,
        0xff81c784,
        0xffaed581,
        0xffff8a65,
        0xffd4e157,
        0xffffd54f,
        0xffffb74d,
        0xffa1887f,
        0xff90a4ae
    )

    private fun getMaterialColor(key: Any): Long {
        return materialColors[Math.abs(key.hashCode() % materialColors.size)]
    }
}