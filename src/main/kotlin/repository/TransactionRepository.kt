package server.repository

import server.database.DataBase
import server.model.AccountTransfer
import java.sql.PreparedStatement
import java.sql.SQLException

object TransactionRepository {

    // Record a transaction (deposit/withdrawal)
    fun recordTransaction(transfer: AccountTransfer): String {
        val connection = DataBase.getConnection()!!

        var successMessage = "Transfer successful"

        try {
            // Step 2: Check if the sender has enough balance
            val checkBalanceQuery = "SELECT balance FROM BankAccount WHERE accountId = ?"
            val checkBalanceStmt = connection.prepareStatement(checkBalanceQuery)
            checkBalanceStmt.setInt(1, transfer.senderAccountId)
            val senderBalanceResult = checkBalanceStmt.executeQuery()

            if (senderBalanceResult.next()) {
                val senderBalance = senderBalanceResult.getDouble("balance")
                if (senderBalance < transfer.amount) {
                    // If the sender does not have enough balance, throw an exception
                    throw SQLException("Insufficient balance in sender's account")
                }
            } else {
                throw SQLException("Sender account not found")
            }

            //insert in transaction table
            val query =
                "INSERT INTO account_transfers (sender_account_id, receiver_account_id, amount, description) VALUES (?, ?, ?, ?)"
            val preparedStatement: PreparedStatement = connection.prepareStatement(query)
            preparedStatement.setInt(1, transfer.senderAccountId)
            preparedStatement.setInt(2, transfer.receiverAccountId)
            preparedStatement.setDouble(3, transfer.amount)
            preparedStatement.setString(4, transfer.description)
            val result = preparedStatement.executeUpdate()


            // Step 4: Update the sender's account balance (deduct money)
            val updateSenderBalanceQuery = "UPDATE BankAccount SET balance = balance - ? WHERE accountId = ?"
            val updateSenderStmt = connection.prepareStatement(updateSenderBalanceQuery)
            updateSenderStmt.setDouble(1, transfer.amount)
            updateSenderStmt.setInt(2, transfer.senderAccountId)
            updateSenderStmt.executeUpdate()


            val updateReceiverBalanceQuery = "UPDATE BankAccount SET balance = balance + ? WHERE accountId = ?"
            val updateReceiverStmt = connection.prepareStatement(updateReceiverBalanceQuery)
            updateReceiverStmt.setDouble(1, transfer.amount)
            updateReceiverStmt.setInt(2, transfer.receiverAccountId)
            updateReceiverStmt.executeUpdate()

            // Step 6: Commit the transaction if all queries are successful

        } catch (e: SQLException) {
            //connection.rollback()
            successMessage = "Transfer failed: ${e.message}"
            e.printStackTrace()
        } finally {
            try {
                // Ensure the connection is closed
                connection.close()
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }
        return "ok|$successMessage"
    }

    fun getTransactionsByAccountId(accountId: Int): List<AccountTransfer> {

        println("transaction history called")
        val connection = DataBase.getConnection()!!
        val transactions = mutableListOf<AccountTransfer>()

        try {
            val query = """
            SELECT transfer_id,sender_account_id, receiver_account_id, amount, description, timestamp
            FROM account_transfers
            WHERE sender_account_id = ? OR receiver_account_id = ?
            ORDER BY timestamp ASC
        """

            val preparedStatement: PreparedStatement = connection.prepareStatement(query)
            preparedStatement.setInt(1, accountId)
            preparedStatement.setInt(2, accountId)

            val resultSet = preparedStatement.executeQuery()

            while (resultSet.next()) {
                val transaction = AccountTransfer(
                    transferId = resultSet.getInt("transfer_id"),
                    senderAccountId = resultSet.getInt("sender_account_id"),
                    receiverAccountId = resultSet.getInt("receiver_account_id"),
                    amount = resultSet.getDouble("amount"),
                    description = resultSet.getString("description"),
                    timestamp = resultSet.getTimestamp("timestamp")
                )
                transactions.add(transaction)
            }

        } catch (e: SQLException) {
            e.printStackTrace()
        } finally {
            try {
                connection.close()
            } catch (e: SQLException) {
                e.printStackTrace()
            }
        }

        return transactions
    }

}

