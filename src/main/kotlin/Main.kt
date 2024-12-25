package server

import server.database.DataBase
import server.communication.Socket


fun main() {
    val socket = Socket

    socket.startSocketServer()
    DataBase.getConnection()
    Thread {
        Socket.listen()
    }.start()
}