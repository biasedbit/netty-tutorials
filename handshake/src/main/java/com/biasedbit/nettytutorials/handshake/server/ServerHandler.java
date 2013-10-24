package com.biasedbit.nettytutorials.handshake.server;

import com.biasedbit.nettytutorials.handshake.common.HandshakeEvent;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:bruno@biasedbit.com">Bruno de Carvalho</a>
 */
public class ServerHandler extends ChannelInboundHandlerAdapter {

    // internal vars ----------------------------------------------------------

    private final AtomicInteger counter;
    private final ServerListener listener;
    private String remoteId;
    private Channel channel;

    // constructors -----------------------------------------------------------

    public ServerHandler(ServerListener listener) {
        this.listener = listener;
        this.counter = new AtomicInteger();
    }

    // SimpleChannelUpstreamHandler -------------------------------------------

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object e)
            throws Exception {
        if (e instanceof HandshakeEvent) {
            if (((HandshakeEvent) e).isSuccessful()) {
                out("+++ SERVER-HANDLER :: Handshake successful, connection " +
                    "to " + ((HandshakeEvent) e).getRemoteId() + " is up.");
                this.remoteId = ((HandshakeEvent) e).getRemoteId();
                this.channel = ctx.channel();
                // Notify the listener that a new connection is now READY
                this.listener.connectionOpen(this);
            } else {
                out("+++ SERVER-HANDLER :: Handshake failed.");
            }
            return;
        }

        super.userEventTriggered(ctx, e);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) 
            throws Exception {
        this.counter.incrementAndGet();
        this.listener.messageReceived(this, msg.toString());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        out("+++ SERVER-HANDLER :: Channel closed, received " +
            this.counter.get() + " messages: " + ctx.channel());
    }

    // public methods ---------------------------------------------------------

    public void sendMessage(String message) {
        if (!message.endsWith("\n")) {
            this.channel.writeAndFlush(message + '\n');
        } else {
            this.channel.writeAndFlush(message);
        }
    }

    public String getRemoteId() {
        return remoteId;
    }

    // private static helpers -------------------------------------------------

    private static void out(String s) {
        System.err.println(s);
    }
}
