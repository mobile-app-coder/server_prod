package server

import server.database.DataBase
import server.socket.Socket


fun main() {
    val socket = Socket

    socket.startSocketServer()
    DataBase.getConnection()
    Thread {
        Socket.listen()
    }.start()
}