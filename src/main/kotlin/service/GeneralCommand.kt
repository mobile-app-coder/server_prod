package server.commands

import model.AccountType
import model.AccountTypeGsonConverter
import server.database.DataBase
import server.models.Currency
import server.models.CurrencyGsonConverter
import server.communication.Socket
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

    fun getCurrencies(clientSocket: Int) {
        val connection = DataBase.getConnection()

        if (connection != null) {
            val query = """
            SELECT currencyId, code, name, symbol, exchangeRate, status
            FROM Currencies
            WHERE status = 'Active'
        """

            try {
                val statement = connection.prepareStatement(query)
                val resultSet = statement.executeQuery()

                val currencies = mutableListOf<Currency>()
                while (resultSet.next()) {
                    val currency = Currency(
                        currencyId = resultSet.getInt("currencyId"),
                        code = resultSet.getString("code"),
                        name = resultSet.getString("name"),
                        symbol = resultSet.getString("symbol"),
                        exchangeRate = resultSet.getDouble("exchangeRate"),
                        status = resultSet.getString("status")
                    )
                    currencies.add(currency)
                }

                if (currencies.isNotEmpty()) {
                    val currenciesJson = CurrencyGsonConverter.toJsonList(currencies)
                    if (currenciesJson != null) {
                        Socket.sendMessageToClient(clientSocket, currenciesJson)
                    }
                    println("Currencies data sent successfully.")
                } else {
                    Socket.sendMessageToClient(clientSocket, "error: currencies_not_found")
                    println("Error: No active currencies found.")
                }

            } catch (e: SQLException) {
                println("Error fetching currencies: ${e.message}")
                Socket.sendMessageToClient(clientSocket, "error: database_error")
            }
        } else {
            Socket.sendMessageToClient(clientSocket, "error: database_connection_error")
            println("Error: Database connection is null.")
        }
    }
}
