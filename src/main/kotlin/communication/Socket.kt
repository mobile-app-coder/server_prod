package server.communication

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import server.commands.UserServices
import server.logic.bank.StaffRepository
import server.logic.bank.UserRepository

external fun stopServer()
external fun createAndListen(): Int
external fun acceptClient(): Int
external fun getMessage(clientSocket: Int): String?
external fun sendMessage(clientSocket: Int, message: String): Int

object Socket {

    private var isAlive = true
    private var serverSocket = -1
    private var customers = mutableListOf<Int>()
    private var admins = mutableListOf<Int>()

    init {
        System.setProperty(
            "java.library.path",
            "/home/shahriyor/IdeaProjects/server_prod/src/main/kotlin/communication"
        )
        System.load("/home/shahriyor/IdeaProjects/server_prod/src/main/kotlin/communication/socket.so")
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
                processRequest(message, clientSocket)
            } else {
                delay(100) // Prevent busy-waiting
            }
        }
        println("Client $clientSocket disconnected.")
    }

    private fun processRequest(message: String, clientSocket: Int): String {
        val request = UserServices.parseMessage(message)
        if (request != null) {
            when (request.action) {
                "client" -> {
                    customers.add(clientSocket)
                    UserRepository.processRequest(message = message, clientSocket = clientSocket)
                }

                "staff" -> {
                    admins.add(clientSocket)
                    StaffRepository.processRequest(message = message, clientSocket = clientSocket)
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
        for (clientSocket in customers) {
            sendMessage(clientSocket, message)
        }
    }


    fun sendMessageToClient(clientSocket: Int, message: String) {
        sendMessage(clientSocket, message)
    }
}
