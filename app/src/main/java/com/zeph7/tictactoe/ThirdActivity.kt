package com.zeph7.tictactoe

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.support.v7.app.AppCompatActivity
import io.socket.client.Socket
import kotlinx.android.synthetic.main.activity_third.*
import org.json.JSONObject

class ThirdActivity : AppCompatActivity() {
    private lateinit var socketManager: SocketManager
    private var board = arrayListOf("", "", "", "", "", "", "", "", "")
    private var isPlayerTurn = false // Default to false until determined
    private var playerSymbol = "X" // Default symbol, will be set by server
    private var gameId: String = "" // Add gameId to keep track of the game
    private var isMultiplayer = false // Flag to check if the game is multiplayer
    private var chance = "X"

    companion object {
        const val TAG = "ThirdActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_third)

        val anim = AnimationUtils.loadAnimation(applicationContext, R.anim.move)
        imageViewGameOn.startAnimation(anim)

        // Initialize SocketManager
        socketManager = SocketManager.getInstance()

        // Check if the game is in multiplayer mode
        isMultiplayer = intent.getBooleanExtra("isMultiplayer", false)
        gameId = intent.getStringExtra("gameId") ?: ""
        Log.d(TAG, "isMultiplayer: $isMultiplayer, gameId: $gameId")

        if (isMultiplayer) {
            setupSocketEvents()

            // Notify backend of multiplayer mode
            socketManager.socket.on(Socket.EVENT_CONNECT) {
                if (gameId.isEmpty()) {
                    // If gameId is empty, create a new game
                    val userId = socketManager.getUserId()
                    if (userId != null) {
                        val data = JSONObject().apply {
                            put("mode", "multi")
                            put("userId", userId)
                        }
                        Log.d(TAG, "Emitting createGame event: $data")
                        socketManager.socket.emit("createGame", data)
                    } else {
                        Log.e(TAG, "Socket ID is null, cannot emit createGame event")
                    }
                } else {
                    // Join an existing game
                    val data = JSONObject().apply {
                        put("gameId", gameId)
                    }
                    Log.d(TAG, "Emitting joinGame event: $data")
                    socketManager.socket.emit("joinGame", data)
                }
            }
        }

        // Set click listeners for buttons
        setButtonClickListeners()
    }

    private fun setButtonClickListeners() {
        listOf(button0, button1, button2, button3, button4, button5, button6, button7, button8).forEachIndexed { index, button ->
            button.setOnClickListener { onPlayerMove(index) }
        }
    }

    private fun setupSocketEvents() {
        Log.d(TAG, "Setting up socket events")
        socketManager.socket.on("gameUpdate") { args ->
            val data = args[0] as JSONObject
            val position = data.getInt("position")
            val symbol = data.getString("symbol")
            Log.d(TAG, "Received gameUpdate: position $position, symbol $symbol")
            runOnUiThread {
                board[position] = symbol
                displaySymbolOnButton(position, symbol)
                isPlayerTurn = symbol != playerSymbol // Update the turn status
                Log.d(TAG, "Player turn updated to: $isPlayerTurn after receiving gameUpdate")
            }
        }

        socketManager.socket.on("gameOver") { args ->
            val data = args[0] as JSONObject
            val winner = data.getString("winner")
            Log.d(TAG, "Game over, winner: $winner")
            runOnUiThread {
                startActivity(Intent(this@ThirdActivity, WonActivity::class.java).putExtra("player", winner.ifEmpty { "Tie" }))
            }
        }

        socketManager.socket.on("gameCreated") { args ->
            val data = args[0] as JSONObject
            gameId = data.getString("id")
            Log.d(TAG, "Game created with id: $gameId")
        }

        socketManager.socket.on("playerAssigned") { args ->
            val data = args[0] as JSONObject
            playerSymbol = data.getString("symbol")
            isPlayerTurn = playerSymbol == "X" // Player X starts the game
            Log.d(TAG, "Player assigned symbol: $playerSymbol, isPlayerTurn: $isPlayerTurn")
        }

        socketManager.socket.on("gameStart") { args ->
            val data = args[0] as JSONObject
            gameId = data.getString("id")
            val player1Id = data.getString("player1Id")
            val player2Id = data.getString("player2Id")
            Log.d(TAG, "Game started with id: $gameId")
            val userId = socketManager.getUserId()
            if (userId == player1Id) {
                playerSymbol = "X"
                isPlayerTurn = true
            } else if (userId == player2Id) {
                playerSymbol = "O"
                isPlayerTurn = false
            }
            Log.d(TAG, "Player symbol: $playerSymbol, isPlayerTurn: $isPlayerTurn")
        }
    }

