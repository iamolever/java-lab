package org.ovr.javalab.fixmsg;

import net.openhft.chronicle.bytes.Bytes;

public interface FixMessage {
    long getSeqNum();
    void setSeqNum(final long seqNum);

    String getMsgType();

    void setMsgType(final MessageType msgType);
    void setMsgType(final String msgType);

    String getSenderCompId();
    void setSenderCompId(final String senderCompId);

    String getTargetCompId();
    void setTargetCompId(final String targetCompId);

    Bytes getRawMessage();

    int getHeaderLength();
    void setHeaderLength(final int headerLength);

    void clear();

    static FixMessage instance() {
        return new StdFixMessage();
    }

    class StdFixMessage implements FixMessage {
        private final static int DEFAULT_BUFFER_SIZE = 512;

        private Bytes bytes = Bytes.allocateElasticDirect(DEFAULT_BUFFER_SIZE);
        private int headerLength = 0;

        private long seqNum;
        private String msgType;
        private String senderCompId;
        private String targetCompId;

        @Override
        public long getSeqNum() {
            return this.seqNum;
        }

        @Override
        public void setSeqNum(long seqNum) {
            this.seqNum = seqNum;
        }

        @Override
        public String getMsgType() {
            return this.msgType;
        }

        @Override
        public void setMsgType(MessageType msgType) {
            this.msgType = msgType.getProtocolValue();
        }

        @Override
        public void setMsgType(String msgType) {
            this.msgType = msgType;
        }

        @Override
        public String getSenderCompId() {
            return this.senderCompId;
        }

        @Override
        public void setSenderCompId(String senderCompId) {
            this.senderCompId = senderCompId;
        }

        @Override
        public String getTargetCompId() {
            return targetCompId;
        }

        @Override
        public void setTargetCompId(String targetCompId) {
            this.targetCompId = targetCompId;
        }

        @Override
        public Bytes getRawMessage() {
            return this.bytes;
        }

        @Override
        public int getHeaderLength() {
            return this.headerLength;
        }

        @Override
        public void setHeaderLength(final int headerLength) {
            this.headerLength = headerLength;
        }

        @Override
        public void clear() {
            this.bytes.clear();
            this.senderCompId = null;
            this.targetCompId = null;
            this.seqNum = 0;
            this.msgType = null;
            this.headerLength = 0;
        }

        @Override
        public String toString() {
            return "StdFixMessage{" +
                    "bytes=" + bytes +
                    ", seqNum=" + seqNum +
                    ", msgType='" + msgType + '\'' +
                    ", senderCompId='" + senderCompId + '\'' +
                    ", targetCompId='" + targetCompId + '\'' +
                    '}';
        }
    }
}
