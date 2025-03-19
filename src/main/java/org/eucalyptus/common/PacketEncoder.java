package org.eucalyptus.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encoder for converting byte arrays to ByteBuf.
 * This encoder writes the length of the message first (as an integer),
 * then writes the message bytes.
 */
public class PacketEncoder extends MessageToByteEncoder<byte[]> {
    private static final Logger logger = LoggerFactory.getLogger(PacketEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, byte[] msg, ByteBuf out) {
        // Write the length of the message
        out.writeInt(msg.length);
        
        // Write the message bytes
        out.writeBytes(msg);
        
        logger.debug("Encoded message of length: {}", msg.length);
    }
}
