package com.biasedbit.nettytutorials.handshake.client;

import com.biasedbit.nettytutorials.handshake.common.HandshakeEvent;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:bruno@biasedbit.com">Bruno de Carvalho</a>
 */
public class ClientHandler extends SimpleChannelUpstreamHandler {

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
    public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e)
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

        super.handleUpstream(ctx, e);
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        this.counter.incrementAndGet();
        this.listener.messageReceived(e.getMessage().toString());
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
        super.channelClosed(ctx, e);
        out("--- CLIENT-HANDLER :: Channel closed, received " +
            this.counter.get() + " messages: " + e.getChannel());
    }

    // private static helpers -------------------------------------------------

    private static void out(String s) {
        System.out.println(s);
    }
}
