package ru.smak.net

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.ConnectException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.charset.Charset
import java.security.InvalidParameterException

class Client(
    var address: InetAddress,
    port: Int = 1412
) {

    var port = port
        set(value) {
            if (value !in 1025..49151) throw InvalidParameterException()
            field = value
        }

    companion object {
        enum class Status{
            NOT_CONNECTED,
            CONNECTED,
        }
    }

    inner class ConnectionHandler : CompletionHandler<Void, Any?> {
        override fun completed(result: Void?, attachment: Any?) {
            status = Status.CONNECTED
        }

        override fun failed(exc: Throwable?, attachment: Any?) {
            throw ConnectException("Не удалось подключиться к серверу")
        }
    }

    inner class WriteHandler : CompletionHandler<Int, Any?> {
        override fun completed(result: Int?, attachment: Any?) {
            result?.let {
                println("Успешная передача $it байт информации на сервер")
            }
        }

        override fun failed(exc: Throwable?, attachment: Any?) {
            throw IOException("Не удалось передать данные на сервер")
        }
    }

    private val ch = AsynchronousSocketChannel.open()
    private val sa: InetSocketAddress
        get() = InetSocketAddress(address, port)
    private val connHandler = ConnectionHandler()
    private val writeHandler = WriteHandler()
    private val charset = Charset.forName("utf-8")
    var status: Status = Status.NOT_CONNECTED

    fun sendMessage(s: String) {
        CoroutineScope(Dispatchers.IO).launch {
            if (ch.isOpen) {
                val buf = charset.encode(s)
                ch.write(buf, null, writeHandler)
            } else throw IOException("Сообщение нельзя отправить без подключения")
        }
    }

    fun asyncStart() {
        CoroutineScope(Dispatchers.IO).launch {
            ch.connect(sa, null, connHandler)
        }
    }

    fun stop (){
        status = Status.NOT_CONNECTED
        ch.close()
    }

}
