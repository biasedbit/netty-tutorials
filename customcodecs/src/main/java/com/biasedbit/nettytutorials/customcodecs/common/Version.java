package com.biasedbit.nettytutorials.customcodecs.common;

/**
 * Envelope version enum.
 */
public enum Version {

    // constants ------------------------------------------------------------------------------------------------------

    VERSION1((byte) 0x01),
    VERSION2((byte) 0x02),
    // put last since it's the least likely one to be encountered in the fromByte() function
    UNKNOWN((byte) 0x00);

    // internal vars --------------------------------------------------------------------------------------------------

    private final byte b;

    // constructors ---------------------------------------------------------------------------------------------------

    private Version(byte b) {
        this.b = b;
    }

    // public static methods ------------------------------------------------------------------------------------------

    public static Version fromByte(byte b) {
        for (Version code : values()) {
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
