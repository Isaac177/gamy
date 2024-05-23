package com.zeph7.tictactoe

import android.app.Dialog
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import kotlinx.android.synthetic.main.activity_won.*
import kotlinx.android.synthetic.main.modal_message.*

class WonActivity : AppCompatActivity() {
    private lateinit var socketManager: SocketManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_won)

        // Initialize SocketManager
        socketManager = SocketManager()
        socketManager.initSocket()

        val player = intent.getStringExtra("player")
        if (player == "Tie") textViewWon.text = "TIE"
        else textViewWon.text = "$player WON"

        val anim = AnimationUtils.loadAnimation(applicationContext, R.anim.zoom)
        textViewWon.startAnimation(anim)

        showGameOverModal(player)

        Handler().postDelayed({
            startActivity(Intent(this@WonActivity, MainActivity::class.java))
        }, 3000)
    }

    private fun showGameOverModal(player: String?) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.modal_message)
        dialog.setCancelable(false)

        if (player == "Tie") {
            dialog.modalMessage.text = "It's a Tie!"
        } else if (player == "YOU") {
            dialog.modalMessage.text = "You Won!"
        } else {
            dialog.modalMessage.text = "You Lost!"
        }

        dialog.closeModalButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        socketManager.disconnect()
    }
}
