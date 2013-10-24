package com.biasedbit.nettytutorials.handshake.client;

import com.biasedbit.nettytutorials.handshake.common.HandshakeEvent;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:bruno@biasedbit.com">Bruno de Carvalho</a>
 */
public class ClientHandler extends ChannelInboundHandlerAdapter {

    // internal vars ----------------------------------------------------------

    private final AtomicInteger counter;
    private final ClientListener listener;

    // constructors -----------------------------------------------------------

    public ClientHandler(ClientListener listener) {
        this.listener = listener;
        this.counter = new AtomicInteger();
    }

    // SimpleChannelUpstreamHandler -------------------------------------------

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object e)
            throws Exception {
        if (e instanceof HandshakeEvent) {
            if (((HandshakeEvent) e).isSuccessful()) {
                out("--- CLIENT-HANDLER :: Handshake successful, connection " +
                    "to " + ((HandshakeEvent) e).getRemoteId() + " is up.");
            } else {
                out("--- CLIENT-HANDLER :: Handshake failed.");
            }
            return;
        }

        super.userEventTriggered(ctx, e);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        this.counter.incrementAndGet();
        this.listener.messageReceived(msg.toString());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        out("--- CLIENT-HANDLER :: Channel closed, received " +
            this.counter.get() + " messages: " + ctx.channel());
    }

    // private static helpers -------------------------------------------------

    private static void out(String s) {
        System.out.println(s);
    }
}
