package com.biasedbit.nettytutorials.handshake;

import com.biasedbit.nettytutorials.handshake.server.Server;
import com.biasedbit.nettytutorials.handshake.server.ServerHandler;
import com.biasedbit.nettytutorials.handshake.server.ServerListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author <a href="mailto:bruno@biasedbit.com">Bruno de Carvalho</a>
 */
public class ServerRunner {

    public static void main(String[] args) {
        final Map<ServerHandler, AtomicInteger> lastMap =
                new ConcurrentHashMap<ServerHandler, AtomicInteger>();

        // Create a new server with id "server1" with a listener that ensures
        // that for each handler, perfect message order is guaranteed.
        final Server s = new Server("server1", new ServerListener() {

            @Override
            public void messageReceived(ServerHandler handler,
                                        String message) {
                AtomicInteger last = lastMap.get(handler);
                int num = Integer.parseInt(message.trim());
                if (num != (last.get() + 1)) {
                    System.err.println("+++ SERVER-LISTENER(" +
                                       handler.getRemoteId() + ") :: " +
                                       "OUT OF ORDER!!! expecting " +
                                       (last.get() + 1) + " and got " +
                                       message);
                } else {
                    last.set(num);
                }

                handler.sendMessage(message);
            }

            @Override
            public void connectionOpen(ServerHandler handler) {
                System.err.println("+++ SERVER-LISTENER(" +
                                   handler.getRemoteId() +
                                   ") :: Connection with " +
                                   handler.getRemoteId() +
                                   " opened & ready to send/receive data.");
                AtomicInteger counter = new AtomicInteger();
                lastMap.put(handler, counter);
            }
        });

        if (!s.start()) {
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                s.stop();
            }
        });
    }
}
