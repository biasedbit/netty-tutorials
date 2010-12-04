package com.biasedbit.nettytutorials.handshake.common;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.WriteCompletionEvent;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:bruno@biasedbit.com">Bruno de Carvalho</a>
 */
public class ByteCounter extends SimpleChannelUpstreamHandler {

    // internal vars ----------------------------------------------------------

    private final String id;
    private final AtomicLong writtenBytes;
    private final AtomicLong readBytes;

    // constructors -----------------------------------------------------------

    public ByteCounter(String id) {
        this.id = id;
        this.writtenBytes = new AtomicLong();
        this.readBytes = new AtomicLong();
    }

    // SimpleChannelUpstreamHandler -------------------------------------------

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        if (e.getMessage() instanceof ChannelBuffer) {
            this.readBytes.addAndGet(((ChannelBuffer) e.getMessage())
                    .readableBytes());
        }

        super.messageReceived(ctx, e);
    }

    @Override
    public void writeComplete(ChannelHandlerContext ctx, WriteCompletionEvent e)
            throws Exception {
        super.writeComplete(ctx, e);
        this.writtenBytes.addAndGet(e.getWrittenAmount());
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
        super.channelClosed(ctx, e);
        System.out.println(this.id + ctx.getChannel() + " -> sent: " +
                           this.getWrittenBytes() + "b, recv: " +
                           this.getReadBytes() + "b");
    }

    // getters & setters ------------------------------------------------------

    public long getWrittenBytes() {
        return writtenBytes.get();
    }

    public long getReadBytes() {
        return readBytes.get();
    }
}
