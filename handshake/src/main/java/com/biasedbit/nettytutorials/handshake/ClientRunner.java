package com.biasedbit.nettytutorials.handshake;

import com.biasedbit.nettytutorials.handshake.client.Client;
import com.biasedbit.nettytutorials.handshake.client.ClientListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:bruno@biasedbit.com">Bruno de Carvalho</a>
 */
public class ClientRunner {

    public static void runClient(final String id, final String serverId,
                                 final int nMessages)
            throws InterruptedException {

        final AtomicInteger cLast = new AtomicInteger();
        final AtomicInteger clientCounter = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(1);

        // Create a client with custom id, that connects to a server with given
        // id and has a message listener that ensures that ALL messages are
        // received in perfect order.
        Client c = new Client(id, serverId, new ClientListener() {
            @Override
            public void messageReceived(String message) {
                int num = Integer.parseInt(message.trim());
                if (num != (cLast.get() + 1)) {
                    System.err.println("--- CLIENT-LISTENER(" + id + ") " +
                                       ":: OUT OF ORDER!!! expecting " +
                                       (cLast.get() + 1) + " and got " +
                                       message);
                } else {
                    cLast.set(num);
                }

                if (clientCounter.incrementAndGet() >= nMessages) {
                    latch.countDown();
                }
            }
        });

        if (!c.start()) {
            return;
        }

        for (int i = 0; i < nMessages; i++) {
            // This sleep here prevents all messages to be instantly queued
            // in the handshake message queue. Since handshake takes some time,
            // all messages sent during handshake will be queued (and later on
            // flushed).
            // Since we want to test the effect of removing the handshake
            // handler from the pipeline (and ensure that message order is
            // preserved), this sleep helps us accomplish that with a random
            // factor.
            // If lucky, a couple of messages will even hit the handshake
            // handler *after* the handshake has been completed but right
            // before the handshake handler is removed from the pipeline.
            // Worry not, that case is also covered :)
            Thread.sleep(1L);
            c.sendMessage((i + 1) + "\n");
        }

        // Run the client for some time, then shut it down.
        latch.await(10, TimeUnit.SECONDS);
        c.stop();
    }

    public static void main(String[] args) throws InterruptedException {
        // More clients will test robustness of the server, but output becomes
        // more confusing.
        int nClients = 1;
        final int nMessages = 10000;
        // Changing this value to something different than the server's id
        // will cause handshaking to fail.
        final String serverId = "server1";
        ExecutorService threadPool = Executors.newCachedThreadPool();
        for (int i = 0; i < nClients; i++) {
            final int finalI = i;
            threadPool.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        ClientRunner.runClient("client" + finalI, serverId,
                                               nMessages);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
