package server.socket

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

external fun stopServer()
external fun createAndListen(): Int
external fun acceptClient(): Int
external fun getMessage(clientSocket: Int): String?
external fun sendMessage(clientSocket: Int, message: String): Int

object Socket {
    var isAlive = true
    var serverSocket = -1
    var clients = mutableListOf<Int>()

    init {
        System.setProperty("java.library.path", "/home/shahriyor/IdeaProjects/server_prod/src/main/kotlin/csocket")
        System.load("/home/shahriyor/IdeaProjects/server_prod/src/main/kotlin/csocket/socket.so")
    }

    private var clientHandler: ((Int) -> Unit)? = null

    fun startSocketServer() {
        serverSocket = createAndListen()
    }

    fun stopServerSocket() {
        isAlive = false
        stopServer()
    }

    private suspend fun acceptClients() = coroutineScope {
        while (isAlive) {
            val clientSocket = acceptClient()
            if (clientSocket >= 0) {
                println("Client connected: $clientSocket")
                clients.add(clientSocket)
                launch { handleClient(clientSocket) }
            } else {
                delay(100) // Prevent busy-waiting
            }
        }
    }

    private suspend fun handleClient(clientSocket: Int) {
        while (isAlive) {
            val message = getMessage(clientSocket)
            if (message != null) {
                println("Received from $clientSocket: $message")
                val response = processRequest(message)
                sendMessage(clientSocket, response)
            } else {
                delay(100) // Prevent busy-waiting
            }
        }
        clients.remove(clientSocket)
        println("Client $clientSocket disconnected.")
    }

    private fun processRequest(message: String): String {
        // Business logic for message processing
        return "Processed: $message"
    }

    fun listen() {
        runBlocking {
            acceptClients()
        }
    }

    fun sendMessageToAllClients(message: String) {
        for (clientSocket in clients) {
            sendMessage(clientSocket, message)
        }
    }
}
