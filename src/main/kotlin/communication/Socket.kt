package server.communication

import kotlinx.coroutines.*
import server.commands.UserServices
import server.logic.bank.StaffRepository
import server.logic.bank.UserRepository
import java.util.*


external fun stopServer()
external fun createAndListen(): Int
external fun acceptClient(): Int
external fun getMessage(clientSocket: Int): String?
external fun sendMessage(clientSocket: Int, message: String): Int
external fun getClientAddress(clientSocket: Int): String
external fun getClientPortAddress(clientSocket: Int): Int


object Socket {

    private var isAlive = true
    private var serverSocket = -1
    private val customers = Collections.synchronizedList(mutableListOf<Int>())
    private val admins = Collections.synchronizedList(mutableListOf<Int>())
    private val connectedClients = Collections.synchronizedSet(mutableSetOf<Int>())

    init {
        System.setProperty(
            "java.library.path", "/home/shahriyor/IdeaProjects/server_prod/src/main/kotlin/communication"
        )
        System.load("/home/shahriyor/IdeaProjects/server_prod/src/main/kotlin/communication/socket.so")
    }

    fun startSocketServer() {
        serverSocket = createAndListen()
    }

    fun stopServerSocket() {
        isAlive = false
        stopServer()
        synchronized(connectedClients) {
            connectedClients.forEach { closeClientSocket(it) }
        }
        println("Server stopped, all clients disconnected.")
    }

    private suspend fun acceptClients() = coroutineScope {
        while (isAlive) {
            val clientSocket = acceptClient()
            if (clientSocket >= 0) {
                synchronized(connectedClients) { connectedClients.add(clientSocket) }
                println("New client connected: $clientSocket")
                launch { handleClient(clientSocket) }
            } else {
                delay(50) // Shorter delay for responsiveness
            }
        }
    }

    private fun clearConsole() {
        // ANSI escape code to clear the screen
        print("\u001b[H\u001b[2J")
        System.out.flush()
    }

    private suspend fun handleClient(clientSocket: Int) {
        try {
            clearConsole()

            // Print header for new connection
            println("=== New Client Connected ===")
            println("Client Socket: $clientSocket")
            println("Client IP: ${getClientIP(clientSocket)}")
            println("Client Port: ${getClientPort(clientSocket)}")
            println("Total connected clients: ${connectedClients.size}")

            // Show all currently connected clients
            println("\n=== Current Connected Clients ===")
            synchronized(connectedClients) {
                connectedClients.forEach { socket ->
                    println("Client Socket: $socket | IP: ${getClientIP(socket)} | Port: ${getClientPort(socket)}")
                }
            }

            while (isAlive) {
                val message = getMessage(clientSocket)
                if (message != null) {
                    processRequest(message, clientSocket)
                }
            }
        } catch (e: Exception) {
            println("Error with client $clientSocket: ${e.message}")
        } finally {
            disconnectClient(clientSocket)
        }
    }

    fun getBankStaffSocket(): Int {
        return admins[0]
    }

    private fun processRequest(message: String, clientSocket: Int) {
        try {
            val request = UserServices.parseMessage(message)
            if (request != null) {
                when (request.action) {
                    "client" -> {
                        synchronized(customers) { customers.add(clientSocket) }
                        UserRepository.processRequest(message, clientSocket)
                    }

                    "staff" -> {
                        synchronized(admins) { admins.add(clientSocket) }
                        StaffRepository.processRequest(message, clientSocket)
                    }

                    else -> throw IllegalArgumentException("Unknown action: ${request.action}")
                }
            } else {
                throw IllegalArgumentException("Invalid request format.")
            }
        } catch (e: Exception) {
            val errorMessage = "Error processing request: ${e.message}"
            println(errorMessage)
            sendMessage(clientSocket, "error|$errorMessage")
        }
    }

    private fun disconnectClient(clientSocket: Int) {
        synchronized(connectedClients) { connectedClients.remove(clientSocket) }
        synchronized(customers) { customers.remove(clientSocket) }
        synchronized(admins) { admins.remove(clientSocket) }
        println("Client disconnected: $clientSocket")
    }

    private fun closeClientSocket(clientSocket: Int) {
        sendMessage(clientSocket, "disconnect|Server closing connection.")
        // Native or manual socket closure logic
    }

    fun listen() {
        CoroutineScope(Dispatchers.IO).launch {
            acceptClients()
        }
    }

    fun sendMessageToAllClients(message: String) {
        synchronized(connectedClients) {
            for (clientSocket in connectedClients) {
                sendMessage(clientSocket, message)
            }
        }
    }

    fun sendMessageToClient(clientSocket: Int, message: String) {
        sendMessage(clientSocket, message)
    }

    private fun getClientIP(clientSocket: Int): String = getClientAddress(clientSocket)
    private fun getClientPort(clientSocket: Int): Int = getClientPortAddress(clientSocket)
}

