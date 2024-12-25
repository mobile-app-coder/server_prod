package server.logic.bank

import server.commands.StaffService
import server.commands.parseMessage
import server.models.LoginModelGsonConverter

object StaffRepository {
    fun processRequest(message: String, clientSocket: Int): String {
        val request = parseMessage(message)
        if (request != null) {
            when (request.action) {
                "staff" -> {
                    when (request.command) {
                        "login" -> {
                            println("login requested")
                            if (request.parameters.isNotEmpty()) {
                                println(request.parameters)
                                val login = LoginModelGsonConverter.fromJson(request.parameters)
                                if (login != null) {
                                    StaffService.login(login, clientSocket)
                                }
                            }
                        }
                    }
                }
            }
        }
        return "Processed: $message"
    }
}