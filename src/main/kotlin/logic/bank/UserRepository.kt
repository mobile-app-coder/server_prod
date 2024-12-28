package server.logic.bank

import server.commands.GeneralCommand
import server.commands.UserServices


object UserRepository {
    fun processRequest(message: String, clientSocket: Int): String {
        val request = UserServices.parseMessage(message)
        if (request != null) {
            when (request.action) {
                "client" -> {
                    when (request.command) {
                        "register" -> UserServices.register(request.parameters, clientSocket)
                        "login" -> UserServices.login(request.parameters, clientSocket)
                        "userdata" -> UserServices.returnUserDataAndAccountData(request.parameters, clientSocket)
                        "accounttype" -> GeneralCommand.getAccountType(clientSocket)
                        "createaccount" -> UserServices.createAccount(request.parameters, clientSocket)
                        "currency" -> GeneralCommand.getCurrencies(clientSocket)

                        "deposit" -> UserServices.deposit(
                            clientSocket = clientSocket, message = request.parameters
                        )

                        "transaction" -> UserServices.transfer(
                            clientSocket = clientSocket, message = request.parameters
                        )

                        "recenttransaction" -> UserServices.returnTransactionHistory(
                            clientSocket = clientSocket, message = request.parameters
                        )

                        "loan" -> {
                            UserServices.deposit(clientSocket = clientSocket, message = message)
                        }

                        "withdraw" -> {}
                    }
                }
            }
        }
        return "Processed: $message"
    }
}