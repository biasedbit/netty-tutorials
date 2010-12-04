package com.biasedbit.nettytutorials.handshake.server;

import com.biasedbit.nettytutorials.handshake.common.ByteCounter;
import com.biasedbit.nettytutorials.handshake.common.MessageCounter;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.Delimiters;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * @author <a href="mailto:bruno@biasedbit.com">Bruno de Carvalho</a>
 */
public class Server {

    // internal vars ----------------------------------------------------------

    private final String id;
    private final ServerListener listener;
    private ServerBootstrap bootstrap;
    private ChannelGroup channelGroup;

    // constructors -----------------------------------------------------------

    public Server(String id, ServerListener listener) {
        this.id = id;
        this.listener = listener;
    }

    // public methods ---------------------------------------------------------

    public boolean start() {
        // Pretty standard Netty startup stuff...
        // boss/worker executors, channel factory, channel group, pipeline, ...
        Executor bossPool = Executors.newCachedThreadPool();
        Executor workerPool = Executors.newCachedThreadPool();
        ChannelFactory factory =
                new NioServerSocketChannelFactory(bossPool, workerPool);
        this.bootstrap = new ServerBootstrap(factory);

        this.channelGroup = new DefaultChannelGroup(this.id + "-all-channels");


        // declared here to fit under the 80 char limit
        final ChannelHandler delimiter =
                new DelimiterBasedFrameDecoder(Integer.MAX_VALUE,
                                               Delimiters.lineDelimiter());
        this.bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ByteCounter counter =
                        new ByteCounter("+++ SERVER-COUNTER :: ");
                MessageCounter messageCounter =
                        new MessageCounter("+++ SERVER-MSGCOUNTER :: ");
                ServerHandshakeHandler handshakeHandler =
                        new ServerHandshakeHandler(id, channelGroup, 5000);
                return Channels.pipeline(counter,
                                         delimiter,
                                         new StringDecoder(),
                                         new StringEncoder(),
                                         messageCounter,
                                         handshakeHandler,
                                         new ServerHandler(listener));
            }
        });

        Channel acceptor = this.bootstrap.bind(new InetSocketAddress(12345));
        if (acceptor.isBound()) {
            System.err.println("+++ SERVER - bound to *:12345");
            this.channelGroup.add(acceptor);
            return true;
        } else {
            System.err.println("+++ SERVER - Failed to bind to *:12345");
            this.bootstrap.releaseExternalResources();
            return false;
        }
    }

    public void stop() {
        this.channelGroup.close().awaitUninterruptibly();
        this.bootstrap.releaseExternalResources();
        System.err.println("+++ SERVER - Stopped.");
    }
}
