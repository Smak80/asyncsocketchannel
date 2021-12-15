package ru.smak

import ru.smak.net.Client
import ru.smak.ui.console.ConsoleUI
import java.lang.Exception
import java.net.InetAddress

val client = Client(InetAddress.getLocalHost())
val ui = ConsoleUI()

fun main() {
    try {
        client.run {
            addFailListener {
                ui.run{
                    showMessage(it)
                    stop()
                }
            }
            addSuccessListener {
                ui.showMessage(it)
            }
            asyncStart()
        }
        ui.run {
            this += ::newData
            start()
        }
    } catch (ex: Exception){
        ui.showMessage(ex.message.toString())
    }
}

private fun newData(message: String) {
    if (message != "STOP") {
        try {
            client.sendMessage(message)
        } catch (ex: Exception) {
            println(ex)
        }
    }
    else {
        client.stop()
        ui.stop()
    }
}