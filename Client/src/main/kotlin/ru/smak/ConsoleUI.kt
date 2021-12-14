package ru.smak

import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CancellationException

class ConsoleUI {

    private val inputListener: MutableList<(String)->Unit> = mutableListOf()
    private var mainJob: Job? = null

    val dataFlow: Flow<String>
        get() = flow {
            var str = ""
            while (!str.equals("STOP")) {
                showMessage("Ожидание сообщения для передачи на сервер...")
                str = readLine() ?: "STOP"
                emit(str)
            }
        }

    fun start() {
        runBlocking {
            mainJob = launch {
                dataFlow.collect { value ->
                    inputListener.forEach { it(value) }
                }
            }
            try {
                mainJob!!.join()
            } catch (_: Throwable){ }
        }
    }

    operator fun plusAssign(listener: (String)->Unit){
        inputListener.add(listener)
    }

    operator fun minusAssign(listener: (String)->Unit){
        inputListener.remove(listener)
    }

    fun stop() {
        mainJob?.cancel()
        showMessage("Работа клиента завершена")
        System.exit(0)
    }

    fun showMessage(msg: String){
        println(msg)
    }
}