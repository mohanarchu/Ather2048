package com.example.newgame

import android.content.Context
import android.graphics.*
import android.graphics.Paint.Align
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.newgame.GameGrid
import com.example.newgame.GameGrid.DIRECTION
import com.example.newgame.GameGrid.GAMESTATE
import java.util.*

class GameView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    var mainRect: RectF? = null
    var mainRectPaint = Paint()
    var mainRectColor = 0
    var mainRectRoundAngles = 0
    var resultPaint = Paint()
    var resultRect = Rect()
    var background = 0
    var gridRectColor = 0
    var gridTempRect = RectF()
    var gridMargins = 0
    var gridSpacing = 0
    var gridLeftTopRect = RectF()
    var gridPaint = Paint()
    var fancyGridOffset = Point()
    var lastDir = DIRECTION.NONE
    var FPS = 1 / 30.0
    var gestureCenter = Point()
    var gestOffset = Point()
    var offsetSize = 0
    var animList: List<GameGrid.Action>? = null
    var animHandler = Handler()
    var animStartTime = System.currentTimeMillis().toInt()
    var animLastOffset = Point()
    var animFadeGameover = 0
    var animStop = false
    var model: GameGrid? = null
    var animationInvalidator: Runnable = object : Runnable {
        override fun run() {
            invalidate()
            if (!(animStop == true && animList == null)) animHandler.postDelayed(this, ANIM_FRAMEDELAY.toLong())
        }
    }
