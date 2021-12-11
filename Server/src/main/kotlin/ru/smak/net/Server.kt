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
    private enum class Status {
        DISCONNECTED, READY
    }
    var port = port
        set(value) {
            if (value !in 1025..49151) throw InvalidParameterException()
            field = value
        }

    private val ssc = AsynchronousServerSocketChannel.open()
    private val isa = InetSocketAddress(this.port)
    private val connHandler = ConnectionHandler()
    private val locker = Channel<Status>(1)
    private val clientsList = mutableListOf<ConnectedClient>()

    inner class ConnectionHandler : CompletionHandler<AsynchronousSocketChannel, Any?>{
        override fun completed(result: AsynchronousSocketChannel?, attachment: Any?) {
            result?.run {
                CoroutineScope(Dispatchers.Default).launch {
                    locker.send(Status.READY)
                }
                ConnectedClient(this, clientsList).start()
            }
        }
        override fun failed(exc: Throwable?, attachment: Any?) {
            CoroutineScope(Dispatchers.Default).launch {
                locker.send(Status.READY)
            }
        }
    }

    fun asyncStart() {
        CoroutineScope(Dispatchers.Default).launch {
            if (ssc.isOpen) {
                withContext(Dispatchers.IO) {
                    ssc.bind(isa)
                    locker.send(Status.READY)
                }
                CoroutineScope(Dispatchers.IO).launch {
                    while(true) {
                        when(locker.receive()) {
                            Status.READY -> {
                                println("Ожидание подключения")
                                ssc.accept(null, connHandler)
                            }
                            Status.DISCONNECTED -> {
                                locker.close()
                                break
                            }
                        }
                    }
                }
            }
        }
    }

    fun stop(){
        clientsList.forEach { it.stop() }
        CoroutineScope(Dispatchers.Default).launch{
            locker.receive()
            locker.send(Status.DISCONNECTED)
        }
    }
}
