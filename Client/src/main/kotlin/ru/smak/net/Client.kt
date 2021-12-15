package ru.smak.net

import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
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

    private val ioChannel = AsynchronousSocketChannel.open()
    private val sa: InetSocketAddress
        get() = InetSocketAddress(address, port)
    private val connectionHandler = ConnectionHandler()

    private val communicator: Communicator by lazy {
        Communicator(ioChannel).also{
            it.addReceiveListener { s, e ->
                if (e!=null)
                    failMessageListener.forEach{ it("Ошибка чтения данных: ${e.message} ${e.cause?.message?.let{ "+ $it"}}") }
                else
                    successMessageListener.forEach { it(s ?: "") }
            }
            it.addSendListener{ sz, e ->
                if (e!=null)
                    failMessageListener.forEach{ it("Ошибка при отправке данных: ${e.message} ${e.cause?.message?.let{ "+ $it"}}") }
                else
                    successMessageListener.forEach { it("Успешно передано $sz байт") }
            }
        }
    }

    inner class ConnectionHandler : CompletionHandler<Void, Any?> {
        override fun completed(result: Void?, attachment: Any?) {
            successMessageListener.forEach { it("Подключение к серверу прошло успешно")}
            communicator.asyncStart()
        }

        override fun failed(exc: Throwable?, attachment: Any?) {
            failMessageListener.forEach { it("Не удалось подключиться к серверу")}
            communicator.stop()
        }
    }

    fun asyncStart() {
        ioChannel.connect(sa, null, connectionHandler)
    }

    fun stop (){
        ioChannel.close()
    }

    fun sendMessage(message: String) = communicator.sendMessage(message)

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
