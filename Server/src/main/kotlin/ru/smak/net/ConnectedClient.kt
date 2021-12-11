package ru.smak.net

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.charset.Charset

class ConnectedClient(
    val channel: AsynchronousSocketChannel,
    val clients: MutableList<ConnectedClient>,
) {

    inner class ReadingHandler : CompletionHandler<Int, Any?> {
        override fun completed(result: Int?, attachment: Any?) {
            println("Прием данных")
            result?.let {
                if (it > 0) {
                    readData(it)
                    setStatus(Status.READY)
                } else {
                    setStatus(Status.FINISHED)
                }
            }
        }
        override fun failed(exc: Throwable?, attachment: Any?) {
            println("Чтение данных не удалось :(")
            setStatus(Status.FINISHED)
        }
    }

    private fun setStatus(s: Status){
        CoroutineScope(Dispatchers.IO).launch {
            locker.send(s)
        }
    }

    private enum class Status{
        READY, FINISHED
    }

    private var buf: ByteBuffer = ByteBuffer.allocate(1024)
    private val charset = Charset.forName("utf-8")
    private val readHandler = ReadingHandler()
    private var stop = false
    private val locker = Channel<Status>(1)

    fun start(){
        println("Подключенный клиент стартует")
        readMessages()
    }

    fun stop(){
        stop = true
        clients.remove(this)
    }

    private fun readMessages() {
        println("Запуск чтения сообщений")
        CoroutineScope(Dispatchers.IO).launch {
            setStatus(Status.READY)
            while (true) {
                if (channel.isOpen) {
                    when (locker.receive()) {
                        Status.READY -> {
                            buf.clear()
                            channel.read(buf, null, readHandler)
                            println("Попытка прочитать данные")
                        }
                        Status.FINISHED -> {
                            locker.close()
                            println("Клиент отключился")
                            break
                        }
                    }
                }
            }
        }
    }

    private fun readData(size: Int) {
        println("Прочитано $size байт")
        buf.flip()
        println(charset.decode(buf))
    }
}
