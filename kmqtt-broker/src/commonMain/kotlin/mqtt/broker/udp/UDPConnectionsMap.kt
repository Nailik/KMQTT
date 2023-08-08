package mqtt.broker.udp

import io.ktor.utils.io.ByteReadChannel
import mqtt.broker.Broker
import mqtt.broker.ClientConnection
import socket.IOException
import socket.SocketInterface
import socket.udp.UDPEventHandler
import socket.udp.UDPSocket

internal class UDPConnectionsMap(private val socket: UDPSocket, private val broker: Broker) : UDPEventHandler, SocketInterface {

    private val udpSessions = mutableMapOf<String, ClientConnection>()
    private var currentKey = ""

    override fun dataReceived() {
        socket.read()?.let { data ->
            currentKey = data.sourceAddress + ":" + data.sourcePort
            if (udpSessions.containsKey(currentKey)) {
                udpSessions[currentKey]?.dataReceived(data.data)
            } else {
                udpSessions[currentKey] = ClientConnection(this@UDPConnectionsMap, broker)
            }
        }
    }

    override fun send(data: UByteArray) {
        val ip = currentKey.split(":")
        socket.send(data, ip[0], ip[1].toInt())
    }

    override suspend fun send(channel: ByteReadChannel) {

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

    override fun sendRemaining() {

    }

    override fun read(): UByteArray? {
        return socket.read()?.data
    }

    override fun close() {
        udpSessions.remove(currentKey)
    }
}
