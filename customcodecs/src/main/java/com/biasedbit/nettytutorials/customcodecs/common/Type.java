package com.biasedbit.nettytutorials.customcodecs.common;

/**
 * Envelope type enum.
 */
public enum Type {

    // constants ------------------------------------------------------------------------------------------------------

    REQUEST((byte) 0x01),
    RESPONSE((byte) 0x02),
    KEEP_ALIVE((byte) 0x03),
    // put last since it's the least likely one to be encountered in the fromByte() function
    UNKNOWN((byte) 0x00);

    // internal vars --------------------------------------------------------------------------------------------------

    private final byte b;

    // constructors ---------------------------------------------------------------------------------------------------

    private Type(byte b) {
        this.b = b;
    }

    // public static methods ------------------------------------------------------------------------------------------

    public static Type fromByte(byte b) {
        for (Type code : values()) {
            if (code.b == b) {
                return code;
            }
        }

        return UNKNOWN;
    }

    // getters & setters ----------------------------------------------------------------------------------------------

    public byte getByteValue() {
        return b;
    }
}
