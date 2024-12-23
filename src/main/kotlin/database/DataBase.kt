package server.database

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Statement

object DataBase {
    private val url = "jdbc:mysql://localhost:3306/bank"
    private val user = "shahriyor"  // your database username
    private val password = "coder"

    // Initialize the database connection pool or single connection
    private var connection: Connection? = null

    // Function to establish the connection
    fun getConnection(): Connection? {
        if (connection == null || connection!!.isClosed) {
            try {
                connection = DriverManager.getConnection(url, user, password)
                println("Connection established successfully.")
            } catch (e: SQLException) {
                println("Error connecting to database: ${e.message}")
            }
        }
        return connection
    }

    // Closing the connection safely
    fun closeConnection() {
        try {
            connection?.close()
            println("Connection closed.")
        } catch (e: SQLException) {
            println("Error closing the connection: ${e.message}")
        }
    }

    // Example of running a database query
    fun runQuery(query: String, isSelectQuery: Boolean = false): List<Map<String, String>> {
        val results = mutableListOf<Map<String, String>>()
        val conn = getConnection()

        if (conn != null) {
            try {
                val stmt: Statement = conn.createStatement()

                if (isSelectQuery) {
                    // If it's a SELECT query, we use executeQuery
                    val resultSet = stmt.executeQuery(query)
                    val metaData = resultSet.metaData
                    val columnCount = metaData.columnCount

                    while (resultSet.next()) {
                        val row = mutableMapOf<String, String>()
                        for (i in 1..columnCount) {
                            row[metaData.getColumnName(i)] = resultSet.getString(i)
                        }
                        results.add(row)
                    }
                } else {
                    // For INSERT, UPDATE, DELETE, we use executeUpdate
                    val rowsAffected = stmt.executeUpdate(query)
                    println("Query executed successfully, $rowsAffected rows affected.")
                }
            } catch (e: SQLException) {
                println("Error executing query: ${e.message}")
            }
        }
        return results
    }



}
