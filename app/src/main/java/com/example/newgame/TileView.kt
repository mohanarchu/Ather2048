package com.example.newgame

import android.graphics.Canvas
import android.graphics.RectF


interface TileView {
    fun draw(c: Canvas, rect: RectF?, rang: Int, howCloseToDissapear: Float)
}