package com.zeph7.tictactoe

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import java.net.URISyntaxException

class SocketManager() {
    lateinit var socket: Socket

    companion object {
        private var instance: SocketManager? = null
        const val TAG = "SocketManager"

        @Synchronized
        fun getInstance(): SocketManager {
            if (instance == null) {
                instance = SocketManager()
                instance!!.initSocket()
            }
            return instance!!
        }
    }

    fun getUserId(): String? {
        return if (::socket.isInitialized && socket.connected()) {
            socket.id()
        } else {
            null
        }
    }

    fun initSocket() {
        try {
            val opts = IO.Options()
            opts.transports = arrayOf("websocket", "polling")
            //socket = IO.socket("https://multiplayer-game-api.fly.dev/", opts)
            socket = IO.socket("http://10.0.2.2:8000")
            socket.connect()
            setupSocketEvents()
            Log.d(TAG, "Socket initialized and connecting...")
        } catch (e: URISyntaxException) {
            Log.e(TAG, "URISyntaxException: ${e.message}")
            e.printStackTrace()
        } catch (e: Exception) {
            Log.e(TAG, "Exception during socket initialization: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun setupSocketEvents() {
        socket.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "Connected to server with socket id: ${socket.id()}")
        }

        socket.on(Socket.EVENT_DISCONNECT) {
            Log.d(TAG, "Disconnected from server")
        }

        socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
            Log.e(TAG, "Connection error: ${args[0]}")
        }
    }

    fun disconnect() {
        if (this::socket.isInitialized) {
            socket.disconnect()
            Log.d(TAG, "Socket disconnected")
        }
    }
}
