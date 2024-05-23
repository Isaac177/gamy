package com.zeph7.tictactoe

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import io.socket.client.Socket
import kotlinx.android.synthetic.main.activity_second.*
import org.json.JSONObject
import kotlin.collections.ArrayList

class SecondActivity : AppCompatActivity() {
    private lateinit var socketManager: SocketManager
    private var board = arrayListOf("", "", "", "", "", "", "", "", "")
    private var isPlayerTurn = true // Assuming the player starts first
    private var isSinglePlayer = true // Assuming single player mode
    private var gameId: String? = null // Store the gameId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_second)

        // Initialize SocketManager
        socketManager = SocketManager.getInstance()

        setupSocketEvents()

        val anim = AnimationUtils.loadAnimation(applicationContext, R.anim.move)
        imageViewGameOn.startAnimation(anim)

        button0.setOnClickListener { onPlayerMove(0) }
        button1.setOnClickListener { onPlayerMove(1) }
        button2.setOnClickListener { onPlayerMove(2) }
        button3.setOnClickListener { onPlayerMove(3) }
        button4.setOnClickListener { onPlayerMove(4) }
        button5.setOnClickListener { onPlayerMove(5) }
        button6.setOnClickListener { onPlayerMove(6) }
        button7.setOnClickListener { onPlayerMove(7) }
        button8.setOnClickListener { onPlayerMove(8) }

        buttonReset.setOnClickListener {
            startActivity(Intent(this@SecondActivity, SecondActivity::class.java))
        }

        // back ImageView
        imageViewBack.setOnClickListener {
            startActivity(Intent(this@SecondActivity, MainActivity::class.java))
        }

        // quit ImageView
        imageViewQuit.setOnClickListener {
            finish()
            moveTaskToBack(true)   //to quit app
        }

        // Notify backend of single player mode
        socketManager.socket.on(Socket.EVENT_CONNECT) {
            val userId = socketManager.getUserId()
            val data = JSONObject()
            data.put("mode", "single")
            data.put("userId", userId)
            socketManager.socket.emit("createGame", data)
        }

        socketManager.socket.on("gameCreated") { args ->
            val data = args[0] as JSONObject
            gameId = data.getString("id")
        }
    }

    private fun setupSocketEvents() {
        socketManager.socket.on("gameUpdate") { args ->
            val data = args[0] as JSONObject
            val position = data.getInt("position")
            val symbol = data.getString("symbol")
            runOnUiThread {
                board[position] = symbol
                displaySymbolOnButton(position, symbol)
                resultOut(board)
                isPlayerTurn = true // It's the player's turn after receiving an update
            }
        }

        socketManager.socket.on("gameOver") { args ->
            val data = args[0] as JSONObject
            val winner = data.getString("winner")
            runOnUiThread {
                if (winner == "tie") {
                    startActivity(Intent(this@SecondActivity, WonActivity::class.java).putExtra("player", "Tie"))
                } else {
                    startActivity(Intent(this@SecondActivity, WonActivity::class.java).putExtra("player", winner))
                }
            }
        }
    }

    private fun onPlayerMove(position: Int) {
        if (board[position] == "" && isPlayerTurn) {
            board[position] = "X"
            displaySymbolOnButton(position, "X")
            resultOut(board)
            isPlayerTurn = false // Wait for the opponent's move

            if (isSinglePlayer) {
                // Handle computer move for single player mode
                if (!result(board, "X") && !isBoardFull(board)) {
                    val computerMove = getComputerMove(board)
                    board[computerMove] = "O"
                    displaySymbolOnButton(computerMove, "O")
                    resultOut(board)
                    isPlayerTurn = true // It's player's turn again after computer moves
                }
            } else {
                emitPlayerMove(position)
            }
        }
    }

    private fun emitPlayerMove(position: Int) {
        val data = JSONObject()
        data.put("position", position)
        data.put("symbol", "X")
        data.put("gameId", gameId) // Include gameId
        socketManager.socket.emit("playerMove", data)
    }

    private fun getComputerMove(board: ArrayList<String>): Int {
        // Implement computer move logic here
        for (i in board.indices) {
            val copy = getBoardCopy(board)
            if (copy[i] == "") {
                copy[i] = "O"
                if (result(copy, "O")) return i
            }
        }
        for (i in board.indices) {
            val copy = getBoardCopy(board)
            if (copy[i] == "") {
                copy[i] = "X"
                if (result(copy, "X")) return i
            }
        }
        val move = chooseRandomMove(board, arrayListOf(0, 2, 6, 8))
        if (move != -1) return move
        if (board[4] == "") return 4
        return chooseRandomMove(board, arrayListOf(1, 3, 5, 7))
    }

    private fun chooseRandomMove(board: ArrayList<String>, list: ArrayList<Int>): Int {
        val possibleMoves = arrayListOf<Int>()
        for (i in list) {
            if (board[i] == "") possibleMoves.add(i)
        }
        return if (possibleMoves.isEmpty()) -1 else possibleMoves.random()
    }

    private fun displaySymbolOnButton(position: Int, symbol: String) {
        when (position) {
            0 -> button0.text = symbol
            1 -> button1.text = symbol
            2 -> button2.text = symbol
            3 -> button3.text = symbol
            4 -> button4.text = symbol
            5 -> button5.text = symbol
            6 -> button6.text = symbol
            7 -> button7.text = symbol
            8 -> button8.text = symbol
        }
    }

    private fun getBoardCopy(board: ArrayList<String>): ArrayList<String> {
        return ArrayList(board)
    }

    private fun isBoardFull(board: ArrayList<String>): Boolean {
        return !board.contains("")
    }

    private fun resultOut(board: ArrayList<String>) {
        when {
            result(board, "X") -> {
                val data = JSONObject()
                data.put("winner", "YOU")
                data.put("userId", socketManager.getUserId())
                data.put("gameId", gameId) // Include gameId
                socketManager.socket.emit("gameOver", data)
                startActivity(Intent(this@SecondActivity, WonActivity::class.java).putExtra("player", "YOU"))
            }
            result(board, "O") -> {
                val data = JSONObject()
                data.put("winner", "COMPUTER")
                data.put("userId", socketManager.getUserId())
                data.put("gameId", gameId) // Include gameId
                socketManager.socket.emit("gameOver", data)
                startActivity(Intent(this@SecondActivity, WonActivity::class.java).putExtra("player", "COMPUTER"))
            }
            isBoardFull(board) -> {
                val data = JSONObject()
                data.put("winner", "tie")
                data.put("userId", socketManager.getUserId())
                data.put("gameId", gameId) // Include gameId
                socketManager.socket.emit("gameOver", data)
                startActivity(Intent(this@SecondActivity, WonActivity::class.java).putExtra("player", "Tie"))
            }
        }
    }

    private fun result(bd: ArrayList<String>, s: String): Boolean =
        (bd[0] == s && bd[1] == s && bd[2] == s) ||
                (bd[3] == s && bd[4] == s && bd[5] == s) ||
                (bd[6] == s && bd[7] == s && bd[8] == s) ||
                (bd[0] == s && bd[3] == s && bd[6] == s) ||
                (bd[1] == s && bd[4] == s && bd[7] == s) ||
                (bd[2] == s && bd[5] == s && bd[8] == s) ||
                (bd[0] == s && bd[4] == s && bd[8] == s) ||
                (bd[2] == s && bd[4] == s && bd[6] == s)

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this@SecondActivity, MainActivity::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        socketManager.disconnect()
    }
}
