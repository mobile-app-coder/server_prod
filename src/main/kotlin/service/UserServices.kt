package server.commands


import model.AccountGsonConverter
import model.BankAccount
import model.User
import model.UserGsonConverter
import server.communication.Socket
import server.database.DataBase
import server.model.AccountTransferGsonConverter
import server.model.DepositGsonConverter
import server.models.LoginModelGsonConverter
import server.repository.DepositRepository
import server.repository.TransactionRepository
import java.security.MessageDigest
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement


object UserServices {
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

                // Validate and format dateOfBirth
                val formattedDateOfBirth = if (client.dateOfBirth.isNotEmpty()) {
                    formatDate(client.dateOfBirth)
                } else {
                    // Handle invalid date format or missing date
                    println("Invalid or missing date of birth")
                    Socket.sendMessageToClient(clientSocket, "error: invalid_date_of_birth")
                    return
                }

                // Set the parameters for the prepared statement
                preparedStatement.setString(1, client.firstName) // first name
                preparedStatement.setString(2, client.lastName) // last name
                preparedStatement.setString(3, client.email) // email
                preparedStatement.setString(4, client.phone) // phone
                preparedStatement.setString(5, formattedDateOfBirth) // formatted date of birth
                preparedStatement.setString(6, client.address) // address

