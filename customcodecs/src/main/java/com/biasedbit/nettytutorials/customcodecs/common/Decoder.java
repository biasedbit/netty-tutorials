package com.biasedbit.nettytutorials.customcodecs.common;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;

public class Decoder extends ReplayingDecoder<Decoder.DecodingState> {

    // internal vars --------------------------------------------------------------------------------------------------

    private Envelope message;

    // constructors ---------------------------------------------------------------------------------------------------

    public Decoder() {
        this.reset();
    }

    // ReplayingDecoder -----------------------------------------------------------------------------------------------

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer,
                            DecodingState state) throws Exception {
        // notice the switch fall-through
        switch (state) {
            case VERSION:
                this.message.setVersion(Version.fromByte(buffer.readByte()));
                checkpoint(DecodingState.TYPE);
            case TYPE:
                this.message.setType(Type.fromByte(buffer.readByte()));
                checkpoint(DecodingState.PAYLOAD_LENGTH);
            case PAYLOAD_LENGTH:
                int size = buffer.readInt();
                if (size <= 0) {
                    throw new Exception("Invalid content size");
                }
                // pre-allocate content buffer
                byte[] content = new byte[size];
                this.message.setPayload(content);
                checkpoint(DecodingState.PAYLOAD);
            case PAYLOAD:
                // drain the channel buffer to the message content buffer
                // I have no idea what the contents are, but I'm sure you'll figure out how to turn these
                // bytes into useful content.
                buffer.readBytes(this.message.getPayload(), 0, this.message.getPayload().length);

                // This is the only exit point of this method (except for the two other exceptions that
                // should never occur).
                // Whenever there aren't enough bytes, a special exception is thrown by the channel buffer
                // and automatically handled by netty. That's why all conditions in the switch fall through
                try {
                    // return the instance var and reset this decoder state after doing so.
                    return this.message;
                } finally {
                    this.reset();
                }
            default:
                throw new Exception("Unknown decoding state: " + state);
        }
    }

    // private helpers ------------------------------------------------------------------------------------------------

    private void reset() {
        checkpoint(DecodingState.VERSION);
        this.message = new Envelope();
    }

    // private classes ------------------------------------------------------------------------------------------------

    public enum DecodingState {

        // constants --------------------------------------------------------------------------------------------------

        VERSION,
        TYPE,
        PAYLOAD_LENGTH,
        PAYLOAD,
    }
}