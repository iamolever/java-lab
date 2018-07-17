package org.ovr.javalab.fixmsg;

import net.openhft.lang.model.constraints.MaxSize;

public interface FixMessage {
    static FixMessage instance() {
        return new FixMessageImpl();
    }

    long getSeqNum();
    void setSeqNum(final long seqNum);

    String getMsgType();

    void setMsgType(@MaxSize(3) final String msgType);

    String getSenderCompId();
    void setSenderCompId(@MaxSize(256) final String senderCompId);

    String getTargetCompId();
    void setTargetCompId(@MaxSize(256) final String targetCompId);

    void clear();

    class FixMessageImpl implements FixMessage {
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
        public void clear() {
            this.senderCompId = null;
            this.targetCompId = null;
            this.seqNum = 0;
            this.msgType = null;
        }

        @Override
        public String toString() {
            return "FixMessageImpl{" +
                    "seqNum=" + seqNum +
                    ", msgType='" + msgType + '\'' +
                    ", senderCompId='" + senderCompId + '\'' +
                    ", targetCompId='" + targetCompId + '\'' +
                    '}';
        }
    }
}
