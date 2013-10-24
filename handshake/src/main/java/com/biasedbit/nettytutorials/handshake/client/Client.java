package com.biasedbit.nettytutorials.handshake.client;

import com.biasedbit.nettytutorials.handshake.common.ByteCounter;
import com.biasedbit.nettytutorials.handshake.common.MessageCounter;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.net.InetSocketAddress;

/**
 * @author <a href="mailto:bruno@biasedbit.com">Bruno de Carvalho</a>
 */
public class Client {

    // internal vars ----------------------------------------------------------

    private final String id;
    private final String serverId;
    private final ClientListener listener;
    private Bootstrap bootstrap;
    private Channel connector;

    // constructors -----------------------------------------------------------

    public Client(String id, String serverId, ClientListener listener) {
        this.id = id;
        this.serverId = serverId;
        this.listener = listener;
    }

    // public methods ---------------------------------------------------------

    public boolean start() {
        // Standard netty bootstrapping stuff.
        NioEventLoopGroup workerGroup = new NioEventLoopGroup();
        this.bootstrap = new Bootstrap()
                .channel(NioSocketChannel.class)
                .group(workerGroup);

        // Declared outside to fit under 80 char limit
        final DelimiterBasedFrameDecoder frameDecoder =
                new DelimiterBasedFrameDecoder(Integer.MAX_VALUE,
                                               Delimiters.lineDelimiter());
        this.bootstrap.handler(new ChannelInitializer() {

            @Override
            protected void initChannel(Channel ch) throws Exception {
                ByteCounter byteCounter =
                        new ByteCounter("--- CLIENT-COUNTER :: ");
                MessageCounter messageCounter =
                        new MessageCounter("--- CLIENT-MSGCOUNTER :: ");
                ClientHandshakeHandler handshakeHandler =
                        new ClientHandshakeHandler(id, serverId, 5000);
                
                ch.pipeline().addLast(byteCounter,
                                         frameDecoder,
                                         new StringDecoder(),
                                         new StringEncoder(),
                                         messageCounter,
                                         handshakeHandler,
                                         new ClientHandler(listener));
            }
        });

        ChannelFuture future = this.bootstrap
                .connect(new InetSocketAddress("localhost", 12345));
        if (!future.awaitUninterruptibly().isSuccess()) {
            System.out.println("--- CLIENT - Failed to connect to server at " +
                               "localhost:12345.");
            workerGroup.shutdownGracefully();
            return false;
        }

        this.connector = future.channel();
        return this.connector.isActive();
    }

    public void stop() {
        if (this.connector != null) {
            this.connector.close().awaitUninterruptibly();
        }
        this.bootstrap.group().shutdownGracefully();
        System.out.println("--- CLIENT - Stopped.");
    }

    public boolean sendMessage(String message) {
        if (this.connector.isActive()) {
            // Append \n if it's not present, because of the frame delimiter
            if (!message.endsWith("\n")) {
                this.connector.writeAndFlush(message + '\n');
            } else {
                this.connector.writeAndFlush(message);
            }
            return true;
        }

        return false;
    }
}
