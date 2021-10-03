package com.example.newgame

import kotlin.Throws
import android.content.SharedPreferences
import android.content.res.TypedArray
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java.lang.IllegalArgumentException
import java.util.*

/**
 * Created by raduetsya on 3/14/14.
 */
class GameGrid(var sizeX: Int, var sizeY: Int, var scoreInterface: ScoreInterface) {
    var rand = Random()
    enum class DIRECTION {
        UP, DOWN, LEFT, RIGHT, NONE
    }

    inner class Tile {
        var rang = 0
        var canMove = IntArray(4)
        var canMerge = BooleanArray(4)
        fun getCanMove(dir: DIRECTION): Int {
            return canMove[getDir(dir)]
        }

        fun setCanMove(dir: DIRECTION, value: Int) {
            canMove[getDir(dir)] = value
        }

        fun getCanMerge(dir: DIRECTION): Boolean {
            return canMerge[getDir(dir)]
        }

        fun setCanMerge(dir: DIRECTION, value: Boolean) {
            canMerge[getDir(dir)] = value
        }

        @Throws(IllegalArgumentException::class)
        private fun getDir(dir: DIRECTION): Int {
            when (dir) {
                DIRECTION.UP -> return 0
                DIRECTION.DOWN -> return 1
                DIRECTION.LEFT -> return 2
                DIRECTION.RIGHT -> return 3
                DIRECTION.NONE -> {
                }
            }
            throw IllegalArgumentException()
        }
    }
    class Action(
            var type: Int,
            var rang: Int,
            var oldX: Int,
            var oldY: Int,
            var newX: Int,
            var newY: Int
    ) {
        companion object {
            const val NOTHING = 0
            const val MOVE = 1
            const val CREATE = 2
            const val MERGE = 3
        }
    }


    var data: Array<Array<Tile?>?>
    var score = 0
    var highscore = 0
    fun updateHighscore() {
        if (score > highscore) {
            highscore = score
        }
    }

    enum class GAMESTATE {
        PLAY, GAMEOVER, WIN
    }

    var gameState = GAMESTATE.PLAY
    fun createData(w: Int, h: Int): Array<Array<Tile?>?> {
        val newData: Array<Array<Tile?>?> = arrayOfNulls(w)
        for (x in 0 until sizeX) {
            newData[x] = arrayOfNulls(h)
            for (y in 0 until sizeY) newData[x]?.set(y, Tile())
        }
        return newData
    }

    fun doNewGame(actionHistory: MutableList<Action>?) {
        updateHighscore()
        for (i in 0 until sizeX) for (j in 0 until sizeY) {
            data[i]?.get(j)!!.rang = 0
        }
        gameState = GAMESTATE.PLAY
        addNewTileToRandomCell(actionHistory)
        addNewTileToRandomCell(actionHistory)
        updateMovingAbilities()
        score = 0
    }


    operator fun get(x: Int, y: Int): Int {
        return data[x]?.get(y)!!.rang
    }

    fun doMove(dir: DIRECTION, actionHistory: MutableList<Action>?) {
        updateState()
        if (gameState != GAMESTATE.PLAY) return
        var canDoMove = false
        val newData = createData(sizeX, sizeY)
        val increaseRang: MutableList<IntArray> = ArrayList()
        for (x in 0 until sizeX) {
            for (y in 0 until sizeY) {
                if (data[x]?.get(y)!!.rang == 0) continue
                var newX = x
                var newY = y
                when (dir) {
                    DIRECTION.UP -> newY -= data[x]?.get(y)!!.getCanMove(DIRECTION.UP)
                    DIRECTION.DOWN -> newY += data[x]?.get(y)!!.getCanMove(DIRECTION.DOWN)
                    DIRECTION.LEFT -> newX -= data[x]?.get(y)!!.getCanMove(DIRECTION.LEFT)
                    DIRECTION.RIGHT -> newX += data[x]?.get(y)!!.getCanMove(DIRECTION.RIGHT)
                    DIRECTION.NONE -> {
                    }
                }
                newData[newX]?.set(newY, data[x]?.get(y))
                if (data[x]?.get(y)!!.getCanMerge(dir)) {
                    increaseRang.add(intArrayOf(newX, newY, data[x]?.get(y)!!.rang + 1))
                }
                actionHistory?.add(Action(Action.Companion.MOVE, data[x]?.get(y)!!.rang, x, y, newX, newY))
                if (x != newX || y != newY) canDoMove = true
            }
        }
        for (tile in increaseRang) {
            score += Math.pow(2.0, tile[2].toDouble()).toInt()
            if (tile[2] == 11) gameState = GAMESTATE.WIN
            newData[tile[0]]?.get(tile[1])!!.rang = tile[2]
            actionHistory?.add(Action(Action.Companion.CREATE, tile[2], tile[0], tile[1], tile[0], tile[1]))

        }
        if (canDoMove) {
            data = newData
            addNewTileToRandomCell(actionHistory)
            updateMovingAbilities()
        }
        scoreInterface.scoreData(score)
    }

