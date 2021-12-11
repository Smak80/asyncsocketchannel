package ru.smak.net

import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.charset.Charset
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Server {
    var port = 1412
    private val ssc = AsynchronousServerSocketChannel.open()
    private val isa = InetSocketAddress(port)
    private val connHandler = ConnectionHandler()
    private val readHandler = ReadingHandler()
    private var clients: Int = 0
    private lateinit var buf: ByteBuffer
    private val charset = Charset.forName("utf-8")
    private val lock = ReentrantLock()
    private val channelCondition = lock.newCondition()

    inner class ConnectionHandler : CompletionHandler<AsynchronousSocketChannel, Int>{
        override fun completed(result: AsynchronousSocketChannel?, attachment: Int?) {
            result?.let {
                signal()
                startCommunication(it, attachment)
            }
        }
        override fun failed(exc: Throwable?, attachment: Int?) {
            println("Соединение не удалось :(")
            signal()
        }
    }

    inner class ReadingHandler : CompletionHandler<Int, Int?>{
        override fun completed(result: Int?, attachment: Int?) {
            result?.let {
                readData(it)
            }
        }
        override fun failed(exc: Throwable?, attachment: Int?) {
            println("Чтение данных не удалось :(")
        }
    }

    fun signal(){
        lock.withLock {
            channelCondition.signal()
        }
    }

    fun start() {
        runBlocking {
            if (ssc.isOpen) {
                withContext(Dispatchers.IO) {
                    ssc.bind(isa)
                }
                val job = CoroutineScope(Dispatchers.IO).launch {
                    while (true) {
                        println("Ожидаем подключения")
                        lock.withLock {
                            ssc.accept(++clients, connHandler)
                            channelCondition.await()
                        }
                    }
                }
                job.join()
            }
        }
    }

    private fun startCommunication(channel: AsynchronousSocketChannel, attachment: Int?) {
        if (channel.isOpen){
            buf = ByteBuffer.allocate(1024)
            buf.clear()
            channel.read(buf, attachment, readHandler)
        }
    }

    private fun readData(size: Int) {
        println("Прочитано $size байт")
        buf.flip()
        println(charset.decode(buf))
    }
}
