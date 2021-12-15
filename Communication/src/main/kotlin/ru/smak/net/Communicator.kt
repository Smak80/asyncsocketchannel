package ru.smak.net

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import ru.smak.net.exceptions.*
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.charset.Charset

class Communicator(
    val ioChannel: AsynchronousSocketChannel,
) {
    /**
     * Случатель получения данных.
     * Также передает в обработчик полученную строку, если получение данных было успешным
     */
    private val dataReceivedListener = mutableListOf<(String?, IOException?)->Unit>()

    /**
     * Случатель получения данных.
     * Также передает в обработчик количество отправленных данных
     */
    private val dataSentListener = mutableListOf<(Int, IOException?)->Unit>()

    fun addReceiveListener(l: (String?, IOException?)->Unit){
        dataReceivedListener.add(l)
    }

    fun removeReceiveListener(l: (String?, IOException?)->Unit){
        dataReceivedListener.remove(l)
    }

    fun addSendListener(l: (Int, IOException?)->Unit){
        dataSentListener.add(l)
    }

    fun removeSendListener(l: (Int, IOException?)->Unit){
        dataSentListener.remove(l)
    }

    private val ioHandler = IOHandler()
    private var readJob: Job? = null

    private val charset = Charset.forName("utf-8")
    private var buffer: ByteBuffer = ByteBuffer.allocate(defaultBufferSize)
    private var gotSize = false
    private var putSize = false

    private val readLocker = Channel<Int>(1)
    private val writeLocker = Channel<Int>(1)

    private var bufSize: Int = defaultBufferSize
        get() {
            if (gotSize)
                return field
            else
                return defaultBufferSize
        }

    fun asyncStart(){
        readJob = CoroutineScope(Dispatchers.IO).launch {
            readMessages()
        }
    }

    fun stop(){
        readJob?.cancel()
        readLocker.close()
    }

    fun sendMessage(msg: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val buf = charset.encode(msg)
            val sz = buf.limit()
            val bufSz = charset.encode(sz.toString())
            try {
                ioChannel.write(bufSz, StreamType.WRITE, ioHandler)
                writeLocker.receive()
                try{
                    ioChannel.write(buf, StreamType.WRITE, ioHandler)
                    writeLocker.receive()
                } catch (e: Throwable){
                    dataSentListener.forEach { it(0, WriteFailIOException("Невозможно записать данные в канал связи", e)) }
                }
            } catch (e: Throwable){
                dataSentListener.forEach { it(0, WriteFailIOException("Невозможно записать размер данных в канал связи", e)) }
            }
        }
    }

    private suspend fun readMessages(){
        while (ioChannel.isOpen) {
            buffer = ByteBuffer.allocate(bufSize)
            try {
                ioChannel.read(buffer, StreamType.READ, ioHandler)
            } catch (e: Throwable){
                dataReceivedListener.forEach { it(null, ReadFailIOException("Невозможно начать чтение из канала", e)) }
            }
            readLocker.receive()
        }
    }

    inner class IOHandler : CompletionHandler<Int, StreamType> {
        override fun completed(result: Int?, attachment: StreamType) {
            result?.let { msgLen ->
                when (attachment){
                    StreamType.WRITE -> {
                        putSize = !putSize
                        if (!putSize)
                            dataSentListener.forEach { it(msgLen, null) }
                        CoroutineScope(Dispatchers.IO).launch {
                            writeLocker.send(msgLen)
                        }
                    }
                    StreamType.READ -> {
                        CoroutineScope(Dispatchers.IO).launch {
                            if (msgLen > 0){
                                buffer.flip()
                                val data: String
                                if (!gotSize){
                                    try {
                                        val v = charset.decode(buffer).toString()
                                        bufSize = v.toInt()
                                        gotSize = true
                                    } catch (e: Throwable){
                                        dataReceivedListener.forEach {
                                            it(
                                                null,
                                                IncorrectDataReceivedIOException("Получены неверные данные: ожидалось количество байт для приёма", e)
                                            )
                                        }
                                    }
                                } else {
                                    data = charset.decode(buffer).toString()
                                    dataReceivedListener.forEach { it(data, null) }
                                    gotSize = false
                                }
                                readLocker.send(msgLen)
                            }
                        }
                    }
                }
            }
        }

        override fun failed(exc: Throwable?, attachment: StreamType) {
            when (attachment) {
                StreamType.READ -> {
                    dataReceivedListener.forEach { it(null, ReceiveFailIOException("Данные не получены", exc)) }
                }
                StreamType.WRITE -> {
                    dataSentListener.forEach { it(0, SendFailIOException("Данные не отправлены", exc)) }
                }
            }
        }
    }

    companion object {

        enum class StreamType{
            READ, WRITE
        }

        const val defaultBufferSize = 12
    }
}