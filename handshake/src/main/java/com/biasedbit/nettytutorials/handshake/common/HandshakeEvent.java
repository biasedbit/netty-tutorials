package com.biasedbit.nettytutorials.handshake.common;

/**
 * @author <a href="mailto:bruno@biasedbit.com">Bruno de Carvalho</a>
 */
public class HandshakeEvent {

    // internal vars ----------------------------------------------------------

    private final boolean successful;
    private final String remoteId;

    // constructors -----------------------------------------------------------

    private HandshakeEvent(String remoteId) {
        this.remoteId = remoteId;
        this.successful = remoteId != null;
    }

    // public static methods --------------------------------------------------

    public static HandshakeEvent handshakeSucceeded(String remoteId) {
        return new HandshakeEvent(remoteId);
    }

    public static HandshakeEvent handshakeFailed() {
        return new HandshakeEvent(null);
    }

    // getters & setters ------------------------------------------------------

    public boolean isSuccessful() {
        return successful;
    }

    public String getRemoteId() {
        return remoteId;
    }
}
