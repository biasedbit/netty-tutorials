package com.biasedbit.nettytutorials.handshake.common;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author <a href="mailto:bruno@biasedbit.com">Bruno de Carvalho</a>
 */
public class ByteCounter extends ChannelDuplexHandler {

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
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        long size;
        if (msg instanceof ByteBuf) {
            size = ((ByteBuf) msg).readableBytes();
        } else if (msg instanceof ByteBufHolder) {
            size = ((ByteBufHolder) msg).content().readableBytes();
        } else {
            size = -1;
        }
        readBytes.addAndGet(size);
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, 
            ChannelPromise promise) throws Exception {
        long size;
        if (msg instanceof ByteBuf) {
            size = ((ByteBuf) msg).writableBytes();
        } else if (msg instanceof ByteBufHolder) {
            size = ((ByteBufHolder) msg).content().writableBytes();
        } else {
            size = -1;
        }
        writtenBytes.addAndGet(size);
        ctx.write(msg, promise);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
        System.out.println(this.id + ctx.channel() + " -> sent: " +
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
