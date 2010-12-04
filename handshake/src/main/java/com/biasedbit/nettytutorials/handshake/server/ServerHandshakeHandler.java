package com.biasedbit.nettytutorials.handshake.server;

import com.biasedbit.nettytutorials.handshake.common.Challenge;
import com.biasedbit.nettytutorials.handshake.common.HandshakeEvent;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.group.ChannelGroup;

import java.net.SocketAddress;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:bruno@biasedbit.com">Bruno de Carvalho</a>
 */
public class ServerHandshakeHandler extends SimpleChannelHandler {

    // internal vars ----------------------------------------------------------

    private final long timeoutInMillis;
    private final String localId;
    private final ChannelGroup group;
    private final AtomicBoolean handshakeComplete;
    private final AtomicBoolean handshakeFailed;
    private final Object handshakeMutex = new Object();
    private final Queue<MessageEvent> messages = new ArrayDeque<MessageEvent>();
    private final CountDownLatch latch = new CountDownLatch(1);

    // constructors -----------------------------------------------------------

    public ServerHandshakeHandler(String localId, ChannelGroup group,
                                  long timeoutInMillis) {
        this.localId = localId;
        this.group = group;
        this.timeoutInMillis = timeoutInMillis;
        this.handshakeComplete = new AtomicBoolean(false);
        this.handshakeFailed = new AtomicBoolean(false);
    }

    // SimpleChannelHandler ---------------------------------------------------

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        if (this.handshakeFailed.get()) {
            // Bail out fast if handshake already failed
            return;
        }

        if (this.handshakeComplete.get()) {
            // If handshake succeeded but message still came through this
            // handler, then immediately send it upwards.
            super.messageReceived(ctx, e);
            return;
        }

