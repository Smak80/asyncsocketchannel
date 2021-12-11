package ru.smak

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.smak.net.Client
import java.lang.Exception
import java.net.InetAddress

val client = Client(InetAddress.getLocalHost())

fun main() {
    val uReader = ConsoleUIReader()
    val flow = uReader.dataFlow
    client.start()
    runBlocking {
        val job = CoroutineScope(Dispatchers.IO).launch {
            flow.collect { value ->
                if (!value.equals("STOP"))
                    try {
                        client.sendMessage(value)
                    } catch (ex: Exception){
                        println("Ошибка отправки сообщения :(")
                        println(ex)
                    }
                else
                    client.stop()
                    return@collect
            }
        }
        job.join()
    }
}