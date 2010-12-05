package com.biasedbit.nettytutorials.customcodecs.client;

import com.biasedbit.nettytutorials.customcodecs.common.Envelope;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;

public class ClientHandler extends SimpleChannelUpstreamHandler {

    // internal vars --------------------------------------------------------------------------------------------------

    private final ClientHandlerListener listener;
    private final ChannelGroup channelGroup;
    private Channel channel;

    // constructors ---------------------------------------------------------------------------------------------------

    public ClientHandler(ClientHandlerListener listener, ChannelGroup channelGroup) {
        this.listener = listener;
        this.channelGroup = channelGroup;
    }

    // SimpleChannelUpstreamHandler -----------------------------------------------------------------------------------

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (e.getMessage() instanceof Envelope) {
            this.listener.messageReceived((Envelope) e.getMessage());
        } else {
            super.messageReceived(ctx, e);
        }
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        this.channel = e.getChannel();
        this.channelGroup.add(e.getChannel());
    }

    // public methods -------------------------------------------------------------------------------------------------

    public void sendMessage(Envelope envelope) {
        if (this.channel != null) {
            this.channel.write(envelope);
        }
    }
}
