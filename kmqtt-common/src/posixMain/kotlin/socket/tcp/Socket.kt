package socket.tcp

import close
import getEagain
import getErrno
import getEwouldblock
import io.ktor.utils.io.ByteReadChannel
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import recv
import send
import shutdown
import socket.IOException
import socket.SocketClosedException
import socket.SocketInterface

public actual open class Socket(
    protected val socket: Int,
    private val writeRequest: MutableList<Int>?,
    private val buffer: ByteArray
) : SocketInterface {

    private var pendingSendData = mutableListOf<UByteArray>()

    actual override fun send(data: UByteArray) {
        data.toByteArray().usePinned { pinned ->
            val length = send(socket, pinned.addressOf(0), data.size, 0)
            if (length < 0) {
                val error = getErrno()
                if (error == getEagain() || error == getEwouldblock()) {
                    pendingSendData.add(data)
                    writeRequest?.add(socket)
                } else {
                    close()
                    throw IOException("Error in send $error")
                }
            } else if (length < data.size) {
                pendingSendData.add(data.copyOfRange(length, data.size))
                writeRequest?.add(socket)
            }
            pinned
        }
    }

    actual override suspend fun send(channel: ByteReadChannel) {

        try {

            var offset = 0
            val byteArray = ByteArray(1024)

            do {
                val currentRead = channel.readAvailable(byteArray, offset, byteArray.size)
                send(byteArray.toUByteArray())
                offset += currentRead
            } while (currentRead > 0)

        } catch (e: Exception) {
            close()
            throw IOException(e.message)
        }

    }

    actual override fun sendRemaining() {
        val sendData = mutableListOf<UByteArray>()
        sendData.addAll(pendingSendData)
        pendingSendData.clear()
        sendData.forEach {
            send(it)
        }
    }

    actual override fun read(): UByteArray? {
        buffer.usePinned { pinned ->
            val length = recv(socket.convert(), pinned.addressOf(0), buffer.size.convert(), 0)
            when {
                length == 0 -> {
                    close()
                    throw SocketClosedException()
                }
                length > 0 -> {
                    return pinned.get().toUByteArray().copyOfRange(0, length)
                }
                else -> {
                    val error = getErrno()
                    if (error != getEagain() && error != getEwouldblock()) {
                        close()
                        throw IOException("Error in recv: $error")
                    } else {
                        return null
                    }
                }
            }
        }
    }

    actual override fun close() {
        shutdown(socket)
        close(socket)
    }

}