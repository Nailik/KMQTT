package socket

import io.ktor.utils.io.ByteReadChannel

public interface SocketInterface {

    public fun send(data: UByteArray)

    public suspend fun send(channel: ByteReadChannel)

    public fun sendRemaining()

    public fun read(): UByteArray?

    public fun close()
}
