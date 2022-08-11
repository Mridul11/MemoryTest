package com.supercoolapps.memorytest

import android.animation.ArgbEvaluator
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.supercoolapps.models.BoardSize
import com.supercoolapps.models.MemoryGame
import com.supercoolapps.utils.EXTRA_BOARD_SIZE

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: MemoryBoardAdapter
    private lateinit var memoryGame: MemoryGame
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvPaiMoves: TextView
    private lateinit var clRoot : ConstraintLayout

    private var boardSize : BoardSize = BoardSize.EASY

    companion object{
        private const val TAG = "MainActivity"
        private const val CREAET_REQUEST_CODE = 111
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        clRoot = findViewById(R.id.clRoot)
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvPaiMoves = findViewById(R.id.tvNumPairs)
        setUpBoard()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
       menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.mi_refresh -> {
                // Setup Game...
                if(memoryGame.getNumMoves() > 0 && !memoryGame.haveWonGame()){
                    showAlertDialog( "Quit your current game? ", null, View.OnClickListener {
                        setUpBoard()
                    })
                }else{
                    setUpBoard()
                }
            }

            R.id.mi_new_size -> {
                showNewSizeDialog()
                return true
            }
            R.id.mi_custom -> {
                showCreationDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showCreationDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        showAlertDialog("Create your memory board", boardSizeView, View.OnClickListener {
            //set a new value for the board size
            val desiredBoardSize: BoardSize = when(radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            // Navigate user to new screen
            val intent = Intent(this, CreateActivity::class.java)
            intent.putExtra(EXTRA_BOARD_SIZE, desiredBoardSize)
            startActivityForResult(intent, CREAET_REQUEST_CODE)
        })
    }

    private fun showNewSizeDialog() {
       val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size,null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        when(boardSize) {
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }
       showAlertDialog("Choose New Size", boardSizeView, View.OnClickListener {
           //set a new value for the board size
           boardSize = when(radioGroupSize.checkedRadioButtonId) {
               R.id.rbEasy -> BoardSize.EASY
               R.id.rbMedium -> BoardSize.MEDIUM
               else -> BoardSize.HARD
           }
           setUpBoard()
       })
    }

    private fun showAlertDialog(title: String, view: View?, positiveClickListener: View.OnClickListener) {
       AlertDialog.Builder(this)
           .setTitle(title)
           .setView(view)
           .setNegativeButton("Cancel", null)
           .setPositiveButton("OK"){
               _,_ -> positiveClickListener.onClick(null)
           }.show()
    }

    private fun setUpBoard(){
        when(boardSize){
            BoardSize.EASY -> {
                tvNumMoves.text = "EASY: 4 x 2"
                tvPaiMoves.text = "Pairs: 0/4"
            }
            BoardSize.MEDIUM -> {
                tvNumMoves.text = "EASY: 6 x 3"
                tvPaiMoves.text = "Pairs: 0/9"
            }
            BoardSize.HARD -> {
                tvNumMoves.text = "EASY: 6 x 4"
                tvPaiMoves.text = "Pairs: 0/12"
            }
        }
        tvPaiMoves.setTextColor(ContextCompat.getColor(this,R.color.color_progress_none))
        memoryGame = MemoryGame(boardSize)
        adapter = MemoryBoardAdapter(this, boardSize, memoryGame.cards, object: MemoryBoardAdapter.CardClickListener{
            override fun onCardClicked(position: Int) {
                Log.i(TAG, "Card clicked ${position} from main")
                updateGameWithFlip(position)
            }
        })
        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true);
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }

    fun updateGameWithFlip(position: Int){
        if(memoryGame.haveWonGame()){
            Snackbar.make(clRoot, "You already won!", Snackbar.LENGTH_LONG).show()
            return
        }
        if(memoryGame.isCardFaceUp(position)){
            Snackbar.make(clRoot, "Invalid Move!", Snackbar.LENGTH_LONG).show()
            return
        }
        if(memoryGame.flipCard(position)){
            Log.i(TAG,"Found a match! ${memoryGame.numOfPairs}")
            val color = ArgbEvaluator().evaluate(
                memoryGame.numOfPairs.toFloat() / boardSize.getNumPairs(),
                ContextCompat.getColor(this, R.color.color_progress_none),
                ContextCompat.getColor(this, R.color.color_progress_full),
            ) as Int
            tvPaiMoves.setTextColor(color)
            tvPaiMoves.text = "Pairs: ${memoryGame.numOfPairs} / ${boardSize.getNumPairs()}"
            if(memoryGame.haveWonGame()){
                Snackbar.make(clRoot, "You won!, congratulations!", Snackbar.LENGTH_LONG).show()
            }
        }
        tvNumMoves.text  = "Moves: ${memoryGame.getNumMoves()}"
        adapter.notifyDataSetChanged()
    }
}