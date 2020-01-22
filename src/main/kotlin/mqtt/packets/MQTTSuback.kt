package mqtt.packets

import mqtt.MQTTControlPacketType
import mqtt.MQTTException
import mqtt.encodeVariableByteInteger
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class MQTTSuback(
    val packetIdentifier: UInt,
    val reasonCodes: List<ReasonCode>,
    val properties: MQTTProperties = MQTTProperties()
) : MQTTPacket {

    companion object : MQTTDeserializer {

        val validProperties = listOf(
            Property.REASON_STRING,
            Property.USER_PROPERTY
        )

        val validReasonCodes = listOf(
            ReasonCode.GRANTED_QOS0,
            ReasonCode.GRANTED_QOS1,
            ReasonCode.GRANTED_QOS2,
            ReasonCode.UNSPECIFIED_ERROR,
            ReasonCode.IMPLEMENTATION_SPECIFIC_ERROR,
            ReasonCode.NOT_AUTHORIZED,
            ReasonCode.TOPIC_FILTER_INVALID,
            ReasonCode.PACKET_IDENTIFIER_IN_USE,
            ReasonCode.QUOTA_EXCEEDED,
            ReasonCode.SHARED_SUBSCRIPTIONS_NOT_SUPPORTED,
            ReasonCode.SUBSCRIPTION_IDENTIFIERS_NOT_SUPPORTED,
            ReasonCode.WILDCARD_SUBSCRIPTIONS_NOT_SUPPORTED
        )

        override fun fromByteArray(flags: Int, data: ByteArray): MQTTSuback {
            checkFlags(flags)
            val inStream = ByteArrayInputStream(data)

            val packetIdentifier = inStream.read2BytesInt()
            val properties = inStream.deserializeProperties(validProperties)
            val reasonCodes = mutableListOf<ReasonCode>()
            while (inStream.available() > 0) {
                val reasonCode =
                    ReasonCode.valueOf(inStream.readByte().toInt()) ?: throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                if (reasonCode !in validReasonCodes)
                    throw MQTTException(ReasonCode.PROTOCOL_ERROR)
                reasonCodes += reasonCode
            }

            return MQTTSuback(packetIdentifier, reasonCodes, properties)
        }
    }

    override fun toByteArray(): ByteArray {
        val outStream = ByteArrayOutputStream()

        outStream.write2BytesInt(packetIdentifier)
        outStream.writeBytes(properties.serializeProperties(validProperties))

        reasonCodes.forEach {
            if (it !in validReasonCodes)
                throw IllegalArgumentException("Invalid reason code")
            outStream.writeByte(it.ordinal.toUInt())
        }

        val result = ByteArrayOutputStream()
        val fixedHeader = (MQTTControlPacketType.SUBACK.ordinal shl 4) and 0xF0
        result.write(fixedHeader)
        result.encodeVariableByteInteger(outStream.size().toUInt())
        return result.toByteArray()
    }
}
