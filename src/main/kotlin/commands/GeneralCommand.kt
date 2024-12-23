package server.commands

import models.AccountType
import models.AccountTypeGsonConverter
import server.database.DataBase
import server.socket.Socket
import java.sql.ResultSet
import java.sql.SQLException

object GeneralCommand {

    fun getAccountType(clientSocket: Int) {

        println("get account type called")
        val accountTypes = mutableListOf<AccountType>()
        val connection = DataBase.getConnection()

        try {
            val query = """SELECT * FROM AccountType"""
            val statement = connection?.createStatement()

            if (statement != null) {
                val resultSet: ResultSet = statement.executeQuery(query)

                // Populate the list with data from the result set
                while (resultSet.next()) {
                    val accountTypeId = resultSet.getInt("accountTypeId")
                    val typeName = resultSet.getString("typeName")
                    val description = resultSet.getString("description")
                    val features = resultSet.getString("features")

                    // Add AccountType to the list
                    accountTypes.add(AccountType(accountTypeId, typeName, description, features))
                }

                // Serialize the list to JSON and send to client after all data is retrieved
                val message = AccountTypeGsonConverter.toJsonList(accountTypes)
                if (message != null) {
                    println(message)
                    Socket.sendMessageToClient(clientSocket, message)
                } else {
                    println("Error: Failed to convert account types to JSON")
                }
            }

        } catch (e: SQLException) {
            e.printStackTrace()  // Log the exception for debugging
        } finally {
            // Ensure the database connection is closed (if needed)
            connection?.close()
        }
    }
}
