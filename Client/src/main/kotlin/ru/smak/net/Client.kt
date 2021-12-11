package ru.smak.net

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.charset.Charset

class Client(
    var address: InetAddress,
    var port: Int = 1412
) {

    companion object {
        enum class Status{
            NOT_CONNECTED,
            CONNECTED,
        }
    }

    inner class ConnectionHandler : CompletionHandler<Void, Any?> {
        override fun completed(result: Void?, attachment: Any?) {
            println("Успешное подключение к серверу")
            status = Status.CONNECTED
        }

        override fun failed(exc: Throwable?, attachment: Any?) {
            println("Не удалось подключиться к серверу")
        }
    }

    inner class WriteHandler : CompletionHandler<Int, Any?> {
        override fun completed(result: Int?, attachment: Any?) {
            result?.let {
                println("Успешная передача информации на сервер")
            }
        }

        override fun failed(exc: Throwable?, attachment: Any?) {
            println("Не удалось передать данные на сервер")
        }
    }

    private val ch = AsynchronousSocketChannel.open()
    private val sa: InetSocketAddress
        get() = InetSocketAddress(address, port)
    private val connHandler = ConnectionHandler()
    private val writeHandler = WriteHandler()
    private val charset = Charset.forName("utf-8")
//    private val lock = ReentrantLock()
//    private val channelCondition = lock.newCondition()
    var status: Status = Status.NOT_CONNECTED

    fun sendMessage(s: String) {
        if (ch.isOpen){
            val buf = charset.encode(s)
            ch.write(buf, null, writeHandler)
        } else throw IOException("Сообщение нельзя отправить без подключения")
    }

    fun start() {
        CoroutineScope(Dispatchers.IO).launch {
            ch.connect(sa, null, connHandler)
        }
    }

    fun stop (){
        status = Status.NOT_CONNECTED
        ch.close()
    }

}
