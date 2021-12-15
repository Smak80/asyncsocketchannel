package ru.smak.net.exceptions

import java.io.IOException

class ReadFailIOException(message: String, cause:Throwable?): IOException(message, cause){
    constructor(message: String) : this(message, null)
}
class WriteFailIOException(message: String, cause:Throwable?): IOException(message, cause){
    constructor(message: String) : this(message, null)
}

class ReceiveFailIOException(message: String, cause:Throwable?): IOException(message, cause){
    constructor(message: String) : this(message, null)
}
class SendFailIOException(message: String, cause:Throwable?): IOException(message, cause){
    constructor(message: String) : this(message, null)
}

class IncorrectDataReceivedIOException(message: String, cause:Throwable?): IOException(message, cause){
    constructor(message: String) : this(message, null)
}
