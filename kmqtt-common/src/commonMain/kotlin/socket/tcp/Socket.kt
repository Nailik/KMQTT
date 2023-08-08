package socket.tcp

import io.ktor.utils.io.ByteReadChannel
import socket.SocketInterface

public expect open class Socket : SocketInterface {
    override fun send(data: UByteArray)

    override suspend fun send(channel: ByteReadChannel)

    override fun sendRemaining()

    override fun read(): UByteArray?

    override fun close()
}