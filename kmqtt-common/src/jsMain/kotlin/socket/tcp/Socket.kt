package socket.tcp

import Buffer
import io.ktor.utils.io.ByteReadChannel
import punycode.toUnicode
import socket.IOException
import socket.SocketInterface
import socket.SocketState
import toBuffer
import toUByteArray

public actual open class Socket(
    protected val socket: net.Socket,
    private val selectCallback: (attachment: Any?, state: SocketState) -> Boolean
) : SocketInterface {

    private val queue = ArrayDeque<UByteArray>()
    private var attachment: Any? = null

    init {
        socket.on("data") { data: Buffer ->
            queue.add(data.toUByteArray())
            selectCallback(attachment, SocketState.READ)
        }
        socket.on("drain", {
            socket.resume()
            Unit
        } as () -> Unit)
    }

    actual override fun send(data: UByteArray) {
        socket.write(data.toBuffer())
        selectCallback(attachment, SocketState.WRITE)
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

    actual override fun read(): UByteArray? {
        return queue.removeFirstOrNull()
    }

    actual override fun close() {
        socket.end()
    }

    actual override fun sendRemaining() {

    }

    public fun setAttachment(attachment: Any?) {
        this.attachment = attachment
    }

}