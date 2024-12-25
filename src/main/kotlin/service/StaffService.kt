package server.commands

import server.database.DataBase
import server.models.LoginModel
import server.communication.sendMessage

object StaffService {
    fun login(login: LoginModel, clientSocket: Int) {
        val loginquery = """SELECT * FROM clients 
                WHERE first_name = "${login.login
                }"
            """
        val result = DataBase.runQuery(loginquery, true)
        println(result)
        if (result.isNotEmpty()) {
            println(clientSocket)
            sendMessage(clientSocket = clientSocket, "login")
        } else {
            sendMessage(clientSocket, "fail")
        }
    }
}