    private fun onPlayerMove(position: Int) {
        Log.d(TAG, "Button $position clicked")

        if (board[position].isEmpty() && isPlayerTurn) {
            Log.d(TAG, "It's player turn and the board position is empty")
            board[position] = playerSymbol
            displaySymbolOnButton(position, playerSymbol)
            resultOut(board)
            emitPlayerMove(position)
            isPlayerTurn = false // Wait for the opponent's move
            Log.d(TAG, "Player move emitted, isPlayerTurn set to false")
        } else {
            Log.d(TAG, "Conditions not met for emitting playerMove: isMultiplayer=$isMultiplayer, board[position]=${board[position]}, isPlayerTurn=$isPlayerTurn")
            return // Early return if conditions are not met
        }

        if (!isMultiplayer) {
            // Handle local game turn switch
            chance = if (chance == "X") "O" else "X"
        }
    }

    private fun emitPlayerMove(position: Int) {
        val data = JSONObject().apply {
            put("position", position)
            put("symbol", playerSymbol)
            put("gameId", gameId) // Ensure gameId is sent with the move
            put("userId", socketManager.getUserId()) // Add userId to the move data
        }
        Log.d(TAG, "Emitting playerMove: position $position, symbol $playerSymbol, gameId $gameId")
        socketManager.socket.emit("playerMove", data)
    }

    private fun buttonAtPosition(position: Int) = when (position) {
        0 -> button0
        1 -> button1
        2 -> button2
        3 -> button3
        4 -> button4
        5 -> button5
        6 -> button6
        7 -> button7
        8 -> button8
        else -> throw IllegalArgumentException("Invalid position")
    }

    private fun displaySymbolOnButton(position: Int, symbol: String) {
        Log.d(TAG, "Displaying symbol $symbol on position $position")
        buttonAtPosition(position).text = symbol
    }

    private fun isBoardFull(board: ArrayList<String>): Boolean {
        return board.all { it == "X" || it == "O" }
    }

    private fun resultOut(board: ArrayList<String>) {
        when {
            result(board, "X") -> {
                if (isMultiplayer) {
                    socketManager.socket.emit("gameOver", JSONObject().put("winner", "X").put("gameId", gameId))
                }
                startActivity(Intent(this@ThirdActivity, WonActivity::class.java).putExtra("player", "X"))
            }
            result(board, "O") -> {
                if (isMultiplayer) {
                    socketManager.socket.emit("gameOver", JSONObject().put("winner", "O").put("gameId", gameId))
                }
                startActivity(Intent(this@ThirdActivity, WonActivity::class.java).putExtra("player", "O"))
            }
            isBoardFull(board) -> {
                if (isMultiplayer) {
                    socketManager.socket.emit("gameOver", JSONObject().put("winner", "tie").put("gameId", gameId))
                }
                startActivity(Intent(this@ThirdActivity, WonActivity::class.java).putExtra("player", "Tie"))
            }
        }
    }

    private fun result(bd: ArrayList<String>, s: String): Boolean {
        return when {
            bd[0] == s && bd[1] == s && bd[2] == s -> true
            bd[3] == s && bd[4] == s && bd[5] == s -> true
            bd[6] == s && bd[7] == s && bd[8] == s -> true
            bd[0] == s && bd[3] == s && bd[6] == s -> true
            bd[1] == s && bd[4] == s && bd[7] == s -> true
            bd[2] == s && bd[5] == s && bd[8] == s -> true
            bd[0] == s && bd[4] == s && bd[8] == s -> true
            bd[2] == s && bd[4] == s && bd[6] == s -> true
            else -> false
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        startActivity(Intent(this@ThirdActivity, MainActivity::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isMultiplayer) {
            socketManager.disconnect()
        }
    }
}
