package ru.smak.net

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.security.InvalidParameterException

class Server(
    port: Int = 1412
) {
    var port = port
        set(value) {
            if (value !in 1025..49151) throw InvalidParameterException()
            field = value
        }

    private val successMessageListener: MutableList<(String)->Unit> = mutableListOf()
    private val failMessageListener: MutableList<(String)->Unit> = mutableListOf()

    private val ssc = AsynchronousServerSocketChannel.open()
    private val isa = InetSocketAddress(this.port)
    private val connHandler = ConnectionHandler()
    private val locker = Channel<Int>(1)
    private val clientsList = mutableListOf<ConnectedClient>()

    inner class ConnectionHandler : CompletionHandler<AsynchronousSocketChannel, Any?>{
        override fun completed(result: AsynchronousSocketChannel?, attachment: Any?) {
            result?.run {
                CoroutineScope(Dispatchers.Default).launch {
                    locker.send(1)
                }
                ConnectedClient(this, clientsList).start()
                val x = clientsList.size
                println("Подключено $x клиентов")
            }
        }
        override fun failed(exc: Throwable?, attachment: Any?) {
            CoroutineScope(Dispatchers.Default).launch {
                locker.send(1)
            }
        }
    }

    fun asyncStart() {
        CoroutineScope(Dispatchers.Default).launch {
            if (ssc.isOpen) {
                withContext(Dispatchers.IO) {
                    try {
                        ssc.bind(isa)
                    } catch (_: Throwable){
                        println("Невозможно занять указанный порт.")
                    }
                }
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        while (ssc.isOpen) {
                            println("Ожидание подключения")
                            ssc.accept(null, connHandler)
                            locker.receive()
                        }
                    }catch (_: Throwable){}
                }
            }
        }
    }

    fun stop(){
        clientsList.toList().forEach { it.stop() }
        ssc.close()
        locker.close()
    }

    fun addSuccessListener(listener: (String)->Unit){
        successMessageListener.add(listener)
    }

    fun removeSuccessListener(listener: (String)->Unit){
        successMessageListener.remove(listener)
    }

    fun addFailListener(listener: (String)->Unit){
        failMessageListener.add(listener)
    }

    fun removeFailListener(listener: (String)->Unit){
        failMessageListener.remove(listener)
    }
}
