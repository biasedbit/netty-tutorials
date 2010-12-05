package com.biasedbit.nettytutorials.customcodecs.common;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

/**
 * OneToOneEncoder implementation that converts an Envelope instance into a ChannelBuffer
 */
@ChannelHandler.Sharable
public class Encoder extends OneToOneEncoder {

    // constructors ---------------------------------------------------------------------------------------------------

    private Encoder() {
    }

    // public static methods ------------------------------------------------------------------------------------------

    public static Encoder getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public static ChannelBuffer encodeMessage(Envelope message) throws IllegalArgumentException {
        // you can move these verifications "upper" (before writing to the channel) in order not to cause a
        // channel shutdown.
        if ((message.getVersion() == null) || (message.getVersion() == Version.UNKNOWN)) {
            throw new IllegalArgumentException("Message version cannot be null or UNKNOWN");
        }

        if ((message.getType() == null) || (message.getType() == Type.UNKNOWN)) {
            throw new IllegalArgumentException("Message type cannot be null or UNKNOWN");
        }

        if ((message.getPayload() == null) || (message.getPayload().length == 0)) {
            throw new IllegalArgumentException("Message payload cannot be null or empty");
        }

        // version(1b) + type(1b) + payload length(4b) + payload(nb)
        int size = 6 + message.getPayload().length;

        ChannelBuffer buffer = ChannelBuffers.buffer(size);
        buffer.writeByte(message.getVersion().getByteValue());
        buffer.writeByte(message.getType().getByteValue());
        buffer.writeInt(message.getPayload().length);
        buffer.writeBytes(message.getPayload());

        return buffer;
    }

    // OneToOneEncoder ------------------------------------------------------------------------------------------------

    @Override
    protected Object encode(ChannelHandlerContext channelHandlerContext, Channel channel, Object msg) throws Exception {
        if (msg instanceof Envelope) {
            return encodeMessage((Envelope) msg);
        } else {
            return msg;
        }
    }

    // private classes ------------------------------------------------------------------------------------------------

    private static final class InstanceHolder {
        private static final Encoder INSTANCE = new Encoder();
    }
}
