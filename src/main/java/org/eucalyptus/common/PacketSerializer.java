package org.eucalyptus.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eucalyptus.model.DataPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Utility class for serializing and deserializing DataPacket objects.
 * Uses Jackson for JSON serialization.
 */
public class PacketSerializer {
    private static final Logger logger = LoggerFactory.getLogger(PacketSerializer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Serializes a DataPacket object to a byte array.
     *
     * @param packet The DataPacket to serialize
     * @return The serialized byte array
     * @throws IOException If serialization fails
     */
    public static byte[] serialize(DataPacket packet) throws IOException {
        try {
            return objectMapper.writeValueAsBytes(packet);
        } catch (IOException e) {
            logger.error("Failed to serialize packet: {}", packet, e);
            throw e;
        }
    }

    /**
     * Deserializes a byte array to a DataPacket object.
     *
     * @param data The byte array to deserialize
     * @return The deserialized DataPacket
     * @throws IOException If deserialization fails
     */
    public static DataPacket deserialize(byte[] data) throws IOException {
        try {
            return objectMapper.readValue(data, DataPacket.class);
        } catch (IOException e) {
            logger.error("Failed to deserialize packet", e);
            throw e;
        }
    }
}
