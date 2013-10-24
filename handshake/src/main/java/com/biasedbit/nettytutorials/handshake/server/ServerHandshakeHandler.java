package com.biasedbit.nettytutorials.handshake.server;

import com.biasedbit.nettytutorials.handshake.common.Challenge;
import com.biasedbit.nettytutorials.handshake.common.HandshakeEvent;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.group.ChannelGroup;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:bruno@biasedbit.com">Bruno de Carvalho</a>
 */
public class ServerHandshakeHandler extends ChannelDuplexHandler {

    // internal vars ----------------------------------------------------------

    private final long timeoutInMillis;
    private final String localId;
    private final ChannelGroup group;
    private final AtomicBoolean handshakeComplete;
    private final AtomicBoolean handshakeFailed;
    private final Object handshakeMutex = new Object();
    private final Queue<Object> messages = new ArrayDeque<Object>();
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
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        if (this.handshakeFailed.get()) {
            // Bail out fast if handshake already failed
            return;
        }

        if (this.handshakeComplete.get()) {
            // If handshake succeeded but message still came through this
            // handler, then immediately send it upwards.
            super.channelRead(ctx, msg);
            return;
        }

        synchronized (this.handshakeMutex) {
            // Recheck conditions after locking the mutex.
            // Things might have changed while waiting for the lock.
            if (this.handshakeFailed.get()) {
                return;
            }

            if (this.handshakeComplete.get()) {
                super.channelRead(ctx, msg);
                return;
            }

            // Validate handshake
            String handshake = (String) msg;
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
            ctx.write(response);

            // Flush any pending messages (in this tutorial, no messages will
            // ever be queued because the server does not take the initiative
            // of sending messages to clients on its own...
            out("+++ SERVER-HS :: " + this.messages.size() +
                " messages in queue to be flushed.");
            for (Object message : this.messages) {
                ctx.write(message);
            }
            ctx.flush();

            // Finally, remove this handler from the pipeline and fire success
            // event up the pipeline.
            out("+++ SERVER-HS :: Removing handshake handler from pipeline.");
            ctx.pipeline().remove(this);
            this.fireHandshakeSucceeded(client, ctx);
        }
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) 
            throws Exception {
        this.group.add(ctx.channel());
        out("+++ SERVER-HS :: Incoming connection established from: " +
            ctx.channel().remoteAddress());

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
                        ctx.fireUserEventTriggered(HandshakeEvent
                                .handshakeFailed());
                        handshakeFailed.set(true);
                        ctx.channel().close();
                    } else {
                        out("+++ SERVER-HS :: (synchro) Handshake timeout " +
                            "checker: discarded (handshake OK)");
                    }
                }
            }
        }.start();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        out("+++ SERVER-HS :: Channel closed.");
        if (!this.handshakeComplete.get()) {
            this.fireHandshakeFailed(ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        out("+++ SERVER-HS :: Exception caught.");
        cause.printStackTrace();
        if (ctx.channel().isActive()) {
            // Closing the channel will trigger handshake failure.
            ctx.channel().close();
        } else {
            // Channel didn't open, so we must fire handshake failure directly.
            this.fireHandshakeFailed(ctx);
        }
    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, 
            ChannelPromise promise) throws Exception {
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
                    "appending '" + msg.toString().trim() +
                    "' to queue!");
                super.write(ctx, msg, promise);
            } else {
                // Otherwise, queue messages in order until the handshake
                // completes.
                this.messages.offer(msg);
            }
        }
    }

    // private static helpers -------------------------------------------------

    private static void out(String s) {
        System.err.println(s);
    }

    // private helpers --------------------------------------------------------

    private void fireHandshakeFailed(ChannelHandlerContext ctx) {
        this.handshakeComplete.set(true);
        this.handshakeFailed.set(true);
        this.latch.countDown();
        ctx.channel().close();
        ctx.fireUserEventTriggered(HandshakeEvent.handshakeFailed());
    }

    private void fireHandshakeSucceeded(String client,
                                        ChannelHandlerContext ctx) {
        this.handshakeComplete.set(true);
        this.handshakeFailed.set(false);
        this.latch.countDown();
        ctx.fireUserEventTriggered(HandshakeEvent
                .handshakeSucceeded(client));
    }
}
