package com.biasedbit.nettytutorials.handshake.client;

import com.biasedbit.nettytutorials.handshake.common.ByteCounter;
import com.biasedbit.nettytutorials.handshake.common.MessageCounter;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
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
public class Client {

    // internal vars ----------------------------------------------------------

    private final String id;
    private final String serverId;
    private final ClientListener listener;
    private ClientBootstrap bootstrap;
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
        Executor bossPool = Executors.newCachedThreadPool();
        Executor workerPool = Executors.newCachedThreadPool();
        ChannelFactory factory =
                new NioClientSocketChannelFactory(bossPool, workerPool);
        this.bootstrap = new ClientBootstrap(factory);

        // Declared outside to fit under 80 char limit
        final DelimiterBasedFrameDecoder frameDecoder =
                new DelimiterBasedFrameDecoder(Integer.MAX_VALUE,
                                               Delimiters.lineDelimiter());
        this.bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                ByteCounter byteCounter =
                        new ByteCounter("--- CLIENT-COUNTER :: ");
                MessageCounter messageCounter =
                        new MessageCounter("--- CLIENT-MSGCOUNTER :: ");
                ClientHandshakeHandler handshakeHandler =
                        new ClientHandshakeHandler(id, serverId, 5000);

                return Channels.pipeline(byteCounter,
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
            this.bootstrap.releaseExternalResources();
            return false;
        }

        this.connector = future.getChannel();
        return this.connector.isConnected();
    }

    public void stop() {
        if (this.connector != null) {
            this.connector.close().awaitUninterruptibly();
        }
        this.bootstrap.releaseExternalResources();
        System.out.println("--- CLIENT - Stopped.");
    }

    public boolean sendMessage(String message) {
        if (this.connector.isConnected()) {
            // Append \n if it's not present, because of the frame delimiter
            if (!message.endsWith("\n")) {
                this.connector.write(message + '\n');
            } else {
                this.connector.write(message);
            }
            return true;
        }

        return false;
    }
}
