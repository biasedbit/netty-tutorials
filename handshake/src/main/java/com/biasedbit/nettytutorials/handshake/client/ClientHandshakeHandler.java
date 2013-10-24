package com.biasedbit.nettytutorials.handshake.client;

import com.biasedbit.nettytutorials.handshake.common.Challenge;
import com.biasedbit.nettytutorials.handshake.common.HandshakeEvent;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import java.nio.channels.Channels;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author <a href="mailto:bruno@biasedbit.com">Bruno de Carvalho</a>
 */
public class ClientHandshakeHandler extends ChannelDuplexHandler {

    // internal vars ----------------------------------------------------------

    private final long timeoutInMillis;
    private final String localId;
    private final String remoteId;
    private final AtomicBoolean handshakeComplete;
    private final AtomicBoolean handshakeFailed;
    private final CountDownLatch latch = new CountDownLatch(1);
    private final Queue<Object> messages = new ArrayDeque<Object>();
    private final Object handshakeMutex = new Object();
    private String challenge;

    // constructors -----------------------------------------------------------

    public ClientHandshakeHandler(String localId, String remoteId,
                                  long timeoutInMillis) {
        this.localId = localId;
        this.remoteId = remoteId;
        this.timeoutInMillis = timeoutInMillis;
        this.handshakeComplete = new AtomicBoolean(false);
        this.handshakeFailed = new AtomicBoolean(false);
    }

    // SimpleChannelHandler ---------------------------------------------------

    @Override
    public void channelRead(ChannelHandlerContext ctx,
            Object msg) throws Exception {
        if (this.handshakeFailed.get()) {
            // Bail out fast if handshake already failed
            return;
        }

        if (this.handshakeComplete.get()) {
            // If handshake succeeded but message still came through this
            // handler, then immediately send it upwards.
            // Chances are it's the last time a message passes through
            // this handler...
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

            // Parse the challenge.
            // Expected format is "clientId:serverId:challenge"
            String[] params = ((String) msg).trim().split(":");
            if (params.length != 3) {
                out("--- CLIENT-HS :: Invalid handshake: expected 3 params, " +
                    "got " + params.length);
                this.fireHandshakeFailed(ctx);
                return;
            }

            // Silly validations...
            // 1. Validate that server replied correctly to this client's id.
            if (!params[0].equals(this.localId)) {
                out("--- CLIENT-HS == Handshake failed: local id is " +
                    this.localId +" but challenge response is for '" +
                    params[0] + "'");
                this.fireHandshakeFailed(ctx);
                return;
            }

            // 2. Validate that asserted server id is its actual id.
            if (!params[1].equals(this.remoteId)) {
                out("--- CLIENT-HS :: Handshake failed: expecting remote id " +
                    this.remoteId + " but got " + params[1]);
                this.fireHandshakeFailed(ctx);
                return;
            }

            // 3. Ensure that challenge response is correct.
            if (!Challenge.isValidResponse(params[2], this.challenge)) {
                out("--- CLIENT-HS :: Handshake failed: '" + params[2] +
                    "' is not a valid response for challenge '" +
                    this.challenge + "'");
                this.fireHandshakeFailed(ctx);
                return;
            }

            // Everything went okay!
            out("--- CLIENT-HS :: Challenge validated, flushing messages & " +
                "removing handshake handler from pipeline.");

            // Flush messages *directly* downwards.
            // Calling ctx.getChannel().write() here would cause the messages
            // to be inserted at the top of the pipeline, thus causing them
            // to pass through this class's writeRequest() and be re-queued.
            out("--- CLIENT-HS :: " + this.messages.size() +
                " messages in queue to be flushed.");
            for (Object message : this.messages) {
                ctx.write(message);
            }
            ctx.flush();

            // Remove this handler from the pipeline; its job is finished.
            ctx.pipeline().remove(this);

            // Finally fire success message upwards.
            this.fireHandshakeSucceeded(this.remoteId, ctx);
        }
    }

    @Override
    public void channelActive(final ChannelHandlerContext ctx) 
            throws Exception {
        out("--- CLIENT-HS :: Outgoing connection established to: " +
            ctx.channel().remoteAddress());

        // Write the handshake & add a timeout listener.
        this.challenge = Challenge.generateChallenge();
        String handshake =
                this.localId + ':' + this.remoteId + ':' + challenge + '\n';

        // We need to write to ctx directly rather than call 
        // ctx.channel().write() otherwise the message would pass through this
        // class's write() method defined below.
        ChannelFuture f = ctx.writeAndFlush(handshake);
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future)
                    throws Exception {
                // Once this message is sent, start the timeout checker.
                new Thread() {
                    @Override
                    public void run() {
                        // Wait until either handshake completes (releases the
                        // latch) or this latch times out.
                        try {
                            latch.await(timeoutInMillis, TimeUnit.MILLISECONDS);
                        } catch (InterruptedException e1) {
                            out("--- CLIENT-HS :: Handshake timeout checker: " +
                                "interrupted!");
                            e1.printStackTrace();
                        }

                        // Informative output, do nothing...
                        if (handshakeFailed.get()) {
                            out("--- CLIENT-HS :: (pre-synchro) Handshake " +
                                "timeout checker: discarded " +
                                "(handshake failed)");
                            return;
                        }

                        // More informative output, do nothing...
                        if (handshakeComplete.get()) {
                            out("--- CLIENT-HS :: (pre-synchro) Handshake " +
                                "timeout checker: discarded" +
                                "(handshake completed)");
                            return;
                        }

                        // Handshake has neither failed nor completed, time
                        // to do something! (trigger failure).
                        // Lock on the mutex first...
                        synchronized (handshakeMutex) {
                            // Same checks as before, conditions might have
                            // changed while waiting to get a lock on the
                            // mutex.
                            if (handshakeFailed.get()) {
                                out("--- CLIENT-HS :: (synchro) Handshake " +
                                    "timeout checker: already failed.");
                                return;
                            }

                            if (!handshakeComplete.get()) {
                                // If handshake wasn't completed meanwhile,
                                // time to mark the handshake as having failed.
                                out("--- CLIENT-HS :: (synchro) Handshake " +
                                    "timeout checker: timed out, " +
                                    "killing connection.");
                                fireHandshakeFailed(ctx);
                            } else {
                                // Informative output; the handshake was
                                // completed while this thread was waiting
                                // for a lock on the handshakeMutex.
                                // Do nothing...
                                out("--- CLIENT-HS :: (synchro) Handshake " +
                                    "timeout checker: discarded " +
                                    "(handshake OK)");
                            }
                        }
                    }
                }.start();
            }
        });
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        out("--- CLIENT-HS :: Channel closed.");
        if (!this.handshakeComplete.get()) {
            this.fireHandshakeFailed(ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        out("--- CLIENT-HS :: Exception caught.");
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
                out("--- CLIENT-HS :: Handshake already completed, not " +
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
        System.out.println(s);
    }

    // private helpers --------------------------------------------------------

    private void fireHandshakeFailed(ChannelHandlerContext ctx) {
        this.handshakeComplete.set(true);
        this.handshakeFailed.set(true);
        this.latch.countDown();
        ctx.channel().close();
        ctx.fireUserEventTriggered(HandshakeEvent.handshakeFailed());
    }

    private void fireHandshakeSucceeded(String server,
                                        ChannelHandlerContext ctx) {
        this.handshakeComplete.set(true);
        this.handshakeFailed.set(false);
        this.latch.countDown();
        ctx.fireUserEventTriggered(HandshakeEvent.handshakeSucceeded(server));
    }
}
