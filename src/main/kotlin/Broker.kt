import mqtt.Session
import mqtt.packets.MQTTProperties
import mqtt.packets.MQTTPublish
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.SocketAddress
import kotlin.concurrent.thread

// TODO 3.3
class Broker(
    local: SocketAddress,
    backlog: Int = 128,
    val maximumSessionExpiryInterval: UInt = 0xFFFFFFFFu,
    val receiveMaximum: Int? = null,
    val maximumQos: Int? = null,
    val retainedAvailable: Boolean = true,
    val maximumPacketSize: UInt? = null,
    val maximumTopicAlias: Int? = null,
    val wildcardSubscriptionAvailable: Boolean = true,
    val subscriptionIdentifiersAvailable: Boolean = true,
    val sharedSubscriptionsAvailable: Boolean = true,
    val serverKeepAlive: Int? = null
) {

    constructor(port: Int, host: String = "127.0.0.1") : this(InetSocketAddress(host, port))

    private val server = ServerSocket()
    val sessions = mutableMapOf<String, Session>()

    init {
        receiveMaximum?.let {
            require(it in 0..65535)
        }
        maximumQos?.let {
            require(it in 0..2)
        }

        server.bind(local, backlog)
    }

    fun listen() {
        while (true) {
            val client = server.accept()
            thread { ClientHandler(client, this).run() }
        }
    }

    fun publish(topicName: String, properties: MQTTProperties, payload: ByteArray) {
        sessions.forEach { session ->
            session.value.subscriptions.forEach { subscription ->
                if (subscription.topicName == topicName) { // TODO check wildcard match
                    subscription.subscriptionIdentifier?.let {
                        // TODO If the subscription was shared, then only the Subscription Identifiers that were present in the SUBSCRIBE packet from the Client which is receiving the message are returned in the PUBLISH packet.
                        properties.subscriptionIdentifier = it
                    }

                    // TODO continue after MQTT-3.3.4-6

                    val packet = MQTTPublish(
                        false,
                        subscription.qos,
                        false,
                        topicName, // TODO maybe use topic aliases
                        session.value.generatePacketId(),
                        properties,
                        payload
                    )
                    session.value.clientHandler.publish(packet)
                }
            }
        }
    }
}