//    var model: GameGrid? = null
    var tileView: TileView? = null
    @JvmName("setModel1")
    fun setModel(model: GameGrid?) {
        this.model = model
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (model == null) return
        mainRect = if (w <= h) RectF(0F, (h / 2 - w / 2).toFloat(), w.toFloat(), (h / 2 + w / 2).toFloat()) else RectF((w / 2 - h / 2).toFloat(), 0F, (w / 2 + h / 2).toFloat(), h.toFloat())
        val gridRectSizeX = ((mainRect!!.width() - gridMargins * 2 - (model!!.sizeX - 1) * gridSpacing) / model!!.sizeX).toInt()
        val gridRectSizeY = ((mainRect!!.height() - gridMargins * 2 - (model!!.sizeY - 1) * gridSpacing) / model!!.sizeY).toInt()
        gridLeftTopRect[mainRect!!.left + gridMargins, mainRect!!.top + gridMargins, mainRect!!.left + gridMargins + gridRectSizeX] = mainRect!!.top + gridMargins + gridRectSizeY
    }

    fun anim(canvas: Canvas): Boolean {
        var time = (System.currentTimeMillis().toInt() - animStartTime) / ANIM_DURATION.toDouble()
        var createTime = (System.currentTimeMillis().toInt() - animStartTime - ANIM_CREATE_DELAY) /
                (ANIM_CREATE_DELAY + ANIM_CREATE_DURATION).toDouble()
        if (createTime < 0.0) createTime = 0.0
        if (createTime > 1.0) {
            animList = null
            return false
        }
        if (time > 1.0) time = 1.0
        time = Math.pow(time, 0.33)
        for (zIndex in 0..2) {
            for (iter in animList!!) {
                val posX = iter.oldX + (iter.newX - iter.oldX) * time
                val posY = iter.oldY + (iter.newY - iter.oldY) * time
                gridTempRect.offsetTo((gridLeftTopRect.left + posX * (gridLeftTopRect.width() + gridSpacing)).toFloat(),
                        (gridLeftTopRect.top + posY * (gridLeftTopRect.height() + gridSpacing)).toFloat())
                if (iter.oldX != iter.newX || iter.oldY != iter.newY) {
                    gridTempRect.offset((animLastOffset.x * (1 - time)).toFloat(), (animLastOffset.y * (1 - time)).toFloat())
                }

                // draw
                if (iter.type == GameGrid.Action.Companion.MOVE && zIndex == 0) {
                    tileView!!.draw(canvas, gridTempRect, iter.rang, 0.0f)
                } else if (iter.type == GameGrid.Action.Companion.MERGE && zIndex == 1) {
                    tileView!!.draw(canvas, gridTempRect, iter.rang + 1, (1 - time).toFloat())
                } else if (iter.type == GameGrid.Action.Companion.CREATE && zIndex == 2) {
                    if (createTime != 0.0) tileView!!.draw(canvas, gridTempRect, iter.rang, createTime.toFloat())
                }
            }
        }
        return true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (model == null) return
        canvas.drawColor(background)

        //mainRectPaint.setColor(Color.rgb(model.rand.nextInt(255), model.rand.nextInt(255), model.rand.nextInt(255)));
        canvas.drawRoundRect(mainRect!!,
                mainRectRoundAngles.toFloat(), mainRectRoundAngles.toFloat(),
                mainRectPaint)
        gridTempRect.set(gridLeftTopRect)
        updateFancyGridOffset()
        for (zIndex in 0..2) {
            if (zIndex > 0 && animList != null) {
                if (anim(canvas)) break
            }
            for (i in 0 until model!!.sizeX) {
                for (j in 0 until model!!.sizeY) {
                    gridTempRect.offsetTo(
                            (gridLeftTopRect.left + i * (gridLeftTopRect.width() + gridSpacing)),
                            (gridLeftTopRect.top + j * (gridLeftTopRect.height() + gridSpacing)))
                    if (zIndex == 0) {
                        canvas.drawRoundRect(gridTempRect,
                                mainRectRoundAngles.toFloat(), mainRectRoundAngles.toFloat(),
                                gridPaint)
                    }
                    if (lastDir != DIRECTION.NONE) {
                        if (zIndex == 1 && !model!!.isAbleToMove(i, j, lastDir)) {
                            tileView!!.draw(canvas, gridTempRect, model!![i, j], 0.0f)
                        }
                        if (zIndex == 2 && model!!.isAbleToMove(i, j, lastDir)) {
                            gridTempRect.offset(fancyGridOffset.x.toFloat(), fancyGridOffset.y.toFloat())
                            tileView!!.draw(canvas, gridTempRect, model!![i, j], 0.0f)
                        }
                    } else {
                        tileView!!.draw(canvas, gridTempRect, model!![i, j], 0.0f)
                    }
                } // for j
            } // for i
        }
        if (model!!.gameState != GAMESTATE.PLAY) {
            resultPaint.color = background
            if (animFadeGameover > 240) {
                animFadeGameover = 240
                animStop = true
            } else {
                animFadeGameover += 4 // 1000ms / 240
            }
            if (animFadeGameover > 10) {
                resultPaint.alpha = animFadeGameover
                canvas.drawRect(mainRect!!, resultPaint)
                resultPaint.color = Color.WHITE // TODO: make color
                resultPaint.alpha = animFadeGameover
                resultPaint.textAlign = Align.CENTER
                resultPaint.textSize = 30f // TODO: make pixel-independent
                resultPaint.getTextBounds("A", 0, 1, resultRect)
                val fontHeight = (resultRect.height() * 1.5).toInt()
                if (model!!.gameState == GAMESTATE.GAMEOVER) canvas.drawText("GAME OVER", mainRect!!.centerX(), mainRect!!.centerY() + fontHeight * -1, resultPaint) else canvas.drawText("2048! YOU WIN!", mainRect!!.centerX(), mainRect!!.centerY() + fontHeight * -1, resultPaint)
                canvas.drawText("Score: " + model!!.score, mainRect!!.centerX(), mainRect!!.centerY() + fontHeight * 1, resultPaint)
            }
        } else {
            animFadeGameover = 0
        }
        resultPaint.color = Color.WHITE
        resultPaint.textAlign = Align.LEFT
        resultPaint.textSize = 30f // TODO: make pixel-independent
        resultPaint.getTextBounds("A", 0, 1, resultRect)
        val fontHeight = (resultRect.height() * 1.5).toInt()
        // pretty ugly function. such draw wow ugly
    }

    fun startAnim(animList: List<GameGrid.Action>?) {
        this.animList = animList
        animStartTime = System.currentTimeMillis().toInt()
        animStop = false
        animHandler.postDelayed(animationInvalidator, ANIM_FRAMEDELAY.toLong())
        invalidate()
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (model == null) return false
        if (ev.action == MotionEvent.ACTION_DOWN) {
            if (model!!.gameState != GAMESTATE.PLAY) {
                startAnim(null)
            } else {
                gestureCenter[ev.x.toInt()] = ev.y.toInt()
                gestOffset[0] = 0
                fancyGridOffset = Point(gestureCenter)
                lastDir = DIRECTION.NONE
                animList = null
                animStop = true
            }
            return true
        } else if (ev.action == MotionEvent.ACTION_MOVE) {
            if (model!!.gameState == GAMESTATE.PLAY) {
                gestOffset[ev.x.toInt() - gestureCenter.x] = ev.y.toInt() - gestureCenter.y
                invalidate()
            }
            return true
        } else if (ev.action == MotionEvent.ACTION_UP) {
            if (offsetSize > MINIMAL_RANGE) {
                val anims: MutableList<GameGrid.Action> = ArrayList()
                model!!.doMove(lastDir, anims)
                animLastOffset[fancyGridOffset.x] = fancyGridOffset.y
                startAnim(anims)
            }
            gestOffset[0] = 0
            lastDir = DIRECTION.NONE
            invalidate()
            return true
        }
        return super.onTouchEvent(ev)
    }

    private fun updateFancyGridOffset() {
        var offsetX = gestOffset.x
        var offsetY = gestOffset.y
        if (Math.abs(offsetX - offsetY) > MINIMAL_RANGE) {
            // set first direction
            lastDir = if (Math.abs(gestOffset.x) > Math.abs(gestOffset.y)) {
                if (gestOffset.x > 0) DIRECTION.RIGHT else DIRECTION.LEFT
            } else {
                if (gestOffset.y > 0) DIRECTION.DOWN else DIRECTION.UP
            }
        }

        if (lastDir == DIRECTION.DOWN || lastDir == DIRECTION.UP) {
            offsetX = 0
            lastDir = if (offsetY > 0) DIRECTION.DOWN else DIRECTION.UP
        } else {
            offsetY = 0
            lastDir = if (offsetX > 0) DIRECTION.RIGHT else DIRECTION.LEFT
        }


        val maxX = ((gridLeftTopRect.width() + gridMargins) * 0.68).toInt()
        val maxY = ((gridLeftTopRect.height() + gridMargins) * 0.68).toInt()
        if (Math.abs(offsetX) > maxX) offsetX = maxX * Math.signum(offsetX.toFloat()).toInt()
        if (Math.abs(offsetY) > maxY) offsetY = maxY * Math.signum(offsetY.toFloat()).toInt()

        
        offsetSize = Math.max(Math.abs(offsetX), Math.abs(offsetY))
        if (offsetSize < MINIMAL_RANGE) {
            offsetX = (MINIMAL_RANGE * Math.pow(offsetX / MINIMAL_RANGE.toDouble(), 3.0)).toInt()
            offsetY = (MINIMAL_RANGE * Math.pow(offsetY / MINIMAL_RANGE.toDouble(), 3.0)).toInt()
            offsetSize = Math.abs(offsetX + offsetY)
        }
        fancyGridOffset[offsetX] = offsetY
    }

    companion object {
        private const val MINIMAL_RANGE = 30
        const val ANIM_DURATION = 150
        const val ANIM_CREATE_DELAY = 80
        const val ANIM_CREATE_DURATION = 200
        const val ANIM_FRAMEDELAY = 33 // 1000/30
    }

    init {
        val a = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.com_example_newgame_GameView,
                0, 0)
        try {
            background = resources.getColor(R.color.colorPrimary)
            mainRectColor = Color.parseColor(a.getString(R.styleable.com_example_newgame_GameView_foregroundColor))
            mainRectRoundAngles = a.getInteger(R.styleable.com_example_newgame_GameView_roundAngles, 0)
            gridRectColor = Color.parseColor(a.getString(R.styleable.com_example_newgame_GameView_gridColor))
            gridSpacing = a.getInteger(R.styleable.com_example_newgame_GameView_gridSpacing, 0)
            gridMargins = a.getInteger(R.styleable.com_example_newgame_GameView_gridMargins, 1)
            tileView = DefaultTileView(context, a.getInteger(R.styleable.com_example_newgame_GameView_roundTileAngles, 0))
        } finally {
            a.recycle()
        }
        mainRectPaint = Paint()
        mainRectPaint.color = mainRectColor
        mainRectPaint.isAntiAlias = true
        gridPaint = Paint()
        gridPaint.color = gridRectColor
        gridPaint.isAntiAlias = true
    }
}