package server


import models.UserGsonConverter
import server.database.DataBase
import server.models.LoginModel
import server.socket.Socket
import server.socket.sendMessage
import java.sql.SQLException
import java.sql.Statement


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


object Command {

    private val connection = DataBase.getConnection()
    fun register(parameters: String, clientSocket: Int) {

        val client = UserGsonConverter.fromJson(parameters)

        if (connection != null && client != null) {
            val insertQuery = """
            INSERT INTO users (
                first_name,
                last_name, 
                email, 
                phone_number, 
                date_of_birth,
                address
                ) 
            VALUES (?, ?, ?, ?, ?, ?)
        """
            try {
                val preparedStatement = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)
                // Split the name into first and last names
                val names = client.name.split(" ")
                val firstName = names.getOrNull(0) ?: ""
                val lastName = names.getOrNull(1) ?: ""

                // Set the parameters for the prepared statement
                preparedStatement.setString(1, firstName) // first name
                preparedStatement.setString(2, lastName) // last name
                preparedStatement.setString(3, client.email) // email
                preparedStatement.setString(4, client.phone) // phone
                preparedStatement.setString(5, formatDate(client.date)) // date of birth
                preparedStatement.setString(6, client.address) // address

                // Execute the query
                val rowsAffected = preparedStatement.executeUpdate()
                if (rowsAffected > 0) {
                    println("New client added successfully.")
                    val generatedKeys = preparedStatement.generatedKeys
                    if (generatedKeys.next()) {
                        val userId = generatedKeys.getInt(1) // Get the generated user_id
                        Socket.sendMessageToClient(clientSocket, userId.toString())
                    }
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


fun formatDate(date: String): String {
    return if (date.length == 8) {
        // Convert from DDMMYYYY to YYYY-MM-DD
        val day = date.substring(0, 2)
        val month = date.substring(2, 4)
        val year = date.substring(4, 8)
        "$year-$month-$day" // Format as YYYY-MM-DD
    } else {
        date // If the date is not in the expected format, return it as-is or handle it as an error
    }
}