        synchronized (this.handshakeMutex) {
            // Recheck conditions after locking the mutex.
            // Things might have changed while waiting for the lock.
            if (this.handshakeFailed.get()) {
                return;
            }

            if (this.handshakeComplete.get()) {
                super.messageReceived(ctx, e);
                return;
            }

            // Validate handshake
            String handshake = (String) e.getMessage();
            // 1. Validate expected clientId:serverId:challenge format
            String[] params = handshake.trim().split(":");
            if (params.length != 3) {
                out("+++ SERVER-HS :: Invalid handshake: expecting 3 params, " +
                    "got " + params.length + " -> '" + handshake + "'");
                this.fireHandshakeFailed(ctx);
                return;
            }

            // 2. Validate the asserted serverId = localId
            String client = params[0];
            if (!this.localId.equals(params[1])) {
                out("+++ SERVER-HS :: Invalid handshake: this is " +
                    this.localId + " and client thinks it's " + params[1]);
                this.fireHandshakeFailed(ctx);
                return;
            }

            // 3. Validate the challenge format.
            if (!Challenge.isValidChallenge(params[2])) {
                out("+++ SERVER-HS :: Invalid handshake: invalid challenge '" +
                    params[2] + "'");
                this.fireHandshakeFailed(ctx);
                return;
            }

            // Success! Write the challenge response.
            out("+++ SERVER-HS :: Challenge validated, flushing messages & " +
                "removing handshake handler from  pipeline.");
            String response = params[0] + ':' + params[1] + ':' +
                              Challenge.generateResponse(params[2]) + '\n';
            this.writeDownstream(ctx, response);

            // Flush any pending messages (in this tutorial, no messages will
            // ever be queued because the server does not take the initiative
            // of sending messages to clients on its own...
            out("+++ SERVER-HS :: " + this.messages.size() +
                " messages in queue to be flushed.");
            for (MessageEvent message : this.messages) {
                ctx.sendDownstream(message);
            }

            // Finally, remove this handler from the pipeline and fire success
            // event up the pipeline.
            out("+++ SERVER-HS :: Removing handshake handler from pipeline.");
            ctx.getPipeline().remove(this);
            this.fireHandshakeSucceeded(client, ctx);
        }
    }

    @Override
    public void channelConnected(final ChannelHandlerContext ctx,
                                 ChannelStateEvent e) throws Exception {
        this.group.add(ctx.getChannel());
        out("+++ SERVER-HS :: Incoming connection established from: " +
            e.getChannel().getRemoteAddress());

        // Fire up the handshake handler timeout checker.
        // Wait X seconds for the handshake then disconnect.
        new Thread() {

            @Override
            public void run() {
                try {
                    latch.await(timeoutInMillis, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e1) {
                    out("+++ SERVER-HS :: Handshake timeout checker: " +
                        "interrupted!");
                    e1.printStackTrace();
                }

                if (handshakeFailed.get()) {
                    out("+++ SERVER-HS :: (pre-synchro) Handshake timeout " +
                        "checker: discarded (handshake failed)");
                    return;
                }

                if (handshakeComplete.get()) {
                    out("+++ SERVER-HS :: (pre-synchro) Handshake timeout " +
                        "checker: discarded (handshake complete)");
                    return;
                }

                synchronized (handshakeMutex) {
                    if (handshakeFailed.get()) {
                        out("+++ SERVER-HS :: (synchro) Handshake timeout " +
                            "checker: already failed.");
                        return;
                    }

                    if (!handshakeComplete.get()) {
                        out("+++ SERVER-HS :: (synchro) Handshake timeout " +
                            "checker: timed out, killing connection.");
                        ctx.sendUpstream(HandshakeEvent
                                .handshakeFailed(ctx.getChannel()));
                        handshakeFailed.set(true);
                        ctx.getChannel().close();
                    } else {
                        out("+++ SERVER-HS :: (synchro) Handshake timeout " +
                            "checker: discarded (handshake OK)");
                    }
                }
            }
        }.start();
    }

    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e)
            throws Exception {
        out("+++ SERVER-HS :: Channel closed.");
        if (!this.handshakeComplete.get()) {
            this.fireHandshakeFailed(ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        out("+++ SERVER-HS :: Exception caught.");
        e.getCause().printStackTrace();
        if (e.getChannel().isConnected()) {
            // Closing the channel will trigger handshake failure.
            e.getChannel().close();
        } else {
            // Channel didn't open, so we must fire handshake failure directly.
            this.fireHandshakeFailed(ctx);
        }
    }


    @Override
    public void writeRequested(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        // Before doing anything, ensure that noone else is working by
        // acquiring a lock on the handshakeMutex.
        synchronized (this.handshakeMutex) {
            if (this.handshakeFailed.get()) {
                // If the handshake failed meanwhile, discard any messages.
                return;
            }

            // If the handshake hasn't failed but completed meanwhile and
            // messages still passed through this handler, then forward
            // them downwards.
            if (this.handshakeComplete.get()) {
                out("+++ SERVER-HS :: Handshake already completed, not " +
                    "appending '" + e.getMessage().toString().trim() +
                    "' to queue!");
                super.writeRequested(ctx, e);
            } else {
                // Otherwise, queue messages in order until the handshake
                // completes.
                this.messages.offer(e);
            }
        }
    }

    // private static helpers -------------------------------------------------

    private static void out(String s) {
        System.err.println(s);
    }

    // private helpers --------------------------------------------------------

    private void writeDownstream(ChannelHandlerContext ctx, Object data) {
        // Just declaring these variables so that last statement in this
        // method fits inside the 80 char limit... I typically use 120 :)
        ChannelFuture f = Channels.succeededFuture(ctx.getChannel());
        SocketAddress address = ctx.getChannel().getRemoteAddress();
        Channel c = ctx.getChannel();

        ctx.sendDownstream(new DownstreamMessageEvent(c, f, data, address));
    }

    private void fireHandshakeFailed(ChannelHandlerContext ctx) {
        this.handshakeComplete.set(true);
        this.handshakeFailed.set(true);
        this.latch.countDown();
        ctx.getChannel().close();
        ctx.sendUpstream(HandshakeEvent.handshakeFailed(ctx.getChannel()));
    }

    private void fireHandshakeSucceeded(String client,
                                        ChannelHandlerContext ctx) {
        this.handshakeComplete.set(true);
        this.handshakeFailed.set(false);
        this.latch.countDown();
        ctx.sendUpstream(HandshakeEvent
                .handshakeSucceeded(client, ctx.getChannel()));
    }
}
