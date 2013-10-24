package com.biasedbit.nettytutorials.handshake.server;

import com.biasedbit.nettytutorials.handshake.common.ByteCounter;
import com.biasedbit.nettytutorials.handshake.common.MessageCounter;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.net.InetSocketAddress;

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
        NioEventLoopGroup bossGroup = new NioEventLoopGroup();
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        this.bootstrap = new ServerBootstrap()
                .channel(NioServerSocketChannel.class)
                .group(bossGroup, workerGroup);

        this.channelGroup = new DefaultChannelGroup(this.id + "-all-channels",
                GlobalEventExecutor.INSTANCE);


        this.bootstrap.childHandler(new ChannelInitializer() {

            @Override
            protected void initChannel(Channel ch) throws Exception {
                ByteCounter counter =
                        new ByteCounter("+++ SERVER-COUNTER :: ");
                ChannelHandler delimiter =
                new DelimiterBasedFrameDecoder(Integer.MAX_VALUE,
                                               Delimiters.lineDelimiter());
                MessageCounter messageCounter =
                        new MessageCounter("+++ SERVER-MSGCOUNTER :: ");
                ServerHandshakeHandler handshakeHandler =
                        new ServerHandshakeHandler(id, channelGroup, 5000);
                
                ch.pipeline().addLast(counter,
                                         delimiter,
                                         new StringDecoder(),
                                         new StringEncoder(),
                                         messageCounter,
                                         handshakeHandler,
                                         new ServerHandler(listener));
            }
        });

        Channel acceptor = this.bootstrap.bind(new InetSocketAddress(12345))
                .awaitUninterruptibly().channel();
        if (acceptor.isActive()) {
            System.err.println("+++ SERVER - bound to *:12345");
            this.channelGroup.add(acceptor);
            return true;
        } else {
            System.err.println("+++ SERVER - Failed to bind to *:12345");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            return false;
        }
    }

    public void stop() {
        this.channelGroup.close().awaitUninterruptibly();
        this.bootstrap.group().shutdownGracefully();
        this.bootstrap.childGroup().shutdownGracefully();
        System.err.println("+++ SERVER - Stopped.");
    }
}
