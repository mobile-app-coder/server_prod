package server


import models.Client
import server.database.DataBase
import server.models.LoginModel
import server.socket.sendMessage
import java.sql.SQLException


fun parseMessage(message: String): ParsedMessage? {
    val parts = message.split(":", limit = 4) // Limit split to 4 parts

    // Ensure the minimum number of parts (userType, action, command) are present
    return if (parts.size >= 3) {
        val userType = parts[0]
        val action = parts[1]
        val command = parts[2]

        // Combine remaining parts into `parameters` to avoid splitting issues
        val parameters = if (parts.size == 4) parts[3] else ""

        ParsedMessage(userType, action, command, parameters)
    } else {
        null // Invalid message format
    }
}


data class ParsedMessage(
    val userType: String, val action: String, val command: String, val parameters: String
) {
    fun isValid(): Boolean {
        return userType.isNotEmpty() && action.isNotEmpty() && command.isNotEmpty()
    }
}


class Command {
    companion object {

        val connection = DataBase.getConnection()


        fun register(client: Client) {
            println("add")
            if (connection != null) {
                val insertQuery = """
            INSERT INTO clients (
            first_name,
            last_name, 
            email, 
            phone_number, 
            date_of_birth, 
            address) 
            VALUES (?, ?, ?, ?, ?, ?)
        """
                try {
                    val preparedStatement = connection.prepareStatement(insertQuery)
                    preparedStatement.setString(1, client.name)
                    preparedStatement.setString(2, client.name)
                    preparedStatement.setString(3, client.email)
                    preparedStatement.setString(4, client.phone)
                    preparedStatement.setString(5, client.date)
                    preparedStatement.setString(6, client.address)


                    // Execute the query
                    val rowsAffected = preparedStatement.executeUpdate()
                    if (rowsAffected > 0) {
                        println("New client added successfully.")
                    } else {
                        println("Failed to add the new client.")
                    }
                } catch (e: SQLException) {
                    println("Error inserting client: ${e.message}")
                }
            }

        }


        fun login(login: LoginModel, clientSocket: Int) {
            val loginquery = """SELECT * FROM clients 
                WHERE first_name = "${login.username}"
            """
            val result = DataBase.runQueryAndGetResults(loginquery)
            println(result)
            if (result.isNotEmpty()) {
                println(clientSocket)
                sendMessage(clientSocket = clientSocket, "login")
            } else {
                sendMessage(clientSocket, "fail")
            }
        }
    }
}
