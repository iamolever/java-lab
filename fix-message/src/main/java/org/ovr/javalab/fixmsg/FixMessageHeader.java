package org.ovr.javalab.fixmsg;

public interface FixMessageHeader {
    int BeginString = 8;
    int BodyLength = 9;
    int MsgSeqNum = 34;
    int MsgType	= 35;
    int SenderCompID = 49;
    int TargetCompID = 56;
    int SendingTime = 52;
    int CheckSum = 10;
}
