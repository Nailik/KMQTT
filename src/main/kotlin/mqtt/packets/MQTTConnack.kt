package mqtt.packets

import mqtt.MQTTControlPacketType
import mqtt.encodeVariableByteInteger
import java.io.ByteArrayOutputStream

data class ConnectAcknowledgeFlags(val sessionPresentFlag: Boolean)

class MQTTConnack(
    val connectAcknowledgeFlags: ConnectAcknowledgeFlags,
    val connectReasonCode: ReasonCode,
    val properties: MQTTProperties
) : MQTTPacket {

    override fun toByteArray(): ByteArray {
        val outStream = ByteArrayOutputStream()

        outStream.write(if (connectAcknowledgeFlags.sessionPresentFlag && connectReasonCode == ReasonCode.SUCCESS) 1 else 0)
        outStream.write(connectReasonCode.ordinal)
        outStream.writeBytes(properties.serializeProperties(validProperties))

        val result = ByteArrayOutputStream()
        result.write((MQTTControlPacketType.CONNACK.ordinal shl 4) and 0xF0)
        result.encodeVariableByteInteger(outStream.size())
        return result.toByteArray()
    }

    companion object : MQTTDeserializer {

        private val validProperties = listOf(
            Property.SESSION_EXPIRY_INTERVAL,
            Property.ASSIGNED_CLIENT_IDENTIFIER,
            Property.SERVER_KEEP_ALIVE,
            Property.AUTHENTICATION_METHOD,
            Property.AUTHENTICATION_DATA,
            Property.RESPONSE_INFORMATION,
            Property.SERVER_REFERENCE,
            Property.REASON_STRING,
            Property.RECEIVE_MAXIMUM,
            Property.TOPIC_ALIAS_MAXIMUM,
            Property.MAXIMUM_QOS,
            Property.RETAIN_AVAILABLE,
            Property.USER_PROPERTY,
            Property.MAXIMUM_PACKET_SIZE,
            Property.WILDCARD_SUBSCRIPTION_AVAILABLE,
            Property.SUBSCRIPTION_IDENTIFIER_AVAILABLE,
            Property.SHARED_SUBSCRIPTION_AVAILABLE
        )

        override fun fromByteArray(flags: Int, data: ByteArray): MQTTPacket {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
}