                // Execute the query
                val rowsAffected = preparedStatement.executeUpdate()
                if (rowsAffected > 0) {
                    println("New client added successfully.")
                    val generatedKeys = preparedStatement.generatedKeys
                    if (generatedKeys.next()) {
                        val userId = generatedKeys.getInt(1) // Get the generated user_id
                        client.login?.let {
                            client.password?.let { it1 ->
                                registerlogin(
                                    clientSocket, userId, it, it1
                                )
                            }
                        }
                    }
                } else {
                    println("Failed to add the new client.")
                }
            } catch (e: SQLException) {
                println("Error inserting client: ${e.message}")
            }
        }
    }


    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun registerlogin(clientSocket: Int, userId: Int, username: String, password: String) {
        // Hash the password
        val hashedPassword = hashPassword(password)

        val insertLoginQuery = """
        INSERT INTO login_credentials (user_id, username, password_hash)
        VALUES (?, ?, ?)
    """

        val connection = DataBase.getConnection()
        if (connection != null) {
            try {
                val preparedStatement: PreparedStatement = connection.prepareStatement(insertLoginQuery)
                preparedStatement.setInt(1, userId) // user_id from the registered user
                preparedStatement.setString(2, username) // username from the client object
                preparedStatement.setString(3, hashedPassword) // hashed password

                val rowsAffected = preparedStatement.executeUpdate()
                if (rowsAffected > 0) {
                    Socket.sendMessageToClient(clientSocket, "ok")
                    println("Login credentials added successfully.")
                } else {
                    println("Failed to add login credentials.")
                    Socket.sendMessageToClient(clientSocket, "fail to save login credentials")
                }
            } catch (e: SQLException) {
                println("Error inserting login credentials: ${e.message}")
                Socket.sendMessageToClient(clientSocket, "fail to save login credentials")
            }
        }
    }

    fun createAccount(parameters: String, clientSocket: Int) {
        val connection = DataBase.getConnection()
        val account = AccountGsonConverter.fromJson(parameters)

        if (connection != null && account != null) {
            val accountInsertQuery = """
            INSERT INTO BankAccount (accountHolder, accountTypeId, balance, status, currencyId, userId)
            VALUES (?, ?, ?, ?, ?, ?)
        """

            try {
                // Insert new account with parameters
                val accountStatement = connection.prepareStatement(accountInsertQuery, Statement.RETURN_GENERATED_KEYS)

                // Account Holder Name: directly use the account's full name or related data
                accountStatement.setString(
                    1, account.accountHolder
                ) // Assuming accountHolder is a field in the Account model
                accountStatement.setInt(2, account.accountTypeId) // Account Type ID
                accountStatement.setDouble(
                    3, account.balance ?: 0.0
                ) // Initial balance (could be in the Account model)
                accountStatement.setString(4, "Active") // Account status
                accountStatement.setString(5, account.currencyId.toString()) // Currency ID
                accountStatement.setInt(6, account.userId) // User ID (provided in Account model)

                val rowsInserted = accountStatement.executeUpdate()

                if (rowsInserted > 0) {
                    // Get the auto-generated account ID
                    val generatedKeys = accountStatement.generatedKeys
                    if (generatedKeys.next()) {
                        val accountId = generatedKeys.getInt(1)

                        // Create a BankAccount object
                        val newAccount = BankAccount(
                            accountId = accountId,
                            accountHolder = account.accountHolder,
                            accountTypeId = account.accountTypeId,
                            balance = account.balance ?: 0.0,
                            dateCreated = "", // Date handled by the database
                            status = "Active",
                            currencyId = account.currencyId,
                            userId = account.userId
                        )

                        // Convert account data to JSON and send to client
                        val accountJson = AccountGsonConverter.toJson(newAccount)

                        if (accountJson != null) {
                            val message = "ok|$accountJson"
                            Socket.sendMessageToClient(clientSocket, message)
                        }
                        println("Account created successfully: $accountJson")
                    }
                } else {
                    Socket.sendMessageToClient(clientSocket, """{"error": "account_creation_failed"}""")
                    println("Error: Failed to create account for user ID ${account.userId}.")
                }

            } catch (e: SQLException) {
                println("Error creating account: ${e.message}")
                Socket.sendMessageToClient(clientSocket, """{"error": "database_error"}""")
            }
        } else {
            // Handle cases where connection or account is null
            val errorMessage = if (connection == null) {
                """{"error": "database_connection_error"}"""
            } else {
                """{"error": "invalid_account_data"}"""
            }
            Socket.sendMessageToClient(clientSocket, errorMessage)
            println("Error: ${if (connection == null) "Database connection is null" else "Invalid account data"}")
        }
    }


    fun login(parameters: String, clientSocket: Int) {
        val connection = DataBase.getConnection()
        val loginModel = LoginModelGsonConverter.fromJson(parameters)
        if (connection != null) {
            val query = """
            SELECT user_id, username, password_hash 
            FROM login_credentials 
            WHERE username = ?
        """

            try {
                val preparedStatement = connection.prepareStatement(query)
                preparedStatement.setString(1, loginModel!!.login)

                val resultSet = preparedStatement.executeQuery()

                if (resultSet.next()) {
                    val storedPasswordHash = resultSet.getString("password_hash")
                    val providedPasswordHash = hashPassword(loginModel.password)

                    if (storedPasswordHash == providedPasswordHash) {
                        // Password matches
                        val userID = resultSet.getString("user_id")
                        Socket.sendMessageToClient(clientSocket, "ok:$userID")

                        println("User ${loginModel.login} logged in successfully.")
                    } else {
                        // Password mismatch
                        Socket.sendMessageToClient(clientSocket, "login_failed: invalid_password")
                        println("Login failed: Invalid password for user ${loginModel.login}.")
                    }
                } else {
                    // No user found with the given username
                    Socket.sendMessageToClient(clientSocket, "login_failed: user_not_found")
                    println("Login failed: User ${loginModel.login} not found.")
                }
            } catch (e: SQLException) {
                println("Error during login: ${e.message}")
                Socket.sendMessageToClient(clientSocket, "login_failed: database_error")
            }
        } else {
            Socket.sendMessageToClient(clientSocket, "login_failed: database_connection_error")
            println("Error: Database connection is null.")
        }
    }


    fun returnUserDataAndAccountData(userId: String, clientSocket: Int) {
        val connection = DataBase.getConnection()

        if (connection != null) {
            val userQuery = """
            SELECT user_id, first_name, last_name, email, phone_number, date_of_birth, address
            FROM users
            WHERE user_id = ?
        """

            val accountQuery = """
            SELECT accountId, accountHolder, accountTypeId, balance, dateCreated, status, currencyId, userId
            FROM BankAccount
            WHERE userId = ?
        """

            try {
                // Fetch user data
                val userStatement = connection.prepareStatement(userQuery)
                userStatement.setString(1, userId)
                val userResultSet = userStatement.executeQuery()

                var user: User? = null
                if (userResultSet.next()) {
                    user = User(
                        id = userResultSet.getString("user_id"),
                        firstName = userResultSet.getString("first_name"),
                        lastName = userResultSet.getString("last_name"),
                        email = userResultSet.getString("email"),
                        phone = userResultSet.getString("phone_number"),
                        dateOfBirth = userResultSet.getString("date_of_birth"),
                        address = userResultSet.getString("address")
                    )
                } else {
                    // No user found
                    Socket.sendMessageToClient(clientSocket, """{"error": "user_not_found"}""")
                    println("Error: No user found with user ID $userId.")
                    return
                }

                // Fetch account data
                val accountStatement = connection.prepareStatement(accountQuery)
                accountStatement.setString(1, userId)
                val accountResultSet = accountStatement.executeQuery()

                val accounts = mutableListOf<BankAccount>()
                while (accountResultSet.next()) {
                    val account = BankAccount(
                        accountId = accountResultSet.getInt("accountId"),
                        accountHolder = accountResultSet.getString("accountHolder"),
                        accountTypeId = accountResultSet.getInt("accountTypeId"),
                        balance = accountResultSet.getDouble("balance"),
                        dateCreated = accountResultSet.getString("dateCreated"),
                        status = accountResultSet.getString("status"),
                        currencyId = accountResultSet.getString("currencyId").toInt(),
                        userId = accountResultSet.getInt("userId")
                    )
                    accounts.add(account)
                }

                val userJson = UserGsonConverter.toJson(user)
                val accountJson = AccountGsonConverter.toJsonList(accounts)
                // Prepare response data
                val message = "ok|$userJson|$accountJson"

                // Convert to JSON and send to client

                Socket.sendMessageToClient(clientSocket, message)
                println("Data sent successfully for user ID: $userId")

            } catch (e: SQLException) {
                println("Error fetching user or account data: ${e.message}")
                Socket.sendMessageToClient(clientSocket, """{"error": "database_error"}""")
            }
        } else {
            Socket.sendMessageToClient(clientSocket, """{"error": "database_connection_error"}""")
            println("Error: Database connection is null.")
        }
    }

    fun transfer(clientSocket: Int, message: String) {
        val transfer = AccountTransferGsonConverter.fromJson(message)
        if (transfer != null) {
            val result = TransactionRepository.recordTransaction(transfer)
            Socket.sendMessageToClient(clientSocket = clientSocket, message = result)
        }
    }


    fun deposit(clientSocket: Int, message: String) {
        val deposit = DepositGsonConverter.fromJson(message)
        if (deposit != null) {
            val result = DepositRepository.depositFunds(deposit)
            Socket.sendMessageToClient(clientSocket = clientSocket, message = result)
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

