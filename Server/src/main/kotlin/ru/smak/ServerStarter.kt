package ru.smak

import ru.smak.net.Server
import java.lang.Exception

val server = Server(1412)

fun main() {
    try {
        server.asyncStart()
    } catch (ex: Exception){
        println(ex)
    }
    askUserForStop()
}

fun askUserForStop(){
    println("Type \"STOP\" for stopping server...")
    var input = ""
    while (input.uppercase() != "STOP"){
        input = readLine()?.uppercase() ?: "STOP"
    }
    server.stop()
}

