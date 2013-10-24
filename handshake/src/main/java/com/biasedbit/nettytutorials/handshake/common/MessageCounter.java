package com.biasedbit.nettytutorials.handshake.common;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:bruno@biasedbit.com">Bruno de Carvalho</a>
 */
public class MessageCounter extends ChannelDuplexHandler {

    // internal vars ----------------------------------------------------------

    private final String id;
    private final AtomicLong writtenMessages;
    private final AtomicLong readMessages;

    // constructors -----------------------------------------------------------

    public MessageCounter(String id) {
        this.id = id;
        this.writtenMessages = new AtomicLong();
        this.readMessages = new AtomicLong();
    }

    // SimpleChannelHandler ---------------------------------------------------

    @Override
    public void channelRead(ChannelHandlerContext ctx, 
            Object msg) throws Exception {
        readMessages.incrementAndGet();
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, 
            ChannelPromise promise) throws Exception {
        writtenMessages.incrementAndGet();
        ctx.write(msg, promise);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
        System.out.println(this.id + ctx.channel() + " -> sent: " +
                           this.getWrittenMessages() + ", recv: " +
                           this.getReadMessages());
    }

    // getters & setters ------------------------------------------------------

    public long getWrittenMessages() {
        return writtenMessages.get();
    }

    public long getReadMessages() {
        return readMessages.get();
    }
}
