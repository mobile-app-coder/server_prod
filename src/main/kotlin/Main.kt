package server

import server.socket.Socket


fun main() {
    val socket = Socket

    socket.startSocketServer()
    Thread {
        Socket.listen()
    }.start()
}