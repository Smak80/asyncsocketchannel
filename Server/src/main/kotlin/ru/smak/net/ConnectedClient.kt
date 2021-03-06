package ru.smak.net

import java.nio.channels.AsynchronousSocketChannel

class ConnectedClient(
    private val ioChannel: AsynchronousSocketChannel,
    private val clients: MutableList<ConnectedClient>,
) {

    private val communicator: Communicator by lazy {
        Communicator(ioChannel).also{ comm ->
            comm.addReceiveListener { s, e ->
                if (e==null)
                    s?.let{ sendToAll(it) }
                else
                    stop()
            }
            comm.addSendListener { _, e ->
                if (e != null){
                    stop()
                }
            }
        }
    }

    init {
        clients.add(this)
    }

    fun start(){
        communicator.asyncStart()
    }

    fun stop(){
        communicator.stop()
        clients.remove(this)
    }

    private fun sendMessage(msg: String) = communicator.sendMessage(msg)

    private fun sendToAll(message: String) {
        clients.filterNot { it == this }.forEach { client ->
            client.sendMessage(message)
        }
    }
}
