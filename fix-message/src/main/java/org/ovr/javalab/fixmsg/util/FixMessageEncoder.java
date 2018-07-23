package org.ovr.javalab.fixmsg.util;

import net.openhft.chronicle.bytes.Bytes;
import org.ovr.javalab.fixmsg.FixMessage;
import org.ovr.javalab.fixmsg.FixVersion;
import org.ovr.javalab.fixmsg.StdHeaderField;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.ovr.javalab.fixmsg.util.FixMessageUtil.CHKSUM_FILES_LEN;
import static org.ovr.javalab.fixmsg.util.FixMessageUtil.SOH;

public interface FixMessageEncoder {
    static DateTimeFormatter FIX_UTC_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMdd-HH:mm:ss.SSS").withZone(ZoneOffset.UTC);

    static FixMessageEncoder stdEncoder(final FixVersion fixVersion) {
        return new StandardFixMessageEncoder(fixVersion);
    }
    static void completeTag(final Bytes buffer) {
        buffer.writeByte(FixMessageUtil.ASCII_EQUALS);
    }
    static void completeField(final Bytes buffer) {
        buffer.writeByte(FixMessageUtil.SOH);
    }

    void encodeMessage(final Bytes buffer, final FixMessage fixMessage);

    class StandardFixMessageEncoder implements FixMessageEncoder {
        final FixVersion fixVersion;

        private StandardFixMessageEncoder(final FixVersion fixVersion) {
            this.fixVersion = fixVersion;
        }

        @Override
        public void encodeMessage(final Bytes buffer, final FixMessage fixMessage) {
            final long msgStartPosition = buffer.writePosition();
            encodeBeginString(buffer);
            final long bodyLenPosition = buffer.writePosition();
            encodeMsgType(buffer, fixMessage);
            encodeSender(buffer, fixMessage);
            encodeTarget(buffer, fixMessage);
            encodeSeqNum(buffer, fixMessage);
            encodeTimestamp(buffer);
            encodeBody(buffer, fixMessage);
            encodeBodyLen(buffer, bodyLenPosition);
            encodeChecksum(buffer, msgStartPosition);
        }

        private void encodeBeginString(final Bytes buffer) {
            buffer.write(StdHeaderField.BeginString.asString());
            completeTag(buffer);
            buffer.write(this.fixVersion.getBeginString());
            completeField(buffer);
        }

        private void encodeBodyLen(final Bytes buffer, final long bodyLenPosition) {
            final long bodyLen = buffer.writePosition() - bodyLenPosition;
            final int digitsInBodyLen = ByteUtil.digitsInNumber(bodyLen);
            final int bodyLenFieldLen = 3 + digitsInBodyLen; // where 3 is '9=' length + SOH in the end
            buffer.writeSkip(bodyLenFieldLen);
            final long writePosition = buffer.writePosition();
            buffer.move(bodyLenPosition, bodyLenPosition + bodyLenFieldLen, bodyLen);
            buffer.writePosition(bodyLenPosition);
            buffer.write(StdHeaderField.BodyLength.asString());
            completeTag(buffer);
            ByteUtil.writePositiveNum(buffer, bodyLen, digitsInBodyLen);
            buffer.writeSkip(digitsInBodyLen);
            completeField(buffer);
            buffer.writePosition(writePosition);
        }

        private void encodeMsgType(final Bytes buffer, final FixMessage fixMessage) {
            buffer.write(StdHeaderField.MsgType.asString());
            completeTag(buffer);
            buffer.write(fixMessage.getMsgType());
            completeField(buffer);
        }

        private void encodeSender(final Bytes buffer, final FixMessage fixMessage) {
            buffer.write(StdHeaderField.SenderCompID.asString());
            completeTag(buffer);
            buffer.write(fixMessage.getSenderCompId());
            completeField(buffer);
        }

        private void encodeTarget(final Bytes buffer, final FixMessage fixMessage) {
            buffer.write(StdHeaderField.TargetCompID.asString());
            completeTag(buffer);
            buffer.write(fixMessage.getTargetCompId());
            completeField(buffer);
        }

        private void encodeSeqNum(final Bytes buffer, final FixMessage fixMessage) {
            buffer.write(StdHeaderField.MsgSeqNum.asString());
            completeTag(buffer);
            ByteUtil.appendPositiveNum(buffer, fixMessage.getSeqNum());
            completeField(buffer);
        }

        private void encodeTimestamp(final Bytes buffer) {
            buffer.write(StdHeaderField.SendingTime.asString());
            completeTag(buffer);
            buffer.write(OffsetDateTime.now().format(FIX_UTC_DATETIME_FORMATTER));
            completeField(buffer);
        }

        private int getChecksumFieldPad(final Bytes buffer) {
            if (buffer.readLimit() >= CHKSUM_FILES_LEN) {
                final int chksumOffset = (int) buffer.readLimit() - CHKSUM_FILES_LEN;
                if (buffer.charAt(chksumOffset) == SOH && buffer.charAt(chksumOffset+1) == '1'
                        && buffer.charAt(chksumOffset+2) == '0'
                        && buffer.charAt(chksumOffset+3) == FixMessageUtil.ASCII_EQUALS) {
                    return CHKSUM_FILES_LEN;
                }
            }
            return 0;
        }

        private void encodeBody(final Bytes buffer, final FixMessage fixMessage) {
            final long bodyLen = fixMessage.getRawMessage().readLimit() -
                    fixMessage.getHeaderLength() - getChecksumFieldPad(fixMessage.getRawMessage());
            buffer.write(fixMessage.getRawMessage(), fixMessage.getHeaderLength(), bodyLen);
            completeField(buffer);
        }

        private void encodeChecksum(final Bytes buffer, final long startMessagePosition) {
            final int checkSum = ByteUtil.checkSum(buffer, startMessagePosition);
            buffer.write(StdHeaderField.CheckSum.asString());
            completeTag(buffer);
            ByteUtil.appendPositiveNum(buffer, checkSum);
            completeField(buffer);
        }
    }
}
