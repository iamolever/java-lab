package org.ovr.javalab.fixmsg;

public enum FixVersion {
    FIX40("FIX.4.0"),
    FIX41("FIX.4.1"),
    FIX42("FIX.4.2"),
    FIX43("FIX.4.3"),
    FIX44("FIX.4.4"),
    FIX50("FIX.5.0"),
    FIX50SP1("FIX.5.0SP1"),
    FIX50SP2("FIX.5.0SP2"),
    FIXT11("FIXT.1.1");

    private final String beginString;

    FixVersion(final String beginString) {
        this.beginString = beginString;
    }

    public String getBeginString() {
        return beginString;
    }
}
