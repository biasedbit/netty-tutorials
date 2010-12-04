package com.biasedbit.nettytutorials.handshake.common;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:bruno@biasedbit.com">Bruno de Carvalho</a>
 */
public class MessageCounter extends SimpleChannelHandler {

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
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        this.readMessages.incrementAndGet();
        super.messageReceived(ctx, e);
    }

    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        this.writtenMessages.incrementAndGet();
        super.writeRequested(ctx, e);
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
        super.channelClosed(ctx, e);
        System.out.println(this.id + ctx.getChannel() + " -> sent: " +
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
