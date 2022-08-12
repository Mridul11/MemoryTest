package com.supercoolapps.memorytest

import android.animation.ArgbEvaluator
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import com.supercoolapps.models.BoardSize
import com.supercoolapps.models.MemoryGame
import com.supercoolapps.models.UserImageList
import com.supercoolapps.utils.EXTRA_BOARD_SIZE
import com.supercoolapps.utils.EXTRA_GAME_NAME

class MainActivity : AppCompatActivity() {

    private lateinit var adapter: MemoryBoardAdapter
    private lateinit var memoryGame: MemoryGame
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvPaiMoves: TextView
    private lateinit var clRoot : ConstraintLayout

    private val db = Firebase.firestore
    private var gameName: String? = null
    private var customGameImages: List<String>? = null
    private var boardSize : BoardSize = BoardSize.EASY

    companion object{
        private const val TAG = "MainActivity"
        private const val CREATE_REQUEST_CODE = 111
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
            R.id.mi_download -> {
                showDownloadDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showDownloadDialog() {
       val boardDownloadView = LayoutInflater.from(this).inflate(R.layout.dialog_download_board, null)
        showAlertDialog("Fetch Memroy game", boardDownloadView, View.OnClickListener {
            // Grab the  text of the game name that the user  want to download..
            val etDownloadGame = boardDownloadView.findViewById<EditText>(R.id.etDownloadGame)
            val gameToDownload = etDownloadGame.text.toString()
            downLoadGame(gameToDownload)
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == CREATE_REQUEST_CODE && resultCode == Activity.RESULT_OK){
            val customGameName = data?.getStringExtra(EXTRA_GAME_NAME)
            if(customGameName == null){
                Log.e(TAG, "Got null from CreateActivity!")
                return
            }
            downLoadGame(customGameName)
        }

    }

    private fun downLoadGame(customGameName: String) {
        db.collection("games").document(customGameName).get().addOnSuccessListener {document ->
                val userImageList = document.toObject(UserImageList::class.java)
                if(userImageList?.images == null){
                    Log.e(TAG, "Invalid custom game data from FireStore! ")
                    Snackbar.make(clRoot, "Sorry, we couldnt find any such game, $customGameName", Snackbar.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
            val numCards = userImageList.images.size * 2
            boardSize = BoardSize.getbyValue(numCards)
            customGameImages = userImageList.images
            for(imageUrl in userImageList.images){
                Picasso.get().load(imageUrl).fetch()
            }
            Snackbar.make(clRoot, "You are now playing $customGameName !", Snackbar.LENGTH_LONG).show()
            gameName = customGameName
            setUpBoard()
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Exception while retriveing game ", exception)
        }
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
            startActivityForResult(intent, CREATE_REQUEST_CODE)
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
           gameName = null
           customGameImages = null
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
        supportActionBar?.title = gameName ?: getString(R.string.app_name)
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
        memoryGame = MemoryGame(boardSize, customGameImages)
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