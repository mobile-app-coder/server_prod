package server

import server.communication.Socket
import server.database.DataBase


fun main() {
    val socket = Socket

    socket.startSocketServer()
    DataBase.getConnection()
    socket.listen()

    // Prevent main thread from exiting
    println("Press Ctrl+C to stop the server.")
    Thread.currentThread().join() // Keeps the main thread alive
}