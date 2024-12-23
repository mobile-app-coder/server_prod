package server.socket

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import server.Command
import server.models.parseJsonToLoginModel
import server.parseMessage

external fun stopServer()
external fun createAndListen(): Int
external fun acceptClient(): Int
external fun getMessage(clientSocket: Int): String?
external fun sendMessage(clientSocket: Int, message: String): Int

object Socket {
    var isAlive = true
    var serverSocket = -1
    var clients = mutableListOf<Int>()
    var admins = mutableListOf<Int>()

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
                launch { handleClient(clientSocket) }
                clients.add(clientSocket)
            } else {
                delay(100) // Prevent busy-waiting
            }
        }
    }

    private suspend fun handleClient(clientSocket: Int) {
        while (isAlive) {
            val message = getMessage(clientSocket)
            if (message != null) {
                println("$clientSocket: $message")
                processRequest(message, clientSocket)
            } else {
                delay(100) // Prevent busy-waiting
            }
        }
        clients.remove(clientSocket)
        println("Client $clientSocket disconnected.")
    }

    private fun processRequest(message: String, clientSocket: Int): String {
        val request = parseMessage(message)
        if (request != null) {

            when (request.action) {
                "client" -> {
                    when (request.command) {
                        "register" -> Command.register(request.parameters, clientSocket)
                    }
                }

                "staff" -> {
                    when (request.command) {
                        "login" -> {
                            println("login requested")
                            if (request.parameters.isNotEmpty()) {
                                println(request.parameters)
                                val login = parseJsonToLoginModel(request.parameters)
                                Command.login(parseJsonToLoginModel(request.parameters), clientSocket)
                            }
                        }
                    }
                }
            }
        }
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


    fun sendMessageToClient(clientSocket: Int, message: String) {
        sendMessage(clientSocket, message)
    }
}
