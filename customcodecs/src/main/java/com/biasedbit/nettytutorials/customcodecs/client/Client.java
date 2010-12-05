package com.biasedbit.nettytutorials.customcodecs.client;

import com.biasedbit.nettytutorials.customcodecs.common.Envelope;
import com.biasedbit.nettytutorials.customcodecs.common.Decoder;
import com.biasedbit.nettytutorials.customcodecs.common.Encoder;
import com.biasedbit.nettytutorials.customcodecs.common.Type;
import com.biasedbit.nettytutorials.customcodecs.common.Version;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class Client implements ClientHandlerListener {

    // configuration --------------------------------------------------------------------------------------------------

    private final String host;
    private final int port;
    private final int messages;
    private final int floods;

    // internal vars --------------------------------------------------------------------------------------------------

    private ChannelFactory clientFactory;
    private ChannelGroup channelGroup;
    private ClientHandler handler;
    private final AtomicInteger received;
    private int flood;
    private long startTime;

    // constructors ---------------------------------------------------------------------------------------------------

    public Client(String host, int port, int messages, int floods) {
        this.host = host;
        this.port = port;
        this.messages = messages;
        this.floods = floods;
        this.received = new AtomicInteger(0);
        this.flood = 0;
    }

    // ClientHandlerListener ------------------------------------------------------------------------------------------

    @Override
    public void messageReceived(Envelope message) {
        // System.err.println("Received message " + message);
        if (this.received.incrementAndGet() == this.messages) {
            long stopTime = System.currentTimeMillis();
            float timeInSeconds = (stopTime - this.startTime) / 1000f;
            System.err.println("Sent and received " + this.messages + " in " + timeInSeconds + "s");
            System.err.println("That's " + (this.messages / timeInSeconds) + " echoes per second!");

            // ideally, this should be sent to another thread, since this method is called by a netty worker thread.
            if (this.flood < this.floods) {
                this.received.set(0);
                this.flood++;
                this.flood();
            }
        }
    }

    // public methods -------------------------------------------------------------------------------------------------

    public boolean start() {

        // For production scenarios, use limited sized thread pools
        this.clientFactory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(),
                                                               Executors.newCachedThreadPool());
        this.channelGroup = new DefaultChannelGroup(this + "-channelGroup");
        this.handler = new ClientHandler(this, this.channelGroup);
        ChannelPipelineFactory pipelineFactory = new ChannelPipelineFactory() {

            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("encoder", Encoder.getInstance());
                pipeline.addLast("decoder", new Decoder());
                pipeline.addLast("handler", handler);
                return pipeline;
            }
        };

        ClientBootstrap bootstrap = new ClientBootstrap(this.clientFactory);
        bootstrap.setOption("reuseAddress", true);
        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setOption("keepAlive", true);
        bootstrap.setPipelineFactory(pipelineFactory);

        
        boolean connected = bootstrap.connect(new InetSocketAddress(host, port)).awaitUninterruptibly().isSuccess();
        if (!connected) {
            this.stop();
        }

        return connected;
    }

    public void stop() {
        if (this.channelGroup != null) {
            this.channelGroup.close();
        }
        if (this.clientFactory != null) {
            this.clientFactory.releaseExternalResources();
        }
    }

    private void flood() {
        if ((this.channelGroup == null) || (this.clientFactory == null)) {
            return;
        }

        this.startTime = System.currentTimeMillis();
        for (int i = 0; i < this.messages; i++) {
            this.handler.sendMessage(new Envelope(Version.VERSION1, Type.REQUEST, new byte[175]));
        }
    }

    // main -----------------------------------------------------------------------------------------------------------

    public static void main(String[] args) throws InterruptedException {
        final Client client = new Client("localhost", 9999, 100000, 10);

        if (!client.start()) {

            System.exit(-1);
            return; // not really needed...
        }

        System.out.println("Client started...");

        client.flood();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                client.stop();
            }
        });
    }
}
