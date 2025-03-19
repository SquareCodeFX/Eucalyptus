package org.eucalyptus.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Decoder for converting ByteBuf to byte arrays.
 * This decoder reads the length of the message first (as an integer),
 * then reads that many bytes to form the complete message.
 */
public class PacketDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(PacketDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        // Wait until we have at least 4 bytes (int length)
        if (in.readableBytes() < 4) {
            return;
        }

        // Mark the current reader index
        in.markReaderIndex();

        // Read the length of the message
        int length = in.readInt();

        // Check if the message is complete
        if (in.readableBytes() < length) {
            // Reset to the marked position and wait for more data
            in.resetReaderIndex();
            return;
        }

        // Read the message bytes
        byte[] data = new byte[length];
        in.readBytes(data);

        // Add the decoded message to the output list
        out.add(data);
        
        logger.debug("Decoded message of length: {}", length);
    }
}
