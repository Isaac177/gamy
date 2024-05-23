package com.zeph7.tictactoe

import android.content.Intent
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
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

        // Handle game invitations
        handleGameInvitations()
    }

    private fun handleGameInvitations() {
        socketManager.socket.on("gameInvitation") { args ->
            val data = args[0] as JSONObject
            val gameId = data.getInt("gameId")
            val inviter = data.getString("inviter")

            runOnUiThread {
                showGameInvitationDialog(gameId, inviter)
            }
        }
    }

    private fun showGameInvitationDialog(gameId: Int, inviter: String) {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Game Invitation")
            .setMessage("You have been invited to join a game by $inviter. Do you want to join?")
            .setPositiveButton("Join") { _, _ ->
                joinGame(gameId)
            }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun joinGame(gameId: Int) {
        val intent = Intent(this@MainActivity, ThirdActivity::class.java)
        intent.putExtra("isMultiplayer", true) // Pass the multiplayer flag
        intent.putExtra("gameId", gameId.toString()) // Pass the game ID
        startActivity(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        moveTaskToBack(true)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            socketManager.disconnect()
            Log.d(TAG, "SocketManager disconnected")
        } catch (e: Exception) {
            Log.e(TAG, "Exception disconnecting SocketManager: ${e.message}")
            e.printStackTrace()
        }
    }
}
