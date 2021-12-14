package ru.smak

import ru.smak.net.Client
import java.lang.Exception
import java.net.InetAddress

val client = Client(InetAddress.getLocalHost())
val uReader = ConsoleUI()

fun main() {
    try {
        client.runCatching {
            addFailListener {
                uReader.run{
                    showMessage(it)
                    stop()
                }
            }
            addSuccessListener {
                uReader.showMessage(it)
            }
            asyncStart()
        }
        uReader.run {
            this += ::newData
            start()
        }
    } catch (ex: Exception){
        println(ex)
    }
}

private fun newData(message: String) {
    if (!message.equals("STOP")) {
        try {
            client.sendMessage(message)
        } catch (ex: Exception) {
            println(ex)
        }
    }
    else {
        client.stop()
        uReader.stop()
    }
}