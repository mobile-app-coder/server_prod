package server.socket

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import server.commands.CommandCustomer
import server.commands.CommandStaff
import server.commands.GeneralCommand
import server.commands.parseMessage
import server.models.LoginModelGsonConverter

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
        val request = parseMessage(message)
        if (request != null) {
            when (request.action) {
                "client" -> {
                    customers.add(clientSocket)
                    when (request.command) {
                        "register" -> CommandCustomer.register(request.parameters, clientSocket)
                        "login" -> CommandCustomer.login(request.parameters, clientSocket)
                        "userdata" -> CommandCustomer.returnUserData(request.parameters, clientSocket)
                        "accounttype" -> GeneralCommand.getAccountType(clientSocket)
                    }
                }

                "staff" -> {
                    admins.add(clientSocket)
                    when (request.command) {
                        "login" -> {
                            println("login requested")
                            if (request.parameters.isNotEmpty()) {
                                println(request.parameters)
                                val login = LoginModelGsonConverter.fromJson(request.parameters)
                                if (login != null) {
                                    CommandStaff.login(login, clientSocket)
                                }
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
        for (clientSocket in customers) {
            sendMessage(clientSocket, message)
        }
    }


    fun sendMessageToClient(clientSocket: Int, message: String) {
        sendMessage(clientSocket, message)
    }
}
