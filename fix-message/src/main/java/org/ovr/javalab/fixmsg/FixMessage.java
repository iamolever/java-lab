package org.ovr.javalab.fixmsg;

public interface FixMessage {
    int BeginString = 8;
    int BodyLength = 9;
    int MsgType	= 35;
    int SenderCompID = 49;
    int TargetCompID = 56;
    int CheckSum = 10;
}
