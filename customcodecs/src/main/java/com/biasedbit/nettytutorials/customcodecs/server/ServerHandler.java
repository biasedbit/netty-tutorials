package com.biasedbit.nettytutorials.customcodecs.server;

import com.biasedbit.nettytutorials.customcodecs.common.Envelope;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;

public class ServerHandler extends SimpleChannelUpstreamHandler {

    // internal vars --------------------------------------------------------------------------------------------------

    private final ChannelGroup channelGroup;

    // constructors ---------------------------------------------------------------------------------------------------

    public ServerHandler(ChannelGroup channelGroup) {
        this.channelGroup = channelGroup;
    }

    // SimpleChannelUpstreamHandler -----------------------------------------------------------------------------------

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        this.channelGroup.add(e.getChannel());
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        if (e.getMessage() instanceof Envelope) {
            // echo it...
            e.getChannel().write(e.getMessage());
        } else {
            super.messageReceived(ctx, e);
        }
    }
}
