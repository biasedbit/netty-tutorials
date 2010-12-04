package com.biasedbit.nettytutorials.handshake.server;

/**
 * @author <a href="mailto:bruno@biasedbit.com">Bruno de Carvalho</a>
 */
public interface ServerListener {

    void messageReceived(ServerHandler handler, String message);

    void connectionOpen(ServerHandler serverHandler);
}
