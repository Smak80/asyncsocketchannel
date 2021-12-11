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
    try {
        client.asyncStart()
    } catch (ex: Exception){
        println(ex)
    }
    runBlocking {
        flow.collect { value ->
            if (!value.equals("STOP"))
                try {
                    client.sendMessage(value)
                } catch (ex: Exception){
                    println(ex)
                }
            else
                client.stop()
                return@collect
        }
    }
}