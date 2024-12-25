package server.repository

import server.database.DataBase
import java.sql.Connection
import java.sql.SQLException
import java.sql.Timestamp

object DepositRepository {


    fun depositFunds(accountId: Int, depositAmount: Double, depositType: String): String {
        // Database connection parameters

        var connection: Connection? = null
        var successMessage = "Deposit successful"

        try {
            // Step 1: Establish a connection to the database
            connection = DataBase.getConnection()
            if (connection != null) {
                connection.autoCommit = false  // Disable auto-commit for transaction management

                // Step 2: Check if the account exists
                val checkAccountQuery = "SELECT balance FROM BankAccount WHERE account_id = ?"
                val checkAccountStmt = connection.prepareStatement(checkAccountQuery)
                checkAccountStmt.setInt(1, accountId)
                val accountResult = checkAccountStmt.executeQuery()

                if (!accountResult.next()) {
                    throw SQLException("Account not found")
                }

                // Step 3: Insert the deposit into the `deposits` table
                val insertDepositQuery = """
            INSERT INTO deposits (account_id, deposit_amount, deposit_type, status, deposit_date)
            VALUES (?, ?, ?, ?, ?)
        """
                val insertDepositStmt = connection.prepareStatement(insertDepositQuery)
                insertDepositStmt.setInt(1, accountId)
                insertDepositStmt.setDouble(2, depositAmount)
                insertDepositStmt.setString(3, depositType)
                insertDepositStmt.setString(4, "completed")
                insertDepositStmt.setTimestamp(5, Timestamp(System.currentTimeMillis()))
                insertDepositStmt.executeUpdate()

                // Step 4: Insert deposit transaction into `deposit_transactions` table
                val depositIdQuery = "SELECT LAST_INSERT_ID()"
                val depositIdStmt = connection.prepareStatement(depositIdQuery)
                val depositIdResult = depositIdStmt.executeQuery()
                depositIdResult.next()
                val depositId = depositIdResult.getInt(1)

                val insertTransactionQuery = """
            INSERT INTO deposit_transactions (deposit_id, transaction_type, amount, transaction_date)
            VALUES (?, ?, ?, ?)
        """
                val insertTransactionStmt = connection.prepareStatement(insertTransactionQuery)
                insertTransactionStmt.setInt(1, depositId)
                insertTransactionStmt.setString(2, "deposit")
                insertTransactionStmt.setDouble(3, depositAmount)
                insertTransactionStmt.setTimestamp(4, Timestamp(System.currentTimeMillis()))
                insertTransactionStmt.executeUpdate()

                // Step 5: Update the account balance (add deposit amount)
                val updateAccountQuery = "UPDATE accounts SET balance = balance + ? WHERE account_id = ?"
                val updateAccountStmt = connection.prepareStatement(updateAccountQuery)
                updateAccountStmt.setDouble(1, depositAmount)
                updateAccountStmt.setInt(2, accountId)
                updateAccountStmt.executeUpdate()

                // Step 6: Commit the transaction
                connection.commit()
            }

        } catch (e: SQLException) {
            connection?.rollback()
            successMessage = "Deposit failed: ${e.message}"
            e.printStackTrace()
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }

        return successMessage
    }

}