    fun isAbleToMove(x: Int, y: Int, dir: DIRECTION): Boolean {
        return data[x]?.get(y)!!.getCanMove(dir) != 0 || data[x]?.get(y)!!.getCanMerge(dir)
    }

    fun addNewTileToRandomCell(actionHistory: MutableList<Action>?) {
        if (gameState != GAMESTATE.PLAY) return
        var x: Int
        var y: Int
        do {
            x = rand.nextInt(sizeX)
            y = rand.nextInt(sizeY)
        } while (data[x]?.get(y)!!.rang != 0)
        data[x]?.get(y)!!.rang = rand.nextInt(MAX_RANG_TO_ADD) + 1
        actionHistory?.add(Action(Action.Companion.CREATE, data[x]?.get(y)!!.rang, x, y, x, y))
    }

    // this vars need to do interaction b/w updateMovingAbilities() and updateTile()
    var foundedSpace = 0
    var lastRangForMerge = 0
    private fun updateTile(x: Int, y: Int, dir: DIRECTION) {
        if (data[x]?.get(y)!!.rang == 0) foundedSpace++ else {
            data[x]?.get(y)!!.setCanMove(dir, foundedSpace)
            if (data[x]?.get(y)!!.rang == lastRangForMerge) {
                data[x]?.get(y)!!.setCanMerge(dir, true)
                lastRangForMerge = 0
                foundedSpace++
                data[x]?.get(y)!!.setCanMove(dir, foundedSpace)
            } else {
                lastRangForMerge = data[x]?.get(y)!!.rang
                data[x]?.get(y)!!.setCanMerge(dir, false)
            }
        }
    }

    private fun updateMovingAbilities() {
        var x: Int
        var y: Int
        y = 0
        while (y < sizeY) {
            foundedSpace = 0
            lastRangForMerge = 0
            x = sizeX - 1
            while (x >= 0) {
                updateTile(x, y, DIRECTION.RIGHT)
                x--
            }
            y++
        }
        y = 0
        while (y < sizeY) {
            foundedSpace = 0
            lastRangForMerge = 0
            x = 0
            while (x < sizeX) {
                updateTile(x, y, DIRECTION.LEFT)
                x++
            }
            y++
        }
        x = 0
        while (x < sizeX) {
            foundedSpace = 0
            lastRangForMerge = 0
            y = 0
            while (y < sizeY) {
                updateTile(x, y, DIRECTION.UP)
                y++
            }
            x++
        }
        x = 0
        while (x < sizeX) {
            foundedSpace = 0
            lastRangForMerge = 0
            y = sizeY - 1
            while (y >= 0) {
                updateTile(x, y, DIRECTION.DOWN)
                y--
            }
            x++
        }
        updateState()
    }

    fun updateState() {
        var nowhereToMove = true
        gameState = GAMESTATE.PLAY
        for (x in 0 until sizeX) {
            for (y in 0 until sizeY) {
                if (data[x]?.get(y)!!.rang == 11) gameState = GAMESTATE.WIN
                if (nowhereToMove) for (m in data[x]?.get(y)!!.canMove.indices) if (data[x]?.get(y)!!.canMove[m] != 0) nowhereToMove = false
            }
        }

        if (nowhereToMove && gameState != GAMESTATE.WIN) gameState = GAMESTATE.GAMEOVER
    }

    /* SERIALIZER */
    fun saveState(bundle: SharedPreferences.Editor) {
        bundle.putInt("GRID_WIDTH", sizeX)
        bundle.putInt("GRID_HEIGHT", sizeY)
        for (x in 0 until sizeX) {
            for (y in 0 until sizeY) {
                bundle.putInt("GRID_DATA_" + x + "_" + y, data[x]?.get(y)!!.rang)
            }
        }
        bundle.putInt("CURRENT_SCORE", score)
        scoreInterface.scoreData(score)
        bundle.putInt("HIGH_SCORE", highscore)
    }

    fun restoreState(bundle: SharedPreferences) {
        sizeX = bundle.getInt("GRID_WIDTH", 4)
        sizeY = bundle.getInt("GRID_HEIGHT", 4)
        data = createData(sizeX, sizeY)
        for (x in 0 until sizeX) {
            for (y in 0 until sizeY) {
                data[x]?.get(y)!!.rang = bundle.getInt("GRID_DATA_" + x + "_" + y, 0)
            }
        }
        score = bundle.getInt("CURRENT_SCORE", 0)
        scoreInterface.scoreData(score)
        highscore = bundle.getInt("HIGH_SCORE", 0)
        updateMovingAbilities()
    }

    companion object {
        private const val MAX_RANG_TO_ADD = 2
    }

    init {
        data = createData(sizeX, sizeY)
    }
}