package org.ovr.javalab.fixmsg;

public enum StdHeaderField {
    BeginString(8),
    BodyLength(9),
    MsgSeqNum(34),
    MsgType(35),
    SenderCompID(49),
    TargetCompID(56),
    SendingTime(52),
    CheckSum(10);

    final int tagNum;
    final CharSequence charSequence;

    StdHeaderField(final int tagNum) {
        this.tagNum = tagNum;
        this.charSequence = String.valueOf(tagNum);
    }

    public int asTagNum() {
        return tagNum;
    }

    public CharSequence asString() {
        return charSequence;
    }
}
