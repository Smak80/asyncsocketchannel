package ru.smak.net

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.ByteBuffer
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
            if (value !in 1025..49151)
                throw InvalidParameterException()
            field = value
        }

    private val successMessageListener: MutableList<(String)->Unit> = mutableListOf()
    private val failMessageListener: MutableList<(String)->Unit> = mutableListOf()

    companion object {

        enum class Status{
            NOT_CONNECTED,
            CONNECTED,
        }

        enum class StreamType{
            READ, WRITE
        }
        const val defaultBufferSize = 12
    }

    inner class ConnectionHandler : CompletionHandler<Void, Any?> {
        override fun completed(result: Void?, attachment: Any?) {
            status = Status.CONNECTED
            successMessageListener.forEach { it("Подключение к серверу прошло успешно")}
            readMessages()
        }

        override fun failed(exc: Throwable?, attachment: Any?) {
            failMessageListener.forEach { it("Не удалось подключиться к серверу")}
        }
    }

    inner class IOHandler : CompletionHandler<Int, StreamType> {
        override fun completed(result: Int?, attachment: StreamType) {
            result?.let { msgLen ->
                if (attachment == StreamType.WRITE){
                    successMessageListener.forEach { it("Успешная передача $msgLen байт информации на сервер") }
                }
                else {
                    CoroutineScope(Dispatchers.IO).launch {
                        if (msgLen > 0){
                            buffer.flip()
                            if (!gotSize){
                                try {
                                    val v = charset.decode(buffer).toString()
                                    bufSize = v.toInt()
                                    gotSize = true
                                } catch (_: Throwable){
                                    // Неверные данные в потоке
                                }
                                lockerChannel.send("")
                            } else {
                                lockerChannel.send(charset.decode(buffer).toString())
                                gotSize = false
                            }
                        }
                    }
                }
            }
        }

        override fun failed(exc: Throwable?, attachment: StreamType) {
            if (attachment == StreamType.WRITE)
                failMessageListener.forEach { it("Не удалось передать данные на сервер")}
        }
    }

    private val ch = AsynchronousSocketChannel.open()
    private val sa: InetSocketAddress
        get() = InetSocketAddress(address, port)
    private val connHandler = ConnectionHandler()
    private val ioHandler = IOHandler()
    private val charset = Charset.forName("utf-8")
    var status: Status = Status.NOT_CONNECTED
    private val lockerChannel = Channel<String>(1)
    private var buffer: ByteBuffer = ByteBuffer.allocate(defaultBufferSize)
    private var gotSize = false
    private var bufSize: Int = defaultBufferSize
        get() {
            if (gotSize)
                return field
            else
                return defaultBufferSize
        }

    fun sendMessage(s: String) = CoroutineScope(Dispatchers.IO).launch {
        val buf = charset.encode(s)
        if (ch.isOpen)
            ch.write(buf, StreamType.WRITE, ioHandler)
        else
            failMessageListener.forEach{ it("Соединение закрыто") }
    }

    fun readMessages() = CoroutineScope(Dispatchers.IO).launch {
        while (ch.isOpen) {
            //buffer?.clear()
            buffer = ByteBuffer.allocate(bufSize)
            try {
                ch.read(buffer, StreamType.READ, ioHandler)
            } catch (_: Throwable){ }
            lockerChannel.receive().also { msg ->
                if (msg.isNotEmpty()) {
                    successMessageListener.forEach { it(msg) }
                }
            }
        }
    }

    fun asyncStart() {
        ch.connect(sa, null, connHandler)
    }

    fun stop (){
        status = Status.NOT_CONNECTED
        ch.close()
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
