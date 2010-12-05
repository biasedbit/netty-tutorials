package com.biasedbit.nettytutorials.customcodecs.common;

import java.io.Serializable;

public class Envelope implements Serializable {

    // internal vars --------------------------------------------------------------------------------------------------

    private Version version;
    private Type type;
    private byte[] payload;

    // constructors ---------------------------------------------------------------------------------------------------

    public Envelope() {
    }

    public Envelope(Version version, Type type, byte[] payload) {
        this.version = version;
        this.type = type;
        this.payload = payload;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    // low level overrides --------------------------------------------------------------------------------------------

    @Override
    public String toString() {
        return new StringBuilder()
                .append("Envelope{")
                .append("version=").append(version)
                .append(", type=").append(type)
                .append(", payload=").append(payload == null ? null : payload.length + "bytes")
                .append('}').toString();
    }
}
