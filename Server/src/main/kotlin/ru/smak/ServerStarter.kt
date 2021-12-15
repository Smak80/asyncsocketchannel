package ru.smak

import ru.smak.net.Server
import ru.smak.ui.console.ConsoleUI
import java.lang.Exception

val server = Server(1412)
val ui = ConsoleUI()

fun main() {
    try {
        server.run {
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
            showMessage("Type \"STOP\" to stop server...")
        }
    } catch (ex: Exception){
        ui.showMessage(ex.message.toString())
    }

}

private fun newData(message: String) {
    if (message.uppercase() == "STOP") {
        try {
            server.stop()
            ui.stop()
        } catch (ex: Exception) {
            ui.showMessage(ex.message.toString())
        }
    }
}

