package org.ovr.javalab.fixmsg;

public enum FixField {
    Account(1),
    BeginSeqNo(7),
    BusinessRejectReason(380),
    ClOrdID(11),
    EncryptMethod(98),
    EndSeqNo(16),
    HeartBtInt(108),
    OrderQty(38),
    OrdType(40),
    OrigSendingTime(122),
    Password(554),
    PossDupFlag(43),
    Price(44),
    RefMsgType(372),
    RefSeqNum(45),
    ResetSeqNumFlag(141),
    SecurityID(48),
    SecurityIDSource(22),
    SessionRejectReason(373),
    Side(54),
    Symbol(55),
    TestReqID(112),
    TransactTime(60),
    RawDataLength(95),
    RawData(96),
    Username(553);

    final int tagNum;
    final CharSequence charSequence;

    FixField(final int tagNum) {
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
