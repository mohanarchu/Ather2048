package com.example.newgame

import android.graphics.Canvas
import android.graphics.RectF

/**
 * Created by raduetsya on 3/18/14.
 */
interface TileView {
    fun draw(c: Canvas, rect: RectF?, rang: Int, howCloseToDissapear: Float)
}