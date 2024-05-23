package com.zeph7.tictactoe

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var socketManager: SocketManager

    companion object {
        const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_main)

        // Initialize SocketManager
        socketManager = SocketManager.getInstance()
        Log.d(TAG, "SocketManager initialized")
        setupSocketEvents()

        val animZoom2 = AnimationUtils.loadAnimation(applicationContext, R.anim.zoom2)
        val animBlink = AnimationUtils.loadAnimation(applicationContext, R.anim.blink)
        imgTicTacToe.startAnimation(animBlink)

        btnSingle.setOnClickListener {
            btnSingle.startAnimation(animZoom2)
            startActivity(Intent(this@MainActivity, SecondActivity::class.java))
        }

        btnMulti.setOnClickListener {
            btnMulti.startAnimation(animZoom2)
            val intent = Intent(this@MainActivity, ThirdActivity::class.java)
            intent.putExtra("isMultiplayer", true) // Pass the multiplayer flag
            startActivity(intent)
        }

        imgTicTacToe.setOnClickListener {
            imgTicTacToe.startAnimation(animBlink)
        }

        // back ImageView
        imageViewBack.setOnClickListener {
            finish()
            moveTaskToBack(true)
        }

        // quit ImageView
        imageViewQuit.setOnClickListener {
            finish()
            moveTaskToBack(true)
        }
    }

    private fun setupSocketEvents() {
        Log.d(TAG, "Setting up socket events")

        socketManager.socket.on("invitationReceived") { args ->
            val data = args[0] as JSONObject
            val gameId = data.getString("gameId")
            val senderId = data.getString("senderId")
            Log.d(TAG, "Invitation received from: $senderId for gameId: $gameId")

            // Handle the invitation (e.g., show a dialog to accept or decline)
            runOnUiThread {
                showInvitationDialog(gameId, senderId)
            }
        }
    }

    private fun showInvitationDialog(gameId: String, inviter: String) {
        AlertDialog.Builder(this)
            .setTitle("Game Invitation")
            .setMessage("You have been invited to a game by $inviter")
            .setPositiveButton("Accept") { _, _ ->
                joinGame(gameId)
            }
            .setNegativeButton("Decline", null)
            .show()
    }

    private fun joinGame(gameId: String) {
        val joinData = JSONObject().apply {
            put("gameId", gameId)
            put("userId", socketManager.socket.id())
        }
        socketManager.socket.emit("joinGame", joinData)
        Log.d(TAG, "Joining game with id: $gameId")

        // Start ThirdActivity and pass the gameId
        val intent = Intent(this, ThirdActivity::class.java)
        intent.putExtra("gameId", gameId)
        intent.putExtra("isMultiplayer", true)
        startActivity(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        moveTaskToBack(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        socketManager.disconnect()
        Log.d(TAG, "SocketManager disconnected")
    }
}
