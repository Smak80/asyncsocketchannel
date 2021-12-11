package ru.smak

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ConsoleUIReader {

    val dataFlow: Flow<String>
        get() = flow {
            var str = ""
            while (!str.equals("STOP")) {
                println("Введите сообщение для передачи на сервер:")
                str = readLine() ?: "STOP"
                emit(str)
            }
        }
}