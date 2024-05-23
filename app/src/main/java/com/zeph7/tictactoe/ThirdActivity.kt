package com.zeph7.tictactoe

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.support.v7.app.AppCompatActivity
import android.text.InputType
import android.widget.EditText
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
    private var gameCreated = false // Flag to ensure single game creation
    private var opponentId = "" // Store the opponent's ID
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
        Log.d(TAG, "SocketManager instance acquired")

        // Check if the game is in multiplayer mode
        isMultiplayer = intent.getBooleanExtra("isMultiplayer", false)
        gameId = intent.getStringExtra("gameId").orEmpty()
        Log.d(TAG, "isMultiplayer: $isMultiplayer, gameId: $gameId")

        if (isMultiplayer) {
            setupSocketEvents()

            // If gameId exists, join the game instead of creating a new one
            if (gameId.isNotEmpty()) {
                joinExistingGame()
            } else {
                // Notify backend of multiplayer mode after ensuring socket is connected
                if (socketManager.socket.connected()) {
                    emitInitializeAndCreateGame()
                } else {
                    socketManager.socket.on(Socket.EVENT_CONNECT) {
                        runOnUiThread {
                            emitInitializeAndCreateGame()
                        }
                    }
                }
            }
        }

        // Set click listeners for buttons
        setButtonClickListeners()

        buttonReset.setOnClickListener {
            startActivity(Intent(this@ThirdActivity, ThirdActivity::class.java))
        }

        // back ImageView
        imageViewBack.setOnClickListener {
            startActivity(Intent(this@ThirdActivity, MainActivity::class.java))
        }

        // quit ImageView
        imageViewQuit.setOnClickListener {
            finish()
            moveTaskToBack(true) //to quit app
        }
    }

    private fun joinExistingGame() {
        val joinData = JSONObject().apply {
            put("gameId", gameId)
            put("userId", socketManager.socket.id())
        }
        Log.d(TAG, "Joining existing game with id: $gameId")
        socketManager.socket.emit("joinGame", joinData)
    }

    private fun emitInitializeAndCreateGame() {
        if (gameCreated) {
            return
        }
        gameCreated = true
        val initializeData = JSONObject().apply {
            put("mode", "multi")
        }
        Log.d(TAG, "Emitting initialize event: $initializeData")
        socketManager.socket.emit("initialize", initializeData)

        val data = JSONObject().apply {
            put("mode", "multi")
            put("userId", socketManager.socket.id()) // Use the public getter method for the socket ID
        }
        Log.d(TAG, "Emitting createGame event: $data")
        socketManager.socket.emit("createGame", data)
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
                isPlayerTurn = (symbol != playerSymbol) // Update turn status
                Log.d(TAG, "Player turn updated to: $isPlayerTurn after receiving gameUpdate")
            }
        }

        socketManager.socket.on("turnUpdate") { args ->
            val data = args[0] as JSONObject
            val nextTurn = data.getString("nextTurn")
            Log.d(TAG, "Received turnUpdate: nextTurn $nextTurn")
            runOnUiThread {
                isPlayerTurn = (playerSymbol == nextTurn)
                Log.d(TAG, "Player turn updated to: $isPlayerTurn after receiving turnUpdate")
            }
        }

        socketManager.socket.on("gameOver") { args ->
            val data = args[0] as JSONObject
            val winner = data.getString("winner")
            Log.d(TAG, "Game over, winner: $winner")
            runOnUiThread {
                startActivity(Intent(this@ThirdActivity, WonActivity::class.java).putExtra("winner", winner.ifEmpty { "Tie" }))
            }
        }

        socketManager.socket.on("gameCreated") { args ->
            val data = args[0] as JSONObject
            gameId = data.getString("id")
            opponentId = data.getString("opponentId") // Get opponent ID
            Log.d(TAG, "Game created with id: $gameId, opponentId: $opponentId")
            runOnUiThread {
                if (playerSymbol == "X") {
                    try {
                        showInviteOthersDialog(opponentId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error showing invite others dialog: ${e.message}")
                    }
                }
            }
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
            val userId = socketManager.socket.id()
            Log.d(TAG, "Game started with id: $gameId, player1Id: $player1Id, player2Id: $player2Id, userId: $userId")
            if (userId == player1Id) {
                playerSymbol = "X"
                isPlayerTurn = true
                Log.d(TAG, "Player 1 starts: isPlayerTurn = true")
            } else if (userId == player2Id) {
                playerSymbol = "O"
                isPlayerTurn = false
                Log.d(TAG, "Player 2 starts: isPlayerTurn = false")
            }
            Log.d(TAG, "Player symbol: $playerSymbol, isPlayerTurn: $isPlayerTurn")
        }
    }

    private fun showInviteOthersDialog(opponentId: String) {
        try {
            if (opponentId.isNotEmpty()) {
                sendInvitation(opponentId)
            } else {
                // Otherwise, show the dialog to enter the opponent's ID
                AlertDialog.Builder(this)
                    .setTitle("Invite Others")
                    .setMessage("Do you want to invite others to join the game?")
                    .setPositiveButton("Yes") { _, _ ->
                        showEnterOpponentIdDialog(opponentId)
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing invite others dialog: ${e.message}")
        }
    }

    private fun showEnterOpponentIdDialog(receivedOpponentId: String) {
        try {
            val input = EditText(this)
            input.inputType = InputType.TYPE_CLASS_TEXT

            AlertDialog.Builder(this)
                .setTitle("Enter Opponent ID")
                .setMessage("Please enter the opponent's ID: $receivedOpponentId")
                .setView(input)
                .setPositiveButton("Send Invitation") { _, _ ->
                    val opponentId = input.text.toString()
                    sendInvitation(opponentId)
                }
                .setNegativeButton("Cancel", null)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing enter opponent ID dialog: ${e.message}")
        }
    }

    private fun sendInvitation(opponentId: String) {
        val invitationData = JSONObject().apply {
            put("gameId", gameId)
            put("opponentId", opponentId)
            put("senderId", socketManager.socket.id())
        }
        socketManager.socket.emit("sendInvitation", invitationData)
        Log.d(TAG, "Invitation sent: $invitationData")
    }


    private fun onPlayerMove(position: Int) {
        Log.d(TAG, "Button $position clicked")
        Log.d(TAG, "Current player turn status: $isPlayerTurn")

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
    }

    private fun emitPlayerMove(position: Int) {
        val data = JSONObject().apply {
            put("position", position)
            put("symbol", playerSymbol)
            put("gameId", gameId) // Ensure gameId is sent with the move
            put("userId", socketManager.socket.id()) // Use the public getter method for the socket ID
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
        socketManager.disconnect()
        Log.d(TAG, "SocketManager disconnected")
    }
}
