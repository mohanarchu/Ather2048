package com.example.newgame

import android.content.Context
import com.example.newgame.TileView
import android.graphics.RectF
import com.example.newgame.R
import com.example.newgame.DefaultTileView
import android.graphics.Paint.Align
import com.example.newgame.GameGrid.DIRECTION
import kotlin.Throws
import com.example.newgame.GameGrid.GAMESTATE
import com.example.newgame.GameGrid
import android.content.SharedPreferences
import com.example.newgame.GameView
import android.view.MotionEvent
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

/**
 * Created by raduetsya on 3/18/14.
 */
class DefaultTileView(context: Context, var roundAngleSize: Int) : TileView {
    var tilePaint = Paint()
    var tempRect = RectF()
    var bounds = Rect()
    var fontSize = 0
    var desiredFontSize_forBounds = 0
    var tileColorArr = intArrayOf(
            R.color.tileColor0, R.color.tileColor1, R.color.tileColor2, R.color.tileColor3,
            R.color.tileColor4, R.color.tileColor5, R.color.tileColor6, R.color.tileColor7,
            R.color.tileColor8, R.color.tileColor9, R.color.tileColor10, R.color.tileColor11
    )
    var tileTextColorArr = intArrayOf(
            R.color.textColor0, R.color.textColor1, R.color.textColor2, R.color.textColor3,
            R.color.textColor4, R.color.textColor5, R.color.textColor6, R.color.textColor7,
            R.color.textColor8, R.color.textColor9, R.color.textColor10, R.color.textColor11
    )

    override fun draw(c: Canvas, rect: RectF?, rang: Int, howCloseToDissapear: Float) {
        var rang = rang
        if (rang == 0) return
        val realRang = rang
        if (rang > 11) rang = 11

        // draw tile
        tilePaint.isAntiAlias = true
        tilePaint.color = tileColorArr[rang]
        tempRect.set(rect!!)
        if (howCloseToDissapear != 0f) {
            val newSize = (Math.pow((1 - howCloseToDissapear).toDouble(), 3.0) * MAX_SIZE_INCREASE).toInt()
            tempRect.inset((-1 * newSize).toFloat(), (-1 * newSize).toFloat())
        }
        c.drawRoundRect(tempRect, roundAngleSize.toFloat(), roundAngleSize.toFloat(), tilePaint)

        // draw text
        val text = "" + Math.pow(2.0, realRang.toDouble()).toInt()
        tilePaint.color = tileTextColorArr[rang]
        tilePaint.textAlign = Align.CENTER
        if (desiredFontSize_forBounds.toDouble() != tempRect.width() * 0.8) {
            val testTextSize = 48f
            tilePaint.textSize = testTextSize
            tilePaint.getTextBounds("2048", 0, 4, bounds)
            fontSize = (testTextSize * (tempRect.width() * 0.8) / bounds.width()).toInt()
            desiredFontSize_forBounds = (tempRect.width() * 0.8).toInt()
        }
        tilePaint.textSize = fontSize.toFloat()
        tilePaint.getTextBounds(text, 0, text.length, bounds)
        val centerX = tempRect.centerX() // - bounds.width()/2;
        val centerY = tempRect.centerY() + bounds.height() / 2
        c.drawText(text, centerX, centerY, tilePaint)
    }

    companion object {
        const val MAX_SIZE_INCREASE = 15
    }

    init {
        for (i in tileColorArr.indices) {
            tileColorArr[i] = context.resources.getColor(tileColorArr[i])
            tileTextColorArr[i] = context.resources.getColor(tileTextColorArr[i])
        }
    }
}