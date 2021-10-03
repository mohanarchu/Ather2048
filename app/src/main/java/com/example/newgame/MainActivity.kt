package com.example.newgame
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.newgame.databinding.ActivityMainBinding
import org.w3c.dom.Text
import java.util.*

class MainActivity : AppCompatActivity() {
    var gameGridModel = null as? GameGrid
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        gameGridModel = GameGrid(4,4,scoreInterface = object : ScoreInterface {
            override fun scoreData(score: Int?) {
                binding.score.text= score.toString()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        binding.gameView.setModel(gameGridModel)
        binding.gameView.invalidate()
        val preferences = getPreferences(MODE_PRIVATE)
        gameGridModel?.restoreState(preferences)
        if (preferences.getBoolean("NEW_GAME", true) == true) gameGridModel?.doNewGame(null)
    }

    override fun onPause() {
        super.onPause()
        val preferences = getPreferences(MODE_PRIVATE)
        val editor = preferences.edit()
        editor.clear()
        editor.putBoolean("NEW_GAME", false)
        gameGridModel?.saveState(editor)
        editor.commit()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {

        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId
        val view = findViewById<View>(R.id.game_view) as GameView
        if (id == R.id.action_newgame) {
            val animList: MutableList<GameGrid.Action> = ArrayList()
            gameGridModel?.doNewGame(animList)
            binding.score.text= "0"
            view.startAnim(animList)
        }
        return super.onOptionsItemSelected(item)
    }
}