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

        socketManager = SocketManager.getInstance()

        val winner = intent.getStringExtra("winner")
        if (winner == "Tie") {
            textViewWon.text = "TIE"
        } else if (winner == socketManager.socket.id()) {
            textViewWon.text = "YOU WON"
        } else {
            textViewWon.text = "YOU LOST"
        }

        val anim = AnimationUtils.loadAnimation(applicationContext, R.anim.zoom)
        textViewWon.startAnimation(anim)

        showGameOverModal(winner)

        Handler().postDelayed({
            startActivity(Intent(this@WonActivity, MainActivity::class.java))
        }, 3000)
    }

    private fun showGameOverModal(winner: String?) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.modal_message)
        dialog.setCancelable(false)

        if (winner == "Tie") {
            dialog.modalMessage.text = "It's a Tie!"
        } else if (winner == socketManager.socket.id()) {
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
