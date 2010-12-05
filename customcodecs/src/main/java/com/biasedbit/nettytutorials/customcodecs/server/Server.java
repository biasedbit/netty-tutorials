package com.biasedbit.nettytutorials.customcodecs.server;

import com.biasedbit.nettytutorials.customcodecs.common.Decoder;
import com.biasedbit.nettytutorials.customcodecs.common.Encoder;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ServerChannelFactory;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Server {

    // configuration --------------------------------------------------------------------------------------------------

    private final String host;
    private final int port;
    private DefaultChannelGroup channelGroup;
    private ServerChannelFactory serverFactory;

    // constructors ---------------------------------------------------------------------------------------------------

    public Server(String host, int port) {
        this.host = host;
        this.port = port;
    }

    // public methods -------------------------------------------------------------------------------------------------

    public boolean start() {

        this.serverFactory = new NioServerSocketChannelFactory(Executors.newCachedThreadPool(),
                                                               Executors.newCachedThreadPool());
        this.channelGroup = new DefaultChannelGroup(this + "-channelGroup");
        ChannelPipelineFactory pipelineFactory = new ChannelPipelineFactory() {

            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("encoder", Encoder.getInstance());
                pipeline.addLast("decoder", new Decoder());
                pipeline.addLast("handler", new ServerHandler(channelGroup));
                return pipeline;
            }
        };

        ServerBootstrap bootstrap = new ServerBootstrap(this.serverFactory);
        bootstrap.setOption("reuseAddress", true);
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);
        bootstrap.setPipelineFactory(pipelineFactory);

        Channel channel = bootstrap.bind(new InetSocketAddress(this.host, this.port));
        if (!channel.isBound()) {
            this.stop();
            return false;
        }

        this.channelGroup.add(channel);
        return true;
    }

    public void stop() {
        if (this.channelGroup != null) {
            this.channelGroup.close();
        }
        if (this.serverFactory != null) {
            this.serverFactory.releaseExternalResources();
        }
    }

    // main -----------------------------------------------------------------------------------------------------------

    public static void main(String[] args) {
        final Server server = new Server("localhost", 9999);

        if (!server.start()) {

            System.exit(-1);
            return; // not really needed...
        }

        System.out.println("Server started...");

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.stop();
            }
        });
    }
}
